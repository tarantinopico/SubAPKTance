package com.antigravity.substance.ui.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.substance.data.db.entities.IntakeLogEntity
import com.antigravity.substance.data.db.entities.SubstanceEntity
import com.antigravity.substance.data.db.entities.SubstanceSubtypeEntity
import com.antigravity.substance.data.preferences.UserSettings
import com.antigravity.substance.domain.engine.AnalyticsAggregatorEngine
import com.antigravity.substance.domain.model.DoseTier
import com.antigravity.substance.ui.theme.DynamicAmbientGlowBox
import com.antigravity.substance.ui.theme.LocalSubstanceGlass
import com.antigravity.substance.ui.theme.SubstanceIcons

/**
 * SCREEN 3: PROFESSIONAL BIO-ANALYTICS SUITE (45+ INTERACTIVE VISUALIZATIONS)
 * Styled in Matte OLED & Indigo matching the reference screenshots:
 * - Header Row with "Analytika" title on left + Segmented Timeframe Capsule (7d | 14d | 30d | 90d | Vše) on right
 * - Dark Pill Chips with Indigo (#5B4DFF) active border for Mode, Substance, and 5 Sub-Pages
 */
@Composable
fun AnalyticsHubScreen(
    substances: List<SubstanceEntity>,
    allLogs: List<IntakeLogEntity>,
    settings: UserSettings,
    subtypes: List<SubstanceSubtypeEntity> = emptyList(),
    onDuplicateLog: (IntakeLogEntity) -> Unit,
    onDeleteLog: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val glassEnv = LocalSubstanceGlass.current
    var selectedMode by remember { mutableIntStateOf(0) } // 0 = Global Overview, 1 = Per-Substance Deep Dive
    var selectedSubPage by remember { mutableIntStateOf(0) } // 0..4 = 5 Sub-Pages, 5 = All Combined
    var selectedTimeframeDays by remember { mutableIntStateOf(30) } // 7, 14, 30, 90, 365
    var selectedSubstanceId by remember(substances) {
        mutableLongStateOf(substances.firstOrNull()?.id ?: 1L)
    }

    val selectedSubstance = substances.firstOrNull { it.id == selectedSubstanceId }
        ?: substances.firstOrNull()

    val nowMillis = remember(allLogs.size, selectedTimeframeDays) { System.currentTimeMillis() }

    val globalReport = remember(substances, allLogs, subtypes, selectedTimeframeDays, settings) {
        AnalyticsAggregatorEngine.buildGlobalReport(
            substances = substances,
            allLogs = allLogs,
            timeframeDays = selectedTimeframeDays,
            nowMillis = nowMillis,
            subtypes = subtypes,
            settings = settings
        )
    }

    val substanceReport = remember(selectedSubstance, allLogs, subtypes, selectedTimeframeDays, settings) {
        selectedSubstance?.let { sub ->
            AnalyticsAggregatorEngine.buildSubstanceDeepDive(
                substance = sub,
                allLogs = allLogs,
                timeframeDays = selectedTimeframeDays,
                bedtimeHour24 = settings.bedtimeHour24,
                nowMillis = nowMillis,
                allSubstances = substances,
                subtypes = subtypes.filter { it.substanceId == sub.id },
                settings = settings
            )
        }
    }

    val accentColor = if (selectedMode == 1 && selectedSubstance != null) {
        Color(selectedSubstance.colorHex)
    } else {
        glassEnv.primaryAccent
    }

    DynamicAmbientGlowBox(
        activeTier = DoseTier.LIGHT,
        overrideAccentColor = accentColor,
        modifier = modifier
            .fillMaxSize()
            .testTag("analytics_hub_screen_root")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Controls Bar matching the reference screenshots
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Row 0: "Analytika" Title + Top-Right Segmented Timeframe Capsule (7d | 14d | 30d | 90d | Vše)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Analytika",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = glassEnv.textPrimary
                    )

                    // Compact Segmented Capsule matching reference screenshot top-right bar
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(glassEnv.cardSurfaceBase)
                            .border(1.dp, glassEnv.cardBorderBase, RoundedCornerShape(10.dp))
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val timeframes = listOf(
                            7 to "7D",
                            14 to "14D",
                            30 to "30D",
                            90 to "90D",
                            365 to "1R"
                        )
                        timeframes.forEach { (days, label) ->
                            val active = selectedTimeframeDays == days
                            val displayLabel = when (days) {
                                7 -> "7d"
                                14 -> "14d"
                                30 -> "30d"
                                90 -> "90d"
                                else -> "Vše"
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (active) glassEnv.badgeSurface
                                        else Color.Transparent
                                    )
                                    .clickable { selectedTimeframeDays = days }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .testTag("timeframe_chip_${label.lowercase()}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = displayLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (active) glassEnv.textPrimary else glassEnv.textSecondary
                                )
                            }
                        }
                    }
                }

                // Row 1: Mode Switcher Pill Row (Všechny látky / Celkový přehled vs Detail látky)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val modes = listOf(
                        0 to "Všechny látky (Přehled)",
                        1 to "Detail látky (${selectedSubstance?.name ?: "Látka"})"
                    )
                    modes.forEach { (mIdx, mLabel) ->
                        val active = selectedMode == mIdx
                        val pillShape = RoundedCornerShape(50)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(pillShape)
                                .background(
                                    if (active) glassEnv.pillSurfaceActive
                                    else glassEnv.pillSurfaceInactive
                                )
                                .border(
                                    width = if (active) 1.3.dp else 1.dp,
                                    color = if (active) glassEnv.pillBorderActive else glassEnv.pillBorderInactive,
                                    shape = pillShape
                                )
                                .clickable { selectedMode = mIdx }
                                .padding(vertical = 6.dp)
                                .testTag("analytics_mode_tab_$mIdx"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = mLabel,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 11.sp,
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (active) glassEnv.textPrimary else glassEnv.textSecondary,
                                maxLines = 1
                            )
                        }
                    }
                }

                // Row 2: Substance Filter Pills when in Per-Substance Mode
                if (selectedMode == 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        substances.forEach { sub ->
                            val isSelected = sub.id == selectedSubstance?.id
                            val subColor = Color(sub.colorHex)
                            val subCount = subtypes.count { it.substanceId == sub.id }
                            val pillShape = RoundedCornerShape(50)
                            Row(
                                modifier = Modifier
                                    .clip(pillShape)
                                    .background(
                                        if (isSelected) lerp(glassEnv.cardSurfaceBase, subColor, 0.20f)
                                        else glassEnv.pillSurfaceInactive
                                    )
                                    .border(
                                        width = if (isSelected) 1.3.dp else 1.dp,
                                        color = if (isSelected) subColor else glassEnv.pillBorderInactive,
                                        shape = pillShape
                                    )
                                    .clickable { selectedSubstanceId = sub.id }
                                    .padding(horizontal = 11.dp, vertical = 5.dp)
                                    .testTag("analytics_sub_${sub.name.lowercase()}"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = SubstanceIcons.iconForName(sub.iconName),
                                    contentDescription = null,
                                    tint = subColor,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "${sub.name} (${sub.defaultUnit}) • $subCount druhů",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontSize = 10.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (isSelected) glassEnv.textPrimary else glassEnv.textSecondary
                                )
                            }
                        }
                    }
                }

                // Row 3: 5 Sub-Pages Pill Strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val subPages = listOf(
                        0 to "Přehled",
                        1 to "Druhy",
                        2 to "Korelace",
                        3 to "Rozestupy",
                        4 to "PK & Tělo",
                        5 to "Všechny grafy"
                    )
                    subPages.forEach { (spIdx, spLabel) ->
                        val active = selectedSubPage == spIdx
                        val pillShape = RoundedCornerShape(50)
                        Box(
                            modifier = Modifier
                                .clip(pillShape)
                                .background(
                                    if (active) glassEnv.pillSurfaceActive
                                    else glassEnv.pillSurfaceInactive
                                )
                                .border(
                                    width = if (active) 1.3.dp else 1.dp,
                                    color = if (active) glassEnv.pillBorderActive else glassEnv.pillBorderInactive,
                                    shape = pillShape
                                )
                                .clickable { selectedSubPage = spIdx }
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                                .testTag("analytics_subpage_tab_$spIdx"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = spLabel,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 10.5.sp,
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (active) glassEnv.textPrimary else glassEnv.textSecondary
                            )
                        }
                    }
                }
            }

            // Content Body
            if (selectedMode == 0) {
                GlobalAnalyticsBentoView(
                    report = globalReport,
                    substances = substances,
                    settings = settings,
                    selectedSubPage = selectedSubPage,
                    onSelectSubPage = { selectedSubPage = it },
                    modifier = Modifier.weight(1f)
                )
            } else if (substanceReport != null) {
                SubstanceAnalyticsBentoView(
                    report = substanceReport,
                    settings = settings,
                    selectedSubPage = selectedSubPage,
                    onSelectSubPage = { selectedSubPage = it },
                    onDuplicateLog = onDuplicateLog,
                    onDeleteLog = onDeleteLog,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
