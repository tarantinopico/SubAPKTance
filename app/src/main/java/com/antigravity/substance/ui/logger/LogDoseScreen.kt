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
import androidx.compose.material.icons.filled.Bolt
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
import com.antigravity.substance.ui.theme.SubstanceIcons
import com.antigravity.substance.ui.theme.TierBadgePill
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

/**
 * SCREEN 2: ZERO-SCROLL FULL-VIEWPORT "LOG DOSE" STUDIO
 * Fits 100% inside the phone viewport without vertical scrolling using proportional
 * Modifier.weight(...) Bento Grid rows (0.16f + 0.38f + 0.18f + 0.28f = 1.00f).
 * Features real-time threshold-reactive ambient background glow and live stacked peak preview.
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

    // Interpolate smooth spectrum color for the active glow
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
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                // SECTION 1 (Weight 0.18f): Substance, Subtype & Route of Administration Bar
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
                        .weight(0.18f)
                )

                // SECTION 2 (Weight 0.36f): Tactile Dose Dial, +/- Stepper, 5-Zone Spectrum & Presets
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
                        .weight(0.36f)
                )

                // SECTION 3 (Weight 0.18f): Live Projected Pharmacokinetic Preview & Stack Alert Strip
                LiveProjectedPkSparklineCard(
                    state = state,
                    accentColor = animatedTint,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.18f)
                )

                // SECTION 4 (Weight 0.28f): Custom Fields Strip, Backdate Scrubber & Glowing Commit Button
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
                        .weight(0.28f)
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
    FrostedBentoCard(
        modifier = modifier.testTag("log_dose_top_selector_card"),
        cornerRadius = 18.dp,
        accentColor = accentColor,
        contentPadding = PaddingValues(horizontal = 9.dp, vertical = 5.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Row 1: Substance Squircle Carousel + Glow Mode Switcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    state.substances.forEach { sub ->
                        val isSelected = sub.id == state.selectedSubstance?.id
                        val subColor = Color(sub.colorHex)
                        val activeLoad = state.activeLoadPerSubstance[sub.id] ?: 0.0
                        val shape = RoundedCornerShape(10.dp)

                        Row(
                            modifier = Modifier
                                .clip(shape)
                                .background(
                                    if (isSelected) subColor.copy(alpha = 0.25f)
                                    else Color.White.copy(alpha = 0.05f)
                                )
                                .border(
                                    width = if (isSelected) 1.2.dp else 0.8.dp,
                                    color = if (isSelected) subColor else Color.White.copy(alpha = 0.12f),
                                    shape = shape
                                )
                                .clickable { onSelectSubstance(sub.id) }
                                .padding(horizontal = 8.dp, vertical = 3.5.dp)
                                .testTag("select_substance_${sub.name.lowercase()}"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = SubstanceIcons.iconForName(sub.iconName),
                                contentDescription = null,
                                tint = subColor,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = sub.name,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                            if (activeLoad > 0.1) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "• ${String.format(Locale.US, "%.0f", activeLoad)}${sub.defaultUnit}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                                    color = subColor
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Toggle: Glow by Stacked Active Load vs Single Dose
                val modeShape = RoundedCornerShape(8.dp)
                Row(
                    modifier = Modifier
                        .clip(modeShape)
                        .background(accentColor.copy(alpha = 0.16f))
                        .border(0.8.dp, accentColor.copy(alpha = 0.45f), modeShape)
                        .clickable(onClick = onToggleGlowStackMode)
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                        .testTag("toggle_glow_mode_button"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = if (state.glowByStackedActiveLoad) "STACK" else "SINGLE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Row 2: Subtypes (Druhy / Varianty) + Route of Administration (RoA) Segmented Pill Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                state.applicableSubtypes.forEach { st ->
                    val selected = st.id == state.selectedSubtype?.id
                    val stColor = Color(st.colorHex)
                    val pillShape = RoundedCornerShape(8.dp)
                    Row(
                        modifier = Modifier
                            .clip(pillShape)
                            .background(
                                if (selected) stColor.copy(alpha = 0.24f)
                                else Color.White.copy(alpha = 0.04f)
                            )
                            .border(
                                width = if (selected) 1.dp else 0.7.dp,
                                color = if (selected) stColor else Color.White.copy(alpha = 0.14f),
                                shape = pillShape
                            )
                            .clickable { onSelectSubtype(st.id) }
                            .padding(horizontal = 7.dp, vertical = 2.5.dp)
                            .testTag("select_subtype_${st.name.lowercase()}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(stColor)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = st.name,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.5.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (st.potencyMultiplier != 1.0) {
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "${String.format(Locale.US, "%.2fx", st.potencyMultiplier)}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
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
                            .clip(pillShape)
                            .background(
                                if (selected) accentColor.copy(alpha = 0.20f)
                                else Color.White.copy(alpha = 0.04f)
                            )
                            .border(
                                width = 0.8.dp,
                                color = if (selected) accentColor else Color.White.copy(alpha = 0.10f),
                                shape = pillShape
                            )
                            .clickable { onSelectRoa(roa.name) }
                            .padding(horizontal = 7.dp, vertical = 2.5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = roa.name,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (roa.bioavailabilityFactor != 1.0) {
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "${String.format(Locale.US, "%.1fx", roa.bioavailabilityFactor)}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                color = accentColor
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
    val sub = state.selectedSubstance
    val preview = state.projectedPreview
    val unit = sub?.defaultUnit ?: "mg"
    val stepSize = sub?.stepSize ?: 10.0

    FrostedBentoCard(
        modifier = modifier.testTag("log_dose_center_stepper_card"),
        cornerRadius = 22.dp,
        accentColor = accentColor,
        contentPadding = PaddingValues(10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Status Row inside Hero: Single Tier vs Stacked Peak Tier + Redline Safety Warning
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = "DOSE SYNTHESIZER",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            letterSpacing = 0.7.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(0xFF94A3B8)
                    )
                    if (preview != null) {
                        TierBadgePill(tier = preview.singleDoseTier, compact = true)
                    }
                }

                if (preview != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${state.settings.formatCurrency(preview.estimatedCost)} • Peak: ",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                            color = Color(0xFFCBD5E1)
                        )
                        TierBadgePill(tier = preview.stackedPeakTier, compact = true)
                    }
                }
            }

            // Center Row: Ergonomic - / + Tactile Stepper & Precision Readout
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Minus Stepper Group (-5x and -1x)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(0.8.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(10.dp))
                            .clickable { onStepDose(-5) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "-5x",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = Color(0xFF94A3B8)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(accentColor.copy(alpha = 0.16f))
                            .border(1.2.dp, accentColor.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
                            .clickable { onStepDose(-1) }
                            .testTag("dose_stepper_minus"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Decrease dose",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Giant Precision Dose Readout (Tap for manual keypad)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
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
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.White,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = unit,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 15.sp,
                                color = accentColor,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    Text(
                        text = "Step ±${state.settings.formatDose(stepSize, unit)} • Tap for exact input",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                        color = Color(0xFF94A3B8),
                        maxLines = 1
                    )
                }

                // Plus Stepper Group (+1x and +5x)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(accentColor.copy(alpha = 0.22f))
                            .border(1.2.dp, accentColor.copy(alpha = 0.70f), RoundedCornerShape(16.dp))
                            .clickable { onStepDose(1) }
                            .testTag("dose_stepper_plus"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Increase dose",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(0.8.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(10.dp))
                            .clickable { onStepDose(5) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+5x",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = Color(0xFF94A3B8)
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

            // Quick-Preset Dose Chips Row
            if (sub != null) {
                val presets = sub.parsedQuickPresets()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    presets.forEach { presetVal ->
                        val isMatch = kotlin.math.abs(presetVal - state.doseAmount) < 0.01
                        val pTier = ThresholdEvaluationEngine.evaluateTier(presetVal, sub)
                        val pColor = pTier.primaryColor
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(9.dp))
                                .background(
                                    if (isMatch) pColor.copy(alpha = 0.28f)
                                    else Color.White.copy(alpha = 0.05f)
                                )
                                .border(
                                    width = if (isMatch) 1.2.dp else 0.8.dp,
                                    color = if (isMatch) pColor else Color.White.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(9.dp)
                                )
                                .clickable { onSelectPreset(presetVal) }
                                .padding(vertical = 4.dp)
                                .testTag("preset_chip_${presetVal}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = state.settings.formatDose(presetVal, sub.defaultUnit),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.5.sp,
                                    fontWeight = if (isMatch) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (isMatch) Color.White else Color(0xFFCBD5E1),
                                maxLines = 1
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
    Column(modifier = Modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
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
                    color = col.copy(alpha = 0.35f),
                    topLeft = Offset(idx * segW + 2f, h * 0.25f),
                    size = Size(segW - 4f, h * 0.50f),
                    cornerRadius = CornerRadius(4.dp.toPx())
                )
            }

            // Single Dose Marker
            val singleX = (singleFraction * w).coerceIn(5f, w - 5f)
            drawCircle(
                color = Color.White,
                radius = 5.dp.toPx(),
                center = Offset(singleX, h / 2f)
            )
            drawCircle(
                color = Color.Black,
                radius = 2.dp.toPx(),
                center = Offset(singleX, h / 2f)
            )

            // Stacked Peak Marker (if higher than single dose)
            if (isStacking && stackedFraction > singleFraction + 0.01f) {
                val stackX = (stackedFraction * w).coerceIn(5f, w - 5f)
                drawLine(
                    color = Color.White.copy(alpha = 0.7f),
                    start = Offset(singleX, h / 2f),
                    end = Offset(stackX, h / 2f),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
                )
                drawCircle(
                    color = Color(0xFFF59E0B),
                    radius = 5.5.dp.toPx(),
                    center = Offset(stackX, h / 2f)
                )
                drawCircle(
                    color = Color.White,
                    radius = 2.2.dp.toPx(),
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
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                color = DoseTier.NEGLIGIBLE.primaryColor
            )
            Text(
                text = "Light ≤${settings.formatDose(substance.thresholdLight)}",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                color = DoseTier.LIGHT.primaryColor
            )
            Text(
                text = "Med ≤${settings.formatDose(substance.thresholdMedium)}",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                color = DoseTier.MEDIUM.primaryColor
            )
            Text(
                text = "Strong ≤${settings.formatDose(substance.thresholdStrong)}",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                color = DoseTier.STRONG.primaryColor
            )
            Text(
                text = "Redline >${settings.formatDose(substance.thresholdStrong)}",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
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
    val sub = state.selectedSubstance
    val preview = state.projectedPreview
    val timeFmt = SimpleDateFormat("HH:mm", Locale.US)

    FrostedBentoCard(
        modifier = modifier.testTag("log_dose_projected_pk_card"),
        cornerRadius = 18.dp,
        accentColor = accentColor,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
    ) {
        if (sub == null || preview == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Select a substance to preview pharmacokinetics", color = Color(0xFF94A3B8))
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Row: Stacking amplification banner or standard timeline header
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
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "STACKING ACTIVE: Peak shifts +${max(0, preview.peakShiftMinutes)}m → ${String.format(Locale.US, "%.1f", preview.stackedPeakLoad)} ${sub.defaultUnit} (${preview.stackedPeakTier.titleEn})",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = preview.stackedPeakTier.primaryColor
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else {
                        Text(
                            text = "PROJECTED PHARMACOKINETIC TRAJECTORY",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                // Sparkline Canvas comparing Existing Active Curve (dashed) vs Projected Stacked Curve (solid glowing)
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

                    val maxY = max(sub.thresholdStrong * 1.15, preview.stackedPeakLoad * 1.15).coerceAtLeast(1.0)
                    val existingPath = Path()
                    val projectedPath = Path()
                    val fillPath = Path()

                    pts.forEachIndexed { idx, pt ->
                        val px = (idx.toFloat() / (pts.size - 1).coerceAtLeast(1)) * w
                        val exY = (h - (pt.plasmaConcentration / maxY).toFloat() * (h * 0.88f)).coerceIn(1f, h)
                        val prY = (h - (pt.activeEffectLoad / maxY).toFloat() * (h * 0.88f)).coerceIn(1f, h)
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

                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(accentColor.copy(alpha = 0.32f), Color.Transparent)
                        )
                    )
                    drawPath(
                        path = existingPath,
                        color = Color.White.copy(alpha = 0.30f),
                        style = Stroke(
                            width = 1.2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f))
                        )
                    )
                    drawPath(
                        path = projectedPath,
                        color = accentColor,
                        style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Bottom Timestamps Strip: Onset -> Combined Peak -> Clear
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Onset @ ${timeFmt.format(Date(preview.onsetTimestampMillis))}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = Color(0xFF94A3B8)
                    )
                    Text(
                        text = "Peak @ ${timeFmt.format(Date(preview.combinedPeakTimestampMillis))} (${String.format(Locale.US, "%.1f", preview.stackedPeakLoad)} ${sub.defaultUnit})",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = accentColor
                    )
                    Text(
                        text = "Clear @ ${timeFmt.format(Date(preview.activeClearTimestampMillis))}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = Color(0xFF94A3B8)
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
    val sub = state.selectedSubstance
    val preview = state.projectedPreview

    FrostedBentoCard(
        modifier = modifier.testTag("log_dose_bottom_commit_card"),
        cornerRadius = 20.dp,
        accentColor = accentColor,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 7.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Row 1: Dynamic Custom Fields & Context Tag Horizontal Bento Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Context Tag Selector Chip (cycles through tags on tap)
                val tags = state.settings.parsedContextTags()
                val chipShape = RoundedCornerShape(9.dp)
                Row(
                    modifier = Modifier
                        .clip(chipShape)
                        .background(Color.White.copy(alpha = 0.06f))
                        .border(0.8.dp, Color.White.copy(alpha = 0.16f), chipShape)
                        .clickable {
                            val idx = tags.indexOf(state.selectedContextTag)
                            val next = tags.getOrElse((idx + 1) % tags.size.coerceAtLeast(1)) { "Focus" }
                            onSelectContextTag(next)
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tag: ${state.selectedContextTag}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(0xFF38BDF8)
                    )
                }

                // Per-Substance & Global Custom Fields (Tap chip to cycle options!)
                state.applicableCustomFields.forEach { fieldDef ->
                    val currentVal = state.customFieldValues[fieldDef.fieldName] ?: fieldDef.defaultValue
                    val options = fieldDef.parsedOptions()
                    Row(
                        modifier = Modifier
                            .clip(chipShape)
                            .background(accentColor.copy(alpha = 0.12f))
                            .border(0.8.dp, accentColor.copy(alpha = 0.35f), chipShape)
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
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${fieldDef.fieldName}: $currentVal",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                            color = Color.White,
                            maxLines = 1
                        )
                    }
                }

                // Note Chip
                Row(
                    modifier = Modifier
                        .clip(chipShape)
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(0.8.dp, Color.White.copy(alpha = 0.14f), chipShape)
                        .clickable(onClick = onOpenNotes)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = if (state.notes.isBlank()) "Add Note" else state.notes.take(14),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                        color = Color(0xFFCBD5E1)
                    )
                }
            }

            // Row 2: Interactive Timestamp & Backdate Bar
            val backdateOptions = listOf(
                0 to "NOW",
                -10 to "-10m",
                -30 to "-30m",
                -60 to "-1h",
                -120 to "-2h",
                -240 to "-4h"
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(12.dp)
                )
                backdateOptions.forEach { (mins, label) ->
                    val selected = state.backdateOffsetMinutes == mins
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(7.dp))
                            .background(
                                if (selected) accentColor.copy(alpha = 0.25f)
                                else Color.White.copy(alpha = 0.04f)
                            )
                            .border(
                                width = 0.8.dp,
                                color = if (selected) accentColor else Color.White.copy(alpha = 0.10f),
                                shape = RoundedCornerShape(7.dp)
                            )
                            .clickable { onSetBackdateMinutes(mins) }
                            .padding(vertical = 3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (selected) Color.White else Color(0xFF94A3B8)
                        )
                    }
                }
            }

            // Row 3: Glowing Commit Button
            val commitShape = RoundedCornerShape(14.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .clip(commitShape)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.85f),
                                state.activeBackgroundTier.primaryColor.copy(alpha = 0.85f)
                            )
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.35f), commitShape)
                    .clickable(onClick = onCommitDose)
                    .testTag("commit_log_dose_button"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (state.lastCommittedMessage != null) Icons.Default.Check else Icons.Default.Bolt,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    val btnLabel = if (sub != null && preview != null) {
                        "LOG ${state.settings.formatDose(state.doseAmount, sub.defaultUnit)} ${sub.name.uppercase()} • ${preview.stackedPeakTier.titleEn.uppercase()} TIER"
                    } else {
                        "LOG DOSE"
                    }
                    Text(
                        text = btnLabel,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
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
