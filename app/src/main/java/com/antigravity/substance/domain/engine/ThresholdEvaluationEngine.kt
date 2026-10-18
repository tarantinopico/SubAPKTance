package com.antigravity.substance.domain.engine

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.antigravity.substance.data.db.entities.SubstanceEntity
import com.antigravity.substance.domain.model.DoseTier

object ThresholdEvaluationEngine {

    /**
     * Evaluates a dose or combined active equivalent load (in the substance's units)
     * into one of the 5 active DoseTiers (or INACTIVE if <= 0.001).
     */
    fun evaluateTier(amount: Double, substance: SubstanceEntity): DoseTier {
        if (amount <= 0.001) return DoseTier.INACTIVE
        return when {
            amount <= substance.thresholdNegligible -> DoseTier.NEGLIGIBLE
            amount <= substance.thresholdLight -> DoseTier.LIGHT
            amount <= substance.thresholdMedium -> DoseTier.MEDIUM
            amount <= substance.thresholdStrong -> DoseTier.STRONG
            else -> DoseTier.DANGEROUS
        }
    }

    /**
     * Maps a dose amount to a normalized 0f..1f position across the 5 equal visual zones
     * on the 5-Zone Threshold Spectrum Bar:
     * [0.00 .. 0.20] -> Negligible (0 .. thresholdNegligible)
     * [0.20 .. 0.40] -> Light (thresholdNegligible .. thresholdLight)
     * [0.40 .. 0.60] -> Medium (thresholdLight .. thresholdMedium)
     * [0.60 .. 0.80] -> Strong (thresholdMedium .. thresholdStrong)
     * [0.80 .. 1.00] -> Dangerous (thresholdStrong .. thresholdDangerous+)
     */
    fun computeSpectrumFraction(amount: Double, substance: SubstanceEntity): Float {
        if (amount <= 0.0) return 0f
        val n = substance.thresholdNegligible.coerceAtLeast(0.01)
        val l = substance.thresholdLight.coerceAtLeast(n + 0.01)
        val m = substance.thresholdMedium.coerceAtLeast(l + 0.01)
        val s = substance.thresholdStrong.coerceAtLeast(m + 0.01)
        val d = substance.thresholdDangerous.coerceAtLeast(s + 0.01)

        val fraction = when {
            amount <= n -> 0.20 * (amount / n)
            amount <= l -> 0.20 + 0.20 * ((amount - n) / (l - n))
            amount <= m -> 0.40 + 0.20 * ((amount - l) / (m - l))
            amount <= s -> 0.60 + 0.20 * ((amount - m) / (s - m))
            else -> 0.80 + 0.20 * ((amount - s) / (d - s)).coerceAtMost(1.0)
        }
        return fraction.toFloat().coerceIn(0f, 1f)
    }

    /**
     * Smoothly interpolates the ambient glow color across the 5-zone spectrum.
     */
    fun interpolateSpectrumColor(amount: Double, substance: SubstanceEntity): Color {
        val f = computeSpectrumFraction(amount, substance)
        return when {
            f <= 0.01f -> DoseTier.INACTIVE.primaryColor
            f <= 0.20f -> lerp(DoseTier.NEGLIGIBLE.primaryColor, DoseTier.LIGHT.primaryColor, (f / 0.20f).coerceIn(0f, 1f) * 0.4f)
            f <= 0.40f -> lerp(DoseTier.NEGLIGIBLE.primaryColor, DoseTier.LIGHT.primaryColor, 0.4f + ((f - 0.20f) / 0.20f) * 0.6f)
            f <= 0.60f -> lerp(DoseTier.LIGHT.primaryColor, DoseTier.MEDIUM.primaryColor, (f - 0.40f) / 0.20f)
            f <= 0.80f -> lerp(DoseTier.MEDIUM.primaryColor, DoseTier.STRONG.primaryColor, (f - 0.60f) / 0.20f)
            else -> lerp(DoseTier.STRONG.primaryColor, DoseTier.DANGEROUS.primaryColor, ((f - 0.80f) / 0.20f).coerceIn(0f, 1f))
        }
    }
}
