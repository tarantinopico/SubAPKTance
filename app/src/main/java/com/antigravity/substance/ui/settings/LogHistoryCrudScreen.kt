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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import com.antigravity.substance.data.db.entities.IntakeLogEntity
import com.antigravity.substance.data.db.entities.SubstanceEntity
import com.antigravity.substance.data.preferences.UserSettings
import com.antigravity.substance.domain.engine.ThresholdEvaluationEngine
import com.antigravity.substance.domain.model.DoseTier
import com.antigravity.substance.ui.theme.FrostedBentoCard
import com.antigravity.substance.ui.theme.LocalSubstanceGlass
import com.antigravity.substance.ui.theme.TierBadgePill
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Full CRUD Intake Log History Manager (Search, Filter by Substance, Edit Dose/Subtype/Cost/Notes, Duplicate, Delete)
 */
@Composable
fun LogHistoryCrudScreen(
    logs: List<IntakeLogEntity>,
    substances: List<SubstanceEntity>,
    settings: UserSettings,
    onUpdateLog: (IntakeLogEntity) -> Unit,
    onDuplicateLog: (IntakeLogEntity) -> Unit,
    onDeleteLog: (Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glass = LocalSubstanceGlass.current
    var searchQuery by remember { mutableStateOf("") }
    var filterSubstanceId by remember { mutableLongStateOf(0L) } // 0L = All
    var editingLog by remember { mutableStateOf<IntakeLogEntity?>(null) }

    val subMap = remember(substances) { substances.associateBy { it.id } }
    val filteredLogs = remember(logs, searchQuery, filterSubstanceId, subMap) {
        logs.filter { log ->
            val matchesSub = filterSubstanceId == 0L || log.substanceId == filterSubstanceId
            val subName = subMap[log.substanceId]?.name.orEmpty()
            val matchesSearch = searchQuery.isBlank() ||
                subName.contains(searchQuery, ignoreCase = true) ||
                log.subtypeName.contains(searchQuery, ignoreCase = true) ||
                log.notes.contains(searchQuery, ignoreCase = true) ||
                log.contextTag.contains(searchQuery, ignoreCase = true) ||
                log.roaName.contains(searchQuery, ignoreCase = true) ||
                log.customFieldsMapStr.contains(searchQuery, ignoreCase = true)
            matchesSub && matchesSearch
        }
    }

    val sdf = remember { SimpleDateFormat("EEE, dd MMM HH:mm", Locale.US) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag("log_history_crud_screen")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = glass.textPrimary)
                }
                Text(
                    text = "Historie záznamů (${filteredLogs.size})",
                    style = MaterialTheme.typography.headlineMedium,
                    color = glass.textPrimary
                )
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Hledat látku, druh (variantu), tag, RoA nebo poznámku...") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Substance filter chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val allChipSelected = filterSubstanceId == 0L
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (allChipSelected) Color(0xFF06B6D4).copy(alpha = 0.25f) else glass.cardSurfaceBase)
                    .border(1.dp, if (allChipSelected) Color(0xFF06B6D4) else glass.cardBorderBase, RoundedCornerShape(8.dp))
                    .clickable { filterSubstanceId = 0L }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text("Vše (${logs.size})", style = MaterialTheme.typography.labelSmall, color = glass.textPrimary)
            }

            substances.forEach { sub ->
                val sel = filterSubstanceId == sub.id
                val col = Color(sub.colorHex)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (sel) col.copy(alpha = 0.25f) else glass.cardSurfaceBase)
                        .border(1.dp, if (sel) col else glass.cardBorderBase, RoundedCornerShape(8.dp))
                        .clickable { filterSubstanceId = sub.id }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(sub.name, style = MaterialTheme.typography.labelSmall, color = glass.textPrimary)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(filteredLogs, key = { it.id }) { log ->
                val sub = subMap[log.substanceId]
                val col = Color(sub?.colorHex ?: 0xFF06B6D4L)
                val subtypeLabel = if (log.subtypeName.isNotBlank()) " [${log.subtypeName}]" else ""
                FrostedBentoCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = col,
                    onClick = { editingLog = log }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(0.68f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${sub?.name ?: "Substance"}$subtypeLabel • ${settings.formatDose(log.doseAmount, log.unit)}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = glass.textPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                TierBadgePill(
                                    tier = DoseTier.fromName(log.stackedPeakTier.ifBlank { log.evaluatedTier }),
                                    compact = true
                                )
                            }
                            Text(
                                text = "${sdf.format(Date(log.timestampMillis))} • ${log.roaName} • ${settings.formatCurrency(log.cost)} • Rating ${log.subjectiveRating}/10",
                                style = MaterialTheme.typography.labelSmall,
                                color = glass.textSecondary
                            )
                            if (log.customFieldsMapStr.isNotBlank() || log.notes.isNotBlank()) {
                                Text(
                                    text = listOf(log.customFieldsMapStr, log.notes).filter { it.isNotBlank() }.joinToString(" | "),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    color = col
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { editingLog = log }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit log", tint = glass.textPrimary, modifier = Modifier.size(16.dp))
                            }
                            IconButton(onClick = { onDuplicateLog(log) }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate log", tint = Color(0xFF0284C7), modifier = Modifier.size(16.dp))
                            }
                            IconButton(onClick = { onDeleteLog(log.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete log", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    editingLog?.let { log ->
        val sub = subMap[log.substanceId]
        var doseStr by remember(log) { mutableStateOf(log.doseAmount.toString()) }
        var subtypeStr by remember(log) { mutableStateOf(log.subtypeName) }
        var costStr by remember(log) { mutableStateOf(log.cost.toString()) }
        var roaStr by remember(log) { mutableStateOf(log.roaName) }
        var tagStr by remember(log) { mutableStateOf(log.contextTag) }
        var ratingStr by remember(log) { mutableStateOf(log.subjectiveRating.toString()) }
        var noteStr by remember(log) { mutableStateOf(log.notes) }

        AlertDialog(
            onDismissRequest = { editingLog = null },
            title = { Text("Upravit záznam #${log.id}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = doseStr,
                        onValueChange = {
                            doseStr = it
                            val d = it.toDoubleOrNull()
                            if (d != null && sub != null) {
                                costStr = (d * sub.costPerUnit).toString()
                            }
                        },
                        label = { Text("Dávka (${log.unit})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = subtypeStr,
                        onValueChange = { subtypeStr = it },
                        label = { Text("Druh / Varianta (např. Espresso)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = costStr,
                        onValueChange = { costStr = it },
                        label = { Text("Cena (${settings.currencySymbol})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = roaStr,
                        onValueChange = { roaStr = it },
                        label = { Text("Způsob podání (RoA)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = tagStr,
                        onValueChange = { tagStr = it },
                        label = { Text("Kontextový štítek") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = ratingStr,
                        onValueChange = { ratingStr = it },
                        label = { Text("Subjektivní hodnocení (1-10)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = noteStr,
                        onValueChange = { noteStr = it },
                        label = { Text("Poznámka") }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val newDose = doseStr.toDoubleOrNull() ?: log.doseAmount
                        val newCost = costStr.toDoubleOrNull() ?: log.cost
                        val newRating = (ratingStr.toIntOrNull() ?: log.subjectiveRating).coerceIn(1, 10)
                        val newTier = if (sub != null) {
                            ThresholdEvaluationEngine.evaluateTier(newDose * log.potencyMultiplier, sub).name
                        } else log.evaluatedTier
                        onUpdateLog(
                            log.copy(
                                doseAmount = newDose,
                                subtypeName = subtypeStr.trim(),
                                cost = newCost,
                                roaName = roaStr.ifBlank { "Oral" },
                                contextTag = tagStr.ifBlank { "Focus" },
                                subjectiveRating = newRating,
                                notes = noteStr,
                                evaluatedTier = newTier,
                                stackedPeakTier = newTier
                            )
                        )
                        editingLog = null
                    }
                ) {
                    Text("Uložit")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingLog = null }) {
                    Text("Zrušit")
                }
            }
        )
    }
}
