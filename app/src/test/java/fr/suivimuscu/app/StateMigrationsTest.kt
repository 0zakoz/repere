package fr.suivimuscu.app

import fr.suivimuscu.app.data.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StateMigrationsTest {
    @Test
    fun versionOneForearmsAreSplitWithoutLosingDedicatedExerciseRoleOrHistory() {
        val snapshot = MuscleSnapshot("forearms", "Avant-bras", MuscleRole.SECONDARY)
        val legacy = AppState(
            schemaVersion = 1,
            muscles = listOf(MuscleGroup("forearms", "Avant-bras")),
            exercises = listOf(
                Exercise(
                    id = "wrist_flexion",
                    name = "Flexion poignets",
                    defaultRepMin = 8,
                    defaultRepMax = 15,
                    muscles = listOf(MuscleAssignment("forearms", MuscleRole.SECONDARY)),
                ),
                Exercise(
                    id = "horizontal_row",
                    name = "Tirage horizontal",
                    defaultRepMin = 6,
                    defaultRepMax = 10,
                    muscles = listOf(MuscleAssignment("forearms", MuscleRole.SECONDARY)),
                ),
            ),
            workoutLogs = listOf(
                WorkoutLog(
                    id = "workout",
                    templateId = "a",
                    templateNameSnapshot = "A",
                    localDate = "2026-07-31",
                    startedAt = 1,
                    status = WorkoutStatus.COMPLETED,
                    exercises = listOf(
                        LoggedExercise(
                            id = "logged",
                            exerciseId = "wrist_flexion",
                            nameSnapshot = "Flexion poignets",
                            repMinSnapshot = 8,
                            repMaxSnapshot = 15,
                            musclesSnapshot = listOf(snapshot),
                            plannedSets = 2,
                            sets = emptyList(),
                        )
                    ),
                )
            ),
        )

        val migrated = StateMigrations.toLatest(legacy)

        assertEquals(4, migrated.schemaVersion)
        assertTrue(migrated.nutritionEntries.isEmpty())
        assertFalse(migrated.muscles.any { it.id == "forearms" })
        assertTrue(migrated.muscles.any { it.id == "forearm_flexors" })
        assertTrue(migrated.muscles.any { it.id == "forearm_extensors" })
        assertEquals(
            MuscleRole.SECONDARY,
            migrated.exercises.first { it.id == "wrist_flexion" }.muscles.single().role,
        )
        assertTrue(
            migrated.exercises.first { it.id == "horizontal_row" }.muscles
                .all { it.role == MuscleRole.TERTIARY },
        )
        assertEquals(
            "forearm_flexors",
            migrated.workoutLogs.single().exercises.single().musclesSnapshot.single().muscleId,
        )
    }
}
