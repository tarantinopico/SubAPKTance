package com.antigravity.substance.data.db.entities

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Immutable
@Entity(
    tableName = "substance_subtypes",
    indices = [Index(value = ["substanceId"])]
)
data class SubstanceSubtypeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val substanceId: Long,
    val name: String, // e.g., "Double Espresso", "V60 Filter", "Green Maeng Da", "Red Bali"
    val subtitle: String = "",
    val colorHex: Long = 0xFF06B6D4L,
    val potencyMultiplier: Double = 1.0, // e.g. 1.25x stronger effective load
    val onsetMultiplier: Double = 1.0,
    val durationMultiplier: Double = 1.0,
    val customCostPerUnit: Double? = null, // null = inherit parent SubstanceEntity.costPerUnit
    val isDefault: Boolean = false
)
