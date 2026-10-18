package com.antigravity.substance.data.db.entities

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Immutable
@Entity(
    tableName = "intake_logs",
    indices = [
        Index(value = ["substanceId"]),
        Index(value = ["subtypeId"]),
        Index(value = ["timestampMillis"])
    ]
)
data class IntakeLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val substanceId: Long,
    val subtypeId: Long? = null,
    val subtypeName: String = "Standard",
    val potencyMultiplier: Double = 1.0,
    val timestampMillis: Long,
    val doseAmount: Double,
    val unit: String,
    val cost: Double,
    val roaName: String = "Oral",
    val bioavailabilityFactor: Double = 1.0,
    val onsetMultiplier: Double = 1.0,
    val durationMultiplier: Double = 1.0,
    val evaluatedTier: String = "MEDIUM",
    val stackedPeakTier: String = "MEDIUM",
    val contextTag: String = "Focus",
    val subjectiveRating: Int = 8, // 1..10 rating for correlation analytics
    val notes: String = "",
    val customFieldsMapStr: String = "" // Format: "Key1=Val1;Key2=Val2"
) {
    val effectiveDoseAmount: Double
        get() = doseAmount * bioavailabilityFactor * potencyMultiplier

    fun parsedCustomFields(): Map<String, String> {
        if (customFieldsMapStr.isBlank()) return emptyMap()
        return customFieldsMapStr.split(";")
            .mapNotNull { entry ->
                val idx = entry.indexOf('=')
                if (idx > 0) {
                    entry.substring(0, idx).trim() to entry.substring(idx + 1).trim()
                } else null
            }.toMap()
    }
}
