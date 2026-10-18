package com.antigravity.substance.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.substance.domain.engine.SummaryStats
import com.antigravity.substance.domain.model.DoseTier
import java.util.Locale

/**
 * Professional Analytical Bento Card:
 * - In Lightweight Mode or Light Mode: Renders a crisp, solid card surface with a clean 1.dp border.
 * - In Standard Dark Mode: Renders subtle surface depth with a clean accent border.
 */
@Composable
fun FrostedBentoCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    accentColor: Color? = null,
    contentPadding: PaddingValues = PaddingValues(10.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val glassEnv = LocalSubstanceGlass.current
    val shape = RoundedCornerShape(cornerRadius)

    val cardMod = if (glassEnv.isLightweightMode || glassEnv.isLightMode) {
        val borderCol = accentColor?.copy(alpha = if (glassEnv.isLightMode) 0.45f else 0.38f)
            ?: glassEnv.cardBorderBase
        modifier
            .clip(shape)
            .background(glassEnv.cardSurfaceBase)
            .border(width = 1.dp, color = borderCol, shape = shape)
            .let { m -> if (onClick != null) m.clickable(onClick = onClick) else m }
            .padding(contentPadding)
    } else {
        val topSurface = glassEnv.cardSurfaceBase.copy(alpha = glassEnv.glassOpacity)
        val bottomSurface = glassEnv.cardSurfaceBase.copy(alpha = (glassEnv.glassOpacity + 0.06f).coerceAtMost(0.98f))
        val topBorderColor = accentColor?.copy(alpha = (glassEnv.specularIntensity + 0.20f).coerceAtMost(0.65f))
            ?: glassEnv.cardBorderBase
        val bottomBorderColor = accentColor?.copy(alpha = 0.12f)
            ?: glassEnv.cardBorderBase.copy(alpha = 0.55f)

        modifier
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(
                    colors = if (accentColor != null) {
                        listOf(
                            accentColor.copy(alpha = 0.07f),
                            topSurface,
                            bottomSurface
                        )
                    } else {
                        listOf(topSurface, bottomSurface)
                    }
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(colors = listOf(topBorderColor, bottomBorderColor)),
                shape = shape
            )
            .let { m -> if (onClick != null) m.clickable(onClick = onClick) else m }
            .padding(contentPadding)
    }

    Box(modifier = cardMod, content = content)
}

@Composable
fun TierBadgePill(
    tier: DoseTier,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val color = if (tier == DoseTier.INACTIVE) Color(0xFF64748B) else tier.primaryColor
    val shape = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .clip(shape)
            .background(color.copy(alpha = 0.16f))
            .border(1.dp, color.copy(alpha = 0.50f), shape)
            .padding(
                horizontal = if (compact) 6.dp else 8.dp,
                vertical = if (compact) 2.dp else 3.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(if (compact) 5.dp else 6.dp)
                .clip(shape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = tier.titleCz.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = if (compact) 9.sp else 10.sp,
                fontWeight = FontWeight.Bold
            ),
            color = color,
            maxLines = 1
        )
    }
}

@Composable
fun SquircleIconBadge(
    icon: ImageVector,
    colorHex: Long,
    modifier: Modifier = Modifier,
    size: Dp = 30.dp,
    iconSize: Dp = 16.dp
) {
    val glassEnv = LocalSubstanceGlass.current
    val palette = generateSubstanceTonalPalette(colorHex, glassEnv.isLightMode)
    val shape = RoundedCornerShape(size * 0.28f)
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(palette.container)
            .border(1.dp, palette.borderAccent, shape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = palette.primary,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
fun BentoMicroHeader(
    title: String,
    icon: ImageVector? = null,
    accentColor: Color = Color(0xFF0284C7),
    trailingText: String? = null
) {
    val glassEnv = LocalSubstanceGlass.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
            }
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    letterSpacing = 0.5.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = glassEnv.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (trailingText != null) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = trailingText,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    color = accentColor,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1
            )
        }
    }
}

/**
 * Statistical Summary Strip displayed above detailed charts showing MIN, AVG, MED, P90, MAX, and SD.
 */
@Composable
fun ChartStatsSummaryStrip(
    stats: SummaryStats,
    unit: String = "",
    decimals: Int = 1,
    accentColor: Color = Color(0xFF06B6D4),
    modifier: Modifier = Modifier
) {
    val glassEnv = LocalSubstanceGlass.current
    fun fmt(v: Double): String {
        val s = if (decimals == 0 || v % 1.0 == 0.0) {
            String.format(Locale.US, "%.0f", v)
        } else {
            String.format(Locale.US, "%.${decimals}f", v)
        }
        return if (unit.isBlank()) s else "$s $unit"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (glassEnv.isLightMode) Color(0xFFF1F5F9) else Color(0xFF0F172A).copy(alpha = 0.65f)
            )
            .border(
                1.dp,
                if (glassEnv.isLightMode) Color(0xFFE2E8F0) else Color(0xFF1E293B),
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatMiniMetric("MIN", fmt(stats.min), Color(0xFF10B981))
        StatMiniMetric("PRŮMĚR", fmt(stats.mean), accentColor)
        StatMiniMetric("MEDIÁN", fmt(stats.median), glassEnv.textPrimary)
        StatMiniMetric("P90", fmt(stats.p90), Color(0xFFF59E0B))
        StatMiniMetric("MAX", fmt(stats.max), Color(0xFFEF4444))
    }
}

@Composable
private fun StatMiniMetric(
    label: String,
    value: String,
    valueColor: Color
) {
    val glassEnv = LocalSubstanceGlass.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
            color = glassEnv.textMuted
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold
            ),
            color = valueColor,
            maxLines = 1
        )
    }
}
