package com.antigravity.substance.ui.logger

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.substance.domain.engine.ThresholdEvaluationEngine
import com.antigravity.substance.domain.model.DoseTier
import com.antigravity.substance.ui.theme.DynamicAmbientGlowBox
import com.antigravity.substance.ui.theme.FrostedBentoCard
import com.antigravity.substance.ui.theme.LocalSubstanceGlass
import com.antigravity.substance.ui.theme.RobotoMonoFontFamily
import com.antigravity.substance.ui.theme.SubstanceIcons
import com.antigravity.substance.ui.theme.TierBadgePill
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

/**
 * SCREEN 2: ZERO-SCROLL FULL-VIEWPORT "LOG DOSE" STUDIO
 * Fits 100% inside the phone viewport without vertical scrolling using proportional
 * Modifier.weight(...) Bento Grid rows (0.15f + 0.37f + 0.22f + 0.26f = 1.00f),
 * with full-height controls and zero dead gaps inside cards.
 */
@Composable
fun LogDoseScreen(
    state: LogDoseUiState,
    onSelectSubstance: (Long) -> Unit,
    onSelectSubtype: (Long?) -> Unit = {},
    onSelectRoa: (String) -> Unit,
    onStepDose: (Int) -> Unit,
    onSetExactDose: (Double) -> Unit,
    onSetBackdateMinutes: (Int) -> Unit,
    onSelectContextTag: (String) -> Unit,
    onUpdateNotes: (String) -> Unit,
    onUpdateCustomField: (String, String) -> Unit,
    onToggleGlowStackMode: () -> Unit,
    onCommitDose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val sub = state.selectedSubstance
    val preview = state.projectedPreview

    val targetGlowAmount = if (state.glowByStackedActiveLoad && preview != null) {
        preview.stackedPeakLoad
    } else {
        preview?.singleEffectiveDose ?: state.doseAmount
    }

    val interpolatedSpectrumColor = if (sub != null) {
        ThresholdEvaluationEngine.interpolateSpectrumColor(targetGlowAmount, sub)
    } else {
        state.activeBackgroundTier.primaryColor
    }

    val animatedTint by animateColorAsState(
        targetValue = interpolatedSpectrumColor,
        animationSpec = tween(420),
        label = "doseTint"
    )

    var showManualDoseDialog by remember { mutableStateOf(false) }
    var showNoteDialog by remember { mutableStateOf(false) }

    DynamicAmbientGlowBox(
        activeTier = state.activeBackgroundTier,
        overrideAccentColor = animatedTint,
        modifier = modifier
            .fillMaxSize()
            .testTag("log_dose_screen_root")
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // SECTION 1 (Weight 0.15f): Substance, Subtype & Route of Administration Bar
                SubstanceAndRoaSelectorCard(
                    state = state,
                    accentColor = animatedTint,
                    onSelectSubstance = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelectSubstance(it)
                    },
                    onSelectSubtype = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelectSubtype(it)
                    },
                    onSelectRoa = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelectRoa(it)
                    },
                    onToggleGlowStackMode = onToggleGlowStackMode,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.15f)
                )

                // SECTION 2 (Weight 0.37f): Tactile Dose Dial, +/- Stepper, 5-Zone Spectrum & Presets
                CenterTactileDoseStepperCard(
                    state = state,
                    accentColor = animatedTint,
                    onStepDose = { mult ->
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onStepDose(mult)
                    },
                    onSelectPreset = { valPreset ->
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSetExactDose(valPreset)
                    },
                    onOpenManualEntry = { showManualDoseDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.37f)
                )

                // SECTION 3 (Weight 0.22f): Live Projected Pharmacokinetic Preview & Stack Alert Strip
                LiveProjectedPkSparklineCard(
                    state = state,
                    accentColor = animatedTint,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.22f)
                )

                // SECTION 4 (Weight 0.26f): Custom Fields Strip, Backdate Scrubber & Glowing Commit Button
                CustomFieldsAndCommitDockCard(
                    state = state,
                    accentColor = animatedTint,
                    onUpdateCustomField = onUpdateCustomField,
                    onSelectContextTag = onSelectContextTag,
                    onOpenNotes = { showNoteDialog = true },
                    onSetBackdateMinutes = onSetBackdateMinutes,
                    onCommitDose = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onCommitDose()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.26f)
                )
            }
        }
    }

    if (showManualDoseDialog && sub != null) {
        var textVal by remember(state.doseAmount) { mutableStateOf(state.doseAmount.toString()) }
        AlertDialog(
            onDismissRequest = { showManualDoseDialog = false },
            title = { Text("Exact Dose (${sub.defaultUnit})") },
            text = {
                OutlinedTextField(
                    value = textVal,
                    onValueChange = { textVal = it },
                    label = { Text("Enter amount in ${sub.defaultUnit}") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        textVal.toDoubleOrNull()?.let { onSetExactDose(it) }
                        showManualDoseDialog = false
                    }
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualDoseDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showNoteDialog) {
        var noteDraft by remember(state.notes) { mutableStateOf(state.notes) }
        AlertDialog(
            onDismissRequest = { showNoteDialog = false },
            title = { Text("Session Note / Batch Details") },
            text = {
                OutlinedTextField(
                    value = noteDraft,
                    onValueChange = { noteDraft = it },
                    label = { Text("Optional note for this intake") }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onUpdateNotes(noteDraft)
                        showNoteDialog = false
                    }
                ) {
                    Text("Save Note")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNoteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SubstanceAndRoaSelectorCard(
    state: LogDoseUiState,
    accentColor: Color,
    onSelectSubstance: (Long) -> Unit,
    onSelectSubtype: (Long?) -> Unit,
    onSelectRoa: (String) -> Unit,
    onToggleGlowStackMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glass = LocalSubstanceGlass.current
    FrostedBentoCard(
        modifier = modifier.testTag("log_dose_top_selector_card"),
        accentColor = accentColor,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 7.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Row 1: Full-Height Substance Pill Carousel + Stack/Single Mode Pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    state.substances.forEach { sub ->
                        val isSelected = sub.id == state.selectedSubstance?.id
                        val subColor = Color(sub.colorHex)
                        val activeLoad = state.activeLoadPerSubstance[sub.id] ?: 0.0
                        val shape = RoundedCornerShape(50)

                        Row(
                            modifier = Modifier
                                .fillMaxHeight(0.92f)
                                .clip(shape)
                                .background(
                                    if (isSelected) glass.pillSurfaceActive
                                    else glass.pillSurfaceInactive
                                )
                                .border(
                                    width = if (isSelected) 1.4.dp else 1.dp,
                                    color = if (isSelected) glass.pillBorderActive else glass.pillBorderInactive,
                                    shape = shape
                                )
                                .clickable { onSelectSubstance(sub.id) }
                                .padding(horizontal = 13.dp)
                                .testTag("select_substance_${sub.name.lowercase()}"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = SubstanceIcons.iconForName(sub.iconName),
                                contentDescription = null,
                                tint = subColor,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = sub.name,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 12.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                                ),
                                color = if (isSelected) glass.textPrimary else glass.textSecondary,
                                maxLines = 1
                            )
                            if (activeLoad > 0.1) {
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "• ${String.format(Locale.US, "%.0f", activeLoad)}${sub.defaultUnit}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = RobotoMonoFontFamily,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = subColor
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Toggle: Stack vs Single mode
                val modeShape = RoundedCornerShape(50)
                Row(
                    modifier = Modifier
                        .fillMaxHeight(0.90f)
                        .clip(modeShape)
                        .background(glass.badgeSurface)
                        .border(1.dp, glass.cardBorderBase, modeShape)
                        .clickable(onClick = onToggleGlowStackMode)
                        .padding(horizontal = 10.dp)
                        .testTag("toggle_glow_mode_button"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = null,
                        tint = glass.primaryAccent,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (state.glowByStackedActiveLoad) "STACK" else "SINGLE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = RobotoMonoFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = glass.textPrimary
                    )
                }
            }

            // Row 2: Full-Height Subtypes (Druhy / Varianty) + Route of Administration (RoA) Pill Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                state.applicableSubtypes.forEach { st ->
                    val selected = st.id == state.selectedSubtype?.id
                    val stColor = Color(st.colorHex)
                    val pillShape = RoundedCornerShape(50)
                    Row(
                        modifier = Modifier
                            .fillMaxHeight(0.92f)
                            .clip(pillShape)
                            .background(
                                if (selected) glass.pillSurfaceActive
                                else glass.pillSurfaceInactive
                            )
                            .border(
                                width = if (selected) 1.4.dp else 1.dp,
                                color = if (selected) glass.pillBorderActive else glass.pillBorderInactive,
                                shape = pillShape
                            )
                            .clickable { onSelectSubtype(st.id) }
                            .padding(horizontal = 12.dp)
                            .testTag("select_subtype_${st.name.lowercase()}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(stColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = st.name,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.5.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (selected) glass.textPrimary else glass.textSecondary
                        )
                        if (st.potencyMultiplier != 1.0) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = String.format(Locale.US, "%.2fx", st.potencyMultiplier),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = RobotoMonoFontFamily,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = stColor
                            )
                        }
                    }
                }

                state.routesOfAdmin.forEach { roa ->
                    val selected = roa.name == state.selectedRoa.name
                    val pillShape = RoundedCornerShape(50)
                    Row(
                        modifier = Modifier
                            .fillMaxHeight(0.92f)
                            .clip(pillShape)
                            .background(
                                if (selected) glass.pillSurfaceActive
                                else glass.pillSurfaceInactive
                            )
                            .border(
                                width = if (selected) 1.3.dp else 1.dp,
                                color = if (selected) glass.pillBorderActive else glass.pillBorderInactive,
                                shape = pillShape
                            )
                            .clickable { onSelectRoa(roa.name) }
                            .padding(horizontal = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = roa.name,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (selected) glass.textPrimary else glass.textSecondary
                        )
                        if (roa.bioavailabilityFactor != 1.0) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = String.format(Locale.US, "%.1fx", roa.bioavailabilityFactor),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = RobotoMonoFontFamily,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = glass.primaryAccent
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CenterTactileDoseStepperCard(
    state: LogDoseUiState,
    accentColor: Color,
    onStepDose: (Int) -> Unit,
    onSelectPreset: (Double) -> Unit,
    onOpenManualEntry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glass = LocalSubstanceGlass.current
    val sub = state.selectedSubstance
    val preview = state.projectedPreview
    val unit = sub?.defaultUnit ?: "mg"
    val stepSize = sub?.stepSize ?: 10.0
    val stepperBtnBg = if (glass.isLightMode) Color(0xFFE2E8F0) else Color(0xFF171A24)
    val stepperBtnBorder = if (glass.isLightMode) Color(0xFFCBD5E1) else Color(0xFF282D3E)

    FrostedBentoCard(
        modifier = modifier.testTag("log_dose_center_stepper_card"),
        accentColor = accentColor,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Status Row inside Hero: DÁVKOVÁNÍ + Tier & Cost
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(
                        text = "DÁVKOVÁNÍ (${unit.uppercase()})",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.5.sp,
                            letterSpacing = 0.5.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = glass.textSecondary,
                        maxLines = 1,
                        softWrap = false
                    )
                    if (preview != null) {
                        TierBadgePill(tier = preview.singleDoseTier, compact = true)
                    }
                }

                if (preview != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${state.settings.formatCurrency(preview.estimatedCost)} • Vrchol: ",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = RobotoMonoFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = glass.textSecondary,
                            maxLines = 1,
                            softWrap = false
                        )
                        TierBadgePill(tier = preview.stackedPeakTier, compact = true)
                    }
                }
            }

            // Center Row: Large Matte Dark Square - / + Stepper & Giant Dose Readout
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Minus Stepper Group (-5x and -1x)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(42.dp)
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(stepperBtnBg)
                            .border(1.dp, stepperBtnBorder, RoundedCornerShape(12.dp))
                            .clickable { onStepDose(-5) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "-5x",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = RobotoMonoFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = glass.textSecondary,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(stepperBtnBg)
                            .border(1.3.dp, stepperBtnBorder, RoundedCornerShape(16.dp))
                            .clickable { onStepDose(-1) }
                            .testTag("dose_stepper_minus"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Decrease dose",
                            tint = glass.textPrimary,
                            modifier = Modifier.size(25.dp)
                        )
                    }
                }

                // Giant Precision Dose Readout (Tap for manual keypad)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onOpenManualEntry)
                        .testTag("dose_numeric_readout")
                ) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = state.settings.formatDose(state.doseAmount),
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontFamily = RobotoMonoFontFamily,
                                fontSize = 38.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = glass.textPrimary,
                            maxLines = 1,
                            softWrap = false
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = unit,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 15.5.sp,
                                color = glass.textSecondary,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(bottom = 4.dp),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    Text(
                        text = "Krok ±${state.settings.formatDose(stepSize, unit)} • Klepněte pro ruční zadání",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = glass.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Plus Stepper Group (+1x and +5x)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(stepperBtnBg)
                            .border(1.3.dp, stepperBtnBorder, RoundedCornerShape(16.dp))
                            .clickable { onStepDose(1) }
                            .testTag("dose_stepper_plus"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Increase dose",
                            tint = glass.textPrimary,
                            modifier = Modifier.size(25.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(42.dp)
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(stepperBtnBg)
                            .border(1.dp, stepperBtnBorder, RoundedCornerShape(12.dp))
                            .clickable { onStepDose(5) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+5x",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = RobotoMonoFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = glass.textSecondary,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            // 5-Zone Threshold Spectrum Bar with Single Dose & Stacked Peak Markers
            if (sub != null && preview != null) {
                FiveZoneThresholdSpectrumBar(
                    singleFraction = preview.singleSpectrumFraction,
                    stackedFraction = preview.stackedSpectrumFraction,
                    isStacking = preview.isStackingActive,
                    substance = sub,
                    settings = state.settings
                )
            }

            // Tall Quick-Preset Dose Chips Row in Indigo Pill Style
            if (sub != null) {
                val presets = sub.parsedQuickPresets()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    presets.forEach { presetVal ->
                        val isMatch = kotlin.math.abs(presetVal - state.doseAmount) < 0.01
                        val pillShape = RoundedCornerShape(50)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(pillShape)
                                .background(
                                    if (isMatch) glass.pillSurfaceActive
                                    else glass.pillSurfaceInactive
                                )
                                .border(
                                    width = if (isMatch) 1.4.dp else 1.dp,
                                    color = if (isMatch) glass.pillBorderActive else glass.pillBorderInactive,
                                    shape = pillShape
                                )
                                .clickable { onSelectPreset(presetVal) }
                                .padding(vertical = 7.dp)
                                .testTag("preset_chip_${presetVal}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = state.settings.formatDose(presetVal, sub.defaultUnit),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = RobotoMonoFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = if (isMatch) FontWeight.Bold else FontWeight.SemiBold
                                ),
                                color = if (isMatch) glass.textPrimary else glass.textSecondary,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FiveZoneThresholdSpectrumBar(
    singleFraction: Float,
    stackedFraction: Float,
    isStacking: Boolean,
    substance: com.antigravity.substance.data.db.entities.SubstanceEntity,
    settings: com.antigravity.substance.data.preferences.UserSettings
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
        ) {
            val w = size.width
            val h = size.height
            val segW = w / 5f
            val zoneColors = listOf(
                DoseTier.NEGLIGIBLE.primaryColor,
                DoseTier.LIGHT.primaryColor,
                DoseTier.MEDIUM.primaryColor,
                DoseTier.STRONG.primaryColor,
                DoseTier.DANGEROUS.primaryColor
            )
            zoneColors.forEachIndexed { idx, col ->
                drawRoundRect(
                    color = col.copy(alpha = 0.42f),
                    topLeft = Offset(idx * segW + 2f, h * 0.22f),
                    size = Size(segW - 4f, h * 0.56f),
                    cornerRadius = CornerRadius(5.dp.toPx())
                )
            }

            // Single Dose Marker
            val singleX = (singleFraction * w).coerceIn(6f, w - 6f)
            drawCircle(
                color = Color.White,
                radius = 6.dp.toPx(),
                center = Offset(singleX, h / 2f)
            )
            drawCircle(
                color = Color.Black,
                radius = 2.5.dp.toPx(),
                center = Offset(singleX, h / 2f)
            )

            // Stacked Peak Marker (if higher than single dose)
            if (isStacking && stackedFraction > singleFraction + 0.01f) {
                val stackX = (stackedFraction * w).coerceIn(6f, w - 6f)
                drawLine(
                    color = Color.White.copy(alpha = 0.75f),
                    start = Offset(singleX, h / 2f),
                    end = Offset(stackX, h / 2f),
                    strokeWidth = 1.8.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 4f))
                )
                drawCircle(
                    color = Color(0xFFF59E0B),
                    radius = 6.5.dp.toPx(),
                    center = Offset(stackX, h / 2f)
                )
                drawCircle(
                    color = Color.White,
                    radius = 2.6.dp.toPx(),
                    center = Offset(stackX, h / 2f)
                )
            }
        }

        // Zone boundary labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Neg ≤${settings.formatDose(substance.thresholdNegligible)}",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = RobotoMonoFontFamily, fontSize = 9.5.sp, fontWeight = FontWeight.Medium),
                color = DoseTier.NEGLIGIBLE.primaryColor
            )
            Text(
                text = "Light ≤${settings.formatDose(substance.thresholdLight)}",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = RobotoMonoFontFamily, fontSize = 9.5.sp, fontWeight = FontWeight.Medium),
                color = DoseTier.LIGHT.primaryColor
            )
            Text(
                text = "Med ≤${settings.formatDose(substance.thresholdMedium)}",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = RobotoMonoFontFamily, fontSize = 9.5.sp, fontWeight = FontWeight.Medium),
                color = DoseTier.MEDIUM.primaryColor
            )
            Text(
                text = "Strong ≤${settings.formatDose(substance.thresholdStrong)}",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = RobotoMonoFontFamily, fontSize = 9.5.sp, fontWeight = FontWeight.Medium),
                color = DoseTier.STRONG.primaryColor
            )
            Text(
                text = "Redline >${settings.formatDose(substance.thresholdStrong)}",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = RobotoMonoFontFamily, fontSize = 9.5.sp, fontWeight = FontWeight.Medium),
                color = DoseTier.DANGEROUS.primaryColor
            )
        }
    }
}

@Composable
private fun LiveProjectedPkSparklineCard(
    state: LogDoseUiState,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val glass = LocalSubstanceGlass.current
    val sub = state.selectedSubstance
    val preview = state.projectedPreview
    val timeFmt = SimpleDateFormat("HH:mm", Locale.US)

    FrostedBentoCard(
        modifier = modifier.testTag("log_dose_projected_pk_card"),
        accentColor = accentColor,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        if (sub == null || preview == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Vyberte látku pro odhad fází", color = glass.textSecondary)
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                // Top Row: ORIENTAČNÍ ODHAD FÁZÍ or Stacking warning
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (preview.isStackingActive) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = preview.stackedPeakTier.primaryColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "STACKING AKTIVNÍ: Vrchol +${max(0, preview.peakShiftMinutes)}m → ${String.format(Locale.US, "%.1f", preview.stackedPeakLoad)} ${sub.defaultUnit} (${preview.stackedPeakTier.titleEn})",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = RobotoMonoFontFamily,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = preview.stackedPeakTier.primaryColor
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else {
                        Text(
                            text = "ORIENTAČNÍ ODHAD FÁZÍ",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.5.sp,
                                letterSpacing = 0.6.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = glass.textSecondary
                        )
                    }
                }

                // Full-Height Sparkline Canvas
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 2.dp)
                ) {
                    val w = size.width
                    val h = size.height
                    val pts = preview.previewCurvePoints
                    if (w <= 10f || h <= 6f || pts.size < 2) return@Canvas

                    val maxY = max(sub.thresholdStrong * 1.15, preview.stackedPeakLoad * 1.12).coerceAtLeast(1.0)
                    val existingPath = Path()
                    val projectedPath = Path()
                    val fillPath = Path()

                    pts.forEachIndexed { idx, pt ->
                        val px = (idx.toFloat() / (pts.size - 1).coerceAtLeast(1)) * w
                        val exY = (h - (pt.plasmaConcentration / maxY).toFloat() * (h * 0.90f)).coerceIn(2f, h)
                        val prY = (h - (pt.activeEffectLoad / maxY).toFloat() * (h * 0.90f)).coerceIn(2f, h)
                        if (idx == 0) {
                            existingPath.moveTo(px, exY)
                            projectedPath.moveTo(px, prY)
                            fillPath.moveTo(px, h)
                            fillPath.lineTo(px, prY)
                        } else {
                            existingPath.lineTo(px, exY)
                            projectedPath.lineTo(px, prY)
                            fillPath.lineTo(px, prY)
                        }
                        if (idx == pts.lastIndex) {
                            fillPath.lineTo(px, h)
                            fillPath.close()
                        }
                    }

                    if (!glass.isLightweightMode) {
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(accentColor.copy(alpha = 0.30f), Color.Transparent)
                            )
                        )
                    }
                    drawPath(
                        path = existingPath,
                        color = glass.textSecondary.copy(alpha = 0.40f),
                        style = Stroke(
                            width = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f))
                        )
                    )
                    drawPath(
                        path = projectedPath,
                        color = accentColor,
                        style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Bottom 4-Phase Timestamps Strip: Užití -> Nástup -> Vrchol -> Konec
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Užití",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = RobotoMonoFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = glass.textSecondary
                    )
                    Text(
                        text = "Nástup @ ${timeFmt.format(Date(preview.onsetTimestampMillis))}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = RobotoMonoFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = glass.textSecondary
                    )
                    Text(
                        text = "Vrchol @ ${timeFmt.format(Date(preview.combinedPeakTimestampMillis))}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = RobotoMonoFontFamily,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = accentColor
                    )
                    Text(
                        text = "Konec @ ${timeFmt.format(Date(preview.activeClearTimestampMillis))}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = RobotoMonoFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = glass.textSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomFieldsAndCommitDockCard(
    state: LogDoseUiState,
    accentColor: Color,
    onUpdateCustomField: (String, String) -> Unit,
    onSelectContextTag: (String) -> Unit,
    onOpenNotes: () -> Unit,
    onSetBackdateMinutes: (Int) -> Unit,
    onCommitDose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glass = LocalSubstanceGlass.current
    val sub = state.selectedSubstance
    val preview = state.projectedPreview

    FrostedBentoCard(
        modifier = modifier.testTag("log_dose_bottom_commit_card"),
        accentColor = glass.primaryAccent,
        contentPadding = PaddingValues(horizontal = 11.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Row 1: Full-Height #tagy (#práce, #relax, #spánek, #focus) + Custom Fields + Poznámka
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.95f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val tags = state.settings.parsedContextTags()
                val pillShape = RoundedCornerShape(50)
                Row(
                    modifier = Modifier
                        .fillMaxHeight(0.92f)
                        .clip(pillShape)
                        .background(glass.pillSurfaceActive)
                        .border(1.3.dp, glass.pillBorderActive, pillShape)
                        .clickable {
                            val idx = tags.indexOf(state.selectedContextTag)
                            val next = tags.getOrElse((idx + 1) % tags.size.coerceAtLeast(1)) { "focus" }
                            onSelectContextTag(next)
                        }
                        .padding(horizontal = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "#${state.selectedContextTag}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = glass.textPrimary
                    )
                }

                // Per-Substance & Global Custom Fields
                state.applicableCustomFields.forEach { fieldDef ->
                    val currentVal = state.customFieldValues[fieldDef.fieldName] ?: fieldDef.defaultValue
                    val options = fieldDef.parsedOptions()
                    Row(
                        modifier = Modifier
                            .fillMaxHeight(0.92f)
                            .clip(pillShape)
                            .background(glass.pillSurfaceInactive)
                            .border(1.dp, glass.pillBorderInactive, pillShape)
                            .clickable {
                                when (fieldDef.fieldType) {
                                    "BOOLEAN_SWITCH" -> {
                                        val next = if (currentVal == "true") "false" else "true"
                                        onUpdateCustomField(fieldDef.fieldName, next)
                                    }
                                    "SLIDER_1_TO_10" -> {
                                        val curInt = currentVal.toIntOrNull() ?: 8
                                        val nextInt = if (curInt >= 10) 5 else curInt + 1
                                        onUpdateCustomField(fieldDef.fieldName, nextInt.toString())
                                    }
                                    else -> {
                                        if (options.isNotEmpty()) {
                                            val idx = options.indexOf(currentVal)
                                            val next = options[(idx + 1) % options.size]
                                            onUpdateCustomField(fieldDef.fieldName, next)
                                        }
                                    }
                                }
                            }
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${fieldDef.fieldName}: $currentVal",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = glass.textSecondary,
                            maxLines = 1
                        )
                    }
                }

                // Note Chip
                Row(
                    modifier = Modifier
                        .fillMaxHeight(0.92f)
                        .clip(pillShape)
                        .background(glass.pillSurfaceInactive)
                        .border(1.dp, glass.pillBorderInactive, pillShape)
                        .clickable(onClick = onOpenNotes)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = glass.textSecondary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (state.notes.isBlank()) "Poznámka..." else state.notes.take(16),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = glass.textSecondary
                    )
                }
            }

            // Row 2: Full-Height ČAS UŽITÍ Pill Strip (Teď | -10m | -30m | -1h | -2h | -4h)
            val backdateOptions = listOf(
                0 to "Teď",
                -10 to "-10m",
                -30 to "-30m",
                -60 to "-1h",
                -120 to "-2h",
                -240 to "-4h"
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.95f),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = glass.textSecondary,
                    modifier = Modifier.size(15.dp)
                )
                backdateOptions.forEach { (mins, label) ->
                    val selected = state.backdateOffsetMinutes == mins
                    val pillShape = RoundedCornerShape(50)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(0.92f)
                            .clip(pillShape)
                            .background(
                                if (selected) glass.pillSurfaceActive
                                else glass.pillSurfaceInactive
                            )
                            .border(
                                width = if (selected) 1.4.dp else 1.dp,
                                color = if (selected) glass.pillBorderActive else glass.pillBorderInactive,
                                shape = pillShape
                            )
                            .clickable { onSetBackdateMinutes(mins) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = RobotoMonoFontFamily,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
                            ),
                            color = if (selected) glass.textPrimary else glass.textSecondary
                        )
                    }
                }
            }

            // Row 3: Prominent Solid Indigo-Violet CTA Button (+ Zaznamenat užití)
            val commitShape = RoundedCornerShape(14.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(commitShape)
                    .background(glass.primaryAccent)
                    .clickable(onClick = onCommitDose)
                    .testTag("commit_log_dose_button"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (state.lastCommittedMessage != null) Icons.Default.Check else Icons.Default.Add,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    val btnLabel = if (sub != null && preview != null) {
                        "Zaznamenat užití (${state.settings.formatDose(state.doseAmount, sub.defaultUnit)} ${sub.name})"
                    } else {
                        "Zaznamenat užití"
                    }
                    Text(
                        text = btnLabel,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
