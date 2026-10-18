package com.antigravity.substance.ui.substances

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.antigravity.substance.data.db.entities.CustomFieldDefEntity
import com.antigravity.substance.data.db.entities.SubstanceEntity
import com.antigravity.substance.data.db.entities.SubstanceSubtypeEntity
import com.antigravity.substance.domain.engine.PharmacokineticEngine
import com.antigravity.substance.ui.theme.FrostedBentoCard
import com.antigravity.substance.ui.theme.LocalSubstanceGlass
import com.antigravity.substance.ui.theme.SubstanceIcons
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Full-screen interactive Substance Editor organized into 5 Clean Sub-Pages:
 * 0 = 1. Základ & Cena (Identity, Icon, Color, Unit, Stepper, Package Price Calculator)
 * 1 = 2. Druhy / Varianty (Subtypes CRUD with custom potency, cost, onset/duration multipliers)
 * 2 = 3. Hranice dávek (5-Tier Dose Thresholds, Safe Daily Limit, Recommended Min Interval)
 * 3 = 4. Doba účinku & Doba v těle (Optional Timing: None / Simple / Detailed + Separate Optional Body Presence Duration)
 * 4 = 5. Koeficienty & Vlastní pole (Per-Substance Tolerance Gain/Decay, Bio-Load Weight, Superposition Weight + Custom Fields)
 * 5 = Vše (Show all sections)
 */
@Composable
fun SubstanceEditorSheet(
    initialSubstance: SubstanceEntity,
    subtypesForSubstance: List<SubstanceSubtypeEntity> = emptyList(),
    customFieldsForSubstance: List<CustomFieldDefEntity>,
    availableUnits: List<String>,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (SubstanceEntity) -> Unit,
    onSaveSubtype: (SubstanceSubtypeEntity) -> Unit = {},
    onDeleteSubtype: (Long) -> Unit = {},
    onSaveCustomField: (CustomFieldDefEntity) -> Unit,
    onDeleteCustomField: (Long) -> Unit
) {
    val glassEnv = LocalSubstanceGlass.current
    var selectedTab by remember { mutableIntStateOf(0) }

    // 1. Identity & Pricing
    var name by remember { mutableStateOf(initialSubstance.name) }
    var subtitle by remember { mutableStateOf(initialSubstance.subtitle) }
    var category by remember { mutableStateOf(initialSubstance.category) }
    var iconName by remember { mutableStateOf(initialSubstance.iconName) }
    var colorHex by remember { mutableLongStateOf(initialSubstance.colorHex) }
    var defaultUnit by remember { mutableStateOf(initialSubstance.defaultUnit) }
    var stepSizeStr by remember { mutableStateOf(initialSubstance.stepSize.toString()) }
    var defaultDoseStr by remember { mutableStateOf(initialSubstance.defaultDose.toString()) }
    var presetsCsv by remember { mutableStateOf(initialSubstance.quickPresetsCsv) }
    var costPerUnitStr by remember { mutableStateOf(initialSubstance.costPerUnit.toString()) }

    var pkgQuantityStr by remember { mutableStateOf("250") }
    var pkgTotalCostStr by remember { mutableStateOf("650") }

    // 2. New Subtype Creator State
    var newSubtypeName by remember { mutableStateOf("") }
    var newSubtypePotencyStr by remember { mutableStateOf("1.0") }
    var newSubtypeCostStr by remember { mutableStateOf("") }
    var newSubtypeOnsetStr by remember { mutableStateOf("1.0") }
    var newSubtypeDurationStr by remember { mutableStateOf("1.0") }
    var newSubtypeColorHex by remember { mutableLongStateOf(initialSubstance.colorHex) }

    // 3. 5-Tier Thresholds & Intervals
    var thNegStr by remember { mutableStateOf(initialSubstance.thresholdNegligible.toString()) }
    var thLightStr by remember { mutableStateOf(initialSubstance.thresholdLight.toString()) }
    var thMedStr by remember { mutableStateOf(initialSubstance.thresholdMedium.toString()) }
    var thStrongStr by remember { mutableStateOf(initialSubstance.thresholdStrong.toString()) }
    var thDangerStr by remember { mutableStateOf(initialSubstance.thresholdDangerous.toString()) }
    var dailyMaxStr by remember { mutableStateOf(initialSubstance.dailyMaxSafeLimit.toString()) }
    var minIntervalMinsStr by remember { mutableStateOf(initialSubstance.recommendedMinIntervalMinutes.toString()) }
    var sleepThresholdStr by remember { mutableStateOf(initialSubstance.sleepDisruptionThreshold.toString()) }

    // 4. Active Effect Duration vs Optional Body Presence Duration
    var timingMode by remember { mutableStateOf(initialSubstance.timingMode) } // SIMPLE, DETAILED, NONE
    var mainEffectDurationMins by remember { mutableFloatStateOf(initialSubstance.mainEffectDurationMinutes.toFloat()) }
    var onsetMins by remember { mutableFloatStateOf(initialSubstance.onsetMinutes.toFloat()) }
    var comeupMins by remember { mutableFloatStateOf(initialSubstance.comeupMinutes.toFloat()) }
    var peakMins by remember { mutableFloatStateOf(initialSubstance.peakMinutes.toFloat()) }
    var offsetMins by remember { mutableFloatStateOf(initialSubstance.offsetMinutes.toFloat()) }
    var hasBodyPresence by remember { mutableStateOf(initialSubstance.hasBodyPresenceDuration) }
    var bodyPresenceHrs by remember { mutableFloatStateOf(initialSubstance.bodyPresenceHours.toFloat()) }
    var halfLifeHrs by remember { mutableFloatStateOf(initialSubstance.halfLifeHours.toFloat()) }
    var stackingBehavior by remember { mutableStateOf(initialSubstance.stackingBehavior) }

    // 5. Computational Coefficients & Custom Fields
    var tolGainRateStr by remember { mutableStateOf(initialSubstance.toleranceGainRate.toString()) }
    var tolDecayRateStr by remember { mutableStateOf(initialSubstance.toleranceDecayRate.toString()) }
    var bioLoadWeightStr by remember { mutableStateOf(initialSubstance.bioLoadWeight.toString()) }
    var superpositionWeightStr by remember { mutableStateOf(initialSubstance.superpositionWeight.toString()) }

    var newFieldName by remember { mutableStateOf("") }
    var newFieldType by remember { mutableStateOf("SINGLE_CHOICE_CHIPS") }
    var newFieldOptions by remember { mutableStateOf("Option A,Option B,Option C") }

    val accentColor = Color(colorHex)

    val showTab0 = selectedTab == 0 || selectedTab == 5
    val showTab1 = selectedTab == 1 || selectedTab == 5
    val showTab2 = selectedTab == 2 || selectedTab == 5
    val showTab3 = selectedTab == 3 || selectedTab == 5
    val showTab4 = selectedTab == 4 || selectedTab == 5

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .testTag("substance_editor_dialog"),
            color = glassEnv.canvasBackgroundTop
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(glassEnv.cardSurfaceBase)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = glassEnv.textPrimary)
                        }
                        Column {
                            Text(
                                text = if (initialSubstance.id == 0L) "Nová látka" else "Upravit: ${initialSubstance.name}",
                                style = MaterialTheme.typography.titleLarge,
                                color = glassEnv.textPrimary
                            )
                            Text(
                                text = "5 podstránek nastavení látky, druhů a výpočtů",
                                style = MaterialTheme.typography.labelSmall,
                                color = glassEnv.textSecondary
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(accentColor)
                            .clickable {
                                val tNeg = thNegStr.toDoubleOrNull()?.coerceAtLeast(0.01) ?: 1.0
                                val tLight = max(tNeg + 0.1, thLightStr.toDoubleOrNull() ?: (tNeg * 2))
                                val tMed = max(tLight + 0.1, thMedStr.toDoubleOrNull() ?: (tLight * 1.8))
                                val tStrong = max(tMed + 0.1, thStrongStr.toDoubleOrNull() ?: (tMed * 1.5))
                                val tDanger = max(tStrong + 0.1, thDangerStr.toDoubleOrNull() ?: (tStrong * 1.4))

                                val effMainMins = mainEffectDurationMins.roundToInt().coerceAtLeast(15)
                                val effOnset = if (timingMode == "SIMPLE") (effMainMins * 0.10).roundToInt().coerceAtLeast(1) else onsetMins.roundToInt().coerceAtLeast(1)
                                val effComeup = if (timingMode == "SIMPLE") (effMainMins * 0.18).roundToInt().coerceAtLeast(1) else comeupMins.roundToInt().coerceAtLeast(1)
                                val effPeak = if (timingMode == "SIMPLE") (effMainMins * 0.37).roundToInt().coerceAtLeast(1) else peakMins.roundToInt().coerceAtLeast(1)
                                val effOffset = if (timingMode == "SIMPLE") (effMainMins * 0.35).roundToInt().coerceAtLeast(1) else offsetMins.roundToInt().coerceAtLeast(1)

                                onSave(
                                    initialSubstance.copy(
                                        name = name.ifBlank { "Substance" },
                                        subtitle = subtitle,
                                        category = category,
                                        iconName = iconName,
                                        colorHex = colorHex,
                                        defaultUnit = defaultUnit.ifBlank { "mg" },
                                        stepSize = (stepSizeStr.toDoubleOrNull() ?: 1.0).coerceAtLeast(0.05),
                                        defaultDose = (defaultDoseStr.toDoubleOrNull() ?: 10.0).coerceAtLeast(0.05),
                                        quickPresetsCsv = presetsCsv,
                                        costPerUnit = (costPerUnitStr.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0),
                                        thresholdNegligible = tNeg,
                                        thresholdLight = tLight,
                                        thresholdMedium = tMed,
                                        thresholdStrong = tStrong,
                                        thresholdDangerous = tDanger,
                                        dailyMaxSafeLimit = (dailyMaxStr.toDoubleOrNull() ?: tStrong).coerceAtLeast(0.1),
                                        recommendedMinIntervalMinutes = (minIntervalMinsStr.toIntOrNull() ?: 180).coerceAtLeast(5),
                                        sleepDisruptionThreshold = (sleepThresholdStr.toDoubleOrNull() ?: tLight).coerceAtLeast(0.0),
                                        timingMode = timingMode,
                                        mainEffectDurationMinutes = effMainMins,
                                        onsetMinutes = effOnset,
                                        comeupMinutes = effComeup,
                                        peakMinutes = effPeak,
                                        offsetMinutes = effOffset,
                                        hasBodyPresenceDuration = hasBodyPresence,
                                        bodyPresenceHours = ((bodyPresenceHrs * 10).roundToInt() / 10.0).coerceAtLeast(0.5),
                                        halfLifeHours = ((halfLifeHrs * 10).roundToInt() / 10.0).coerceAtLeast(0.5),
                                        stackingBehavior = stackingBehavior,
                                        toleranceGainRate = (tolGainRateStr.toDoubleOrNull() ?: 1.0).coerceIn(0.1, 5.0),
                                        toleranceDecayRate = (tolDecayRateStr.toDoubleOrNull() ?: 1.0).coerceIn(0.1, 5.0),
                                        bioLoadWeight = (bioLoadWeightStr.toDoubleOrNull() ?: 1.0).coerceIn(0.1, 5.0),
                                        superpositionWeight = (superpositionWeightStr.toDoubleOrNull() ?: 1.0).coerceIn(0.1, 3.0)
                                    )
                                )
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .testTag("save_substance_editor_button"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "ULOŽIT",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.Black
                        )
                    }
                }

                // 5 Sub-Pages Navigation Strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(glassEnv.cardSurfaceBase)
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val tabs = listOf(
                        0 to "1. Základ & Cena",
                        1 to "2. Druhy (${subtypesForSubstance.size})",
                        2 to "3. Hranice dávek",
                        3 to "4. Účinek & Tělo",
                        4 to "5. Koeficienty & Pole",
                        5 to "Vše"
                    )
                    tabs.forEach { (idx, lbl) ->
                        val active = selectedTab == idx
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (active) accentColor.copy(alpha = 0.22f) else Color.Transparent)
                                .border(1.dp, if (active) accentColor else glassEnv.cardBorderBase, RoundedCornerShape(8.dp))
                                .clickable { selectedTab = idx }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                                .testTag("substance_editor_tab_$idx")
                        ) {
                            Text(
                                text = lbl,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (active) accentColor else glassEnv.textSecondary
                            )
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // =========================================================================
                    // SUB-PAGE 1: ZÁKLADNÍ INFO, VZHLED & KALKULAČKA CENY
                    // =========================================================================
                    if (showTab0) {
                        item {
                            FrostedBentoCard(accentColor = accentColor, modifier = Modifier.fillMaxWidth()) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("1. IDENTITA, IKONA & BARVA LÁTKY", style = MaterialTheme.typography.labelLarge, color = accentColor)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = name,
                                            onValueChange = { name = it },
                                            label = { Text("Název látky") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = category,
                                            onValueChange = { category = it },
                                            label = { Text("Kategorie") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    OutlinedTextField(
                                        value = subtitle,
                                        onValueChange = { subtitle = it },
                                        label = { Text("Popis / Třída látky") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        SubstanceIcons.presetColorsHex.forEach { hex ->
                                            val selected = hex == colorHex
                                            Box(
                                                modifier = Modifier
                                                    .size(30.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(hex))
                                                    .border(
                                                        width = if (selected) 2.5.dp else 0.dp,
                                                        color = if (selected) glassEnv.textPrimary else Color.Transparent,
                                                        shape = CircleShape
                                                    )
                                                    .clickable { colorHex = hex }
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        SubstanceIcons.allIconOptions.forEach { (iKey, iVec) ->
                                            val selected = iKey == iconName
                                            Box(
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .clip(RoundedCornerShape(9.dp))
                                                    .background(if (selected) accentColor.copy(alpha = 0.25f) else glassEnv.cardBorderBase.copy(alpha = 0.25f))
                                                    .border(1.dp, if (selected) accentColor else Color.Transparent, RoundedCornerShape(9.dp))
                                                    .clickable { iconName = iKey },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(iVec, contentDescription = iKey, tint = if (selected) accentColor else glassEnv.textPrimary, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            FrostedBentoCard(accentColor = accentColor, modifier = Modifier.fillMaxWidth()) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("JEDNOTKA, KROK (+/-) & KALKULAČKA CENY BALENÍ", style = MaterialTheme.typography.labelLarge, color = accentColor)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        availableUnits.forEach { u ->
                                            val selected = u == defaultUnit
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (selected) accentColor.copy(alpha = 0.25f) else glassEnv.cardBorderBase.copy(alpha = 0.25f))
                                                    .border(1.dp, if (selected) accentColor else glassEnv.cardBorderBase, RoundedCornerShape(8.dp))
                                                    .clickable { defaultUnit = u }
                                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                                            ) {
                                                Text(u, style = MaterialTheme.typography.labelLarge, color = glassEnv.textPrimary)
                                            }
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = stepSizeStr,
                                            onValueChange = { stepSizeStr = it },
                                            label = { Text("Krok +/- ($defaultUnit)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = defaultDoseStr,
                                            onValueChange = { defaultDoseStr = it },
                                            label = { Text("Výchozí dávka") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = costPerUnitStr,
                                            onValueChange = { costPerUnitStr = it },
                                            label = { Text("$currencySymbol / $defaultUnit") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    OutlinedTextField(
                                        value = presetsCsv,
                                        onValueChange = { presetsCsv = it },
                                        label = { Text("Rychlé předvolby CSV (např. 1.5, 2.5, 3.5, 5.0)") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(glassEnv.cardBorderBase.copy(alpha = 0.25f))
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(Icons.Default.Calculate, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                                        OutlinedTextField(
                                            value = pkgQuantityStr,
                                            onValueChange = { pkgQuantityStr = it },
                                            label = { Text("Balení ($defaultUnit)") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = pkgTotalCostStr,
                                            onValueChange = { pkgTotalCostStr = it },
                                            label = { Text("Cena ($currencySymbol)") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFF10B981).copy(alpha = 0.25f))
                                                .border(1.dp, Color(0xFF10B981), RoundedCornerShape(8.dp))
                                                .clickable {
                                                    val q = pkgQuantityStr.toDoubleOrNull() ?: 1.0
                                                    val p = pkgTotalCostStr.toDoubleOrNull() ?: 0.0
                                                    if (q > 0.0) {
                                                        val unitCost = ((p / q) * 1000.0).roundToInt() / 1000.0
                                                        costPerUnitStr = unitCost.toString()
                                                    }
                                                }
                                                .padding(horizontal = 8.dp, vertical = 10.dp)
                                        ) {
                                            Text("Vypočítat", style = MaterialTheme.typography.labelSmall, color = glassEnv.textPrimary)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // =========================================================================
                    // SUB-PAGE 2: DRUHY / PODDRUHY LÁTKY (SUBTYPES CRUD)
                    // =========================================================================
                    if (showTab1) {
                        item {
                            FrostedBentoCard(accentColor = accentColor, modifier = Modifier.fillMaxWidth()) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("2. DRUHY / VARIANTY TÉTO LÁTKY (SUBTYPES)", style = MaterialTheme.typography.labelLarge, color = accentColor)
                                    Text(
                                        text = "Každý druh (např. Espresso vs V60 Filtr, Green vs Red) má vlastní sílu (potenci), volitelnou cenu a samostatné grafy v analytice.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = glassEnv.textSecondary
                                    )

                                    if (initialSubstance.id == 0L) {
                                        Text(
                                            text = "Nejprve uložte novou látku a poté k ní přidejte poddruhy.",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = Color(0xFFF59E0B)
                                        )
                                    } else {
                                        subtypesForSubstance.forEach { st ->
                                            val stCol = Color(st.colorHex)
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(stCol.copy(alpha = 0.10f))
                                                    .border(1.dp, stCol.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "${st.name} (Síla ${String.format(Locale.US, "%.2fx", st.potencyMultiplier)})",
                                                        style = MaterialTheme.typography.labelLarge,
                                                        color = glassEnv.textPrimary
                                                    )
                                                    val customPriceStr = st.customCostPerUnit?.let { "$it $currencySymbol/$defaultUnit" } ?: "Výchozí cena látky"
                                                    Text(
                                                        text = "Nástup ${st.onsetMultiplier}× • Trvání ${st.durationMultiplier}× • $customPriceStr",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = glassEnv.textSecondary
                                                    )
                                                }
                                                IconButton(onClick = { onDeleteSubtype(st.id) }) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Delete subtype", tint = Color(0xFFEF4444))
                                                }
                                            }
                                        }

                                        // Add New Subtype Form
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedTextField(
                                                value = newSubtypeName,
                                                onValueChange = { newSubtypeName = it },
                                                label = { Text("Název druhu (např. Green Maeng Da)") },
                                                singleLine = true,
                                                modifier = Modifier.weight(1.4f)
                                            )
                                            OutlinedTextField(
                                                value = newSubtypePotencyStr,
                                                onValueChange = { newSubtypePotencyStr = it },
                                                label = { Text("Síla (např. 1.2)") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                singleLine = true,
                                                modifier = Modifier.weight(0.9f)
                                            )
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedTextField(
                                                value = newSubtypeCostStr,
                                                onValueChange = { newSubtypeCostStr = it },
                                                label = { Text("Vlastní cena/$defaultUnit (volit.)") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                singleLine = true,
                                                modifier = Modifier.weight(1f)
                                            )
                                            OutlinedTextField(
                                                value = newSubtypeOnsetStr,
                                                onValueChange = { newSubtypeOnsetStr = it },
                                                label = { Text("Nástup ×") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                singleLine = true,
                                                modifier = Modifier.weight(0.7f)
                                            )
                                            OutlinedTextField(
                                                value = newSubtypeDurationStr,
                                                onValueChange = { newSubtypeDurationStr = it },
                                                label = { Text("Doba ×") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                singleLine = true,
                                                modifier = Modifier.weight(0.7f)
                                            )
                                        }
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            SubstanceIcons.presetColorsHex.forEach { hex ->
                                                val sel = hex == newSubtypeColorHex
                                                Box(
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(hex))
                                                        .border(if (sel) 2.dp else 0.dp, glassEnv.textPrimary, CircleShape)
                                                        .clickable { newSubtypeColorHex = hex }
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Row(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(accentColor.copy(alpha = 0.25f))
                                                    .border(1.dp, accentColor, RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        if (newSubtypeName.isNotBlank()) {
                                                            onSaveSubtype(
                                                                SubstanceSubtypeEntity(
                                                                    substanceId = initialSubstance.id,
                                                                    name = newSubtypeName.trim(),
                                                                    colorHex = newSubtypeColorHex,
                                                                    potencyMultiplier = (newSubtypePotencyStr.toDoubleOrNull() ?: 1.0).coerceIn(0.1, 10.0),
                                                                    onsetMultiplier = (newSubtypeOnsetStr.toDoubleOrNull() ?: 1.0).coerceIn(0.2, 5.0),
                                                                    durationMultiplier = (newSubtypeDurationStr.toDoubleOrNull() ?: 1.0).coerceIn(0.2, 5.0),
                                                                    customCostPerUnit = newSubtypeCostStr.toDoubleOrNull(),
                                                                    isDefault = subtypesForSubstance.isEmpty()
                                                                )
                                                            )
                                                            newSubtypeName = ""
                                                        }
                                                    }
                                                    .padding(horizontal = 12.dp, vertical = 7.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Přidat druh", style = MaterialTheme.typography.labelLarge, color = glassEnv.textPrimary)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // =========================================================================
                    // SUB-PAGE 3: HRANICE DÁVEK (5-TIER THRESHOLDS & INTERVALS)
                    // =========================================================================
                    if (showTab2) {
                        item {
                            FrostedBentoCard(accentColor = accentColor, modifier = Modifier.fillMaxWidth()) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("3. HRANICE 5 ZÓN DÁVKOVÁNÍ & BEZPEČNÉ LIMITY", style = MaterialTheme.typography.labelLarge, color = accentColor)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = thNegStr,
                                            onValueChange = { thNegStr = it },
                                            label = { Text("Zanedbatelná ≤ ($defaultUnit)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = thLightStr,
                                            onValueChange = { thLightStr = it },
                                            label = { Text("Malá ≤ ($defaultUnit)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = thMedStr,
                                            onValueChange = { thMedStr = it },
                                            label = { Text("Střední ≤ ($defaultUnit)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = thStrongStr,
                                            onValueChange = { thStrongStr = it },
                                            label = { Text("Velká ≤ ($defaultUnit)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = thDangerStr,
                                            onValueChange = { thDangerStr = it },
                                            label = { Text("Nebezpečná ≥") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = dailyMaxStr,
                                            onValueChange = { dailyMaxStr = it },
                                            label = { Text("Denní max limit") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = minIntervalMinsStr,
                                            onValueChange = { minIntervalMinsStr = it },
                                            label = { Text("Doporučený min. rozestup (minuty)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = sleepThresholdStr,
                                            onValueChange = { sleepThresholdStr = it },
                                            label = { Text("Práh narušení spánku ($defaultUnit)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // =========================================================================
                    // SUB-PAGE 4: HLAVNÍ DOBA ÚČINKU VS VOLITELNÁ DOBA V TĚLE
                    // =========================================================================
                    if (showTab3) {
                        item {
                            FrostedBentoCard(accentColor = accentColor, modifier = Modifier.fillMaxWidth()) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("4. HLAVNÍ DOBA ÚČINKU VS. DOBA SETRVÁNÍ V TĚLE (NEPOVINNÉ)", style = MaterialTheme.typography.labelLarge, color = accentColor)
                                    Text(
                                        text = "Zvolte, zda chcete zadávat pouze hlavní dobu účinku, detailní 4 fáze, nebo časování u této látky zcela vypnout.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = glassEnv.textSecondary
                                    )

                                    // 3-Mode Timing Selector: SIMPLE, DETAILED, NONE
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf(
                                            "SIMPLE" to "Jen doba účinku",
                                            "DETAILED" to "Detailní 4 fáze",
                                            "NONE" to "Vypnuto (Nepovinné)"
                                        ).forEach { (mKey, mLabel) ->
                                            val sel = timingMode == mKey
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (sel) accentColor.copy(alpha = 0.24f) else glassEnv.cardBorderBase.copy(alpha = 0.25f))
                                                    .border(1.dp, if (sel) accentColor else glassEnv.cardBorderBase, RoundedCornerShape(8.dp))
                                                    .clickable { timingMode = mKey }
                                                    .padding(vertical = 7.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = mLabel,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontSize = 9.5.sp,
                                                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium
                                                    ),
                                                    color = if (sel) accentColor else glassEnv.textPrimary
                                                )
                                            }
                                        }
                                    }

                                    if (timingMode != "NONE") {
                                        // Live Dual Curve Preview: Active Effect (solid) vs Body Presence (dashed)
                                        val previewOnset = if (timingMode == "SIMPLE") mainEffectDurationMins * 0.10f else onsetMins
                                        val previewComeup = if (timingMode == "SIMPLE") mainEffectDurationMins * 0.18f else comeupMins
                                        val previewPeak = if (timingMode == "SIMPLE") mainEffectDurationMins * 0.37f else peakMins
                                        val previewOffset = if (timingMode == "SIMPLE") mainEffectDurationMins * 0.35f else offsetMins
                                        val totalEffectM = (previewOnset + previewComeup + previewPeak + previewOffset).coerceAtLeast(15f)
                                        val totalHorizonM = if (hasBodyPresence) max(totalEffectM, bodyPresenceHrs * 60f) else totalEffectM

                                        Canvas(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(74.dp)
                                        ) {
                                            val w = size.width
                                            val h = size.height
                                            val activePath = Path()
                                            val bodyPath = Path()
                                            val steps = 80
                                            val tempSub = initialSubstance.copy(
                                                timingMode = timingMode,
                                                mainEffectDurationMinutes = mainEffectDurationMins.roundToInt(),
                                                onsetMinutes = previewOnset.roundToInt(),
                                                comeupMinutes = previewComeup.roundToInt(),
                                                peakMinutes = previewPeak.roundToInt(),
                                                offsetMinutes = previewOffset.roundToInt(),
                                                hasBodyPresenceDuration = hasBodyPresence,
                                                bodyPresenceHours = bodyPresenceHrs.toDouble(),
                                                halfLifeHours = halfLifeHrs.toDouble()
                                            )
                                            for (i in 0..steps) {
                                                val dt = (i.toFloat() / steps) * totalHorizonM
                                                val eff = PharmacokineticEngine.singleDoseEffectAt(
                                                    deltaMinutes = dt.toDouble(),
                                                    effectiveDose = 1.0,
                                                    onsetMinutes = previewOnset.toDouble(),
                                                    comeupMinutes = previewComeup.toDouble(),
                                                    peakMinutes = previewPeak.toDouble(),
                                                    offsetMinutes = previewOffset.toDouble()
                                                )
                                                val px = (i.toFloat() / steps) * w
                                                val py = (h - eff.toFloat() * (h * 0.85f)).coerceIn(2f, h)
                                                if (i == 0) activePath.moveTo(px, py) else activePath.lineTo(px, py)

                                                if (hasBodyPresence) {
                                                    val bodyVal = PharmacokineticEngine.singleDosePlasmaAt(
                                                        deltaMinutes = dt.toDouble(),
                                                        effectiveDose = 1.0,
                                                        substance = tempSub
                                                    )
                                                    val by = (h - bodyVal.toFloat() * (h * 0.82f)).coerceIn(2f, h)
                                                    if (i == 0) bodyPath.moveTo(px, by) else bodyPath.lineTo(px, by)
                                                }
                                            }
                                            if (hasBodyPresence) {
                                                drawPath(
                                                    path = bodyPath,
                                                    color = Color(0xFF8B5CF6),
                                                    style = Stroke(
                                                        width = 1.8.dp.toPx(),
                                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f))
                                                    )
                                                )
                                            }
                                            drawPath(
                                                path = activePath,
                                                color = accentColor,
                                                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                                            )
                                        }

                                        if (timingMode == "SIMPLE") {
                                            Text(
                                                text = "HLAVNÍ DOBA ÚČINKU: ${mainEffectDurationMins.roundToInt()} min (${String.format(Locale.US, "%.1f", mainEffectDurationMins / 60f)} h)",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = accentColor
                                            )
                                            Slider(
                                                value = mainEffectDurationMins,
                                                onValueChange = { mainEffectDurationMins = it },
                                                valueRange = 30f..720f
                                            )
                                        } else {
                                            Text(
                                                text = "Hlavní doba účinku celkem: ${(onsetMins + comeupMins + peakMins + offsetMins).roundToInt()} min (${String.format(Locale.US, "%.1f", (onsetMins + comeupMins + peakMins + offsetMins) / 60f)} h)",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = accentColor
                                            )
                                            Text("Nástup účinku (Onset): ${onsetMins.roundToInt()} min", style = MaterialTheme.typography.labelSmall, color = glassEnv.textPrimary)
                                            Slider(value = onsetMins, onValueChange = { onsetMins = it }, valueRange = 1f..90f)

                                            Text("Náběh do špičky (Come-Up): ${comeupMins.roundToInt()} min", style = MaterialTheme.typography.labelSmall, color = glassEnv.textPrimary)
                                            Slider(value = comeupMins, onValueChange = { comeupMins = it }, valueRange = 5f..120f)

                                            Text("Plató špičky účinku (Peak): ${peakMins.roundToInt()} min", style = MaterialTheme.typography.labelSmall, color = glassEnv.textPrimary)
                                            Slider(value = peakMins, onValueChange = { peakMins = it }, valueRange = 10f..300f)

                                            Text("Dojezd účinku (Offset): ${offsetMins.roundToInt()} min", style = MaterialTheme.typography.labelSmall, color = glassEnv.textPrimary)
                                            Slider(value = offsetMins, onValueChange = { offsetMins = it }, valueRange = 20f..480f)
                                        }

                                        // Separate Optional Body Presence Duration Section
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Samostatná doba setrvání v těle (volitelné)",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = Color(0xFF8B5CF6)
                                                )
                                                Text(
                                                    text = "Sleduje, jak dlouho látka zůstává v organismu po odeznění hlavních účinků",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = glassEnv.textSecondary
                                                )
                                            }
                                            Switch(
                                                checked = hasBodyPresence,
                                                onCheckedChange = { hasBodyPresence = it }
                                            )
                                        }

                                        if (hasBodyPresence) {
                                            Text(
                                                text = "Celková doba v těle (Body Presence): ${String.format(Locale.US, "%.1f", bodyPresenceHrs)} hodin",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF8B5CF6)
                                            )
                                            Slider(
                                                value = bodyPresenceHrs,
                                                onValueChange = { bodyPresenceHrs = it },
                                                valueRange = 1f..72f
                                            )

                                            Text(
                                                text = "Biologický poločas rozpadu (t½): ${String.format(Locale.US, "%.1f", halfLifeHrs)} hodin",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = glassEnv.textPrimary
                                            )
                                            Slider(
                                                value = halfLifeHrs,
                                                onValueChange = { halfLifeHrs = it },
                                                valueRange = 0.5f..24f
                                            )
                                        }

                                        // Stacking Behavior Selector
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            listOf(
                                                "FULL_SUPERPOSITION" to "Plná superpozice",
                                                "DAMPED_STACKING" to "Tlumené vrstvení",
                                                "INDEPENDENT_TIMERS" to "Nezávislé dávky"
                                            ).forEach { (mode, lbl) ->
                                                val sel = stackingBehavior == mode
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(if (sel) accentColor.copy(alpha = 0.24f) else glassEnv.cardBorderBase.copy(alpha = 0.25f))
                                                        .border(1.dp, if (sel) accentColor else glassEnv.cardBorderBase, RoundedCornerShape(8.dp))
                                                        .clickable { stackingBehavior = mode }
                                                        .padding(vertical = 6.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(lbl, style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp), color = glassEnv.textPrimary)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // =========================================================================
                    // SUB-PAGE 5: VÝPOČETNÍ KOEFICIENTY LÁTKY & VLASTNÍ POLE
                    // =========================================================================
                    if (showTab4) {
                        item {
                            FrostedBentoCard(accentColor = accentColor, modifier = Modifier.fillMaxWidth()) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("5. VÝPOČETNÍ KOEFICIENTY TÉTO LÁTKY", style = MaterialTheme.typography.labelLarge, color = accentColor)
                                    Text(
                                        text = "Upravte váhy a rychlosti, které se používají při výpočtu tolerance, bio-zátěže a vrstvení dávek u této látky.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = glassEnv.textSecondary
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = tolGainRateStr,
                                            onValueChange = { tolGainRateStr = it },
                                            label = { Text("Růst tolerance × (1.0)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = tolDecayRateStr,
                                            onValueChange = { tolDecayRateStr = it },
                                            label = { Text("Pokles tolerance × (1.0)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = bioLoadWeightStr,
                                            onValueChange = { bioLoadWeightStr = it },
                                            label = { Text("Váha bio-zátěže × (1.0)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = superpositionWeightStr,
                                            onValueChange = { superpositionWeightStr = it },
                                            label = { Text("Váha superpozice × (1.0)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }

                        if (initialSubstance.id != 0L) {
                            item {
                                FrostedBentoCard(accentColor = accentColor, modifier = Modifier.fillMaxWidth()) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("VLASTNÍ SLEDOVANÁ POLE PRO TUTO LÁTKU (CUSTOM FIELDS)", style = MaterialTheme.typography.labelLarge, color = accentColor)
                                        customFieldsForSubstance.forEach { cf ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(glassEnv.cardBorderBase.copy(alpha = 0.25f))
                                                    .padding(horizontal = 8.dp, vertical = 5.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(cf.fieldName, style = MaterialTheme.typography.labelLarge, color = glassEnv.textPrimary)
                                                    Text("${cf.fieldType} • [${cf.optionsCsv}]", style = MaterialTheme.typography.labelSmall, color = glassEnv.textSecondary)
                                                }
                                                IconButton(onClick = { onDeleteCustomField(cf.id) }) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Delete field", tint = Color(0xFFEF4444))
                                                }
                                            }
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedTextField(
                                                value = newFieldName,
                                                onValueChange = { newFieldName = it },
                                                label = { Text("Název pole (např. Šarže / Žaludek)") },
                                                singleLine = true,
                                                modifier = Modifier.weight(1f)
                                            )
                                            OutlinedTextField(
                                                value = newFieldOptions,
                                                onValueChange = { newFieldOptions = it },
                                                label = { Text("Možnosti CSV") },
                                                singleLine = true,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }

                                        Row(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(accentColor.copy(alpha = 0.25f))
                                                .border(1.dp, accentColor, RoundedCornerShape(8.dp))
                                                .clickable {
                                                    if (newFieldName.isNotBlank()) {
                                                        val firstOpt = newFieldOptions.split(",").firstOrNull()?.trim().orEmpty()
                                                        onSaveCustomField(
                                                            CustomFieldDefEntity(
                                                                substanceId = initialSubstance.id,
                                                                fieldName = newFieldName.trim(),
                                                                fieldType = newFieldType,
                                                                optionsCsv = newFieldOptions.trim(),
                                                                defaultValue = firstOpt
                                                            )
                                                        )
                                                        newFieldName = ""
                                                    }
                                                }
                                                .padding(horizontal = 12.dp, vertical = 7.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Přidat vlastní pole", style = MaterialTheme.typography.labelLarge, color = glassEnv.textPrimary)
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
