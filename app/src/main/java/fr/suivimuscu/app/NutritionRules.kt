package fr.suivimuscu.app

import fr.suivimuscu.app.data.NutritionDayRemaining
import fr.suivimuscu.app.data.NutritionDayTotal
import fr.suivimuscu.app.data.NutritionEntry
import fr.suivimuscu.app.data.NutritionTargets
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

internal fun normalizedCalories(value: String): Int? =
    value.trim().toIntOrNull()?.takeIf { it in 1..100_000 }

internal fun normalizedProteinGrams(value: String): Double? {
    val parsed = value.trim().replace(',', '.').toDoubleOrNull() ?: return null
    if (!parsed.isFinite() || parsed < 0.0 || parsed > 10_000.0) return null
    return (parsed * 10.0).roundToInt() / 10.0
}

internal fun calculateNutritionTrend(
    entries: List<NutritionEntry>,
    weeks: Int?,
    today: LocalDate = LocalDate.now(),
    zone: ZoneId = ZoneId.systemDefault(),
): List<NutritionDayTotal> {
    val cutoff = weeks?.let { today.minusWeeks(it.toLong()) }
    return entries.mapNotNull { entry ->
        runCatching { LocalDate.parse(entry.date) }.getOrNull()?.let { it to entry }
    }
        .filter { (date, _) -> !date.isAfter(today) && (cutoff == null || !date.isBefore(cutoff)) }
        .groupBy({ it.first }, { it.second })
        .toSortedMap()
        .map { (date, dayEntries) ->
            NutritionDayTotal(
                date = date.toString(),
                timestamp = date.atStartOfDay(zone).toInstant().toEpochMilli(),
                caloriesKcal = dayEntries.sumOf { it.caloriesKcal },
                proteinGrams = dayEntries.sumOf { it.proteinGrams },
                entryCount = dayEntries.size,
            )
        }
}

internal fun calculateNutritionRemaining(
    entries: List<NutritionEntry>,
    date: String,
    targets: NutritionTargets?,
): NutritionDayRemaining {
    val day = entries.filter { it.date == date }
    val caloriesIn = day.sumOf { it.caloriesKcal }
    val proteinIn = (day.sumOf { it.proteinGrams } * 10.0).roundToInt() / 10.0
    return NutritionDayRemaining(
        caloriesIn = caloriesIn,
        proteinIn = proteinIn,
        caloriesLeft = targets?.caloriesKcal?.minus(caloriesIn),
        proteinLeft = targets?.proteinGrams?.let { ((it - proteinIn) * 10.0).roundToInt() / 10.0 },
    )
}
