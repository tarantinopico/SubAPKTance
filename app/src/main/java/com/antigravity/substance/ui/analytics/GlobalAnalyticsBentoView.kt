package com.antigravity.substance.ui.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.ScatterPlot
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.StackedBarChart
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.antigravity.substance.data.db.entities.SubstanceEntity
import com.antigravity.substance.data.preferences.UserSettings
import com.antigravity.substance.domain.engine.AnalyticsAggregatorEngine
import com.antigravity.substance.domain.engine.GlobalAnalyticsReport
import com.antigravity.substance.ui.analytics.charts.BioActivityHeatmapCanvas
import com.antigravity.substance.ui.analytics.charts.CircadianPolarClockCanvas
import com.antigravity.substance.ui.analytics.charts.DualContinuousCurveCanvas
import com.antigravity.substance.ui.analytics.charts.InteractiveScatterRegressionChart
import com.antigravity.substance.ui.analytics.charts.IntervalEvolutionChart
import com.antigravity.substance.ui.analytics.charts.SpendDonutAndTrendCanvas
import com.antigravity.substance.ui.analytics.charts.StackedConsumptionTimelineCanvas
import com.antigravity.substance.ui.analytics.charts.ThresholdTierHorizontalStackedBar
import com.antigravity.substance.ui.theme.BentoMicroHeader
import com.antigravity.substance.ui.theme.ChartStatsSummaryStrip
import com.antigravity.substance.ui.theme.FrostedBentoCard
import com.antigravity.substance.ui.theme.LocalSubstanceGlass
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * MODE A: GLOBAL OVERVIEW ANALYTICS (5 Sub-Pages, 22+ Interactive Visualizations)
 * Sub-Pages:
 * 0 = 1. Přehled & Spotřeba (KPI, Denní skládaný graf s inspektorem, Finance, 84D Kalendář, Dynamika 7D vs 30D)
 * 1 = 2. Druhy & Varianty (Žebříček poddruhů napříč látkami, Nákladovost druhů, Síla & Hodnocení druhů, Cesty podání & Kontext)
 * 2 = 3. Korelace & Závislosti (Pearsonova matice korelací mezi látkami, Závislost velikosti dávky na rozestupu, Čas 1. dávky vs zátěž, Zátěž vs Hodnocení)
 * 3 = 4. Rozestupy & Rytmus (Globální distribuce rozestupů, Vývoj rozestupů v čase, 24h Cirkadiánní hodiny, Dny v týdnu, Abstinence)
 * 4 = 5. PK & Tělo (Kompozitní 48h křivka Aktivní účinek vs Hladina v těle, Prahové zóny, Dodržování limitů)
 * 5 = Vše (Kompletní přehled všech 5 sekcí)
 */
@Composable
fun GlobalAnalyticsBentoView(
    report: GlobalAnalyticsReport,
    substances: List<SubstanceEntity>,
    settings: UserSettings,
    selectedSubPage: Int = 0,
    onSelectSubPage: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val glassEnv = LocalSubstanceGlass.current
    var timelineMode by remember { mutableIntStateOf(0) } // 0 = Doses, 1 = Bio-Load, 2 = Spend
    var weekdayMode by remember { mutableIntStateOf(0) } // 0 = Doses, 1 = Spend, 2 = Bio-Load
    var waveMode by remember { mutableIntStateOf(0) } // 0 = Active Effect, 1 = Body Presence

    val showOverview = selectedSubPage == 0 || selectedSubPage == 5
    val showSubtypes = selectedSubPage == 1 || selectedSubPage == 5
    val showCorrelations = selectedSubPage == 2 || selectedSubPage == 5
    val showIntervals = selectedSubPage == 3 || selectedSubPage == 5
    val showPkBody = selectedSubPage == 4 || selectedSubPage == 5

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .testTag("global_analytics_list"),
        contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 4.dp, bottom = 92.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // =========================================================================
        // SUB-PAGE 1: PŘEHLED & FINANCE
        // =========================================================================
        if (showOverview) {
            // 1. Global KPI Hero Matrix
            item {
                FrostedBentoCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = Color(0xFF06B6D4)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        BentoMicroHeader(
                            title = "1. SOUHRNNÁ MATICE KPI (${report.timeframeDays} DNÍ)",
                            icon = Icons.Default.Shield,
                            accentColor = Color(0xFF06B6D4),
                            trailingText = "SAFETY ${report.harmReductionSafetyScore}/100"
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            KpiMiniBox(
                                label = "CELKOVÁ ÚTRATA",
                                value = settings.formatCurrency(report.totalSpend),
                                sub = "Ø ${settings.formatCurrency(report.dailyAvgSpend)}/den",
                                color = Color(0xFF06B6D4),
                                modifier = Modifier.weight(1f)
                            )
                            KpiMiniBox(
                                label = "30D PREDIKCE",
                                value = settings.formatCurrency(report.projectedMonthlySpend),
                                sub = "${report.totalIntakesCount} záznamů",
                                color = Color(0xFFF59E0B),
                                modifier = Modifier.weight(1f)
                            )
                            KpiMiniBox(
                                label = "DOBA ÚČINKU / V TĚLE",
                                value = "${report.activeHoursPercentage.roundToInt()}% / ${report.bodyPresenceHoursPercentage.roundToInt()}%",
                                sub = "Účinek vs Eliminace",
                                color = Color(0xFF10B981),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        ChartStatsSummaryStrip(
                            stats = report.dailySpendStats,
                            unit = settings.currencySymbol,
                            decimals = 0,
                            accentColor = Color(0xFF06B6D4)
                        )
                    }
                }
            }

            // 2. Interactive Stacked Consumption Timeline
            item {
                FrostedBentoCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = Color(0xFF38BDF8)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BentoMicroHeader(
                                title = "2. DENNÍ SPOTŘEBA & TREND (KLIKNI NA DEN)",
                                icon = Icons.Default.StackedBarChart,
                                accentColor = Color(0xFF38BDF8)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(0 to "Počet dávek", 1 to "Bio-Zátěž %", 2 to "Výdaje").forEach { (idx, lbl) ->
                                val active = timelineMode == idx
                                Text(
                                    text = lbl,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                    color = if (active) Color(0xFF38BDF8) else glassEnv.textSecondary,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (active) Color(0xFF38BDF8).copy(alpha = 0.16f) else Color.Transparent)
                                        .border(1.dp, if (active) Color(0xFF38BDF8) else glassEnv.cardBorderBase, RoundedCornerShape(6.dp))
                                        .clickable { timelineMode = idx }
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        ChartStatsSummaryStrip(
                            stats = if (timelineMode == 2) report.dailySpendStats else report.dailyDoseCountStats,
                            unit = if (timelineMode == 2) settings.currencySymbol else "×",
                            decimals = if (timelineMode == 2) 0 else 1,
                            accentColor = Color(0xFF38BDF8)
                        )

                        StackedConsumptionTimelineCanvas(
                            points = report.dailyTimeline,
                            substances = substances,
                            mode = timelineMode,
                            currencySymbol = settings.currencySymbol,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(132.dp)
                        )
                    }
                }
            }

            // 3. Financial Expenditure Breakdown + Cumulative Curve
            item {
                FrostedBentoCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = Color(0xFF10B981)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        BentoMicroHeader(
                            title = "3. FINANČNÍ ROZPAD & KUMULATIVNÍ VÝVOJ",
                            icon = Icons.Default.AccountBalanceWallet,
                            accentColor = Color(0xFF10B981),
                            trailingText = settings.formatCurrency(report.totalSpend)
                        )
                        SpendDonutAndTrendCanvas(
                            shares = report.spendShares,
                            cumulativeCurve = report.cumulativeSpendCurve,
                            currencySymbol = settings.currencySymbol,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // 4. 84-Day Bio-Activity Heatmap Calendar with Inspector
            item {
                FrostedBentoCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = Color(0xFF8B5CF6)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        BentoMicroHeader(
                            title = "4. 84DENNÍ KALENDÁŘ AKTIVITY (KLIKNI NA BUŇKU)",
                            icon = Icons.Default.CalendarMonth,
                            accentColor = Color(0xFF8B5CF6),
                            trailingText = "${report.cleanDaysCount} ČISTÝCH DNÍ"
                        )
                        BioActivityHeatmapCanvas(
                            cells = report.heatmapCells,
                            currencySymbol = settings.currencySymbol,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(98.dp)
                        )
                    }
                }
            }

            // 5. Rolling 7D vs 30D Velocity Momentum
            item {
                FrostedBentoCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = Color(0xFFF59E0B)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        BentoMicroHeader(
                            title = "5. DYNAMIKA SPOTŘEBY (7D VS 30D TREND)",
                            icon = Icons.Default.Speed,
                            accentColor = Color(0xFFF59E0B),
                            trailingText = "RYCHLOST ZMĚNY"
                        )
                        report.velocityMetrics.forEach { vm ->
                            val isRising = vm.deltaPercent > 5
                            val deltaColor = if (isRising) Color(0xFFEF4444) else Color(0xFF10B981)
                            val sign = if (vm.deltaPercent > 0) "+" else ""
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(vm.substance.colorHex))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = vm.substance.name,
                                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp),
                                        color = glassEnv.textPrimary
                                    )
                                }
                                Text(
                                    text = "7D: ${settings.formatDose(vm.rolling7dDailyUnits, vm.substance.defaultUnit)}/d vs 30D: ${settings.formatDose(vm.rolling30dDailyUnits, vm.substance.defaultUnit)}/d",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                    color = glassEnv.textSecondary
                                )
                                Text(
                                    text = "$sign${vm.deltaPercent}%",
                                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                    color = deltaColor
                                )
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // SUB-PAGE 2: DRUHY & VARIANTY (SUBTYPES ACROSS SUBSTANCES)
        // =========================================================================
        if (showSubtypes) {
            // 6. Top Subtypes Share & Volume Breakdown
            item {
                FrostedBentoCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = Color(0xFF10B981)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        BentoMicroHeader(
                            title = "6. PŘEHLED PODDRUHŮ & VARIANT LÁTEK",
                            icon = Icons.Default.Category,
                            accentColor = Color(0xFF10B981),
                            trailingText = "${report.globalSubtypeMetrics.size} AKTIVNÍCH DRUHŮ"
                        )
                        if (report.globalSubtypeMetrics.isEmpty()) {
                            Text(
                                text = "Žádné záznamy s poddruhy v tomto období.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = glassEnv.textSecondary
                            )
                        } else {
                            report.globalSubtypeMetrics.take(10).forEach { sm ->
                                val col = Color(sm.colorHex)
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(col.copy(alpha = 0.08f))
                                        .border(1.dp, col.copy(alpha = 0.28f), RoundedCornerShape(10.dp))
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
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(col)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "${sm.parentSubstanceName} • ${sm.subtypeName}",
                                                style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp),
                                                color = glassEnv.textPrimary
                                            )
                                        }
                                        Text(
                                            text = "${sm.doseCount}× (${(sm.shareOfUnitsFraction * 100).roundToInt()}% látky)",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold),
                                            color = col
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Min: ${settings.formatDose(sm.doseStats.min)} • Ø: ${settings.formatDose(sm.doseStats.mean)} • Max: ${settings.formatDose(sm.doseStats.max)} ${sm.unit}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                            color = glassEnv.textSecondary
                                        )
                                        Text(
                                            text = "Síla ${String.format(Locale.US, "%.2f", sm.potencyMultiplier)}× • ${settings.formatCurrency(sm.totalSpend)}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
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

            // 7. Subtype Cost Efficiency & Subjective Rating Comparison
            item {
                FrostedBentoCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = Color(0xFFF59E0B)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        BentoMicroHeader(
                            title = "7. SROVNÁNÍ CENY ZA DÁVKU A HODNOCENÍ PODLE DRUHU",
                            icon = Icons.Default.AccountBalanceWallet,
                            accentColor = Color(0xFFF59E0B),
                            trailingText = "CENA & EFEKTIVITA"
                        )
                        report.globalSubtypeMetrics.take(8).forEach { sm ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${sm.subtypeName} (${sm.parentSubstanceName})",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = glassEnv.textPrimary,
                                    modifier = Modifier.weight(1.3f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Ø ${settings.formatCurrency(sm.avgCostPerDose)}/dávka",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                    color = Color(0xFFF59E0B),
                                    modifier = Modifier.weight(0.9f)
                                )
                                Text(
                                    text = "Hodnocení ${String.format(Locale.US, "%.1f", sm.avgRating)}/10",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold),
                                    color = Color(0xFF10B981)
                                )
                            }
                        }
                    }
                }
            }

            // 8. RoA & Context Tags Breakdown
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FrostedBentoCard(
                        modifier = Modifier.weight(1f),
                        accentColor = Color(0xFF14B8A6)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            BentoMicroHeader(
                                title = "8. CESTY PODÁNÍ (RoA)",
                                icon = Icons.Default.Route,
                                accentColor = Color(0xFF14B8A6)
                            )
                            report.roaMetrics.take(5).forEach { roa ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = roa.roaName,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                        color = glassEnv.textPrimary
                                    )
                                    Text(
                                        text = "${(roa.shareFraction * 100).roundToInt()}% (${roa.count}×)",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                        color = Color(0xFF14B8A6)
                                    )
                                }
                            }
                        }
                    }

                    FrostedBentoCard(
                        modifier = Modifier.weight(1f),
                        accentColor = Color(0xFFF97316)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            BentoMicroHeader(
                                title = "9. KONTEXT UŽITÍ",
                                icon = Icons.Default.LocalOffer,
                                accentColor = Color(0xFFF97316)
                            )
                            report.contextMetrics.take(5).forEach { ctx ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "#${ctx.tag}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                        color = glassEnv.textPrimary
                                    )
                                    Text(
                                        text = "${ctx.count}× (★${String.format(Locale.US, "%.1f", ctx.avgRating)})",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        color = Color(0xFFF97316)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // SUB-PAGE 3: KORELACE & ZÁVISLOSTI (CROSS-SUBSTANCE & INTERVAL CORRELATIONS)
        // =========================================================================
        if (showCorrelations) {
            // 10. Cross-Substance Co-Administration & Pearson Dose Correlation Matrix
            item {
                FrostedBentoCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = Color(0xFFEC4899)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        BentoMicroHeader(
                            title = "10. KORELACE DÁVEK MEZI LÁTKAMI (PEARSON r & SOUBĚH)",
                            icon = Icons.Default.Hub,
                            accentColor = Color(0xFFEC4899),
                            trailingText = "ZÁVISLOST KOMBINACÍ"
                        )
                        if (report.synergyPairs.isEmpty()) {
                            Text(
                                text = "Žádné kombinace látek ve stejném dni.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = glassEnv.textSecondary
                            )
                        } else {
                            report.synergyPairs.take(8).forEach { pair ->
                                val r = pair.pearsonDoseCorrelation
                                val rColor = when {
                                    r >= 0.25 -> Color(0xFFF59E0B)
                                    r <= -0.25 -> Color(0xFF38BDF8)
                                    else -> glassEnv.textSecondary
                                }
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(glassEnv.cardBorderBase.copy(alpha = 0.22f))
                                        .padding(7.dp),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${pair.subAName} ↔ ${pair.subBName}",
                                            style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp),
                                            color = glassEnv.textPrimary
                                        )
                                        Text(
                                            text = "Souběh ${pair.coOccurrencePercent}% (${pair.sharedDaysCount} dní)",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                            color = Color(0xFFEC4899)
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        val rDesc = when {
                                            r >= 0.35 -> "Vyšší dávka A vede k vyšší dávce B"
                                            r <= -0.35 -> "Vyšší dávka A substituuje (snižuje) B"
                                            else -> "Velikosti dávek na sobě přímo nezávisí"
                                        }
                                        Text(
                                            text = rDesc,
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                            color = glassEnv.textSecondary
                                        )
                                        Text(
                                            text = "r = ${String.format(Locale.US, "%+.2f", r)}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold),
                                            color = rColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 11. Global Scatter: Relative Dose Size vs Previous Interval (Závislost velikosti dávky na rozestupu)
            item {
                FrostedBentoCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = Color(0xFF06B6D4)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        BentoMicroHeader(
                            title = "11. ZÁVISLOST VELIKOSTI DÁVKY NA ROZESTUPU (KLIKNI NA BOD)",
                            icon = Icons.Default.ScatterPlot,
                            accentColor = Color(0xFF06B6D4),
                            trailingText = "r = ${String.format(Locale.US, "%+.2f", report.intervalVsNormalizedDoseRegression.pearsonR)}"
                        )
                        ChartStatsSummaryStrip(
                            stats = report.intervalVsNormalizedDoseRegression.yStats,
                            unit = "% výchozí",
                            decimals = 0,
                            accentColor = Color(0xFF06B6D4)
                        )
                        InteractiveScatterRegressionChart(
                            regression = report.intervalVsNormalizedDoseRegression,
                            xAxisLabel = "Předchozí rozestup (h)",
                            yAxisLabel = "Relativní dávka (%)",
                            accentColor = Color(0xFF06B6D4),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(145.dp)
                        )
                    }
                }
            }

            // 12. First-Dose Hour vs Total Daily Bio-Load Scatter & Regression
            item {
                FrostedBentoCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = Color(0xFFF59E0B)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        BentoMicroHeader(
                            title = "12. VLIV ČASU PRVNÍ DÁVKY NA CELKOVOU DENNÍ ZÁTĚŽ",
                            icon = Icons.Default.CompareArrows,
                            accentColor = Color(0xFFF59E0B),
                            trailingText = "r = ${String.format(Locale.US, "%+.2f", report.firstDoseHourVsDailyLoadRegression.pearsonR)}"
                        )
                        InteractiveScatterRegressionChart(
                            regression = report.firstDoseHourVsDailyLoadRegression,
                            xAxisLabel = "Hodina 1. dávky (0-24h)",
                            yAxisLabel = "Denní bio-zátěž (%)",
                            accentColor = Color(0xFFF59E0B),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(135.dp)
                        )
                    }
                }
            }

            // 13. Daily Bio-Load vs Subjective Rating Correlation
            item {
                FrostedBentoCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = Color(0xFF10B981)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        BentoMicroHeader(
                            title = "13. KORELACE DENNÍ ZÁTĚŽE A SUBJEKTIVNÍHO HODNOCENÍ",
                            icon = Icons.Default.AutoGraph,
                            accentColor = Color(0xFF10B981),
                            trailingText = "r = ${String.format(Locale.US, "%+.2f", report.dailyLoadVsSubjectiveRatingRegression.pearsonR)}"
                        )
                        InteractiveScatterRegressionChart(
                            regression = report.dailyLoadVsSubjectiveRatingRegression,
                            xAxisLabel = "Denní bio-zátěž (%)",
                            yAxisLabel = "Hodnocení dne (1-10)",
                            accentColor = Color(0xFF10B981),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                        )
                    }
                }
            }
        }

        // =========================================================================
        // SUB-PAGE 4: ROZESTUPY & FREKVENCE (INTERVALS, RHYTHM & ABSTINENCE)
        // =========================================================================
        if (showIntervals) {
            // 14. Global Inter-Dose Interval Histogram & Statistics
            item {
                FrostedBentoCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = Color(0xFF06B6D4)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        BentoMicroHeader(
                            title = "14. DISTRIBUCE ROZESTUPŮ MEZI DÁVKAMI (VŠECHNY LÁTKY)",
                            icon = Icons.Default.AccessTime,
                            accentColor = Color(0xFF06B6D4),
                            trailingText = "MEDIÁN ${String.format(Locale.US, "%.1f", report.globalIntervalStats.median)}h"
                        )
                        ChartStatsSummaryStrip(
                            stats = report.globalIntervalStats,
                            unit = "h",
                            decimals = 1,
                            accentColor = Color(0xFF06B6D4)
                        )
                        val bucketLabels = listOf("<1h", "1-2h", "2-4h", "4-8h", "8-16h", "16-24h", ">24h")
                        val maxB = (report.globalIntervalBuckets7.maxOrNull() ?: 1).coerceAtLeast(1)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(86.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            report.globalIntervalBuckets7.forEachIndexed { idx, cnt ->
                                val frac = (cnt.toFloat() / maxB).coerceIn(0.08f, 1f)
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
                    }
                }
            }

            // 15. Chronological Interval Evolution Curve
            item {
                FrostedBentoCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = Color(0xFF38BDF8)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        BentoMicroHeader(
                            title = "15. VÝVOJ ROZESTUPŮ V ČASE + KLOUZAVÝ PRŮMĚR",
                            icon = Icons.Default.Timeline,
                            accentColor = Color(0xFF38BDF8),
                            trailingText = "TREND PAUZ"
                        )
                        IntervalEvolutionChart(
                            points = report.globalIntervalEvolution,
                            recommendedMinHours = settings.compulsiveRedoseThresholdHours,
                            accentColor = Color(0xFF38BDF8),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(115.dp)
                        )
                    }
                }
            }

            // 16 & 17. 24-Hour Circadian Polar Clock + Day-of-Week Profile
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FrostedBentoCard(
                        modifier = Modifier.weight(1f),
                        accentColor = Color(0xFF06B6D4)
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            BentoMicroHeader(
                                title = "16. 24H CIRKADIÁNNÍ HODINY",
                                icon = Icons.Default.Radar,
                                accentColor = Color(0xFF06B6D4)
                            )
                            CircadianPolarClockCanvas(
                                hourlyCounts24 = report.hourlyDistribution24,
                                hourlyColors24 = report.hourlyDominantColor24,
                                modifier = Modifier.size(112.dp)
                            )
                            val peakHour = report.hourlyDistribution24.indices
                                .maxByOrNull { report.hourlyDistribution24[it] } ?: 9
                            Text(
                                text = "Špička: ${String.format(Locale.US, "%02d:00", peakHour)}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                color = Color(0xFF06B6D4)
                            )
                        }
                    }

                    FrostedBentoCard(
                        modifier = Modifier.weight(1.2f),
                        accentColor = Color(0xFFF59E0B)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BentoMicroHeader(
                                    title = "17. DNY V TÝDNU",
                                    icon = Icons.Default.DateRange,
                                    accentColor = Color(0xFFF59E0B)
                                )
                                Text(
                                    text = when (weekdayMode) {
                                        0 -> "Dávky"
                                        1 -> settings.currencySymbol
                                        else -> "Zátěž"
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, color = Color(0xFFF59E0B)),
                                    modifier = Modifier.clickable { weekdayMode = (weekdayMode + 1) % 3 }
                                )
                            }

                            val vals = when (weekdayMode) {
                                0 -> report.weekdayAvgDoses
                                1 -> report.weekdayAvgSpend
                                else -> report.weekdayAvgLoad
                            }
                            val maxW = (vals.maxOrNull() ?: 1f).coerceAtLeast(0.1f)
                            val days = listOf("Po", "Út", "St", "Čt", "Pá", "So", "Ne")

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(94.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                vals.forEachIndexed { idx, v ->
                                    val barFrac = (v / maxW).coerceIn(0.08f, 1f)
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Bottom,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = String.format(Locale.US, "%.1f", v),
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.5.sp),
                                            color = glassEnv.textSecondary
                                        )
                                        Box(
                                            modifier = Modifier
                                                .width(12.dp)
                                                .height((62 * barFrac).dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(if (idx >= 5) Color(0xFFF59E0B) else Color(0xFF06B6D4))
                                        )
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = days[idx],
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                                            color = glassEnv.textSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 18. Abstinence & Clean-Day Streaks Leaderboard
            item {
                FrostedBentoCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = Color(0xFF10B981)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        BentoMicroHeader(
                            title = "18. PAUZY OD POSLEDNÍ DÁVKY & REKORDY ABSTINENCE",
                            icon = Icons.Default.EmojiEvents,
                            accentColor = Color(0xFF10B981),
                            trailingText = "AKTUÁLNÍ VS MAX PAUZA"
                        )
                        report.streakLeaderboard.forEach { item ->
                            val frac = (item.currentElapsedHours / item.longestStreakHours.coerceAtLeast(1.0))
                                .toFloat().coerceIn(0.05f, 1f)
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${item.substance.name} (Ø rozestup ${String.format(Locale.US, "%.1f", item.avgGapHours)}h)",
                                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 10.5.sp),
                                        color = glassEnv.textPrimary
                                    )
                                    Text(
                                        text = "Nyní ${formatHoursShort(item.currentElapsedHours)} / Max ${formatHoursShort(item.longestStreakHours)}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                        color = Color(item.substance.colorHex)
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
                                            .background(Color(item.substance.colorHex))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // SUB-PAGE 5: FARMAKOKINETIKA & TĚLO (ACTIVE EFFECT VS BODY PRESENCE)
        // =========================================================================
        if (showPkBody) {
            // 19. Composite 48-Hour Multi-Substance PK Wave (Active Effect vs Body Presence)
            item {
                FrostedBentoCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = Color(0xFF8B5CF6)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        BentoMicroHeader(
                            title = "19. 48H VLNA: DOBA ÚČINKU VS DOBA V TĚLE",
                            icon = Icons.Default.Waves,
                            accentColor = Color(0xFF8B5CF6),
                            trailingText = if (waveMode == 0) "HLAVNÍ DOBA ÚČINKU" else "DOBA V TĚLE (ELIMINACE)"
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(
                                0 to "Hlavní doba účinku (Active Effect)",
                                1 to "Doba v těle (Body Presence)"
                            ).forEach { (idx, lbl) ->
                                val active = waveMode == idx
                                Text(
                                    text = lbl,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                    color = if (active) Color(0xFF8B5CF6) else glassEnv.textSecondary,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (active) Color(0xFF8B5CF6).copy(alpha = 0.16f) else Color.Transparent)
                                        .border(1.dp, if (active) Color(0xFF8B5CF6) else glassEnv.cardBorderBase, RoundedCornerShape(6.dp))
                                        .clickable { waveMode = idx }
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        val wavesMap = if (waveMode == 0) report.composite48hActiveWaves else report.composite48hBodyWaves
                        val firstSub = substances.firstOrNull { wavesMap.containsKey(it.id) }
                        val firstWave = firstSub?.let { wavesMap[it.id] } ?: List(48) { 0f }
                        val secondSub = substances.drop(1).firstOrNull { wavesMap.containsKey(it.id) }
                        val secondWave = secondSub?.let { wavesMap[it.id] }

                        DualContinuousCurveCanvas(
                            primaryPoints = firstWave,
                            secondaryPoints = secondWave,
                            primaryColor = Color(firstSub?.colorHex ?: 0xFF06B6D4L),
                            secondaryColor = Color(secondSub?.colorHex ?: 0xFF10B981L),
                            primaryLabel = firstSub?.name ?: "Látka 1",
                            secondaryLabel = secondSub?.name ?: "Látka 2",
                            unit = "rel.",
                            xStepLabelPrefix = "Hodina",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp)
                        )
                    }
                }
            }

            // 20. Threshold Tier Distribution
            item {
                FrostedBentoCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = Color(0xFFF97316)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        BentoMicroHeader(
                            title = "20. ROZLOŽENÍ DÁVEK DO 5 PRAHOVÝCH ZÓN INTENZITY",
                            icon = Icons.Default.Layers,
                            accentColor = Color(0xFFF97316),
                            trailingText = "${report.totalIntakesCount} ZÁZNAMŮ"
                        )
                        ThresholdTierHorizontalStackedBar(
                            tierCounts = report.tierCounts,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // 21. Daily Safe Limit Compliance
            item {
                FrostedBentoCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = Color(0xFFEF4444)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        BentoMicroHeader(
                            title = "21. DODRŽOVÁNÍ BEZPEČNÝCH DENNÍCH LIMITŮ",
                            icon = Icons.Default.Shield,
                            accentColor = Color(0xFFEF4444),
                            trailingText = "${report.breachedDaysCount} PŘEKROČENÍ"
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            KpiMiniBox(
                                label = "ČISTÉ DNY BEZ LÁTEK",
                                value = "${report.cleanDaysCount} dní",
                                sub = "Plná regenerace",
                                color = Color(0xFF10B981),
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            KpiMiniBox(
                                label = "V BEZPEČNÉM LIMITU",
                                value = "${(report.timeframeDays - report.breachedDaysCount).coerceAtLeast(0)} dní",
                                sub = "Pod denním maximem",
                                color = Color(0xFF06B6D4),
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            KpiMiniBox(
                                label = "PŘEKROČENÝ LIMIT",
                                value = "${report.breachedDaysCount} dní",
                                sub = "Nad bezpečnou hranicí",
                                color = Color(0xFFEF4444),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KpiMiniBox(
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
            .border(1.dp, color.copy(alpha = 0.30f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
            color = glassEnv.textSecondary,
            maxLines = 1
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = com.antigravity.substance.ui.theme.RobotoMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            ),
            color = glassEnv.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = sub,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun formatHoursShort(hours: Double): String {
    return if (hours >= 48.0) {
        String.format(Locale.US, "%.1fd", hours / 24.0)
    } else {
        String.format(Locale.US, "%.1fh", hours)
    }
}
