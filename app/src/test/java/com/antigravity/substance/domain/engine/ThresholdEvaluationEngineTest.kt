package com.antigravity.substance.domain.engine

import com.antigravity.substance.data.db.SubstanceDatabase
import com.antigravity.substance.domain.model.DoseTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThresholdEvaluationEngineTest {

    private val kratom = SubstanceDatabase.defaultSubstances().first { it.name == "Kratom" }

    @Test
    fun evaluateTier_correctlyClassifiesAllFiveTiersAndInactive() {
        // Kratom boundaries: Negligible <= 1.0, Light <= 2.5, Medium <= 4.5, Strong <= 6.8, Dangerous > 6.8
        assertEquals(DoseTier.INACTIVE, ThresholdEvaluationEngine.evaluateTier(0.0, kratom))
        assertEquals(DoseTier.NEGLIGIBLE, ThresholdEvaluationEngine.evaluateTier(0.8, kratom))
        assertEquals(DoseTier.LIGHT, ThresholdEvaluationEngine.evaluateTier(2.0, kratom))
        assertEquals(DoseTier.MEDIUM, ThresholdEvaluationEngine.evaluateTier(3.5, kratom))
        assertEquals(DoseTier.STRONG, ThresholdEvaluationEngine.evaluateTier(5.5, kratom))
        assertEquals(DoseTier.DANGEROUS, ThresholdEvaluationEngine.evaluateTier(7.5, kratom))
    }

    @Test
    fun computeSpectrumFraction_monotonicallyIncreasesAcrossBoundaries() {
        val f0 = ThresholdEvaluationEngine.computeSpectrumFraction(0.0, kratom)
        val fNeg = ThresholdEvaluationEngine.computeSpectrumFraction(kratom.thresholdNegligible, kratom)
        val fLight = ThresholdEvaluationEngine.computeSpectrumFraction(kratom.thresholdLight, kratom)
        val fMed = ThresholdEvaluationEngine.computeSpectrumFraction(kratom.thresholdMedium, kratom)
        val fStrong = ThresholdEvaluationEngine.computeSpectrumFraction(kratom.thresholdStrong, kratom)
        val fDanger = ThresholdEvaluationEngine.computeSpectrumFraction(kratom.thresholdDangerous, kratom)

        assertEquals(0.0f, f0, 0.001f)
        assertEquals(0.20f, fNeg, 0.001f)
        assertEquals(0.40f, fLight, 0.001f)
        assertEquals(0.60f, fMed, 0.001f)
        assertEquals(0.80f, fStrong, 0.001f)
        assertEquals(1.00f, fDanger, 0.001f)
        assertTrue(fNeg < fLight && fLight < fMed && fMed < fStrong && fStrong < fDanger)
    }
}
