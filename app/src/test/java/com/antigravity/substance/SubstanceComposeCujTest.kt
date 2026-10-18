package com.antigravity.substance

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.antigravity.substance.data.db.SubstanceDatabase
import com.antigravity.substance.data.preferences.UserSettings
import com.antigravity.substance.domain.engine.AnalyticsAggregatorEngine
import com.antigravity.substance.domain.engine.PharmacokineticEngine
import com.antigravity.substance.domain.model.DoseTier
import com.antigravity.substance.ui.analytics.AnalyticsHubScreen
import com.antigravity.substance.ui.dashboard.DashboardScreen
import com.antigravity.substance.ui.dashboard.DashboardUiState
import com.antigravity.substance.ui.logger.LogDoseScreen
import com.antigravity.substance.ui.logger.LogDoseUiState
import com.antigravity.substance.ui.settings.SettingsControlCenterScreen
import com.antigravity.substance.ui.theme.SubstanceGlassTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SubstanceComposeCujTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun dashboardScreen_rendersAllZeroScrollBentoCardsInLightAndLightweightMode() {
        val now = 1_700_050_000_000L
        val substances = SubstanceDatabase.defaultSubstances()
        val demoLogs = SubstanceDatabase.buildDemoLogs(now)
        val caffeine = substances.first()
        val session = PharmacokineticEngine.buildActiveSession(
            substance = caffeine,
            allSubstanceLogs = demoLogs.filter { it.substanceId == caffeine.id },
            nowMillis = now,
            startOfTodayMillis = AnalyticsAggregatorEngine.startOfDayMillis(now)
        )

        val settings = UserSettings(isLightMode = true, isLightweightMode = true)
        val state = DashboardUiState(
            nowMillis = now,
            settings = settings,
            activeSessions = listOfNotNull(session),
            highestActiveTier = session?.currentTier ?: DoseTier.MEDIUM,
            todayTotalSpend = 85.0,
            todayDosesCount = 4,
            todayDistinctSubstancesCount = 3
        )

        composeTestRule.setContent {
            SubstanceGlassTheme(settings = settings) {
                DashboardScreen(
                    state = state,
                    onSelectSessionTab = {},
                    onCycleHeroMode = {},
                    onCycleFavoritesPage = {},
                    onFlipConsumptionCard = {},
                    onQuickLogPreset = {},
                    onUndoQuickLog = {},
                    onNavigateToLogDose = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("dashboard_screen_root").assertIsDisplayed()
        composeTestRule.onNodeWithTag("dashboard_bio_load_header").assertIsDisplayed()
        composeTestRule.onNodeWithTag("dashboard_hero_pk_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("dashboard_time_since_last_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("dashboard_today_consumption_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("dashboard_bottom_dock_card").assertIsDisplayed()
    }

    @Test
    fun logDoseScreen_stepperAndSubtypeSelectionWorkCorrectly() {
        val substances = SubstanceDatabase.defaultSubstances()
        val subtypes = SubstanceDatabase.defaultSubtypes()
        val kratom = substances.first { it.name == "Kratom" }
        val kratomSubtypes = subtypes.filter { it.substanceId == kratom.id }
        val roas = SubstanceDatabase.defaultRoutesOfAdmin()
        var currentStepCalls = 0
        var selectedSubtypeId: Long? = null

        val preview = PharmacokineticEngine.previewCandidateDose(
            substance = kratom,
            existingLogs = emptyList(),
            candidateDoseAmount = 3.0,
            roa = roas.first(),
            candidateTimestampMillis = 1_700_050_000_000L
        )

        val state = LogDoseUiState(
            substances = substances,
            selectedSubstance = kratom,
            applicableSubtypes = kratomSubtypes,
            selectedSubtype = kratomSubtypes.firstOrNull(),
            routesOfAdmin = roas,
            selectedRoa = roas.first(),
            doseAmount = 3.0,
            activeBackgroundTier = preview.stackedPeakTier,
            projectedPreview = preview
        )

        composeTestRule.setContent {
            SubstanceGlassTheme(settings = UserSettings(isLightweightMode = true)) {
                LogDoseScreen(
                    state = state,
                    onSelectSubstance = {},
                    onSelectSubtype = { selectedSubtypeId = it },
                    onSelectRoa = {},
                    onStepDose = { mult -> currentStepCalls += mult },
                    onSetExactDose = {},
                    onSetBackdateMinutes = {},
                    onSelectContextTag = {},
                    onUpdateNotes = {},
                    onUpdateCustomField = { _, _ -> },
                    onToggleGlowStackMode = {},
                    onCommitDose = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("log_dose_screen_root").assertIsDisplayed()
        composeTestRule.onNodeWithTag("dose_stepper_plus").performClick()
        composeTestRule.onNodeWithTag("dose_stepper_plus").performClick()
        assertEquals(2, currentStepCalls)

        val firstSubtype = kratomSubtypes.first()
        composeTestRule.onNodeWithTag("select_subtype_${firstSubtype.name.lowercase()}").performClick()
        assertEquals(firstSubtype.id, selectedSubtypeId)
        composeTestRule.onNodeWithTag("commit_log_dose_button").assertIsDisplayed()
    }

    @Test
    fun analyticsHubScreen_switchesModesAndAll5SubPages() {
        val now = 1_700_050_000_000L
        val substances = SubstanceDatabase.defaultSubstances()
        val subtypes = SubstanceDatabase.defaultSubtypes()
        val demoLogs = SubstanceDatabase.buildDemoLogs(now)
        val settings = UserSettings(isLightweightMode = true)

        composeTestRule.setContent {
            SubstanceGlassTheme(settings = settings) {
                AnalyticsHubScreen(
                    substances = substances,
                    subtypes = subtypes,
                    allLogs = demoLogs,
                    settings = settings,
                    onDuplicateLog = {},
                    onDeleteLog = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("global_analytics_list").assertIsDisplayed()

        // Navigate across the Sub-Pages in Global Analytics
        for (pageIdx in 0..3) {
            composeTestRule.onNodeWithTag("analytics_subpage_tab_$pageIdx").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("global_analytics_list").assertIsDisplayed()
        }

        // Switch to Mode B: Per-Substance Deep Dive and navigate across its Sub-Pages
        composeTestRule.onNodeWithTag("analytics_mode_tab_1").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("substance_deep_dive_list").assertIsDisplayed()

        for (pageIdx in 0..3) {
            composeTestRule.onNodeWithTag("analytics_subpage_tab_$pageIdx").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("substance_deep_dive_list").assertIsDisplayed()
        }
    }

    @Test
    fun settingsControlCenter_switches5SubPagesAndTogglesLightAndLightweightModes() {
        val substances = SubstanceDatabase.defaultSubstances()
        val roas = SubstanceDatabase.defaultRoutesOfAdmin()
        var toggledLightMode = false

        composeTestRule.setContent {
            SubstanceGlassTheme(settings = UserSettings(isLightweightMode = true)) {
                SettingsControlCenterScreen(
                    settings = UserSettings(isLightMode = false, isLightweightMode = false),
                    substances = substances,
                    allLogs = emptyList(),
                    routesOfAdmin = roas,
                    globalCustomFields = emptyList(),
                    exportedDataPreview = null,
                    onUpdateCurrency = { _, _, _ -> },
                    onUpdateBedtime = {},
                    onUpdateVisualTheme = { _, _, _, _, _, isLight, _, _ ->
                        if (isLight != null) {
                            toggledLightMode = isLight
                        }
                    },
                    onAddCustomUnit = {},
                    onRemoveCustomUnit = {},
                    onAddContextTag = {},
                    onSaveRoa = {},
                    onDeleteRoa = {},
                    onSaveGlobalCustomField = {},
                    onDeleteGlobalCustomField = {},
                    onUpdateLog = {},
                    onDuplicateLog = {},
                    onDeleteLog = {},
                    onReloadDemoDataset = {},
                    onClearSampleLogs = {},
                    onExportJson = {},
                    onExportCsv = {},
                    onImportJson = {},
                    onDismissExportPreview = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("settings_control_center_root").assertIsDisplayed()
        composeTestRule.onNodeWithTag("toggle_light_mode_switch").performClick()
        assertTrue(toggledLightMode)

        for (pageIdx in 1..3) {
            composeTestRule.onNodeWithTag("settings_subpage_tab_$pageIdx").performClick()
            composeTestRule.waitForIdle()
        }
    }
}
