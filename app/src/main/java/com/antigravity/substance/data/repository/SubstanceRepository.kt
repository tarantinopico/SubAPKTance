package com.antigravity.substance.data.repository

import com.antigravity.substance.data.db.SubstanceDatabase
import com.antigravity.substance.data.db.dao.IntakeLogDao
import com.antigravity.substance.data.db.dao.SubstanceDao
import com.antigravity.substance.data.db.entities.CustomFieldDefEntity
import com.antigravity.substance.data.db.entities.IntakeCustomFieldValueEntity
import com.antigravity.substance.data.db.entities.IntakeLogEntity
import com.antigravity.substance.data.db.entities.RouteOfAdminEntity
import com.antigravity.substance.data.db.entities.SubstanceEntity
import com.antigravity.substance.data.db.entities.SubstanceSubtypeEntity
import com.antigravity.substance.data.preferences.UserSettings
import com.antigravity.substance.data.preferences.UserSettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SubstanceRepository(
    private val substanceDao: SubstanceDao,
    private val intakeLogDao: IntakeLogDao,
    val settingsStore: UserSettingsStore
) {
    val activeSubstancesFlow: Flow<List<SubstanceEntity>> = substanceDao.observeActiveSubstances()
    val allSubstancesFlow: Flow<List<SubstanceEntity>> = substanceDao.observeAllSubstances()
    val allSubtypesFlow: Flow<List<SubstanceSubtypeEntity>> = substanceDao.observeAllSubtypes()
    val allLogsFlow: Flow<List<IntakeLogEntity>> = intakeLogDao.observeAllLogs()
    val customFieldDefsFlow: Flow<List<CustomFieldDefEntity>> = substanceDao.observeAllCustomFieldDefs()
    val routesOfAdminFlow: Flow<List<RouteOfAdminEntity>> = substanceDao.observeAllRoutesOfAdmin()
    val settingsFlow: Flow<UserSettings> = settingsStore.settingsFlow

    fun observeLogsSince(sinceMillis: Long): Flow<List<IntakeLogEntity>> =
        intakeLogDao.observeLogsSince(sinceMillis)

    fun observeLogsForSubstance(substanceId: Long): Flow<List<IntakeLogEntity>> =
        intakeLogDao.observeLogsForSubstance(substanceId)

    fun observeSubtypesForSubstance(substanceId: Long): Flow<List<SubstanceSubtypeEntity>> =
        substanceDao.observeSubtypesForSubstance(substanceId)

    fun observeCustomFieldsForSubstance(substanceId: Long): Flow<List<CustomFieldDefEntity>> =
        substanceDao.observeCustomFieldsForSubstance(substanceId)

    suspend fun ensureSeededIfNeeded() = withContext(Dispatchers.IO) {
        val currentSubstances = substanceDao.getAllSubstancesOnce()
        val settings = settingsStore.settingsFlow.first()
        if (currentSubstances.isEmpty() && !settings.isInitialSeedDone) {
            seedDefaultDataWithDemoLogs()
            settingsStore.markInitialSeedDone()
        } else {
            if (substanceDao.getAllRoutesOfAdminOnce().isEmpty()) {
                substanceDao.insertRoutesOfAdmin(SubstanceDatabase.defaultRoutesOfAdmin())
            }
            if (substanceDao.getAllSubtypesOnce().isEmpty()) {
                substanceDao.insertSubtypes(SubstanceDatabase.defaultSubtypes())
            }
        }
    }

    suspend fun seedDefaultDataWithDemoLogs(nowMillis: Long = System.currentTimeMillis()) = withContext(Dispatchers.IO) {
        substanceDao.deleteAllSubstances()
        substanceDao.deleteAllSubtypes()
        substanceDao.deleteAllCustomFieldDefs()
        substanceDao.deleteAllRoutesOfAdmin()
        intakeLogDao.deleteAllLogs()
        intakeLogDao.deleteAllCustomFieldValues()

        substanceDao.insertRoutesOfAdmin(SubstanceDatabase.defaultRoutesOfAdmin())
        substanceDao.insertSubstances(SubstanceDatabase.defaultSubstances())
        substanceDao.insertSubtypes(SubstanceDatabase.defaultSubtypes())
        substanceDao.insertCustomFieldDefs(SubstanceDatabase.defaultCustomFieldDefs())
        val demoLogs = SubstanceDatabase.buildDemoLogs(nowMillis)
        intakeLogDao.insertLogs(demoLogs)
    }

    suspend fun clearSampleLogsKeepSubstances() = withContext(Dispatchers.IO) {
        intakeLogDao.deleteAllLogs()
        intakeLogDao.deleteAllCustomFieldValues()
    }

    // Substance CRUD
    suspend fun upsertSubstance(substance: SubstanceEntity): Long =
        substanceDao.upsertSubstance(substance)

    suspend fun duplicateSubstance(substance: SubstanceEntity): Long {
        val copy = substance.copy(
            id = 0L,
            name = "${substance.name} (Copy)",
            sortOrder = substance.sortOrder + 1
        )
        return substanceDao.upsertSubstance(copy)
    }

    suspend fun deleteSubstance(substanceId: Long) {
        intakeLogDao.deleteLogsForSubstance(substanceId)
        substanceDao.deleteSubtypesForSubstance(substanceId)
        substanceDao.deleteSubstanceById(substanceId)
    }

    // Subtype CRUD
    suspend fun upsertSubtype(subtype: SubstanceSubtypeEntity): Long =
        substanceDao.upsertSubtype(subtype)

    suspend fun deleteSubtype(subtypeId: Long) =
        substanceDao.deleteSubtypeById(subtypeId)

    // Custom Field CRUD
    suspend fun upsertCustomFieldDef(def: CustomFieldDefEntity): Long =
        substanceDao.upsertCustomFieldDef(def)

    suspend fun deleteCustomFieldDef(id: Long) =
        substanceDao.deleteCustomFieldDefById(id)

    // Route of Admin CRUD
    suspend fun upsertRouteOfAdmin(roa: RouteOfAdminEntity): Long =
        substanceDao.upsertRouteOfAdmin(roa)

    suspend fun deleteRouteOfAdmin(id: Long) =
        substanceDao.deleteRouteOfAdminById(id)

    // Intake Log CRUD
    suspend fun logIntake(
        log: IntakeLogEntity,
        fieldValues: List<IntakeCustomFieldValueEntity> = emptyList()
    ): Long = withContext(Dispatchers.IO) {
        val logId = intakeLogDao.upsertLog(log)
        intakeLogDao.deleteCustomFieldValuesForLog(logId)
        if (fieldValues.isNotEmpty()) {
            intakeLogDao.insertCustomFieldValues(
                fieldValues.map { it.copy(logId = logId) }
            )
        }
        logId
    }

    suspend fun deleteLogById(logId: Long) = withContext(Dispatchers.IO) {
        intakeLogDao.deleteCustomFieldValuesForLog(logId)
        intakeLogDao.deleteLogById(logId)
    }

    suspend fun duplicateLog(log: IntakeLogEntity, newTimestampMillis: Long = System.currentTimeMillis()): Long {
        val copy = log.copy(id = 0L, timestampMillis = newTimestampMillis)
        return intakeLogDao.upsertLog(copy)
    }

    // Full Database JSON Export & Import (including Subtypes & Timing Modes)
    suspend fun exportFullDatabaseJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("version", 2)
        root.put("exportedAtMillis", System.currentTimeMillis())

        val substancesArray = JSONArray()
        substanceDao.getAllSubstancesOnce().forEach { s ->
            substancesArray.put(
                JSONObject().apply {
                    put("id", s.id)
                    put("name", s.name)
                    put("subtitle", s.subtitle)
                    put("category", s.category)
                    put("iconName", s.iconName)
                    put("colorHex", s.colorHex)
                    put("defaultUnit", s.defaultUnit)
                    put("stepSize", s.stepSize)
                    put("defaultDose", s.defaultDose)
                    put("quickPresetsCsv", s.quickPresetsCsv)
                    put("costPerUnit", s.costPerUnit)
                    put("thresholdNegligible", s.thresholdNegligible)
                    put("thresholdLight", s.thresholdLight)
                    put("thresholdMedium", s.thresholdMedium)
                    put("thresholdStrong", s.thresholdStrong)
                    put("thresholdDangerous", s.thresholdDangerous)
                    put("dailyMaxSafeLimit", s.dailyMaxSafeLimit)
                    put("recommendedMinIntervalMinutes", s.recommendedMinIntervalMinutes)
                    put("timingMode", s.timingMode)
                    put("mainEffectDurationMinutes", s.mainEffectDurationMinutes)
                    put("onsetMinutes", s.onsetMinutes)
                    put("comeupMinutes", s.comeupMinutes)
                    put("peakMinutes", s.peakMinutes)
                    put("offsetMinutes", s.offsetMinutes)
                    put("hasBodyPresenceDuration", s.hasBodyPresenceDuration)
                    put("bodyPresenceHours", s.bodyPresenceHours)
                    put("halfLifeHours", s.halfLifeHours)
                    put("sleepDisruptionThreshold", s.sleepDisruptionThreshold)
                    put("stackingBehavior", s.stackingBehavior)
                    put("toleranceGainRate", s.toleranceGainRate)
                    put("toleranceDecayRate", s.toleranceDecayRate)
                    put("bioLoadWeight", s.bioLoadWeight)
                    put("superpositionWeight", s.superpositionWeight)
                    put("isFavorite", s.isFavorite)
                    put("isArchived", s.isArchived)
                    put("sortOrder", s.sortOrder)
                }
            )
        }
        root.put("substances", substancesArray)

        val subtypesArray = JSONArray()
        substanceDao.getAllSubtypesOnce().forEach { st ->
            subtypesArray.put(
                JSONObject().apply {
                    put("id", st.id)
                    put("substanceId", st.substanceId)
                    put("name", st.name)
                    put("subtitle", st.subtitle)
                    put("colorHex", st.colorHex)
                    put("potencyMultiplier", st.potencyMultiplier)
                    put("onsetMultiplier", st.onsetMultiplier)
                    put("durationMultiplier", st.durationMultiplier)
                    if (st.customCostPerUnit != null) put("customCostPerUnit", st.customCostPerUnit)
                    put("isDefault", st.isDefault)
                }
            )
        }
        root.put("subtypes", subtypesArray)

        val logsArray = JSONArray()
        intakeLogDao.getAllLogsOnce().forEach { l ->
            logsArray.put(
                JSONObject().apply {
                    put("id", l.id)
                    put("substanceId", l.substanceId)
                    if (l.subtypeId != null) put("subtypeId", l.subtypeId)
                    put("subtypeName", l.subtypeName)
                    put("potencyMultiplier", l.potencyMultiplier)
                    put("timestampMillis", l.timestampMillis)
                    put("doseAmount", l.doseAmount)
                    put("unit", l.unit)
                    put("cost", l.cost)
                    put("roaName", l.roaName)
                    put("bioavailabilityFactor", l.bioavailabilityFactor)
                    put("onsetMultiplier", l.onsetMultiplier)
                    put("durationMultiplier", l.durationMultiplier)
                    put("evaluatedTier", l.evaluatedTier)
                    put("stackedPeakTier", l.stackedPeakTier)
                    put("contextTag", l.contextTag)
                    put("subjectiveRating", l.subjectiveRating)
                    put("notes", l.notes)
                    put("customFieldsMapStr", l.customFieldsMapStr)
                }
            )
        }
        root.put("logs", logsArray)
        root.toString(2)
    }

    suspend fun importDatabaseJson(jsonStr: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonStr)
            val subArr = root.optJSONArray("substances")
            val subtypesArr = root.optJSONArray("subtypes")
            val logsArr = root.optJSONArray("logs")
            if (subArr != null && subArr.length() > 0) {
                val parsedSubs = mutableListOf<SubstanceEntity>()
                for (i in 0 until subArr.length()) {
                    val o = subArr.getJSONObject(i)
                    parsedSubs.add(
                        SubstanceEntity(
                            id = o.optLong("id", 0L),
                            name = o.optString("name", "Custom"),
                            subtitle = o.optString("subtitle", ""),
                            category = o.optString("category", "Custom"),
                            iconName = o.optString("iconName", "capsule"),
                            colorHex = o.optLong("colorHex", 0xFF0284C7L),
                            defaultUnit = o.optString("defaultUnit", "mg"),
                            stepSize = o.optDouble("stepSize", 10.0),
                            defaultDose = o.optDouble("defaultDose", 50.0),
                            quickPresetsCsv = o.optString("quickPresetsCsv", "25,50,100"),
                            costPerUnit = o.optDouble("costPerUnit", 0.1),
                            thresholdNegligible = o.optDouble("thresholdNegligible", 20.0),
                            thresholdLight = o.optDouble("thresholdLight", 50.0),
                            thresholdMedium = o.optDouble("thresholdMedium", 100.0),
                            thresholdStrong = o.optDouble("thresholdStrong", 200.0),
                            thresholdDangerous = o.optDouble("thresholdDangerous", 300.0),
                            dailyMaxSafeLimit = o.optDouble("dailyMaxSafeLimit", 250.0),
                            recommendedMinIntervalMinutes = o.optInt("recommendedMinIntervalMinutes", 180),
                            timingMode = o.optString("timingMode", "DETAILED"),
                            mainEffectDurationMinutes = o.optInt("mainEffectDurationMinutes", 240),
                            onsetMinutes = o.optInt("onsetMinutes", 15),
                            comeupMinutes = o.optInt("comeupMinutes", 30),
                            peakMinutes = o.optInt("peakMinutes", 90),
                            offsetMinutes = o.optInt("offsetMinutes", 120),
                            hasBodyPresenceDuration = o.optBoolean("hasBodyPresenceDuration", true),
                            bodyPresenceHours = o.optDouble("bodyPresenceHours", 10.0),
                            halfLifeHours = o.optDouble("halfLifeHours", 4.0),
                            sleepDisruptionThreshold = o.optDouble("sleepDisruptionThreshold", 30.0),
                            stackingBehavior = o.optString("stackingBehavior", "FULL_SUPERPOSITION"),
                            toleranceGainRate = o.optDouble("toleranceGainRate", 1.0),
                            toleranceDecayRate = o.optDouble("toleranceDecayRate", 1.0),
                            bioLoadWeight = o.optDouble("bioLoadWeight", 1.0),
                            superpositionWeight = o.optDouble("superpositionWeight", 1.0),
                            isFavorite = o.optBoolean("isFavorite", true),
                            isArchived = o.optBoolean("isArchived", false),
                            sortOrder = o.optInt("sortOrder", i)
                        )
                    )
                }
                substanceDao.insertSubstances(parsedSubs)
            }
            if (subtypesArr != null) {
                val parsedSubtypes = mutableListOf<SubstanceSubtypeEntity>()
                for (i in 0 until subtypesArr.length()) {
                    val o = subtypesArr.getJSONObject(i)
                    parsedSubtypes.add(
                        SubstanceSubtypeEntity(
                            id = o.optLong("id", 0L),
                            substanceId = o.optLong("substanceId", 1L),
                            name = o.optString("name", "Standard"),
                            subtitle = o.optString("subtitle", ""),
                            colorHex = o.optLong("colorHex", 0xFF0284C7L),
                            potencyMultiplier = o.optDouble("potencyMultiplier", 1.0),
                            onsetMultiplier = o.optDouble("onsetMultiplier", 1.0),
                            durationMultiplier = o.optDouble("durationMultiplier", 1.0),
                            customCostPerUnit = if (o.has("customCostPerUnit")) o.optDouble("customCostPerUnit") else null,
                            isDefault = o.optBoolean("isDefault", false)
                        )
                    )
                }
                substanceDao.insertSubtypes(parsedSubtypes)
            }
            if (logsArr != null) {
                val parsedLogs = mutableListOf<IntakeLogEntity>()
                for (i in 0 until logsArr.length()) {
                    val o = logsArr.getJSONObject(i)
                    parsedLogs.add(
                        IntakeLogEntity(
                            id = o.optLong("id", 0L),
                            substanceId = o.optLong("substanceId", 1L),
                            subtypeId = if (o.has("subtypeId")) o.optLong("subtypeId") else null,
                            subtypeName = o.optString("subtypeName", "Standard"),
                            potencyMultiplier = o.optDouble("potencyMultiplier", 1.0),
                            timestampMillis = o.optLong("timestampMillis", System.currentTimeMillis()),
                            doseAmount = o.optDouble("doseAmount", 1.0),
                            unit = o.optString("unit", "mg"),
                            cost = o.optDouble("cost", 0.0),
                            roaName = o.optString("roaName", "Oral"),
                            bioavailabilityFactor = o.optDouble("bioavailabilityFactor", 1.0),
                            onsetMultiplier = o.optDouble("onsetMultiplier", 1.0),
                            durationMultiplier = o.optDouble("durationMultiplier", 1.0),
                            evaluatedTier = o.optString("evaluatedTier", "MEDIUM"),
                            stackedPeakTier = o.optString("stackedPeakTier", "MEDIUM"),
                            contextTag = o.optString("contextTag", "Focus"),
                            subjectiveRating = o.optInt("subjectiveRating", 8),
                            notes = o.optString("notes", ""),
                            customFieldsMapStr = o.optString("customFieldsMapStr", "")
                        )
                    )
                }
                intakeLogDao.insertLogs(parsedLogs)
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun exportLogsCsv(): String = withContext(Dispatchers.IO) {
        val substances = substanceDao.getAllSubstancesOnce().associateBy { it.id }
        val logs = intakeLogDao.getAllLogsOnce()
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val sb = StringBuilder()
        sb.appendLine("id,timestamp_iso,substance,subtype,potency_mult,dose_amount,unit,cost,roa,single_tier,stacked_tier,context_tag,rating,custom_fields,notes")
        for (l in logs) {
            val subName = substances[l.substanceId]?.name ?: "Unknown"
            val iso = sdf.format(Date(l.timestampMillis))
            val safeNotes = l.notes.replace("\"", "'").replace(",", ";")
            val safeFields = l.customFieldsMapStr.replace(",", "|")
            sb.appendLine("${l.id},$iso,$subName,\"${l.subtypeName}\",${l.potencyMultiplier},${l.doseAmount},${l.unit},${l.cost},${l.roaName},${l.evaluatedTier},${l.stackedPeakTier},${l.contextTag},${l.subjectiveRating},\"$safeFields\",\"$safeNotes\"")
        }
        sb.toString()
    }
}
