package com.antigravity.substance.domain.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.antigravity.substance.data.db.entities.IntakeLogEntity
import com.antigravity.substance.data.db.entities.SubstanceEntity

enum class DoseTier(
    val titleEn: String,
    val titleCz: String,
    val primaryColorHex: Long,
    val secondaryColorHex: Long,
    val ambientAlpha: Float,
    val pulseDurationMs: Int
) {
    INACTIVE(
        titleEn = "Inactive",
        titleCz = "Neaktivní",
        primaryColorHex = 0xFF0F172AL,
        secondaryColorHex = 0xFF1E293BL,
        ambientAlpha = 0.15f,
        pulseDurationMs = 4200
    ),
    NEGLIGIBLE(
        titleEn = "Negligible",
        titleCz = "Zanedbatelná",
        primaryColorHex = 0xFF38BDF8L,
        secondaryColorHex = 0xFF0284C7L,
        ambientAlpha = 0.26f,
        pulseDurationMs = 3600
    ),
    LIGHT(
        titleEn = "Light",
        titleCz = "Malá",
        primaryColorHex = 0xFF10B981L,
        secondaryColorHex = 0xFF059669L,
        ambientAlpha = 0.38f,
        pulseDurationMs = 3000
    ),
    MEDIUM(
        titleEn = "Common",
        titleCz = "Střední",
        primaryColorHex = 0xFFF59E0BL,
        secondaryColorHex = 0xFFD97706L,
        ambientAlpha = 0.50f,
        pulseDurationMs = 2400
    ),
    STRONG(
        titleEn = "Strong",
        titleCz = "Velká",
        primaryColorHex = 0xFFF97316L,
        secondaryColorHex = 0xFFEA580CL,
        ambientAlpha = 0.65f,
        pulseDurationMs = 1650
    ),
    DANGEROUS(
        titleEn = "Redline",
        titleCz = "Nebezpečná",
        primaryColorHex = 0xFFEF4444L,
        secondaryColorHex = 0xFFEC4899L,
        ambientAlpha = 0.82f,
        pulseDurationMs = 950
    );

    val primaryColor: Color get() = Color(primaryColorHex)
    val secondaryColor: Color get() = Color(secondaryColorHex)

    companion object {
        fun fromName(name: String): DoseTier =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: MEDIUM
    }
}

enum class PkPhase(val label: String) {
    INACTIVE("Baseline"),
    ONSET_COUNTDOWN("Onset Countdown"),
    CLIMBING_TO_PEAK("Climbing to Peak"),
    PEAKING_NOW("Peaking Now"),
    OFFSET_COMEDOWN("Offset Comedown"),
    RESIDUAL_HALF_LIFE("Residual Half-Life")
}

@Immutable
data class PkCurvePoint(
    val timestampMillis: Long,
    val relativeMinutesFromNow: Float,
    val activeEffectLoad: Double,
    val plasmaConcentration: Double
)

@Immutable
data class ActiveSubstanceSession(
    val substance: SubstanceEntity,
    val activeLogs: List<IntakeLogEntity>,
    val currentActiveLoad: Double,
    val currentPlasmaLoad: Double,
    val combinedPeakLoad: Double,
    val combinedPeakTimestampMillis: Long,
    val isolatedFirstPeakTimestampMillis: Long,
    val peakShiftMinutes: Int,
    val currentTier: DoseTier,
    val combinedPeakTier: DoseTier,
    val currentPhase: PkPhase,
    val phaseCountdownSeconds: Long,
    val phaseTargetTimestampMillis: Long,
    val activeEffectsEndTimestampMillis: Long,
    val halfLifeClearTimestampMillis: Long,
    val totalIngestedTodayUnits: Double,
    val curvePoints: List<PkCurvePoint>,
    val doseTimestampsMillis: List<Long>
)

@Immutable
data class ProjectedDosePreview(
    val singleEffectiveDose: Double,
    val singleDoseTier: DoseTier,
    val singleSpectrumFraction: Float,
    val existingActiveLoadAtTarget: Double,
    val stackedPeakLoad: Double,
    val stackedPeakTier: DoseTier,
    val stackedSpectrumFraction: Float,
    val isStackingActive: Boolean,
    val peakShiftMinutes: Int,
    val onsetTimestampMillis: Long,
    val combinedPeakTimestampMillis: Long,
    val activeClearTimestampMillis: Long,
    val estimatedCost: Double,
    val previewCurvePoints: List<PkCurvePoint>
)
