package fr.suivimuscu.app

import fr.suivimuscu.app.data.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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

        assertEquals(5, migrated.schemaVersion)
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

    @Test
    fun versionFourGainsNullTargetsAndWeightGoalWithoutLosingData() {
        val legacy = AppState(
            schemaVersion = 4,
            bodyWeights = listOf(BodyWeightEntry("w", "2026-08-29", 80.2, 1)),
            nutritionEntries = listOf(NutritionEntry("n", "2026-08-29", 650, 42.5, 1)),
        )

        val migrated = StateMigrations.toLatest(legacy)

        assertEquals(5, migrated.schemaVersion)
        assertNull(migrated.nutritionTargets)
        assertNull(migrated.weightGoalKg)
        assertEquals(80.2, migrated.bodyWeights.single().weightKg, 0.001)
        assertEquals(650, migrated.nutritionEntries.single().caloriesKcal)
    }

    @Test
    fun versionFiveKeepsExistingTargetsAndWeightGoal() {
        val legacy = AppState(
            schemaVersion = 5,
            nutritionTargets = NutritionTargets(2200, 140.0),
            weightGoalKg = 75.5,
        )

        val migrated = StateMigrations.toLatest(legacy)

        assertEquals(5, migrated.schemaVersion)
        assertEquals(NutritionTargets(2200, 140.0), migrated.nutritionTargets)
        assertEquals(75.5, migrated.weightGoalKg!!, 0.001)
    }
}
