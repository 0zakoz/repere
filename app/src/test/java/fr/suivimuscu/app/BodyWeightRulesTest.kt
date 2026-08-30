package fr.suivimuscu.app

import fr.suivimuscu.app.data.AppState
import fr.suivimuscu.app.data.BodyWeightEntry
import fr.suivimuscu.app.data.WeightCsvExporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class BodyWeightRulesTest {
    @Test
    fun weightInputAcceptsFrenchDecimalAndRoundsToOneDecimal() {
        assertEquals(83.5, normalizedWeightKg("83,47")!!, 0.001)
        assertEquals(84.0, normalizedWeightKg("84")!!, 0.001)
        assertNull(normalizedWeightKg(""))
        assertNull(normalizedWeightKg("0"))
        assertNull(normalizedWeightKg("501"))
    }

    @Test
    fun sevenDayAverageUsesAvailableCalendarDaysOnly() {
        val entries = listOf(
            BodyWeightEntry("1", "2026-07-01", 80.0, 1),
            BodyWeightEntry("2", "2026-07-03", 82.0, 2),
            BodyWeightEntry("3", "2026-07-08", 84.0, 3),
        )

        val points = calculateBodyWeightTrend(
            entries = entries,
            weeks = null,
            today = LocalDate.of(2026, 7, 8),
            zone = ZoneId.of("Europe/Paris"),
        )

        assertEquals(80.0, points[0].average7DaysKg, 0.001)
        assertEquals(81.0, points[1].average7DaysKg, 0.001)
        assertEquals(83.0, points[2].average7DaysKg, 0.001)
    }

    @Test
    fun missingDayIsPrefilledFromMostRecentEarlierMeasurement() {
        val entries = listOf(
            BodyWeightEntry("1", "2026-07-29", 81.2, 1),
            BodyWeightEntry("2", "2026-07-31", 80.8, 2),
        )

        val previous = previousBodyWeightEntry(entries, LocalDate.of(2026, 8, 2))

        assertEquals("2026-07-31", previous?.date)
        assertEquals(80.8, previous!!.weightKg, 0.001)
    }

    @Test
    fun weightCsvContainsRawAndSevenDayAverage() {
        val state = AppState(bodyWeights = listOf(
            BodyWeightEntry("1", "2026-07-01", 80.0, 1),
            BodyWeightEntry("2", "2026-07-03", 82.0, 2),
        ))

        val csv = WeightCsvExporter.export(state)

        assertTrue(csv.contains("date,weight_kg,average_7_days_kg"))
        assertTrue(csv.contains("2026-07-03,82.0,81.0"))
    }
}
