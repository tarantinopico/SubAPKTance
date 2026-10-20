package com.antigravity.substance.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.antigravity.substance.domain.model.DoseTier

/**
 * Background container that defaults to a pure Matte OLED pitch-black (#000000) canvas
 * matching the reference screenshots, while still supporting optional ambient radial glow
 * if the user increases ambientGlowIntensity in Settings.
 */
@Composable
fun DynamicAmbientGlowBox(
    activeTier: DoseTier,
    modifier: Modifier = Modifier,
    overrideAccentColor: Color? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val glassEnv = LocalSubstanceGlass.current

    // In default Matte OLED mode (ambientGlowIntensity <= 0.02f) or Lightweight mode,
    // render a clean, distraction-free solid canvas background (#000000 in OLED dark mode).
    if (glassEnv.isLightweightMode || glassEnv.ambientGlowIntensity <= 0.02f) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(glassEnv.canvasBackgroundTop),
            content = content
        )
        return
    }

    val targetPrimary = overrideAccentColor ?: activeTier.primaryColor
    val targetSecondary = Color(activeTier.secondaryColorHex)

    val animatedPrimary by animateColorAsState(
        targetValue = targetPrimary,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "ambientPrimaryColor"
    )
    val animatedSecondary by animateColorAsState(
        targetValue = targetSecondary,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "ambientSecondaryColor"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "ambientPulse")
    val pulseFactor by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseFactor"
    )

    val glowAlphaMultiplier = if (glassEnv.isLightMode) {
        (glassEnv.ambientGlowIntensity * 0.25f).coerceIn(0f, 0.22f)
    } else {
        (glassEnv.ambientGlowIntensity * 0.55f).coerceIn(0f, 0.55f)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                val w = size.width
                val h = size.height

                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(glassEnv.canvasBackgroundTop, glassEnv.canvasBackgroundBottom)
                    )
                )

                if (glowAlphaMultiplier > 0.01f && w > 0f && h > 0f) {
                    val primaryAlpha = (0.24f * glowAlphaMultiplier * pulseFactor).coerceIn(0f, 0.45f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                animatedPrimary.copy(alpha = primaryAlpha),
                                animatedPrimary.copy(alpha = primaryAlpha * 0.25f),
                                Color.Transparent
                            ),
                            center = Offset(w * 0.25f, h * 0.14f),
                            radius = w * 0.85f * pulseFactor
                        ),
                        center = Offset(w * 0.25f, h * 0.14f),
                        radius = w * 0.85f * pulseFactor
                    )

                    val secondaryAlpha = (0.16f * glowAlphaMultiplier).coerceIn(0f, 0.35f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                animatedSecondary.copy(alpha = secondaryAlpha),
                                Color.Transparent
                            ),
                            center = Offset(w * 0.82f, h * 0.72f),
                            radius = w * 0.75f
                        ),
                        center = Offset(w * 0.82f, h * 0.72f),
                        radius = w * 0.75f
                    )
                }
            },
        content = content
    )
}
