package com.antigravity.substance.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterVintage
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flare
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.VapingRooms
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.antigravity.substance.data.preferences.UserSettings
import com.example.R

val SpaceGroteskFontFamily = FontFamily(
    Font(R.font.space_grotesk, FontWeight.Normal),
    Font(R.font.space_grotesk, FontWeight.Medium),
    Font(R.font.space_grotesk, FontWeight.SemiBold),
    Font(R.font.space_grotesk, FontWeight.Bold)
)

val RobotoMonoFontFamily = FontFamily(
    Font(R.font.roboto_mono, FontWeight.Normal),
    Font(R.font.roboto_mono, FontWeight.Medium),
    Font(R.font.roboto_mono, FontWeight.Bold)
)

@Immutable
data class SubstanceTonalPalette(
    val primary: Color,
    val container: Color,
    val onContainer: Color,
    val glowAura: Color,
    val borderAccent: Color
)

fun generateSubstanceTonalPalette(colorHex: Long, isLightMode: Boolean = false): SubstanceTonalPalette {
    val base = Color(colorHex)
    return if (isLightMode) {
        SubstanceTonalPalette(
            primary = lerp(base, Color(0xFF0F172A), 0.15f),
            container = base.copy(alpha = 0.14f),
            onContainer = lerp(base, Color(0xFF0F172A), 0.55f),
            glowAura = base.copy(alpha = 0.20f),
            borderAccent = base.copy(alpha = 0.40f)
        )
    } else {
        SubstanceTonalPalette(
            primary = base,
            container = base.copy(alpha = 0.16f),
            onContainer = lerp(base, Color.White, 0.55f),
            glowAura = base.copy(alpha = 0.35f),
            borderAccent = lerp(base, Color.White, 0.20f).copy(alpha = 0.42f)
        )
    }
}

@Immutable
data class SubstanceGlassEnvironment(
    val isLightMode: Boolean = false,
    val isLightweightMode: Boolean = false,
    val showChartMinMaxAvgLines: Boolean = true,
    val canvasBackgroundTop: Color,
    val canvasBackgroundBottom: Color,
    val cardSurfaceBase: Color,
    val cardBorderBase: Color,
    val glassOpacity: Float,
    val specularIntensity: Float,
    val ambientGlowIntensity: Float,
    val textPrimary: Color = Color(0xFFF8FAFC),
    val textSecondary: Color = Color(0xFF94A3B8),
    val textMuted: Color = Color(0xFF64748B)
)

val LocalSubstanceGlass = staticCompositionLocalOf {
    SubstanceGlassEnvironment(
        isLightMode = false,
        isLightweightMode = false,
        showChartMinMaxAvgLines = true,
        canvasBackgroundTop = Color(0xFF0B0F17),
        canvasBackgroundBottom = Color(0xFF0F172A),
        cardSurfaceBase = Color(0xFF131B2E),
        cardBorderBase = Color(0xFF23304A),
        glassOpacity = 0.88f,
        specularIntensity = 0.16f,
        ambientGlowIntensity = 0.35f
    )
}

val SubstanceTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = SpaceGroteskFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.8).sp
    ),
    displayMedium = TextStyle(
        fontFamily = SpaceGroteskFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = SpaceGroteskFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 24.sp
    ),
    titleLarge = TextStyle(
        fontFamily = SpaceGroteskFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 22.sp
    ),
    titleMedium = TextStyle(
        fontFamily = SpaceGroteskFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = SpaceGroteskFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = SpaceGroteskFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = RobotoMonoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontFamily = RobotoMonoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 13.sp
    )
)

@Composable
fun SubstanceGlassTheme(
    settings: UserSettings = UserSettings(),
    content: @Composable () -> Unit
) {
    val isLight = settings.isLightMode || settings.darkThemePreset == "CLINICAL_LIGHT"

    val (bgTop, bgBottom, cardBase, borderBase) = if (isLight) {
        listOf(
            Color(0xFFF1F5F9),
            Color(0xFFE2E8F0),
            Color(0xFFFFFFFF),
            Color(0xFFCBD5E1)
        )
    } else {
        when (settings.darkThemePreset) {
            "PITCH_BLACK_AMOLED" -> listOf(
                Color(0xFF000000),
                Color(0xFF050810),
                Color(0xFF0D131F),
                Color(0xFF1E293B)
            )
            "IOS_ABYSSAL_GLASS" -> listOf(
                Color(0xFF070A12),
                Color(0xFF0C1220),
                Color(0xFF151E30),
                Color(0xFF26354D)
            )
            else -> listOf( // PROFESSIONAL_SLATE (Default clean professional dark)
                Color(0xFF0B0F17),
                Color(0xFF0F172A),
                Color(0xFF151F32),
                Color(0xFF24324B)
            )
        }
    }

    val textPri = if (isLight) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val textSec = if (isLight) Color(0xFF475569) else Color(0xFF94A3B8)
    val textMut = if (isLight) Color(0xFF64748B) else Color(0xFF64748B)

    val effectiveOpacity = if (settings.isLightweightMode || isLight) 1.0f else settings.glassOpacity
    val effectiveGlow = if (settings.isLightweightMode) 0.0f else settings.ambientGlowIntensity

    val glassEnv = SubstanceGlassEnvironment(
        isLightMode = isLight,
        isLightweightMode = settings.isLightweightMode,
        showChartMinMaxAvgLines = settings.showChartMinMaxAvgLines,
        canvasBackgroundTop = bgTop,
        canvasBackgroundBottom = bgBottom,
        cardSurfaceBase = cardBase,
        cardBorderBase = borderBase,
        glassOpacity = effectiveOpacity,
        specularIntensity = settings.specularBorderIntensity,
        ambientGlowIntensity = effectiveGlow,
        textPrimary = textPri,
        textSecondary = textSec,
        textMuted = textMut
    )

    val colorScheme = if (isLight) {
        lightColorScheme(
            primary = Color(0xFF0284C7),
            secondary = Color(0xFF059669),
            tertiary = Color(0xFFD97706),
            background = bgTop,
            surface = cardBase,
            onBackground = textPri,
            onSurface = textPri,
            error = Color(0xFFDC2626)
        )
    } else {
        darkColorScheme(
            primary = Color(0xFF06B6D4),
            secondary = Color(0xFF10B981),
            tertiary = Color(0xFFF59E0B),
            background = bgTop,
            surface = cardBase,
            onBackground = textPri,
            onSurface = textPri,
            error = Color(0xFFEF4444)
        )
    }

    CompositionLocalProvider(LocalSubstanceGlass provides glassEnv) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SubstanceTypography,
            content = content
        )
    }
}

object SubstanceIcons {
    val allIconOptions: List<Pair<String, ImageVector>> = listOf(
        "coffee" to Icons.Default.Coffee,
        "leaf" to Icons.Default.Eco,
        "bolt" to Icons.Default.Bolt,
        "brain" to Icons.Default.Psychology,
        "glass" to Icons.Default.LocalBar,
        "capsule" to Icons.Default.Medication,
        "science" to Icons.Default.Science,
        "biotech" to Icons.Default.Biotech,
        "dropper" to Icons.Default.Colorize,
        "water" to Icons.Default.WaterDrop,
        "flame" to Icons.Default.LocalFireDepartment,
        "vape" to Icons.Default.VapingRooms,
        "spa" to Icons.Default.Spa,
        "fitness" to Icons.Default.FitnessCenter,
        "moon" to Icons.Default.Bedtime,
        "sun" to Icons.Default.WbSunny,
        "heart" to Icons.Default.Favorite,
        "cafe" to Icons.Default.LocalCafe,
        "drink" to Icons.Default.LocalDrink,
        "pharmacy" to Icons.Default.LocalPharmacy,
        "electric" to Icons.Default.ElectricBolt,
        "zen" to Icons.Default.SelfImprovement,
        "crystal" to Icons.Default.AcUnit,
        "spark" to Icons.Default.AutoAwesome,
        "vintage" to Icons.Default.FilterVintage,
        "flare" to Icons.Default.Flare,
        "night" to Icons.Default.Nightlight,
        "opacity" to Icons.Default.Opacity,
        "speed" to Icons.Default.Speed,
        "timeline" to Icons.Default.Timeline
    )

    fun iconForName(name: String): ImageVector {
        return allIconOptions.firstOrNull { it.first.equals(name, ignoreCase = true) }?.second
            ?: Icons.Default.Science
    }

    val presetColorsHex: List<Long> = listOf(
        0xFF06B6D4L, // Cyan
        0xFF10B981L, // Emerald
        0xFFF59E0BL, // Amber
        0xFF8B5CF6L, // Violet
        0xFFF43F5EL, // Rose
        0xFF38BDF8L, // Sky Blue
        0xFFEC4899L, // Magenta
        0xFF22C55EL, // Green
        0xFFF97316L, // Orange
        0xFF6366F1L, // Indigo
        0xFF14B8A6L, // Teal
        0xFFEAB308L  // Gold
    )
}
