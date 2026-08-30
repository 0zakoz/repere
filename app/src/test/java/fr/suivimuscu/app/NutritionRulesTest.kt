package fr.suivimuscu.app

import fr.suivimuscu.app.data.AppState
import fr.suivimuscu.app.data.NutritionCsvExporter
import fr.suivimuscu.app.data.NutritionEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class NutritionRulesTest {
    @Test
    fun valuesAreValidatedAndProteinIsRoundedToOneDecimal() {
        assertEquals(650, normalizedCalories("650"))
        assertNull(normalizedCalories("0"))
        assertNull(normalizedCalories("12,5"))
        assertEquals(42.6, normalizedProteinGrams("42,56")!!, 0.001)
        assertEquals(0.0, normalizedProteinGrams("0")!!, 0.001)
        assertNull(normalizedProteinGrams("-1"))
    }

    @Test
    fun multipleEntriesAreSummedIntoOneDailyPoint() {
        val entries = listOf(
            NutritionEntry("a", "2026-08-29", 600, 35.5, 1),
            NutritionEntry("b", "2026-08-29", 450, 20.0, 2),
            NutritionEntry("c", "2026-08-30", 800, 50.0, 3),
        )

        val points = calculateNutritionTrend(
            entries,
            weeks = null,
            today = LocalDate.of(2026, 8, 30),
            zone = ZoneId.of("UTC"),
        )

        assertEquals(2, points.size)
        assertEquals(1050, points.first().caloriesKcal)
        assertEquals(55.5, points.first().proteinGrams, 0.001)
        assertEquals(2, points.first().entryCount)
    }

    @Test
    fun csvKeepsEveryEntryAndDailyTotals() {
        val state = AppState(nutritionEntries = listOf(
            NutritionEntry("a", "2026-08-30", 600, 35.5, 1),
            NutritionEntry("b", "2026-08-30", 450, 20.0, 2),
        ))

        val csv = NutritionCsvExporter.export(state)

        assertEquals(3, csv.lines().count { it.isNotBlank() })
        assertTrue(csv.contains("600,35.5,1050,55.5"))
        assertTrue(csv.contains("450,20.0,1050,55.5"))
    }
}
