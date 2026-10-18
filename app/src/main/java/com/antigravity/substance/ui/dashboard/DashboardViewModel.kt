package com.antigravity.substance.ui.dashboard

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antigravity.substance.data.db.entities.IntakeLogEntity
import com.antigravity.substance.data.db.entities.SubstanceEntity
import com.antigravity.substance.data.preferences.UserSettings
import com.antigravity.substance.data.repository.SubstanceRepository
import com.antigravity.substance.domain.engine.AnalyticsAggregatorEngine
import com.antigravity.substance.domain.engine.PharmacokineticEngine
import com.antigravity.substance.domain.engine.ThresholdEvaluationEngine
import com.antigravity.substance.domain.model.ActiveSubstanceSession
import com.antigravity.substance.domain.model.DoseTier
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.roundToInt

@Immutable
data class TimeSinceLastUseItem(
    val substance: SubstanceEntity,
    val elapsedSeconds: Long,
    val lastDoseAmount: Double,
    val recoveryProgressFraction: Float
)

@Immutable
data class TodaySubstanceConsumptionItem(
    val substance: SubstanceEntity,
    val todayTotalUnits: Double,
    val dailySafeLimit: Double,
    val todayTotalCost: Double,
    val doseCountToday: Int,
    val limitFraction: Float
)

@Immutable
data class TimelineGemItem(
    val logId: Long,
    val substance: SubstanceEntity,
    val hourOfDayFraction: Float, // 0f..1f across 24h
    val doseAmount: Double,
    val tier: DoseTier
)

@Immutable
data class QuickPresetDockItem(
    val substance: SubstanceEntity,
    val doseAmount: Double,
    val label: String
)

@Immutable
data class DashboardUiState(
    val nowMillis: Long = System.currentTimeMillis(),
    val settings: UserSettings = UserSettings(),
    val activeSessions: List<ActiveSubstanceSession> = emptyList(),
    val selectedSessionTabIndex: Int = 0, // 0..N-1 for individual active substances, N for "ALL STACKED"
    val heroCardDisplayMode: Int = 0, // 0 = PK Curve, 1 = Stacked Dose Breakdown & Timers
    val favoritePageOffset: Int = 0,
    val showCostInsteadOfUnits: Boolean = false,
    val totalBioLoadFraction: Float = 0f,
    val highestActiveTier: DoseTier = DoseTier.INACTIVE,
    val todayTotalSpend: Double = 0.0,
    val spendVs7dAvgPercent: Int = 0,
    val todayDosesCount: Int = 0,
    val todayDistinctSubstancesCount: Int = 0,
    val safetyStatusLabel: String = "OPTIMAL", // OPTIMAL, ELEVATED, LIMIT EXCEEDED
    val safetyStatusTier: DoseTier = DoseTier.LIGHT,
    val timeSinceLastUseItems: List<TimeSinceLastUseItem> = emptyList(),
    val todayConsumptionItems: List<TodaySubstanceConsumptionItem> = emptyList(),
    val todayTimelineGems: List<TimelineGemItem> = emptyList(),
    val sleepReadinessOptimal: Boolean = true,
    val sleepWarningSubstanceName: String? = null,
    val quickPresetDockItems: List<QuickPresetDockItem> = emptyList(),
    val lastQuickLoggedId: Long? = null,
    val lastQuickLoggedLabel: String? = null
)

class DashboardViewModel(
    private val repository: SubstanceRepository
) : ViewModel() {

    private val selectedTabFlow = MutableStateFlow(0)
    private val heroModeFlow = MutableStateFlow(0)
    private val favPageFlow = MutableStateFlow(0)
    private val costFlipOverrideFlow = MutableStateFlow<Boolean?>(null)
    private val lastQuickLogState = MutableStateFlow<Pair<Long, String>?>(null)

    // 1-second live ticker for real-time PK curves and second-by-second countdowns
    private val tickerFlow = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(1_000L)
        }
    }

    private val localUiTogglesFlow = combine(
        selectedTabFlow,
        heroModeFlow,
        favPageFlow,
        costFlipOverrideFlow,
        lastQuickLogState
    ) { tab, heroMode, favPage, costFlip, quickLog ->
        LocalDashboardToggles(tab, heroMode, favPage, costFlip, quickLog)
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        repository.activeSubstancesFlow,
        repository.allLogsFlow,
        repository.settingsFlow,
        tickerFlow,
        localUiTogglesFlow
    ) { substances, allLogs, settings, nowMillis, toggles ->
        buildDashboardState(
            substances = substances,
            allLogs = allLogs,
            settings = settings,
            nowMillis = nowMillis,
            toggles = toggles
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState()
    )

    fun selectActiveSessionTab(index: Int) {
        selectedTabFlow.value = index.coerceAtLeast(0)
    }

    fun cycleHeroCardDisplayMode() {
        heroModeFlow.value = (heroModeFlow.value + 1) % 2
    }

    fun cycleFavoritesPage() {
        favPageFlow.value = favPageFlow.value + 1
    }

    fun flipTodayConsumptionCard() {
        val current = costFlipOverrideFlow.value ?: uiState.value.showCostInsteadOfUnits
        costFlipOverrideFlow.value = !current
        viewModelScope.launch {
            repository.settingsStore.toggleDashboardCostCard(!current)
        }
    }

    fun quickLogPreset(item: QuickPresetDockItem) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val sub = item.substance
            val singleTier = ThresholdEvaluationEngine.evaluateTier(item.doseAmount, sub)
            val existingSubLogs = repository.allLogsFlow.stateIn(viewModelScope).value
                .filter { it.substanceId == sub.id }
            val currentLoad = PharmacokineticEngine.combinedEffectAt(now, existingSubLogs, sub)
            val stackedTier = ThresholdEvaluationEngine.evaluateTier(currentLoad + item.doseAmount, sub)

            val logId = repository.logIntake(
                IntakeLogEntity(
                    substanceId = sub.id,
                    timestampMillis = now,
                    doseAmount = item.doseAmount,
                    unit = sub.defaultUnit,
                    cost = item.doseAmount * sub.costPerUnit,
                    roaName = "Oral",
                    bioavailabilityFactor = 1.0,
                    onsetMultiplier = 1.0,
                    durationMultiplier = 1.0,
                    evaluatedTier = singleTier.name,
                    stackedPeakTier = stackedTier.name,
                    contextTag = "Focus",
                    notes = "Quick 1-Tap Cockpit Log"
                )
            )
            lastQuickLogState.value = logId to "${sub.name} ${settingsFormatDose(item.doseAmount, sub.defaultUnit)}"
        }
    }

    fun undoLastQuickLog() {
        val last = lastQuickLogState.value ?: return
        lastQuickLogState.value = null
        viewModelScope.launch {
            repository.deleteLogById(last.first)
        }
    }

    private fun settingsFormatDose(amount: Double, unit: String): String {
        val num = if (amount % 1.0 == 0.0) amount.toInt().toString() else amount.toString()
        return "$num$unit"
    }

    private fun buildDashboardState(
        substances: List<SubstanceEntity>,
        allLogs: List<IntakeLogEntity>,
        settings: UserSettings,
        nowMillis: Long,
        toggles: LocalDashboardToggles
    ): DashboardUiState {
        val startOfToday = AnalyticsAggregatorEngine.startOfDayMillis(nowMillis)
        val logsBySubstance = allLogs.groupBy { it.substanceId }

        // 1. Build all active multi-dose pharmacokinetic sessions
        val activeSessions = substances.mapNotNull { sub ->
            val subLogs = logsBySubstance[sub.id].orEmpty()
            PharmacokineticEngine.buildActiveSession(
                substance = sub,
                allSubstanceLogs = subLogs,
                nowMillis = nowMillis,
                startOfTodayMillis = startOfToday
            )
        }.sortedByDescending { it.currentTier.ordinal * 100.0 + it.currentActiveLoad }

        val highestActiveTier = activeSessions.maxByOrNull { it.currentTier.ordinal }?.currentTier
            ?: DoseTier.INACTIVE

        val totalBioLoadFraction = activeSessions.sumOf {
            (it.currentActiveLoad / it.substance.thresholdStrong.coerceAtLeast(1.0)).coerceIn(0.0, 1.2)
        }.toFloat().coerceIn(0f, 1f)

        // 2. Today's Telemetry
        val todayLogs = allLogs.filter { it.timestampMillis >= startOfToday && it.timestampMillis <= nowMillis + 3_600_000L }
        val todaySpend = todayLogs.sumOf { it.cost }
        val sevenDaysAgo = startOfToday - 7 * 86_400_000L
        val past7dLogs = allLogs.filter { it.timestampMillis in sevenDaysAgo until startOfToday }
        val avg7dDailySpend = past7dLogs.sumOf { it.cost } / 7.0
        val spendVs7dPct = if (avg7dDailySpend > 0.01) {
            (((todaySpend - avg7dDailySpend) / avg7dDailySpend) * 100.0).roundToInt()
        } else 0

        val todayDistinctSubs = todayLogs.map { it.substanceId }.distinct().size

        // 3. Today's Consumption per Substance & Limit Compliance
        var anyLimitExceeded = false
        var anyElevated = highestActiveTier.ordinal >= DoseTier.STRONG.ordinal
        val todayConsumptionItems = substances.mapNotNull { sub ->
            val sToday = todayLogs.filter { it.substanceId == sub.id }
            if (sToday.isEmpty()) return@mapNotNull null
            val sumUnits = sToday.sumOf { it.doseAmount }
            val sumCost = sToday.sumOf { it.cost }
            val frac = (sumUnits / sub.dailyMaxSafeLimit.coerceAtLeast(0.1)).toFloat()
            if (frac > 1.0f) anyLimitExceeded = true
            else if (frac >= 0.75f) anyElevated = true
            TodaySubstanceConsumptionItem(
                substance = sub,
                todayTotalUnits = sumUnits,
                dailySafeLimit = sub.dailyMaxSafeLimit,
                todayTotalCost = sumCost,
                doseCountToday = sToday.size,
                limitFraction = frac.coerceIn(0f, 1.25f)
            )
        }.sortedByDescending { it.limitFraction }

        val (safetyLabel, safetyTier) = when {
            anyLimitExceeded || highestActiveTier == DoseTier.DANGEROUS -> "LIMIT EXCEEDED" to DoseTier.DANGEROUS
            anyElevated -> "ELEVATED" to DoseTier.MEDIUM
            else -> "OPTIMAL" to DoseTier.LIGHT
        }

        // 4. Time Since Last Use Ticker
        val timeSinceItems = substances.mapNotNull { sub ->
            val subLogs = logsBySubstance[sub.id].orEmpty()
            val latest = subLogs.maxByOrNull { it.timestampMillis } ?: return@mapNotNull null
            val elapsedSec = max(0L, (nowMillis - latest.timestampMillis) / 1000L)
            val fullClearSec = (sub.halfLifeHours * 5.0 * 3600.0).toLong().coerceAtLeast(3600L)
            val recoveryFrac = (elapsedSec.toFloat() / fullClearSec.toFloat()).coerceIn(0.05f, 1f)
            TimeSinceLastUseItem(
                substance = sub,
                elapsedSeconds = elapsedSec,
                lastDoseAmount = latest.doseAmount,
                recoveryProgressFraction = recoveryFrac
            )
        }.sortedBy { it.elapsedSeconds }

        // 5. 24-Hour Timeline Gems & Bedtime Sleep Readiness Prediction
        val subMap = substances.associateBy { it.id }
        val timelineGems = todayLogs.mapNotNull { log ->
            val sub = subMap[log.substanceId] ?: return@mapNotNull null
            val hr = AnalyticsAggregatorEngine.hourOfDayFloat(log.timestampMillis)
            TimelineGemItem(
                logId = log.id,
                substance = sub,
                hourOfDayFraction = (hr / 24f).coerceIn(0f, 1f),
                doseAmount = log.doseAmount,
                tier = DoseTier.fromName(log.stackedPeakTier.ifBlank { log.evaluatedTier })
            )
        }.sortedBy { it.hourOfDayFraction }

        val bedtimeMillis = startOfToday + settings.bedtimeHour24 * 3_600_000L
        var sleepDisruptingSub: String? = null
        for (sub in substances) {
            val sLogs = logsBySubstance[sub.id].orEmpty().filter { it.timestampMillis >= startOfToday - 12 * 3_600_000L }
            if (sLogs.isNotEmpty()) {
                val loadAtBedtime = PharmacokineticEngine.combinedEffectAt(bedtimeMillis, sLogs, sub)
                if (loadAtBedtime > sub.sleepDisruptionThreshold) {
                    sleepDisruptingSub = sub.name
                    break
                }
            }
        }

        // 6. Quick 1-Tap Preset Dock Items (Top 3 favorite substances)
        val quickDock = substances.take(3).map { sub ->
            QuickPresetDockItem(
                substance = sub,
                doseAmount = sub.defaultDose,
                label = "+ ${sub.name} ${settings.formatDose(sub.defaultDose, sub.defaultUnit)}"
            )
        }

        val maxTabs = (activeSessions.size + if (activeSessions.size > 1) 1 else 0).coerceAtLeast(1)
        val validTab = toggles.selectedTab.coerceIn(0, maxTabs - 1)

        return DashboardUiState(
            nowMillis = nowMillis,
            settings = settings,
            activeSessions = activeSessions,
            selectedSessionTabIndex = validTab,
            heroCardDisplayMode = toggles.heroMode,
            favoritePageOffset = toggles.favPage,
            showCostInsteadOfUnits = toggles.costFlip ?: settings.showCostOnDashboard,
            totalBioLoadFraction = totalBioLoadFraction,
            highestActiveTier = highestActiveTier,
            todayTotalSpend = todaySpend,
            spendVs7dAvgPercent = spendVs7dPct,
            todayDosesCount = todayLogs.size,
            todayDistinctSubstancesCount = todayDistinctSubs,
            safetyStatusLabel = safetyLabel,
            safetyStatusTier = safetyTier,
            timeSinceLastUseItems = timeSinceItems,
            todayConsumptionItems = todayConsumptionItems,
            todayTimelineGems = timelineGems,
            sleepReadinessOptimal = sleepDisruptingSub == null,
            sleepWarningSubstanceName = sleepDisruptingSub,
            quickPresetDockItems = quickDock,
            lastQuickLoggedId = toggles.quickLog?.first,
            lastQuickLoggedLabel = toggles.quickLog?.second
        )
    }

    private data class LocalDashboardToggles(
        val selectedTab: Int,
        val heroMode: Int,
        val favPage: Int,
        val costFlip: Boolean?,
        val quickLog: Pair<Long, String>?
    )
}
