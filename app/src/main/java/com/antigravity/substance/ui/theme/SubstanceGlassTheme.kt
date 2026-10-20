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
            glowAura = base.copy(alpha = 0.18f),
            borderAccent = base.copy(alpha = 0.45f)
        )
    } else {
        SubstanceTonalPalette(
            primary = base,
            container = base.copy(alpha = 0.15f),
            onContainer = lerp(base, Color.White, 0.55f),
            glowAura = base.copy(alpha = 0.25f),
            borderAccent = base.copy(alpha = 0.65f)
        )
    }
}

@Immutable
data class SubstanceGlassEnvironment(
    val isLightMode: Boolean = false,
    val isLightweightMode: Boolean = false,
    val showChartMinMaxAvgLines: Boolean = true,
    val primaryAccent: Color = Color(0xFF5B4DFF),
    val cardCornerRadiusDp: Int = 20,
    val canvasBackgroundTop: Color = Color(0xFF000000),
    val canvasBackgroundBottom: Color = Color(0xFF000000),
    val cardSurfaceBase: Color = Color(0xFF0F1117),
    val cardBorderBase: Color = Color(0xFF1E222D),
    val pillSurfaceInactive: Color = Color(0xFF0F1117),
    val pillBorderInactive: Color = Color(0xFF232734),
    val pillSurfaceActive: Color = Color(0xFF191836),
    val pillBorderActive: Color = Color(0xFF5B4DFF),
    val badgeSurface: Color = Color(0xFF1B1E28),
    val glassOpacity: Float = 0.96f,
    val specularIntensity: Float = 0.08f,
    val ambientGlowIntensity: Float = 0.0f,
    val textPrimary: Color = Color(0xFFF8FAFC),
    val textSecondary: Color = Color(0xFF9CA3AF),
    val textMuted: Color = Color(0xFF6B7280)
)

val LocalSubstanceGlass = staticCompositionLocalOf {
    SubstanceGlassEnvironment()
}

private val SubstanceTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = RobotoMonoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 42.sp,
        letterSpacing = (-1.0).sp
    ),
    displayMedium = TextStyle(
        fontFamily = RobotoMonoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = SpaceGroteskFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.3).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = SpaceGroteskFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 23.sp,
        letterSpacing = (-0.2).sp
    ),
    titleLarge = TextStyle(
        fontFamily = SpaceGroteskFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp
    ),
    titleMedium = TextStyle(
        fontFamily = SpaceGroteskFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.5.sp,
        lineHeight = 18.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = SpaceGroteskFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = SpaceGroteskFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 11.5.sp,
        lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = RobotoMonoFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.5.sp,
        lineHeight = 15.sp
    ),
    labelSmall = TextStyle(
        fontFamily = RobotoMonoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 13.sp,
        letterSpacing = 0.2.sp
    )
)

@Composable
fun SubstanceGlassTheme(
    settings: UserSettings = UserSettings(),
    content: @Composable () -> Unit
) {
    val isLight = settings.isLightMode || settings.darkThemePreset == "CLINICAL_LIGHT"
    val accentColor = Color(settings.primaryAccentHex)

    val (bgTop, bgBottom, cardBase, borderBase) = when {
        isLight -> listOf(
            Color(0xFFF8FAFC),
            Color(0xFFF1F5F9),
            Color(0xFFFFFFFF),
            Color(0xFFE2E8F0)
        )
        settings.darkThemePreset == "PROFESSIONAL_SLATE" -> listOf(
            Color(0xFF090C12),
            Color(0xFF0E131D),
            Color(0xFF141B29),
            Color(0xFF232D42)
        )
        settings.darkThemePreset == "IOS_ABYSSAL_GLASS" -> listOf(
            Color(0xFF05070E),
            Color(0xFF0A0F1D),
            Color(0xFF121829),
            Color(0xFF242E47)
        )
        else -> listOf(
            // Default: OLED_MATTE_INDIGO & PITCH_BLACK_AMOLED matching reference screenshots
            Color(0xFF000000),
            Color(0xFF000000),
            Color(0xFF0F1117),
            Color(0xFF1E222D)
        )
    }

    val colorScheme = if (isLight) {
        lightColorScheme(
            primary = accentColor,
            onPrimary = Color.White,
            secondary = Color(0xFF10B981),
            onSecondary = Color.White,
            tertiary = Color(0xFFF59E0B),
            background = bgTop,
            onBackground = Color(0xFF0F172A),
            surface = cardBase,
            onSurface = Color(0xFF0F172A),
            surfaceVariant = Color(0xFFF1F5F9),
            onSurfaceVariant = Color(0xFF475569),
            error = Color(0xFFDC2626)
        )
    } else {
        darkColorScheme(
            primary = accentColor,
            onPrimary = Color.White,
            secondary = Color(0xFF10B981),
            onSecondary = Color(0xFF032219),
            tertiary = Color(0xFFF59E0B),
            background = bgTop,
            onBackground = Color(0xFFF8FAFC),
            surface = cardBase,
            onSurface = Color(0xFFF8FAFC),
            surfaceVariant = Color(0xFF171A24),
            onSurfaceVariant = Color(0xFF9CA3AF),
            error = Color(0xFFEF4444)
        )
    }

    val glassEnv = SubstanceGlassEnvironment(
        isLightMode = isLight,
        isLightweightMode = settings.isLightweightMode,
        showChartMinMaxAvgLines = settings.showChartMinMaxAvgLines,
        primaryAccent = accentColor,
        cardCornerRadiusDp = settings.cardCornerRadiusDp.coerceIn(10, 32),
        canvasBackgroundTop = bgTop,
        canvasBackgroundBottom = bgBottom,
        cardSurfaceBase = cardBase,
        cardBorderBase = borderBase,
        pillSurfaceInactive = if (isLight) Color(0xFFF1F5F9) else Color(0xFF0F1117),
        pillBorderInactive = if (isLight) Color(0xFFCBD5E1) else Color(0xFF232734),
        pillSurfaceActive = if (isLight) accentColor.copy(alpha = 0.14f) else lerp(Color(0xFF0F1117), accentColor, 0.22f),
        pillBorderActive = accentColor,
        badgeSurface = if (isLight) Color(0xFFE2E8F0) else Color(0xFF1C1F2B),
        glassOpacity = if (settings.isLightweightMode || isLight) 1.0f else settings.glassOpacity,
        specularIntensity = if (settings.isLightweightMode) 0.05f else settings.specularBorderIntensity,
        ambientGlowIntensity = if (settings.isLightweightMode) 0.0f else settings.ambientGlowIntensity,
        textPrimary = if (isLight) Color(0xFF0F172A) else Color(0xFFF8FAFC),
        textSecondary = if (isLight) Color(0xFF475569) else Color(0xFF9CA3AF),
        textMuted = if (isLight) Color(0xFF64748B) else Color(0xFF6B7280)
    )

    CompositionLocalProvider(LocalSubstanceGlass provides glassEnv) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SubstanceTypography,
            content = content
        )
    }
}

object SubstanceIcons {
    val availableIcons: List<Pair<String, ImageVector>> = listOf(
        "LocalCafe" to Icons.Default.LocalCafe,
        "Coffee" to Icons.Default.Coffee,
        "Eco" to Icons.Default.Eco,
        "Spa" to Icons.Default.Spa,
        "Bolt" to Icons.Default.Bolt,
        "ElectricBolt" to Icons.Default.ElectricBolt,
        "Psychology" to Icons.Default.Psychology,
        "Science" to Icons.Default.Science,
        "Biotech" to Icons.Default.Biotech,
        "Medication" to Icons.Default.Medication,
        "LocalPharmacy" to Icons.Default.LocalPharmacy,
        "VapingRooms" to Icons.Default.VapingRooms,
        "WaterDrop" to Icons.Default.WaterDrop,
        "LocalDrink" to Icons.Default.LocalDrink,
        "LocalBar" to Icons.Default.LocalBar,
        "Nightlight" to Icons.Default.Nightlight,
        "Bedtime" to Icons.Default.Bedtime,
        "WbSunny" to Icons.Default.WbSunny,
        "FitnessCenter" to Icons.Default.FitnessCenter,
        "SelfImprovement" to Icons.Default.SelfImprovement,
        "Favorite" to Icons.Default.Favorite,
        "LocalFireDepartment" to Icons.Default.LocalFireDepartment,
        "Flare" to Icons.Default.Flare,
        "FilterVintage" to Icons.Default.FilterVintage,
        "AcUnit" to Icons.Default.AcUnit,
        "Colorize" to Icons.Default.Colorize,
        "Opacity" to Icons.Default.Opacity,
        "Speed" to Icons.Default.Speed,
        "Timeline" to Icons.Default.Timeline,
        "AutoAwesome" to Icons.Default.AutoAwesome
    )

    val allIconOptions: List<Pair<String, ImageVector>> = availableIcons

    val presetColorsHex: List<Long> = listOf(
        0xFF5B4DFFL, // Indigo
        0xFFF59E0BL, // Amber
        0xFF10B981L, // Emerald
        0xFF06B6D4L, // Cyan
        0xFF8B5CF6L, // Violet
        0xFFEC4899L, // Pink
        0xFF3B82F6L, // Blue
        0xFFEF4444L, // Red
        0xFF14B8A6L, // Teal
        0xFFA855F7L, // Purple
        0xFFEAB308L, // Yellow
        0xFFF97316L  // Orange
    )

    private val iconMap = availableIcons.toMap()

    fun iconForName(name: String): ImageVector =
        iconMap[name] ?: Icons.Default.Science
}
