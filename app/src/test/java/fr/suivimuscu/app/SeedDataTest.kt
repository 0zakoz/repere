package fr.suivimuscu.app

import fr.suivimuscu.app.data.MuscleRole
import fr.suivimuscu.app.data.SeedData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SeedDataTest {
    @Test
    fun initialProgramAndSessionsMatchSpecification() {
        val state = SeedData.create()
        assertEquals(18, state.muscles.size)
        assertEquals(13, state.exercises.size)
        assertEquals(2, state.templates.size)

        val a = state.templates.first { it.id == "session_a" }
        val b = state.templates.first { it.id == "session_b" }
        assertEquals(7, a.exercises.size)
        assertEquals(6, b.exercises.size)
        assertTrue((a.exercises + b.exercises).all { it.targetSets == 2 })

        val program = state.programs.single()
        assertTrue(program.active)
        assertEquals(listOf("session_a", "session_b"), program.templateCycle)
        assertEquals(listOf(1, 3, 5), program.trainingDays)
    }

    @Test
    fun correctedMuscleMappingsAreSeeded() {
        val state = SeedData.create()
        val chestPress = state.exercises.first { it.id == "chest_press" }
        val rearFly = state.exercises.first { it.id == "rear_delt_fly" }
        val row = state.exercises.first { it.id == "horizontal_row" }

        assertTrue(chestPress.muscles.any { it.muscleId == "upper_pecs" && it.role == MuscleRole.SECONDARY })
        assertTrue(chestPress.muscles.none { it.muscleId == "triceps" })
        assertEquals(listOf("rear_delts"), rearFly.muscles.map { it.muscleId })
        assertTrue(row.muscles.any { it.muscleId == "forearm_flexors" && it.role == MuscleRole.TERTIARY })
        assertTrue(row.muscles.any { it.muscleId == "forearm_extensors" && it.role == MuscleRole.TERTIARY })
        assertTrue(state.muscles.none { it.id == "forearms" })
    }
}
