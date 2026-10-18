package com.antigravity.substance.domain.engine

import com.antigravity.substance.data.db.SubstanceDatabase
import com.antigravity.substance.data.db.entities.IntakeLogEntity
import com.antigravity.substance.data.preferences.UserSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalyticsAggregatorEngineTest {

    private val substances = SubstanceDatabase.defaultSubstances()
    private val subtypes = SubstanceDatabase.defaultSubtypes()
    private val kratom = substances.first { it.name == "Kratom" }

    @Test
    fun substanceDeepDiveReport_computesSummaryStats_subtypes_correlations_andDualPkCurves() {
        val nowMs = 1_700_050_000_000L
        val dayMs = 86_400_000L
        val hourMs = 3_600_000L

        val t1 = AnalyticsAggregatorEngine.startOfDayMillis(nowMs - 2 * dayMs) + 9 * hourMs
        val t2 = t1 + 4 * hourMs
        val t3 = AnalyticsAggregatorEngine.startOfDayMillis(nowMs - 1 * dayMs) + 9 * hourMs

        val logs = listOf(
            IntakeLogEntity(
                id = 1L,
                substanceId = kratom.id,
                subtypeId = 4L,
                subtypeName = "Green Maeng Da",
                potencyMultiplier = 1.15,
                timestampMillis = t1,
                doseAmount = 3.0,
                unit = "g",
                cost = 9.6,
                subjectiveRating = 9
            ),
            IntakeLogEntity(
                id = 2L,
                substanceId = kratom.id,
                subtypeId = 5L,
                subtypeName = "Red Bali",
                potencyMultiplier = 0.90,
                timestampMillis = t2,
                doseAmount = 2.0,
                unit = "g",
                cost = 6.4,
                subjectiveRating = 7
            ),
            IntakeLogEntity(
                id = 3L,
                substanceId = kratom.id,
                subtypeId = 4L,
                subtypeName = "Green Maeng Da",
                potencyMultiplier = 1.15,
                timestampMillis = t3,
                doseAmount = 4.0,
                unit = "g",
                cost = 12.8,
                subjectiveRating = 8
            )
        )

        val report = AnalyticsAggregatorEngine.buildSubstanceDeepDive(
            substance = kratom,
            allLogs = logs,
            timeframeDays = 10,
            bedtimeHour24 = 23,
            nowMillis = nowMs,
            allSubstances = substances,
            subtypes = subtypes,
            settings = UserSettings()
        )

        assertEquals(9.0, report.totalUnitsConsumed, 0.01)
        assertEquals(3, report.totalDosesCount)
        assertEquals(2, report.activeDaysCount)
        assertEquals(0.90, report.avgUnitsPerCalendarDay, 0.01)
        assertEquals(4.50, report.avgUnitsPerActiveDay, 0.01)
        assertEquals(28.8, report.totalSpend, 0.05)
        // Inter-dose intervals: (t2 - t1) = 4h, (t3 - t2) = 20h -> average = 12.0h
        assertEquals(12.0, report.meanIntervalHours, 0.05)

        // Verify SummaryStats on Single Dose Size (2.0, 3.0, 4.0)
        assertEquals(2.0, report.singleDoseStats.min, 0.01)
        assertEquals(3.0, report.singleDoseStats.mean, 0.01)
        assertEquals(3.0, report.singleDoseStats.median, 0.01)
        assertEquals(4.0, report.singleDoseStats.max, 0.01)

        // Verify Subtype metrics
        assertEquals(2, report.subtypeMetrics.size)
        assertEquals("Green Maeng Da", report.subtypeMetrics.first().subtypeName)
        assertEquals(7.0, report.subtypeMetrics.first().totalUnits, 0.01)

        // Verify Chronological Interval Evolution & Correlations
        assertEquals(2, report.intervalEvolution.size)
        assertEquals(2, report.prevIntervalVsDoseRegression.points.size)
        assertEquals(2, report.doseVsNextIntervalRegression.points.size)

        // Verify Dual Active Effect vs Body Presence curves
        assertTrue(report.simulated7dActiveEffectCurve.isNotEmpty())
        assertTrue(report.simulated7dPlasmaCurve.isNotEmpty())
    }

    @Test
    fun globalAnalyticsReport_populatesAllSubpagesSubtypesAndCrossCorrelationsFromDemoData() {
        val nowMs = 1_700_050_000_000L
        val demoLogs = SubstanceDatabase.buildDemoLogs(nowMs)
        val global = AnalyticsAggregatorEngine.buildGlobalReport(
            substances = substances,
            allLogs = demoLogs,
            timeframeDays = 30,
            nowMillis = nowMs,
            subtypes = subtypes,
            settings = UserSettings()
        )

        assertTrue(global.totalIntakesCount > 30)
        assertTrue(global.totalSpend > 100.0)
        assertEquals(30, global.dailyTimeline.size)
        assertTrue(global.dailyDoseCountStats.max >= global.dailyDoseCountStats.mean)
        assertTrue(global.dailySpendStats.max >= global.dailySpendStats.mean)
        assertTrue(global.spendShares.isNotEmpty())
        assertTrue(global.globalSubtypeMetrics.isNotEmpty())
        assertTrue(global.synergyPairs.isNotEmpty())
        assertTrue(global.globalIntervalEvolution.isNotEmpty())
        assertTrue(global.intervalVsNormalizedDoseRegression.points.isNotEmpty())
        assertEquals(84, global.heatmapCells.size)
        assertEquals(24, global.hourlyDistribution24.size)
        assertEquals(7, global.weekdayAvgDoses.size)
        assertTrue(global.composite48hActiveWaves.isNotEmpty())
        assertTrue(global.composite48hBodyWaves.isNotEmpty())
        assertEquals(substances.size, global.streakLeaderboard.size)
        assertTrue(global.contextMetrics.isNotEmpty())
        assertTrue(global.roaMetrics.isNotEmpty())
        assertTrue(global.velocityMetrics.isNotEmpty())
    }
}
