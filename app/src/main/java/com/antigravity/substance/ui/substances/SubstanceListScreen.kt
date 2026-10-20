package com.antigravity.substance.ui.substances

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.substance.data.db.entities.CustomFieldDefEntity
import com.antigravity.substance.data.db.entities.SubstanceEntity
import com.antigravity.substance.data.db.entities.SubstanceSubtypeEntity
import com.antigravity.substance.data.preferences.UserSettings
import com.antigravity.substance.domain.model.DoseTier
import com.antigravity.substance.ui.theme.DynamicAmbientGlowBox
import com.antigravity.substance.ui.theme.FrostedBentoCard
import com.antigravity.substance.ui.theme.LocalSubstanceGlass
import com.antigravity.substance.ui.theme.SquircleIconBadge
import com.antigravity.substance.ui.theme.SubstanceIcons
import java.util.Locale

/**
 * SCREEN 4: SUBSTANCE MANAGEMENT, SUBTYPES & PHARMACOKINETIC STUDIO (100% CRUD)
 */
@Composable
fun SubstanceListScreen(
    substances: List<SubstanceEntity>,
    customFields: List<CustomFieldDefEntity>,
    settings: UserSettings,
    subtypes: List<SubstanceSubtypeEntity> = emptyList(),
    onSaveSubstance: (SubstanceEntity) -> Unit,
    onDuplicateSubstance: (SubstanceEntity) -> Unit,
    onDeleteSubstance: (Long) -> Unit,
    onSaveSubtype: (SubstanceSubtypeEntity) -> Unit = {},
    onDeleteSubtype: (Long) -> Unit = {},
    onSaveCustomField: (CustomFieldDefEntity) -> Unit,
    onDeleteCustomField: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val glassEnv = LocalSubstanceGlass.current
    var editingSubstance by remember { mutableStateOf<SubstanceEntity?>(null) }

    DynamicAmbientGlowBox(
        activeTier = DoseTier.LIGHT,
        overrideAccentColor = Color(0xFF10B981),
        modifier = modifier
            .fillMaxSize()
            .testTag("substance_list_screen_root")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Header + "+ Nová látka" button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SPRÁVA LÁTEK, DRUHŮ & MODELŮ",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.5.sp,
                            color = Color(0xFF10B981),
                            letterSpacing = 0.7.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "Látky & Poddruhy (${substances.size})",
                        style = MaterialTheme.typography.headlineMedium,
                        color = glassEnv.textPrimary
                    )
                }

                val btnShape = RoundedCornerShape(12.dp)
                Row(
                    modifier = Modifier
                        .clip(btnShape)
                        .background(Color(0xFF10B981).copy(alpha = 0.22f))
                        .border(1.dp, Color(0xFF10B981), btnShape)
                        .clickable {
                            editingSubstance = SubstanceEntity(
                                id = 0L,
                                name = "Nová látka",
                                subtitle = "Doplněk / Nootropikum",
                                category = "Nootropic",
                                iconName = "science",
                                colorHex = 0xFF06B6D4L,
                                defaultUnit = "mg",
                                stepSize = 25.0,
                                defaultDose = 100.0,
                                quickPresetsCsv = "50,100,150,200,300",
                                costPerUnit = 0.15,
                                thresholdNegligible = 30.0,
                                thresholdLight = 80.0,
                                thresholdMedium = 160.0,
                                thresholdStrong = 250.0,
                                thresholdDangerous = 380.0,
                                dailyMaxSafeLimit = 300.0,
                                recommendedMinIntervalMinutes = 180,
                                timingMode = "SIMPLE",
                                mainEffectDurationMinutes = 240,
                                onsetMinutes = 20,
                                comeupMinutes = 30,
                                peakMinutes = 90,
                                offsetMinutes = 100,
                                hasBodyPresenceDuration = true,
                                bodyPresenceHours = 10.0,
                                halfLifeHours = 4.0,
                                sleepDisruptionThreshold = 50.0,
                                sortOrder = substances.size
                            )
                        }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                        .testTag("add_new_substance_button"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Nová látka",
                        style = MaterialTheme.typography.labelLarge,
                        color = glassEnv.textPrimary
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(substances, key = { it.id }) { sub ->
                    val subColor = Color(sub.colorHex)
                    val subFields = customFields.filter { it.substanceId == sub.id }
                    val subSubtypes = subtypes.filter { it.substanceId == sub.id }

                    FrostedBentoCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("substance_card_${sub.name.lowercase()}"),
                        accentColor = subColor,
                        contentPadding = PaddingValues(13.dp),
                        onClick = { editingSubstance = sub }
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    SquircleIconBadge(
                                        icon = SubstanceIcons.iconForName(sub.iconName),
                                        colorHex = sub.colorHex,
                                        size = 42.dp,
                                        iconSize = 22.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = sub.name,
                                                style = MaterialTheme.typography.titleLarge.copy(
                                                    fontSize = 17.sp,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                color = glassEnv.textPrimary
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "• ${sub.category}",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                ),
                                                color = subColor
                                            )
                                            if (sub.isArchived) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "[ARCHIV]",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                                    color = Color(0xFFF59E0B)
                                                )
                                            }
                                        }
                                        Text(
                                            text = sub.subtitle,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp),
                                            color = glassEnv.textSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { onSaveSubstance(sub.copy(isFavorite = !sub.isFavorite)) },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (sub.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                            contentDescription = "Favorite",
                                            tint = if (sub.isFavorite) Color(0xFFF59E0B) else glassEnv.textMuted,
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { onDuplicateSubstance(sub) },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Duplicate substance",
                                            tint = Color(0xFF38BDF8),
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { onSaveSubstance(sub.copy(isArchived = !sub.isArchived)) },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Archive,
                                            contentDescription = "Archive substance",
                                            tint = if (sub.isArchived) Color(0xFFF59E0B) else glassEnv.textSecondary,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { editingSubstance = sub },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit substance",
                                            tint = glassEnv.textPrimary,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                    if (substances.size > 1) {
                                        IconButton(
                                            onClick = { onDeleteSubstance(sub.id) },
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete substance",
                                                tint = Color(0xFFEF4444),
                                                modifier = Modifier.size(17.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Subtypes list badge
                            if (subSubtypes.isNotEmpty()) {
                                Text(
                                    text = "Druhy (${subSubtypes.size}): ${subSubtypes.joinToString(", ") { "${it.name} (${String.format(Locale.US, "%.2fx", it.potencyMultiplier)})" }}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = subColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Unit, Cost & Safe Max Strip
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(glassEnv.badgeSurface)
                                    .border(0.8.dp, glassEnv.cardBorderBase, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Jednotka: ${sub.defaultUnit} (±${settings.formatDose(sub.stepSize)})",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = glassEnv.textPrimary
                                )
                                Text(
                                    text = "Cena: ${settings.formatCurrency(sub.costPerUnit)}/${sub.defaultUnit}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color(0xFF10B981)
                                )
                                Text(
                                    text = "Max/den: ${settings.formatDose(sub.dailyMaxSafeLimit, sub.defaultUnit)}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color(0xFFF59E0B)
                                )
                            }

                            // Active Effect vs Body Presence Summary
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val timingDesc = when {
                                    !sub.isTimingEnabled -> "Časové modelování: Vypnuto (nepovinné)"
                                    sub.hasBodyPresenceDuration ->
                                        "Hlavní doba účinku: ${String.format(Locale.US, "%.1f", sub.totalDurationMinutes / 60.0)}h • V těle: ${String.format(Locale.US, "%.1f", sub.bodyPresenceHours)}h"
                                    else ->
                                        "Hlavní doba účinku: ${String.format(Locale.US, "%.1f", sub.totalDurationMinutes / 60.0)}h (Doba v těle vypnuta)"
                                }
                                Text(
                                    text = timingDesc,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                    color = glassEnv.textSecondary
                                )
                                Text(
                                    text = "${subFields.size} vlastních polí",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = subColor
                                )
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(76.dp)) }
            }
        }

        editingSubstance?.let { draft ->
            SubstanceEditorSheet(
                initialSubstance = draft,
                subtypesForSubstance = subtypes.filter { it.substanceId == draft.id && draft.id != 0L },
                customFieldsForSubstance = customFields.filter { it.substanceId == draft.id && draft.id != 0L },
                availableUnits = settings.parsedUnits(),
                currencySymbol = settings.currencySymbol,
                onDismiss = { editingSubstance = null },
                onSave = { updated ->
                    onSaveSubstance(updated)
                    editingSubstance = null
                },
                onSaveSubtype = onSaveSubtype,
                onDeleteSubtype = onDeleteSubtype,
                onSaveCustomField = onSaveCustomField,
                onDeleteCustomField = onDeleteCustomField
            )
        }
    }
}
