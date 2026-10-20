package com.antigravity.substance.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.substance.data.db.entities.CustomFieldDefEntity
import com.antigravity.substance.data.db.entities.IntakeLogEntity
import com.antigravity.substance.data.db.entities.RouteOfAdminEntity
import com.antigravity.substance.data.db.entities.SubstanceEntity
import com.antigravity.substance.data.preferences.UserSettings
import com.antigravity.substance.domain.model.DoseTier
import com.antigravity.substance.ui.theme.BentoMicroHeader
import com.antigravity.substance.ui.theme.DynamicAmbientGlowBox
import com.antigravity.substance.ui.theme.FrostedBentoCard
import com.antigravity.substance.ui.theme.LocalSubstanceGlass
import java.util.Locale
import kotlin.math.roundToInt

/**
 * SCREEN 5: SETTINGS & COMPUTATIONAL CONTROL CENTER (5 Sub-Pages)
 * 0 = 1. Vzhled & Výkon (Light / Dark Mode, Lightweight Mode, Chart Min/Max/Avg lines)
 * 1 = 2. Výpočetní Engine (All global mathematical & pharmacokinetic constants)
 * 2 = 3. Jednotky, Měna & RoA (Currency, Bedtime, Units, Context Tags, Routes of Administration)
 * 3 = 4. Historie záznamů (Full Log History CRUD Manager)
 * 4 = 5. Data, Zálohy & Demo (JSON/CSV Export/Import, 30-Day Demo Data Generator)
 */
@Composable
fun SettingsControlCenterScreen(
    settings: UserSettings,
    substances: List<SubstanceEntity>,
    allLogs: List<IntakeLogEntity>,
    routesOfAdmin: List<RouteOfAdminEntity>,
    globalCustomFields: List<CustomFieldDefEntity>,
    exportedDataPreview: String?,
    onUpdateCurrency: (String, Boolean, Int) -> Unit,
    onUpdateBedtime: (Int) -> Unit,
    onUpdateVisualTheme: (
        glassOpacity: Float?,
        specularIntensity: Float?,
        glowIntensity: Float?,
        glowByStacked: Boolean?,
        darkThemePreset: String?,
        isLightMode: Boolean?,
        isLightweightMode: Boolean?,
        showChartMinMaxAvgLines: Boolean?
    ) -> Unit,
    onUpdateStyleCustomization: (
        primaryAccentHex: Long?,
        cardCornerRadiusDp: Int?
    ) -> Unit = { _, _ -> },
    onUpdateGlobalMathConstants: (
        tolGainFactor: Double?,
        tolDecayPerDay: Double?,
        superpositionFactor: Double?,
        simpleOnsetRatio: Double?,
        simpleComeupRatio: Double?,
        simplePeakRatio: Double?,
        simpleOffsetRatio: Double?,
        compulsiveRedoseHours: Double?,
        breachPenalty: Int?,
        redlinePenalty: Int?,
        bodyElimHalfLifeMult: Double?
    ) -> Unit = { _, _, _, _, _, _, _, _, _, _, _ -> },
    onResetMathConstants: () -> Unit = {},
    onAddCustomUnit: (String) -> Unit,
    onRemoveCustomUnit: (String) -> Unit,
    onAddContextTag: (String) -> Unit,
    onSaveRoa: (RouteOfAdminEntity) -> Unit,
    onDeleteRoa: (Long) -> Unit,
    onSaveGlobalCustomField: (CustomFieldDefEntity) -> Unit,
    onDeleteGlobalCustomField: (Long) -> Unit,
    onUpdateLog: (IntakeLogEntity) -> Unit,
    onDuplicateLog: (IntakeLogEntity) -> Unit,
    onDeleteLog: (Long) -> Unit,
    onReloadDemoDataset: () -> Unit,
    onClearSampleLogs: () -> Unit,
    onExportJson: () -> Unit,
    onExportCsv: () -> Unit,
    onImportJson: (String) -> Unit,
    onDismissExportPreview: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glassEnv = LocalSubstanceGlass.current
    var selectedSubPage by remember { mutableIntStateOf(0) } // 0..4 = 5 Sub-Pages
    var newUnitText by remember { mutableStateOf("") }
    var newTagText by remember { mutableStateOf("") }
    var newRoaName by remember { mutableStateOf("") }
    var newRoaBioStr by remember { mutableStateOf("1.15") }
    var newRoaOnsetStr by remember { mutableStateOf("0.75") }
    var newRoaDurStr by remember { mutableStateOf("0.90") }
    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonDraft by remember { mutableStateOf("") }

    // Math Constants Editable Drafts
    var tolGainStr by remember(settings.globalToleranceGainFactor) {
        mutableStateOf(settings.globalToleranceGainFactor.toString())
    }
    var tolDecayStr by remember(settings.globalToleranceDecayPerDay) {
        mutableStateOf(settings.globalToleranceDecayPerDay.toString())
    }
    var superposStr by remember(settings.globalSuperpositionFactor) {
        mutableStateOf(settings.globalSuperpositionFactor.toString())
    }
    var compulsiveHrsStr by remember(settings.compulsiveRedoseThresholdHours) {
        mutableStateOf(settings.compulsiveRedoseThresholdHours.toString())
    }
    var bodyHalfLifeMultStr by remember(settings.bodyEliminationHalfLifeMultiplier) {
        mutableStateOf(settings.bodyEliminationHalfLifeMultiplier.toString())
    }
    var breachPenStr by remember(settings.safetyScoreBreachPenalty) {
        mutableStateOf(settings.safetyScoreBreachPenalty.toString())
    }
    var redlinePenStr by remember(settings.safetyScoreRedlinePenalty) {
        mutableStateOf(settings.safetyScoreRedlinePenalty.toString())
    }

    DynamicAmbientGlowBox(
        activeTier = DoseTier.LIGHT,
        overrideAccentColor = glassEnv.primaryAccent,
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_control_center_root")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Header + 5 Sub-Pages Navigation Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "NASTAVENÍ, VZHLED, VÝPOČTY & SPRÁVA DAT",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.5.sp,
                        color = glassEnv.primaryAccent,
                        letterSpacing = 0.7.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "Nastavení & Control Center",
                    style = MaterialTheme.typography.headlineMedium,
                    color = glassEnv.textPrimary
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val tabs = listOf(
                        0 to "1. Vzhled & Výkon",
                        1 to "2. Výpočetní Engine",
                        2 to "3. Jednotky & RoA",
                        3 to "4. Historie (${allLogs.size})",
                        4 to "5. Data & Demo"
                    )
                    tabs.forEach { (idx, lbl) ->
                        val active = selectedSubPage == idx
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
                                .clickable { selectedSubPage = idx }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .testTag("settings_subpage_tab_$idx")
                        ) {
                            Text(
                                text = lbl,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.5.sp,
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (active) glassEnv.textPrimary else glassEnv.textSecondary
                            )
                        }
                    }
                }
            }

            if (selectedSubPage == 3) {
                // Sub-Page 4: Full Log History CRUD Manager
                LogHistoryCrudScreen(
                    logs = allLogs,
                    substances = substances,
                    settings = settings,
                    onUpdateLog = onUpdateLog,
                    onDuplicateLog = onDuplicateLog,
                    onDeleteLog = onDeleteLog,
                    onBack = { selectedSubPage = 0 }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 4.dp, bottom = 92.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // =========================================================================
                    // SUB-PAGE 1: VZHLED, MOTIV KARET, AKCENTOVÁ BARVA & LIGHTWEIGHT REŽIM
                    // =========================================================================
                    if (selectedSubPage == 0) {
                        item {
                            FrostedBentoCard(
                                modifier = Modifier.fillMaxWidth(),
                                accentColor = glassEnv.primaryAccent
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    BentoMicroHeader(
                                        title = "MOTIV VZHLEDU (MATNÝ OLED, BŘIDLICE NEBO LIGHT)",
                                        icon = Icons.Default.Palette,
                                        accentColor = glassEnv.primaryAccent,
                                        trailingText = "MOTIV"
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Světlý profesionální režim (Light Mode)",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = glassEnv.textPrimary
                                            )
                                            Text(
                                                text = "Vysoký kontrast pro denní světlo a klinickou čitelnost",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = glassEnv.textSecondary
                                            )
                                        }
                                        Switch(
                                            checked = settings.isLightMode,
                                            onCheckedChange = { isLight ->
                                                onUpdateVisualTheme(
                                                    null,
                                                    null,
                                                    null,
                                                    null,
                                                    if (isLight) "CLINICAL_LIGHT" else "OLED_MATTE_INDIGO",
                                                    isLight,
                                                    null,
                                                    null
                                                )
                                            },
                                            modifier = Modifier.testTag("toggle_light_mode_switch")
                                        )
                                    }

                                    // 4 Theme Presets
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf(
                                            "OLED_MATTE_INDIGO" to "Matný OLED",
                                            "PITCH_BLACK_AMOLED" to "Černá AMOLED",
                                            "PROFESSIONAL_SLATE" to "Břidlicová",
                                            "CLINICAL_LIGHT" to "Světlý Light"
                                        ).forEach { (presetKey, label) ->
                                            val sel = settings.darkThemePreset == presetKey
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(50))
                                                    .background(
                                                        if (sel) glassEnv.pillSurfaceActive
                                                        else glassEnv.pillSurfaceInactive
                                                    )
                                                    .border(
                                                        width = if (sel) 1.3.dp else 1.dp,
                                                        color = if (sel) glassEnv.pillBorderActive else glassEnv.pillBorderInactive,
                                                        shape = RoundedCornerShape(50)
                                                    )
                                                    .clickable {
                                                        val light = presetKey == "CLINICAL_LIGHT"
                                                        onUpdateVisualTheme(null, null, null, null, presetKey, light, null, null)
                                                    }
                                                    .padding(vertical = 7.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = label,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontSize = 9.sp,
                                                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium
                                                    ),
                                                    color = if (sel) glassEnv.textPrimary else glassEnv.textSecondary,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Accent Color Palette & Card Corner Radius / Surface Customization Card
                        item {
                            FrostedBentoCard(
                                modifier = Modifier.fillMaxWidth(),
                                accentColor = glassEnv.primaryAccent
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    BentoMicroHeader(
                                        title = "HLAVNÍ AKCENTOVÁ BARVA & TVAR KARET",
                                        icon = Icons.Default.Palette,
                                        accentColor = glassEnv.primaryAccent,
                                        trailingText = "STYL UI"
                                    )

                                    Text(
                                        text = "Zvolte hlavní akcentovou barvu pro aktivní pilulky, tlačítka a zvýraznění:",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = glassEnv.textSecondary
                                    )

                                    val accentPresets = listOf(
                                        0xFF5B4DFFL to "Indigo",
                                        0xFF8B5CF6L to "Fialová",
                                        0xFF3B82F6L to "Modrá",
                                        0xFF06B6D4L to "Azurová",
                                        0xFF10B981L to "Smaragd",
                                        0xFFF59E0BL to "Jantar",
                                        0xFFEC4899L to "Růžová"
                                    )
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        accentPresets.forEach { (hexVal, name) ->
                                            val isSelected = settings.primaryAccentHex == hexVal
                                            val swatchColor = Color(hexVal)
                                            Row(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(50))
                                                    .background(
                                                        if (isSelected) swatchColor.copy(alpha = 0.22f)
                                                        else glassEnv.pillSurfaceInactive
                                                    )
                                                    .border(
                                                        width = if (isSelected) 1.5.dp else 1.dp,
                                                        color = if (isSelected) swatchColor else glassEnv.pillBorderInactive,
                                                        shape = RoundedCornerShape(50)
                                                    )
                                                    .clickable { onUpdateStyleCustomization(hexVal, null) }
                                                    .padding(horizontal = 11.dp, vertical = 6.dp)
                                                    .testTag("accent_swatch_${name.lowercase()}"),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(12.dp)
                                                        .clip(RoundedCornerShape(50))
                                                        .background(swatchColor)
                                                )
                                                Text(
                                                    text = name,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontSize = 10.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                    ),
                                                    color = if (isSelected) glassEnv.textPrimary else glassEnv.textSecondary
                                                )
                                            }
                                        }
                                    }

                                    // Card Corner Radius Slider & Quick Chips
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Zaoblení rohů karet: ${settings.cardCornerRadiusDp} dp",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = glassEnv.textPrimary
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                            listOf(12, 16, 20, 24, 28).forEach { rDp ->
                                                val sel = settings.cardCornerRadiusDp == rDp
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(50))
                                                        .background(if (sel) glassEnv.pillSurfaceActive else glassEnv.pillSurfaceInactive)
                                                        .border(
                                                            1.dp,
                                                            if (sel) glassEnv.pillBorderActive else glassEnv.pillBorderInactive,
                                                            RoundedCornerShape(50)
                                                        )
                                                        .clickable { onUpdateStyleCustomization(null, rDp) }
                                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                                ) {
                                                    Text(
                                                        text = "${rDp}dp",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                        color = if (sel) glassEnv.textPrimary else glassEnv.textSecondary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    Slider(
                                        value = settings.cardCornerRadiusDp.toFloat(),
                                        onValueChange = { onUpdateStyleCustomization(null, it.roundToInt()) },
                                        valueRange = 10f..32f
                                    )

                                    // Card Opacity / Saturation & Border Intensity
                                    if (!settings.isLightMode) {
                                        Text(
                                            text = "Sytost matného povrchu karet: ${(settings.glassOpacity * 100).roundToInt()}%",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = glassEnv.textSecondary
                                        )
                                        Slider(
                                            value = settings.glassOpacity,
                                            onValueChange = { onUpdateVisualTheme(it, null, null, null, null, null, null, null) },
                                            valueRange = 0.65f..1.0f
                                        )

                                        Text(
                                            text = "Zvýraznění okrajů karet: ${(settings.specularBorderIntensity * 100).roundToInt()}%",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = glassEnv.textSecondary
                                        )
                                        Slider(
                                            value = settings.specularBorderIntensity,
                                            onValueChange = { onUpdateVisualTheme(null, it, null, null, null, null, null, null) },
                                            valueRange = 0.04f..0.45f
                                        )
                                    }
                                }
                            }
                        }

                        // Lightweight Mode & Chart Reference Lines Card
                        item {
                            FrostedBentoCard(
                                modifier = Modifier.fillMaxWidth(),
                                accentColor = Color(0xFF10B981)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    BentoMicroHeader(
                                        title = "VÝKON & LIGHTWEIGHT REŽIM (VYPNUTÍ EFEKTŮ)",
                                        icon = Icons.Default.Speed,
                                        accentColor = Color(0xFF10B981),
                                        trailingText = if (settings.isLightweightMode) "LIGHTWEIGHT AKTIVNÍ" else "STANDARDNÍ"
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Lightweight režim (Vypnout efekty a animace)",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = glassEnv.textPrimary
                                            )
                                            Text(
                                                text = "Čistý matný OLED povrch bez barevných aur pozadí a pulzujících animací pro maximální plynulost",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = glassEnv.textSecondary
                                            )
                                        }
                                        Switch(
                                            checked = settings.isLightweightMode,
                                            onCheckedChange = { lw ->
                                                onUpdateVisualTheme(null, null, null, null, null, null, lw, null)
                                            },
                                            modifier = Modifier.testTag("toggle_lightweight_mode_switch")
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Zobrazovat linie Min / Průměr / Max přímo v grafech",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = glassEnv.textPrimary
                                            )
                                            Text(
                                                text = "Vykreslí čárkovanou přímku průměru a body globálního minima a maxima v analytice",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = glassEnv.textSecondary
                                            )
                                        }
                                        Switch(
                                            checked = settings.showChartMinMaxAvgLines,
                                            onCheckedChange = { showLines ->
                                                onUpdateVisualTheme(null, null, null, null, null, null, null, showLines)
                                            }
                                        )
                                    }

                                    if (!settings.isLightweightMode && !settings.isLightMode) {
                                        Text(
                                            text = "Volitelné barevné podsvícení pozadí (0% = čistý matný OLED): ${(settings.ambientGlowIntensity * 100).roundToInt()}%",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = glassEnv.textSecondary
                                        )
                                        Slider(
                                            value = settings.ambientGlowIntensity,
                                            onValueChange = { onUpdateVisualTheme(null, null, it, null, null, null, null, null) },
                                            valueRange = 0f..1f
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // =========================================================================
                    // SUB-PAGE 2: VÝPOČETNÍ ENGINE & MATEMATICKÉ KONSTANTY
                    // =========================================================================
                    if (selectedSubPage == 1) {
                        item {
                            FrostedBentoCard(
                                modifier = Modifier.fillMaxWidth(),
                                accentColor = Color(0xFF8B5CF6)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    BentoMicroHeader(
                                        title = "GLOBÁLNÍ PARAMETRY VÝPOČTŮ, TOLERANCE & SUPERPOZICE",
                                        icon = Icons.Default.Calculate,
                                        accentColor = Color(0xFF8B5CF6),
                                        trailingText = "PLNOU KONTROLOU"
                                    )
                                    Text(
                                        text = "Všechny konstanty používané ve farmakokinetických a analytických výpočtech můžete níže libovolně upravit.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = glassEnv.textSecondary
                                    )

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = tolGainStr,
                                            onValueChange = {
                                                tolGainStr = it
                                                it.toDoubleOrNull()?.let { v ->
                                                    onUpdateGlobalMathConstants(v, null, null, null, null, null, null, null, null, null, null)
                                                }
                                            },
                                            label = { Text("Růst tolerance (výchozí 16.5)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = tolDecayStr,
                                            onValueChange = {
                                                tolDecayStr = it
                                                it.toDoubleOrNull()?.let { v ->
                                                    onUpdateGlobalMathConstants(null, v, null, null, null, null, null, null, null, null, null)
                                                }
                                            },
                                            label = { Text("Pokles tolerance/den (0.18)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = superposStr,
                                            onValueChange = {
                                                superposStr = it
                                                it.toDoubleOrNull()?.let { v ->
                                                    onUpdateGlobalMathConstants(null, null, v, null, null, null, null, null, null, null, null)
                                                }
                                            },
                                            label = { Text("Váha superpozice dávek (1.0)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = bodyHalfLifeMultStr,
                                            onValueChange = {
                                                bodyHalfLifeMultStr = it
                                                it.toDoubleOrNull()?.let { v ->
                                                    onUpdateGlobalMathConstants(null, null, null, null, null, null, null, null, null, null, v)
                                                }
                                            },
                                            label = { Text("Násobitel t½ v těle (4.5×)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = compulsiveHrsStr,
                                            onValueChange = {
                                                compulsiveHrsStr = it
                                                it.toDoubleOrNull()?.let { v ->
                                                    onUpdateGlobalMathConstants(null, null, null, null, null, null, null, v, null, null, null)
                                                }
                                            },
                                            label = { Text("Práh redosingu (hodiny, 2.5)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = breachPenStr,
                                            onValueChange = {
                                                breachPenStr = it
                                                it.toIntOrNull()?.let { v ->
                                                    onUpdateGlobalMathConstants(null, null, null, null, null, null, null, null, v, null, null)
                                                }
                                            },
                                            label = { Text("Penalizace limitu (-5 b.)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            modifier = Modifier.weight(0.8f)
                                        )
                                        OutlinedTextField(
                                            value = redlinePenStr,
                                            onValueChange = {
                                                redlinePenStr = it
                                                it.toIntOrNull()?.let { v ->
                                                    onUpdateGlobalMathConstants(null, null, null, null, null, null, null, null, null, v, null)
                                                }
                                            },
                                            label = { Text("Penalizace Redline (-6)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            modifier = Modifier.weight(0.8f)
                                        )
                                    }

                                    // Simple Timing Phase Ratios
                                    Text(
                                        text = "Poměry fází při jednoduchém zadání hlavní doby účinku: Nástup ${(settings.simpleOnsetRatio * 100).roundToInt()}% • Náběh ${(settings.simpleComeupRatio * 100).roundToInt()}% • Špička ${(settings.simplePeakRatio * 100).roundToInt()}% • Dojezd ${(settings.simpleOffsetRatio * 100).roundToInt()}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = glassEnv.textSecondary
                                    )

                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF8B5CF6).copy(alpha = 0.20f))
                                            .border(1.dp, Color(0xFF8B5CF6), RoundedCornerShape(8.dp))
                                            .clickable(onClick = onResetMathConstants)
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Obnovit výchozí matematické konstanty",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = glassEnv.textPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // =========================================================================
                    // SUB-PAGE 3: JEDNOTKY, MĚNA, SPÁNEK & CESTY PODÁNÍ (RoA)
                    // =========================================================================
                    if (selectedSubPage == 2) {
                        item {
                            FrostedBentoCard(
                                modifier = Modifier.fillMaxWidth(),
                                accentColor = Color(0xFFF59E0B)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    BentoMicroHeader(
                                        title = "MĚNA, VEČERKA PRO SPÁNEK & VLASTNÍ JEDNOTKY",
                                        icon = Icons.Default.Tune,
                                        accentColor = Color(0xFFF59E0B)
                                    )

                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf(
                                            Triple("Kč", false, "Kč (CZK)"),
                                            Triple("€", true, "€ (EUR)"),
                                            Triple("$", true, "$ (USD)"),
                                            Triple("£", true, "£ (GBP)")
                                        ).forEach { (sym, pref, label) ->
                                            val sel = settings.currencySymbol == sym
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (sel) Color(0xFFF59E0B).copy(alpha = 0.24f) else glassEnv.cardBorderBase.copy(alpha = 0.25f))
                                                    .border(1.dp, if (sel) Color(0xFFF59E0B) else glassEnv.cardBorderBase, RoundedCornerShape(8.dp))
                                                    .clickable { onUpdateCurrency(sym, pref, settings.decimalPrecision) }
                                                    .padding(vertical = 6.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp), color = glassEnv.textPrimary)
                                            }
                                        }
                                    }

                                    Text(
                                        text = "Hodina večerky pro výpočet spánkové hygieny: ${settings.bedtimeHour24}:00",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = glassEnv.textPrimary
                                    )
                                    Slider(
                                        value = settings.bedtimeHour24.toFloat(),
                                        onValueChange = { onUpdateBedtime(it.roundToInt()) },
                                        valueRange = 20f..24f,
                                        steps = 3
                                    )

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        settings.parsedUnits().forEach { u ->
                                            Row(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(glassEnv.cardBorderBase.copy(alpha = 0.30f))
                                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(u, style = MaterialTheme.typography.labelSmall, color = glassEnv.textPrimary)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "×",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFFEF4444),
                                                    modifier = Modifier.clickable { onRemoveCustomUnit(u) }
                                                )
                                            }
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = newUnitText,
                                            onValueChange = { newUnitText = it },
                                            label = { Text("Nová jednotka (např. kapky, tbl)") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFFF59E0B).copy(alpha = 0.25f))
                                                .border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(8.dp))
                                                .clickable {
                                                    if (newUnitText.isNotBlank()) {
                                                        onAddCustomUnit(newUnitText)
                                                        newUnitText = ""
                                                    }
                                                }
                                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                        ) {
                                            Text("+ Jednotka", style = MaterialTheme.typography.labelSmall, color = glassEnv.textPrimary)
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = newTagText,
                                            onValueChange = { newTagText = it },
                                            label = { Text("Nový kontextový štítek (např. Sport)") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFF38BDF8).copy(alpha = 0.25f))
                                                .border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(8.dp))
                                                .clickable {
                                                    if (newTagText.isNotBlank()) {
                                                        onAddContextTag(newTagText)
                                                        newTagText = ""
                                                    }
                                                }
                                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                        ) {
                                            Text("+ Štítek", style = MaterialTheme.typography.labelSmall, color = glassEnv.textPrimary)
                                        }
                                    }
                                }
                            }
                        }

                        // Routes of Administration (RoA) CRUD Card
                        item {
                            FrostedBentoCard(
                                modifier = Modifier.fillMaxWidth(),
                                accentColor = Color(0xFF10B981)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    BentoMicroHeader(
                                        title = "CESTY PODÁNÍ (RoA) & BIOLOGICKÁ DOSTUPNOST",
                                        icon = Icons.Default.Science,
                                        accentColor = Color(0xFF10B981)
                                    )

                                    routesOfAdmin.forEach { roa ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(glassEnv.cardBorderBase.copy(alpha = 0.22f))
                                                .padding(horizontal = 8.dp, vertical = 5.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${roa.name} • Síla ${roa.bioavailabilityFactor}× • Nástup ${roa.onsetMultiplier}× • Doba ${roa.durationMultiplier}×",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = glassEnv.textPrimary
                                            )
                                            if (!roa.isDefault) {
                                                IconButton(
                                                    onClick = { onDeleteRoa(roa.id) },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Delete RoA", tint = Color(0xFFEF4444), modifier = Modifier.size(14.dp))
                                                }
                                            }
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        OutlinedTextField(
                                            value = newRoaName,
                                            onValueChange = { newRoaName = it },
                                            label = { Text("Název RoA") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1.2f)
                                        )
                                        OutlinedTextField(
                                            value = newRoaBioStr,
                                            onValueChange = { newRoaBioStr = it },
                                            label = { Text("Síla ×") },
                                            singleLine = true,
                                            modifier = Modifier.weight(0.7f)
                                        )
                                        OutlinedTextField(
                                            value = newRoaOnsetStr,
                                            onValueChange = { newRoaOnsetStr = it },
                                            label = { Text("Nástup ×") },
                                            singleLine = true,
                                            modifier = Modifier.weight(0.7f)
                                        )
                                        OutlinedTextField(
                                            value = newRoaDurStr,
                                            onValueChange = { newRoaDurStr = it },
                                            label = { Text("Doba ×") },
                                            singleLine = true,
                                            modifier = Modifier.weight(0.7f)
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF10B981).copy(alpha = 0.25f))
                                            .border(1.dp, Color(0xFF10B981), RoundedCornerShape(8.dp))
                                            .clickable {
                                                if (newRoaName.isNotBlank()) {
                                                    val bio = (newRoaBioStr.toDoubleOrNull() ?: 1.0).coerceIn(0.2, 3.0)
                                                    val ons = (newRoaOnsetStr.toDoubleOrNull() ?: 1.0).coerceIn(0.2, 3.0)
                                                    val dur = (newRoaDurStr.toDoubleOrNull() ?: 1.0).coerceIn(0.2, 3.0)
                                                    onSaveRoa(
                                                        RouteOfAdminEntity(
                                                            name = newRoaName.trim(),
                                                            onsetMultiplier = ons,
                                                            durationMultiplier = dur,
                                                            bioavailabilityFactor = bio
                                                        )
                                                    )
                                                    newRoaName = ""
                                                }
                                            }
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text("+ Přidat cestu podání", style = MaterialTheme.typography.labelSmall, color = glassEnv.textPrimary)
                                    }
                                }
                            }
                        }
                    }

                    // =========================================================================
                    // SUB-PAGE 5: DATA, ZÁLOHY & DEMO DATASET
                    // =========================================================================
                    if (selectedSubPage == 4) {
                        item {
                            FrostedBentoCard(
                                modifier = Modifier.fillMaxWidth(),
                                accentColor = Color(0xFF8B5CF6)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    BentoMicroHeader(
                                        title = "30DENNÍ REALISTICKÁ DEMO DATA (VČETNĚ DRUHŮ)",
                                        icon = Icons.Default.AutoAwesome,
                                        accentColor = Color(0xFF8B5CF6),
                                        trailingText = "${allLogs.size} ZÁZNAMŮ V DB"
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color(0xFF10B981).copy(alpha = 0.22f))
                                                .border(1.dp, Color(0xFF10B981), RoundedCornerShape(10.dp))
                                                .clickable(onClick = onReloadDemoDataset)
                                                .padding(vertical = 10.dp)
                                                .testTag("reload_demo_dataset_button"),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text("Nahrát 30D Demo Data", style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp), color = glassEnv.textPrimary)
                                        }

                                        Row(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color(0xFFEF4444).copy(alpha = 0.20f))
                                                .border(1.dp, Color(0xFFEF4444), RoundedCornerShape(10.dp))
                                                .clickable(onClick = onClearSampleLogs)
                                                .padding(vertical = 10.dp)
                                                .testTag("clear_sample_data_button"),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text("Vymazat záznamy", style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp), color = glassEnv.textPrimary)
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            FrostedBentoCard(
                                modifier = Modifier.fillMaxWidth(),
                                accentColor = Color(0xFF38BDF8)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    BentoMicroHeader(
                                        title = "EXPORT & IMPORT DATABÁZE (JSON / CSV)",
                                        icon = Icons.Default.Code,
                                        accentColor = Color(0xFF38BDF8)
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf(
                                            "Export JSON" to onExportJson,
                                            "Export CSV" to onExportCsv,
                                            "Import JSON" to { showImportDialog = true }
                                        ).forEach { (lbl, action) ->
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(9.dp))
                                                    .background(Color(0xFF38BDF8).copy(alpha = 0.16f))
                                                    .border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(9.dp))
                                                    .clickable(onClick = action)
                                                    .padding(vertical = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(lbl, style = MaterialTheme.typography.labelLarge.copy(fontSize = 10.5.sp), color = glassEnv.textPrimary)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (exportedDataPreview != null) {
        AlertDialog(
            onDismissRequest = onDismissExportPreview,
            title = { Text("Exportovaná data") },
            text = {
                OutlinedTextField(
                    value = exportedDataPreview.take(4000),
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = onDismissExportPreview) {
                    Text("Zavřít")
                }
            }
        )
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import JSON Zálohy") },
            text = {
                OutlinedTextField(
                    value = importJsonDraft,
                    onValueChange = { importJsonDraft = it },
                    label = { Text("Vložte JSON obsah") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onImportJson(importJsonDraft)
                        showImportDialog = false
                    }
                ) {
                    Text("Importovat")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Zrušit")
                }
            }
        )
    }
}
