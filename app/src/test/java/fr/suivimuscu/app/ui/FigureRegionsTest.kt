package fr.suivimuscu.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FigureRegionsTest {
    private val expectedIds = setOf(
        "pecs", "upper_pecs", "lats", "traps", "lower_back",
        "front_delts", "side_delts", "rear_delts", "biceps", "triceps",
        "forearm_flexors", "forearm_extensors", "quads", "hamstrings",
        "glutes", "adductors", "calves", "abs",
    )

    @Test
    fun everyMuscleHasExactlyOneRegion() {
        val ids = (frontRegions + backRegions).map { it.muscleId }

        assertEquals(18, ids.size)
        assertEquals(expectedIds, ids.toSet())
    }

    @Test
    fun everyRegionPathIsClosedAndDrawable() {
        (frontRegions + backRegions).forEach { region ->
            assertTrue(region.muscleId + " est vide", region.pathData.isNotBlank())
            assertTrue(
                region.muscleId + " n'est pas fermé",
                region.pathData.trimEnd().endsWith("Z", ignoreCase = true),
            )
        }
    }
}
