package com.antigravity.substance.ui.analytics.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.substance.data.db.entities.SubstanceEntity
import com.antigravity.substance.domain.engine.DailyStackedPoint
import com.antigravity.substance.domain.engine.DailySubstanceUnitPoint
import com.antigravity.substance.domain.engine.DailySubtypeStackedPoint
import com.antigravity.substance.domain.engine.HeatmapDayCell
import com.antigravity.substance.domain.engine.IntervalEvolutionPoint
import com.antigravity.substance.domain.engine.LinearRegressionResult
import com.antigravity.substance.domain.engine.SubstanceSpendShare
import com.antigravity.substance.domain.engine.SubtypeAnalyticsMetric
import com.antigravity.substance.domain.model.DoseTier
import com.antigravity.substance.ui.theme.LocalSubstanceGlass
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun InspectorPointBanner(
    primaryText: String,
    secondaryText: String,
    accentColor: Color = Color(0xFF06B6D4),
    onClear: (() -> Unit)? = null
) {
    val glassEnv = LocalSubstanceGlass.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(accentColor.copy(alpha = if (glassEnv.isLightMode) 0.12f else 0.16f))
            .border(1.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(accentColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = primaryText,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = glassEnv.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = secondaryText,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            ),
            color = accentColor,
            maxLines = 1
        )
    }
}

/**
 * Interactive Multi-Substance Stacked Bar Timeline with Min/Avg/Max reference lines and Point Inspector
 */
@Composable
fun StackedConsumptionTimelineCanvas(
    points: List<DailyStackedPoint>,
    substances: List<SubstanceEntity>,
    mode: Int, // 0 = Dose Count, 1 = Normalized Load, 2 = Spend
    currencySymbol: String = "Kč",
    modifier: Modifier = Modifier
) {
    val glassEnv = LocalSubstanceGlass.current
    val subColorMap = substances.associate { it.id to Color(it.colorHex) }
    val subNameMap = substances.associate { it.id to it.name }
    var selectedIdx by remember(points.size, mode) { mutableIntStateOf(points.lastIndex.coerceAtLeast(0)) }

    val totals = remember(points, mode) {
        points.map { pt ->
            when (mode) {
                0 -> pt.perSubstanceCount.values.sum().toFloat()
                1 -> pt.perSubstanceNormalizedLoad.values.sum() * 100f
                else -> pt.totalSpend.toFloat()
            }
        }
    }

    val maxStack = (totals.maxOrNull() ?: 1f).coerceAtLeast(1f)
    val avgStack = if (totals.isNotEmpty()) totals.average().toFloat() else 0f
    val maxIdx = totals.indices.maxByOrNull { totals[it] } ?: -1

    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        if (points.isNotEmpty() && selectedIdx in points.indices) {
            val pt = points[selectedIdx]
            val valNum = totals[selectedIdx]
            val diffPct = if (avgStack > 0.01f) (((valNum - avgStack) / avgStack) * 100f).roundToInt() else 0
            val diffSign = if (diffPct >= 0) "+$diffPct%" else "$diffPct%"
            val breakdown = pt.perSubstanceCount.entries
                .sortedByDescending { it.value }
                .take(3)
                .joinToString(", ") { "${subNameMap[it.key] ?: "Sub"}: ${it.value}×" }
                .ifBlank { "Žádný záznam" }
            val valueStr = when (mode) {
                0 -> "${valNum.roundToInt()} dávek ($diffSign vs průměr)"
                1 -> "Zátěž ${valNum.roundToInt()}% ($diffSign)"
                else -> "${String.format(Locale.US, "%.0f", valNum)} $currencySymbol ($diffSign)"
            }
            InspectorPointBanner(
                primaryText = "${pt.label} • $breakdown",
                secondaryText = valueStr,
                accentColor = if (pt.breachedLimit) Color(0xFFEF4444) else Color(0xFF06B6D4)
            )
        }

        Canvas(
            modifier = modifier
                .pointerInput(points.size) {
                    detectTapGestures { offset ->
                        if (points.isNotEmpty() && size.width > 0) {
                            val idx = ((offset.x / size.width) * points.size).toInt().coerceIn(0, points.lastIndex)
                            selectedIdx = idx
                        }
                    }
                }
                .pointerInput(points.size) {
                    detectHorizontalDragGestures { change, _ ->
                        if (points.isNotEmpty() && size.width > 0) {
                            val idx = ((change.position.x / size.width) * points.size).toInt().coerceIn(0, points.lastIndex)
                            selectedIdx = idx
                        }
                    }
                }
        ) {
            val w = size.width
            val h = size.height
            if (points.isEmpty() || w <= 10f || h <= 10f) return@Canvas

            val gridCol = if (glassEnv.isLightMode) Color(0xFFCBD5E1) else Color.White.copy(alpha = 0.07f)
            listOf(0.25f, 0.5f, 0.75f).forEach { frac ->
                drawLine(
                    color = gridCol,
                    start = Offset(0f, h * frac),
                    end = Offset(w, h * frac),
                    strokeWidth = 1.dp.toPx()
                )
            }

            val barSlotW = w / points.size.toFloat()
            val barW = (barSlotW * 0.68f).coerceAtLeast(2.5f)

            points.forEachIndexed { idx, pt ->
                val x = idx * barSlotW + (barSlotW - barW) / 2f
                var currentBottom = h
                val entries = when (mode) {
                    0 -> pt.perSubstanceCount.entries.map { it.key to it.value.toFloat() }
                    1 -> pt.perSubstanceNormalizedLoad.entries.map { it.key to (it.value * 100f) }
                    else -> pt.perSubstanceUnits.entries.map { it.key to (pt.totalSpend.toFloat() / pt.perSubstanceUnits.size.coerceAtLeast(1)) }
                }

                entries.forEach { (subId, v) ->
                    val segH = (v / (maxStack * 1.12f)) * (h * 0.90f)
                    val top = (currentBottom - segH).coerceAtLeast(0f)
                    val baseCol = subColorMap[subId] ?: Color(0xFF06B6D4)
                    val col = if (idx == selectedIdx) baseCol else baseCol.copy(alpha = 0.78f)
                    drawRoundRect(
                        color = col,
                        topLeft = Offset(x, top),
                        size = Size(barW, (currentBottom - top).coerceAtLeast(1.5f)),
                        cornerRadius = CornerRadius(2.dp.toPx())
                    )
                    currentBottom = top - 1f
                }

                // Highlight selected bar crosshair
                if (idx == selectedIdx) {
                    val cx = idx * barSlotW + barSlotW / 2f
                    drawLine(
                        color = Color(0xFF06B6D4).copy(alpha = 0.75f),
                        start = Offset(cx, 0f),
                        end = Offset(cx, h),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 4f))
                    )
                }
            }

            // Draw AVG dashed horizontal line & MAX marker
            if (glassEnv.showChartMinMaxAvgLines && avgStack > 0f) {
                val avgY = (h - (avgStack / (maxStack * 1.12f)) * (h * 0.90f)).coerceIn(4f, h - 4f)
                drawLine(
                    color = Color(0xFFF59E0B),
                    start = Offset(0f, avgY),
                    end = Offset(w, avgY),
                    strokeWidth = 1.4.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 5f))
                )
                if (maxIdx in points.indices) {
                    val mx = maxIdx * barSlotW + barSlotW / 2f
                    val my = (h - (totals[maxIdx] / (maxStack * 1.12f)) * (h * 0.90f)).coerceIn(5f, h - 5f)
                    drawCircle(
                        color = Color(0xFFEF4444),
                        radius = 4.dp.toPx(),
                        center = Offset(mx, my)
                    )
                }
            }
        }
    }
}

/**
 * Multi-Ring Donut + Interactive Cumulative Trend Line Chart
 */
@Composable
fun SpendDonutAndTrendCanvas(
    shares: List<SubstanceSpendShare>,
    cumulativeCurve: List<Float>,
    currencySymbol: String = "Kč",
    modifier: Modifier = Modifier
) {
    val glassEnv = LocalSubstanceGlass.current
    var selectedIdx by remember(cumulativeCurve.size) {
        mutableIntStateOf(cumulativeCurve.lastIndex.coerceAtLeast(0))
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier.size(94.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = 11.dp.toPx()
                var startAngle = -90f
                if (shares.isEmpty()) {
                    drawArc(
                        color = glassEnv.cardBorderBase,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = stroke)
                    )
                } else {
                    shares.forEach { share ->
                        val sweep = (share.shareFraction * 360f).coerceAtLeast(4f)
                        drawArc(
                            color = Color(share.substance.colorHex),
                            startAngle = startAngle,
                            sweepAngle = (sweep - 2f).coerceAtLeast(2f),
                            useCenter = false,
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                        startAngle += sweep
                    }
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${shares.size}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = glassEnv.textPrimary
                )
                Text(
                    text = "LÁTEK",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                    color = glassEnv.textSecondary
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (cumulativeCurve.isNotEmpty() && selectedIdx in cumulativeCurve.indices) {
                Text(
                    text = "Den ${selectedIdx + 1}/${cumulativeCurve.size}: Kumulativně ${String.format(Locale.US, "%.0f", cumulativeCurve[selectedIdx])} $currencySymbol",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold),
                    color = Color(0xFF06B6D4)
                )
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .pointerInput(cumulativeCurve.size) {
                        detectTapGestures { offset ->
                            if (cumulativeCurve.isNotEmpty() && size.width > 0) {
                                selectedIdx = ((offset.x / size.width) * cumulativeCurve.size)
                                    .toInt()
                                    .coerceIn(0, cumulativeCurve.lastIndex)
                            }
                        }
                    }
            ) {
                val w = size.width
                val h = size.height
                if (cumulativeCurve.size < 2) return@Canvas
                val maxVal = (cumulativeCurve.maxOrNull() ?: 1f).coerceAtLeast(1f)
                val path = Path()
                val fill = Path()
                cumulativeCurve.forEachIndexed { idx, v ->
                    val px = (idx.toFloat() / (cumulativeCurve.size - 1)) * w
                    val py = h - (v / maxVal) * (h * 0.85f)
                    if (idx == 0) {
                        path.moveTo(px, py)
                        fill.moveTo(px, h)
                        fill.lineTo(px, py)
                    } else {
                        path.lineTo(px, py)
                        fill.lineTo(px, py)
                    }
                    if (idx == cumulativeCurve.lastIndex) {
                        fill.lineTo(px, h)
                        fill.close()
                    }
                }
                if (!glassEnv.isLightweightMode) {
                    drawPath(
                        path = fill,
                        brush = Brush.verticalGradient(
                            listOf(Color(0xFF06B6D4).copy(alpha = 0.28f), Color.Transparent)
                        )
                    )
                }
                drawPath(
                    path = path,
                    color = Color(0xFF06B6D4),
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
                if (selectedIdx in cumulativeCurve.indices) {
                    val sx = (selectedIdx.toFloat() / (cumulativeCurve.size - 1)) * w
                    val sy = h - (cumulativeCurve[selectedIdx] / maxVal) * (h * 0.85f)
                    drawCircle(color = Color(0xFF06B6D4), radius = 4.dp.toPx(), center = Offset(sx, sy))
                }
            }

            shares.take(4).forEach { s ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(Color(s.substance.colorHex))
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = s.substance.name,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = glassEnv.textPrimary
                        )
                    }
                    Text(
                        text = "${(s.shareFraction * 100).roundToInt()}% (${s.doseCount}×)",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                        color = Color(s.substance.colorHex)
                    )
                }
            }
        }
    }
}

/**
 * Interactive 12-Week (84-Day) Bio-Activity Heatmap Calendar Grid with Cell Inspector
 */
@Composable
fun BioActivityHeatmapCanvas(
    cells: List<HeatmapDayCell>,
    currencySymbol: String = "Kč",
    modifier: Modifier = Modifier
) {
    val glassEnv = LocalSubstanceGlass.current
    var selectedCellIdx by remember(cells.size) { mutableIntStateOf(cells.lastIndex.coerceAtLeast(0)) }

    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        if (cells.isNotEmpty() && selectedCellIdx in cells.indices) {
            val c = cells[selectedCellIdx]
            InspectorPointBanner(
                primaryText = "Den ${c.dateLabel} • ${c.dominantSubstanceName}",
                secondaryText = if (c.doseCount == 0) "0 dávek (Čistý den)"
                else "${c.doseCount} dávek • ${String.format(Locale.US, "%.0f", c.totalSpend)} $currencySymbol",
                accentColor = if (c.doseCount == 0) Color(0xFF10B981) else Color(c.dominantSubstanceColorHex)
            )
        }

        Canvas(
            modifier = modifier.pointerInput(cells.size) {
                detectTapGestures { offset ->
                    val cols = 12
                    val rows = 7
                    if (size.width > 0 && size.height > 0) {
                        val c = ((offset.x / size.width) * cols).toInt().coerceIn(0, cols - 1)
                        val r = ((offset.y / size.height) * rows).toInt().coerceIn(0, rows - 1)
                        val idx = (c * rows + r).coerceIn(0, (cells.size - 1).coerceAtLeast(0))
                        selectedCellIdx = idx
                    }
                }
            }
        ) {
            val cols = 12
            val rows = 7
            val gap = 3.dp.toPx()
            val cellW = (size.width - gap * (cols - 1)) / cols
            val cellH = (size.height - gap * (rows - 1)) / rows
            val side = min(cellW, cellH).coerceAtLeast(4f)

            cells.take(cols * rows).forEachIndexed { idx, cell ->
                val c = idx / rows
                val r = idx % rows
                val x = c * (cellW + gap)
                val y = r * (cellH + gap)
                val emptyCol = if (glassEnv.isLightMode) Color(0xFFE2E8F0) else Color.White.copy(alpha = 0.06f)
                val color = if (cell.doseCount == 0) {
                    emptyCol
                } else {
                    Color(cell.dominantSubstanceColorHex).copy(alpha = cell.intensityFraction.coerceIn(0.30f, 0.95f))
                }
                drawRoundRect(
                    color = color,
                    topLeft = Offset(x, y),
                    size = Size(cellW, side),
                    cornerRadius = CornerRadius(3.dp.toPx())
                )
                if (idx == selectedCellIdx) {
                    drawRoundRect(
                        color = glassEnv.textPrimary,
                        topLeft = Offset(x, y),
                        size = Size(cellW, side),
                        cornerRadius = CornerRadius(3.dp.toPx()),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }
            }
        }
    }
}

/**
 * 24-Hour Circadian Intake Polar Clock (Radar / Rose Chart)
 */
@Composable
fun CircadianPolarClockCanvas(
    hourlyCounts24: List<Int>,
    hourlyColors24: List<Long>,
    modifier: Modifier = Modifier
) {
    val glassEnv = LocalSubstanceGlass.current
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val maxR = min(cx, cy) * 0.90f
        val innerR = maxR * 0.24f

        val ringCol = if (glassEnv.isLightMode) Color(0xFFCBD5E1) else Color.White.copy(alpha = 0.09f)
        listOf(0.4f, 0.7f, 1.0f).forEach { rFrac ->
            drawCircle(
                color = ringCol,
                radius = maxR * rFrac,
                center = Offset(cx, cy),
                style = Stroke(width = 1.dp.toPx())
            )
        }

        val maxCount = (hourlyCounts24.maxOrNull() ?: 1).coerceAtLeast(1)
        val sweepPerHour = 360f / 24f

        for (h in 0 until 24) {
            val count = hourlyCounts24.getOrElse(h) { 0 }
            val frac = (count.toFloat() / maxCount).coerceIn(0.10f, 1f)
            val col = if (count > 0) {
                Color(hourlyColors24.getOrElse(h) { 0xFF06B6D4L })
            } else {
                ringCol
            }
            val startDeg = -90f + h * sweepPerHour
            val rOuter = innerR + (maxR - innerR) * frac
            val rad = (startDeg + sweepPerHour * 0.5f) * (PI / 180.0)
            val x1 = cx + innerR * cos(rad).toFloat()
            val y1 = cy + innerR * sin(rad).toFloat()
            val x2 = cx + rOuter * cos(rad).toFloat()
            val y2 = cy + rOuter * sin(rad).toFloat()

            drawLine(
                color = col,
                start = Offset(x1, y1),
                end = Offset(x2, y2),
                strokeWidth = 4.2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

/**
 * Interactive Per-Substance Daily Consumption Bar Chart + 7-Day Moving Average + Min/Avg/Max/Limit Lines + Point Inspector
 */
@Composable
fun DailyUnitConsumptionBarChartCanvas(
    bars: List<DailySubstanceUnitPoint>,
    dailySafeLimit: Double,
    unit: String,
    primaryColor: Color,
    modifier: Modifier = Modifier
) {
    val glassEnv = LocalSubstanceGlass.current
    var selectedIdx by remember(bars.size) { mutableIntStateOf(bars.lastIndex.coerceAtLeast(0)) }

    val avgUnits = remember(bars) {
        if (bars.isNotEmpty()) bars.map { it.totalUnits }.average() else 0.0
    }
    val maxIdx = remember(bars) {
        bars.indices.maxByOrNull { bars[it].totalUnits } ?: -1
    }
    val minActiveIdx = remember(bars) {
        bars.indices.filter { bars[it].totalUnits > 0.0 }.minByOrNull { bars[it].totalUnits } ?: -1
    }

    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        if (bars.isNotEmpty() && selectedIdx in bars.indices) {
            val pt = bars[selectedIdx]
            val diffPct = if (avgUnits > 0.01) (((pt.totalUnits - avgUnits) / avgUnits) * 100.0).roundToInt() else 0
            val diffSign = if (diffPct >= 0) "+$diffPct%" else "$diffPct%"
            InspectorPointBanner(
                primaryText = "${pt.label} • ${pt.doseCount}× dávka (7D průměr: ${String.format(Locale.US, "%.1f", pt.movingAvg7d)} $unit)",
                secondaryText = "${String.format(Locale.US, "%.1f", pt.totalUnits)} $unit ($diffSign)",
                accentColor = if (pt.breachedSafeLimit) Color(0xFFEF4444) else primaryColor
            )
        }

        Canvas(
            modifier = modifier
                .pointerInput(bars.size) {
                    detectTapGestures { offset ->
                        if (bars.isNotEmpty() && size.width > 0) {
                            selectedIdx = ((offset.x / size.width) * bars.size).toInt().coerceIn(0, bars.lastIndex)
                        }
                    }
                }
                .pointerInput(bars.size) {
                    detectHorizontalDragGestures { change, _ ->
                        if (bars.isNotEmpty() && size.width > 0) {
                            selectedIdx = ((change.position.x / size.width) * bars.size).toInt().coerceIn(0, bars.lastIndex)
                        }
                    }
                }
        ) {
            val w = size.width
            val h = size.height
            if (bars.isEmpty() || w <= 10f || h <= 10f) return@Canvas

            val maxVal = max(
                dailySafeLimit * 1.15,
                bars.maxOf { max(it.totalUnits, it.movingAvg7d) } * 1.15
            ).coerceAtLeast(1.0)

            // Safe Daily Limit horizontal dashed line
            val limitY = (h - (dailySafeLimit / maxVal).toFloat() * (h * 0.88f)).coerceIn(3f, h - 3f)
            drawLine(
                color = Color(0xFFEF4444).copy(alpha = 0.65f),
                start = Offset(0f, limitY),
                end = Offset(w, limitY),
                strokeWidth = 1.2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(7f, 5f))
            )

            // AVG horizontal dashed line
            if (glassEnv.showChartMinMaxAvgLines && avgUnits > 0.0) {
                val avgY = (h - (avgUnits / maxVal).toFloat() * (h * 0.88f)).coerceIn(3f, h - 3f)
                drawLine(
                    color = Color(0xFFF59E0B).copy(alpha = 0.80f),
                    start = Offset(0f, avgY),
                    end = Offset(w, avgY),
                    strokeWidth = 1.2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
                )
            }

            val slotW = w / bars.size.toFloat()
            val barW = (slotW * 0.66f).coerceAtLeast(2.5f)
            val maPath = Path()

            bars.forEachIndexed { idx, bar ->
                val x = idx * slotW + (slotW - barW) / 2f
                val barH = ((bar.totalUnits / maxVal).toFloat() * (h * 0.88f)).coerceAtLeast(2f)
                val emptyCol = if (glassEnv.isLightMode) Color(0xFFE2E8F0) else Color.White.copy(alpha = 0.06f)
                val barColor = when {
                    bar.breachedSafeLimit -> Color(0xFFEF4444)
                    bar.totalUnits <= 0.001 -> emptyCol
                    else -> primaryColor
                }
                val alpha = if (idx == selectedIdx) 1.0f else 0.75f
                drawRoundRect(
                    color = barColor.copy(alpha = alpha),
                    topLeft = Offset(x, h - barH),
                    size = Size(barW, barH),
                    cornerRadius = CornerRadius(3.dp.toPx())
                )

                if (idx == selectedIdx) {
                    val cx = idx * slotW + slotW / 2f
                    drawLine(
                        color = primaryColor,
                        start = Offset(cx, 0f),
                        end = Offset(cx, h),
                        strokeWidth = 1.4.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
                    )
                }

                val maX = idx * slotW + slotW / 2f
                val maY = (h - (bar.movingAvg7d / maxVal).toFloat() * (h * 0.88f)).coerceIn(2f, h - 2f)
                if (idx == 0) maPath.moveTo(maX, maY) else maPath.lineTo(maX, maY)
            }

            drawPath(
                path = maPath,
                color = if (glassEnv.isLightMode) Color(0xFF0F172A) else Color.White,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )

            // Draw MAX and MIN markers
            if (glassEnv.showChartMinMaxAvgLines) {
                if (maxIdx in bars.indices && bars[maxIdx].totalUnits > 0.0) {
                    val mx = maxIdx * slotW + slotW / 2f
                    val my = (h - (bars[maxIdx].totalUnits / maxVal).toFloat() * (h * 0.88f)).coerceIn(4f, h - 4f)
                    drawCircle(color = Color(0xFFEF4444), radius = 4.2.dp.toPx(), center = Offset(mx, my))
                }
                if (minActiveIdx in bars.indices && minActiveIdx != maxIdx) {
                    val mnx = minActiveIdx * slotW + slotW / 2f
                    val mny = (h - (bars[minActiveIdx].totalUnits / maxVal).toFloat() * (h * 0.88f)).coerceIn(4f, h - 4f)
                    drawCircle(color = Color(0xFF10B981), radius = 4.0.dp.toPx(), center = Offset(mnx, mny))
                }
            }
        }
    }
}

/**
 * Interactive Dual-Curve Continuous Line Chart with Touch Scrubber & Min/Avg/Max lines
 */
@Composable
fun DualContinuousCurveCanvas(
    primaryPoints: List<Float>,
    secondaryPoints: List<Float>? = null,
    primaryColor: Color,
    secondaryColor: Color = Color(0xFF94A3B8),
    primaryLabel: String = "Hlavní křivka",
    secondaryLabel: String = "Vedlejší křivka",
    unit: String = "",
    xStepLabelPrefix: String = "Bod",
    modifier: Modifier = Modifier
) {
    val glassEnv = LocalSubstanceGlass.current
    var selectedIdx by remember(primaryPoints.size) {
        mutableIntStateOf(primaryPoints.lastIndex.coerceAtLeast(0))
    }

    val avgP = remember(primaryPoints) {
        if (primaryPoints.isNotEmpty()) primaryPoints.average().toFloat() else 0f
    }
    val maxIdx = remember(primaryPoints) {
        primaryPoints.indices.maxByOrNull { primaryPoints[it] } ?: -1
    }

    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        if (primaryPoints.isNotEmpty() && selectedIdx in primaryPoints.indices) {
            val pVal = primaryPoints[selectedIdx]
            val sVal = secondaryPoints?.getOrNull(selectedIdx)
            val secStr = if (sVal != null) " • $secondaryLabel: ${String.format(Locale.US, "%.1f", sVal)} $unit" else ""
            InspectorPointBanner(
                primaryText = "$xStepLabelPrefix ${selectedIdx + 1}/${primaryPoints.size}$secStr",
                secondaryText = "$primaryLabel: ${String.format(Locale.US, "%.1f", pVal)} $unit",
                accentColor = primaryColor
            )
        }

        Canvas(
            modifier = modifier
                .pointerInput(primaryPoints.size) {
                    detectTapGestures { offset ->
                        if (primaryPoints.isNotEmpty() && size.width > 0) {
                            selectedIdx = ((offset.x / size.width) * primaryPoints.size)
                                .toInt()
                                .coerceIn(0, primaryPoints.lastIndex)
                        }
                    }
                }
                .pointerInput(primaryPoints.size) {
                    detectHorizontalDragGestures { change, _ ->
                        if (primaryPoints.isNotEmpty() && size.width > 0) {
                            selectedIdx = ((change.position.x / size.width) * primaryPoints.size)
                                .toInt()
                                .coerceIn(0, primaryPoints.lastIndex)
                        }
                    }
                }
        ) {
            val w = size.width
            val h = size.height
            if (primaryPoints.size < 2 || w <= 10f || h <= 10f) return@Canvas

            val maxP = primaryPoints.maxOrNull() ?: 1f
            val maxS = secondaryPoints?.maxOrNull() ?: 0f
            val maxY = max(maxP, maxS).coerceAtLeast(0.1f) * 1.12f

            // AVG reference line
            if (glassEnv.showChartMinMaxAvgLines && avgP > 0f) {
                val avgY = (h - (avgP / maxY) * (h * 0.88f)).coerceIn(3f, h - 3f)
                drawLine(
                    color = Color(0xFFF59E0B).copy(alpha = 0.65f),
                    start = Offset(0f, avgY),
                    end = Offset(w, avgY),
                    strokeWidth = 1.1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f))
                )
            }

            if (secondaryPoints != null && secondaryPoints.size >= 2) {
                val sPath = Path()
                secondaryPoints.forEachIndexed { idx, v ->
                    val px = (idx.toFloat() / (secondaryPoints.size - 1)) * w
                    val py = (h - (v / maxY) * (h * 0.88f)).coerceIn(2f, h)
                    if (idx == 0) sPath.moveTo(px, py) else sPath.lineTo(px, py)
                }
                drawPath(
                    path = sPath,
                    color = secondaryColor,
                    style = Stroke(
                        width = 1.6.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f))
                    )
                )
            }

            val pPath = Path()
            val fillPath = Path()
            primaryPoints.forEachIndexed { idx, v ->
                val px = (idx.toFloat() / (primaryPoints.size - 1)) * w
                val py = (h - (v / maxY) * (h * 0.88f)).coerceIn(2f, h)
                if (idx == 0) {
                    pPath.moveTo(px, py)
                    fillPath.moveTo(px, h)
                    fillPath.lineTo(px, py)
                } else {
                    pPath.lineTo(px, py)
                    fillPath.lineTo(px, py)
                }
                if (idx == primaryPoints.lastIndex) {
                    fillPath.lineTo(px, h)
                    fillPath.close()
                }
            }

            if (!glassEnv.isLightweightMode) {
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        listOf(primaryColor.copy(alpha = 0.28f), Color.Transparent)
                    )
                )
            }
            drawPath(
                path = pPath,
                color = primaryColor,
                style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
            )

            // Peak marker
            if (glassEnv.showChartMinMaxAvgLines && maxIdx in primaryPoints.indices) {
                val mx = (maxIdx.toFloat() / (primaryPoints.size - 1)) * w
                val my = (h - (primaryPoints[maxIdx] / maxY) * (h * 0.88f)).coerceIn(4f, h - 4f)
                drawCircle(color = Color(0xFFEF4444), radius = 3.8.dp.toPx(), center = Offset(mx, my))
            }

            // Selected Crosshair & Point
            if (selectedIdx in primaryPoints.indices) {
                val sx = (selectedIdx.toFloat() / (primaryPoints.size - 1)) * w
                val sy = (h - (primaryPoints[selectedIdx] / maxY) * (h * 0.88f)).coerceIn(3f, h - 3f)
                drawLine(
                    color = primaryColor.copy(alpha = 0.75f),
                    start = Offset(sx, 0f),
                    end = Offset(sx, h),
                    strokeWidth = 1.2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
                )
                drawCircle(color = primaryColor, radius = 4.5.dp.toPx(), center = Offset(sx, sy))
            }
        }
    }
}

/**
 * Interactive Scatter Plot + Linear Regression Line + Pearson r & R² + Point Tap Inspector
 * Used for:
 * - Dose Size vs Previous Interval (Závislost velikosti dávky na rozestupu)
 * - Next Interval vs Dose Size (Závislost další pauzy na velikosti dávky)
 * - First Dose Hour vs Daily Intake (Vliv času 1. dávky na denní spotřebu)
 * - Dose / Load vs Subjective Rating
 */
@Composable
fun InteractiveScatterRegressionChart(
    regression: LinearRegressionResult,
    xAxisLabel: String,
    yAxisLabel: String,
    accentColor: Color = Color(0xFF06B6D4),
    modifier: Modifier = Modifier
) {
    val glassEnv = LocalSubstanceGlass.current
    val pts = regression.points
    var selectedIdx by remember(pts.size) { mutableIntStateOf(if (pts.isNotEmpty()) pts.lastIndex else -1) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // Interpretation & Pearson r header badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = regression.interpretationCz,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = accentColor,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "n = ${pts.size}",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = glassEnv.textSecondary
            )
        }

        if (pts.isNotEmpty() && selectedIdx in pts.indices) {
            val p = pts[selectedIdx]
            InspectorPointBanner(
                primaryText = p.label,
                secondaryText = p.secondaryLabel,
                accentColor = Color(p.colorHex)
            )
        }

        Canvas(
            modifier = modifier.pointerInput(pts.size) {
                detectTapGestures { tap ->
                    if (pts.isEmpty() || size.width <= 10f || size.height <= 10f) return@detectTapGestures
                    val minX = regression.xStats.min.toFloat()
                    val maxX = max(regression.xStats.max.toFloat(), minX + 0.1f)
                    val minY = regression.yStats.min.toFloat()
                    val maxY = max(regression.yStats.max.toFloat(), minY + 0.1f)

                    var bestIdx = 0
                    var bestDist = Float.MAX_VALUE
                    pts.forEachIndexed { idx, pt ->
                        val px = ((pt.x - minX) / (maxX - minX)).coerceIn(0.04f, 0.96f) * size.width
                        val py = (size.height - ((pt.y - minY) / (maxY - minY)).coerceIn(0.06f, 0.94f) * size.height)
                        val d = hypot(tap.x - px, tap.y - py)
                        if (d < bestDist) {
                            bestDist = d
                            bestIdx = idx
                        }
                    }
                    selectedIdx = bestIdx
                }
            }
        ) {
            val w = size.width
            val h = size.height
            if (w <= 10f || h <= 10f) return@Canvas

            val gridCol = if (glassEnv.isLightMode) Color(0xFFCBD5E1) else Color.White.copy(alpha = 0.08f)
            listOf(0.25f, 0.5f, 0.75f).forEach { f ->
                drawLine(color = gridCol, start = Offset(0f, h * f), end = Offset(w, h * f), strokeWidth = 1.dp.toPx())
                drawLine(color = gridCol, start = Offset(w * f, 0f), end = Offset(w * f, h), strokeWidth = 1.dp.toPx())
            }

            if (pts.isEmpty()) return@Canvas

            val minX = regression.xStats.min.toFloat()
            val maxX = max(regression.xStats.max.toFloat(), minX + 0.1f)
            val minY = regression.yStats.min.toFloat()
            val maxY = max(regression.yStats.max.toFloat(), minY + 0.1f)

            fun mapX(xVal: Float): Float = ((xVal - minX) / (maxX - minX)).coerceIn(0.04f, 0.96f) * w
            fun mapY(yVal: Float): Float = h - ((yVal - minY) / (maxY - minY)).coerceIn(0.06f, 0.94f) * h

            // Mean X and Mean Y crosshair reference lines
            if (glassEnv.showChartMinMaxAvgLines) {
                val meanXScreen = mapX(regression.xStats.mean.toFloat())
                val meanYScreen = mapY(regression.yStats.mean.toFloat())
                drawLine(
                    color = Color(0xFFF59E0B).copy(alpha = 0.35f),
                    start = Offset(meanXScreen, 0f),
                    end = Offset(meanXScreen, h),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
                )
                drawLine(
                    color = Color(0xFFF59E0B).copy(alpha = 0.35f),
                    start = Offset(0f, meanYScreen),
                    end = Offset(w, meanYScreen),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
                )
            }

            // Linear Regression Trendline y = slope * x + intercept
            if (pts.size >= 2) {
                val yAtMinX = (regression.slope * minX + regression.intercept).toFloat()
                val yAtMaxX = (regression.slope * maxX + regression.intercept).toFloat()
                drawLine(
                    color = accentColor.copy(alpha = 0.85f),
                    start = Offset(mapX(minX), mapY(yAtMinX)),
                    end = Offset(mapX(maxX), mapY(yAtMaxX)),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(9f, 5f))
                )
            }

            // Scatter Points
            pts.forEachIndexed { idx, pt ->
                val px = mapX(pt.x)
                val py = mapY(pt.y)
                val col = Color(pt.colorHex)
                val isSelected = idx == selectedIdx
                if (isSelected) {
                    drawCircle(
                        color = col.copy(alpha = 0.35f),
                        radius = 9.dp.toPx(),
                        center = Offset(px, py)
                    )
                    drawCircle(
                        color = glassEnv.textPrimary,
                        radius = 5.5.dp.toPx(),
                        center = Offset(px, py),
                        style = Stroke(width = 1.8.dp.toPx())
                    )
                }
                drawCircle(
                    color = col,
                    radius = if (isSelected) 4.5.dp.toPx() else 3.6.dp.toPx(),
                    center = Offset(px, py)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Osa X: $xAxisLabel (${String.format(Locale.US, "%.1f", regression.xStats.min)} – ${String.format(Locale.US, "%.1f", regression.xStats.max)})",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                color = glassEnv.textSecondary
            )
            Text(
                text = "Osa Y: $yAxisLabel (${String.format(Locale.US, "%.1f", regression.yStats.min)} – ${String.format(Locale.US, "%.1f", regression.yStats.max)})",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                color = glassEnv.textSecondary
            )
        }
    }
}

/**
 * Interactive Chronological Inter-Dose Interval Evolution Chart (Vývoj rozestupů v čase)
 */
@Composable
fun IntervalEvolutionChart(
    points: List<IntervalEvolutionPoint>,
    recommendedMinHours: Double,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val glassEnv = LocalSubstanceGlass.current
    var selectedIdx by remember(points.size) { mutableIntStateOf(points.lastIndex.coerceAtLeast(0)) }

    val avgGap = remember(points) {
        if (points.isNotEmpty()) points.map { it.intervalHours }.average().toFloat() else 0f
    }

    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        if (points.isNotEmpty() && selectedIdx in points.indices) {
            val pt = points[selectedIdx]
            InspectorPointBanner(
                primaryText = "#${pt.index + 1} (${pt.dateLabel}) • ${pt.subtypeName}",
                secondaryText = "Pauza ${String.format(Locale.US, "%.1f", pt.intervalHours)}h (Klouz. průměr ${String.format(Locale.US, "%.1f", pt.rollingAvgHours)}h)",
                accentColor = if (pt.intervalHours < recommendedMinHours) Color(0xFFEF4444) else accentColor
            )
        }

        Canvas(
            modifier = modifier
                .pointerInput(points.size) {
                    detectTapGestures { offset ->
                        if (points.isNotEmpty() && size.width > 0) {
                            selectedIdx = ((offset.x / size.width) * points.size).toInt().coerceIn(0, points.lastIndex)
                        }
                    }
                }
                .pointerInput(points.size) {
                    detectHorizontalDragGestures { change, _ ->
                        if (points.isNotEmpty() && size.width > 0) {
                            selectedIdx = ((change.position.x / size.width) * points.size).toInt().coerceIn(0, points.lastIndex)
                        }
                    }
                }
        ) {
            val w = size.width
            val h = size.height
            if (points.isEmpty() || w <= 10f || h <= 10f) return@Canvas

            val maxHrs = max(
                recommendedMinHours.toFloat() * 1.25f,
                (points.maxOfOrNull { it.intervalHours } ?: 8f) * 1.15f
            ).coerceAtLeast(2f)

            // Recommended minimum interval dashed line
            if (recommendedMinHours > 0.1) {
                val recY = (h - (recommendedMinHours.toFloat() / maxHrs) * (h * 0.88f)).coerceIn(3f, h - 3f)
                drawLine(
                    color = Color(0xFF10B981).copy(alpha = 0.70f),
                    start = Offset(0f, recY),
                    end = Offset(w, recY),
                    strokeWidth = 1.2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f))
                )
            }

            // Mean interval line
            if (glassEnv.showChartMinMaxAvgLines && avgGap > 0f) {
                val avgY = (h - (avgGap / maxHrs) * (h * 0.88f)).coerceIn(3f, h - 3f)
                drawLine(
                    color = Color(0xFFF59E0B).copy(alpha = 0.75f),
                    start = Offset(0f, avgY),
                    end = Offset(w, avgY),
                    strokeWidth = 1.2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
                )
            }

            val slotW = w / points.size.toFloat()
            val barW = (slotW * 0.58f).coerceAtLeast(2.5f)
            val rollPath = Path()

            points.forEachIndexed { idx, pt ->
                val x = idx * slotW + (slotW - barW) / 2f
                val barH = ((pt.intervalHours / maxHrs) * (h * 0.88f)).coerceAtLeast(2f)
                val barCol = if (pt.intervalHours < recommendedMinHours) Color(0xFFEF4444) else accentColor
                drawRoundRect(
                    color = barCol.copy(alpha = if (idx == selectedIdx) 1.0f else 0.65f),
                    topLeft = Offset(x, h - barH),
                    size = Size(barW, barH),
                    cornerRadius = CornerRadius(2.dp.toPx())
                )

                val rx = idx * slotW + slotW / 2f
                val ry = (h - (pt.rollingAvgHours / maxHrs) * (h * 0.88f)).coerceIn(2f, h - 2f)
                if (idx == 0) rollPath.moveTo(rx, ry) else rollPath.lineTo(rx, ry)
            }

            drawPath(
                path = rollPath,
                color = if (glassEnv.isLightMode) Color(0xFF0F172A) else Color.White,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}

/**
 * Interactive Subtype Stacked Daily Consumption Chart
 */
@Composable
fun SubtypeStackedTimelineChart(
    dailyPoints: List<DailySubtypeStackedPoint>,
    subtypes: List<SubtypeAnalyticsMetric>,
    unit: String,
    modifier: Modifier = Modifier
) {
    val glassEnv = LocalSubstanceGlass.current
    val colorMap = remember(subtypes) {
        subtypes.associate { it.subtypeName to Color(it.colorHex) }
    }
    var selectedIdx by remember(dailyPoints.size) { mutableIntStateOf(dailyPoints.lastIndex.coerceAtLeast(0)) }

    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        if (dailyPoints.isNotEmpty() && selectedIdx in dailyPoints.indices) {
            val pt = dailyPoints[selectedIdx]
            val details = pt.perSubtypeUnits.entries
                .sortedByDescending { it.value }
                .joinToString(", ") { "${it.key}: ${String.format(Locale.US, "%.1f", it.value)} $unit" }
                .ifBlank { "Žádná dávka" }
            InspectorPointBanner(
                primaryText = "${pt.label} • $details",
                secondaryText = "Celkem ${String.format(Locale.US, "%.1f", pt.totalUnits)} $unit",
                accentColor = subtypes.firstOrNull()?.let { Color(it.colorHex) } ?: Color(0xFF06B6D4)
            )
        }

        Canvas(
            modifier = modifier
                .pointerInput(dailyPoints.size) {
                    detectTapGestures { offset ->
                        if (dailyPoints.isNotEmpty() && size.width > 0) {
                            selectedIdx = ((offset.x / size.width) * dailyPoints.size).toInt().coerceIn(0, dailyPoints.lastIndex)
                        }
                    }
                }
                .pointerInput(dailyPoints.size) {
                    detectHorizontalDragGestures { change, _ ->
                        if (dailyPoints.isNotEmpty() && size.width > 0) {
                            selectedIdx = ((change.position.x / size.width) * dailyPoints.size).toInt().coerceIn(0, dailyPoints.lastIndex)
                        }
                    }
                }
        ) {
            val w = size.width
            val h = size.height
            if (dailyPoints.isEmpty() || w <= 10f || h <= 10f) return@Canvas

            val maxTotal = (dailyPoints.maxOfOrNull { it.totalUnits } ?: 1.0).coerceAtLeast(1.0).toFloat() * 1.12f
            val slotW = w / dailyPoints.size.toFloat()
            val barW = (slotW * 0.68f).coerceAtLeast(2.5f)

            dailyPoints.forEachIndexed { idx, pt ->
                val x = idx * slotW + (slotW - barW) / 2f
                var currentBottom = h
                pt.perSubtypeUnits.forEach { (sName, uVal) ->
                    val segH = (uVal.toFloat() / maxTotal) * (h * 0.88f)
                    val top = (currentBottom - segH).coerceAtLeast(0f)
                    val col = colorMap[sName] ?: Color(0xFF06B6D4)
                    drawRoundRect(
                        color = col.copy(alpha = if (idx == selectedIdx) 1.0f else 0.78f),
                        topLeft = Offset(x, top),
                        size = Size(barW, (currentBottom - top).coerceAtLeast(1.5f)),
                        cornerRadius = CornerRadius(2.dp.toPx())
                    )
                    currentBottom = top - 1f
                }

                if (idx == selectedIdx) {
                    val cx = idx * slotW + slotW / 2f
                    drawLine(
                        color = glassEnv.textPrimary.copy(alpha = 0.65f),
                        start = Offset(cx, 0f),
                        end = Offset(cx, h),
                        strokeWidth = 1.2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
                    )
                }
            }
        }
    }
}

/**
 * Threshold Tier Stacked Horizontal Bar
 */
@Composable
fun ThresholdTierHorizontalStackedBar(
    tierCounts: Map<DoseTier, Int>,
    modifier: Modifier = Modifier
) {
    val glassEnv = LocalSubstanceGlass.current
    val total = tierCounts.values.sum().coerceAtLeast(1)
    val tiers = listOf(
        DoseTier.NEGLIGIBLE,
        DoseTier.LIGHT,
        DoseTier.MEDIUM,
        DoseTier.STRONG,
        DoseTier.DANGEROUS
    )

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(50))
                .background(if (glassEnv.isLightMode) Color(0xFFE2E8F0) else Color.White.copy(alpha = 0.06f))
        ) {
            tiers.forEach { t ->
                val count = tierCounts[t] ?: 0
                if (count > 0) {
                    val weight = (count.toFloat() / total).coerceAtLeast(0.04f)
                    Box(
                        modifier = Modifier
                            .weight(weight)
                            .fillMaxHeight()
                            .background(t.primaryColor)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            tiers.forEach { t ->
                val count = tierCounts[t] ?: 0
                val pct = ((count.toFloat() / total) * 100).roundToInt()
                Text(
                    text = "${t.titleCz.take(5)}. $pct% ($count×)",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                    color = t.primaryColor
                )
            }
        }
    }
}
