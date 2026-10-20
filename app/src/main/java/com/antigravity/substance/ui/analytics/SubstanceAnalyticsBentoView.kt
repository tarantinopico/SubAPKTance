package com.antigravity.substance.ui.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.ScatterPlot
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.StackedLineChart
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.substance.data.db.entities.IntakeLogEntity
import com.antigravity.substance.data.preferences.UserSettings
import com.antigravity.substance.domain.engine.SubstanceDeepDiveReport
import com.antigravity.substance.ui.analytics.charts.DailyUnitConsumptionBarChartCanvas
import com.antigravity.substance.ui.analytics.charts.DualContinuousCurveCanvas
import com.antigravity.substance.ui.analytics.charts.InteractiveScatterRegressionChart
import com.antigravity.substance.ui.analytics.charts.IntervalEvolutionChart
import com.antigravity.substance.ui.analytics.charts.SubtypeStackedTimelineChart
import com.antigravity.substance.ui.analytics.charts.ThresholdTierHorizontalStackedBar
import com.antigravity.substance.ui.theme.BentoMicroHeader
import com.antigravity.substance.ui.theme.ChartStatsSummaryStrip
import com.antigravity.substance.ui.theme.FrostedBentoCard
import com.antigravity.substance.ui.theme.LocalSubstanceGlass
import com.antigravity.substance.ui.theme.RobotoMonoFontFamily
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * MODE B: PER-SUBSTANCE DEEP DIVE ANALYTICS (5 Sub-Pages, 24+ Visualizations)
 * Sub-Pages:
 * 0 = 1. Přehled & Spotřeba (Kvarteto jednotek, Denní sloupcový graf s Min/Avg/Max a inspektorem, Kumulativní vývoj + 30D predikce, Dny v týdnu, Kvantily dávek)
 * 1 = 2. Druhy této látky (Podíl poddruhů na spotřebě, Denní skládaný graf druhů v čase, Srovnání síly/ceny/hodnocení druhů, Cesty podání)
 * 2 = 3. Korelace & Závislosti (Závislost velikosti dávky na předchozím rozestupu, Závislost další pauzy na dávce, Čas 1. dávky vs denní součet, Dávka vs Hodnocení, Korelace s jinými látkami, Vlastní pole)
 * 3 = 4. Rozestupy & Rytmus (Histogram rozestupů + kompulzivní index, Chronologický vývoj rozestupů v čase, 24h cirkadiánní profil, Nejdelší pauzy bez látky)
 * 4 = 5. PK, Tělo & Historie (7D simulace: Aktivní účinek vs Doba v těle, Prahové zóny Single vs Stacked, Vrstvení dávek, Křivka tolerance, Spánková hygiena, Historie záznamů)
 * 5 = Vše (Kompletní přehled všech 24 karet pod sebou)
 */
@Composable
fun SubstanceAnalyticsBentoView(
    report: SubstanceDeepDiveReport,
    settings: UserSettings,
    selectedSubPage: Int = 0,
    onSelectSubPage: (Int) -> Unit = {},
    onDuplicateLog: (IntakeLogEntity) -> Unit,
    onDeleteLog: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val glassEnv = LocalSubstanceGlass.current
    val sub = report.substance
    val unit = sub.defaultUnit
    val primaryColor = Color(sub.colorHex)
    var selectedSubtypeFilter by remember(sub.id) { mutableStateOf<String?>(null) }

    val showOverview = selectedSubPage == 0 || selectedSubPage == 5
    val showSubtypes = selectedSubPage == 1 || selectedSubPage == 5
    val showCorrelations = selectedSubPage == 2 || selectedSubPage == 5
    val showIntervals = selectedSubPage == 3 || selectedSubPage == 5
    val showPkBody = selectedSubPage == 4 || selectedSubPage == 5

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .testTag("substance_deep_dive_list"),
        contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 4.dp, bottom = 92.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // =========================================================================
        // SUB-PAGE 1: SPOTŘEBA & JEDNOTKY
        // =========================================================================
        if (showOverview) {
            // 1. Unit Velocity Quartet & Summary Stats
            item {
                FrostedBentoCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = primaryColor
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        BentoMicroHeader(
                            title = "1. SOUHRN SPOTŘEBY V JEDNOTKÁCH (${sub.name} • $unit)",
                            icon = Icons.Default.Speed,
                            accentColor = primaryColor,
                            trailingText = "${report.totalDosesCount} DÁVEK (${report.activeDaysCount}/${report.timeframeDays} DNÍ)"
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            SubMetricCell(
                                label = "Ø / KALENDÁŘNÍ DEN",
                                value = settings.formatDose(report.avgUnitsPerCalendarDay, unit),
                                sub = "Včetně čistých dnů",
                                color = primaryColor,
                                modifier = Modifier.weight(1f)
                            )
                            SubMetricCell(
                                label = "Ø / AKTIVNÍ DEN",
                                value = settings.formatDose(report.avgUnitsPerActiveDay, unit),
                                sub = "Dny s užitím",
                                color = Color(0xFF10B981),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            SubMetricCell(
                                label = "PRŮMĚRNÁ 1 DÁVKA",
                                value = settings.formatDose(report.avgSingleDoseUnits, unit),
                                sub = "Medián: ${settings.formatDose(report.medianDose, unit)}",
                                color = Color(0xFFF59E0B),
                                modifier = Modifier.weight(1f)
                            )
                            SubMetricCell(
                                label = "MAXIMÁLNÍ DEN",
                                value = settings.formatDose(report.peakDayUnits, unit),
                                sub = "Limit: ${settings.formatDose(sub.dailyMaxSafeLimit, unit)}/d",
                                color = if (report.peakDayUnits > sub.dailyMaxSafeLimit) Color(0xFFEF4444) else Color(0xFF38BDF8),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        ChartStatsSummaryStrip(
                            stats = report.dailyUnitStats,
                            unit = unit,
                            decimals = 1,
                            accentColor = primaryColor
                        )
                    }
                }
            }

            // 2. Interactive Daily Consumption in Units + 7-Day Moving Average + Safe Limit Line
            item {
                FrostedBentoCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = primaryColor
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        BentoMicroHeader(
                            title = "2. DENNÍ SPOTŘEBA ($unit) + 7D KLOUZAVÝ PRŮMĚR (KLIKNI NA SLOUPEC)",
                            icon = Icons.Default.BarChart,
                            accentColor = primaryColor,
                            trailingText = "MAX LIMIT ${settings.formatDose(sub.dailyMaxSafeLimit, unit)}"
                        )
                        DailyUnitConsumptionBarChartCanvas(
                            bars = report.dailyUnitPoints,
                            dailySafeLimit = sub.dailyMaxSafeLimit,
                            unit = unit,
                            primaryColor = primaryColor,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(132.dp)
                        )
                    }
                }
            }

            // 3. Cumulative Unit & Cost Burn-Up + 30-Day Projection Cone
            item {
                FrostedBentoCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = Color(0xFF38BDF8)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        BentoMicroHeader(
                            title = "3. KUMULATIVNÍ RŮST SPOTŘEBY & 30DENNÍ PREDIKCE",
                            icon = Icons.Default.StackedLineChart,
                            accentColor = Color(0xFF38BDF8),
                            trailingText = "CELKEM ${settings.formatDose(report.totalUnitsConsumed, unit)}"
                        )
                        DualContinuousCurveCanvas(
                            primaryPoints = report.cumulativeUnitsCurve,
                            secondaryPoints = report.cumulativeCostCurve,
                            primaryColor = primaryColor,
                            secondaryColor = Color(0xFFF59E0B),
                            primaryLabel = "Kumulativně",
                            secondaryLabel = "Náklady",
                            unit = unit,
                            xStepLabelPrefix = "Den",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "30D predikce: ${settings.formatDose(report.projected30dUnits, unit)}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                color = primaryColor
                            )
                            Text(
                                text = "30D odhad ceny: ${settings.formatCurrency(report.projected30dCost)}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                color = Color(0xFFF59E0B)
                            )
                        }
                    }
                }
            }

            // 4. Day-of-Week Unit Profile (Mon-Sun with Min, Avg, Max)
            item {
                FrostedBentoCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = Color(0xFFF59E0B)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        BentoMicroHeader(
                            title = "4. PROFIL DNŮ V TÝDNU (MIN / PRŮMĚR / MAX V $unit)",
                            icon = Icons.Default.DateRange,
                            accentColor = Color(0xFFF59E0B),
                            trailingText = "PO – NE"
                        )
                        val maxW = (report.weekdayMaxUnits.maxOrNull() ?: 1f).coerceAtLeast(0.1f)
                        val days = listOf("Po", "Út", "St", "Čt", "Pá", "So", "Ne")
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(104.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            report.weekdayAvgUnits.forEachIndexed { idx, avgV ->
                                val maxV = report.weekdayMaxUnits.getOrElse(idx) { avgV }
                                val minV = report.weekdayMinActiveUnits.getOrElse(idx) { 0f }
                                val avgFrac = (avgV / maxW).coerceIn(0.06f, 1f)
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Bottom,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = String.format(Locale.US, "%.1f", avgV),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                                        color = primaryColor
                                    )
                                    Text(
                                        text = "▲${String.format(Locale.US, "%.0f", maxV)}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp),
                                        color = glassEnv.textMuted
                                    )
                                    Box(
                                        modifier = Modifier
                                            .width(15.dp)
                                            .height((56 * avgFrac).dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (idx >= 5) Color(0xFFF59E0B) else primaryColor)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = days[idx],
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                                        color = glassEnv.textPrimary
                                    )
                                    Text(
                                        text = "▼${String.format(Locale.US, "%.0f", minV)}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp),
                                        color = Color(0xFF10B981)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 5. Dose Size Quantiles & Distribution Histogram
            item {
                FrostedBentoCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = Color(0xFF10B981)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        BentoMicroHeader(
                            title = "5. KVANTILY A ROZLOŽENÍ JEDNOTLIVÝCH DÁVEK ($unit)",
                            icon = Icons.Default.Equalizer,
                            accentColor = Color(0xFF10B981),
                            trailingText = "SD ±${settings.formatDose(report.singleDoseStats.stdDev, unit)}"
                        )
                        ChartStatsSummaryStrip(
                            stats = report.singleDoseStats,
                            unit = unit,
                            decimals = 1,
                            accentColor = Color(0xFF10B981)
                        )
                        val maxBin = (report.doseHistogramBins.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            report.doseHistogramBins.forEach { (binLabel, cnt) ->
                                val f = (cnt.toFloat() / maxBin).coerceIn(0.04f, 1f)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = binLabel,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        color = glassEnv.textSecondary,
                                        modifier = Modifier.width(68.dp)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(7.dp)
                                            .clip(RoundedCornerShape(50))
                                            .background(glassEnv.cardBorderBase)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(f)
                                                .fillMaxHeight()
                                                .background(primaryColor)
                                        )
                                    }
                                    Text(
                                        text = "${cnt}×",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        color = glassEnv.textPrimary,
                                        modifier = Modifier.width(28.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // SUB-PAGE 2: DRUHY TÉTO LÁTKY (SUBTYPES DEEP DIVE)
        // =========================================================================
        if (showSubtypes) {
            // 6. Subtypes Share & Complete Stats Table
            item {
                FrostedBentoCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = primaryColor
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        BentoMicroHeader(
                            title = "6. PODDRUHY / VARIANTY LÁTKY ${sub.name.uppercase()}",
                            icon = Icons.Default.Category,
                            accentColor = primaryColor,
                            trailingText = "${report.subtypeMetrics.size} DRUHŮ"
                        )
                        if (report.subtypeMetrics.isEmpty()) {
                            Text(
                                text = "Žádné záznamy pro druhy této látky.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = glassEnv.textSecondary
                            )
                        } else {
                            report.subtypeMetrics.forEach { sm ->
                                val col = Color(sm.colorHex)
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(col.copy(alpha = 0.09f))
                                        .border(1.dp, col.copy(alpha = 0.32f), RoundedCornerShape(10.dp))
                                        .padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(9.dp)
                                                    .clip(CircleShape)
                                                    .background(col)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = sm.subtypeName,
                                                style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Bold),
                                                color = glassEnv.textPrimary
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Síla ${String.format(Locale.US, "%.2f", sm.potencyMultiplier)}×",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                color = col
                                            )
                                        }
                                        Text(
                                            text = "${(sm.shareOfUnitsFraction * 100).roundToInt()}% (${settings.formatDose(sm.totalUnits, unit)})",
                                            style = MaterialTheme.typography.labelLarge.copy(fontSize = 10.5.sp),
                                            color = col
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Min: ${settings.formatDose(sm.doseStats.min)} • Ø: ${settings.formatDose(sm.doseStats.mean)} • Med: ${settings.formatDose(sm.doseStats.median)} • Max: ${settings.formatDose(sm.doseStats.max)} $unit",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.8.sp),
                                            color = glassEnv.textSecondary
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${sm.doseCount}× dávka • Efektivně: ${settings.formatDose(sm.totalEffectiveUnits, unit)}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.8.sp),
                                            color = glassEnv.textSecondary
                                        )
                                        Text(
                                            text = "${settings.formatCurrency(sm.totalSpend)} (Ø ${settings.formatCurrency(sm.avgCostPerDose)}/dávka) • ★${String.format(Locale.US, "%.1f", sm.avgRating)}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.8.sp, fontWeight = FontWeight.Bold),
                                            color = glassEnv.textPrimary
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(5.dp)
                                            .clip(RoundedCornerShape(50))
                                            .background(glassEnv.cardBorderBase)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(sm.shareOfUnitsFraction.coerceIn(0.04f, 1f))
                                                .fillMaxHeight()
                                                .background(col)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 7. Daily Stacked Subtype Timeline with Point Inspector
            item {
                FrostedBentoCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = Color(0xFF10B981)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        BentoMicroHeader(
                            title = "7. VÝVOJ SPOTŘEBY PODLE DRUHŮ V ČASE (KLIKNI NA DEN)",
                            icon = Icons.Default.BarChart,
                            accentColor = Color(0xFF10B981),
                            trailingText = "SKLÁDANÝ GRAF DRUHŮ"
                        )
                        SubtypeStackedTimelineChart(
                            dailyPoints = report.dailySubtypeTimeline,
                            subtypes = report.subtypeMetrics,
                            unit = unit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(125.dp)
                        )
                    }
                }
            }

            // 8. RoA Breakdown for this Substance
            item {
                FrostedBentoCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = Color(0xFF14B8A6)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        BentoMicroHeader(
                            title = "8. CESTY PODÁNÍ (RoA) U TÉTO LÁTKY",
                            icon = Icons.Default.Route,
                            accentColor = Color(0xFF14B8A6)
                        )
                        report.roaMetrics.forEach { roa ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${roa.roaName} (nástup ${String.format(Locale.US, "%.2f", roa.avgOnsetMultiplier)}×)",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = glassEnv.textPrimary
                                )
                                Text(
                                    text = "${settings.formatDose(roa.totalUnits, unit)} (${(roa.shareFraction * 100).roundToInt()}% • ${roa.count}×)",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                    color = Color(0xFF14B8A6)
                                )
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // SUB-PAGE 3: KORELACE & ZÁVISLOSTI TÉTO LÁTKY
        // =========================================================================
        if (showCorrelations) {
            // 9. Dose Size vs Previous Interval Correlation (Závislost velikosti dávky na předchozím rozestupu)
            item {
                FrostedBentoCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = primaryColor
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        BentoMicroHeader(
                            title = "9. ZÁVISLOST VELIKOSTI DÁVKY NA PŘEDCHOZÍM ROZESTUPU",
                            icon = Icons.Default.ScatterPlot,
                            accentColor = primaryColor,
                            trailingText = "r = ${String.format(Locale.US, "%+.2f", report.prevIntervalVsDoseRegression.pearsonR)}"
                        )
                        InteractiveScatterRegressionChart(
                            regression = report.prevIntervalVsDoseRegression,
                            xAxisLabel = "Předchozí pauza (h)",
                            yAxisLabel = "Velikost dávky ($unit)",
                            accentColor = primaryColor,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(145.dp)
                        )
                    }
                }
            }

            // 10. Subsequent Interval vs Previous Dose Size (Závislost délky další pauzy na velikosti dávky)
            item {
                FrostedBentoCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = Color(0xFF38BDF8)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        BentoMicroHeader(
                            title = "10. ZÁVISLOST DÉLKY DALŠÍ PAUZY NA VELIKOSTI PŘEDCHOZÍ DÁVKY",
                            icon = Icons.AutoMirrored.Filled.CompareArrows,
                            accentColor = Color(0xFF38BDF8),
                            trailingText = "r = ${String.format(Locale.US, "%+.2f", report.doseVsNextIntervalRegression.pearsonR)}"
                        )
                        InteractiveScatterRegressionChart(
                            regression = report.doseVsNextIntervalRegression,
                            xAxisLabel = "Předchozí dávka ($unit)",
                            yAxisLabel = "Následující pauza (h)",
                            accentColor = Color(0xFF38BDF8),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                        )
                    }
                }
            }

            // 11. First-Dose Hour vs Total Daily Intake
            item {
                FrostedBentoCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = Color(0xFFF59E0B)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        BentoMicroHeader(
                            title = "11. VLIV ČASU PRVNÍ RANNÍ DÁVKY NA CELKOVOU DENNÍ SPOTŘEBU",
                            icon = Icons.Default.AutoGraph,
                            accentColor = Color(0xFFF59E0B),
                            trailingText = "r = ${String.format(Locale.US, "%+.2f", report.firstDoseHourVsDailyUnitsRegression.pearsonR)}"
                        )
                        InteractiveScatterRegressionChart(
                            regression = report.firstDoseHourVsDailyUnitsRegression,
                            xAxisLabel = "Hodina 1. dávky (0-24h)",
                            yAxisLabel = "Celkem za den ($unit)",
                            accentColor = Color(0xFFF59E0B),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(135.dp)
                        )
                    }
                }
            }

            // 12. Dose Size vs Subjective Rating
            item {
                FrostedBentoCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = Color(0xFF10B981)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        BentoMicroHeader(
                            title = "12. KORELACE VELIKOSTI DÁVKY A SUBJEKTIVNÍHO HODNOCENÍ (1-10)",
                            icon = Icons.Default.Analytics,
                            accentColor = Color(0xFF10B981),
                            trailingText = "r = ${String.format(Locale.US, "%+.2f", report.doseVsRatingRegression.pearsonR)}"
                        )
                        InteractiveScatterRegressionChart(
                            regression = report.doseVsRatingRegression,
                            xAxisLabel = "Dávka ($unit)",
                            yAxisLabel = "Hodnocení (1-10)",
                            accentColor = Color(0xFF10B981),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                        )
                    }
                }
            }

            // 13. Cross-Substance Correlations with this Substance
            item {
                FrostedBentoCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = Color(0xFFEC4899)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        BentoMicroHeader(
                            title = "13. KORELACE ${sub.name.uppercase()} S OSTATNÍMI LÁTKAMI",
                            icon = Icons.Default.Hub,
                            accentColor = Color(0xFFEC4899)
                        )
                        if (report.crossSubstanceCorrelations.isEmpty()) {
                            Text(
                                text = "Žádné souběžné užití s jinými látkami.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = glassEnv.textSecondary
                            )
                        } else {
                            report.crossSubstanceCorrelations.forEach { pair ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${pair.subAName} + ${pair.subBName}",
                                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 10.5.sp),
                                        color = glassEnv.textPrimary
                                    )
                                    Text(
                                        text = "Souběh ${pair.coOccurrencePercent}% (${pair.sharedDaysCount}d) • r = ${String.format(Locale.US, "%+.2f", pair.pearsonDoseCorrelation)}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold),
                                        color = Color(pair.subBColorHex)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 14. Custom Fields Correlation & Breakdown
            if (report.customFieldGroups.isNotEmpty()) {
                item {
                    FrostedBentoCard(
                        modifier = Modifier.fillMaxWidth(),
                        accentColor = Color(0xFF8B5CF6)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            BentoMicroHeader(
                                title = "14. ANALÝZA VLASTNÍCH POLÍ (CUSTOM FIELDS)",
                                icon = Icons.Default.Tune,
                                accentColor = Color(0xFF8B5CF6)
                            )
                            report.customFieldGroups.forEach { group ->
                                Text(
                                    text = group.fieldName.uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.5.sp,
                                        color = Color(0xFF8B5CF6),
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                val ratingMap = group.valueAvgRatingBreakdown.toMap()
                                val countMap = group.valueCountBreakdown.toMap()
                                group.valueBreakdown.take(5).forEach { (valName, valUnits) ->
                                    val cnt = countMap[valName] ?: 1
                                    val avgRat = ratingMap[valName] ?: 8.0
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "$valName (${cnt}×)",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                            color = glassEnv.textPrimary
                                        )
                                        Text(
                                            text = "${settings.formatDose(valUnits, unit)} • ★${String.format(Locale.US, "%.1f", avgRat)}/10",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                            color = primaryColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // SUB-PAGE 4: ROZESTUPY & RYTMUS TÉTO LÁTKY
        // =========================================================================
        if (showIntervals) {
            // 15. Inter-Dose Interval Histogram + Compulsive Redosing Index + Full Stats
            item {
                FrostedBentoCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = Color(0xFF06B6D4)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        BentoMicroHeader(
                            title = "15. DISTRIBUCE ROZESTUPŮ MEZI DÁVKAMI (${sub.name})",
                            icon = Icons.Default.AccessTime,
                            accentColor = Color(0xFF06B6D4),
                            trailingText = "KOMPULZIVNÍ INDEX ${report.compulsiveRedosingIndexPercent}%"
                        )
                        ChartStatsSummaryStrip(
                            stats = report.intervalStatsHours,
                            unit = "h",
                            decimals = 1,
                            accentColor = Color(0xFF06B6D4)
                        )
                        val bucketLabels = listOf("<1h", "1-2h", "2-4h", "4-8h", "8-16h", "16-24h", ">24h")
                        val maxBucket = (report.intervalBuckets7.maxOrNull() ?: 1).coerceAtLeast(1)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(86.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            report.intervalBuckets7.forEachIndexed { idx, cnt ->
                                val frac = (cnt.toFloat() / maxBucket).coerceIn(0.08f, 1f)
                                val barCol = when {
                                    idx <= 1 -> Color(0xFFEF4444)
                                    idx == 2 -> Color(0xFFF59E0B)
                                    else -> Color(0xFF10B981)
                                }
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Bottom
                                ) {
                                    Text(
                                        text = "$cnt",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                                        color = glassEnv.textSecondary
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.72f)
                                            .height((56 * frac).dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(barCol)
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = bucketLabels[idx],
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                                        color = glassEnv.textPrimary
                                    )
                                }
                            }
                        }
                        Text(
                            text = "Doporučený min. rozestup: ${String.format(Locale.US, "%.1f", sub.recommendedMinIntervalMinutes / 60.0)}h • Průměr: ${String.format(Locale.US, "%.1f", report.meanIntervalHours)}h • Medián: ${String.format(Locale.US, "%.1f", report.medianIntervalHours)}h",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = glassEnv.textSecondary
                        )
                    }
                }
            }

            // 16. Chronological Inter-Dose Interval Evolution Curve
            item {
                FrostedBentoCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = primaryColor
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        BentoMicroHeader(
                            title = "16. CHRONOLOGICKÝ VÝVOJ ROZESTUPŮ V ČASE (KLIKNI NA MEZERU)",
                            icon = Icons.Default.Timeline,
                            accentColor = primaryColor,
                            trailingText = "KLOUZAVÝ PRŮMĚR 5 PAUZ"
                        )
                        IntervalEvolutionChart(
                            points = report.intervalEvolution,
                            recommendedMinHours = sub.recommendedMinIntervalMinutes / 60.0,
                            accentColor = primaryColor,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                        )
                    }
                }
            }

            // 17. 24-Hour Circadian Unit Distribution
            item {
                FrostedBentoCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = Color(0xFF38BDF8)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        BentoMicroHeader(
                            title = "17. 24HODINOVÝ CIRKADIÁNNÍ PROFIL SPOTŘEBY ($unit)",
                            icon = Icons.Default.AccessTime,
                            accentColor = Color(0xFF38BDF8),
                            trailingText = "1. DÁVKA Ø ${formatDecimalHour(report.avgFirstDoseHour)} • POSL. Ø ${formatDecimalHour(report.avgLastDoseHour)}"
                        )
                        val maxHrU = (report.hourlyUnitDistribution24.maxOrNull() ?: 1f).coerceAtLeast(0.1f)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            report.hourlyUnitDistribution24.forEachIndexed { hr, u ->
                                val f = (u / maxHrU).coerceIn(0.06f, 1f)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height((56 * f).dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(if (u > 0f) primaryColor else glassEnv.cardBorderBase)
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            listOf("00:00", "06:00", "12:00", "18:00", "23:00").forEach { lbl ->
                                Text(
                                    text = lbl,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                                    color = glassEnv.textSecondary
                                )
                            }
                        }
                    }
                }
            }

            // 18. Top Historical Abstinence Pauses for this Substance
            item {
                FrostedBentoCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = Color(0xFF10B981)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        BentoMicroHeader(
                            title = "18. NEJDELŠÍ PAUZY BEZ LÁTKY ${sub.name.uppercase()}",
                            icon = Icons.Default.EmojiEvents,
                            accentColor = Color(0xFF10B981)
                        )
                        val maxStreak = (report.topAbstinenceStreaksHours.maxOfOrNull { it.second } ?: 24f).coerceAtLeast(1f)
                        report.topAbstinenceStreaksHours.forEach { (periodLabel, hrs) ->
                            val frac = (hrs / maxStreak).coerceIn(0.05f, 1f)
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = periodLabel,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                        color = glassEnv.textPrimary
                                    )
                                    Text(
                                        text = if (hrs >= 48f) String.format(Locale.US, "%.1f dní (%.0fh)", hrs / 24f, hrs)
                                        else String.format(Locale.US, "%.1f hodin", hrs),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold),
                                        color = Color(0xFF10B981)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(5.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(glassEnv.cardBorderBase)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(frac)
                                            .fillMaxHeight()
                                            .background(Color(0xFF10B981))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // SUB-PAGE 5: FARMAKOKINETIKA, TĚLO & HISTORIE ZÁZNAMŮ
        // =========================================================================
        if (showPkBody) {
            // 19. 7-Day Dual Simulation: Active Effect Duration vs Body Presence Duration
            item {
                FrostedBentoCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = Color(0xFF8B5CF6)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val timingBadge = when {
                            !sub.isTimingEnabled -> "ČASOVÁNÍ VYPNUTO"
                            sub.hasBodyPresenceDuration -> "ÚČINEK ${sub.totalDurationMinutes / 60}h • V TĚLE ${String.format(Locale.US, "%.0f", sub.bodyPresenceHours)}h"
                            else -> "POUZE DOBA ÚČINKU (${sub.totalDurationMinutes / 60}h)"
                        }
                        BentoMicroHeader(
                            title = "19. 7DENNÍ SIMULACE: HLAVNÍ DOBA ÚČINKU VS DOBA V TĚLE",
                            icon = Icons.Default.Timeline,
                            accentColor = Color(0xFF8B5CF6),
                            trailingText = timingBadge
                        )
                        if (!sub.isTimingEnabled) {
                            Text(
                                text = "Časové modelování je u této látky vypnuté (lze zapnout v Editoru látky -> Doba účinku & Doba v těle).",
                                style = MaterialTheme.typography.bodyMedium,
                                color = glassEnv.textSecondary
                            )
                        } else {
                            ChartStatsSummaryStrip(
                                stats = report.activeEffectStats,
                                unit = unit,
                                decimals = 1,
                                accentColor = primaryColor
                            )
                            DualContinuousCurveCanvas(
                                primaryPoints = report.simulated7dActiveEffectCurve,
                                secondaryPoints = if (sub.hasBodyPresenceDuration) report.simulated7dPlasmaCurve else null,
                                primaryColor = primaryColor,
                                secondaryColor = Color(0xFF8B5CF6),
                                primaryLabel = "Aktivní účinek",
                                secondaryLabel = "V těle (Eliminace)",
                                unit = unit,
                                xStepLabelPrefix = "Hodina",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(115.dp)
                            )
                        }
                    }
                }
            }

            // 20 & 21. Threshold Zone Breakdown (Single vs Stacked) + Stacking Amplification
            item {
                FrostedBentoCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = Color(0xFFF97316)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        BentoMicroHeader(
                            title = "20. PRAHOVÉ ZÓNY: SAMOSTATNÉ DÁVKY VS NAVRSTVENÉ ŠPIČKY",
                            icon = Icons.Default.Layers,
                            accentColor = Color(0xFFF97316),
                            trailingText = "${report.stackedSessionsCount}× VRSTVENÍ (Ø +${report.avgPeakShiftMinutes}m)"
                        )
                        Text(
                            text = "Samostatné dávky (Single Dose Evaluation):",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = glassEnv.textSecondary
                        )
                        ThresholdTierHorizontalStackedBar(
                            tierCounts = report.singleDoseTierCounts,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "Navrstvené špičky po superpozici (Multi-Dose Stacked Peak):",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = glassEnv.textSecondary
                        )
                        ThresholdTierHorizontalStackedBar(
                            tierCounts = report.stackedDoseTierCounts,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // 22 & 23. Tolerance & Receptor Saturation Index + Sleep Hygiene Compliance
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FrostedBentoCard(
                        modifier = Modifier.weight(1.25f),
                        accentColor = Color(0xFFEC4899)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            BentoMicroHeader(
                                title = "21. TOLERANCE & RECEPTORY",
                                icon = Icons.Default.Psychology,
                                accentColor = Color(0xFFEC4899),
                                trailingText = "${report.currentToleranceIndexPercent}%"
                            )
                            DualContinuousCurveCanvas(
                                primaryPoints = report.toleranceCurve30d,
                                primaryColor = Color(0xFFEC4899),
                                primaryLabel = "Tolerance",
                                unit = "%",
                                xStepLabelPrefix = "Den",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(74.dp)
                            )
                            Text(
                                text = "Reset na baseline: ~${report.estimatedDaysToBaselineReset} čistých dnů (koef. ${sub.toleranceGainRate}× / ${sub.toleranceDecayRate}×)",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                                color = glassEnv.textSecondary
                            )
                        }
                    }

                    FrostedBentoCard(
                        modifier = Modifier.weight(1f),
                        accentColor = Color(0xFF38BDF8)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            BentoMicroHeader(
                                title = "22. SPÁNKOVÁ HYGIENA",
                                icon = Icons.Default.Bedtime,
                                accentColor = Color(0xFF38BDF8)
                            )
                            Text(
                                text = "${report.sleepCompliancePercent}%",
                                style = MaterialTheme.typography.displayMedium.copy(
                                    fontFamily = RobotoMonoFontFamily,
                                    fontSize = 24.sp
                                ),
                                color = if (report.sleepCompliancePercent >= 75) Color(0xFF10B981) else Color(0xFFF59E0B)
                            )
                            Text(
                                text = "${report.sleepSafeDaysCount} čistých večerů / ${report.sleepDisruptedDaysCount} narušených",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = glassEnv.textPrimary
                            )
                            Text(
                                text = "Večerka ${settings.bedtimeHour24}:00 • Práh <${settings.formatDose(sub.sleepDisruptionThreshold, unit)}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                                color = glassEnv.textSecondary
                            )
                        }
                    }
                }
            }

            // 24. Per-Substance Log History Table with Subtype Filter
            item {
                FrostedBentoCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = primaryColor
                ) {
                    val fmt = remember { SimpleDateFormat("dd.MM. HH:mm", Locale.getDefault()) }
                    val filteredLogs = remember(report.recentLogs, selectedSubtypeFilter) {
                        if (selectedSubtypeFilter == null) report.recentLogs
                        else report.recentLogs.filter { it.subtypeName.equals(selectedSubtypeFilter, ignoreCase = true) }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        BentoMicroHeader(
                            title = "23. HISTORIE ZÁZNAMŮ TÉTO LÁTKY (FILTR PODLE DRUHU)",
                            icon = Icons.Default.History,
                            accentColor = primaryColor,
                            trailingText = "${filteredLogs.size} ZÁZNAMŮ"
                        )

                        // Subtype Filter Chips
                        if (report.subtypeMetrics.size > 1) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                val allActive = selectedSubtypeFilter == null
                                Text(
                                    text = "Všechny druhy",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    color = if (allActive) primaryColor else glassEnv.textSecondary,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (allActive) primaryColor.copy(alpha = 0.16f) else Color.Transparent)
                                        .border(1.dp, if (allActive) primaryColor else glassEnv.cardBorderBase, RoundedCornerShape(6.dp))
                                        .clickable { selectedSubtypeFilter = null }
                                        .padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                                report.subtypeMetrics.forEach { sm ->
                                    val active = selectedSubtypeFilter == sm.subtypeName
                                    val col = Color(sm.colorHex)
                                    Text(
                                        text = "${sm.subtypeName} (${sm.doseCount})",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        color = if (active) col else glassEnv.textSecondary,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (active) col.copy(alpha = 0.16f) else Color.Transparent)
                                            .border(1.dp, if (active) col else glassEnv.cardBorderBase, RoundedCornerShape(6.dp))
                                            .clickable { selectedSubtypeFilter = sm.subtypeName }
                                            .padding(horizontal = 7.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        filteredLogs.take(15).forEach { log ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(glassEnv.cardBorderBase.copy(alpha = 0.22f))
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${settings.formatDose(log.doseAmount, log.unit)} • ${log.subtypeName} (${log.roaName})",
                                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp),
                                        color = glassEnv.textPrimary
                                    )
                                    Text(
                                        text = "${fmt.format(Date(log.timestampMillis))} • #${log.contextTag} • ★${log.subjectiveRating}/10 • ${settings.formatCurrency(log.cost)}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        color = glassEnv.textSecondary
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Duplicate",
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable { onDuplicateLog(log) }
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable { onDeleteLog(log.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubMetricCell(
    label: String,
    value: String,
    sub: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val glassEnv = LocalSubstanceGlass.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = if (glassEnv.isLightMode) 0.10f else 0.09f))
            .border(1.dp, color.copy(alpha = 0.32f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.5.sp,
                fontWeight = FontWeight.SemiBold
            ),
            color = glassEnv.textSecondary,
            maxLines = 1
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = RobotoMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            ),
            color = glassEnv.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = sub,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            ),
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun formatDecimalHour(hr: Double): String {
    val h = hr.toInt().coerceIn(0, 23)
    val m = ((hr - h) * 60.0).roundToInt().coerceIn(0, 59)
    return String.format(Locale.US, "%02d:%02d", h, m)
}
