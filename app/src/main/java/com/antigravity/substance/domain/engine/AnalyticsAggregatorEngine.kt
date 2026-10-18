package com.antigravity.substance.domain.engine

import androidx.compose.runtime.Immutable
import com.antigravity.substance.data.db.entities.IntakeLogEntity
import com.antigravity.substance.data.db.entities.SubstanceEntity
import com.antigravity.substance.data.db.entities.SubstanceSubtypeEntity
import com.antigravity.substance.data.preferences.UserSettings
import com.antigravity.substance.domain.model.DoseTier
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

@Immutable
data class SummaryStats(
    val count: Int = 0,
    val min: Double = 0.0,
    val max: Double = 0.0,
    val mean: Double = 0.0,
    val median: Double = 0.0,
    val p25: Double = 0.0,
    val p90: Double = 0.0,
    val stdDev: Double = 0.0,
    val sum: Double = 0.0
)

@Immutable
data class ScatterDataPoint(
    val x: Float,
    val y: Float,
    val label: String,
    val secondaryLabel: String = "",
    val colorHex: Long = 0xFF06B6D4L,
    val timestampMillis: Long = 0L
)

@Immutable
data class LinearRegressionResult(
    val points: List<ScatterDataPoint>,
    val pearsonR: Double,
    val rSquared: Double,
    val slope: Double,
    val intercept: Double,
    val xStats: SummaryStats,
    val yStats: SummaryStats,
    val interpretationCz: String
)

@Immutable
data class SubtypeAnalyticsMetric(
    val subtypeId: Long?,
    val subtypeName: String,
    val parentSubstanceId: Long,
    val parentSubstanceName: String,
    val unit: String,
    val colorHex: Long,
    val potencyMultiplier: Double,
    val doseCount: Int,
    val shareOfUnitsFraction: Float,
    val shareOfCountFraction: Float,
    val totalUnits: Double,
    val totalEffectiveUnits: Double,
    val totalSpend: Double,
    val avgCostPerDose: Double,
    val avgRating: Double,
    val doseStats: SummaryStats
)

@Immutable
data class DailySubtypeStackedPoint(
    val dayIndex: Int,
    val label: String,
    val dayStartMillis: Long,
    val perSubtypeUnits: Map<String, Double>,
    val totalUnits: Double
)

@Immutable
data class IntervalEvolutionPoint(
    val index: Int,
    val timestampMillis: Long,
    val dateLabel: String,
    val intervalHours: Float,
    val rollingAvgHours: Float,
    val doseAmountAfterGap: Double,
    val subtypeName: String
)

@Immutable
data class DailyStackedPoint(
    val dayIndex: Int,
    val label: String,
    val dayStartMillis: Long,
    val perSubstanceCount: Map<Long, Int>,
    val perSubstanceNormalizedLoad: Map<Long, Float>,
    val perSubstanceUnits: Map<Long, Double>,
    val totalSpend: Double,
    val breachedLimit: Boolean
)

@Immutable
data class SubstanceSpendShare(
    val substance: SubstanceEntity,
    val totalSpend: Double,
    val shareFraction: Float,
    val doseCount: Int,
    val totalUnits: Double
)

@Immutable
data class HeatmapDayCell(
    val dayOffsetFromToday: Int,
    val timestampMillis: Long,
    val dateLabel: String,
    val dominantSubstanceColorHex: Long,
    val dominantSubstanceName: String,
    val intensityFraction: Float,
    val doseCount: Int,
    val totalSpend: Double
)

@Immutable
data class SynergyPairCell(
    val subAId: Long,
    val subBId: Long,
    val subAName: String,
    val subBName: String,
    val subAColorHex: Long,
    val subBColorHex: Long,
    val coOccurrencePercent: Int,
    val sharedDaysCount: Int,
    val pearsonDoseCorrelation: Double // r in [-1, 1] between daily amounts of A and B
)

@Immutable
data class AbstinenceStreakItem(
    val substance: SubstanceEntity,
    val currentElapsedHours: Double,
    val longestStreakHours: Double,
    val avgGapHours: Double,
    val lastDoseAmount: Double
)

@Immutable
data class ContextTagMetric(
    val tag: String,
    val count: Int,
    val totalSpend: Double,
    val avgRating: Double,
    val shareFraction: Float
)

@Immutable
data class RoaBreakdownMetric(
    val roaName: String,
    val count: Int,
    val totalUnits: Double,
    val shareFraction: Float,
    val avgOnsetMultiplier: Double
)

@Immutable
data class VelocityMomentumMetric(
    val substance: SubstanceEntity,
    val rolling7dDailyUnits: Double,
    val rolling30dDailyUnits: Double,
    val deltaPercent: Int
)

@Immutable
data class GlobalAnalyticsReport(
    val timeframeDays: Int,
    // 1. Global KPI Hero Matrix
    val totalSpend: Double,
    val dailyAvgSpend: Double,
    val projectedMonthlySpend: Double,
    val totalIntakesCount: Int,
    val activeHoursPercentage: Float,
    val bodyPresenceHoursPercentage: Float,
    val harmReductionSafetyScore: Int,
    val dailySpendStats: SummaryStats,
    val dailyDoseCountStats: SummaryStats,
    // 2. Stacked Consumption Timeline
    val dailyTimeline: List<DailyStackedPoint>,
    // 3. Financial Expenditure Breakdown
    val spendShares: List<SubstanceSpendShare>,
    val cumulativeSpendCurve: List<Float>,
    // 4. Bio-Activity Heatmap Calendar (84 days = 12 weeks x 7 days)
    val heatmapCells: List<HeatmapDayCell>,
    // 5. 24-Hour Circadian Polar Clock
    val hourlyDistribution24: List<Int>,
    val hourlyDominantColor24: List<Long>,
    // 6. Day-of-Week Profile (Mon=0 .. Sun=6)
    val weekdayAvgDoses: List<Float>,
    val weekdayAvgSpend: List<Float>,
    val weekdayAvgLoad: List<Float>,
    // 7. Subtypes Breakdown (Across All Substances)
    val globalSubtypeMetrics: List<SubtypeAnalyticsMetric>,
    // 8. Cross-Substance Synergy & Correlation Matrix
    val synergyPairs: List<SynergyPairCell>,
    // 9. Global Correlation Scatter Plots & Regressions
    val intervalVsNormalizedDoseRegression: LinearRegressionResult,
    val firstDoseHourVsDailyLoadRegression: LinearRegressionResult,
    val dailyLoadVsSubjectiveRatingRegression: LinearRegressionResult,
    // 10. Global Inter-Dose Interval Histogram & Evolution
    val globalIntervalBuckets7: List<Int>,
    val globalIntervalStats: SummaryStats,
    val globalIntervalEvolution: List<IntervalEvolutionPoint>,
    // 11. Threshold Tier Distribution
    val tierCounts: Map<DoseTier, Int>,
    // 12. Composite 48-Hour Multi-Substance PK Wave (Active Effect vs Body Presence)
    val composite48hActiveWaves: Map<Long, List<Float>>,
    val composite48hBodyWaves: Map<Long, List<Float>>,
    // 13. Abstinence & Clean-Day Streaks
    val streakLeaderboard: List<AbstinenceStreakItem>,
    // 14. Daily Limit Compliance & Redline Breaches
    val cleanDaysCount: Int,
    val breachedDaysCount: Int,
    // 15. Context & Trigger Tag Cloud
    val contextMetrics: List<ContextTagMetric>,
    // 16. RoA Breakdown
    val roaMetrics: List<RoaBreakdownMetric>,
    // 17. Rolling 7-Day vs 30-Day Velocity Momentum
    val velocityMetrics: List<VelocityMomentumMetric>
)

@Immutable
data class DailySubstanceUnitPoint(
    val dayIndex: Int,
    val label: String,
    val dayStartMillis: Long,
    val totalUnits: Double,
    val effectiveUnits: Double,
    val movingAvg7d: Double,
    val doseCount: Int,
    val totalSpend: Double,
    val breachedSafeLimit: Boolean
)

@Immutable
data class CustomFieldAnalyticsGroup(
    val fieldName: String,
    val valueBreakdown: List<Pair<String, Double>>, // Value -> Total Units
    val valueCountBreakdown: List<Pair<String, Int>>, // Value -> Dose Count
    val valueAvgRatingBreakdown: List<Pair<String, Double>> // Value -> Avg Rating
)

@Immutable
data class SubstanceDeepDiveReport(
    val substance: SubstanceEntity,
    val timeframeDays: Int,
    // 1. Unit Velocity Quartet & Complete Stats
    val avgUnitsPerCalendarDay: Double,
    val avgUnitsPerActiveDay: Double,
    val avgSingleDoseUnits: Double,
    val peakDayUnits: Double,
    val totalUnitsConsumed: Double,
    val totalEffectiveUnitsConsumed: Double,
    val totalSpend: Double,
    val totalDosesCount: Int,
    val activeDaysCount: Int,
    val dailyUnitStats: SummaryStats,
    val singleDoseStats: SummaryStats,
    // 2. Daily Unit Consumption + 7-Day Moving Average
    val dailyUnitPoints: List<DailySubstanceUnitPoint>,
    // 3. Cumulative Unit & Cost Burn-Up + 30-Day Projection Cone
    val cumulativeUnitsCurve: List<Float>,
    val cumulativeCostCurve: List<Float>,
    val projected30dUnits: Double,
    val projected30dCost: Double,
    // 4. Day-of-Week Unit Profile (Mon..Sun)
    val weekdayAvgUnits: List<Float>,
    val weekdayMaxUnits: List<Float>,
    val weekdayMinActiveUnits: List<Float>,
    // 5. Subtypes Deep Dive (Druhy této látky)
    val subtypeMetrics: List<SubtypeAnalyticsMetric>,
    val dailySubtypeTimeline: List<DailySubtypeStackedPoint>,
    // 6. Correlations & Dependencies (Korelace & Závislosti)
    val prevIntervalVsDoseRegression: LinearRegressionResult,
    val doseVsNextIntervalRegression: LinearRegressionResult,
    val firstDoseHourVsDailyUnitsRegression: LinearRegressionResult,
    val doseVsRatingRegression: LinearRegressionResult,
    val crossSubstanceCorrelations: List<SynergyPairCell>,
    // 7. Inter-Dose Interval Histogram & Evolution (Rozestupy)
    val intervalBuckets7: List<Int>, // <1h, 1-2h, 2-4h, 4-8h, 8-16h, 16-24h, >24h
    val intervalStatsHours: SummaryStats,
    val intervalEvolution: List<IntervalEvolutionPoint>,
    val medianIntervalHours: Double,
    val meanIntervalHours: Double,
    val compulsiveRedosingIndexPercent: Int,
    val topAbstinenceStreaksHours: List<Pair<String, Float>>,
    // 8. 24-Hour Circadian Distribution
    val hourlyUnitDistribution24: List<Float>,
    val hourlyCountDistribution24: List<Int>,
    val avgFirstDoseHour: Double,
    val avgLastDoseHour: Double,
    // 9. Dose Size Quantiles & Distribution
    val minDose: Double,
    val p25Dose: Double,
    val medianDose: Double,
    val meanDose: Double,
    val p90Dose: Double,
    val maxDose: Double,
    val doseHistogramBins: List<Pair<String, Int>>,
    // 10. Threshold Zone Breakdown (Single vs Stacked)
    val singleDoseTierCounts: Map<DoseTier, Int>,
    val stackedDoseTierCounts: Map<DoseTier, Int>,
    // 11. Multi-Dose Stacking Amplification
    val stackedSessionsCount: Int,
    val avgStackAmplificationRatio: Double,
    val avgPeakShiftMinutes: Int,
    // 12. 7-Day Dual Simulation: Active Effect vs Body Presence
    val simulated7dActiveEffectCurve: List<Float>,
    val simulated7dPlasmaCurve: List<Float>,
    val activeEffectStats: SummaryStats,
    val bodyPresenceStats: SummaryStats,
    // 13. Tolerance & Receptor Saturation Index
    val currentToleranceIndexPercent: Int,
    val estimatedDaysToBaselineReset: Int,
    val toleranceCurve30d: List<Float>,
    // 14. Sleep Hygiene Cutoff Compliance
    val sleepSafeDaysCount: Int,
    val sleepDisruptedDaysCount: Int,
    val sleepCompliancePercent: Int,
    // 15. Custom Field Breakdown
    val customFieldGroups: List<CustomFieldAnalyticsGroup>,
    // 16. RoA Breakdown
    val roaMetrics: List<RoaBreakdownMetric>,
    // 17. Filtered Logs
    val recentLogs: List<IntakeLogEntity>
)

object AnalyticsAggregatorEngine {

    private const val HOUR_MS = 3_600_000L
    private const val DAY_MS = 24 * HOUR_MS

    fun computeSummaryStats(values: List<Double>): SummaryStats {
        if (values.isEmpty()) return SummaryStats()
        val sorted = values.sorted()
        val n = sorted.size
        val sum = sorted.sum()
        val mean = sum / n
        val variance = sorted.sumOf { (it - mean) * (it - mean) } / n
        val stdDev = sqrt(variance)

        fun percentile(p: Double): Double {
            if (n == 1) return sorted[0]
            val pos = p * (n - 1)
            val lower = pos.toInt().coerceIn(0, n - 1)
            val upper = (lower + 1).coerceIn(0, n - 1)
            val weight = pos - lower
            return sorted[lower] * (1.0 - weight) + sorted[upper] * weight
        }

        return SummaryStats(
            count = n,
            min = sorted.first(),
            max = sorted.last(),
            mean = mean,
            median = percentile(0.50),
            p25 = percentile(0.25),
            p90 = percentile(0.90),
            stdDev = stdDev,
            sum = sum
        )
    }

    fun computeLinearRegression(
        points: List<ScatterDataPoint>,
        xNameCz: String,
        yNameCz: String
    ): LinearRegressionResult {
        val xVals = points.map { it.x.toDouble() }
        val yVals = points.map { it.y.toDouble() }
        val xStats = computeSummaryStats(xVals)
        val yStats = computeSummaryStats(yVals)

        if (points.size < 2) {
            return LinearRegressionResult(
                points = points,
                pearsonR = 0.0,
                rSquared = 0.0,
                slope = 0.0,
                intercept = yStats.mean,
                xStats = xStats,
                yStats = yStats,
                interpretationCz = "Nedostatek bodů pro výpočet korelace"
            )
        }

        val meanX = xStats.mean
        val meanY = yStats.mean
        var num = 0.0
        var denX = 0.0
        var denY = 0.0
        for (i in points.indices) {
            val dx = xVals[i] - meanX
            val dy = yVals[i] - meanY
            num += dx * dy
            denX += dx * dx
            denY += dy * dy
        }

        val slope = if (denX > 1e-9) num / denX else 0.0
        val intercept = meanY - slope * meanX
        val denom = sqrt(denX * denY)
        val r = if (denom > 1e-9) (num / denom).coerceIn(-1.0, 1.0) else 0.0
        val r2 = r * r

        val absR = abs(r)
        val strength = when {
            absR >= 0.65 -> "Silná"
            absR >= 0.35 -> "Střední"
            absR >= 0.15 -> "Slabá"
            else -> "Zanedbatelná"
        }
        val direction = if (r >= 0) "pozitivní" else "negativní"
        val interp = if (absR < 0.12) {
            "Nezávislé ($xNameCz nemá přímý lineární vliv na $yNameCz, r = ${String.format(Locale.US, "%.2f", r)})"
        } else {
            "$strength $direction korelace (r = ${String.format(Locale.US, "%.2f", r)}, R² = ${(r2 * 100).roundToInt()}%)"
        }

        return LinearRegressionResult(
            points = points,
            pearsonR = r,
            rSquared = r2,
            slope = slope,
            intercept = intercept,
            xStats = xStats,
            yStats = yStats,
            interpretationCz = interp
        )
    }

    fun buildGlobalReport(
        substances: List<SubstanceEntity>,
        allLogs: List<IntakeLogEntity>,
        timeframeDays: Int,
        nowMillis: Long,
        subtypes: List<SubstanceSubtypeEntity> = emptyList(),
        settings: UserSettings = UserSettings()
    ): GlobalAnalyticsReport {
        val substanceMap = substances.associateBy { it.id }
        val subtypeMap = subtypes.associateBy { it.id }
        val startOfToday = startOfDayMillis(nowMillis)
        val daysCount = timeframeDays.coerceIn(7, 365)
        val windowStart = startOfToday - (daysCount - 1L) * DAY_MS

        val filteredLogs = allLogs.filter { it.timestampMillis in windowStart..(nowMillis + DAY_MS) }
            .sortedBy { it.timestampMillis }

        val totalSpend = filteredLogs.sumOf { it.cost }
        val dailyAvgSpend = totalSpend / daysCount
        val projectedMonthlySpend = dailyAvgSpend * 30.0
        val totalIntakesCount = filteredLogs.size

        val dayLabels = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        val dateFmt = SimpleDateFormat("dd.MM.", Locale.getDefault())
        val dailyTimeline = ArrayList<DailyStackedPoint>(daysCount)
        val cumulativeSpendCurve = ArrayList<Float>(daysCount)
        var runningSpend = 0.0
        var breachedDaysCount = 0
        var cleanDaysCount = 0

        for (d in 0 until daysCount) {
            val dStart = windowStart + d * DAY_MS
            val dEnd = dStart + DAY_MS
            val dayLogs = filteredLogs.filter { it.timestampMillis in dStart until dEnd }
            val cal = Calendar.getInstance().apply { timeInMillis = dStart }
            val dowLabel = dayLabels[cal.get(Calendar.DAY_OF_WEEK) - 1]
            val dateStr = "$dowLabel ${cal.get(Calendar.DAY_OF_MONTH)}.${cal.get(Calendar.MONTH) + 1}."

            val countMap = mutableMapOf<Long, Int>()
            val loadMap = mutableMapOf<Long, Float>()
            val unitsMap = mutableMapOf<Long, Double>()
            var dayBreached = false

            dayLogs.groupBy { it.substanceId }.forEach { (subId, sLogs) ->
                val sub = substanceMap[subId]
                val uSum = sLogs.sumOf { it.doseAmount }
                countMap[subId] = sLogs.size
                unitsMap[subId] = uSum
                val safeMax = (sub?.dailyMaxSafeLimit ?: 100.0).coerceAtLeast(1.0)
                loadMap[subId] = (uSum / safeMax).toFloat().coerceAtMost(2.5f)
                if (uSum > safeMax) dayBreached = true
            }

            if (dayLogs.isEmpty()) cleanDaysCount++
            if (dayBreached) breachedDaysCount++

            val dSpend = dayLogs.sumOf { it.cost }
            runningSpend += dSpend
            cumulativeSpendCurve.add(runningSpend.toFloat())

            dailyTimeline.add(
                DailyStackedPoint(
                    dayIndex = d,
                    label = dateStr,
                    dayStartMillis = dStart,
                    perSubstanceCount = countMap,
                    perSubstanceNormalizedLoad = loadMap,
                    perSubstanceUnits = unitsMap,
                    totalSpend = dSpend,
                    breachedLimit = dayBreached
                )
            )
        }

        val dailySpendStats = computeSummaryStats(dailyTimeline.map { it.totalSpend })
        val dailyDoseCountStats = computeSummaryStats(dailyTimeline.map { it.perSubstanceCount.values.sum().toDouble() })

        // Active effect hours vs Body presence hours
        val totalWindowHours = (daysCount * 24.0).coerceAtLeast(24.0)
        val estimatedActiveHours = filteredLogs.sumOf { log ->
            val sub = substanceMap[log.substanceId]
            if (sub != null && sub.isTimingEnabled) (sub.totalDurationMinutes / 60.0) * 0.62 else 0.0
        }.coerceAtMost(totalWindowHours)
        val estimatedBodyHours = filteredLogs.sumOf { log ->
            val sub = substanceMap[log.substanceId]
            if (sub != null && sub.isTimingEnabled && sub.hasBodyPresenceDuration) {
                sub.bodyPresenceHours * 0.55
            } else if (sub != null && sub.isTimingEnabled) {
                (sub.totalDurationMinutes / 60.0) * 0.62
            } else 0.0
        }.coerceAtMost(totalWindowHours)

        val activeHoursPercentage = ((estimatedActiveHours / totalWindowHours) * 100.0).toFloat().coerceIn(0f, 100f)
        val bodyPresenceHoursPercentage = ((estimatedBodyHours / totalWindowHours) * 100.0).toFloat().coerceIn(0f, 100f)

        val redlineCount = filteredLogs.count {
            it.evaluatedTier == DoseTier.DANGEROUS.name || it.stackedPeakTier == DoseTier.DANGEROUS.name
        }
        val strongCount = filteredLogs.count {
            it.evaluatedTier == DoseTier.STRONG.name || it.stackedPeakTier == DoseTier.STRONG.name
        }
        val safetyPenalty = (breachedDaysCount * settings.safetyScoreBreachPenalty) +
            (redlineCount * settings.safetyScoreRedlinePenalty) +
            (strongCount * 2)
        val safetyBonus = (cleanDaysCount * 2)
        val harmReductionSafetyScore = (92 - safetyPenalty + safetyBonus).coerceIn(18, 100)

        // Spend Shares
        val spendDenom = totalSpend.coerceAtLeast(0.01)
        val spendShares = substances.mapNotNull { sub ->
            val sLogs = filteredLogs.filter { it.substanceId == sub.id }
            if (sLogs.isEmpty()) null
            else {
                val sp = sLogs.sumOf { it.cost }
                SubstanceSpendShare(
                    substance = sub,
                    totalSpend = sp,
                    shareFraction = (sp / spendDenom).toFloat().coerceIn(0f, 1f),
                    doseCount = sLogs.size,
                    totalUnits = sLogs.sumOf { it.doseAmount }
                )
            }
        }.sortedByDescending { it.totalSpend }

        // 84-day Heatmap
        val heatmapStart = startOfToday - 83L * DAY_MS
        val heatmapCells = ArrayList<HeatmapDayCell>(84)
        for (i in 0 until 84) {
            val cellStart = heatmapStart + i * DAY_MS
            val cellEnd = cellStart + DAY_MS
            val cLogs = allLogs.filter { it.timestampMillis in cellStart until cellEnd }
            if (cLogs.isEmpty()) {
                heatmapCells.add(
                    HeatmapDayCell(
                        dayOffsetFromToday = 83 - i,
                        timestampMillis = cellStart,
                        dateLabel = dateFmt.format(Date(cellStart)),
                        dominantSubstanceColorHex = 0xFF1E293BL,
                        dominantSubstanceName = "Čistý den",
                        intensityFraction = 0f,
                        doseCount = 0,
                        totalSpend = 0.0
                    )
                )
            } else {
                val dominantSubId = cLogs.groupBy { it.substanceId }
                    .maxByOrNull { it.value.size }?.key ?: substances.firstOrNull()?.id ?: 1L
                val domSub = substanceMap[dominantSubId]
                val domColor = domSub?.colorHex ?: 0xFF06B6D4L
                val intensity = (cLogs.size / 6f).coerceIn(0.22f, 1f)
                heatmapCells.add(
                    HeatmapDayCell(
                        dayOffsetFromToday = 83 - i,
                        timestampMillis = cellStart,
                        dateLabel = dateFmt.format(Date(cellStart)),
                        dominantSubstanceColorHex = domColor,
                        dominantSubstanceName = domSub?.name ?: "Substance",
                        intensityFraction = intensity,
                        doseCount = cLogs.size,
                        totalSpend = cLogs.sumOf { it.cost }
                    )
                )
            }
        }

        // 24-Hour Circadian
        val hourlyCounts = IntArray(24)
        val hourlyColorBuckets = Array(24) { mutableMapOf<Long, Int>() }
        filteredLogs.forEach { log ->
            val cal = Calendar.getInstance().apply { timeInMillis = log.timestampMillis }
            val hr = cal.get(Calendar.HOUR_OF_DAY).coerceIn(0, 23)
            hourlyCounts[hr]++
            val col = substanceMap[log.substanceId]?.colorHex ?: 0xFF06B6D4L
            hourlyColorBuckets[hr][col] = (hourlyColorBuckets[hr][col] ?: 0) + 1
        }
        val hourlyDominantColors = (0 until 24).map { hr ->
            hourlyColorBuckets[hr].maxByOrNull { it.value }?.key ?: 0xFF06B6D4L
        }

        // Weekday Profile (Mon=0 .. Sun=6)
        val weekdayDosesSum = FloatArray(7)
        val weekdaySpendSum = FloatArray(7)
        val weekdayLoadSum = FloatArray(7)
        val weekdayCounts = IntArray(7)

        dailyTimeline.forEach { pt ->
            val cal = Calendar.getInstance().apply { timeInMillis = pt.dayStartMillis }
            val dow = cal.get(Calendar.DAY_OF_WEEK)
            val monIdx = if (dow == Calendar.SUNDAY) 6 else dow - 2
            weekdayCounts[monIdx]++
            weekdayDosesSum[monIdx] += pt.perSubstanceCount.values.sum().toFloat()
            weekdaySpendSum[monIdx] += pt.totalSpend.toFloat()
            weekdayLoadSum[monIdx] += pt.perSubstanceNormalizedLoad.values.sum()
        }

        val weekdayAvgDoses = (0..6).map { i ->
            if (weekdayCounts[i] > 0) weekdayDosesSum[i] / weekdayCounts[i] else 0f
        }
        val weekdayAvgSpend = (0..6).map { i ->
            if (weekdayCounts[i] > 0) weekdaySpendSum[i] / weekdayCounts[i] else 0f
        }
        val weekdayAvgLoad = (0..6).map { i ->
            if (weekdayCounts[i] > 0) weekdayLoadSum[i] / weekdayCounts[i] else 0f
        }

        // Global Subtype Metrics across all substances
        val totalLogsDenom = filteredLogs.size.coerceAtLeast(1).toFloat()
        val globalSubtypeMetrics = filteredLogs
            .groupBy { Pair(it.substanceId, it.subtypeName.ifBlank { "Standard" }) }
            .mapNotNull { (key, sLogs) ->
                val sub = substanceMap[key.first] ?: return@mapNotNull null
                val subtypeName = key.second
                val matchedSubtype = sLogs.firstNotNullOfOrNull { it.subtypeId }?.let { subtypeMap[it] }
                val subTotalUnits = filteredLogs.filter { it.substanceId == sub.id }.sumOf { it.doseAmount }.coerceAtLeast(0.01)
                val uSum = sLogs.sumOf { it.doseAmount }
                val effSum = sLogs.sumOf { it.effectiveDoseAmount }
                val spSum = sLogs.sumOf { it.cost }
                SubtypeAnalyticsMetric(
                    subtypeId = matchedSubtype?.id,
                    subtypeName = subtypeName,
                    parentSubstanceId = sub.id,
                    parentSubstanceName = sub.name,
                    unit = sub.defaultUnit,
                    colorHex = matchedSubtype?.colorHex ?: sub.colorHex,
                    potencyMultiplier = matchedSubtype?.potencyMultiplier ?: sLogs.first().potencyMultiplier,
                    doseCount = sLogs.size,
                    shareOfUnitsFraction = (uSum / subTotalUnits).toFloat().coerceIn(0f, 1f),
                    shareOfCountFraction = (sLogs.size / totalLogsDenom).coerceIn(0f, 1f),
                    totalUnits = uSum,
                    totalEffectiveUnits = effSum,
                    totalSpend = spSum,
                    avgCostPerDose = spSum / sLogs.size,
                    avgRating = sLogs.map { it.subjectiveRating.toDouble() }.average(),
                    doseStats = computeSummaryStats(sLogs.map { it.doseAmount })
                )
            }
            .sortedByDescending { it.doseCount }

        // Cross-Substance Synergy & Pearson Correlation Matrix
        val synergyPairs = mutableListOf<SynergyPairCell>()
        val activeDayLogs = dailyTimeline.filter { it.perSubstanceCount.isNotEmpty() }
        val activeDaysTotal = activeDayLogs.size.coerceAtLeast(1)

        for (i in substances.indices) {
            for (j in i + 1 until substances.size) {
                val subA = substances[i]
                val subB = substances[j]
                val shared = activeDayLogs.count {
                    (it.perSubstanceCount[subA.id] ?: 0) > 0 && (it.perSubstanceCount[subB.id] ?: 0) > 0
                }
                // Compute Pearson r across daily units of A and B on days where at least one was taken
                val unionDays = activeDayLogs.filter {
                    (it.perSubstanceCount[subA.id] ?: 0) > 0 || (it.perSubstanceCount[subB.id] ?: 0) > 0
                }
                var pearsonR = 0.0
                if (unionDays.size >= 3) {
                    val aVals = unionDays.map { it.perSubstanceUnits[subA.id] ?: 0.0 }
                    val bVals = unionDays.map { it.perSubstanceUnits[subB.id] ?: 0.0 }
                    val meanA = aVals.average()
                    val meanB = bVals.average()
                    var num = 0.0
                    var dA = 0.0
                    var dB = 0.0
                    for (k in unionDays.indices) {
                        val da = aVals[k] - meanA
                        val db = bVals[k] - meanB
                        num += da * db
                        dA += da * da
                        dB += db * db
                    }
                    val den = sqrt(dA * dB)
                    if (den > 1e-9) pearsonR = (num / den).coerceIn(-1.0, 1.0)
                }

                if (shared > 0 || abs(pearsonR) > 0.05) {
                    synergyPairs.add(
                        SynergyPairCell(
                            subAId = subA.id,
                            subBId = subB.id,
                            subAName = subA.name,
                            subBName = subB.name,
                            subAColorHex = subA.colorHex,
                            subBColorHex = subB.colorHex,
                            coOccurrencePercent = ((shared * 100.0) / activeDaysTotal).roundToInt().coerceIn(1, 100),
                            sharedDaysCount = shared,
                            pearsonDoseCorrelation = pearsonR
                        )
                    )
                }
            }
        }
        synergyPairs.sortByDescending { it.coOccurrencePercent }

        // Global Inter-Dose Intervals & Scatter: Interval vs Relative Dose (% of defaultDose)
        val globalIntervalBuckets = IntArray(7)
        val allGapHours = mutableListOf<Double>()
        val intervalScatterPoints = mutableListOf<ScatterDataPoint>()
        val globalIntervalEvolution = mutableListOf<IntervalEvolutionPoint>()

        substances.forEach { sub ->
            val sLogs = filteredLogs.filter { it.substanceId == sub.id }.sortedBy { it.timestampMillis }
            for (idx in 1 until sLogs.size) {
                val prev = sLogs[idx - 1]
                val curr = sLogs[idx]
                val gapHrs = (curr.timestampMillis - prev.timestampMillis) / 3_600_000.0
                if (gapHrs in 0.1..168.0) {
                    allGapHours.add(gapHrs)
                    when {
                        gapHrs < 1.0 -> globalIntervalBuckets[0]++
                        gapHrs < 2.0 -> globalIntervalBuckets[1]++
                        gapHrs < 4.0 -> globalIntervalBuckets[2]++
                        gapHrs < 8.0 -> globalIntervalBuckets[3]++
                        gapHrs < 16.0 -> globalIntervalBuckets[4]++
                        gapHrs < 24.0 -> globalIntervalBuckets[5]++
                        else -> globalIntervalBuckets[6]++
                    }
                    val relDosePct = ((curr.doseAmount / sub.defaultDose.coerceAtLeast(0.1)) * 100.0).toFloat()
                    if (gapHrs <= 48.0) {
                        intervalScatterPoints.add(
                            ScatterDataPoint(
                                x = gapHrs.toFloat(),
                                y = relDosePct,
                                label = "${sub.name} (${curr.subtypeName})",
                                secondaryLabel = "${String.format(Locale.US, "%.1f", gapHrs)}h → ${curr.doseAmount} ${sub.defaultUnit}",
                                colorHex = sub.colorHex,
                                timestampMillis = curr.timestampMillis
                            )
                        )
                    }
                }
            }
        }

        // Chronological Global Interval Evolution (last 40 gaps)
        val sortedLogsAll = filteredLogs.sortedBy { it.timestampMillis }
        val recentGapsRaw = mutableListOf<Triple<Long, Float, IntakeLogEntity>>()
        val lastSeenBySub = mutableMapOf<Long, IntakeLogEntity>()
        for (log in sortedLogsAll) {
            val prev = lastSeenBySub[log.substanceId]
            if (prev != null) {
                val gapH = ((log.timestampMillis - prev.timestampMillis) / 3_600_000.0).toFloat()
                if (gapH in 0.15f..72f) {
                    recentGapsRaw.add(Triple(log.timestampMillis, gapH, log))
                }
            }
            lastSeenBySub[log.substanceId] = log
        }
        val tailGaps = recentGapsRaw.takeLast(40)
        for (i in tailGaps.indices) {
            val windowSlice = tailGaps.subList(max(0, i - 4), i + 1)
            val rollAvg = windowSlice.map { it.second }.average().toFloat()
            val item = tailGaps[i]
            globalIntervalEvolution.add(
                IntervalEvolutionPoint(
                    index = i,
                    timestampMillis = item.first,
                    dateLabel = dateFmt.format(Date(item.first)),
                    intervalHours = item.second,
                    rollingAvgHours = rollAvg,
                    doseAmountAfterGap = item.third.doseAmount,
                    subtypeName = item.third.subtypeName
                )
            )
        }

        val intervalVsNormalizedDoseRegression = computeLinearRegression(
            points = intervalScatterPoints.takeLast(80),
            xNameCz = "Rozestup (h)",
            yNameCz = "Relativní dávka (%)"
        )

        // First-Dose Hour vs Daily Bio-Load Regression
        val firstDoseVsLoadPoints = mutableListOf<ScatterDataPoint>()
        val loadVsRatingPoints = mutableListOf<ScatterDataPoint>()
        for (pt in dailyTimeline) {
            val dEnd = pt.dayStartMillis + DAY_MS
            val dLogs = filteredLogs.filter { it.timestampMillis in pt.dayStartMillis until dEnd }
            if (dLogs.isNotEmpty()) {
                val firstLog = dLogs.minBy { it.timestampMillis }
                val cal = Calendar.getInstance().apply { timeInMillis = firstLog.timestampMillis }
                val firstHr = cal.get(Calendar.HOUR_OF_DAY) + cal.get(Calendar.MINUTE) / 60f
                val totalLoadPct = (pt.perSubstanceNormalizedLoad.values.sum() * 100f)
                val domSub = substanceMap[firstLog.substanceId]
                firstDoseVsLoadPoints.add(
                    ScatterDataPoint(
                        x = firstHr,
                        y = totalLoadPct,
                        label = pt.label,
                        secondaryLabel = "První v ${String.format(Locale.US, "%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))} → Zátěž ${totalLoadPct.roundToInt()}%",
                        colorHex = domSub?.colorHex ?: 0xFF06B6D4L,
                        timestampMillis = pt.dayStartMillis
                    )
                )
                val avgRating = dLogs.map { it.subjectiveRating }.average().toFloat()
                loadVsRatingPoints.add(
                    ScatterDataPoint(
                        x = totalLoadPct,
                        y = avgRating,
                        label = pt.label,
                        secondaryLabel = "Zátěž ${totalLoadPct.roundToInt()}% → Hodnocení ${String.format(Locale.US, "%.1f", avgRating)}/10",
                        colorHex = domSub?.colorHex ?: 0xFF10B981L,
                        timestampMillis = pt.dayStartMillis
                    )
                )
            }
        }

        val firstDoseHourVsDailyLoadRegression = computeLinearRegression(
            points = firstDoseVsLoadPoints,
            xNameCz = "Hodina 1. dávky",
            yNameCz = "Denní bio-zátěž (%)"
        )

        val dailyLoadVsSubjectiveRatingRegression = computeLinearRegression(
            points = loadVsRatingPoints,
            xNameCz = "Denní bio-zátěž (%)",
            yNameCz = "Hodnocení (1-10)"
        )

        // Tier Distribution
        val tierCounts = DoseTier.entries.filter { it != DoseTier.INACTIVE }.associateWith { tier ->
            filteredLogs.count { it.stackedPeakTier == tier.name || it.evaluatedTier == tier.name }
        }

        // Composite 48-Hour Multi-Substance PK Wave (Active Effect vs Body Presence)
        val waveStart = nowMillis - 36 * HOUR_MS
        val waveStep = HOUR_MS
        val composite48hActiveWaves = mutableMapOf<Long, List<Float>>()
        val composite48hBodyWaves = mutableMapOf<Long, List<Float>>()
        substances.filter { it.isTimingEnabled }.take(5).forEach { sub ->
            val sLogs = allLogs.filter {
                it.substanceId == sub.id && it.timestampMillis >= waveStart - 24 * HOUR_MS
            }
            val activePts = ArrayList<Float>(48)
            val bodyPts = ArrayList<Float>(48)
            for (h in 0 until 48) {
                val t = waveStart + h * waveStep
                val eff = PharmacokineticEngine.combinedEffectAt(t, sLogs, sub, settings)
                val body = PharmacokineticEngine.combinedPlasmaAt(t, sLogs, sub, settings)
                val normEff = (eff / sub.thresholdStrong.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1.5f)
                val normBody = (body / sub.thresholdStrong.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1.5f)
                activePts.add(normEff)
                bodyPts.add(normBody)
            }
            composite48hActiveWaves[sub.id] = activePts
            composite48hBodyWaves[sub.id] = bodyPts
        }

        // Abstinence Streaks
        val streakLeaderboard = substances.mapNotNull { sub ->
            val sLogs = allLogs.filter { it.substanceId == sub.id }.sortedBy { it.timestampMillis }
            if (sLogs.isEmpty()) null
            else {
                val lastLog = sLogs.last()
                val currentElapsedHrs = max(0.0, (nowMillis - lastLog.timestampMillis) / 3_600_000.0)
                var maxGapHrs = currentElapsedHrs
                var sumGaps = 0.0
                var gapCount = 0
                for (k in 1 until sLogs.size) {
                    val gap = (sLogs[k].timestampMillis - sLogs[k - 1].timestampMillis) / 3_600_000.0
                    if (gap > maxGapHrs) maxGapHrs = gap
                    sumGaps += gap
                    gapCount++
                }
                AbstinenceStreakItem(
                    substance = sub,
                    currentElapsedHours = currentElapsedHrs,
                    longestStreakHours = maxGapHrs,
                    avgGapHours = if (gapCount > 0) sumGaps / gapCount else currentElapsedHrs,
                    lastDoseAmount = lastLog.doseAmount
                )
            }
        }.sortedByDescending { it.currentElapsedHours }

        // Context Tag Cloud
        val contextMetrics = filteredLogs.groupBy { it.contextTag.ifBlank { "Focus" } }
            .map { (tag, logs) ->
                ContextTagMetric(
                    tag = tag,
                    count = logs.size,
                    totalSpend = logs.sumOf { it.cost },
                    avgRating = logs.map { it.subjectiveRating.toDouble() }.average(),
                    shareFraction = (logs.size / totalLogsDenom).coerceIn(0f, 1f)
                )
            }.sortedByDescending { it.count }

        // RoA Breakdown
        val roaMetrics = filteredLogs.groupBy { it.roaName.ifBlank { "Oral" } }
            .map { (roa, logs) ->
                RoaBreakdownMetric(
                    roaName = roa,
                    count = logs.size,
                    totalUnits = logs.sumOf { it.doseAmount },
                    shareFraction = (logs.size / totalLogsDenom).coerceIn(0f, 1f),
                    avgOnsetMultiplier = logs.map { it.onsetMultiplier }.average()
                )
            }.sortedByDescending { it.count }

        // Velocity 7d vs 30d
        val start7d = startOfToday - 6L * DAY_MS
        val start30d = startOfToday - 29L * DAY_MS
        val velocityMetrics = substances.mapNotNull { sub ->
            val logs30 = allLogs.filter { it.substanceId == sub.id && it.timestampMillis >= start30d }
            if (logs30.isEmpty()) null
            else {
                val logs7 = logs30.filter { it.timestampMillis >= start7d }
                val avg7 = logs7.sumOf { it.doseAmount } / 7.0
                val avg30 = logs30.sumOf { it.doseAmount } / 30.0
                val deltaPct = if (avg30 > 0.001) (((avg7 - avg30) / avg30) * 100.0).roundToInt() else 0
                VelocityMomentumMetric(
                    substance = sub,
                    rolling7dDailyUnits = avg7,
                    rolling30dDailyUnits = avg30,
                    deltaPercent = deltaPct
                )
            }
        }

        return GlobalAnalyticsReport(
            timeframeDays = daysCount,
            totalSpend = totalSpend,
            dailyAvgSpend = dailyAvgSpend,
            projectedMonthlySpend = projectedMonthlySpend,
            totalIntakesCount = totalIntakesCount,
            activeHoursPercentage = activeHoursPercentage,
            bodyPresenceHoursPercentage = bodyPresenceHoursPercentage,
            harmReductionSafetyScore = harmReductionSafetyScore,
            dailySpendStats = dailySpendStats,
            dailyDoseCountStats = dailyDoseCountStats,
            dailyTimeline = dailyTimeline,
            spendShares = spendShares,
            cumulativeSpendCurve = cumulativeSpendCurve,
            heatmapCells = heatmapCells,
            hourlyDistribution24 = hourlyCounts.toList(),
            hourlyDominantColor24 = hourlyDominantColors,
            weekdayAvgDoses = weekdayAvgDoses,
            weekdayAvgSpend = weekdayAvgSpend,
            weekdayAvgLoad = weekdayAvgLoad,
            globalSubtypeMetrics = globalSubtypeMetrics,
            synergyPairs = synergyPairs,
            intervalVsNormalizedDoseRegression = intervalVsNormalizedDoseRegression,
            firstDoseHourVsDailyLoadRegression = firstDoseHourVsDailyLoadRegression,
            dailyLoadVsSubjectiveRatingRegression = dailyLoadVsSubjectiveRatingRegression,
            globalIntervalBuckets7 = globalIntervalBuckets.toList(),
            globalIntervalStats = computeSummaryStats(allGapHours),
            globalIntervalEvolution = globalIntervalEvolution,
            tierCounts = tierCounts,
            composite48hActiveWaves = composite48hActiveWaves,
            composite48hBodyWaves = composite48hBodyWaves,
            streakLeaderboard = streakLeaderboard,
            cleanDaysCount = cleanDaysCount,
            breachedDaysCount = breachedDaysCount,
            contextMetrics = contextMetrics,
            roaMetrics = roaMetrics,
            velocityMetrics = velocityMetrics
        )
    }

    fun buildSubstanceDeepDive(
        substance: SubstanceEntity,
        allLogs: List<IntakeLogEntity>,
        timeframeDays: Int,
        bedtimeHour24: Int,
        nowMillis: Long,
        allSubstances: List<SubstanceEntity> = listOf(substance),
        subtypes: List<SubstanceSubtypeEntity> = emptyList(),
        settings: UserSettings = UserSettings()
    ): SubstanceDeepDiveReport {
        val startOfToday = startOfDayMillis(nowMillis)
        val daysCount = timeframeDays.coerceIn(7, 365)
        val windowStart = startOfToday - (daysCount - 1L) * DAY_MS
        val subtypeMap = subtypes.associateBy { it.id }
        val dateFmt = SimpleDateFormat("dd.MM.", Locale.getDefault())

        val subLogsAllTime = allLogs.filter { it.substanceId == substance.id }
            .sortedBy { it.timestampMillis }
        val subLogs = subLogsAllTime.filter { it.timestampMillis in windowStart..(nowMillis + DAY_MS) }

        val totalUnits = subLogs.sumOf { it.doseAmount }
        val totalEffectiveUnits = subLogs.sumOf { it.effectiveDoseAmount }
        val totalSpend = subLogs.sumOf { it.cost }
        val totalDoses = subLogs.size

        val dayLabels = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        val dailyUnitPoints = ArrayList<DailySubstanceUnitPoint>(daysCount)
        val dailySubtypeTimeline = ArrayList<DailySubtypeStackedPoint>(daysCount)
        val cumulativeUnitsCurve = ArrayList<Float>(daysCount)
        val cumulativeCostCurve = ArrayList<Float>(daysCount)

        var runningUnits = 0.0
        var runningCost = 0.0
        var activeDaysCount = 0
        var peakDayUnits = 0.0

        val rawDailyUnits = DoubleArray(daysCount)
        for (d in 0 until daysCount) {
            val dStart = windowStart + d * DAY_MS
            val dEnd = dStart + DAY_MS
            val dLogs = subLogs.filter { it.timestampMillis in dStart until dEnd }
            val uSum = dLogs.sumOf { it.doseAmount }
            rawDailyUnits[d] = uSum
            if (dLogs.isNotEmpty()) activeDaysCount++
            if (uSum > peakDayUnits) peakDayUnits = uSum
        }

        for (d in 0 until daysCount) {
            val dStart = windowStart + d * DAY_MS
            val dEnd = dStart + DAY_MS
            val dLogs = subLogs.filter { it.timestampMillis in dStart until dEnd }
            val uSum = rawDailyUnits[d]
            val effSum = dLogs.sumOf { it.effectiveDoseAmount }
            val cSum = dLogs.sumOf { it.cost }

            var windowSum = 0.0
            var windowCnt = 0
            for (k in max(0, d - 6)..d) {
                windowSum += rawDailyUnits[k]
                windowCnt++
            }
            val ma7 = if (windowCnt > 0) windowSum / windowCnt else 0.0

            runningUnits += uSum
            runningCost += cSum
            cumulativeUnitsCurve.add(runningUnits.toFloat())
            cumulativeCostCurve.add(runningCost.toFloat())

            val cal = Calendar.getInstance().apply { timeInMillis = dStart }
            val dowLabel = dayLabels[cal.get(Calendar.DAY_OF_WEEK) - 1]
            val dateStr = "$dowLabel ${cal.get(Calendar.DAY_OF_MONTH)}.${cal.get(Calendar.MONTH) + 1}."

            dailyUnitPoints.add(
                DailySubstanceUnitPoint(
                    dayIndex = d,
                    label = dateStr,
                    dayStartMillis = dStart,
                    totalUnits = uSum,
                    effectiveUnits = effSum,
                    movingAvg7d = ma7,
                    doseCount = dLogs.size,
                    totalSpend = cSum,
                    breachedSafeLimit = uSum > substance.dailyMaxSafeLimit
                )
            )

            val subtypeUnitsMap = dLogs.groupBy { it.subtypeName.ifBlank { "Standard" } }
                .mapValues { (_, logs) -> logs.sumOf { it.doseAmount } }
            dailySubtypeTimeline.add(
                DailySubtypeStackedPoint(
                    dayIndex = d,
                    label = dateStr,
                    dayStartMillis = dStart,
                    perSubtypeUnits = subtypeUnitsMap,
                    totalUnits = uSum
                )
            )
        }

        val avgUnitsCalendar = totalUnits / daysCount
        val avgUnitsActive = if (activeDaysCount > 0) totalUnits / activeDaysCount else 0.0
        val avgSingleDose = if (totalDoses > 0) totalUnits / totalDoses else 0.0

        val projected30dUnits = avgUnitsCalendar * 30.0
        val projected30dCost = (totalSpend / daysCount) * 30.0

        val dailyUnitStats = computeSummaryStats(rawDailyUnits.toList())
        val singleDoseStats = computeSummaryStats(subLogs.map { it.doseAmount })

        // Weekday Unit Profile (Mon=0..Sun=6)
        val weekdayLists = Array(7) { mutableListOf<Double>() }
        dailyUnitPoints.forEach { pt ->
            val cal = Calendar.getInstance().apply { timeInMillis = pt.dayStartMillis }
            val dow = cal.get(Calendar.DAY_OF_WEEK)
            val monIdx = if (dow == Calendar.SUNDAY) 6 else dow - 2
            weekdayLists[monIdx].add(pt.totalUnits)
        }
        val weekdayAvgUnits = (0..6).map { i ->
            if (weekdayLists[i].isNotEmpty()) weekdayLists[i].average().toFloat() else 0f
        }
        val weekdayMaxUnits = (0..6).map { i ->
            (weekdayLists[i].maxOrNull() ?: 0.0).toFloat()
        }
        val weekdayMinActiveUnits = (0..6).map { i ->
            (weekdayLists[i].filter { it > 0.0 }.minOrNull() ?: 0.0).toFloat()
        }

        // Subtypes Breakdown for this substance
        val totalSubUnitsDenom = totalUnits.coerceAtLeast(0.01)
        val totalSubCountDenom = totalDoses.coerceAtLeast(1).toFloat()
        val subtypeMetrics = subLogs.groupBy { it.subtypeName.ifBlank { "Standard" } }
            .map { (sName, sLogs) ->
                val matchedSub = sLogs.firstNotNullOfOrNull { it.subtypeId }?.let { subtypeMap[it] }
                    ?: subtypes.firstOrNull { it.substanceId == substance.id && it.name.equals(sName, ignoreCase = true) }
                val uSum = sLogs.sumOf { it.doseAmount }
                val effSum = sLogs.sumOf { it.effectiveDoseAmount }
                val spSum = sLogs.sumOf { it.cost }
                SubtypeAnalyticsMetric(
                    subtypeId = matchedSub?.id,
                    subtypeName = sName,
                    parentSubstanceId = substance.id,
                    parentSubstanceName = substance.name,
                    unit = substance.defaultUnit,
                    colorHex = matchedSub?.colorHex ?: substance.colorHex,
                    potencyMultiplier = matchedSub?.potencyMultiplier ?: sLogs.first().potencyMultiplier,
                    doseCount = sLogs.size,
                    shareOfUnitsFraction = (uSum / totalSubUnitsDenom).toFloat().coerceIn(0f, 1f),
                    shareOfCountFraction = (sLogs.size / totalSubCountDenom).coerceIn(0f, 1f),
                    totalUnits = uSum,
                    totalEffectiveUnits = effSum,
                    totalSpend = spSum,
                    avgCostPerDose = spSum / sLogs.size,
                    avgRating = sLogs.map { it.subjectiveRating.toDouble() }.average(),
                    doseStats = computeSummaryStats(sLogs.map { it.doseAmount })
                )
            }
            .sortedByDescending { it.totalUnits }

        // Inter-Dose Intervals & Correlations
        val intervalBuckets = IntArray(7)
        val intervalHoursList = mutableListOf<Double>()
        val prevIntervalVsDosePoints = mutableListOf<ScatterDataPoint>()
        val doseVsNextIntervalPoints = mutableListOf<ScatterDataPoint>()
        val intervalEvolution = mutableListOf<IntervalEvolutionPoint>()
        var compulsiveRedoseCount = 0
        val compulsiveThresholdHrs = max(
            substance.recommendedMinIntervalMinutes / 60.0,
            settings.compulsiveRedoseThresholdHours * 0.5
        )

        for (i in 1 until subLogs.size) {
            val prev = subLogs[i - 1]
            val curr = subLogs[i]
            val gapHrs = (curr.timestampMillis - prev.timestampMillis) / 3_600_000.0
            if (gapHrs >= 0.05) {
                intervalHoursList.add(gapHrs)
                if (gapHrs < compulsiveThresholdHrs) compulsiveRedoseCount++
                when {
                    gapHrs < 1.0 -> intervalBuckets[0]++
                    gapHrs < 2.0 -> intervalBuckets[1]++
                    gapHrs < 4.0 -> intervalBuckets[2]++
                    gapHrs < 8.0 -> intervalBuckets[3]++
                    gapHrs < 16.0 -> intervalBuckets[4]++
                    gapHrs < 24.0 -> intervalBuckets[5]++
                    else -> intervalBuckets[6]++
                }

                if (gapHrs <= 72.0) {
                    val subColor = curr.subtypeId?.let { subtypeMap[it]?.colorHex } ?: substance.colorHex
                    prevIntervalVsDosePoints.add(
                        ScatterDataPoint(
                            x = gapHrs.toFloat(),
                            y = curr.doseAmount.toFloat(),
                            label = "${curr.subtypeName} (${dateFmt.format(Date(curr.timestampMillis))})",
                            secondaryLabel = "Po pauze ${String.format(Locale.US, "%.1f", gapHrs)}h → Dávka ${curr.doseAmount} ${substance.defaultUnit}",
                            colorHex = subColor,
                            timestampMillis = curr.timestampMillis
                        )
                    )
                    doseVsNextIntervalPoints.add(
                        ScatterDataPoint(
                            x = prev.doseAmount.toFloat(),
                            y = gapHrs.toFloat(),
                            label = "${prev.subtypeName} (${dateFmt.format(Date(prev.timestampMillis))})",
                            secondaryLabel = "Dávka ${prev.doseAmount} ${substance.defaultUnit} → Další pauza ${String.format(Locale.US, "%.1f", gapHrs)}h",
                            colorHex = subColor,
                            timestampMillis = prev.timestampMillis
                        )
                    )

                    val recentWindow = intervalHoursList.takeLast(5)
                    val rollAvg = recentWindow.average().toFloat()
                    intervalEvolution.add(
                        IntervalEvolutionPoint(
                            index = intervalEvolution.size,
                            timestampMillis = curr.timestampMillis,
                            dateLabel = dateFmt.format(Date(curr.timestampMillis)),
                            intervalHours = gapHrs.toFloat(),
                            rollingAvgHours = rollAvg,
                            doseAmountAfterGap = curr.doseAmount,
                            subtypeName = curr.subtypeName
                        )
                    )
                }
            }
        }

        val intervalStatsHours = computeSummaryStats(intervalHoursList)
        val medianIntervalHours = intervalStatsHours.median
        val meanIntervalHours = intervalStatsHours.mean
        val compulsiveRedosingIndexPercent = if (intervalHoursList.isNotEmpty()) {
            ((compulsiveRedoseCount * 100.0) / intervalHoursList.size).roundToInt().coerceIn(0, 100)
        } else 0

        // Top Abstinence Streaks for this substance
        val allStreaks = mutableListOf<Pair<String, Float>>()
        for (i in 1 until subLogsAllTime.size) {
            val gapH = ((subLogsAllTime[i].timestampMillis - subLogsAllTime[i - 1].timestampMillis) / 3_600_000.0).toFloat()
            if (gapH >= 6f) {
                val lbl = "${dateFmt.format(Date(subLogsAllTime[i - 1].timestampMillis))}–${dateFmt.format(Date(subLogsAllTime[i].timestampMillis))}"
                allStreaks.add(lbl to gapH)
            }
        }
        if (subLogsAllTime.isNotEmpty()) {
            val currentGapH = max(0f, ((nowMillis - subLogsAllTime.last().timestampMillis) / 3_600_000.0).toFloat())
            allStreaks.add("Aktuální pauza" to currentGapH)
        }
        val topAbstinenceStreaksHours = allStreaks.sortedByDescending { it.second }.take(8)

        // First-Dose Hour vs Total Daily Units & Dose vs Rating
        val firstHourVsDailyPoints = mutableListOf<ScatterDataPoint>()
        val firstHours = mutableListOf<Double>()
        val lastHours = mutableListOf<Double>()
        dailyUnitPoints.forEach { pt ->
            val dEnd = pt.dayStartMillis + DAY_MS
            val dLogs = subLogs.filter { it.timestampMillis in pt.dayStartMillis until dEnd }
            if (dLogs.isNotEmpty()) {
                val firstLog = dLogs.first()
                val lastLog = dLogs.last()
                val calFirst = Calendar.getInstance().apply { timeInMillis = firstLog.timestampMillis }
                val calLast = Calendar.getInstance().apply { timeInMillis = lastLog.timestampMillis }
                val fHr = calFirst.get(Calendar.HOUR_OF_DAY) + calFirst.get(Calendar.MINUTE) / 60.0
                val lHr = calLast.get(Calendar.HOUR_OF_DAY) + calLast.get(Calendar.MINUTE) / 60.0
                firstHours.add(fHr)
                lastHours.add(lHr)
                firstHourVsDailyPoints.add(
                    ScatterDataPoint(
                        x = fHr.toFloat(),
                        y = pt.totalUnits.toFloat(),
                        label = pt.label,
                        secondaryLabel = "1. dávka v ${String.format(Locale.US, "%02d:%02d", calFirst.get(Calendar.HOUR_OF_DAY), calFirst.get(Calendar.MINUTE))} → Za den ${pt.totalUnits} ${substance.defaultUnit}",
                        colorHex = substance.colorHex,
                        timestampMillis = pt.dayStartMillis
                    )
                )
            }
        }

        val doseVsRatingPoints = subLogs.map { log ->
            ScatterDataPoint(
                x = log.doseAmount.toFloat(),
                y = log.subjectiveRating.toFloat(),
                label = "${log.subtypeName} (${dateFmt.format(Date(log.timestampMillis))})",
                secondaryLabel = "${log.doseAmount} ${substance.defaultUnit} → Hodnocení ${log.subjectiveRating}/10",
                colorHex = log.subtypeId?.let { subtypeMap[it]?.colorHex } ?: substance.colorHex,
                timestampMillis = log.timestampMillis
            )
        }

        val prevIntervalVsDoseRegression = computeLinearRegression(
            points = prevIntervalVsDosePoints,
            xNameCz = "Předchozí rozestup (h)",
            yNameCz = "Velikost dávky (${substance.defaultUnit})"
        )
        val doseVsNextIntervalRegression = computeLinearRegression(
            points = doseVsNextIntervalPoints,
            xNameCz = "Velikost dávky (${substance.defaultUnit})",
            yNameCz = "Následující rozestup (h)"
        )
        val firstDoseHourVsDailyUnitsRegression = computeLinearRegression(
            points = firstHourVsDailyPoints,
            xNameCz = "Hodina 1. dávky",
            yNameCz = "Denní součet (${substance.defaultUnit})"
        )
        val doseVsRatingRegression = computeLinearRegression(
            points = doseVsRatingPoints,
            xNameCz = "Dávka (${substance.defaultUnit})",
            yNameCz = "Subjektivní hodnocení (1-10)"
        )

        // Cross-Substance Correlations specifically with this substance
        val otherSubstances = allSubstances.filter { it.id != substance.id }
        val crossCorrelations = mutableListOf<SynergyPairCell>()
        for (other in otherSubstances) {
            var sharedDays = 0
            val aDaily = mutableListOf<Double>()
            val bDaily = mutableListOf<Double>()
            for (d in 0 until daysCount) {
                val dStart = windowStart + d * DAY_MS
                val dEnd = dStart + DAY_MS
                val aSum = subLogs.filter { it.timestampMillis in dStart until dEnd }.sumOf { it.doseAmount }
                val bSum = allLogs.filter { it.substanceId == other.id && it.timestampMillis in dStart until dEnd }.sumOf { it.doseAmount }
                if (aSum > 0 && bSum > 0) sharedDays++
                if (aSum > 0 || bSum > 0) {
                    aDaily.add(aSum)
                    bDaily.add(bSum)
                }
            }
            if (sharedDays > 0 || aDaily.size >= 3) {
                var r = 0.0
                if (aDaily.size >= 3) {
                    val mA = aDaily.average()
                    val mB = bDaily.average()
                    var num = 0.0
                    var dA = 0.0
                    var dB = 0.0
                    for (k in aDaily.indices) {
                        val da = aDaily[k] - mA
                        val db = bDaily[k] - mB
                        num += da * db
                        dA += da * da
                        dB += db * db
                    }
                    val den = sqrt(dA * dB)
                    if (den > 1e-9) r = (num / den).coerceIn(-1.0, 1.0)
                }
                val coPct = if (activeDaysCount > 0) ((sharedDays * 100.0) / activeDaysCount).roundToInt().coerceIn(0, 100) else 0
                if (sharedDays > 0 || abs(r) > 0.05) {
                    crossCorrelations.add(
                        SynergyPairCell(
                            subAId = substance.id,
                            subBId = other.id,
                            subAName = substance.name,
                            subBName = other.name,
                            subAColorHex = substance.colorHex,
                            subBColorHex = other.colorHex,
                            coOccurrencePercent = coPct,
                            sharedDaysCount = sharedDays,
                            pearsonDoseCorrelation = r
                        )
                    )
                }
            }
        }
        crossCorrelations.sortByDescending { it.coOccurrencePercent }

        // 24-Hour Circadian Distribution
        val hourlyUnits = FloatArray(24)
        val hourlyCounts = IntArray(24)
        subLogs.forEach { log ->
            val cal = Calendar.getInstance().apply { timeInMillis = log.timestampMillis }
            val hr = cal.get(Calendar.HOUR_OF_DAY).coerceIn(0, 23)
            hourlyUnits[hr] += log.doseAmount.toFloat()
            hourlyCounts[hr]++
        }

        // Dose Quantiles & Histogram Bins
        val minDose = singleDoseStats.min
        val p25Dose = singleDoseStats.p25
        val medianDose = singleDoseStats.median
        val meanDose = singleDoseStats.mean
        val p90Dose = singleDoseStats.p90
        val maxDose = singleDoseStats.max

        val doseHistogramBins = listOf(
            "Pod-prahová" to subLogs.count { it.doseAmount < substance.thresholdLight },
            "Malá" to subLogs.count { it.doseAmount in substance.thresholdLight..<substance.thresholdMedium },
            "Střední" to subLogs.count { it.doseAmount in substance.thresholdMedium..<substance.thresholdStrong },
            "Velká" to subLogs.count { it.doseAmount in substance.thresholdStrong..<substance.thresholdDangerous },
            "Redline+" to subLogs.count { it.doseAmount >= substance.thresholdDangerous }
        )

        val activeTiers = DoseTier.entries.filter { it != DoseTier.INACTIVE }
        val singleDoseTierCounts = activeTiers.associateWith { t ->
            subLogs.count { it.evaluatedTier == t.name }
        }
        val stackedDoseTierCounts = activeTiers.associateWith { t ->
            subLogs.count { it.stackedPeakTier == t.name }
        }

        // Stacking Amplification
        var stackedSessions = 0
        var totalShiftMins = 0
        var ampRatioSum = 0.0
        for (i in 1 until subLogs.size) {
            val prev = subLogs[i - 1]
            val curr = subLogs[i]
            val overlapMins = (prev.timestampMillis + substance.totalDurationMinutes * 60_000L - curr.timestampMillis) / 60_000L
            if (overlapMins > 15) {
                stackedSessions++
                totalShiftMins += (overlapMins * 0.42).roundToInt()
                ampRatioSum += 1.32
            }
        }
        val avgStackAmp = if (stackedSessions > 0) ampRatioSum / stackedSessions else 1.0
        val avgShiftMins = if (stackedSessions > 0) totalShiftMins / stackedSessions else 0

        // 7-Day Dual Simulation: Active Effect Curve vs Body Presence Curve (168 hourly points)
        val sim7dStart = nowMillis - 7 * DAY_MS
        val recent7dLogs = subLogsAllTime.filter { it.timestampMillis >= sim7dStart - DAY_MS }
        val simulated7dActiveEffectCurve = ArrayList<Float>(168)
        val simulated7dPlasmaCurve = ArrayList<Float>(168)
        val activeDoublePts = ArrayList<Double>(168)
        val bodyDoublePts = ArrayList<Double>(168)

        for (h in 0 until 168) {
            val t = sim7dStart + h * HOUR_MS
            val activeVal = PharmacokineticEngine.combinedEffectAt(t, recent7dLogs, substance, settings)
            val plasmaVal = PharmacokineticEngine.combinedPlasmaAt(t, recent7dLogs, substance, settings)
            simulated7dActiveEffectCurve.add(activeVal.toFloat())
            simulated7dPlasmaCurve.add(plasmaVal.toFloat())
            activeDoublePts.add(activeVal)
            bodyDoublePts.add(plasmaVal)
        }

        val activeEffectStats = computeSummaryStats(activeDoublePts)
        val bodyPresenceStats = computeSummaryStats(bodyDoublePts)

        // Tolerance & Receptor Saturation Index (30 days) using tunable parameters!
        val start30d = startOfToday - 29L * DAY_MS
        val toleranceCurve30d = ArrayList<Float>(30)
        var tolLevel = 0.0
        val safeRef = substance.dailyMaxSafeLimit.coerceAtLeast(1.0)
        val effectiveGainRate = settings.globalToleranceGainFactor * substance.toleranceGainRate
        val effectiveDecayPerDay = (1.0 - (settings.globalToleranceDecayPerDay * substance.toleranceDecayRate)).coerceIn(0.40, 0.98)

        for (d in 0 until 30) {
            val dStart = start30d + d * DAY_MS
            val dEnd = dStart + DAY_MS
            val dUnits = subLogsAllTime.filter { it.timestampMillis in dStart until dEnd }.sumOf { it.effectiveDoseAmount }
            tolLevel = tolLevel * effectiveDecayPerDay + (dUnits / safeRef) * effectiveGainRate
            tolLevel = tolLevel.coerceIn(0.0, 100.0)
            toleranceCurve30d.add(tolLevel.toFloat())
        }
        val currentTolerancePct = tolLevel.roundToInt().coerceIn(0, 100)
        val decayPerDayPct = ((1.0 - effectiveDecayPerDay) * 100.0).coerceAtLeast(2.0)
        val estimatedResetDays = (currentTolerancePct / decayPerDayPct).roundToInt().coerceIn(0, 30)

        // Sleep Hygiene Cutoff Compliance
        var sleepSafeDays = 0
        var sleepDisruptedDays = 0
        for (d in 0 until daysCount) {
            val dStart = windowStart + d * DAY_MS
            val cal = Calendar.getInstance().apply {
                timeInMillis = dStart
                set(Calendar.HOUR_OF_DAY, bedtimeHour24.coerceIn(0, 23))
                set(Calendar.MINUTE, 0)
            }
            val bedtimeMs = cal.timeInMillis
            val dayLogs = subLogsAllTime.filter {
                it.timestampMillis in (bedtimeMs - 24 * HOUR_MS)..bedtimeMs
            }
            if (dayLogs.isEmpty()) {
                sleepSafeDays++
            } else {
                val residualActiveAtBed = PharmacokineticEngine.combinedEffectAt(bedtimeMs, dayLogs, substance, settings)
                val residualPlasmaAtBed = PharmacokineticEngine.combinedPlasmaAt(bedtimeMs, dayLogs, substance, settings)
                if (max(residualActiveAtBed, residualPlasmaAtBed * 0.65) > substance.sleepDisruptionThreshold) {
                    sleepDisruptedDays++
                } else {
                    sleepSafeDays++
                }
            }
        }
        val sleepCompliancePct = ((sleepSafeDays * 100.0) / daysCount).roundToInt().coerceIn(0, 100)

        // Custom Field Breakdown with Units, Count, and Avg Rating
        val fieldMapUnits = mutableMapOf<String, MutableMap<String, Double>>()
        val fieldMapCounts = mutableMapOf<String, MutableMap<String, Int>>()
        val fieldMapRatings = mutableMapOf<String, MutableMap<String, MutableList<Int>>>()
        subLogs.forEach { log ->
            log.parsedCustomFields().forEach { (k, v) ->
                val uMap = fieldMapUnits.getOrPut(k) { mutableMapOf() }
                uMap[v] = (uMap[v] ?: 0.0) + log.doseAmount

                val cMap = fieldMapCounts.getOrPut(k) { mutableMapOf() }
                cMap[v] = (cMap[v] ?: 0) + 1

                val rMap = fieldMapRatings.getOrPut(k) { mutableMapOf() }
                rMap.getOrPut(v) { mutableListOf() }.add(log.subjectiveRating)
            }
        }
        val customFieldGroups = fieldMapUnits.map { (fieldName, valMap) ->
            val countMap = fieldMapCounts[fieldName] ?: emptyMap()
            val ratingMap = fieldMapRatings[fieldName] ?: emptyMap()
            CustomFieldAnalyticsGroup(
                fieldName = fieldName,
                valueBreakdown = valMap.entries.sortedByDescending { it.value }.map { it.key to it.value },
                valueCountBreakdown = countMap.entries.sortedByDescending { it.value }.map { it.key to it.value },
                valueAvgRatingBreakdown = ratingMap.entries.map { it.key to it.value.average() }
            )
        }

        // RoA Breakdown
        val roaMetrics = subLogs.groupBy { it.roaName.ifBlank { "Oral" } }
            .map { (roa, logs) ->
                RoaBreakdownMetric(
                    roaName = roa,
                    count = logs.size,
                    totalUnits = logs.sumOf { it.doseAmount },
                    shareFraction = (logs.size / totalSubCountDenom).coerceIn(0f, 1f),
                    avgOnsetMultiplier = logs.map { it.onsetMultiplier }.average()
                )
            }.sortedByDescending { it.count }

        return SubstanceDeepDiveReport(
            substance = substance,
            timeframeDays = daysCount,
            avgUnitsPerCalendarDay = avgUnitsCalendar,
            avgUnitsPerActiveDay = avgUnitsActive,
            avgSingleDoseUnits = avgSingleDose,
            peakDayUnits = peakDayUnits,
            totalUnitsConsumed = totalUnits,
            totalEffectiveUnitsConsumed = totalEffectiveUnits,
            totalSpend = totalSpend,
            totalDosesCount = totalDoses,
            activeDaysCount = activeDaysCount,
            dailyUnitStats = dailyUnitStats,
            singleDoseStats = singleDoseStats,
            dailyUnitPoints = dailyUnitPoints,
            cumulativeUnitsCurve = cumulativeUnitsCurve,
            cumulativeCostCurve = cumulativeCostCurve,
            projected30dUnits = projected30dUnits,
            projected30dCost = projected30dCost,
            weekdayAvgUnits = weekdayAvgUnits,
            weekdayMaxUnits = weekdayMaxUnits,
            weekdayMinActiveUnits = weekdayMinActiveUnits,
            subtypeMetrics = subtypeMetrics,
            dailySubtypeTimeline = dailySubtypeTimeline,
            prevIntervalVsDoseRegression = prevIntervalVsDoseRegression,
            doseVsNextIntervalRegression = doseVsNextIntervalRegression,
            firstDoseHourVsDailyUnitsRegression = firstDoseHourVsDailyUnitsRegression,
            doseVsRatingRegression = doseVsRatingRegression,
            crossSubstanceCorrelations = crossCorrelations,
            intervalBuckets7 = intervalBuckets.toList(),
            intervalStatsHours = intervalStatsHours,
            intervalEvolution = intervalEvolution,
            medianIntervalHours = medianIntervalHours,
            meanIntervalHours = meanIntervalHours,
            compulsiveRedosingIndexPercent = compulsiveRedosingIndexPercent,
            topAbstinenceStreaksHours = topAbstinenceStreaksHours,
            hourlyUnitDistribution24 = hourlyUnits.toList(),
            hourlyCountDistribution24 = hourlyCounts.toList(),
            avgFirstDoseHour = if (firstHours.isNotEmpty()) firstHours.average() else 9.0,
            avgLastDoseHour = if (lastHours.isNotEmpty()) lastHours.average() else 18.0,
            minDose = minDose,
            p25Dose = p25Dose,
            medianDose = medianDose,
            meanDose = meanDose,
            p90Dose = p90Dose,
            maxDose = maxDose,
            doseHistogramBins = doseHistogramBins,
            singleDoseTierCounts = singleDoseTierCounts,
            stackedDoseTierCounts = stackedDoseTierCounts,
            stackedSessionsCount = stackedSessions,
            avgStackAmplificationRatio = avgStackAmp,
            avgPeakShiftMinutes = avgShiftMins,
            simulated7dActiveEffectCurve = simulated7dActiveEffectCurve,
            simulated7dPlasmaCurve = simulated7dPlasmaCurve,
            activeEffectStats = activeEffectStats,
            bodyPresenceStats = bodyPresenceStats,
            currentToleranceIndexPercent = currentTolerancePct,
            estimatedDaysToBaselineReset = estimatedResetDays,
            toleranceCurve30d = toleranceCurve30d,
            sleepSafeDaysCount = sleepSafeDays,
            sleepDisruptedDaysCount = sleepDisruptedDays,
            sleepCompliancePercent = sleepCompliancePct,
            customFieldGroups = customFieldGroups,
            roaMetrics = roaMetrics,
            recentLogs = subLogs.sortedByDescending { it.timestampMillis }
        )
    }

    fun startOfDayMillis(timestampMillis: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = timestampMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun hourOfDayFloat(timestampMillis: Long): Float {
        val cal = Calendar.getInstance().apply { timeInMillis = timestampMillis }
        val h = cal.get(Calendar.HOUR_OF_DAY)
        val m = cal.get(Calendar.MINUTE)
        return h + (m / 60f)
    }
}
