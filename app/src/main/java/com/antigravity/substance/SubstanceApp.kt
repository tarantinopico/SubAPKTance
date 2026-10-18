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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
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
import androidx.compose.ui.graphics.Brush
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
import com.antigravity.substance.ui.theme.LocalSubstanceGlass
import com.antigravity.substance.ui.theme.SubstanceGlassTheme
import kotlinx.coroutines.launch

enum class SubstanceTab(val label: String, val icon: ImageVector, val tag: String) {
    DASHBOARD("Cockpit", Icons.Default.Dashboard, "nav_tab_dashboard"),
    LOG_DOSE("Log Dose", Icons.Default.AddCircle, "nav_tab_log_dose"),
    ANALYTICS("Analytics", Icons.Default.Analytics, "nav_tab_analytics"),
    SUBSTANCES("Studio", Icons.Default.Science, "nav_tab_substances"),
    SETTINGS("Control", Icons.Default.Tune, "nav_tab_settings")
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(glass.canvasBackgroundTop)
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            // Main Viewport Content (Above Compact Floating Glass Bottom Nav Bar)
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

            // Compact Floating Bottom Navigation Bar (Adapts to Light Mode & Lightweight Mode)
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
    val barShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)

    val bgModifier = if (glass.isLightweightMode) {
        Modifier.background(glass.cardSurfaceBase)
    } else {
        Modifier.background(
            Brush.verticalGradient(
                colors = listOf(
                    glass.cardSurfaceBase.copy(alpha = 0.96f),
                    glass.canvasBackgroundBottom.copy(alpha = 0.99f)
                )
            )
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(barShape)
            .then(bgModifier)
            .border(
                width = 1.dp,
                color = glass.cardBorderBase,
                shape = barShape
            )
            .windowInsetsPadding(WindowInsets.navigationBars)
            .height(56.dp)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SubstanceTab.entries.forEachIndexed { idx, tab ->
            val selected = idx == currentTabIndex
            val isCenterLogTab = tab == SubstanceTab.LOG_DOSE
            val activeColor = if (isCenterLogTab) Color(0xFF10B981) else Color(0xFF06B6D4)

            val pillShape = RoundedCornerShape(14.dp)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(pillShape)
                    .background(
                        if (selected) activeColor.copy(alpha = if (glass.isLightMode) 0.14f else 0.18f)
                        else Color.Transparent
                    )
                    .clickable { onSelectTab(idx) }
                    .padding(vertical = 5.dp)
                    .testTag(tab.tag),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = tab.icon,
                    contentDescription = tab.label,
                    tint = if (selected) activeColor else glass.textSecondary,
                    modifier = Modifier.size(if (isCenterLogTab) 22.dp else 19.dp)
                )
                Text(
                    text = tab.label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.5.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (selected) glass.textPrimary else glass.textSecondary,
                    maxLines = 1
                )
            }
        }
    }
}
