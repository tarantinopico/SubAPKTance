package com.antigravity.substance.data.preferences

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale

private val Context.substanceDataStore: DataStore<Preferences> by preferencesDataStore(name = "substance_user_settings")

@Immutable
data class UserSettings(
    val currencySymbol: String = "Kč",
    val currencyPrefix: Boolean = false,
    val decimalPrecision: Int = 1,
    val use24HourFormat: Boolean = true,
    val bedtimeHour24: Int = 23,
    // Professional Theme, Light Mode & Lightweight Mode
    val isLightMode: Boolean = false,
    val isLightweightMode: Boolean = false, // 1-tap disable all glows, animations, translucency
    val showChartMinMaxAvgLines: Boolean = true,
    val glassOpacity: Float = 0.88f,
    val specularBorderIntensity: Float = 0.16f,
    val ambientGlowIntensity: Float = 0.35f,
    val glowByStackedActiveLoad: Boolean = true,
    val darkThemePreset: String = "PROFESSIONAL_SLATE", // PROFESSIONAL_SLATE, PITCH_BLACK_AMOLED, IOS_ABYSSAL_GLASS, CLINICAL_LIGHT
    val showCostOnDashboard: Boolean = false,
    val customUnitsCsv: String = "mg,g,mcg,ml,L,IU,pouches,puffs,shots,drinks,pills,drops",
    val contextTagsCsv: String = "Focus,Work,Gym,Study,Social,Morning,Recovery,Gaming",
    // Global Computational & Pharmacokinetic Constants (All user-editable!)
    val globalToleranceGainFactor: Double = 16.5,
    val globalToleranceDecayPerDay: Double = 0.18,
    val globalSuperpositionFactor: Double = 1.0,
    val simpleOnsetRatio: Double = 0.10,
    val simpleComeupRatio: Double = 0.18,
    val simplePeakRatio: Double = 0.37,
    val simpleOffsetRatio: Double = 0.35,
    val compulsiveRedoseThresholdHours: Double = 2.5,
    val safetyScoreBreachPenalty: Int = 5,
    val safetyScoreRedlinePenalty: Int = 6,
    val bodyEliminationHalfLifeMultiplier: Double = 4.5,
    val isInitialSeedDone: Boolean = false
) {
    fun formatCurrency(amount: Double): String {
        val formatted = String.format(Locale.US, "%.${decimalPrecision.coerceIn(0, 2)}f", amount)
        return if (currencyPrefix) "$currencySymbol$formatted" else "$formatted $currencySymbol"
    }

    fun formatDose(amount: Double, unit: String = ""): String {
        val str = if (amount % 1.0 == 0.0) {
            amount.toInt().toString()
        } else {
            String.format(Locale.US, "%.2f", amount).trimEnd('0').trimEnd('.')
        }
        return if (unit.isBlank()) str else "$str $unit"
    }

    fun parsedUnits(): List<String> =
        customUnitsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.distinct()

    fun parsedContextTags(): List<String> =
        contextTagsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.distinct()
}

class UserSettingsStore(private val context: Context) {
    private object Keys {
        val CURRENCY_SYMBOL = stringPreferencesKey("currency_symbol")
        val CURRENCY_PREFIX = booleanPreferencesKey("currency_prefix")
        val DECIMAL_PRECISION = intPreferencesKey("decimal_precision")
        val USE_24H = booleanPreferencesKey("use_24h")
        val BEDTIME_HOUR = intPreferencesKey("bedtime_hour")
        val IS_LIGHT_MODE = booleanPreferencesKey("is_light_mode")
        val IS_LIGHTWEIGHT_MODE = booleanPreferencesKey("is_lightweight_mode")
        val SHOW_CHART_MIN_MAX_AVG = booleanPreferencesKey("show_chart_min_max_avg")
        val GLASS_OPACITY = floatPreferencesKey("glass_opacity")
        val SPECULAR_INTENSITY = floatPreferencesKey("specular_intensity")
        val GLOW_INTENSITY = floatPreferencesKey("glow_intensity")
        val GLOW_BY_STACKED = booleanPreferencesKey("glow_by_stacked")
        val DARK_THEME_PRESET = stringPreferencesKey("dark_theme_preset")
        val SHOW_COST_ON_DASH = booleanPreferencesKey("show_cost_on_dash")
        val CUSTOM_UNITS_CSV = stringPreferencesKey("custom_units_csv")
        val CONTEXT_TAGS_CSV = stringPreferencesKey("context_tags_csv")
        // Global Math Constants
        val GLOBAL_TOL_GAIN = doublePreferencesKey("global_tol_gain")
        val GLOBAL_TOL_DECAY = doublePreferencesKey("global_tol_decay")
        val GLOBAL_SUPERPOSITION = doublePreferencesKey("global_superposition")
        val SIMPLE_ONSET_RATIO = doublePreferencesKey("simple_onset_ratio")
        val SIMPLE_COMEUP_RATIO = doublePreferencesKey("simple_comeup_ratio")
        val SIMPLE_PEAK_RATIO = doublePreferencesKey("simple_peak_ratio")
        val SIMPLE_OFFSET_RATIO = doublePreferencesKey("simple_offset_ratio")
        val COMPULSIVE_REDOSE_HRS = doublePreferencesKey("compulsive_redose_hrs")
        val SAFETY_BREACH_PENALTY = intPreferencesKey("safety_breach_penalty")
        val SAFETY_REDLINE_PENALTY = intPreferencesKey("safety_redline_penalty")
        val BODY_ELIM_HL_MULT = doublePreferencesKey("body_elim_hl_mult")
        val INITIAL_SEED_DONE = booleanPreferencesKey("initial_seed_done")
    }

    val settingsFlow: Flow<UserSettings> = context.substanceDataStore.data.map { prefs ->
        UserSettings(
            currencySymbol = prefs[Keys.CURRENCY_SYMBOL] ?: "Kč",
            currencyPrefix = prefs[Keys.CURRENCY_PREFIX] ?: false,
            decimalPrecision = prefs[Keys.DECIMAL_PRECISION] ?: 1,
            use24HourFormat = prefs[Keys.USE_24H] ?: true,
            bedtimeHour24 = prefs[Keys.BEDTIME_HOUR] ?: 23,
            isLightMode = prefs[Keys.IS_LIGHT_MODE] ?: false,
            isLightweightMode = prefs[Keys.IS_LIGHTWEIGHT_MODE] ?: false,
            showChartMinMaxAvgLines = prefs[Keys.SHOW_CHART_MIN_MAX_AVG] ?: true,
            glassOpacity = prefs[Keys.GLASS_OPACITY] ?: 0.88f,
            specularBorderIntensity = prefs[Keys.SPECULAR_INTENSITY] ?: 0.16f,
            ambientGlowIntensity = prefs[Keys.GLOW_INTENSITY] ?: 0.35f,
            glowByStackedActiveLoad = prefs[Keys.GLOW_BY_STACKED] ?: true,
            darkThemePreset = prefs[Keys.DARK_THEME_PRESET] ?: "PROFESSIONAL_SLATE",
            showCostOnDashboard = prefs[Keys.SHOW_COST_ON_DASH] ?: false,
            customUnitsCsv = prefs[Keys.CUSTOM_UNITS_CSV]
                ?: "mg,g,mcg,ml,L,IU,pouches,puffs,shots,drinks,pills,drops",
            contextTagsCsv = prefs[Keys.CONTEXT_TAGS_CSV]
                ?: "Focus,Work,Gym,Study,Social,Morning,Recovery,Gaming",
            globalToleranceGainFactor = prefs[Keys.GLOBAL_TOL_GAIN] ?: 16.5,
            globalToleranceDecayPerDay = prefs[Keys.GLOBAL_TOL_DECAY] ?: 0.18,
            globalSuperpositionFactor = prefs[Keys.GLOBAL_SUPERPOSITION] ?: 1.0,
            simpleOnsetRatio = prefs[Keys.SIMPLE_ONSET_RATIO] ?: 0.10,
            simpleComeupRatio = prefs[Keys.SIMPLE_COMEUP_RATIO] ?: 0.18,
            simplePeakRatio = prefs[Keys.SIMPLE_PEAK_RATIO] ?: 0.37,
            simpleOffsetRatio = prefs[Keys.SIMPLE_OFFSET_RATIO] ?: 0.35,
            compulsiveRedoseThresholdHours = prefs[Keys.COMPULSIVE_REDOSE_HRS] ?: 2.5,
            safetyScoreBreachPenalty = prefs[Keys.SAFETY_BREACH_PENALTY] ?: 5,
            safetyScoreRedlinePenalty = prefs[Keys.SAFETY_REDLINE_PENALTY] ?: 6,
            bodyEliminationHalfLifeMultiplier = prefs[Keys.BODY_ELIM_HL_MULT] ?: 4.5,
            isInitialSeedDone = prefs[Keys.INITIAL_SEED_DONE] ?: false
        )
    }

    suspend fun updateCurrency(symbol: String, prefix: Boolean, precision: Int = 1) {
        context.substanceDataStore.edit { prefs ->
            prefs[Keys.CURRENCY_SYMBOL] = symbol.ifBlank { "Kč" }
            prefs[Keys.CURRENCY_PREFIX] = prefix
            prefs[Keys.DECIMAL_PRECISION] = precision.coerceIn(0, 2)
        }
    }

    suspend fun updateBedtimeHour(hour24: Int) {
        context.substanceDataStore.edit { prefs ->
            prefs[Keys.BEDTIME_HOUR] = hour24.coerceIn(0, 23)
        }
    }

    suspend fun updateVisualTheme(
        glassOpacity: Float? = null,
        specularIntensity: Float? = null,
        glowIntensity: Float? = null,
        glowByStacked: Boolean? = null,
        themePreset: String? = null,
        isLightMode: Boolean? = null,
        isLightweightMode: Boolean? = null,
        showChartMinMaxAvgLines: Boolean? = null
    ) {
        context.substanceDataStore.edit { prefs ->
            glassOpacity?.let { prefs[Keys.GLASS_OPACITY] = it.coerceIn(0.40f, 1.0f) }
            specularIntensity?.let { prefs[Keys.SPECULAR_INTENSITY] = it.coerceIn(0.0f, 0.55f) }
            glowIntensity?.let { prefs[Keys.GLOW_INTENSITY] = it.coerceIn(0.0f, 1.0f) }
            glowByStacked?.let { prefs[Keys.GLOW_BY_STACKED] = it }
            themePreset?.let { prefs[Keys.DARK_THEME_PRESET] = it }
            isLightMode?.let { prefs[Keys.IS_LIGHT_MODE] = it }
            isLightweightMode?.let { prefs[Keys.IS_LIGHTWEIGHT_MODE] = it }
            showChartMinMaxAvgLines?.let { prefs[Keys.SHOW_CHART_MIN_MAX_AVG] = it }
        }
    }

    suspend fun updateCalculationConstants(
        toleranceGainFactor: Double? = null,
        toleranceDecayPerDay: Double? = null,
        superpositionFactor: Double? = null,
        simpleOnsetRatio: Double? = null,
        simpleComeupRatio: Double? = null,
        simplePeakRatio: Double? = null,
        simpleOffsetRatio: Double? = null,
        compulsiveRedoseHours: Double? = null,
        safetyBreachPenalty: Int? = null,
        safetyRedlinePenalty: Int? = null,
        bodyElimHalfLifeMult: Double? = null
    ) {
        context.substanceDataStore.edit { prefs ->
            toleranceGainFactor?.let { prefs[Keys.GLOBAL_TOL_GAIN] = it.coerceIn(2.0, 45.0) }
            toleranceDecayPerDay?.let { prefs[Keys.GLOBAL_TOL_DECAY] = it.coerceIn(0.02, 0.60) }
            superpositionFactor?.let { prefs[Keys.GLOBAL_SUPERPOSITION] = it.coerceIn(0.30, 1.50) }
            simpleOnsetRatio?.let { prefs[Keys.SIMPLE_ONSET_RATIO] = it.coerceIn(0.03, 0.35) }
            simpleComeupRatio?.let { prefs[Keys.SIMPLE_COMEUP_RATIO] = it.coerceIn(0.05, 0.40) }
            simplePeakRatio?.let { prefs[Keys.SIMPLE_PEAK_RATIO] = it.coerceIn(0.15, 0.65) }
            simpleOffsetRatio?.let { prefs[Keys.SIMPLE_OFFSET_RATIO] = it.coerceIn(0.15, 0.65) }
            compulsiveRedoseHours?.let { prefs[Keys.COMPULSIVE_REDOSE_HRS] = it.coerceIn(0.5, 12.0) }
            safetyBreachPenalty?.let { prefs[Keys.SAFETY_BREACH_PENALTY] = it.coerceIn(1, 25) }
            safetyRedlinePenalty?.let { prefs[Keys.SAFETY_REDLINE_PENALTY] = it.coerceIn(1, 25) }
            bodyElimHalfLifeMult?.let { prefs[Keys.BODY_ELIM_HL_MULT] = it.coerceIn(2.0, 8.0) }
        }
    }

    suspend fun resetCalculationConstantsToDefault() {
        context.substanceDataStore.edit { prefs ->
            prefs[Keys.GLOBAL_TOL_GAIN] = 16.5
            prefs[Keys.GLOBAL_TOL_DECAY] = 0.18
            prefs[Keys.GLOBAL_SUPERPOSITION] = 1.0
            prefs[Keys.SIMPLE_ONSET_RATIO] = 0.10
            prefs[Keys.SIMPLE_COMEUP_RATIO] = 0.18
            prefs[Keys.SIMPLE_PEAK_RATIO] = 0.37
            prefs[Keys.SIMPLE_OFFSET_RATIO] = 0.35
            prefs[Keys.COMPULSIVE_REDOSE_HRS] = 2.5
            prefs[Keys.SAFETY_BREACH_PENALTY] = 5
            prefs[Keys.SAFETY_REDLINE_PENALTY] = 6
            prefs[Keys.BODY_ELIM_HL_MULT] = 4.5
        }
    }

    suspend fun toggleDashboardCostCard(showCost: Boolean) {
        context.substanceDataStore.edit { prefs ->
            prefs[Keys.SHOW_COST_ON_DASH] = showCost
        }
    }

    suspend fun addCustomUnit(unit: String) {
        val clean = unit.trim().replace(",", "")
        if (clean.isBlank()) return
        context.substanceDataStore.edit { prefs ->
            val current = (prefs[Keys.CUSTOM_UNITS_CSV]
                ?: "mg,g,mcg,ml,L,IU,pouches,puffs,shots,drinks,pills,drops")
                .split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
            if (!current.contains(clean)) {
                current.add(clean)
                prefs[Keys.CUSTOM_UNITS_CSV] = current.joinToString(",")
            }
        }
    }

    suspend fun removeCustomUnit(unit: String) {
        context.substanceDataStore.edit { prefs ->
            val current = (prefs[Keys.CUSTOM_UNITS_CSV]
                ?: "mg,g,mcg,ml,L,IU,pouches,puffs,shots,drinks,pills,drops")
                .split(",").map { it.trim() }.filter { it.isNotEmpty() && it != unit.trim() }
            if (current.isNotEmpty()) {
                prefs[Keys.CUSTOM_UNITS_CSV] = current.joinToString(",")
            }
        }
    }

    suspend fun addContextTag(tag: String) {
        val clean = tag.trim().replace(",", "")
        if (clean.isBlank()) return
        context.substanceDataStore.edit { prefs ->
            val current = (prefs[Keys.CONTEXT_TAGS_CSV]
                ?: "Focus,Work,Gym,Study,Social,Morning,Recovery,Gaming")
                .split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
            if (!current.contains(clean)) {
                current.add(clean)
                prefs[Keys.CONTEXT_TAGS_CSV] = current.joinToString(",")
            }
        }
    }

    suspend fun removeContextTag(tag: String) {
        context.substanceDataStore.edit { prefs ->
            val current = (prefs[Keys.CONTEXT_TAGS_CSV]
                ?: "Focus,Work,Gym,Study,Social,Morning,Recovery,Gaming")
                .split(",").map { it.trim() }.filter { it.isNotEmpty() && it != tag.trim() }
            if (current.isNotEmpty()) {
                prefs[Keys.CONTEXT_TAGS_CSV] = current.joinToString(",")
            }
        }
    }

    suspend fun markInitialSeedDone() {
        context.substanceDataStore.edit { prefs ->
            prefs[Keys.INITIAL_SEED_DONE] = true
        }
    }
}
