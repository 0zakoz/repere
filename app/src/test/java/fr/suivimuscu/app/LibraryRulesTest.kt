package fr.suivimuscu.app

import fr.suivimuscu.app.data.AppState
import fr.suivimuscu.app.data.Exercise
import fr.suivimuscu.app.data.LoggedExercise
import fr.suivimuscu.app.data.MuscleAssignment
import fr.suivimuscu.app.data.MuscleGroup
import fr.suivimuscu.app.data.MuscleSnapshot
import fr.suivimuscu.app.data.MuscleRole
import fr.suivimuscu.app.data.TemplateExercise
import fr.suivimuscu.app.data.TrainingProgram
import fr.suivimuscu.app.data.WorkoutLog
import fr.suivimuscu.app.data.WorkoutSet
import fr.suivimuscu.app.data.WorkoutStatus
import fr.suivimuscu.app.data.WorkoutTemplate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryRulesTest {
    private fun completedLog(
        id: String,
        exerciseIds: List<String>,
        templateId: String? = null,
        programId: String? = null,
        muscleIds: List<String> = emptyList(),
    ) = WorkoutLog(
        id = id,
        templateId = templateId,
        templateNameSnapshot = "T",
        programId = programId,
        localDate = "2026-08-01",
        startedAt = 1,
        status = WorkoutStatus.COMPLETED,
        exercises = exerciseIds.map { exerciseId ->
            LoggedExercise(
                id = "$id-$exerciseId",
                exerciseId = exerciseId,
                nameSnapshot = exerciseId,
                repMinSnapshot = 6,
                repMaxSnapshot = 10,
                musclesSnapshot = muscleIds.map { MuscleSnapshot(it, it, MuscleRole.PRIMARY) },
                plannedSets = 1,
                sets = listOf(WorkoutSet("s", 1, "50", "8", completed = true)),
            )
        },
    )

    private fun state(
        logs: List<WorkoutLog> = emptyList(),
        exercises: List<Exercise> = emptyList(),
        templates: List<WorkoutTemplate> = emptyList(),
        programs: List<TrainingProgram> = emptyList(),
        muscles: List<MuscleGroup> = emptyList(),
    ) = AppState(
        muscles = muscles,
        exercises = exercises,
        templates = templates,
        programs = programs,
        workoutLogs = logs,
    )

    @Test
    fun draftsAndDeletedLogsDoNotCountAsHistory() {
        val draft = completedLog("d", listOf("curl")).copy(status = WorkoutStatus.DRAFT)
        val deleted = completedLog("x", listOf("curl")).copy(deletedAt = 99)
        val s = state(logs = listOf(draft, deleted))

        assertFalse(exerciseUsedInHistory(s, "curl"))
        assertFalse(muscleUsedInHistory(s, "biceps"))
    }

    @Test
    fun exerciseUsedInHistoryBlocksDeletion() {
        val s = state(logs = listOf(completedLog("a", listOf("curl"))))

        assertTrue(exerciseUsedInHistory(s, "curl"))
        assertEquals(s, stateAfterExerciseDeletion(s, "curl"))
    }

    @Test
    fun deletingUnusedExerciseCascadesToTemplatesAndTheirCycles() {
        val s = state(
            exercises = listOf(Exercise("curl", "Curl", 6, 12)),
            templates = listOf(
                WorkoutTemplate("a", "A", listOf(TemplateExercise("curl", 2), TemplateExercise("press", 2))),
                WorkoutTemplate("b", "B", listOf(TemplateExercise("curl", 2))),
            ),
            programs = listOf(
                TrainingProgram(
                    id = "p",
                    name = "Full body",
                    templateCycle = listOf("b", "a"),
                    nextIndex = 1,
                ),
            ),
        )

        val after = stateAfterExerciseDeletion(s, "curl")

        assertFalse(after.exercises.any { it.id == "curl" })
        assertEquals(listOf("press"), after.templates.first { it.id == "a" }.exercises.map { it.exerciseId })
        assertTrue(after.templates.none { it.id == "b" })
        val program = after.programs.first()
        assertEquals(listOf("a"), program.templateCycle)
        assertEquals(0, program.nextIndex)
    }

    @Test
    fun deletingTemplateAdjustsProgramCycleAndNextIndex() {
        val s = state(
            templates = listOf(
                WorkoutTemplate("a", "A", listOf(TemplateExercise("press", 2))),
                WorkoutTemplate("b", "B", listOf(TemplateExercise("curl", 2))),
            ),
            programs = listOf(
                TrainingProgram(id = "p", name = "P", templateCycle = listOf("a", "b"), nextIndex = 1),
            ),
        )

        val after = stateAfterTemplateDeletion(s, "a")

        assertTrue(after.templates.none { it.id == "a" })
        val program = after.programs.first()
        assertEquals(listOf("b"), program.templateCycle)
        assertEquals(0, program.nextIndex)
    }

    @Test
    fun deletingUnusedMuscleRemovesItFromExerciseAssignments() {
        val s = state(
            muscles = listOf(MuscleGroup("biceps", "Biceps")),
            exercises = listOf(
                Exercise(
                    "curl", "Curl", 6, 12,
                    muscles = listOf(MuscleAssignment("biceps", MuscleRole.PRIMARY), MuscleAssignment("triceps", MuscleRole.SECONDARY)),
                ),
            ),
        )

        val after = stateAfterMuscleDeletion(s, "biceps")

        assertTrue(after.muscles.none { it.id == "biceps" })
        assertEquals(listOf("triceps"), after.exercises.first().muscles.map { it.muscleId })
    }

    @Test
    fun muscleUsedInHistoryBlocksDeletion() {
        val s = state(
            logs = listOf(completedLog("a", listOf("curl"), muscleIds = listOf("biceps"))),
            muscles = listOf(MuscleGroup("biceps", "Biceps")),
        )

        assertTrue(muscleUsedInHistory(s, "biceps"))
        assertEquals(s, stateAfterMuscleDeletion(s, "biceps"))
    }
}
