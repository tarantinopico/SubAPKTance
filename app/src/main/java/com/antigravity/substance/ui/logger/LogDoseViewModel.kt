package com.antigravity.substance.ui.logger

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antigravity.substance.data.db.entities.CustomFieldDefEntity
import com.antigravity.substance.data.db.entities.IntakeCustomFieldValueEntity
import com.antigravity.substance.data.db.entities.IntakeLogEntity
import com.antigravity.substance.data.db.entities.RouteOfAdminEntity
import com.antigravity.substance.data.db.entities.SubstanceEntity
import com.antigravity.substance.data.db.entities.SubstanceSubtypeEntity
import com.antigravity.substance.data.preferences.UserSettings
import com.antigravity.substance.data.repository.SubstanceRepository
import com.antigravity.substance.domain.engine.PharmacokineticEngine
import com.antigravity.substance.domain.model.DoseTier
import com.antigravity.substance.domain.model.ProjectedDosePreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.roundToInt

@Immutable
data class LogDoseUiState(
    val substances: List<SubstanceEntity> = emptyList(),
    val selectedSubstance: SubstanceEntity? = null,
    val applicableSubtypes: List<SubstanceSubtypeEntity> = emptyList(),
    val selectedSubtype: SubstanceSubtypeEntity? = null,
    val routesOfAdmin: List<RouteOfAdminEntity> = emptyList(),
    val selectedRoa: RouteOfAdminEntity = RouteOfAdminEntity(1L, "Oral", 1.0, 1.0, 1.0, true),
    val doseAmount: Double = 100.0,
    val backdateOffsetMinutes: Int = 0, // 0 = NOW, -15 = 15 mins ago, etc.
    val selectedContextTag: String = "Focus",
    val subjectiveRating: Int = 8,
    val notes: String = "",
    val applicableCustomFields: List<CustomFieldDefEntity> = emptyList(),
    val customFieldValues: Map<String, String> = emptyMap(),
    val glowByStackedActiveLoad: Boolean = true,
    val activeBackgroundTier: DoseTier = DoseTier.MEDIUM,
    val projectedPreview: ProjectedDosePreview? = null,
    val activeLoadPerSubstance: Map<Long, Double> = emptyMap(),
    val settings: UserSettings = UserSettings(),
    val lastCommittedMessage: String? = null
)

class LogDoseViewModel(
    private val repository: SubstanceRepository
) : ViewModel() {

    private val selectedSubstanceIdFlow = MutableStateFlow<Long?>(null)
    private val selectedSubtypeIdFlow = MutableStateFlow<Long?>(null)
    private val selectedRoaNameFlow = MutableStateFlow("Oral")
    private val doseAmountOverrideFlow = MutableStateFlow<Double?>(null)
    private val backdateMinutesFlow = MutableStateFlow(0)
    private val contextTagFlow = MutableStateFlow("Focus")
    private val subjectiveRatingFlow = MutableStateFlow(8)
    private val notesFlow = MutableStateFlow("")
    private val customFieldsMapFlow = MutableStateFlow<Map<String, String>>(emptyMap())
    private val lastCommittedMsgFlow = MutableStateFlow<String?>(null)

    private val formInputFlow = combine(
        selectedSubstanceIdFlow,
        selectedSubtypeIdFlow,
        selectedRoaNameFlow,
        doseAmountOverrideFlow,
        backdateMinutesFlow
    ) { subId, subtypeId, roaName, doseOverride, backdateMins ->
        FormInputTuple(subId, subtypeId, roaName, doseOverride, backdateMins)
    }

    private val metaInputFlow = combine(
        contextTagFlow,
        subjectiveRatingFlow,
        notesFlow,
        customFieldsMapFlow,
        lastCommittedMsgFlow
    ) { tag, rating, notes, customMap, committedMsg ->
        MetaInputTuple(tag, rating, notes, customMap, committedMsg)
    }

    val uiState: StateFlow<LogDoseUiState> = combine(
        repository.activeSubstancesFlow,
        repository.allLogsFlow,
        repository.allSubtypesFlow,
        combine(repository.routesOfAdminFlow, repository.customFieldDefsFlow) { r, c -> r to c },
        combine(repository.settingsFlow, formInputFlow, metaInputFlow) { s, f, m -> Triple(s, f, m) }
    ) { substances, allLogs, allSubtypes, (routes, allFieldDefs), (settings, form, meta) ->
        val nowMillis = System.currentTimeMillis()
        val selectedSub = substances.firstOrNull { it.id == form.subId }
            ?: substances.firstOrNull()

        val applicableSubtypes = if (selectedSub != null) {
            allSubtypes.filter { it.substanceId == selectedSub.id }
        } else emptyList()

        val selectedSubtype = applicableSubtypes.firstOrNull { it.id == form.subtypeId }
            ?: applicableSubtypes.firstOrNull { it.isDefault }
            ?: applicableSubtypes.firstOrNull()

        val effectiveDoseAmount = form.doseOverride ?: selectedSub?.defaultDose ?: 100.0
        val effectiveRoutes = routes.ifEmpty {
            listOf(RouteOfAdminEntity(1L, "Oral", 1.0, 1.0, 1.0, true))
        }
        val selectedRoa = effectiveRoutes.firstOrNull { it.name == form.roaName }
            ?: effectiveRoutes.first()

        // Active load badges for the top substance carousel
        val logsBySub = allLogs.groupBy { it.substanceId }
        val activeLoadMap = substances.associate { sub ->
            val sLogs = logsBySub[sub.id].orEmpty()
            sub.id to PharmacokineticEngine.combinedEffectAt(nowMillis, sLogs, sub, settings)
        }

        // Applicable Custom Fields for the selected substance
        val applicableDefs = if (selectedSub != null) {
            allFieldDefs.filter { it.substanceId == null || it.substanceId == selectedSub.id }
        } else emptyList()

        val mergedCustomValues = mutableMapOf<String, String>()
        for (def in applicableDefs) {
            mergedCustomValues[def.fieldName] = meta.customMap[def.fieldName] ?: def.defaultValue
        }

        val targetTimestampMs = nowMillis + form.backdateMins * 60_000L
        val preview = if (selectedSub != null) {
            PharmacokineticEngine.previewCandidateDose(
                substance = selectedSub,
                existingLogs = logsBySub[selectedSub.id].orEmpty(),
                candidateDoseAmount = effectiveDoseAmount,
                roa = selectedRoa,
                candidateTimestampMillis = targetTimestampMs,
                subtype = selectedSubtype,
                settings = settings
            )
        } else null

        val activeGlowTier = if (preview != null) {
            if (settings.glowByStackedActiveLoad) preview.stackedPeakTier else preview.singleDoseTier
        } else DoseTier.INACTIVE

        LogDoseUiState(
            substances = substances,
            selectedSubstance = selectedSub,
            applicableSubtypes = applicableSubtypes,
            selectedSubtype = selectedSubtype,
            routesOfAdmin = effectiveRoutes,
            selectedRoa = selectedRoa,
            doseAmount = effectiveDoseAmount,
            backdateOffsetMinutes = form.backdateMins,
            selectedContextTag = meta.tag,
            subjectiveRating = meta.rating,
            notes = meta.notes,
            applicableCustomFields = applicableDefs,
            customFieldValues = mergedCustomValues,
            glowByStackedActiveLoad = settings.glowByStackedActiveLoad,
            activeBackgroundTier = activeGlowTier,
            projectedPreview = preview,
            activeLoadPerSubstance = activeLoadMap,
            settings = settings,
            lastCommittedMessage = meta.committedMsg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LogDoseUiState()
    )

    fun selectSubstance(substanceId: Long) {
        val sub = uiState.value.substances.firstOrNull { it.id == substanceId }
        selectedSubstanceIdFlow.value = substanceId
        selectedSubtypeIdFlow.value = null
        doseAmountOverrideFlow.value = sub?.defaultDose
        lastCommittedMsgFlow.value = null
    }

    fun selectSubtype(subtypeId: Long?) {
        selectedSubtypeIdFlow.value = subtypeId
        lastCommittedMsgFlow.value = null
    }

    fun selectRoa(roaName: String) {
        selectedRoaNameFlow.value = roaName
        lastCommittedMsgFlow.value = null
    }

    fun stepDose(multiplier: Int) {
        val current = uiState.value
        val step = current.selectedSubstance?.stepSize ?: 10.0
        val raw = max(0.0, current.doseAmount + step * multiplier)
        val rounded = (raw * 100.0).roundToInt() / 100.0
        doseAmountOverrideFlow.value = rounded
        lastCommittedMsgFlow.value = null
    }

    fun setExactDose(amount: Double) {
        val rounded = (max(0.0, amount) * 100.0).roundToInt() / 100.0
        doseAmountOverrideFlow.value = rounded
        lastCommittedMsgFlow.value = null
    }

    fun setBackdateOffsetMinutes(offsetMinutes: Int) {
        backdateMinutesFlow.value = offsetMinutes
    }

    fun selectContextTag(tag: String) {
        contextTagFlow.value = tag
    }

    fun setSubjectiveRating(rating: Int) {
        subjectiveRatingFlow.value = rating.coerceIn(1, 10)
    }

    fun updateNotes(text: String) {
        notesFlow.value = text
    }

    fun updateCustomFieldValue(fieldName: String, value: String) {
        val updated = customFieldsMapFlow.value.toMutableMap()
        updated[fieldName] = value
        customFieldsMapFlow.value = updated
    }

    fun toggleGlowStackMode() {
        val current = uiState.value.glowByStackedActiveLoad
        viewModelScope.launch {
            repository.settingsStore.updateVisualTheme(glowByStacked = !current)
        }
    }

    fun commitDoseLog(onCommitted: () -> Unit = {}) {
        val state = uiState.value
        val sub = state.selectedSubstance ?: return
        val subtype = state.selectedSubtype
        val preview = state.projectedPreview ?: return
        if (state.doseAmount <= 0.0) return

        val timestampMs = System.currentTimeMillis() + state.backdateOffsetMinutes * 60_000L
        val customFieldsStr = state.customFieldValues.entries
            .joinToString(";") { "${it.key}=${it.value}" }

        val eavEntities = state.applicableCustomFields.mapNotNull { def ->
            val v = state.customFieldValues[def.fieldName] ?: return@mapNotNull null
            IntakeCustomFieldValueEntity(
                logId = 0L,
                fieldDefId = def.id,
                fieldName = def.fieldName,
                fieldValue = v
            )
        }

        val potencyMult = subtype?.potencyMultiplier ?: 1.0
        val totalOnsetMult = state.selectedRoa.onsetMultiplier * (subtype?.onsetMultiplier ?: 1.0)
        val totalDurMult = state.selectedRoa.durationMultiplier * (subtype?.durationMultiplier ?: 1.0)

        viewModelScope.launch {
            repository.logIntake(
                log = IntakeLogEntity(
                    substanceId = sub.id,
                    subtypeId = subtype?.id,
                    subtypeName = subtype?.name ?: "Standard",
                    potencyMultiplier = potencyMult,
                    timestampMillis = timestampMs,
                    doseAmount = state.doseAmount,
                    unit = sub.defaultUnit,
                    cost = preview.estimatedCost,
                    roaName = state.selectedRoa.name,
                    bioavailabilityFactor = state.selectedRoa.bioavailabilityFactor,
                    onsetMultiplier = totalOnsetMult,
                    durationMultiplier = totalDurMult,
                    evaluatedTier = preview.singleDoseTier.name,
                    stackedPeakTier = preview.stackedPeakTier.name,
                    contextTag = state.selectedContextTag,
                    subjectiveRating = state.subjectiveRating,
                    notes = state.notes,
                    customFieldsMapStr = customFieldsStr
                ),
                fieldValues = eavEntities
            )
            val subtypeStr = if (subtype != null) " • ${subtype.name}" else ""
            lastCommittedMsgFlow.value =
                "Zapsáno ${state.settings.formatDose(state.doseAmount, sub.defaultUnit)} ${sub.name}$subtypeStr (${preview.stackedPeakTier.titleCz})"
            notesFlow.value = ""
            backdateMinutesFlow.value = 0
            onCommitted()
        }
    }

    private data class FormInputTuple(
        val subId: Long?,
        val subtypeId: Long?,
        val roaName: String,
        val doseOverride: Double?,
        val backdateMins: Int
    )

    private data class MetaInputTuple(
        val tag: String,
        val rating: Int,
        val notes: String,
        val customMap: Map<String, String>,
        val committedMsg: String?
    )
}
