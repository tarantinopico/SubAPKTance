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
 * Organized into:
 * - Mode A: Global Overview (5 Sub-Pages)
 * - Mode B: Per-Substance Deep Dive (5 Sub-Pages)
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
        Color(0xFF06B6D4)
    }

    val chipBgInactive = if (glassEnv.isLightMode) Color(0xFFE2E8F0) else Color.White.copy(alpha = 0.05f)
    val chipBorderInactive = glassEnv.cardBorderBase

    DynamicAmbientGlowBox(
        activeTier = DoseTier.LIGHT,
        overrideAccentColor = accentColor,
        modifier = modifier
            .fillMaxSize()
            .testTag("analytics_hub_screen_root")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Controls Bar: Mode Switcher + Timeframe + Substance Selector + 5 Sub-Pages Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                // Row 1: Mode Switcher (Global vs Per-Substance)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(chipBgInactive)
                        .border(1.dp, chipBorderInactive, RoundedCornerShape(12.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val modes = listOf(
                        0 to "Celkový Přehled (Global)",
                        1 to "Detail Látky (Per-Substance)"
                    )
                    modes.forEach { (mIdx, mLabel) ->
                        val active = selectedMode == mIdx
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(9.dp))
                                .background(
                                    if (active) accentColor.copy(alpha = 0.22f)
                                    else Color.Transparent
                                )
                                .border(
                                    width = if (active) 1.dp else 0.dp,
                                    color = if (active) accentColor else Color.Transparent,
                                    shape = RoundedCornerShape(9.dp)
                                )
                                .clickable { selectedMode = mIdx }
                                .padding(vertical = 6.dp)
                                .testTag("analytics_mode_tab_$mIdx"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = mLabel,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontSize = 11.sp,
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (active) glassEnv.textPrimary else glassEnv.textSecondary
                            )
                        }
                    }
                }

                // Row 2: Substance Selector when in Per-Substance Mode
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
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isSelected) subColor.copy(alpha = 0.22f)
                                        else chipBgInactive
                                    )
                                    .border(
                                        width = if (isSelected) 1.2.dp else 0.8.dp,
                                        color = if (isSelected) subColor else chipBorderInactive,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { selectedSubstanceId = sub.id }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
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
                                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 10.sp),
                                    color = glassEnv.textPrimary
                                )
                            }
                        }
                    }
                }

                // Row 3: 5 Sub-Pages Navigation Strip + Timeframe Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val subPages = listOf(
                        0 to "1. Přehled",
                        1 to "2. Druhy",
                        2 to "3. Korelace",
                        3 to "4. Rozestupy",
                        4 to "5. PK & Tělo",
                        5 to "Vše"
                    )
                    subPages.forEach { (spIdx, spLabel) ->
                        val active = selectedSubPage == spIdx
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (active) accentColor.copy(alpha = 0.24f)
                                    else chipBgInactive
                                )
                                .border(
                                    width = if (active) 1.dp else 0.8.dp,
                                    color = if (active) accentColor else chipBorderInactive,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedSubPage = spIdx }
                                .padding(horizontal = 9.dp, vertical = 5.dp)
                                .testTag("analytics_subpage_tab_$spIdx"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = spLabel,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (active) accentColor else glassEnv.textSecondary
                            )
                        }
                    }
                }

                // Row 4: Timeframe Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
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
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (active) accentColor.copy(alpha = 0.18f)
                                    else chipBgInactive
                                )
                                .border(
                                    width = 0.8.dp,
                                    color = if (active) accentColor else chipBorderInactive,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedTimeframeDays = days }
                                .padding(vertical = 3.5.dp)
                                .testTag("timeframe_chip_${label.lowercase()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.5.sp,
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
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
