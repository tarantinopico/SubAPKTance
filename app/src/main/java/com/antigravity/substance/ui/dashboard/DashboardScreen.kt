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
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.antigravity.substance.ui.theme.LatkyPhaseProgressStrip
import com.antigravity.substance.ui.theme.LocalSubstanceGlass
import com.antigravity.substance.ui.theme.RobotoMonoFontFamily
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
 * Modifier.weight(...) Bento Grid rows (0.12f + 0.40f + 0.28f + 0.20f = 1.00f),
 * with internal weighted items so cards have zero dead space.
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
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // ROW 1 (Weight 0.12f): Live Bio-Load Header & Quick Telemetry Bar
                TopBioLoadHeaderRow(
                    state = state,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.12f)
                )

                // ROW 2 (Weight 0.40f): Hero Centerpiece — Live Active Substances & Stacked PK Monitor
                HeroActivePkMonitorCard(
                    state = state,
                    onSelectTab = onSelectSessionTab,
                    onCycleHeroMode = onCycleHeroMode,
                    onQuickAddDose = { onNavigateToLogDose(selectedSession?.substance?.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.40f)
                )

                // ROW 3 (Weight 0.28f): Middle Bento Row — Time Since Last Use & Today's Consumption/Cost Flip Card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.28f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TimeSinceLastUseBentoCard(
                        state = state,
                        onCyclePage = onCycleFavoritesPage,
                        modifier = Modifier
                            .weight(0.53f)
                            .fillMaxHeight()
                    )

                    TodayConsumptionFlipBentoCard(
                        state = state,
                        onFlipCard = onFlipConsumptionCard,
                        modifier = Modifier
                            .weight(0.47f)
                            .fillMaxHeight()
                    )
                }

                // ROW 4 (Weight 0.20f): Bottom Bento Row — 24h Circadian Timeline, Sleep Readiness & 1-Tap Quick-Log Dock
                BottomCircadianAndQuickDockCard(
                    state = state,
                    onQuickLogPreset = onQuickLogPreset,
                    onUndoQuickLog = onUndoQuickLog,
                    onOpenCustomLogger = { onNavigateToLogDose(null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.20f)
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
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Left: Animated Bio-Load Ring + Cockpit Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(0.35f)
            ) {
                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val ringColor = if (state.highestActiveTier == DoseTier.INACTIVE) {
                        Color(0xFF38BDF8)
                    } else {
                        state.highestActiveTier.primaryColor
                    }
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val stroke = 4.8.dp.toPx()
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
                            fontFamily = RobotoMonoFontFamily,
                            fontSize = 11.sp,
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
                            fontSize = 9.5.sp,
                            letterSpacing = 0.8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8)
                        ),
                        maxLines = 1
                    )
                    Text(
                        text = SimpleDateFormat("EEE, d MMM", Locale.US).format(Date(state.nowMillis)),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White,
                        maxLines = 1
                    )
                }
            }

            // Right: 3 Full-Height Telemetry Boxes
            Row(
                modifier = Modifier
                    .weight(0.65f)
                    .fillMaxHeight(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MicroTelemetryPill(
                    label = "SPEND",
                    value = state.settings.formatCurrency(state.todayTotalSpend),
                    subValue = if (state.spendVs7dAvgPercent >= 0) "+${state.spendVs7dAvgPercent}% vs 7d" else "${state.spendVs7dAvgPercent}% vs 7d",
                    accentColor = Color(0xFF06B6D4),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
                MicroTelemetryPill(
                    label = "DOSES",
                    value = "${state.todayDosesCount} logs",
                    subValue = "${state.todayDistinctSubstancesCount} active",
                    accentColor = Color(0xFF10B981),
                    modifier = Modifier
                        .weight(0.92f)
                        .fillMaxHeight()
                )
                MicroTelemetryPill(
                    label = "STATUS",
                    value = if (state.safetyStatusLabel == "LIMIT EXCEEDED") "REDLINE" else state.safetyStatusLabel,
                    subValue = state.highestActiveTier.titleEn,
                    accentColor = state.safetyStatusTier.primaryColor,
                    modifier = Modifier
                        .weight(0.98f)
                        .fillMaxHeight()
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
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(Color.White.copy(alpha = 0.045f))
            .border(1.dp, accentColor.copy(alpha = 0.35f), shape)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = RobotoMonoFontFamily,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold
            ),
            color = Color(0xFF94A3B8),
            maxLines = 1
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge.copy(
                fontFamily = RobotoMonoFontFamily,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold
            ),
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = subValue,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = RobotoMonoFontFamily,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Medium
            ),
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
        cornerRadius = 20.dp,
        accentColor = accentColor,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        onClick = onCycleHeroMode
    ) {
        if (sessions.isEmpty() || activeSession == null) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Timeline,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Zero Active Pharmacokinetic Load",
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = "All plasma levels cleared to baseline • Tap below to log intake",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                    color = Color(0xFF94A3B8)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF06B6D4).copy(alpha = 0.22f))
                        .border(1.dp, Color(0xFF06B6D4), RoundedCornerShape(50))
                        .clickable(onClick = onQuickAddDose)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "+ Log First Dose",
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.5.sp),
                        color = Color(0xFF38BDF8)
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Top Strip: Horizontally Scrollable Active Substance Pill Switcher + Tier Badge
                val glass = LocalSubstanceGlass.current
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
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
                                        if (selected) glass.pillSurfaceActive
                                        else glass.pillSurfaceInactive
                                    )
                                    .border(
                                        width = if (selected) 1.4.dp else 1.dp,
                                        color = if (selected) glass.pillBorderActive else glass.pillBorderInactive,
                                        shape = pillShape
                                    )
                                    .clickable { onSelectTab(idx) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = SubstanceIcons.iconForName(sess.substance.iconName),
                                    contentDescription = null,
                                    tint = subColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = sess.substance.name,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontSize = 12.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (selected) glass.textPrimary else glass.textSecondary,
                                    maxLines = 1
                                )
                                if (sess.activeLogs.size > 1) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${sess.activeLogs.size}x",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = RobotoMonoFontFamily,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = subColor,
                                        maxLines = 1
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
                                        if (isAllStackedComposite) glass.pillSurfaceActive
                                        else glass.pillSurfaceInactive
                                    )
                                    .border(
                                        width = if (isAllStackedComposite) 1.4.dp else 1.dp,
                                        color = if (isAllStackedComposite) glass.pillBorderActive else glass.pillBorderInactive,
                                        shape = pillShape
                                    )
                                    .clickable { onSelectTab(sessions.size) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Layers,
                                    contentDescription = null,
                                    tint = glass.primaryAccent,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "Všechny aktivní",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = if (isAllStackedComposite) glass.textPrimary else glass.textSecondary,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    TierBadgePill(
                        tier = activeSession.currentTier,
                        compact = false
                    )
                }

                // Center Area: Full-Height Live Multi-Dose Superposition Curve OR Stacked Dose Breakdown Matrix
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
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
            initialValue = 0.75f,
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
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 16.dp)
        ) {
            val w = size.width
            val h = size.height
            if (w <= 10f || h <= 10f || points.size < 2) return@Canvas

            val maxScaleY = max(
                sub.thresholdStrong * 1.15,
                session.combinedPeakLoad * 1.12
            ).coerceAtLeast(1.0)

            // 1. Horizontal threshold zone bands (Light, Medium, Strong)
            val thresholds = listOf(
                sub.thresholdLight to DoseTier.LIGHT.primaryColor,
                sub.thresholdMedium to DoseTier.MEDIUM.primaryColor,
                sub.thresholdStrong to DoseTier.STRONG.primaryColor
            )
            thresholds.forEach { (thVal, thCol) ->
                val y = (h - (thVal / maxScaleY).toFloat() * h).coerceIn(0f, h)
                drawLine(
                    color = thCol.copy(alpha = 0.25f),
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1.1.dp.toPx(),
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
                color = Color.White.copy(alpha = 0.35f),
                start = Offset(nowX, 0f),
                end = Offset(nowX, h),
                strokeWidth = 1.4.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
            )

            // 3. Individual dose intake markers (dashed vertical lines for t_1, t_2...)
            val nowMs = points.firstOrNull { it.relativeMinutesFromNow >= 0f }?.timestampMillis ?: 0L
            session.doseTimestampsMillis.forEach { doseMs ->
                val relM = (doseMs - nowMs) / 60_000f
                if (relM in minRel..maxRel) {
                    val dx = xForRelMins(relM)
                    drawLine(
                        color = primaryColor.copy(alpha = 0.50f),
                        start = Offset(dx, h * 0.12f),
                        end = Offset(dx, h),
                        strokeWidth = 1.2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
                    )
                    drawCircle(
                        color = primaryColor,
                        radius = 3.8.dp.toPx(),
                        center = Offset(dx, h - 3.8.dp.toPx())
                    )
                }
            }

            if (isCompositeMode) {
                allSessions.forEach { s ->
                    val sColor = Color(s.substance.colorHex)
                    val sMax = max(s.substance.thresholdStrong * 1.12, s.combinedPeakLoad * 1.12).coerceAtLeast(1.0)
                    val path = Path()
                    s.curvePoints.forEachIndexed { idx, pt ->
                        val px = xForRelMins(pt.relativeMinutesFromNow)
                        val py = (h - (pt.activeEffectLoad / sMax).toFloat() * (h * 0.92f)).coerceIn(2f, h)
                        if (idx == 0) path.moveTo(px, py) else path.lineTo(px, py)
                    }
                    drawPath(
                        path = path,
                        color = sColor,
                        style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            } else {
                // 4. Residual Plasma Half-Life Curve
                val plasmaPath = Path()
                points.forEachIndexed { idx, pt ->
                    val px = xForRelMins(pt.relativeMinutesFromNow)
                    val py = (h - (pt.plasmaConcentration / maxScaleY).toFloat() * (h * 0.92f)).coerceIn(2f, h)
                    if (idx == 0) plasmaPath.moveTo(px, py) else plasmaPath.lineTo(px, py)
                }
                drawPath(
                    path = plasmaPath,
                    color = primaryColor.copy(alpha = 0.40f),
                    style = Stroke(
                        width = 1.6.dp.toPx(),
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f))
                    )
                )

                // 5. Combined Active Effect Superposition Curve + Area Gradient Fill
                val effectPath = Path()
                val fillPath = Path()
                points.forEachIndexed { idx, pt ->
                    val px = xForRelMins(pt.relativeMinutesFromNow)
                    val py = (h - (pt.activeEffectLoad / maxScaleY).toFloat() * (h * 0.92f)).coerceIn(2f, h)
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
                            primaryColor.copy(alpha = 0.42f),
                            session.currentTier.primaryColor.copy(alpha = 0.16f),
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
                            primaryColor.copy(alpha = 0.75f)
                        )
                    ),
                    style = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round)
                )

                // 6. Highlighted Combined Shifted Peak Marker
                val peakRelMins = (session.combinedPeakTimestampMillis - nowMs) / 60_000f
                if (peakRelMins in minRel..maxRel) {
                    val peakX = xForRelMins(peakRelMins)
                    val peakY = (h - (session.combinedPeakLoad / maxScaleY).toFloat() * (h * 0.92f)).coerceIn(4f, h - 4f)
                    drawCircle(
                        color = session.combinedPeakTier.primaryColor.copy(alpha = 0.38f),
                        radius = 8.dp.toPx(),
                        center = Offset(peakX, peakY)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 3.8.dp.toPx(),
                        center = Offset(peakX, peakY)
                    )
                }

                // 7. Glowing Pulsing Orb at exact NOW position on the curve
                val nowY = (h - (session.currentActiveLoad / maxScaleY).toFloat() * (h * 0.92f)).coerceIn(4f, h - 4f)
                drawCircle(
                    color = primaryColor.copy(alpha = 0.40f),
                    radius = 10.5.dp.toPx() * orbPulse,
                    center = Offset(nowX, nowY)
                )
                drawCircle(
                    color = primaryColor,
                    radius = 5.2.dp.toPx(),
                    center = Offset(nowX, nowY)
                )
                drawCircle(
                    color = Color.White,
                    radius = 2.4.dp.toPx(),
                    center = Offset(nowX, nowY)
                )
            }
        }

        // Overlay Legend Badges (Top-Right inside Graph)
        val timeFmt = SimpleDateFormat("HH:mm", Locale.US)
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 2.dp, end = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (session.activeLogs.size > 1 && session.peakShiftMinutes > 0) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF59E0B).copy(alpha = 0.18f))
                        .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.55f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 3.5.dp)
                ) {
                    Text(
                        text = "STACKED PEAK SHIFT +${session.peakShiftMinutes}m (${timeFmt.format(Date(session.combinedPeakTimestampMillis))})",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = RobotoMonoFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(0xFFFBBF24)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .border(0.8.dp, glass.cardBorderBase, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 3.5.dp)
                ) {
                    Text(
                        text = "Peak ${session.substance.defaultUnit}: ${String.format(Locale.US, "%.1f", session.combinedPeakLoad)} @ ${timeFmt.format(Date(session.combinedPeakTimestampMillis))}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = RobotoMonoFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = Color(0xFFE2E8F0)
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
            Text("-2h", style = MaterialTheme.typography.labelSmall.copy(fontFamily = RobotoMonoFontFamily, fontSize = 9.5.sp), color = Color(0xFF94A3B8))
            Text("NOW", style = MaterialTheme.typography.labelSmall.copy(fontFamily = RobotoMonoFontFamily, fontSize = 10.sp, fontWeight = FontWeight.Bold), color = primaryColor)
            Text("+2h", style = MaterialTheme.typography.labelSmall.copy(fontFamily = RobotoMonoFontFamily, fontSize = 9.5.sp), color = Color(0xFF94A3B8))
            Text("+5h", style = MaterialTheme.typography.labelSmall.copy(fontFamily = RobotoMonoFontFamily, fontSize = 9.5.sp), color = Color(0xFF94A3B8))
            Text("+8h", style = MaterialTheme.typography.labelSmall.copy(fontFamily = RobotoMonoFontFamily, fontSize = 9.5.sp), color = Color(0xFF94A3B8))
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
            .background(Color.White.copy(alpha = 0.035f))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SUPERPOSITION DOSE STACK (${session.activeLogs.size} OVERLAPPING)",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.5.sp,
                    color = Color(sub.colorHex),
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = "Tap card for PK Curve",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                color = Color(0xFF94A3B8)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            session.activeLogs.takeLast(3).forEachIndexed { i, log ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.045f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Dose #${i + 1} • ${timeFmt.format(Date(log.timestampMillis))} (${log.roaName})",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Medium),
                        color = Color.White
                    )
                    Text(
                        text = "${settings.formatDose(log.doseAmount, log.unit)} (${settings.formatCurrency(log.cost)})",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = RobotoMonoFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        ),
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
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = Color(0xFF94A3B8)
            )
            Text(
                text = "t½ = ${sub.halfLifeHours}h",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = RobotoMonoFontFamily,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold
                ),
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
    val countdownMins = (session.phaseCountdownSeconds / 60).coerceAtLeast(1)

    val (phaseBadgeText, phaseBadgeColor, progressFrac) = when (session.currentPhase) {
        PkPhase.ONSET_COUNTDOWN -> Triple("Nástup (Onset)", Color(0xFFF59E0B), 0.18f)
        PkPhase.CLIMBING_TO_PEAK -> Triple("Náběh (Comeup)", Color(0xFFF59E0B), 0.42f)
        PkPhase.PEAKING_NOW -> Triple("Vrchol (Peak)", Color(0xFF10B981), 0.65f)
        PkPhase.OFFSET_COMEDOWN -> Triple("Doznívání (Offset)", Color(0xFF38BDF8), 0.82f)
        PkPhase.RESIDUAL_HALF_LIFE -> Triple("V těle (t½)", Color(0xFF8B5CF6), 0.94f)
        PkPhase.INACTIVE -> Triple("Čistý stav", Color(0xFF6B7280), 1.0f)
    }

    val rightStatus = "${String.format(Locale.US, "%.1f", session.currentActiveLoad)} ${sub.defaultUnit} • Konec za ${countdownMins}m (${timeFmt.format(Date(session.activeEffectsEndTimestampMillis))})"

    LatkyPhaseProgressStrip(
        phaseBadgeText = phaseBadgeText,
        phaseColor = phaseBadgeColor,
        rightStatusText = rightStatus,
        progressFraction = progressFrac
    )
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
        contentPadding = PaddingValues(10.dp),
        onClick = onCyclePage
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            BentoMicroHeader(
                title = "TIME SINCE LAST",
                icon = Icons.Default.History,
                accentColor = Color(0xFF38BDF8),
                trailingText = if (totalPages > 1) "${pageIdx + 1}/$totalPages" else null
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                visibleItems.forEach { item ->
                    val subColor = Color(item.substance.colorHex)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.035f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(0.55f)
                        ) {
                            SquircleIconBadge(
                                icon = SubstanceIcons.iconForName(item.substance.iconName),
                                colorHex = item.substance.colorHex,
                                size = 28.dp,
                                iconSize = 15.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(verticalArrangement = Arrangement.Center) {
                                Text(
                                    text = item.substance.name,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = state.settings.formatDose(item.lastDoseAmount, item.substance.defaultUnit),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = RobotoMonoFontFamily,
                                        fontSize = 10.sp
                                    ),
                                    color = Color(0xFF94A3B8),
                                    maxLines = 1
                                )
                            }
                        }

                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.weight(0.45f)
                        ) {
                            Text(
                                text = formatElapsedCompact(item.elapsedSeconds),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontFamily = RobotoMonoFontFamily,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = subColor
                                ),
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            // Recovery progress bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.90f)
                                    .height(5.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.09f))
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
        contentPadding = PaddingValues(10.dp),
        onClick = onFlipCard
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
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
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                        color = Color(0xFF64748B)
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    items.forEach { item ->
                        val subColor = Color(item.substance.colorHex)
                        val barColor = if (item.limitFraction > 1.0f) Color(0xFFEF4444) else subColor
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.035f))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.substance.name,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 12.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(0.48f)
                                )
                                Text(
                                    text = if (state.showCostInsteadOfUnits) {
                                        "${state.settings.formatCurrency(item.todayTotalCost)} (${item.doseCountToday}x)"
                                    } else {
                                        "${state.settings.formatDose(item.todayTotalUnits)}/${state.settings.formatDose(item.dailySafeLimit, item.substance.defaultUnit)}"
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = RobotoMonoFontFamily,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = barColor
                                    ),
                                    maxLines = 1
                                )
                            }
                            Spacer(modifier = Modifier.height(5.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.09f))
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
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            // Top Row: 24-Hour Circadian Intake Timeline Header + Sleep Readiness Indicator
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
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "24H CIRCADIAN TIMELINE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = Color(0xFFCBD5E1)
                    )
                }

                // Sleep Readiness Indicator at configured bedtime
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (state.sleepReadinessOptimal) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (state.sleepReadinessOptimal) Color(0xFF10B981) else Color(0xFFF59E0B),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (state.sleepReadinessOptimal) {
                            "Sleep @ ${state.settings.bedtimeHour24}:00 Clear"
                        } else {
                            "${state.sleepWarningSubstanceName} active @ ${state.settings.bedtimeHour24}:00"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = RobotoMonoFontFamily,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = if (state.sleepReadinessOptimal) Color(0xFF10B981) else Color(0xFFFBBF24)
                    )
                }
            }

            // Middle: Full-Height 24-Hour Timeline Bar with glowing dose gems, bedtime marker & hour labels
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(18.dp)
                ) {
                    val w = size.width
                    val h = size.height
                    val midY = h / 2f

                    // Track bar
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.09f),
                        topLeft = Offset(0f, midY - 3.5.dp.toPx()),
                        size = Size(w, 7.dp.toPx()),
                        cornerRadius = CornerRadius(4.dp.toPx())
                    )

                    // Hour ticks (0, 6, 12, 18, 24)
                    listOf(0f, 0.25f, 0.5f, 0.75f, 1f).forEach { frac ->
                        drawCircle(
                            color = Color.White.copy(alpha = 0.28f),
                            radius = 2.dp.toPx(),
                            center = Offset(w * frac, midY)
                        )
                    }

                    // Bedtime marker
                    val bedX = (state.settings.bedtimeHour24 / 24f).coerceIn(0f, 1f) * w
                    drawLine(
                        color = Color(0xFF8B5CF6),
                        start = Offset(bedX, 0f),
                        end = Offset(bedX, h),
                        strokeWidth = 2.dp.toPx()
                    )

                    // Logged Gems
                    state.todayTimelineGems.forEach { gem ->
                        val gx = (gem.hourOfDayFraction * w).coerceIn(5f, w - 5f)
                        val gColor = Color(gem.substance.colorHex)
                        drawCircle(
                            color = gColor.copy(alpha = 0.38f),
                            radius = 7.5.dp.toPx(),
                            center = Offset(gx, midY)
                        )
                        drawCircle(
                            color = gColor,
                            radius = 4.5.dp.toPx(),
                            center = Offset(gx, midY)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("00h", "06h", "12h", "18h", "24h").forEach { hr ->
                        Text(
                            text = hr,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = RobotoMonoFontFamily,
                                fontSize = 8.5.sp
                            ),
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }

            // Bottom: Tall 1-Tap Quick-Log Dock (or Undo confirmation strip)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (state.lastQuickLoggedLabel != null) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF10B981).copy(alpha = 0.20f))
                            .border(1.dp, Color(0xFF10B981).copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                            .clickable(onClick = onUndoQuickLog)
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Logged ${state.lastQuickLoggedLabel} • PK Stack Updated",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = Color.White,
                            maxLines = 1
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Undo,
                                contentDescription = "Undo quick log",
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "UNDO",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color(0xFF10B981)
                            )
                        }
                    }
                } else {
                    state.quickPresetDockItems.forEach { preset ->
                        val pColor = Color(preset.substance.colorHex)
                        val shape = RoundedCornerShape(12.dp)
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(shape)
                                .background(pColor.copy(alpha = 0.16f))
                                .border(1.dp, pColor.copy(alpha = 0.55f), shape)
                                .clickable { onQuickLogPreset(preset) }
                                .padding(horizontal = 8.dp)
                                .testTag("quick_log_${preset.substance.name.lowercase()}"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = pColor,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = preset.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
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
                            .fillMaxHeight()
                            .width(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.10f))
                            .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                            .clickable(onClick = onOpenCustomLogger)
                            .testTag("dashboard_open_custom_logger"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Open Dose Studio",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
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
