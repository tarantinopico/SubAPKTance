package com.antigravity.substance.data.db.entities

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.math.roundToInt

@Immutable
@Entity(tableName = "substances")
data class SubstanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val subtitle: String,
    val category: String,
    val iconName: String,
    val colorHex: Long,
    val defaultUnit: String,
    val stepSize: Double,
    val defaultDose: Double,
    val quickPresetsCsv: String,
    val costPerUnit: Double,
    // 5-Tier Thresholds:
    val thresholdNegligible: Double,
    val thresholdLight: Double,
    val thresholdMedium: Double,
    val thresholdStrong: Double,
    val thresholdDangerous: Double,
    val dailyMaxSafeLimit: Double,
    val recommendedMinIntervalMinutes: Int,
    // Timing & Pharmacokinetic Configuration:
    // timingMode: "SIMPLE" (uses mainEffectDurationMinutes), "DETAILED" (uses 4 phases), "NONE" (timing disabled / optional)
    val timingMode: String = "DETAILED",
    val mainEffectDurationMinutes: Int = 240, // Primary duration of noticeable effects
    val onsetMinutes: Int = 15,
    val comeupMinutes: Int = 30,
    val peakMinutes: Int = 90,
    val offsetMinutes: Int = 105,
    // Separate Optional Body Presence / Elimination Duration:
    val hasBodyPresenceDuration: Boolean = true,
    val bodyPresenceHours: Double = 12.0, // How long it stays in the body (separate from active effect!)
    val halfLifeHours: Double = 4.0,
    val sleepDisruptionThreshold: Double = 30.0,
    val stackingBehavior: String = "FULL_SUPERPOSITION", // FULL_SUPERPOSITION, DAMPED_STACKING, INDEPENDENT_TIMERS
    // Per-Substance Tunable Computational Parameters:
    val toleranceGainRate: Double = 1.0,
    val toleranceDecayRate: Double = 1.0,
    val bioLoadWeight: Double = 1.0,
    val superpositionWeight: Double = 1.0,
    val isFavorite: Boolean = true,
    val isArchived: Boolean = false,
    val sortOrder: Int = 0
) {
    val isTimingEnabled: Boolean
        get() = !timingMode.equals("NONE", ignoreCase = true)

    val totalDurationMinutes: Int
        get() = when (timingMode.uppercase()) {
            "NONE" -> 0
            "SIMPLE" -> mainEffectDurationMinutes.coerceAtLeast(15)
            else -> (onsetMinutes + comeupMinutes + peakMinutes + offsetMinutes).coerceAtLeast(15)
        }

    fun effectiveOnsetMinutes(simpleOnsetRatio: Double = 0.10): Int =
        if (timingMode == "SIMPLE") (mainEffectDurationMinutes * simpleOnsetRatio).roundToInt().coerceAtLeast(1)
        else onsetMinutes.coerceAtLeast(1)

    fun effectiveComeupMinutes(simpleComeupRatio: Double = 0.18): Int =
        if (timingMode == "SIMPLE") (mainEffectDurationMinutes * simpleComeupRatio).roundToInt().coerceAtLeast(1)
        else comeupMinutes.coerceAtLeast(1)

    fun effectivePeakMinutes(simplePeakRatio: Double = 0.37): Int =
        if (timingMode == "SIMPLE") (mainEffectDurationMinutes * simplePeakRatio).roundToInt().coerceAtLeast(1)
        else peakMinutes.coerceAtLeast(1)

    fun effectiveOffsetMinutes(simpleOffsetRatio: Double = 0.35): Int =
        if (timingMode == "SIMPLE") (mainEffectDurationMinutes * simpleOffsetRatio).roundToInt().coerceAtLeast(1)
        else offsetMinutes.coerceAtLeast(1)

    fun effectiveBodyPresenceMinutes(): Int =
        if (hasBodyPresenceDuration) (bodyPresenceHours * 60.0).roundToInt().coerceAtLeast(totalDurationMinutes)
        else totalDurationMinutes

    fun parsedQuickPresets(): List<Double> =
        quickPresetsCsv.split(",")
            .mapNotNull { it.trim().toDoubleOrNull() }
            .filter { it > 0.0 }
}
