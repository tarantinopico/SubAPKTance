package com.antigravity.substance.data.db.entities

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Immutable
@Entity(
    tableName = "intake_custom_field_values",
    indices = [
        Index(value = ["logId"]),
        Index(value = ["fieldDefId"])
    ]
)
data class IntakeCustomFieldValueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val logId: Long,
    val fieldDefId: Long,
    val fieldName: String,
    val fieldValue: String
)
