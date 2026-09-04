package fr.suivimuscu.app

import fr.suivimuscu.app.data.BodyWeightEntry
import fr.suivimuscu.app.data.BodyWeightTrendPoint
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

internal fun normalizedWeightKg(value: String): Double? {    val parsed = value.trim().replace(',', '.').toDoubleOrNull() ?: return null
    if (!parsed.isFinite() || parsed <= 0.0 || parsed > 500.0) return null
    return (parsed * 10.0).roundToInt() / 10.0
}

internal fun previousBodyWeightEntry(
    entries: List<BodyWeightEntry>,
    date: LocalDate,
): BodyWeightEntry? = entries
    .mapNotNull { entry -> runCatching { LocalDate.parse(entry.date) }.getOrNull()?.let { it to entry } }
    .filter { it.first.isBefore(date) }
    .maxWithOrNull(compareBy<Pair<LocalDate, BodyWeightEntry>> { it.first }.thenBy { it.second.updatedAt })
    ?.second

internal fun calculateBodyWeightTrend(
    entries: List<BodyWeightEntry>,
    weeks: Int?,
    today: LocalDate = LocalDate.now(),
    zone: ZoneId = ZoneId.systemDefault(),
): List<BodyWeightTrendPoint> {
    val normalized = entries
        .mapNotNull { entry ->
            runCatching { LocalDate.parse(entry.date) }.getOrNull()?.let { date -> date to entry }
        }
        .groupBy { it.first }
        .mapValues { (_, values) -> values.maxBy { it.second.updatedAt }.second }
        .toSortedMap()
    val cutoff = weeks?.let { today.minusWeeks(it.toLong()) }
    return normalized.mapNotNull { (date, entry) ->
        if (cutoff != null && date.isBefore(cutoff)) return@mapNotNull null
        val windowStart = date.minusDays(6)
        val windowValues = normalized
            .filterKeys { !it.isBefore(windowStart) && !it.isAfter(date) }
            .values
            .map { it.weightKg }
        BodyWeightTrendPoint(
            date = date.toString(),
            timestamp = date.atStartOfDay(zone).toInstant().toEpochMilli(),
            weightKg = entry.weightKg,
            average7DaysKg = windowValues.average(),
        )
    }
}
