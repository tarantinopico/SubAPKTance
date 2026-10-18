package com.antigravity.substance.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.antigravity.substance.data.db.dao.IntakeLogDao
import com.antigravity.substance.data.db.dao.SubstanceDao
import com.antigravity.substance.data.db.entities.CustomFieldDefEntity
import com.antigravity.substance.data.db.entities.IntakeCustomFieldValueEntity
import com.antigravity.substance.data.db.entities.IntakeLogEntity
import com.antigravity.substance.data.db.entities.RouteOfAdminEntity
import com.antigravity.substance.data.db.entities.SubstanceEntity
import com.antigravity.substance.data.db.entities.SubstanceSubtypeEntity
import kotlin.math.roundToInt
import kotlin.random.Random

@Database(
    entities = [
        SubstanceEntity::class,
        SubstanceSubtypeEntity::class,
        CustomFieldDefEntity::class,
        RouteOfAdminEntity::class,
        IntakeLogEntity::class,
        IntakeCustomFieldValueEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class SubstanceDatabase : RoomDatabase() {
    abstract fun substanceDao(): SubstanceDao
    abstract fun intakeLogDao(): IntakeLogDao

    companion object {
        @Volatile
        private var INSTANCE: SubstanceDatabase? = null

        fun getInstance(context: Context): SubstanceDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SubstanceDatabase::class.java,
                    "substance_cockpit.db"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        fun defaultRoutesOfAdmin(): List<RouteOfAdminEntity> = listOf(
            RouteOfAdminEntity(
                id = 1L,
                name = "Oral",
                onsetMultiplier = 1.0,
                durationMultiplier = 1.0,
                bioavailabilityFactor = 1.0,
                isDefault = true
            ),
            RouteOfAdminEntity(
                id = 2L,
                name = "Sublingual",
                onsetMultiplier = 0.65,
                durationMultiplier = 0.85,
                bioavailabilityFactor = 1.20,
                isDefault = false
            ),
            RouteOfAdminEntity(
                id = 3L,
                name = "Infusion / Tea",
                onsetMultiplier = 0.78,
                durationMultiplier = 0.92,
                bioavailabilityFactor = 1.05,
                isDefault = false
            ),
            RouteOfAdminEntity(
                id = 4L,
                name = "Vaporized",
                onsetMultiplier = 0.30,
                durationMultiplier = 0.55,
                bioavailabilityFactor = 1.35,
                isDefault = false
            ),
            RouteOfAdminEntity(
                id = 5L,
                name = "Insufflated",
                onsetMultiplier = 0.45,
                durationMultiplier = 0.65,
                bioavailabilityFactor = 1.25,
                isDefault = false
            )
        )

        fun defaultSubstances(): List<SubstanceEntity> = listOf(
            SubstanceEntity(
                id = 1L,
                name = "Caffeine",
                subtitle = "Methylxanthine • Adenosine Antagonist",
                category = "Stimulant",
                iconName = "coffee",
                colorHex = 0xFF0284C7L, // Professional Ocean Cyan
                defaultUnit = "mg",
                stepSize = 20.0,
                defaultDose = 100.0,
                quickPresetsCsv = "40,80,120,160,200,300",
                costPerUnit = 0.22,
                thresholdNegligible = 35.0,
                thresholdLight = 90.0,
                thresholdMedium = 175.0,
                thresholdStrong = 280.0,
                thresholdDangerous = 420.0,
                dailyMaxSafeLimit = 400.0,
                recommendedMinIntervalMinutes = 180,
                timingMode = "DETAILED",
                mainEffectDurationMinutes = 300,
                onsetMinutes = 15,
                comeupMinutes = 30,
                peakMinutes = 90,
                offsetMinutes = 165,
                hasBodyPresenceDuration = true,
                bodyPresenceHours = 12.0,
                halfLifeHours = 5.0,
                sleepDisruptionThreshold = 45.0,
                stackingBehavior = "FULL_SUPERPOSITION",
                toleranceGainRate = 1.0,
                toleranceDecayRate = 1.0,
                bioLoadWeight = 1.0,
                superpositionWeight = 1.0,
                isFavorite = true,
                sortOrder = 0
            ),
            SubstanceEntity(
                id = 2L,
                name = "Kratom",
                subtitle = "Mitragyna speciosa • Alkaloid Botanical",
                category = "Botanical",
                iconName = "leaf",
                colorHex = 0xFF059669L, // Clinical Botanical Emerald
                defaultUnit = "g",
                stepSize = 0.5,
                defaultDose = 3.0,
                quickPresetsCsv = "1.5,2.5,3.5,4.5,6.0,7.5",
                costPerUnit = 3.20,
                thresholdNegligible = 1.0,
                thresholdLight = 2.5,
                thresholdMedium = 4.5,
                thresholdStrong = 6.8,
                thresholdDangerous = 9.5,
                dailyMaxSafeLimit = 10.0,
                recommendedMinIntervalMinutes = 240,
                timingMode = "DETAILED",
                mainEffectDurationMinutes = 320,
                onsetMinutes = 20,
                comeupMinutes = 35,
                peakMinutes = 110,
                offsetMinutes = 155,
                hasBodyPresenceDuration = true,
                bodyPresenceHours = 10.5,
                halfLifeHours = 3.8,
                sleepDisruptionThreshold = 1.5,
                stackingBehavior = "FULL_SUPERPOSITION",
                toleranceGainRate = 1.15,
                toleranceDecayRate = 0.90,
                bioLoadWeight = 1.1,
                superpositionWeight = 1.0,
                isFavorite = true,
                sortOrder = 1
            ),
            SubstanceEntity(
                id = 3L,
                name = "Nicotine",
                subtitle = "Nicotinic Cholinergic Agonist",
                category = "Stimulant",
                iconName = "bolt",
                colorHex = 0xFFD97706L, // Warm Amber
                defaultUnit = "mg",
                stepSize = 1.0,
                defaultDose = 4.0,
                quickPresetsCsv = "2,4,6,8,12,16",
                costPerUnit = 1.60,
                thresholdNegligible = 1.5,
                thresholdLight = 4.0,
                thresholdMedium = 7.5,
                thresholdStrong = 12.0,
                thresholdDangerous = 18.0,
                dailyMaxSafeLimit = 32.0,
                recommendedMinIntervalMinutes = 90,
                timingMode = "SIMPLE",
                mainEffectDurationMinutes = 90,
                onsetMinutes = 3,
                comeupMinutes = 10,
                peakMinutes = 25,
                offsetMinutes = 52,
                hasBodyPresenceDuration = true,
                bodyPresenceHours = 6.5,
                halfLifeHours = 2.0,
                sleepDisruptionThreshold = 2.5,
                stackingBehavior = "FULL_SUPERPOSITION",
                toleranceGainRate = 1.25,
                toleranceDecayRate = 1.10,
                bioLoadWeight = 0.9,
                superpositionWeight = 0.95,
                isFavorite = true,
                sortOrder = 2
            ),
            SubstanceEntity(
                id = 4L,
                name = "L-Theanine",
                subtitle = "Gamma-Glutamylethylamide • Nootropic",
                category = "Nootropic",
                iconName = "brain",
                colorHex = 0xFF7C3AEDL, // Deep Violet
                defaultUnit = "mg",
                stepSize = 50.0,
                defaultDose = 200.0,
                quickPresetsCsv = "100,150,200,300,400",
                costPerUnit = 0.04,
                thresholdNegligible = 50.0,
                thresholdLight = 120.0,
                thresholdMedium = 250.0,
                thresholdStrong = 400.0,
                thresholdDangerous = 600.0,
                dailyMaxSafeLimit = 600.0,
                recommendedMinIntervalMinutes = 240,
                timingMode = "SIMPLE",
                mainEffectDurationMinutes = 320,
                onsetMinutes = 25,
                comeupMinutes = 35,
                peakMinutes = 120,
                offsetMinutes = 140,
                hasBodyPresenceDuration = true,
                bodyPresenceHours = 7.5,
                halfLifeHours = 2.5,
                sleepDisruptionThreshold = 999.0,
                stackingBehavior = "FULL_SUPERPOSITION",
                toleranceGainRate = 0.4,
                toleranceDecayRate = 1.5,
                bioLoadWeight = 0.5,
                superpositionWeight = 1.0,
                isFavorite = true,
                sortOrder = 3
            ),
            SubstanceEntity(
                id = 5L,
                name = "Alcohol",
                subtitle = "Ethanol • GABAergic Depressant",
                category = "Depressant",
                iconName = "glass",
                colorHex = 0xFFE11D48L, // Crimson
                defaultUnit = "drinks",
                stepSize = 0.5,
                defaultDose = 1.0,
                quickPresetsCsv = "0.5,1.0,1.5,2.0,3.0,4.5",
                costPerUnit = 65.0,
                thresholdNegligible = 0.5,
                thresholdLight = 1.5,
                thresholdMedium = 3.0,
                thresholdStrong = 4.5,
                thresholdDangerous = 6.5,
                dailyMaxSafeLimit = 3.0,
                recommendedMinIntervalMinutes = 60,
                timingMode = "DETAILED",
                mainEffectDurationMinutes = 277,
                onsetMinutes = 12,
                comeupMinutes = 25,
                peakMinutes = 60,
                offsetMinutes = 180,
                hasBodyPresenceDuration = true,
                bodyPresenceHours = 11.0,
                halfLifeHours = 4.2,
                sleepDisruptionThreshold = 1.0,
                stackingBehavior = "FULL_SUPERPOSITION",
                toleranceGainRate = 0.9,
                toleranceDecayRate = 1.0,
                bioLoadWeight = 1.3,
                superpositionWeight = 1.0,
                isFavorite = false,
                sortOrder = 4
            ),
            SubstanceEntity(
                id = 6L,
                name = "Magnesium",
                subtitle = "Magnesium Bisglycinate • Recovery Mineral",
                category = "Supplement",
                iconName = "capsule",
                colorHex = 0xFF0EA5E9L, // Sky Blue
                defaultUnit = "mg",
                stepSize = 50.0,
                defaultDose = 300.0,
                quickPresetsCsv = "150,200,300,400,500",
                costPerUnit = 0.025,
                thresholdNegligible = 100.0,
                thresholdLight = 200.0,
                thresholdMedium = 350.0,
                thresholdStrong = 500.0,
                thresholdDangerous = 750.0,
                dailyMaxSafeLimit = 500.0,
                recommendedMinIntervalMinutes = 360,
                timingMode = "SIMPLE",
                mainEffectDurationMinutes = 360,
                onsetMinutes = 35,
                comeupMinutes = 45,
                peakMinutes = 150,
                offsetMinutes = 130,
                hasBodyPresenceDuration = false, // Demonstrates optional body presence disabled
                bodyPresenceHours = 14.0,
                halfLifeHours = 6.0,
                sleepDisruptionThreshold = 999.0,
                stackingBehavior = "FULL_SUPERPOSITION",
                toleranceGainRate = 0.2,
                toleranceDecayRate = 2.0,
                bioLoadWeight = 0.3,
                superpositionWeight = 1.0,
                isFavorite = false,
                sortOrder = 5
            )
        )

        fun defaultSubtypes(): List<SubstanceSubtypeEntity> = listOf(
            // Caffeine Subtypes (substanceId = 1L)
            SubstanceSubtypeEntity(
                id = 1L,
                substanceId = 1L,
                name = "Double Espresso",
                subtitle = "Fast extraction • High peak",
                colorHex = 0xFF0284C7L,
                potencyMultiplier = 1.10,
                onsetMultiplier = 0.85,
                durationMultiplier = 0.92,
                customCostPerUnit = 0.28,
                isDefault = true
            ),
            SubstanceSubtypeEntity(
                id = 2L,
                substanceId = 1L,
                name = "V60 Pour-Over",
                subtitle = "Clean filter curve",
                colorHex = 0xFF0EA5E9L,
                potencyMultiplier = 1.00,
                onsetMultiplier = 1.00,
                durationMultiplier = 1.05,
                customCostPerUnit = 0.18,
                isDefault = false
            ),
            SubstanceSubtypeEntity(
                id = 3L,
                substanceId = 1L,
                name = "Matcha Ceremonial",
                subtitle = "Sustained catechins + L-Theanine",
                colorHex = 0xFF10B981L,
                potencyMultiplier = 0.95,
                onsetMultiplier = 1.15,
                durationMultiplier = 1.25,
                customCostPerUnit = 0.32,
                isDefault = false
            ),
            SubstanceSubtypeEntity(
                id = 4L,
                substanceId = 1L,
                name = "Cold Brew Concentrate",
                subtitle = "High-caffeine cold steep",
                colorHex = 0xFF3B82F6L,
                potencyMultiplier = 1.15,
                onsetMultiplier = 0.90,
                durationMultiplier = 1.10,
                customCostPerUnit = 0.24,
                isDefault = false
            ),

            // Kratom Subtypes (substanceId = 2L)
            SubstanceSubtypeEntity(
                id = 5L,
                substanceId = 2L,
                name = "Green Maeng Da",
                subtitle = "Balanced focus & mood alkaloid ratio",
                colorHex = 0xFF059669L,
                potencyMultiplier = 1.15,
                onsetMultiplier = 0.95,
                durationMultiplier = 1.05,
                customCostPerUnit = 3.40,
                isDefault = true
            ),
            SubstanceSubtypeEntity(
                id = 6L,
                substanceId = 2L,
                name = "White Borneo",
                subtitle = "Energetic daytime cultivar",
                colorHex = 0xFF14B8A6L,
                potencyMultiplier = 1.10,
                onsetMultiplier = 0.88,
                durationMultiplier = 0.95,
                customCostPerUnit = 3.20,
                isDefault = false
            ),
            SubstanceSubtypeEntity(
                id = 7L,
                substanceId = 2L,
                name = "Red Bali",
                subtitle = "Relaxing evening profile",
                colorHex = 0xFFE11D48L,
                potencyMultiplier = 0.95,
                onsetMultiplier = 1.10,
                durationMultiplier = 1.15,
                customCostPerUnit = 3.00,
                isDefault = false
            ),
            SubstanceSubtypeEntity(
                id = 8L,
                substanceId = 2L,
                name = "Gold Extract 2x",
                subtitle = "Concentrated standardized extract",
                colorHex = 0xFFD97706L,
                potencyMultiplier = 1.45,
                onsetMultiplier = 0.80,
                durationMultiplier = 1.10,
                customCostPerUnit = 5.50,
                isDefault = false
            ),

            // Nicotine Subtypes (substanceId = 3L)
            SubstanceSubtypeEntity(
                id = 9L,
                substanceId = 3L,
                name = "Mint Pouch 4mg",
                subtitle = "Clean oral nicotine pouch",
                colorHex = 0xFFD97706L,
                potencyMultiplier = 1.00,
                onsetMultiplier = 1.00,
                durationMultiplier = 1.00,
                customCostPerUnit = 1.50,
                isDefault = true
            ),
            SubstanceSubtypeEntity(
                id = 10L,
                substanceId = 3L,
                name = "Freeze Strong Pouch",
                subtitle = "Rapid pH-buffered release",
                colorHex = 0xFFF59E0BL,
                potencyMultiplier = 1.20,
                onsetMultiplier = 0.80,
                durationMultiplier = 0.95,
                customCostPerUnit = 1.75,
                isDefault = false
            ),

            // L-Theanine Subtypes (substanceId = 4L)
            SubstanceSubtypeEntity(
                id = 11L,
                substanceId = 4L,
                name = "Suntheanine Sublingual",
                subtitle = "Pure L-isomer powder",
                colorHex = 0xFF7C3AEDL,
                potencyMultiplier = 1.10,
                onsetMultiplier = 0.85,
                durationMultiplier = 1.00,
                customCostPerUnit = 0.045,
                isDefault = true
            ),
            SubstanceSubtypeEntity(
                id = 12L,
                substanceId = 4L,
                name = "Standard Capsule",
                subtitle = "Oral veggie capsule",
                colorHex = 0xFF8B5CF6L,
                potencyMultiplier = 1.00,
                onsetMultiplier = 1.00,
                durationMultiplier = 1.00,
                customCostPerUnit = 0.035,
                isDefault = false
            ),

            // Alcohol Subtypes (substanceId = 5L)
            SubstanceSubtypeEntity(
                id = 13L,
                substanceId = 5L,
                name = "Craft Pilsner",
                subtitle = "4.8% ABV Czech Lager",
                colorHex = 0xFFE11D48L,
                potencyMultiplier = 1.00,
                onsetMultiplier = 1.00,
                durationMultiplier = 1.00,
                customCostPerUnit = 58.0,
                isDefault = true
            ),
            SubstanceSubtypeEntity(
                id = 14L,
                substanceId = 5L,
                name = "Dry Red Wine",
                subtitle = "13.5% ABV Pinot Noir",
                colorHex = 0xFFBE123CL,
                potencyMultiplier = 1.10,
                onsetMultiplier = 0.95,
                durationMultiplier = 1.08,
                customCostPerUnit = 85.0,
                isDefault = false
            ),

            // Magnesium Subtypes (substanceId = 6L)
            SubstanceSubtypeEntity(
                id = 15L,
                substanceId = 6L,
                name = "Bisglycinate Chelate",
                subtitle = "High bioavailability evening chelate",
                colorHex = 0xFF0EA5E9L,
                potencyMultiplier = 1.10,
                onsetMultiplier = 1.00,
                durationMultiplier = 1.05,
                customCostPerUnit = 0.025,
                isDefault = true
            ),
            SubstanceSubtypeEntity(
                id = 16L,
                substanceId = 6L,
                name = "Magnesium L-Threonate",
                subtitle = "Blood-brain barrier nootropic form",
                colorHex = 0xFF0284C7L,
                potencyMultiplier = 1.25,
                onsetMultiplier = 0.90,
                durationMultiplier = 1.10,
                customCostPerUnit = 0.048,
                isDefault = false
            )
        )

        fun defaultCustomFieldDefs(): List<CustomFieldDefEntity> = listOf(
            CustomFieldDefEntity(
                id = 1L,
                substanceId = null, // Global
                fieldName = "Stomach Fullness",
                fieldType = "SINGLE_CHOICE_CHIPS",
                optionsCsv = "Empty,Light Meal,Full Meal",
                defaultValue = "Empty",
                sortOrder = 0
            ),
            CustomFieldDefEntity(
                id = 2L,
                substanceId = null, // Global
                fieldName = "Subjective Focus",
                fieldType = "SLIDER_1_TO_10",
                optionsCsv = "1,10",
                defaultValue = "8",
                sortOrder = 1
            ),
            CustomFieldDefEntity(
                id = 3L,
                substanceId = 2L, // Kratom
                fieldName = "Potentiator Used",
                fieldType = "BOOLEAN_SWITCH",
                optionsCsv = "Lemon / Citric,None",
                defaultValue = "true",
                sortOrder = 2
            ),
            CustomFieldDefEntity(
                id = 4L,
                substanceId = 1L, // Caffeine
                fieldName = "Extraction Temp",
                fieldType = "SINGLE_CHOICE_CHIPS",
                optionsCsv = "93°C Hot,Iced,Cold Steep",
                defaultValue = "93°C Hot",
                sortOrder = 2
            )
        )

        /**
         * Generates 30 days of realistic multi-substance & multi-subtype logs so that:
         * 1. Live active doses with stacked peaks are visible right now on the Cockpit.
         * 2. All 5 sub-pages of Global Analytics and all 5 sub-pages of Per-Substance Analytics
         *    (including Subtype comparisons, Dose-vs-Interval correlations, Next-Interval-vs-Dose,
         *    and Active Effect vs Body Presence curves) are immediately populated with rich data.
         */
        fun buildDemoLogs(nowMillis: Long = System.currentTimeMillis()): List<IntakeLogEntity> {
            val logs = mutableListOf<IntakeLogEntity>()
            val random = Random(424242L)
            val minuteMs = 60_000L
            val hourMs = 60 * minuteMs
            val dayMs = 24 * hourMs

            val contextTags = listOf("Work", "Focus", "Gym", "Study", "Social", "Morning")
            val stomachs = listOf("Empty", "Light Meal", "Full Meal")
            val subtypes = defaultSubtypes().groupBy { it.substanceId }

            // 1. TODAY'S ACTIVE & STACKED LOGS
            // Caffeine Dose #1: V60 Pour-Over taken 115 minutes ago (90 mg)
            logs.add(
                IntakeLogEntity(
                    substanceId = 1L,
                    subtypeId = 2L,
                    subtypeName = "V60 Pour-Over",
                    potencyMultiplier = 1.00,
                    timestampMillis = nowMillis - 115 * minuteMs,
                    doseAmount = 90.0,
                    unit = "mg",
                    cost = 90.0 * 0.18,
                    roaName = "Oral",
                    bioavailabilityFactor = 1.0,
                    onsetMultiplier = 1.0,
                    durationMultiplier = 1.05,
                    evaluatedTier = "LIGHT",
                    stackedPeakTier = "LIGHT",
                    contextTag = "Morning",
                    subjectiveRating = 8,
                    notes = "Morning V60 pour-over",
                    customFieldsMapStr = "Stomach Fullness=Empty;Subjective Focus=8;Extraction Temp=93°C Hot"
                )
            )
            // Caffeine Dose #2 (STACKED REDOSE!): Double Espresso taken 28 minutes ago (120 mg)
            logs.add(
                IntakeLogEntity(
                    substanceId = 1L,
                    subtypeId = 1L,
                    subtypeName = "Double Espresso",
                    potencyMultiplier = 1.10,
                    timestampMillis = nowMillis - 28 * minuteMs,
                    doseAmount = 120.0,
                    unit = "mg",
                    cost = 120.0 * 0.28,
                    roaName = "Oral",
                    bioavailabilityFactor = 1.0,
                    onsetMultiplier = 0.85,
                    durationMultiplier = 0.92,
                    evaluatedTier = "MEDIUM",
                    stackedPeakTier = "STRONG",
                    contextTag = "Work",
                    subjectiveRating = 9,
                    notes = "Stacked double espresso before deep work sprint",
                    customFieldsMapStr = "Stomach Fullness=Light Meal;Subjective Focus=9;Extraction Temp=93°C Hot"
                )
            )
            // L-Theanine Synergistic Stack: Suntheanine Sublingual taken 26 minutes ago (200 mg)
            logs.add(
                IntakeLogEntity(
                    substanceId = 4L,
                    subtypeId = 11L,
                    subtypeName = "Suntheanine Sublingual",
                    potencyMultiplier = 1.10,
                    timestampMillis = nowMillis - 26 * minuteMs,
                    doseAmount = 200.0,
                    unit = "mg",
                    cost = 200.0 * 0.045,
                    roaName = "Sublingual",
                    bioavailabilityFactor = 1.2,
                    onsetMultiplier = 0.65,
                    durationMultiplier = 0.85,
                    evaluatedTier = "MEDIUM",
                    stackedPeakTier = "MEDIUM",
                    contextTag = "Focus",
                    subjectiveRating = 9,
                    notes = "2:1 L-Theanine synergy stack",
                    customFieldsMapStr = "Stomach Fullness=Light Meal;Subjective Focus=9"
                )
            )
            // Kratom Active Session: Green Maeng Da taken 65 minutes ago (3.5 g)
            logs.add(
                IntakeLogEntity(
                    substanceId = 2L,
                    subtypeId = 5L,
                    subtypeName = "Green Maeng Da",
                    potencyMultiplier = 1.15,
                    timestampMillis = nowMillis - 65 * minuteMs,
                    doseAmount = 3.5,
                    unit = "g",
                    cost = 3.5 * 3.40,
                    roaName = "Infusion / Tea",
                    bioavailabilityFactor = 1.05,
                    onsetMultiplier = 0.78,
                    durationMultiplier = 0.92,
                    evaluatedTier = "MEDIUM",
                    stackedPeakTier = "MEDIUM",
                    contextTag = "Focus",
                    subjectiveRating = 9,
                    notes = "Green Maeng Da warm lemon infusion",
                    customFieldsMapStr = "Stomach Fullness=Empty;Subjective Focus=9;Potentiator Used=true"
                )
            )
            // Nicotine Pouch earlier today: Mint Pouch 4mg taken 200 minutes ago
            logs.add(
                IntakeLogEntity(
                    substanceId = 3L,
                    subtypeId = 9L,
                    subtypeName = "Mint Pouch 4mg",
                    potencyMultiplier = 1.00,
                    timestampMillis = nowMillis - 200 * minuteMs,
                    doseAmount = 4.0,
                    unit = "mg",
                    cost = 4.0 * 1.50,
                    roaName = "Sublingual",
                    bioavailabilityFactor = 1.2,
                    onsetMultiplier = 0.65,
                    durationMultiplier = 0.85,
                    evaluatedTier = "LIGHT",
                    stackedPeakTier = "LIGHT",
                    contextTag = "Work",
                    subjectiveRating = 8,
                    notes = "Clean focus pouch",
                    customFieldsMapStr = "Stomach Fullness=Empty;Subjective Focus=8"
                )
            )

            // 2. PAST 29 DAYS OF REALISTIC MULTI-SUBSTANCE & MULTI-SUBTYPE TELEMETRY
            for (daysAgo in 1..29) {
                val dayBase = nowMillis - daysAgo * dayMs
                val cSubs = subtypes[1L].orEmpty()
                val kSubs = subtypes[2L].orEmpty()
                val nSubs = subtypes[3L].orEmpty()
                val tSubs = subtypes[4L].orEmpty()
                val aSubs = subtypes[5L].orEmpty()
                val mSubs = subtypes[6L].orEmpty()

                // Morning Caffeine (almost daily)
                if (daysAgo % 9 != 0 && cSubs.isNotEmpty()) {
                    val chosenSub = cSubs[random.nextInt(cSubs.size)]
                    val morningDose = listOf(80.0, 100.0, 120.0, 140.0, 160.0)[random.nextInt(5)]
                    val stomach = stomachs[random.nextInt(stomachs.size)]
                    val focus = 7 + random.nextInt(4)
                    val firstTime = dayBase - (3 + random.nextInt(3)) * hourMs
                    val unitCost = chosenSub.customCostPerUnit ?: 0.22
                    logs.add(
                        IntakeLogEntity(
                            substanceId = 1L,
                            subtypeId = chosenSub.id,
                            subtypeName = chosenSub.name,
                            potencyMultiplier = chosenSub.potencyMultiplier,
                            timestampMillis = firstTime,
                            doseAmount = morningDose,
                            unit = "mg",
                            cost = (morningDose * unitCost * 100).roundToInt() / 100.0,
                            roaName = if (chosenSub.name.contains("Matcha")) "Infusion / Tea" else "Oral",
                            bioavailabilityFactor = 1.0,
                            onsetMultiplier = chosenSub.onsetMultiplier,
                            durationMultiplier = chosenSub.durationMultiplier,
                            evaluatedTier = if (morningDose <= 90) "LIGHT" else "MEDIUM",
                            stackedPeakTier = if (morningDose <= 90) "LIGHT" else "MEDIUM",
                            contextTag = contextTags[random.nextInt(contextTags.size)],
                            subjectiveRating = focus,
                            notes = "Daily ${chosenSub.name}",
                            customFieldsMapStr = "Stomach Fullness=$stomach;Subjective Focus=$focus;Extraction Temp=93°C Hot"
                        )
                    )

                    // Afternoon Caffeine Redose (models correlation between interval and dose size)
                    if (daysAgo % 2 == 0 || daysAgo % 5 == 0) {
                        val secondSubtype = cSubs[random.nextInt(cSubs.size)]
                        val delayMins = 100 + random.nextInt(200) // 1.6h to 5h after first dose
                        // Realistic correlation: shorter interval often paired with booster dose
                        val secondDose = if (delayMins < 180) {
                            listOf(100.0, 120.0, 160.0)[random.nextInt(3)]
                        } else {
                            listOf(60.0, 80.0, 100.0)[random.nextInt(3)]
                        }
                        val combinedEst = morningDose * 0.55 + secondDose * secondSubtype.potencyMultiplier
                        val uCost2 = secondSubtype.customCostPerUnit ?: 0.22
                        logs.add(
                            IntakeLogEntity(
                                substanceId = 1L,
                                subtypeId = secondSubtype.id,
                                subtypeName = secondSubtype.name,
                                potencyMultiplier = secondSubtype.potencyMultiplier,
                                timestampMillis = firstTime + delayMins * minuteMs,
                                doseAmount = secondDose,
                                unit = "mg",
                                cost = (secondDose * uCost2 * 100).roundToInt() / 100.0,
                                roaName = "Oral",
                                bioavailabilityFactor = 1.0,
                                onsetMultiplier = secondSubtype.onsetMultiplier,
                                durationMultiplier = secondSubtype.durationMultiplier,
                                evaluatedTier = if (secondDose <= 90) "LIGHT" else "MEDIUM",
                                stackedPeakTier = if (combinedEst > 280) "DANGEROUS" else if (combinedEst > 175) "STRONG" else "MEDIUM",
                                contextTag = "Work",
                                subjectiveRating = 8,
                                notes = "Afternoon ${secondSubtype.name} stack",
                                customFieldsMapStr = "Stomach Fullness=Light Meal;Subjective Focus=8;Extraction Temp=93°C Hot"
                            )
                        )
                    }
                }

                // Kratom (taken ~5 days a week across different strains/subtypes)
                if (daysAgo % 3 != 0 && kSubs.isNotEmpty()) {
                    val kSub = kSubs[random.nextInt(kSubs.size)]
                    val kDose = listOf(2.0, 2.5, 3.0, 3.5, 4.0, 5.0)[random.nextInt(6)]
                    val stomach = stomachs[random.nextInt(stomachs.size)]
                    val focus = if (stomach == "Empty") 8 + random.nextInt(3) else 6 + random.nextInt(3)
                    val roa = if (daysAgo % 2 == 0) "Infusion / Tea" else "Oral"
                    val kTime = dayBase - (1 + random.nextInt(4)) * hourMs
                    val kCost = kSub.customCostPerUnit ?: 3.20
                    logs.add(
                        IntakeLogEntity(
                            substanceId = 2L,
                            subtypeId = kSub.id,
                            subtypeName = kSub.name,
                            potencyMultiplier = kSub.potencyMultiplier,
                            timestampMillis = kTime,
                            doseAmount = kDose,
                            unit = "g",
                            cost = (kDose * kCost * 100).roundToInt() / 100.0,
                            roaName = roa,
                            bioavailabilityFactor = if (roa == "Infusion / Tea") 1.05 else 1.0,
                            onsetMultiplier = kSub.onsetMultiplier,
                            durationMultiplier = kSub.durationMultiplier,
                            evaluatedTier = when {
                                kDose <= 2.5 -> "LIGHT"
                                kDose <= 4.5 -> "MEDIUM"
                                else -> "STRONG"
                            },
                            stackedPeakTier = if (kDose <= 2.5) "LIGHT" else if (kDose <= 4.5) "MEDIUM" else "STRONG",
                            contextTag = if (kSub.name.contains("White")) "Gym" else "Focus",
                            subjectiveRating = focus.coerceIn(1, 10),
                            notes = "${kSub.name} botanical session",
                            customFieldsMapStr = "Stomach Fullness=$stomach;Subjective Focus=${focus.coerceIn(1, 10)};Potentiator Used=${daysAgo % 2 == 0}"
                        )
                    )

                    if (daysAgo % 4 == 1) {
                        val redBali = kSubs.firstOrNull { it.name.contains("Red") } ?: kSub
                        val kRedose = listOf(2.0, 2.5, 3.5)[random.nextInt(3)]
                        val gapMins = 140 + random.nextInt(120)
                        logs.add(
                            IntakeLogEntity(
                                substanceId = 2L,
                                subtypeId = redBali.id,
                                subtypeName = redBali.name,
                                potencyMultiplier = redBali.potencyMultiplier,
                                timestampMillis = kTime + gapMins * minuteMs,
                                doseAmount = kRedose,
                                unit = "g",
                                cost = (kRedose * (redBali.customCostPerUnit ?: 3.0) * 100).roundToInt() / 100.0,
                                roaName = "Oral",
                                bioavailabilityFactor = 1.0,
                                onsetMultiplier = redBali.onsetMultiplier,
                                durationMultiplier = redBali.durationMultiplier,
                                evaluatedTier = "LIGHT",
                                stackedPeakTier = "STRONG",
                                contextTag = "Social",
                                subjectiveRating = 8,
                                notes = "Evening ${redBali.name} stack",
                                customFieldsMapStr = "Stomach Fullness=Light Meal;Subjective Focus=8;Potentiator Used=false"
                            )
                        )
                    }
                }

                // L-Theanine
                if ((daysAgo % 2 == 0 || daysAgo % 3 == 0) && tSubs.isNotEmpty()) {
                    val tSub = tSubs[daysAgo % tSubs.size]
                    val tDose = listOf(150.0, 200.0, 250.0)[random.nextInt(3)]
                    logs.add(
                        IntakeLogEntity(
                            substanceId = 4L,
                            subtypeId = tSub.id,
                            subtypeName = tSub.name,
                            potencyMultiplier = tSub.potencyMultiplier,
                            timestampMillis = dayBase - 3 * hourMs + 15 * minuteMs,
                            doseAmount = tDose,
                            unit = "mg",
                            cost = (tDose * (tSub.customCostPerUnit ?: 0.04) * 100).roundToInt() / 100.0,
                            roaName = "Sublingual",
                            bioavailabilityFactor = 1.2,
                            onsetMultiplier = 0.65,
                            durationMultiplier = 0.85,
                            evaluatedTier = "MEDIUM",
                            stackedPeakTier = "MEDIUM",
                            contextTag = "Focus",
                            subjectiveRating = 9,
                            notes = "Alpha-wave synergy stack",
                            customFieldsMapStr = "Stomach Fullness=Empty;Subjective Focus=9"
                        )
                    )
                }

                // Nicotine
                if (daysAgo % 2 == 1 && nSubs.isNotEmpty()) {
                    val nSub = nSubs[daysAgo % nSubs.size]
                    val nDose = listOf(4.0, 6.0, 8.0)[random.nextInt(3)]
                    logs.add(
                        IntakeLogEntity(
                            substanceId = 3L,
                            subtypeId = nSub.id,
                            subtypeName = nSub.name,
                            potencyMultiplier = nSub.potencyMultiplier,
                            timestampMillis = dayBase + 1 * hourMs,
                            doseAmount = nDose,
                            unit = "mg",
                            cost = (nDose * (nSub.customCostPerUnit ?: 1.60) * 100).roundToInt() / 100.0,
                            roaName = "Sublingual",
                            bioavailabilityFactor = 1.2,
                            onsetMultiplier = nSub.onsetMultiplier,
                            durationMultiplier = nSub.durationMultiplier,
                            evaluatedTier = if (nDose <= 4.0) "LIGHT" else if (nDose <= 7.5) "MEDIUM" else "STRONG",
                            stackedPeakTier = if (nDose <= 4.0) "LIGHT" else if (nDose <= 7.5) "MEDIUM" else "STRONG",
                            contextTag = "Study",
                            subjectiveRating = 8,
                            notes = "Deep coding session (${nSub.name})",
                            customFieldsMapStr = "Stomach Fullness=Light Meal;Subjective Focus=8"
                        )
                    )
                }

                // Alcohol
                if (daysAgo % 6 == 0 && aSubs.isNotEmpty()) {
                    val aSub = aSubs[(daysAgo / 6) % aSubs.size]
                    val aDose = listOf(1.5, 2.0, 3.0)[random.nextInt(3)]
                    logs.add(
                        IntakeLogEntity(
                            substanceId = 5L,
                            subtypeId = aSub.id,
                            subtypeName = aSub.name,
                            potencyMultiplier = aSub.potencyMultiplier,
                            timestampMillis = dayBase + 6 * hourMs,
                            doseAmount = aDose,
                            unit = "drinks",
                            cost = aDose * (aSub.customCostPerUnit ?: 65.0),
                            roaName = "Oral",
                            bioavailabilityFactor = 1.0,
                            onsetMultiplier = aSub.onsetMultiplier,
                            durationMultiplier = aSub.durationMultiplier,
                            evaluatedTier = if (aDose <= 1.5) "LIGHT" else "MEDIUM",
                            stackedPeakTier = if (aDose <= 1.5) "LIGHT" else "MEDIUM",
                            contextTag = "Social",
                            subjectiveRating = 7,
                            notes = "Evening ${aSub.name}",
                            customFieldsMapStr = "Stomach Fullness=Full Meal;Subjective Focus=7"
                        )
                    )
                }

                // Magnesium
                if (daysAgo % 3 != 2 && mSubs.isNotEmpty()) {
                    val mSub = mSubs[daysAgo % mSubs.size]
                    val mDose = listOf(200.0, 300.0, 400.0)[random.nextInt(3)]
                    logs.add(
                        IntakeLogEntity(
                            substanceId = 6L,
                            subtypeId = mSub.id,
                            subtypeName = mSub.name,
                            potencyMultiplier = mSub.potencyMultiplier,
                            timestampMillis = dayBase + 7 * hourMs,
                            doseAmount = mDose,
                            unit = "mg",
                            cost = (mDose * (mSub.customCostPerUnit ?: 0.025) * 100).roundToInt() / 100.0,
                            roaName = "Oral",
                            bioavailabilityFactor = 1.0,
                            onsetMultiplier = 1.0,
                            durationMultiplier = 1.0,
                            evaluatedTier = "MEDIUM",
                            stackedPeakTier = "MEDIUM",
                            contextTag = "Morning",
                            subjectiveRating = 8,
                            notes = "Evening recovery (${mSub.name})",
                            customFieldsMapStr = "Stomach Fullness=Light Meal;Subjective Focus=8"
                        )
                    )
                }
            }

            return logs.sortedByDescending { it.timestampMillis }
        }
    }
}
