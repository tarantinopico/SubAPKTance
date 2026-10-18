package com.antigravity.substance.domain.engine

import com.antigravity.substance.data.db.SubstanceDatabase
import com.antigravity.substance.data.db.entities.IntakeLogEntity
import com.antigravity.substance.data.db.entities.RouteOfAdminEntity
import com.antigravity.substance.data.preferences.UserSettings
import com.antigravity.substance.domain.model.DoseTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PharmacokineticEngineTest {

    private val caffeine = SubstanceDatabase.defaultSubstances().first { it.name == "Caffeine" }

    @Test
    fun singleDoseCurve_startsAtZero_climbsToPeak_andDecaysToZero() {
        val dose = 100.0
        val onset = caffeine.onsetMinutes.toDouble()
        val comeup = caffeine.comeupMinutes.toDouble()
        val peak = caffeine.peakMinutes.toDouble()
        val offset = caffeine.offsetMinutes.toDouble()
        val total = onset + comeup + peak + offset

        assertEquals(0.0, PharmacokineticEngine.singleDoseEffectAt(-5.0, dose, onset, comeup, peak, offset), 0.001)
        assertEquals(0.0, PharmacokineticEngine.singleDoseEffectAt(0.0, dose, onset, comeup, peak, offset), 0.001)

        val atOnsetEnd = PharmacokineticEngine.singleDoseEffectAt(onset, dose, onset, comeup, peak, offset)
        assertEquals(15.0, atOnsetEnd, 0.5)

        val midPeakTime = onset + comeup + 0.5 * peak
        val atPeakMid = PharmacokineticEngine.singleDoseEffectAt(midPeakTime, dose, onset, comeup, peak, offset)
        assertEquals(100.0, atPeakMid, 0.5)

        val midOffsetTime = onset + comeup + peak + 0.5 * offset
        val atOffsetMid = PharmacokineticEngine.singleDoseEffectAt(midOffsetTime, dose, onset, comeup, peak, offset)
        assertTrue("Offset midpoint should be between 10 and 75 mg, was $atOffsetMid", atOffsetMid in 10.0..75.0)

        val afterTotal = PharmacokineticEngine.singleDoseEffectAt(total + 1.0, dose, onset, comeup, peak, offset)
        assertEquals(0.0, afterTotal, 0.001)
    }

    @Test
    fun simpleVsDetailedTiming_andSeparateBodyPresence_resolveAccurately() {
        // 1. Simple mode (timingMode = "SIMPLE") auto-splits mainEffectDurationMinutes (e.g. 200m)
        val simpleSubstance = caffeine.copy(
            timingMode = "SIMPLE",
            mainEffectDurationMinutes = 200,
            hasBodyPresenceDuration = true,
            bodyPresenceHours = 24.0
        )
        val totalPhases = (
            simpleSubstance.effectiveOnsetMinutes() +
                simpleSubstance.effectiveComeupMinutes() +
                simpleSubstance.effectivePeakMinutes() +
                simpleSubstance.effectiveOffsetMinutes()
            ).toDouble()
        assertEquals(200.0, totalPhases, 2.0)

        val plasmaAt6h = PharmacokineticEngine.singleDosePlasmaAt(
            deltaMinutes = 360.0,
            effectiveDose = 100.0,
            substance = simpleSubstance,
            settings = UserSettings()
        )
        assertTrue("Expected positive body presence plasma level at 6h", plasmaAt6h > 10.0)

        // 2. When hasBodyPresenceDuration = false, singleDosePlasmaAt returns 0.0
        val noBodySub = simpleSubstance.copy(hasBodyPresenceDuration = false)
        val plasmaDisabled = PharmacokineticEngine.singleDosePlasmaAt(
            deltaMinutes = 120.0,
            effectiveDose = 100.0,
            substance = noBodySub,
            settings = UserSettings()
        )
        assertEquals(0.0, plasmaDisabled, 0.001)
    }

    @Test
    fun multiDoseSuperposition_stacksOverlappingDoses_shiftsPeak_andUpgradesTier() {
        val baseTimeMs = 1_700_000_000_000L
        val minuteMs = 60_000L

        val log1 = IntakeLogEntity(
            id = 1L,
            substanceId = caffeine.id,
            timestampMillis = baseTimeMs,
            doseAmount = 100.0,
            unit = "mg",
            cost = 22.0,
            roaName = "Oral",
            bioavailabilityFactor = 1.0,
            onsetMultiplier = 1.0,
            durationMultiplier = 1.0
        )

        val log2 = IntakeLogEntity(
            id = 2L,
            substanceId = caffeine.id,
            timestampMillis = baseTimeMs + 60 * minuteMs,
            doseAmount = 100.0,
            unit = "mg",
            cost = 22.0,
            roaName = "Oral",
            bioavailabilityFactor = 1.0,
            onsetMultiplier = 1.0,
            durationMultiplier = 1.0
        )

        val singleSession = PharmacokineticEngine.buildActiveSession(
            substance = caffeine,
            allSubstanceLogs = listOf(log1),
            nowMillis = baseTimeMs + 75 * minuteMs,
            startOfTodayMillis = baseTimeMs - 120 * minuteMs
        )
        assertNotNull(singleSession)

        val stackedSession = PharmacokineticEngine.buildActiveSession(
            substance = caffeine,
            allSubstanceLogs = listOf(log1, log2),
            nowMillis = baseTimeMs + 110 * minuteMs,
            startOfTodayMillis = baseTimeMs - 120 * minuteMs
        )
        assertNotNull(stackedSession)

        assertTrue(
            "Expected stacked peak > 175 mg, got ${stackedSession!!.combinedPeakLoad}",
            stackedSession.combinedPeakLoad > 175.0
        )

        assertTrue(
            "Expected shifted peak timestamp (${stackedSession.combinedPeakTimestampMillis}) > isolated peak (${singleSession!!.combinedPeakTimestampMillis})",
            stackedSession.combinedPeakTimestampMillis > singleSession.combinedPeakTimestampMillis
        )
        assertTrue("Expected positive peak shift minutes", stackedSession.peakShiftMinutes > 0)

        assertEquals(DoseTier.MEDIUM, singleSession.combinedPeakTier)
        assertEquals(DoseTier.STRONG, stackedSession.combinedPeakTier)
    }

    @Test
    fun previewCandidateDose_supportsSubtypePotencyAndDurationMultipliers() {
        val nowMs = 1_700_000_000_000L
        val existingLog = IntakeLogEntity(
            id = 1L,
            substanceId = caffeine.id,
            timestampMillis = nowMs - 45 * 60_000L,
            doseAmount = 110.0,
            unit = "mg",
            cost = 24.2
        )
        val oralRoa = RouteOfAdminEntity(1L, "Oral", 1.0, 1.0, 1.0, true)
        val espressoSubtype = SubstanceDatabase.defaultSubtypes().first { it.name.contains("Espresso") }

        val preview = PharmacokineticEngine.previewCandidateDose(
            substance = caffeine,
            existingLogs = listOf(existingLog),
            candidateDoseAmount = 100.0,
            roa = oralRoa,
            subtype = espressoSubtype,
            candidateTimestampMillis = nowMs
        )

        assertTrue(preview.isStackingActive)
        assertEquals(DoseTier.MEDIUM, preview.singleDoseTier)
        assertEquals(DoseTier.STRONG, preview.stackedPeakTier)
        assertTrue(preview.stackedPeakLoad > 180.0)
    }
}
