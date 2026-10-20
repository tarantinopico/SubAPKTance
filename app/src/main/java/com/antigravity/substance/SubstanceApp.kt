package com.antigravity.substance

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.antigravity.substance.data.preferences.UserSettings
import com.antigravity.substance.data.repository.SubstanceRepository
import com.antigravity.substance.ui.analytics.AnalyticsHubScreen
import com.antigravity.substance.ui.dashboard.DashboardScreen
import com.antigravity.substance.ui.dashboard.DashboardViewModel
import com.antigravity.substance.ui.logger.LogDoseScreen
import com.antigravity.substance.ui.logger.LogDoseViewModel
import com.antigravity.substance.ui.settings.SettingsControlCenterScreen
import com.antigravity.substance.ui.substances.SubstanceListScreen
import com.antigravity.substance.ui.theme.LatkyTopBrandHeader
import com.antigravity.substance.ui.theme.LocalSubstanceGlass
import com.antigravity.substance.ui.theme.SubstanceGlassTheme
import kotlinx.coroutines.launch

enum class SubstanceTab(val label: String, val icon: ImageVector, val tag: String) {
    DASHBOARD("Přehled", Icons.Default.Dashboard, "nav_tab_dashboard"),
    LOG_DOSE("Přidat", Icons.Default.Add, "nav_tab_log_dose"),
    ANALYTICS("Analytika", Icons.Default.Analytics, "nav_tab_analytics"),
    SUBSTANCES("Látky", Icons.Default.Science, "nav_tab_substances"),
    SETTINGS("Nastavení", Icons.Default.Tune, "nav_tab_settings")
}

@Composable
fun SubstanceApp(
    repository: SubstanceRepository,
    dashboardViewModel: DashboardViewModel,
    logDoseViewModel: LogDoseViewModel
) {
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        repository.ensureSeededIfNeeded()
    }

    val settings by repository.settingsFlow.collectAsStateWithLifecycle(initialValue = UserSettings())
    val allSubstances by repository.allSubstancesFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val activeSubstances by repository.activeSubstancesFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val allSubtypes by repository.allSubtypesFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val allLogs by repository.allLogsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val customFields by repository.customFieldDefsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val routesOfAdmin by repository.routesOfAdminFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    val dashboardState by dashboardViewModel.uiState.collectAsStateWithLifecycle()
    val logDoseState by logDoseViewModel.uiState.collectAsStateWithLifecycle()

    var currentTabIndex by remember { mutableIntStateOf(0) }
    var exportedDataSnapshot by remember { mutableStateOf<String?>(null) }

    if (currentTabIndex != 0) {
        BackHandler {
            currentTabIndex = 0
        }
    }

    SubstanceGlassTheme(settings = settings) {
        val glass = LocalSubstanceGlass.current
        val safetyScore = remember(dashboardState.totalBioLoadFraction) {
            (100 - (dashboardState.totalBioLoadFraction * 45f).toInt()).coerceIn(10, 100)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(glass.canvasBackgroundTop)
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            // Top Látky Brand & Offline Status Header matching screenshots
            LatkyTopBrandHeader(
                safetyScore = safetyScore,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )

            // Main Viewport Content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (SubstanceTab.entries[currentTabIndex]) {
                    SubstanceTab.DASHBOARD -> {
                        DashboardScreen(
                            state = dashboardState,
                            onSelectSessionTab = dashboardViewModel::selectActiveSessionTab,
                            onCycleHeroMode = dashboardViewModel::cycleHeroCardDisplayMode,
                            onCycleFavoritesPage = dashboardViewModel::cycleFavoritesPage,
                            onFlipConsumptionCard = dashboardViewModel::flipTodayConsumptionCard,
                            onQuickLogPreset = dashboardViewModel::quickLogPreset,
                            onUndoQuickLog = dashboardViewModel::undoLastQuickLog,
                            onNavigateToLogDose = { subId ->
                                if (subId != null) {
                                    logDoseViewModel.selectSubstance(subId)
                                }
                                currentTabIndex = SubstanceTab.LOG_DOSE.ordinal
                            }
                        )
                    }

                    SubstanceTab.LOG_DOSE -> {
                        LogDoseScreen(
                            state = logDoseState,
                            onSelectSubstance = logDoseViewModel::selectSubstance,
                            onSelectSubtype = logDoseViewModel::selectSubtype,
                            onSelectRoa = logDoseViewModel::selectRoa,
                            onStepDose = logDoseViewModel::stepDose,
                            onSetExactDose = logDoseViewModel::setExactDose,
                            onSetBackdateMinutes = logDoseViewModel::setBackdateOffsetMinutes,
                            onSelectContextTag = logDoseViewModel::selectContextTag,
                            onUpdateNotes = logDoseViewModel::updateNotes,
                            onUpdateCustomField = logDoseViewModel::updateCustomFieldValue,
                            onToggleGlowStackMode = logDoseViewModel::toggleGlowStackMode,
                            onCommitDose = {
                                logDoseViewModel.commitDoseLog()
                            }
                        )
                    }

                    SubstanceTab.ANALYTICS -> {
                        AnalyticsHubScreen(
                            substances = activeSubstances.ifEmpty { allSubstances },
                            subtypes = allSubtypes,
                            allLogs = allLogs,
                            settings = settings,
                            onDuplicateLog = { log ->
                                scope.launch { repository.duplicateLog(log) }
                            },
                            onDeleteLog = { id ->
                                scope.launch { repository.deleteLogById(id) }
                            }
                        )
                    }

                    SubstanceTab.SUBSTANCES -> {
                        SubstanceListScreen(
                            substances = allSubstances,
                            subtypes = allSubtypes,
                            customFields = customFields,
                            settings = settings,
                            onSaveSubstance = { sub ->
                                scope.launch { repository.upsertSubstance(sub) }
                            },
                            onDuplicateSubstance = { sub ->
                                scope.launch { repository.duplicateSubstance(sub) }
                            },
                            onDeleteSubstance = { id ->
                                scope.launch { repository.deleteSubstance(id) }
                            },
                            onSaveSubtype = { st ->
                                scope.launch { repository.upsertSubtype(st) }
                            },
                            onDeleteSubtype = { id ->
                                scope.launch { repository.deleteSubtype(id) }
                            },
                            onSaveCustomField = { def ->
                                scope.launch { repository.upsertCustomFieldDef(def) }
                            },
                            onDeleteCustomField = { id ->
                                scope.launch { repository.deleteCustomFieldDef(id) }
                            }
                        )
                    }

                    SubstanceTab.SETTINGS -> {
                        SettingsControlCenterScreen(
                            settings = settings,
                            substances = allSubstances,
                            allLogs = allLogs,
                            routesOfAdmin = routesOfAdmin,
                            globalCustomFields = customFields.filter { it.substanceId == null },
                            exportedDataPreview = exportedDataSnapshot,
                            onUpdateCurrency = { sym, pref, prec ->
                                scope.launch { repository.settingsStore.updateCurrency(sym, pref, prec) }
                            },
                            onUpdateBedtime = { hr ->
                                scope.launch { repository.settingsStore.updateBedtimeHour(hr) }
                            },
                            onUpdateVisualTheme = { gOp, sInt, glInt, glStack, preset, isLight, isLightweight, showRefLines ->
                                scope.launch {
                                    repository.settingsStore.updateVisualTheme(
                                        glassOpacity = gOp,
                                        specularIntensity = sInt,
                                        glowIntensity = glInt,
                                        glowByStacked = glStack,
                                        themePreset = preset,
                                        isLightMode = isLight,
                                        isLightweightMode = isLightweight,
                                        showChartMinMaxAvgLines = showRefLines
                                    )
                                }
                            },
                            onUpdateStyleCustomization = { accentHex, cornerRadiusDp ->
                                scope.launch {
                                    repository.settingsStore.updateVisualTheme(
                                        primaryAccentHex = accentHex,
                                        cardCornerRadiusDp = cornerRadiusDp
                                    )
                                }
                            },
                            onUpdateGlobalMathConstants = { tolGain, tolDecay, superpos, onsetR, comeupR, peakR, offsetR, redoseH, breachP, redlineP, bodyMult ->
                                scope.launch {
                                    repository.settingsStore.updateCalculationConstants(
                                        toleranceGainFactor = tolGain,
                                        toleranceDecayPerDay = tolDecay,
                                        superpositionFactor = superpos,
                                        simpleOnsetRatio = onsetR,
                                        simpleComeupRatio = comeupR,
                                        simplePeakRatio = peakR,
                                        simpleOffsetRatio = offsetR,
                                        compulsiveRedoseHours = redoseH,
                                        safetyBreachPenalty = breachP,
                                        safetyRedlinePenalty = redlineP,
                                        bodyElimHalfLifeMult = bodyMult
                                    )
                                }
                            },
                            onResetMathConstants = {
                                scope.launch { repository.settingsStore.resetCalculationConstantsToDefault() }
                            },
                            onAddCustomUnit = { u ->
                                scope.launch { repository.settingsStore.addCustomUnit(u) }
                            },
                            onRemoveCustomUnit = { u ->
                                scope.launch { repository.settingsStore.removeCustomUnit(u) }
                            },
                            onAddContextTag = { t ->
                                scope.launch { repository.settingsStore.addContextTag(t) }
                            },
                            onSaveRoa = { roa ->
                                scope.launch { repository.upsertRouteOfAdmin(roa) }
                            },
                            onDeleteRoa = { id ->
                                scope.launch { repository.deleteRouteOfAdmin(id) }
                            },
                            onSaveGlobalCustomField = { def ->
                                scope.launch { repository.upsertCustomFieldDef(def) }
                            },
                            onDeleteGlobalCustomField = { id ->
                                scope.launch { repository.deleteCustomFieldDef(id) }
                            },
                            onUpdateLog = { log ->
                                scope.launch { repository.logIntake(log) }
                            },
                            onDuplicateLog = { log ->
                                scope.launch { repository.duplicateLog(log) }
                            },
                            onDeleteLog = { id ->
                                scope.launch { repository.deleteLogById(id) }
                            },
                            onReloadDemoDataset = {
                                scope.launch { repository.seedDefaultDataWithDemoLogs() }
                            },
                            onClearSampleLogs = {
                                scope.launch { repository.clearSampleLogsKeepSubstances() }
                            },
                            onExportJson = {
                                scope.launch {
                                    exportedDataSnapshot = repository.exportFullDatabaseJson()
                                }
                            },
                            onExportCsv = {
                                scope.launch {
                                    exportedDataSnapshot = repository.exportLogsCsv()
                                }
                            },
                            onImportJson = { rawJson ->
                                scope.launch { repository.importDatabaseJson(rawJson) }
                            },
                            onDismissExportPreview = {
                                exportedDataSnapshot = null
                            }
                        )
                    }
                }
            }

            // Matte OLED Bottom Navigation Bar matching reference screenshots
            FrostedBottomNavBar(
                currentTabIndex = currentTabIndex,
                onSelectTab = { currentTabIndex = it }
            )
        }
    }
}

@Composable
private fun FrostedBottomNavBar(
    currentTabIndex: Int,
    onSelectTab: (Int) -> Unit
) {
    val glass = LocalSubstanceGlass.current
    val navBg = if (glass.isLightMode) Color(0xFFFFFFFF) else Color(0xFF07090D)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(navBg)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        // Subtle 1dp top divider line (#181B24)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(glass.cardBorderBase)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SubstanceTab.entries.forEachIndexed { idx, tab ->
                val selected = idx == currentTabIndex
                val isAddTab = tab == SubstanceTab.LOG_DOSE
                val activeAccent = glass.primaryAccent
                val pillShape = RoundedCornerShape(14.dp)

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(pillShape)
                        .clickable { onSelectTab(idx) }
                        .padding(vertical = 5.dp)
                        .testTag(tab.tag),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (isAddTab) {
                        // Signature Emerald Green (+) circle button matching reference screenshots
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(
                                    if (selected) Color(0xFF10B981)
                                    else Color(0xFF10B981).copy(alpha = 0.22f)
                                )
                                .border(
                                    width = 1.2.dp,
                                    color = Color(0xFF10B981),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label,
                                tint = if (selected) Color(0xFF042F2E) else Color(0xFF10B981),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    } else {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            tint = if (selected) activeAccent else glass.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = when {
                            isAddTab && selected -> Color(0xFF10B981)
                            selected -> activeAccent
                            else -> glass.textSecondary
                        },
                        maxLines = 1
                    )
                }
            }
        }
    }
}
