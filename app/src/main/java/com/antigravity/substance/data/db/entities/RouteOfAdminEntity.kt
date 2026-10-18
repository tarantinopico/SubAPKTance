package com.antigravity.substance.data.db.entities

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Immutable
@Entity(tableName = "routes_of_admin")
data class RouteOfAdminEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val onsetMultiplier: Double = 1.0,
    val durationMultiplier: Double = 1.0,
    val bioavailabilityFactor: Double = 1.0,
    val isDefault: Boolean = false
)
