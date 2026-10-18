package com.antigravity.substance.ui.dashboard

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.substance.data.preferences.UserSettings
import com.antigravity.substance.domain.model.ActiveSubstanceSession
import com.antigravity.substance.domain.model.DoseTier
import com.antigravity.substance.domain.model.PkPhase
import com.antigravity.substance.ui.theme.BentoMicroHeader
import com.antigravity.substance.ui.theme.DynamicAmbientGlowBox
import com.antigravity.substance.ui.theme.FrostedBentoCard
import com.antigravity.substance.ui.theme.LocalSubstanceGlass
import com.antigravity.substance.ui.theme.SquircleIconBadge
import com.antigravity.substance.ui.theme.SubstanceIcons
import com.antigravity.substance.ui.theme.TierBadgePill
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

/**
 * SCREEN 1: ZERO-SCROLL FULL-VIEWPORT DASHBOARD COCKPIT
 * Fits 100% inside the device viewport above the bottom nav bar using proportional
 * Modifier.weight(...) Bento Grid rows (0.13f + 0.42f + 0.24f + 0.21f = 1.00f).
 */
@Composable
fun DashboardScreen(
    state: DashboardUiState,
    onSelectSessionTab: (Int) -> Unit,
    onCycleHeroMode: () -> Unit,
    onCycleFavoritesPage: () -> Unit,
    onFlipConsumptionCard: () -> Unit,
    onQuickLogPreset: (QuickPresetDockItem) -> Unit,
    onUndoQuickLog: () -> Unit,
    onNavigateToLogDose: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedSession = state.activeSessions.getOrNull(state.selectedSessionTabIndex)
    val activeAccentColor = selectedSession?.let { Color(it.substance.colorHex) }
        ?: state.highestActiveTier.primaryColor

    DynamicAmbientGlowBox(
        activeTier = state.highestActiveTier,
        overrideAccentColor = activeAccentColor,
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen_root")
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                // ROW 1 (Weight 0.13f): Live Bio-Load Header & Quick Telemetry Bar
                TopBioLoadHeaderRow(
                    state = state,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.13f)
                )

                // ROW 2 (Weight 0.42f): Hero Centerpiece — Live Active Substances & Stacked PK Monitor
                HeroActivePkMonitorCard(
                    state = state,
                    onSelectTab = onSelectSessionTab,
                    onCycleHeroMode = onCycleHeroMode,
                    onQuickAddDose = { onNavigateToLogDose(selectedSession?.substance?.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.42f)
                )

                // ROW 3 (Weight 0.24f): Middle Bento Row — Time Since Last Use & Today's Consumption/Cost Flip Card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.24f),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    TimeSinceLastUseBentoCard(
                        state = state,
                        onCyclePage = onCycleFavoritesPage,
                        modifier = Modifier
                            .weight(0.54f)
                            .fillMaxHeight()
                    )

                    TodayConsumptionFlipBentoCard(
                        state = state,
                        onFlipCard = onFlipConsumptionCard,
                        modifier = Modifier
                            .weight(0.46f)
                            .fillMaxHeight()
                    )
                }

                // ROW 4 (Weight 0.21f): Bottom Bento Row — 24h Circadian Timeline, Sleep Readiness & 1-Tap Quick-Log Dock
                BottomCircadianAndQuickDockCard(
                    state = state,
                    onQuickLogPreset = onQuickLogPreset,
                    onUndoQuickLog = onUndoQuickLog,
                    onOpenCustomLogger = { onNavigateToLogDose(null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.21f)
                )
            }
        }
    }
}

@Composable
private fun TopBioLoadHeaderRow(
    state: DashboardUiState,
    modifier: Modifier = Modifier
) {
    FrostedBentoCard(
        modifier = modifier.testTag("dashboard_bio_load_header"),
        cornerRadius = 18.dp,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Animated Bio-Load Ring + Cockpit Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(0.36f)
            ) {
                Box(
                    modifier = Modifier.size(38.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val ringColor = if (state.highestActiveTier == DoseTier.INACTIVE) {
                        Color(0xFF38BDF8)
                    } else {
                        state.highestActiveTier.primaryColor
                    }
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val stroke = 4.dp.toPx()
                        drawArc(
                            color = Color.White.copy(alpha = 0.10f),
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = ringColor,
                            startAngle = -90f,
                            sweepAngle = (state.totalBioLoadFraction * 360f).coerceIn(18f, 360f),
                            useCenter = false,
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                    }
                    Text(
                        text = "${(state.totalBioLoadFraction * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(verticalArrangement = Arrangement.Center) {
                    Text(
                        text = "BIO-COCKPIT",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            letterSpacing = 0.8.sp,
                            color = Color(0xFF38BDF8)
                        ),
                        maxLines = 1
                    )
                    Text(
                        text = SimpleDateFormat("EEE, d MMM", Locale.US).format(Date(state.nowMillis)),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White,
                        maxLines = 1
                    )
                }
            }

            // Right: 3 Compact Telemetry Micro-Pills
            Row(
                modifier = Modifier.weight(0.64f),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pill 1: Today's Spend
                MicroTelemetryPill(
                    label = "SPEND",
                    value = state.settings.formatCurrency(state.todayTotalSpend),
                    subValue = if (state.spendVs7dAvgPercent >= 0) "+${state.spendVs7dAvgPercent}% vs 7d" else "${state.spendVs7dAvgPercent}% vs 7d",
                    accentColor = Color(0xFF06B6D4),
                    modifier = Modifier.weight(1f)
                )
                // Pill 2: Today's Doses
                MicroTelemetryPill(
                    label = "DOSES",
                    value = "${state.todayDosesCount} logs",
                    subValue = "${state.todayDistinctSubstancesCount} active",
                    accentColor = Color(0xFF10B981),
                    modifier = Modifier.weight(0.9f)
                )
                // Pill 3: Safety Status Badge
                MicroTelemetryPill(
                    label = "STATUS",
                    value = if (state.safetyStatusLabel == "LIMIT EXCEEDED") "REDLINE" else state.safetyStatusLabel,
                    subValue = state.highestActiveTier.titleEn,
                    accentColor = state.safetyStatusTier.primaryColor,
                    modifier = Modifier.weight(0.95f)
                )
            }
        }
    }
}

@Composable
private fun MicroTelemetryPill(
    label: String,
    value: String,
    subValue: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(11.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(Color.White.copy(alpha = 0.04f))
            .border(0.8.dp, accentColor.copy(alpha = 0.30f), shape)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
            color = Color(0xFF94A3B8),
            maxLines = 1
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 10.5.sp),
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = subValue,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
            color = accentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun HeroActivePkMonitorCard(
    state: DashboardUiState,
    onSelectTab: (Int) -> Unit,
    onCycleHeroMode: () -> Unit,
    onQuickAddDose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sessions = state.activeSessions
    val isAllStackedComposite = sessions.size > 1 && state.selectedSessionTabIndex == sessions.size
    val activeSession = sessions.getOrNull(state.selectedSessionTabIndex) ?: sessions.firstOrNull()
    val accentColor = activeSession?.let { Color(it.substance.colorHex) } ?: Color(0xFF06B6D4)

    FrostedBentoCard(
        modifier = modifier.testTag("dashboard_hero_pk_card"),
        cornerRadius = 22.dp,
        accentColor = accentColor,
        contentPadding = PaddingValues(10.dp),
        onClick = onCycleHeroMode
    ) {
        if (sessions.isEmpty() || activeSession == null) {
            // Clean empty state with 1-tap launch into Log Dose
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Timeline,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Zero Active Pharmacokinetic Load",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Text(
                    text = "All plasma levels cleared to baseline • Tap below to log intake",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF94A3B8)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF06B6D4).copy(alpha = 0.22f))
                        .border(1.dp, Color(0xFF06B6D4), RoundedCornerShape(50))
                        .clickable(onClick = onQuickAddDose)
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "+ Log First Dose",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFF38BDF8)
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Strip: Active Substance Pill Switcher + Mode Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        sessions.forEachIndexed { idx, sess ->
                            val selected = !isAllStackedComposite && idx == state.selectedSessionTabIndex
                            val subColor = Color(sess.substance.colorHex)
                            val pillShape = RoundedCornerShape(50)
                            Row(
                                modifier = Modifier
                                    .clip(pillShape)
                                    .background(
                                        if (selected) subColor.copy(alpha = 0.25f)
                                        else Color.White.copy(alpha = 0.05f)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (selected) subColor else Color.White.copy(alpha = 0.12f),
                                        shape = pillShape
                                    )
                                    .clickable { onSelectTab(idx) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = SubstanceIcons.iconForName(sess.substance.iconName),
                                    contentDescription = null,
                                    tint = subColor,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = sess.substance.name,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = Color.White,
                                    maxLines = 1
                                )
                                if (sess.activeLogs.size > 1) {
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "${sess.activeLogs.size}x",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        color = subColor
                                    )
                                }
                            }
                        }

                        if (sessions.size > 1) {
                            val pillShape = RoundedCornerShape(50)
                            Row(
                                modifier = Modifier
                                    .clip(pillShape)
                                    .background(
                                        if (isAllStackedComposite) Color(0xFF8B5CF6).copy(alpha = 0.28f)
                                        else Color.White.copy(alpha = 0.05f)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isAllStackedComposite) Color(0xFF8B5CF6) else Color.White.copy(alpha = 0.12f),
                                        shape = pillShape
                                    )
                                    .clickable { onSelectTab(sessions.size) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Layers,
                                    contentDescription = null,
                                    tint = Color(0xFFC084FC),
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "ALL STACKED",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color.White
                                )
                            }
                        }
                    }

                    TierBadgePill(
                        tier = activeSession.currentTier,
                        compact = true
                    )
                }

                // Center Area: Either Live Multi-Dose Superposition Curve OR Stacked Dose Breakdown Matrix
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 4.dp)
                ) {
                    if (state.heroCardDisplayMode == 0) {
                        LiveSuperpositionCurveCanvas(
                            session = activeSession,
                            allSessions = sessions,
                            isCompositeMode = isAllStackedComposite,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        ActiveSessionDoseBreakdownView(
                            session = activeSession,
                            settings = state.settings,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Bottom Live Second-by-Second Telemetry Strip
                LivePkTelemetryFooterStrip(
                    session = activeSession,
                    settings = state.settings
                )
            }
        }
    }
}

@Composable
private fun LiveSuperpositionCurveCanvas(
    session: ActiveSubstanceSession,
    allSessions: List<ActiveSubstanceSession>,
    isCompositeMode: Boolean,
    modifier: Modifier = Modifier
) {
    val glass = LocalSubstanceGlass.current
    val orbPulse = if (glass.isLightweightMode) {
        1.0f
    } else {
        val infiniteTransition = rememberInfiniteTransition(label = "orbPulse")
        val animated by infiniteTransition.animateFloat(
            initialValue = 0.7f,
            targetValue = 1.35f,
            animationSpec = infiniteRepeatable(
                animation = tween(1100, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "orbScale"
        )
        animated
    }

    val sub = session.substance
    val primaryColor = Color(sub.colorHex)
    val points = session.curvePoints

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            if (w <= 10f || h <= 10f || points.size < 2) return@Canvas

            val maxScaleY = max(
                sub.thresholdStrong * 1.18,
                session.combinedPeakLoad * 1.15
            ).coerceAtLeast(1.0)

            // 1. Horizontal threshold zone bands (Light, Medium, Strong, Redline)
            val thresholds = listOf(
                sub.thresholdLight to DoseTier.LIGHT.primaryColor,
                sub.thresholdMedium to DoseTier.MEDIUM.primaryColor,
                sub.thresholdStrong to DoseTier.STRONG.primaryColor
            )
            thresholds.forEach { (thVal, thCol) ->
                val y = (h - (thVal / maxScaleY).toFloat() * h).coerceIn(0f, h)
                drawLine(
                    color = thCol.copy(alpha = 0.22f),
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                )
            }

            // Time window is -120 mins to +480 mins (total 600 mins)
            val minRel = -120f
            val maxRel = 480f
            val spanMins = maxRel - minRel

            fun xForRelMins(relMins: Float): Float =
                ((relMins - minRel) / spanMins).coerceIn(0f, 1f) * w

            // 2. Vertical NOW cursor line (at relMins = 0f -> 20% from left)
            val nowX = xForRelMins(0f)
            drawLine(
                color = Color.White.copy(alpha = 0.30f),
                start = Offset(nowX, 0f),
                end = Offset(nowX, h),
                strokeWidth = 1.2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
            )

            // 3. Individual dose intake markers (dashed vertical lines for t_1, t_2...)
            val nowMs = points.firstOrNull { it.relativeMinutesFromNow >= 0f }?.timestampMillis ?: 0L
            session.doseTimestampsMillis.forEach { doseMs ->
                val relM = (doseMs - nowMs) / 60_000f
                if (relM in minRel..maxRel) {
                    val dx = xForRelMins(relM)
                    drawLine(
                        color = primaryColor.copy(alpha = 0.45f),
                        start = Offset(dx, h * 0.15f),
                        end = Offset(dx, h),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
                    )
                    drawCircle(
                        color = primaryColor,
                        radius = 3.dp.toPx(),
                        center = Offset(dx, h - 3.dp.toPx())
                    )
                }
            }

            if (isCompositeMode) {
                // Draw all active sessions normalized
                allSessions.forEach { s ->
                    val sColor = Color(s.substance.colorHex)
                    val sMax = max(s.substance.thresholdStrong * 1.15, s.combinedPeakLoad * 1.15).coerceAtLeast(1.0)
                    val path = Path()
                    s.curvePoints.forEachIndexed { idx, pt ->
                        val px = xForRelMins(pt.relativeMinutesFromNow)
                        val py = (h - (pt.activeEffectLoad / sMax).toFloat() * (h * 0.88f)).coerceIn(2f, h)
                        if (idx == 0) path.moveTo(px, py) else path.lineTo(px, py)
                    }
                    drawPath(
                        path = path,
                        color = sColor,
                        style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            } else {
                // 4. Residual Plasma Half-Life Curve (subtle dashed background wave)
                val plasmaPath = Path()
                points.forEachIndexed { idx, pt ->
                    val px = xForRelMins(pt.relativeMinutesFromNow)
                    val py = (h - (pt.plasmaConcentration / maxScaleY).toFloat() * (h * 0.88f)).coerceIn(2f, h)
                    if (idx == 0) plasmaPath.moveTo(px, py) else plasmaPath.lineTo(px, py)
                }
                drawPath(
                    path = plasmaPath,
                    color = primaryColor.copy(alpha = 0.35f),
                    style = Stroke(
                        width = 1.4.dp.toPx(),
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f))
                    )
                )

                // 5. Combined Active Effect Superposition Curve + Area Gradient Fill
                val effectPath = Path()
                val fillPath = Path()
                points.forEachIndexed { idx, pt ->
                    val px = xForRelMins(pt.relativeMinutesFromNow)
                    val py = (h - (pt.activeEffectLoad / maxScaleY).toFloat() * (h * 0.88f)).coerceIn(2f, h)
                    if (idx == 0) {
                        effectPath.moveTo(px, py)
                        fillPath.moveTo(px, h)
                        fillPath.lineTo(px, py)
                    } else {
                        effectPath.lineTo(px, py)
                        fillPath.lineTo(px, py)
                    }
                    if (idx == points.lastIndex) {
                        fillPath.lineTo(px, h)
                        fillPath.close()
                    }
                }

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.40f),
                            session.currentTier.primaryColor.copy(alpha = 0.14f),
                            Color.Transparent
                        )
                    )
                )

                drawPath(
                    path = effectPath,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            primaryColor,
                            session.combinedPeakTier.primaryColor,
                            primaryColor.copy(alpha = 0.7f)
                        )
                    ),
                    style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round)
                )

                // 6. Highlighted Combined Shifted Peak Marker
                val peakRelMins = (session.combinedPeakTimestampMillis - nowMs) / 60_000f
                if (peakRelMins in minRel..maxRel) {
                    val peakX = xForRelMins(peakRelMins)
                    val peakY = (h - (session.combinedPeakLoad / maxScaleY).toFloat() * (h * 0.88f)).coerceIn(4f, h - 4f)
                    drawCircle(
                        color = session.combinedPeakTier.primaryColor.copy(alpha = 0.35f),
                        radius = 7.dp.toPx(),
                        center = Offset(peakX, peakY)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 3.2.dp.toPx(),
                        center = Offset(peakX, peakY)
                    )
                }

                // 7. Glowing Pulsing Orb at exact NOW position on the curve
                val nowY = (h - (session.currentActiveLoad / maxScaleY).toFloat() * (h * 0.88f)).coerceIn(4f, h - 4f)
                drawCircle(
                    color = primaryColor.copy(alpha = 0.38f),
                    radius = 9.dp.toPx() * orbPulse,
                    center = Offset(nowX, nowY)
                )
                drawCircle(
                    color = primaryColor,
                    radius = 4.5.dp.toPx(),
                    center = Offset(nowX, nowY)
                )
                drawCircle(
                    color = Color.White,
                    radius = 2.dp.toPx(),
                    center = Offset(nowX, nowY)
                )
            }
        }

        // Overlay Legend Badges (Top-Right inside Graph)
        val timeFmt = SimpleDateFormat("HH:mm", Locale.US)
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (session.activeLogs.size > 1 && session.peakShiftMinutes > 0) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFF59E0B).copy(alpha = 0.18f))
                        .border(0.8.dp, Color(0xFFF59E0B).copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "STACKED PEAK SHIFT +${session.peakShiftMinutes}m (${timeFmt.format(Date(session.combinedPeakTimestampMillis))})",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                        color = Color(0xFFFBBF24)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.45f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Peak ${session.substance.defaultUnit}: ${String.format(Locale.US, "%.1f", session.combinedPeakLoad)} @ ${timeFmt.format(Date(session.combinedPeakTimestampMillis))}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                        color = Color(0xFFCBD5E1)
                    )
                }
            }
        }

        // X-axis horizon labels at bottom of graph
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("-2h", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp), color = Color(0xFF64748B))
            Text("NOW", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, fontWeight = FontWeight.Bold), color = primaryColor)
            Text("+2h", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp), color = Color(0xFF64748B))
            Text("+5h", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp), color = Color(0xFF64748B))
            Text("+8h", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp), color = Color(0xFF64748B))
        }
    }
}

@Composable
private fun ActiveSessionDoseBreakdownView(
    session: ActiveSubstanceSession,
    settings: UserSettings,
    modifier: Modifier = Modifier
) {
    val timeFmt = SimpleDateFormat("HH:mm", Locale.US)
    val sub = session.substance
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .padding(8.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SUPERPOSITION DOSE STACK (${session.activeLogs.size} OVERLAPPING)",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.5.sp,
                    color = Color(sub.colorHex),
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = "Tap card for PK Curve",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                color = Color(0xFF94A3B8)
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            session.activeLogs.takeLast(3).forEachIndexed { i, log ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.04f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Dose #${i + 1} • ${timeFmt.format(Date(log.timestampMillis))} (${log.roaName})",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = Color.White
                    )
                    Text(
                        text = "${settings.formatDose(log.doseAmount, log.unit)} (${settings.formatCurrency(log.cost)})",
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 10.5.sp),
                        color = Color(sub.colorHex)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Onset: ${sub.onsetMinutes}m • Comeup: ${sub.comeupMinutes}m • Peak: ${sub.peakMinutes}m",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = Color(0xFF94A3B8)
            )
            Text(
                text = "t½ = ${sub.halfLifeHours}h",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = Color(0xFF38BDF8)
            )
        }
    }
}

@Composable
private fun LivePkTelemetryFooterStrip(
    session: ActiveSubstanceSession,
    settings: UserSettings
) {
    val sub = session.substance
    val timeFmt = SimpleDateFormat("HH:mm", Locale.US)
    val countdownFormatted = formatCountdownHms(session.phaseCountdownSeconds)

    val phaseText = when (session.currentPhase) {
        PkPhase.ONSET_COUNTDOWN -> "Onset starts in $countdownFormatted"
        PkPhase.CLIMBING_TO_PEAK -> "Climbing • Peak in $countdownFormatted (${timeFmt.format(Date(session.phaseTargetTimestampMillis))})"
        PkPhase.PEAKING_NOW -> "PEAKING NOW • $countdownFormatted left"
        PkPhase.OFFSET_COMEDOWN -> "Comedown • Clear in $countdownFormatted (${timeFmt.format(Date(session.activeEffectsEndTimestampMillis))})"
        PkPhase.RESIDUAL_HALF_LIFE -> "Plasma tail • Clear at ${timeFmt.format(Date(session.halfLifeClearTimestampMillis))}"
        PkPhase.INACTIVE -> "Baseline"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.35f))
            .border(0.8.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left: Active equivalent dose right now
        Column(modifier = Modifier.weight(0.38f)) {
            Text(
                text = "ACTIVE EQUIVALENT LOAD",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                color = Color(0xFF94A3B8),
                maxLines = 1
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "${String.format(Locale.US, "%.1f", session.currentActiveLoad)} ${sub.defaultUnit}",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 13.sp,
                        color = Color(sub.colorHex)
                    ),
                    maxLines = 1
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "/ ${settings.formatDose(session.totalIngestedTodayUnits, sub.defaultUnit)} today",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                    color = Color(0xFF94A3B8),
                    maxLines = 1
                )
            }
        }

        // Right: Live Second-by-Second Phase Countdown & Half-Life Clear Time
        Column(
            modifier = Modifier.weight(0.62f),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = phaseText,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 10.5.sp,
                    color = Color.White
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "t½ Plasma clear @ ${timeFmt.format(Date(session.halfLifeClearTimestampMillis))} • Peak ${String.format(Locale.US, "%.1f", session.combinedPeakLoad)} ${sub.defaultUnit}",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                color = Color(0xFF94A3B8),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TimeSinceLastUseBentoCard(
    state: DashboardUiState,
    onCyclePage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val items = state.timeSinceLastUseItems
    val pageSize = 3
    val totalPages = ((items.size + pageSize - 1) / pageSize).coerceAtLeast(1)
    val pageIdx = state.favoritePageOffset % totalPages
    val visibleItems = items.drop(pageIdx * pageSize).take(pageSize)

    FrostedBentoCard(
        modifier = modifier.testTag("dashboard_time_since_last_card"),
        cornerRadius = 18.dp,
        contentPadding = PaddingValues(9.dp),
        onClick = onCyclePage
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            BentoMicroHeader(
                title = "TIME SINCE LAST",
                icon = Icons.Default.History,
                accentColor = Color(0xFF38BDF8),
                trailingText = if (totalPages > 1) "${pageIdx + 1}/$totalPages" else null
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                visibleItems.forEach { item ->
                    val subColor = Color(item.substance.colorHex)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(0.52f)
                        ) {
                            SquircleIconBadge(
                                icon = SubstanceIcons.iconForName(item.substance.iconName),
                                colorHex = item.substance.colorHex,
                                size = 22.dp,
                                iconSize = 12.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = item.substance.name,
                                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 11.5.sp),
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = state.settings.formatDose(item.lastDoseAmount, item.substance.defaultUnit),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                                    color = Color(0xFF94A3B8),
                                    maxLines = 1
                                )
                            }
                        }

                        Column(
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier.weight(0.48f)
                        ) {
                            Text(
                                text = formatElapsedCompact(item.elapsedSeconds),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontSize = 10.5.sp,
                                    color = subColor
                                ),
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            // Recovery progress bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .height(3.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.08f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(item.recoveryProgressFraction)
                                        .background(subColor)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TodayConsumptionFlipBentoCard(
    state: DashboardUiState,
    onFlipCard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val items = state.todayConsumptionItems.take(3)

    FrostedBentoCard(
        modifier = modifier.testTag("dashboard_today_consumption_card"),
        cornerRadius = 18.dp,
        contentPadding = PaddingValues(9.dp),
        onClick = onFlipCard
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            BentoMicroHeader(
                title = if (state.showCostInsteadOfUnits) "TODAY COST" else "TODAY UNITS",
                icon = Icons.Default.SwapHoriz,
                accentColor = Color(0xFF10B981),
                trailingText = "FLIP"
            )

            if (items.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No doses logged today",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                        color = Color(0xFF64748B)
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items.forEach { item ->
                        val subColor = Color(item.substance.colorHex)
                        val barColor = if (item.limitFraction > 1.0f) Color(0xFFEF4444) else subColor
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.substance.name,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(0.45f)
                                )
                                Text(
                                    text = if (state.showCostInsteadOfUnits) {
                                        "${state.settings.formatCurrency(item.todayTotalCost)} (${item.doseCountToday}x)"
                                    } else {
                                        "${state.settings.formatDose(item.todayTotalUnits)}/${state.settings.formatDose(item.dailySafeLimit, item.substance.defaultUnit)}"
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.5.sp,
                                        color = barColor
                                    ),
                                    maxLines = 1
                                )
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.5.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.08f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(item.limitFraction.coerceIn(0.06f, 1f))
                                        .background(barColor)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomCircadianAndQuickDockCard(
    state: DashboardUiState,
    onQuickLogPreset: (QuickPresetDockItem) -> Unit,
    onUndoQuickLog: () -> Unit,
    onOpenCustomLogger: () -> Unit,
    modifier: Modifier = Modifier
) {
    FrostedBentoCard(
        modifier = modifier.testTag("dashboard_bottom_dock_card"),
        cornerRadius = 18.dp,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 7.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top half: 24-Hour Circadian Intake Timeline Strip + Sleep Readiness Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "24H CIRCADIAN TIMELINE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(0xFF94A3B8)
                    )
                }

                // Sleep Readiness Indicator at configured bedtime
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (state.sleepReadinessOptimal) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (state.sleepReadinessOptimal) Color(0xFF10B981) else Color(0xFFF59E0B),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = if (state.sleepReadinessOptimal) {
                            "Sleep @ ${state.settings.bedtimeHour24}:00 Clear"
                        } else {
                            "${state.sleepWarningSubstanceName} active @ ${state.settings.bedtimeHour24}:00"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = if (state.sleepReadinessOptimal) Color(0xFF10B981) else Color(0xFFFBBF24)
                    )
                }
            }

            // 24-Hour Timeline Bar with glowing dose gems & bedtime marker
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
            ) {
                val w = size.width
                val h = size.height
                val midY = h / 2f

                // Track bar
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.07f),
                    topLeft = Offset(0f, midY - 2.5.dp.toPx()),
                    size = Size(w, 5.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                )

                // Hour ticks (0, 6, 12, 18, 24)
                listOf(0f, 0.25f, 0.5f, 0.75f, 1f).forEach { frac ->
                    drawCircle(
                        color = Color.White.copy(alpha = 0.22f),
                        radius = 1.5.dp.toPx(),
                        center = Offset(w * frac, midY)
                    )
                }

                // Bedtime marker
                val bedX = (state.settings.bedtimeHour24 / 24f).coerceIn(0f, 1f) * w
                drawLine(
                    color = Color(0xFF8B5CF6),
                    start = Offset(bedX, 0f),
                    end = Offset(bedX, h),
                    strokeWidth = 1.5.dp.toPx()
                )

                // Logged Gems
                state.todayTimelineGems.forEach { gem ->
                    val gx = (gem.hourOfDayFraction * w).coerceIn(4f, w - 4f)
                    val gColor = Color(gem.substance.colorHex)
                    drawCircle(
                        color = gColor.copy(alpha = 0.35f),
                        radius = 6.dp.toPx(),
                        center = Offset(gx, midY)
                    )
                    drawCircle(
                        color = gColor,
                        radius = 3.5.dp.toPx(),
                        center = Offset(gx, midY)
                    )
                }
            }

            // Bottom half: 1-Tap Quick-Log Dock (or Undo confirmation strip)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (state.lastQuickLoggedLabel != null) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF10B981).copy(alpha = 0.18f))
                            .border(1.dp, Color(0xFF10B981).copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .clickable(onClick = onUndoQuickLog)
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Logged ${state.lastQuickLoggedLabel} • PK Stack Updated",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                            color = Color.White,
                            maxLines = 1
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Undo,
                                contentDescription = "Undo quick log",
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "UNDO",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color(0xFF10B981)
                            )
                        }
                    }
                } else {
                    state.quickPresetDockItems.forEach { preset ->
                        val pColor = Color(preset.substance.colorHex)
                        val shape = RoundedCornerShape(10.dp)
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(shape)
                                .background(pColor.copy(alpha = 0.14f))
                                .border(0.8.dp, pColor.copy(alpha = 0.45f), shape)
                                .clickable { onQuickLogPreset(preset) }
                                .padding(horizontal = 6.dp, vertical = 5.dp)
                                .testTag("quick_log_${preset.substance.name.lowercase()}"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = pColor,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = preset.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Custom dose button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(0.8.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                            .clickable(onClick = onOpenCustomLogger)
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                            .testTag("dashboard_open_custom_logger"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Open Dose Studio",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

fun formatCountdownHms(totalSeconds: Long): String {
    val hrs = totalSeconds / 3600
    val mins = (totalSeconds % 3600) / 60
    val secs = totalSeconds % 60
    return if (hrs > 0) {
        String.format(Locale.US, "%02dh %02dm %02ds", hrs, mins, secs)
    } else {
        String.format(Locale.US, "%02dm %02ds", mins, secs)
    }
}

fun formatElapsedCompact(totalSeconds: Long): String {
    val days = totalSeconds / 86_400
    val hrs = (totalSeconds % 86_400) / 3600
    val mins = (totalSeconds % 3600) / 60
    return when {
        days > 0 -> "${days}d ${String.format(Locale.US, "%02dh", hrs)} ago"
        hrs > 0 -> "${String.format(Locale.US, "%02dh %02dm", hrs, mins)} ago"
        else -> "${String.format(Locale.US, "%02dm", mins)} ago"
    }
}
