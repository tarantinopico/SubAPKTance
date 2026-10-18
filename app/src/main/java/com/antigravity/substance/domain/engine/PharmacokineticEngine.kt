package com.antigravity.substance.domain.engine

import com.antigravity.substance.data.db.entities.IntakeLogEntity
import com.antigravity.substance.data.db.entities.RouteOfAdminEntity
import com.antigravity.substance.data.db.entities.SubstanceEntity
import com.antigravity.substance.data.db.entities.SubstanceSubtypeEntity
import com.antigravity.substance.data.preferences.UserSettings
import com.antigravity.substance.domain.model.ActiveSubstanceSession
import com.antigravity.substance.domain.model.PkCurvePoint
import com.antigravity.substance.domain.model.PkPhase
import com.antigravity.substance.domain.model.ProjectedDosePreview
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

object PharmacokineticEngine {

    private const val MINUTE_MS = 60_000L
    private const val HOUR_MS = 60 * MINUTE_MS

    /**
     * Instantaneous effective active load E_i(t) for a single dose taken at t_i.
     * Models the PRIMARY ACTIVE EFFECT DURATION (Hlavní doba účinku).
     */
    fun singleDoseEffectAt(
        deltaMinutes: Double,
        effectiveDose: Double,
        onsetMinutes: Double,
        comeupMinutes: Double,
        peakMinutes: Double,
        offsetMinutes: Double
    ): Double {
        if (deltaMinutes < 0.0 || effectiveDose <= 0.0) return 0.0
        val tOnset = onsetMinutes.coerceAtLeast(1.0)
        val tComeup = comeupMinutes.coerceAtLeast(1.0)
        val tPeak = peakMinutes.coerceAtLeast(1.0)
        val tOffset = offsetMinutes.coerceAtLeast(1.0)
        val tTotal = tOnset + tComeup + tPeak + tOffset

        if (deltaMinutes >= tTotal) return 0.0

        return when {
            deltaMinutes < tOnset -> {
                val u = (deltaMinutes / tOnset).coerceIn(0.0, 1.0)
                val hermite = u * u * (3.0 - 2.0 * u)
                effectiveDose * 0.15 * hermite
            }
            deltaMinutes < tOnset + tComeup -> {
                val u = ((deltaMinutes - tOnset) / tComeup).coerceIn(0.0, 1.0)
                val s = sin((PI / 2.0) * u)
                effectiveDose * (0.15 + 0.85 * s * s)
            }
            deltaMinutes < tOnset + tComeup + tPeak -> {
                val u = ((deltaMinutes - tOnset - tComeup) / tPeak).coerceIn(0.0, 1.0)
                val dome = 0.98 + 0.02 * sin(PI * u)
                effectiveDose * dome
            }
            else -> {
                val u = ((deltaMinutes - tOnset - tComeup - tPeak) / tOffset).coerceIn(0.0, 1.0)
                val cosineDecay = 0.5 * (1.0 + cos(PI * u))
                effectiveDose * 0.98 * cosineDecay * exp(-0.6 * u)
            }
        }
    }

    /**
     * Separate Body Presence / Plasma Elimination Curve C_i(t) (Doba setrvání v těle).
     * If hasBodyPresenceDuration is disabled on the substance, returns 0.0.
     * Uses bodyPresenceHours (or halfLifeHours) so the user can configure separate durations
     * for how long effects last vs. how long the substance remains in the body.
     */
    fun singleDosePlasmaAt(
        deltaMinutes: Double,
        effectiveDose: Double,
        substance: SubstanceEntity,
        onsetMult: Double = 1.0,
        durationMult: Double = 1.0,
        settings: UserSettings? = null
    ): Double {
        if (!substance.hasBodyPresenceDuration || !substance.isTimingEnabled) return 0.0
        if (deltaMinutes <= 0.0 || effectiveDose <= 0.0) return 0.0

        val bodyMins = (substance.bodyPresenceHours.coerceAtLeast(0.5) * 60.0) * durationMult
        if (deltaMinutes >= bodyMins * 1.35) return 0.0

        val effectiveHalfLifeMins = if (substance.bodyPresenceHours > 0.5) {
            val mult = settings?.bodyEliminationHalfLifeMultiplier ?: 4.5
            (substance.bodyPresenceHours * 60.0) / mult.coerceAtLeast(2.0)
        } else {
            substance.halfLifeHours.coerceAtLeast(0.5) * 60.0
        }

        val ke = ln(2.0) / effectiveHalfLifeMins.coerceAtLeast(10.0)
        val tMax = (substance.effectiveOnsetMinutes() * onsetMult) +
            (substance.effectiveComeupMinutes() * onsetMult) +
            0.5 * (substance.effectivePeakMinutes() * durationMult)
        val ka = max(ke * 2.2, 1.6 / tMax.coerceAtLeast(5.0))
        val rawAtTMax = exp(-ke * tMax) - exp(-ka * tMax)
        val norm = if (rawAtTMax > 1e-6) 1.0 / rawAtTMax else 1.0
        val raw = (exp(-ke * deltaMinutes) - exp(-ka * deltaMinutes)).coerceAtLeast(0.0)
        // Smoothly taper to 0 near bodyPresenceHours boundary
        val tailTaper = if (deltaMinutes > bodyMins) {
            (1.0 - (deltaMinutes - bodyMins) / (bodyMins * 0.35)).coerceIn(0.0, 1.0)
        } else 1.0
        return (effectiveDose * norm * raw * tailTaper).coerceAtLeast(0.0)
    }

    fun singleLogEffectAt(
        timestampMillis: Long,
        log: IntakeLogEntity,
        substance: SubstanceEntity,
        settings: UserSettings? = null
    ): Double {
        if (!substance.isTimingEnabled) return 0.0
        val dtMins = (timestampMillis - log.timestampMillis) / 60_000.0
        val effectiveDose = log.effectiveDoseAmount
        val sOnset = settings?.simpleOnsetRatio ?: 0.10
        val sComeup = settings?.simpleComeupRatio ?: 0.18
        val sPeak = settings?.simplePeakRatio ?: 0.37
        val sOffset = settings?.simpleOffsetRatio ?: 0.35

        return singleDoseEffectAt(
            deltaMinutes = dtMins,
            effectiveDose = effectiveDose,
            onsetMinutes = substance.effectiveOnsetMinutes(sOnset) * log.onsetMultiplier,
            comeupMinutes = substance.effectiveComeupMinutes(sComeup) * log.onsetMultiplier,
            peakMinutes = substance.effectivePeakMinutes(sPeak) * log.durationMultiplier,
            offsetMinutes = substance.effectiveOffsetMinutes(sOffset) * log.durationMultiplier
        )
    }

    fun singleLogPlasmaAt(
        timestampMillis: Long,
        log: IntakeLogEntity,
        substance: SubstanceEntity,
        settings: UserSettings? = null
    ): Double {
        if (!substance.isTimingEnabled || !substance.hasBodyPresenceDuration) return 0.0
        val dtMins = (timestampMillis - log.timestampMillis) / 60_000.0
        val effectiveDose = log.effectiveDoseAmount
        return singleDosePlasmaAt(
            deltaMinutes = dtMins,
            effectiveDose = effectiveDose,
            substance = substance,
            onsetMult = log.onsetMultiplier,
            durationMult = log.durationMultiplier,
            settings = settings
        )
    }

    /**
     * Multi-Dose Superposition: sums active effect load across doses of the same substance,
     * applying configurable superpositionWeight for overlapping doses.
     */
    fun combinedEffectAt(
        timestampMillis: Long,
        logs: List<IntakeLogEntity>,
        substance: SubstanceEntity,
        settings: UserSettings? = null
    ): Double {
        if (logs.isEmpty() || !substance.isTimingEnabled) return 0.0
        if (substance.stackingBehavior == "INDEPENDENT_TIMERS") {
            return logs.maxOfOrNull { singleLogEffectAt(timestampMillis, it, substance, settings) } ?: 0.0
        }
        val activeContributions = logs.map { singleLogEffectAt(timestampMillis, it, substance, settings) }
            .filter { it > 0.0 }
            .sortedDescending()
        if (activeContributions.isEmpty()) return 0.0

        val stackWeight = (substance.superpositionWeight * (settings?.globalSuperpositionFactor ?: 1.0))
            .coerceIn(0.3, 1.5)
        var sum = activeContributions.first()
        for (i in 1 until activeContributions.size) {
            sum += activeContributions[i] * stackWeight
        }
        return sum
    }

    fun combinedPlasmaAt(
        timestampMillis: Long,
        logs: List<IntakeLogEntity>,
        substance: SubstanceEntity,
        settings: UserSettings? = null
    ): Double {
        if (logs.isEmpty() || !substance.isTimingEnabled || !substance.hasBodyPresenceDuration) return 0.0
        var sum = 0.0
        for (log in logs) {
            sum += singleLogPlasmaAt(timestampMillis, log, substance, settings)
        }
        return sum
    }

    /**
     * Evaluates a substance's complete active multi-dose session around nowMillis.
     * Primary focus is Active Effect Duration (Hlavní doba účinku), with optional Body Presence Duration.
     */
    fun buildActiveSession(
        substance: SubstanceEntity,
        allSubstanceLogs: List<IntakeLogEntity>,
        nowMillis: Long,
        startOfTodayMillis: Long,
        settings: UserSettings? = null
    ): ActiveSubstanceSession? {
        if (!substance.isTimingEnabled) return null

        val windowStart = nowMillis - 48 * HOUR_MS
        val recentLogs = allSubstanceLogs
            .filter { it.timestampMillis in windowStart..(nowMillis + 12 * HOUR_MS) }
            .sortedBy { it.timestampMillis }

        if (recentLogs.isEmpty()) return null

        val currentActive = combinedEffectAt(nowMillis, recentLogs, substance, settings)
        val currentPlasma = combinedPlasmaAt(nowMillis, recentLogs, substance, settings)

        if (currentActive <= 0.005 && currentPlasma <= substance.thresholdNegligible * 0.05) {
            return null
        }

        val clusterLogs = recentLogs.filter {
            val endMs = it.timestampMillis + (substance.totalDurationMinutes * it.durationMultiplier * MINUTE_MS).toLong()
            endMs >= nowMillis - 2 * HOUR_MS
        }.ifEmpty { listOf(recentLogs.last()) }

        val firstLog = clusterLogs.first()
        val isolatedFirstPeakMs = firstLog.timestampMillis +
            ((substance.effectiveOnsetMinutes() * firstLog.onsetMultiplier +
                substance.effectiveComeupMinutes() * firstLog.onsetMultiplier +
                0.5 * substance.effectivePeakMinutes() * firstLog.durationMultiplier) * MINUTE_MS).toLong()

        val scanStartMs = clusterLogs.minOf { it.timestampMillis }
        val scanEndMs = clusterLogs.maxOf {
            it.timestampMillis + (substance.totalDurationMinutes * it.durationMultiplier * MINUTE_MS).toLong()
        }

        var maxCombinedEffect = 0.0
        var maxCombinedPeakMs = isolatedFirstPeakMs
        var activeEndMs = scanEndMs

        var t = scanStartMs
        while (t <= scanEndMs) {
            val eff = combinedEffectAt(t, clusterLogs, substance, settings)
            if (eff > maxCombinedEffect) {
                maxCombinedEffect = eff
                maxCombinedPeakMs = t
            }
            t += MINUTE_MS
        }

        var clearScan = max(nowMillis, maxCombinedPeakMs)
        while (clearScan <= scanEndMs + HOUR_MS) {
            if (combinedEffectAt(clearScan, clusterLogs, substance, settings) <= 0.01) {
                activeEndMs = clearScan
                break
            }
            clearScan += MINUTE_MS
        }

        val lastDoseMs = clusterLogs.maxOf { it.timestampMillis }
        val bodyClearMs = if (substance.hasBodyPresenceDuration) {
            lastDoseMs + (substance.bodyPresenceHours * HOUR_MS).toLong()
        } else {
            activeEndMs
        }

        val latestLog = clusterLogs.last()
        val latestOnsetEndMs = latestLog.timestampMillis +
            (substance.effectiveOnsetMinutes() * latestLog.onsetMultiplier * MINUTE_MS).toLong()
        val plateauHalfSpanMs = (0.45 * substance.effectivePeakMinutes() * latestLog.durationMultiplier * MINUTE_MS).toLong()
        val combinedPlateauStartMs = maxCombinedPeakMs - plateauHalfSpanMs
        val combinedPlateauEndMs = maxCombinedPeakMs + plateauHalfSpanMs

        val (phase, targetMs) = when {
            currentActive <= 0.01 -> PkPhase.RESIDUAL_HALF_LIFE to bodyClearMs
            nowMillis < latestOnsetEndMs && currentActive < latestLog.effectiveDoseAmount * 0.25 ->
                PkPhase.ONSET_COUNTDOWN to latestOnsetEndMs
            nowMillis < combinedPlateauStartMs ->
                PkPhase.CLIMBING_TO_PEAK to maxCombinedPeakMs
            nowMillis <= combinedPlateauEndMs ->
                PkPhase.PEAKING_NOW to combinedPlateauEndMs
            else ->
                PkPhase.OFFSET_COMEDOWN to activeEndMs
        }

        val countdownSec = max(0L, (targetMs - nowMillis) / 1000L)
        val peakShiftMins = ((maxCombinedPeakMs - isolatedFirstPeakMs) / MINUTE_MS).toInt()

        val graphStartMs = nowMillis - 2 * HOUR_MS
        val graphEndMs = nowMillis + 8 * HOUR_MS
        val stepMs = 5 * MINUTE_MS
        val points = ArrayList<PkCurvePoint>(125)
        var ptMs = graphStartMs
        while (ptMs <= graphEndMs) {
            val relMins = (ptMs - nowMillis) / 60_000f
            points.add(
                PkCurvePoint(
                    timestampMillis = ptMs,
                    relativeMinutesFromNow = relMins,
                    activeEffectLoad = combinedEffectAt(ptMs, recentLogs, substance, settings),
                    plasmaConcentration = combinedPlasmaAt(ptMs, recentLogs, substance, settings)
                )
            )
            ptMs += stepMs
        }

        val todayIngested = allSubstanceLogs
            .filter { it.timestampMillis >= startOfTodayMillis }
            .sumOf { it.doseAmount }

        val currentTier = ThresholdEvaluationEngine.evaluateTier(currentActive, substance)
        val peakTier = ThresholdEvaluationEngine.evaluateTier(maxCombinedEffect, substance)

        return ActiveSubstanceSession(
            substance = substance,
            activeLogs = clusterLogs,
            currentActiveLoad = currentActive,
            currentPlasmaLoad = currentPlasma,
            combinedPeakLoad = maxCombinedEffect,
            combinedPeakTimestampMillis = maxCombinedPeakMs,
            isolatedFirstPeakTimestampMillis = isolatedFirstPeakMs,
            peakShiftMinutes = peakShiftMins,
            currentTier = currentTier,
            combinedPeakTier = peakTier,
            currentPhase = phase,
            phaseCountdownSeconds = countdownSec,
            phaseTargetTimestampMillis = targetMs,
            activeEffectsEndTimestampMillis = activeEndMs,
            halfLifeClearTimestampMillis = bodyClearMs,
            totalIngestedTodayUnits = todayIngested,
            curvePoints = points,
            doseTimestampsMillis = clusterLogs.map { it.timestampMillis }
        )
    }

    /**
     * Simulates adding a candidate dose (with optional Subtype potency/cost/timing multipliers)
     * on the Log Dose Screen.
     */
    fun previewCandidateDose(
        substance: SubstanceEntity,
        existingLogs: List<IntakeLogEntity>,
        candidateDoseAmount: Double,
        roa: RouteOfAdminEntity,
        candidateTimestampMillis: Long,
        subtype: SubstanceSubtypeEntity? = null,
        settings: UserSettings? = null
    ): ProjectedDosePreview {
        val potencyMult = subtype?.potencyMultiplier ?: 1.0
        val subtypeOnsetMult = subtype?.onsetMultiplier ?: 1.0
        val subtypeDurMult = subtype?.durationMultiplier ?: 1.0
        val unitCost = subtype?.customCostPerUnit ?: substance.costPerUnit

        val totalOnsetMult = roa.onsetMultiplier * subtypeOnsetMult
        val totalDurMult = roa.durationMultiplier * subtypeDurMult
        val candidateEffective = candidateDoseAmount * roa.bioavailabilityFactor * potencyMult

        val singleTier = ThresholdEvaluationEngine.evaluateTier(candidateEffective, substance)
        val singleFraction = ThresholdEvaluationEngine.computeSpectrumFraction(candidateEffective, substance)

        if (!substance.isTimingEnabled) {
            return ProjectedDosePreview(
                singleEffectiveDose = candidateEffective,
                singleDoseTier = singleTier,
                singleSpectrumFraction = singleFraction,
                existingActiveLoadAtTarget = 0.0,
                stackedPeakLoad = candidateEffective,
                stackedPeakTier = singleTier,
                stackedSpectrumFraction = singleFraction,
                isStackingActive = false,
                peakShiftMinutes = 0,
                onsetTimestampMillis = candidateTimestampMillis,
                combinedPeakTimestampMillis = candidateTimestampMillis,
                activeClearTimestampMillis = candidateTimestampMillis,
                estimatedCost = (candidateDoseAmount * unitCost * 100.0).roundToInt() / 100.0,
                previewCurvePoints = emptyList()
            )
        }

        val windowStart = candidateTimestampMillis - 24 * HOUR_MS
        val activeCluster = existingLogs.filter {
            val endMs = it.timestampMillis + (substance.totalDurationMinutes * it.durationMultiplier * MINUTE_MS).toLong()
            it.timestampMillis >= windowStart && endMs > candidateTimestampMillis
        }

        val existingLoadAtTarget = combinedEffectAt(candidateTimestampMillis, activeCluster, substance, settings)
        val isStacking = existingLoadAtTarget > substance.thresholdNegligible * 0.15

        val syntheticLog = IntakeLogEntity(
            id = -999L,
            substanceId = substance.id,
            subtypeId = subtype?.id,
            subtypeName = subtype?.name ?: "Standard",
            potencyMultiplier = potencyMult,
            timestampMillis = candidateTimestampMillis,
            doseAmount = candidateDoseAmount,
            unit = substance.defaultUnit,
            cost = candidateDoseAmount * unitCost,
            roaName = roa.name,
            bioavailabilityFactor = roa.bioavailabilityFactor,
            onsetMultiplier = totalOnsetMult,
            durationMultiplier = totalDurMult
        )

        val combinedLogs = activeCluster + syntheticLog

        val onsetEndMs = candidateTimestampMillis +
            (substance.effectiveOnsetMinutes() * totalOnsetMult * MINUTE_MS).toLong()
        val isolatedCandidatePeakMs = candidateTimestampMillis +
            ((substance.effectiveOnsetMinutes() * totalOnsetMult +
                substance.effectiveComeupMinutes() * totalOnsetMult +
                0.5 * substance.effectivePeakMinutes() * totalDurMult) * MINUTE_MS).toLong()
        val candidateClearMs = candidateTimestampMillis +
            (substance.totalDurationMinutes * totalDurMult * MINUTE_MS).toLong()

        var baselinePeakMs = isolatedCandidatePeakMs
        if (activeCluster.isNotEmpty()) {
            var bestBase = 0.0
            val baseStart = activeCluster.minOf { it.timestampMillis }
            val baseEnd = activeCluster.maxOf {
                it.timestampMillis + (substance.totalDurationMinutes * it.durationMultiplier * MINUTE_MS).toLong()
            }
            var t = baseStart
            while (t <= baseEnd) {
                val e = combinedEffectAt(t, activeCluster, substance, settings)
                if (e > bestBase) {
                    bestBase = e
                    baselinePeakMs = t
                }
                t += MINUTE_MS
            }
        }

        val scanStart = min(candidateTimestampMillis, combinedLogs.minOf { it.timestampMillis })
        val scanEnd = max(
            candidateClearMs,
            combinedLogs.maxOf {
                it.timestampMillis + (substance.totalDurationMinutes * it.durationMultiplier * MINUTE_MS).toLong()
            }
        )

        var stackedPeakLoad = candidateEffective
        var stackedPeakMs = isolatedCandidatePeakMs
        var t = scanStart
        while (t <= scanEnd) {
            val eff = combinedEffectAt(t, combinedLogs, substance, settings)
            if (eff > stackedPeakLoad) {
                stackedPeakLoad = eff
                stackedPeakMs = t
            }
            t += MINUTE_MS
        }

        val shiftMinutes = if (isStacking) {
            ((stackedPeakMs - baselinePeakMs) / MINUTE_MS).toInt()
        } else 0

        val stackedTier = ThresholdEvaluationEngine.evaluateTier(stackedPeakLoad, substance)
        val stackedFraction = ThresholdEvaluationEngine.computeSpectrumFraction(stackedPeakLoad, substance)

        val previewPoints = ArrayList<PkCurvePoint>(75)
        val pStart = candidateTimestampMillis - 60 * MINUTE_MS
        val pEnd = candidateTimestampMillis + 360 * MINUTE_MS
        var pt = pStart
        while (pt <= pEnd) {
            previewPoints.add(
                PkCurvePoint(
                    timestampMillis = pt,
                    relativeMinutesFromNow = (pt - candidateTimestampMillis) / 60_000f,
                    activeEffectLoad = combinedEffectAt(pt, combinedLogs, substance, settings),
                    plasmaConcentration = combinedEffectAt(pt, activeCluster, substance, settings)
                )
            )
            pt += 6 * MINUTE_MS
        }

        return ProjectedDosePreview(
            singleEffectiveDose = candidateEffective,
            singleDoseTier = singleTier,
            singleSpectrumFraction = singleFraction,
            existingActiveLoadAtTarget = existingLoadAtTarget,
            stackedPeakLoad = stackedPeakLoad,
            stackedPeakTier = stackedTier,
            stackedSpectrumFraction = stackedFraction,
            isStackingActive = isStacking,
            peakShiftMinutes = shiftMinutes,
            onsetTimestampMillis = onsetEndMs,
            combinedPeakTimestampMillis = stackedPeakMs,
            activeClearTimestampMillis = scanEnd,
            estimatedCost = (candidateDoseAmount * unitCost * 100.0).roundToInt() / 100.0,
            previewCurvePoints = previewPoints
        )
    }
}
