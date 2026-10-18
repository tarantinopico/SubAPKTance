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
 * Full-viewport background container that renders the clean professional surface.
 * When Lightweight Mode is enabled (or ambientGlowIntensity == 0), skips all infinite
 * animations and radial gradients for zero GPU overhead.
 */
@Composable
fun DynamicAmbientGlowBox(
    activeTier: DoseTier,
    modifier: Modifier = Modifier,
    overrideAccentColor: Color? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val glassEnv = LocalSubstanceGlass.current

    if (glassEnv.isLightweightMode || glassEnv.ambientGlowIntensity <= 0.01f) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(glassEnv.canvasBackgroundTop),
            content = content
        )
        return
    }

    val targetPrimaryColor = overrideAccentColor ?: activeTier.primaryColor
    val targetSecondaryColor = activeTier.secondaryColor

    val animatedPrimary by animateColorAsState(
        targetValue = targetPrimaryColor,
        animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing),
        label = "ambientPrimaryColor"
    )
    val animatedSecondary by animateColorAsState(
        targetValue = targetSecondaryColor,
        animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing),
        label = "ambientSecondaryColor"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "ambientPulse")
    val pulseFraction by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = activeTier.pulseDurationMs.coerceIn(1100, 4500),
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseRadius"
    )

    val modeMultiplier = if (glassEnv.isLightMode) 0.32f else 0.72f
    val effectiveAlpha = (activeTier.ambientAlpha * glassEnv.ambientGlowIntensity * modeMultiplier).coerceIn(0f, 0.65f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        glassEnv.canvasBackgroundTop,
                        glassEnv.canvasBackgroundBottom
                    )
                )
            )
            .drawBehind {
                if (effectiveAlpha > 0.01f && size.width > 0f && size.height > 0f) {
                    val maxDim = size.maxDimension.coerceAtLeast(100f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                animatedPrimary.copy(alpha = effectiveAlpha * 0.45f),
                                animatedSecondary.copy(alpha = effectiveAlpha * 0.16f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.5f, size.height * 0.25f),
                            radius = (maxDim * 0.52f * pulseFraction).coerceAtLeast(10f)
                        ),
                        center = Offset(size.width * 0.5f, size.height * 0.25f),
                        radius = (maxDim * 0.52f * pulseFraction).coerceAtLeast(10f)
                    )
                }
            },
        content = content
    )
}
