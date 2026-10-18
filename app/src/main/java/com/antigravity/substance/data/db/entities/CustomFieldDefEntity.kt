package com.antigravity.substance.data.db.entities

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Immutable
@Entity(tableName = "custom_field_definitions")
data class CustomFieldDefEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val substanceId: Long? = null, // null means global custom field
    val fieldName: String,
    val fieldType: String, // SINGLE_CHOICE_CHIPS, MULTI_SELECT_TAGS, SLIDER_1_TO_10, BOOLEAN_SWITCH, NUMERIC_VALUE, TEXT_NOTE
    val optionsCsv: String = "",
    val defaultValue: String = "",
    val sortOrder: Int = 0
) {
    fun parsedOptions(): List<String> =
        optionsCsv.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
}
