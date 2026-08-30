package fr.suivimuscu.app

import fr.suivimuscu.app.data.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvExporterTest {
    @Test
    fun exportContainsOneRowPerCompletedSetAndEscapesNotes() {
        val exercise = LoggedExercise(
            id = "logged-exercise",
            exerciseId = "exercise",
            nameSnapshot = "Press pecs",
            repMinSnapshot = 6,
            repMaxSnapshot = 10,
            musclesSnapshot = listOf(MuscleSnapshot("pecs", "Pectoraux", MuscleRole.PRIMARY)),
            plannedSets = 2,
            sets = listOf(
                WorkoutSet("s1", 1, "50.5", "10", 1, null, true),
                WorkoutSet("s2", 2, "50.5", "8", 0, 120, true),
            ),
        )
        val log = WorkoutLog(
            id = "log",
            templateId = "a",
            templateNameSnapshot = "A",
            localDate = "2026-07-30",
            startedAt = 1_000,
            endedAt = 61_000,
            note = "Bonne séance, \"propre\"",
            status = WorkoutStatus.COMPLETED,
            exercises = listOf(exercise),
        )

        val csv = CsvExporter.export(AppState(workoutLogs = listOf(log)))
        val lines = csv.lines().filter { it.isNotBlank() }
        assertEquals(3, lines.size)
        assertTrue(lines.first().contains("tertiary_muscles"))
        assertTrue(csv.contains("\"Bonne séance, \"\"propre\"\"\""))
        assertTrue(lines[2].endsWith("50.5,8,0,120"))
    }
}
