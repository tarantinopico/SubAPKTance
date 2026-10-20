package com.antigravity.substance.ui.theme

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
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
 * Matte OLED & customizable card container matching the reference screenshots:
 * - Deep matte charcoal surface (#0F1117 in OLED mode, customizable opacity)
 * - Clean 1dp dark hairline border (#1E222D, or subtly tinted when specularIntensity > 0.18f)
 * - Configurable corner radius from UserSettings (default 20.dp)
 */
@Composable
fun FrostedBentoCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = Dp.Unspecified,
    accentColor: Color? = null,
    contentPadding: PaddingValues = PaddingValues(14.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val glass = LocalSubstanceGlass.current
    val resolvedRadius = if (cornerRadius != Dp.Unspecified) cornerRadius else glass.cardCornerRadiusDp.dp
    val shape = RoundedCornerShape(resolvedRadius)

    val clickMod = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }

    val usePureMatte = glass.isLightweightMode || glass.ambientGlowIntensity <= 0.04f

    if (usePureMatte) {
        val surfaceColor = if (glass.isLightMode) {
            glass.cardSurfaceBase
        } else {
            glass.cardSurfaceBase.copy(alpha = glass.glassOpacity.coerceIn(0.75f, 1f))
        }
        val borderColor = if (accentColor != null && glass.specularIntensity > 0.16f) {
            lerp(glass.cardBorderBase, accentColor, 0.35f)
        } else {
            glass.cardBorderBase
        }
        Box(
            modifier = modifier
                .clip(shape)
                .background(color = surfaceColor, shape = shape)
                .border(width = 1.dp, color = borderColor, shape = shape)
                .then(clickMod)
                .padding(contentPadding),
            content = content
        )
        return
    }

    // Optional tinted/glass mode when user increases Ambient Glow / Specular sliders in Settings
    val surfaceTop = if (accentColor != null) {
        lerp(glass.cardSurfaceBase, accentColor, if (glass.isLightMode) 0.05f else 0.08f)
            .copy(alpha = glass.glassOpacity)
    } else {
        glass.cardSurfaceBase.copy(alpha = glass.glassOpacity)
    }
    val surfaceBottom = glass.cardSurfaceBase.copy(alpha = (glass.glassOpacity + 0.03f).coerceAtMost(1f))

    val borderTop = accentColor?.copy(alpha = (glass.specularIntensity * 1.4f).coerceIn(0.12f, 0.65f))
        ?: glass.cardBorderBase

    Box(
        modifier = modifier
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(colors = listOf(surfaceTop, surfaceBottom)),
                shape = shape
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(colors = listOf(borderTop, glass.cardBorderBase)),
                shape = shape
            )
            .then(clickMod)
            .padding(contentPadding),
        content = content
    )
}

/**
 * Top branding & status header matching the reference screenshots:
 * Left: Purple-pink gradient squircle "L" badge + "Látky" title + "OFFLINE" dark monospace badge
 * Right: Dark red outlined safety score badge (or custom trailing content)
 */
@Composable
fun LatkyTopBrandHeader(
    safetyScore: Int = 100,
    trailingContent: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val glass = LocalSubstanceGlass.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Gradient purple-pink squircle "L" icon
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF6366F1), Color(0xFFD946EF))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "L",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    ),
                    color = Color.White
                )
            }
            Text(
                text = "Látky",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                ),
                color = glass.textPrimary
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(glass.badgeSurface)
                    .border(0.8.dp, glass.cardBorderBase, RoundedCornerShape(6.dp))
                    .padding(horizontal = 7.dp, vertical = 2.5.dp)
            ) {
                Text(
                    text = "OFFLINE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = RobotoMonoFontFamily,
                        fontSize = 9.sp,
                        letterSpacing = 0.6.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = glass.textSecondary
                )
            }
        }

        if (trailingContent != null) {
            trailingContent()
        } else {
            // Safety / Status Pill matching the top-right red/amber pill in screenshots
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFF281117))
                    .border(1.dp, Color(0xFF6E202B), RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Shield,
                    contentDescription = null,
                    tint = Color(0xFFF87171),
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = "$safetyScore",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = RobotoMonoFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color(0xFFFECACA)
                )
            }
        }
    }
}

/**
 * Signature pill chip matching the reference screenshots:
 * - Inactive: Matte #0F1117 fill with #232734 border and secondary gray text
 * - Active: Tinted dark indigo (#191836) fill with crisp #5B4DFF (or custom accent) 1.2dp border and bold white text
 */
@Composable
fun LatkyStylePillChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    iconTint: Color? = null,
    activeBorderColor: Color? = null
) {
    val glass = LocalSubstanceGlass.current
    val resolvedActiveBorder = activeBorderColor ?: glass.primaryAccent
    val pillShape = RoundedCornerShape(50)

    val bgColor = if (selected) {
        if (activeBorderColor != null) {
            lerp(glass.cardSurfaceBase, activeBorderColor, if (glass.isLightMode) 0.14f else 0.20f)
        } else {
            glass.pillSurfaceActive
        }
    } else {
        glass.pillSurfaceInactive
    }

    val borderColor = if (selected) resolvedActiveBorder else glass.pillBorderInactive

    Row(
        modifier = modifier
            .clip(pillShape)
            .background(bgColor)
            .border(
                width = if (selected) 1.4.dp else 1.dp,
                color = borderColor,
                shape = pillShape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = iconTint ?: if (selected) resolvedActiveBorder else glass.textSecondary,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            ),
            color = if (selected) glass.textPrimary else glass.textSecondary,
            maxLines = 1,
            softWrap = false
        )
    }
}

/**
 * Signature 4-phase horizontal progress bar ("Užití — Nástup — Vrchol — Konec")
 * with an oval phase badge ("Doznívání (Offset)" / "Nástup (Onset)") and countdown on the right,
 * matching the reference screenshots.
 */
@Composable
fun LatkyPhaseProgressStrip(
    phaseBadgeText: String,
    phaseColor: Color,
    rightStatusText: String,
    progressFraction: Float,
    modifier: Modifier = Modifier
) {
    val glass = LocalSubstanceGlass.current
    val clamped = progressFraction.coerceIn(0.03f, 1f)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Oval Phase Pill (e.g. "Doznívání (Offset)" or "Nástup (Onset)")
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(phaseColor.copy(alpha = 0.16f))
                    .border(1.1.dp, phaseColor.copy(alpha = 0.65f), RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = phaseBadgeText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = phaseColor
                )
            }

            Text(
                text = rightStatusText,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = RobotoMonoFontFamily,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = glass.textPrimary.copy(alpha = 0.90f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // 4-Segment Progress Bar with smooth gradient fill
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
        ) {
            val w = size.width
            val h = size.height
            val gap = 3.5.dp.toPx()
            val segW = (w - gap * 3f) / 4f

            // Draw 4 dark background segments
            for (i in 0 until 4) {
                val sx = i * (segW + gap)
                drawRoundRect(
                    color = glass.badgeSurface,
                    topLeft = Offset(sx, 0f),
                    size = Size(segW, h),
                    cornerRadius = CornerRadius(h / 2f, h / 2f)
                )
            }

            // Draw continuous gradient active progress bar
            val fillW = (w * clamped).coerceIn(h, w)
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFFF59E0B),
                        Color(0xFF10B981),
                        Color(0xFF38BDF8)
                    )
                ),
                topLeft = Offset(0f, 0f),
                size = Size(fillW, h),
                cornerRadius = CornerRadius(h / 2f, h / 2f)
            )
        }

        // 4 Phase Labels: Užití | Nástup | Vrchol | Konec
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("Užití", "Nástup", "Vrchol", "Konec").forEach { lbl ->
                Text(
                    text = lbl,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = RobotoMonoFontFamily,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = glass.textSecondary
                )
            }
        }
    }
}

/**
 * Compact statistical summary strip rendered inside Analytics cards:
 * Displays MIN | AVG (Průměr) | MED (Medián) | P90 | MAX in Roboto Mono.
 */
@Composable
fun ChartStatsSummaryStrip(
    stats: SummaryStats,
    unit: String = "",
    accentColor: Color = Color(0xFF5B4DFF),
    decimals: Int = 1,
    showSum: Boolean = false,
    modifier: Modifier = Modifier
) {
    val glass = LocalSubstanceGlass.current
    val uSuffix = if (unit.isBlank()) "" else " $unit"

    fun fmt(v: Double): String {
        return if (decimals <= 0 || v % 1.0 == 0.0) "${v.toInt()}$uSuffix"
        else String.format(Locale.US, "%.${decimals.coerceIn(1, 2)}f%s", v, uSuffix)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(glass.badgeSurface.copy(alpha = 0.72f))
            .border(0.8.dp, glass.cardBorderBase, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 6.5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatMiniCell("MIN", fmt(stats.min), glass.textSecondary)
        StatMiniCell("PRŮMĚR", fmt(stats.mean), accentColor, isBold = true)
        StatMiniCell("MEDIÁN", fmt(stats.median), glass.textPrimary)
        StatMiniCell("P90", fmt(stats.p90), Color(0xFFF59E0B))
        StatMiniCell("MAX", fmt(stats.max), Color(0xFFEF4444), isBold = true)
        if (showSum) {
            StatMiniCell("CELKEM", fmt(stats.sum), Color(0xFF10B981))
        }
    }
}

@Composable
private fun StatMiniCell(
    label: String,
    value: String,
    valueColor: Color,
    isBold: Boolean = false
) {
    val glass = LocalSubstanceGlass.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = RobotoMonoFontFamily,
                fontSize = 8.5.sp,
                letterSpacing = 0.4.sp
            ),
            color = glass.textMuted,
            maxLines = 1
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge.copy(
                fontFamily = RobotoMonoFontFamily,
                fontSize = 11.sp,
                fontWeight = if (isBold) FontWeight.Bold else FontWeight.SemiBold
            ),
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SquircleIconBadge(
    icon: ImageVector,
    colorHex: Long,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    iconSize: Dp = 18.dp
) {
    val glass = LocalSubstanceGlass.current
    val palette = generateSubstanceTonalPalette(colorHex, glass.isLightMode)
    val shape = RoundedCornerShape(size * 0.30f)
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(palette.container)
            .border(1.dp, palette.borderAccent.copy(alpha = 0.45f), shape),
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
fun TierBadgePill(
    tier: DoseTier,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier = modifier
            .clip(shape)
            .background(tier.primaryColor.copy(alpha = 0.16f))
            .border(1.dp, tier.primaryColor.copy(alpha = 0.60f), shape)
            .padding(
                horizontal = if (compact) 8.dp else 11.dp,
                vertical = if (compact) 3.dp else 4.5.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = tier.titleEn.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = RobotoMonoFontFamily,
                fontSize = if (compact) 9.sp else 10.sp,
                fontWeight = FontWeight.Bold,
                color = tier.primaryColor
            ),
            maxLines = 1,
            softWrap = false
        )
    }
}

/**
 * Card section header redesigned to match the reference screenshots:
 * Left: Uppercase title (e.g. "ČASOVÁ OSA: KÁVA / KOFEIN") + dark rounded monospace badge ("Dávky v čase")
 * Right: Outlined info icon (i)
 */
@Composable
fun BentoMicroHeader(
    title: String,
    icon: ImageVector? = null,
    accentColor: Color = Color(0xFF5B4DFF),
    trailingText: String? = null
) {
    val glass = LocalSubstanceGlass.current

    // Strip any leading numeric index like "1. " or "12. " for a cleaner header like the screenshots
    val cleanTitleRaw = title.replace(Regex("^\\d+\\.\\s*"), "")

    // Extract parenthetical or bulleted subtitle into the dark monospace pill if trailingText is not given
    val (mainTitle, extractedBadge) = if (trailingText != null) {
        cleanTitleRaw to trailingText
    } else if (cleanTitleRaw.contains("(") && cleanTitleRaw.endsWith(")")) {
        val idx = cleanTitleRaw.indexOf('(')
        cleanTitleRaw.substring(0, idx).trim() to cleanTitleRaw.substring(idx + 1, cleanTitleRaw.length - 1).trim()
    } else {
        cleanTitleRaw to null
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = mainTitle.uppercase(),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 12.sp,
                    letterSpacing = 0.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = glass.textPrimary.copy(alpha = 0.95f)
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )

            if (!extractedBadge.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(glass.badgeSurface)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = extractedBadge,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = RobotoMonoFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = glass.textSecondary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(6.dp))
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = "Info",
            tint = glass.textSecondary.copy(alpha = 0.75f),
            modifier = Modifier.size(16.dp)
        )
    }
}
