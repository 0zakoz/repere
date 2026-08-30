package fr.suivimuscu.app

import fr.suivimuscu.app.data.AppState
import fr.suivimuscu.app.data.Exercise
import fr.suivimuscu.app.data.LoggedExercise
import fr.suivimuscu.app.data.MuscleAssignment
import fr.suivimuscu.app.data.MuscleRole
import fr.suivimuscu.app.data.MuscleSnapshot
import fr.suivimuscu.app.data.WorkoutLog
import fr.suivimuscu.app.data.WorkoutSet
import fr.suivimuscu.app.data.WorkoutStatus
import fr.suivimuscu.app.data.TemplateExercise
import fr.suivimuscu.app.data.WorkoutTemplate
import fr.suivimuscu.app.data.ProgramEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class WorkoutRulesTest {
    @Test
    fun repAdjustmentUsesOneToNineHundredNinetyNineBounds() {
        assertEquals("1", adjustedReps("", 1))
        assertEquals("2", adjustedReps("1", 1))
        assertEquals("7", adjustedReps("8", -1))
        assertEquals("1", adjustedReps("1", -1))
        assertEquals("999", adjustedReps("999", 1))
    }

    @Test
    fun lastPerformanceIgnoresSkippedEmptyDraftAndDeletedOccurrences() {
        fun exercise(id: String, firstCompleted: Boolean, secondCompleted: Boolean) = LoggedExercise(
            id = "logged-$id",
            exerciseId = "curl",
            nameSnapshot = "Curl",
            repMinSnapshot = 6,
            repMaxSnapshot = 12,
            musclesSnapshot = emptyList(),
            plannedSets = 2,
            sets = listOf(
                WorkoutSet("$id-1", 1, "20", "10", completed = firstCompleted),
                WorkoutSet("$id-2", 2, "20", "8", completed = secondCompleted),
            ),
        )
        fun workout(
            id: String,
            date: String,
            loggedExercise: LoggedExercise,
            status: WorkoutStatus = WorkoutStatus.COMPLETED,
            deletedAt: Long? = null,
        ) = WorkoutLog(
            id = id,
            templateId = "a",
            templateNameSnapshot = "A",
            localDate = date,
            startedAt = 1_000,
            status = status,
            deletedAt = deletedAt,
            exercises = listOf(loggedExercise),
        )

        val state = AppState(
            workoutLogs = listOf(
                workout("old-complete", "2026-07-01", exercise("old", true, true)),
                workout("latest-performed", "2026-07-08", exercise("partial", true, false)),
                workout("empty", "2026-07-15", exercise("empty", false, false)),
                workout("draft", "2026-07-22", exercise("draft", true, true), WorkoutStatus.DRAFT),
                workout("deleted", "2026-07-29", exercise("deleted", true, true), deletedAt = 5),
            ),
            programEvents = listOf(ProgramEvent("skip", "program", "a", "2026-08-01", "SKIPPED")),
        )

        val performance = lastPerformedExercise(state, "curl", ZoneId.of("Europe/Paris"))

        assertEquals("latest-performed", performance?.workout?.id)
        assertEquals("20", performance?.completedSetAt(1)?.weightKg)
        assertEquals(null, performance?.completedSetAt(2))
    }

    @Test
    fun setRequiresExplicitWeightAndReps() {
        assertFalse(isSetInputValid(WorkoutSet("1", 1, weightKg = "50", reps = "")))
        assertFalse(isSetInputValid(WorkoutSet("1", 1, weightKg = "", reps = "8")))
        assertFalse(isSetInputValid(WorkoutSet("1", 1, weightKg = "50", reps = "0")))
        assertTrue(isSetInputValid(WorkoutSet("1", 1, weightKg = "0", reps = "8")))
        assertTrue(isSetInputValid(WorkoutSet("1", 1, weightKg = "50,5", reps = "8")))
    }

    @Test
    fun autoRestSubtractsExecutionOffsetFromElapsed() {
        val start = 1_000_000L
        assertEquals(60, restSecondsAutoRecorded(start, start + 100_000))
        assertEquals(40, restSecondsAutoRecorded(start, start + 80_000))
        assertEquals(0, restSecondsAutoRecorded(start, start + 20_000))
        assertEquals(null, restSecondsAutoRecorded(null, start + 100_000))
    }

    @Test
    fun autoRestOffsetIsFortySeconds() {
        assertEquals(40L, AUTO_REST_OFFSET_SECONDS)
    }

    @Test
    fun finishingKeepsEveryPlannedExerciseAndSet() {
        val exercises = (1..7).map { exerciseOrder ->
            LoggedExercise(
                id = "log-$exerciseOrder",
                exerciseId = "exercise-$exerciseOrder",
                nameSnapshot = "Exercice $exerciseOrder",
                repMinSnapshot = 6,
                repMaxSnapshot = 10,
                musclesSnapshot = emptyList(),
                plannedSets = 2,
                sets = listOf(
                    WorkoutSet(
                        id = "$exerciseOrder-1",
                        order = 1,
                        weightKg = if (exerciseOrder <= 4) "50" else "",
                        reps = if (exerciseOrder <= 4) "8" else "",
                        completed = exerciseOrder <= 4,
                    ),
                    WorkoutSet(id = "$exerciseOrder-2", order = 2),
                ),
            )
        }

        val normalized = normalizedFinishedExercises(exercises)

        assertEquals(7, normalized.size)
        assertEquals(14, normalized.sumOf { it.plannedSets })
        assertEquals(14, normalized.sumOf { it.sets.size })
        assertEquals(4, normalized.sumOf { exercise -> exercise.sets.count { it.completed } })
    }

    @Test
    fun chronologyUsesOriginalStartTimeAndSelectedDateNotEditTime() {
        val zone = ZoneId.of("Europe/Paris")
        val originalStart = ZonedDateTime.of(2026, 7, 10, 18, 30, 0, 0, zone)
            .toInstant().toEpochMilli()
        val log = WorkoutLog(
            id = "workout",
            templateId = "a",
            templateNameSnapshot = "A",
            localDate = "2026-07-12",
            startedAt = originalStart,
            endedAt = originalStart + 10L * 24 * 60 * 60 * 1000,
            exercises = emptyList(),
        )

        val timestamp = workoutChronologyTimestamp(log, zone)
        val result = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(timestamp), zone)

        assertEquals(2026, result.year)
        assertEquals(7, result.monthValue)
        assertEquals(12, result.dayOfMonth)
        assertEquals(18, result.hour)
        assertEquals(30, result.minute)
    }

    @Test
    fun muscleStatsUseCurrentExerciseMappingRetroactively() {
        val loggedExercise = LoggedExercise(
            id = "logged",
            exerciseId = "curl",
            nameSnapshot = "Curl",
            repMinSnapshot = 6,
            repMaxSnapshot = 12,
            musclesSnapshot = listOf(MuscleSnapshot("forearms", "Avant-bras", MuscleRole.PRIMARY)),
            plannedSets = 2,
            sets = listOf(
                WorkoutSet("1", 1, "20", "10", rir = 1, completed = true),
                WorkoutSet("2", 2, "20", "8", rir = 2, completed = true),
            ),
        )
        val state = AppState(
            exercises = listOf(
                Exercise(
                    id = "curl",
                    name = "Curl",
                    defaultRepMin = 6,
                    defaultRepMax = 12,
                    muscles = listOf(MuscleAssignment("forearms", MuscleRole.SECONDARY)),
                )
            ),
            workoutLogs = listOf(
                WorkoutLog(
                    id = "workout",
                    templateId = "a",
                    templateNameSnapshot = "A",
                    localDate = "2026-07-30",
                    startedAt = 1,
                    status = WorkoutStatus.COMPLETED,
                    exercises = listOf(loggedExercise),
                )
            ),
        )

        val stats = calculateMusclePeriodStats(state, weeks = null).getValue("forearms")

        assertEquals(1.0, stats.weightedSets, 0.001)
        assertEquals(9.0, stats.averageReps!!, 0.001)
        assertEquals(1.5, stats.averageRir!!, 0.001)
    }

    @Test
    fun tertiaryMuscleRoleCountsAsQuarterSet() {
        assertEquals(0.25, muscleRoleFactor(MuscleRole.TERTIARY), 0.001)
    }

    @Test
    fun sessionPeriodIncludesMissingPlannedExercisesAndAveragesEachExercise() {
        fun loggedExercise(id: String, completed: Int) = LoggedExercise(
            id = "logged-$id",
            exerciseId = id,
            nameSnapshot = id,
            repMinSnapshot = 6,
            repMaxSnapshot = 10,
            musclesSnapshot = emptyList(),
            plannedSets = 2,
            sets = (1..2).map { order ->
                WorkoutSet(
                    id = "$id-$order",
                    order = order,
                    weightKg = "50",
                    reps = "8",
                    rir = 1,
                    completed = order <= completed,
                )
            },
        )
        val template = WorkoutTemplate(
            id = "a",
            name = "A",
            exercises = listOf(TemplateExercise("press", 2), TemplateExercise("curl", 2)),
        )
        val logs = listOf(
            WorkoutLog(
                id = "one",
                templateId = "a",
                templateNameSnapshot = "A",
                localDate = "2026-07-20",
                startedAt = 1_000,
                endedAt = 61_000,
                status = WorkoutStatus.COMPLETED,
                exercises = listOf(loggedExercise("press", 2), loggedExercise("curl", 1)),
            ),
            WorkoutLog(
                id = "two",
                templateId = "a",
                templateNameSnapshot = "A",
                localDate = "2026-07-27",
                startedAt = 100_000,
                endedAt = 220_000,
                status = WorkoutStatus.COMPLETED,
                exercises = listOf(loggedExercise("press", 1)),
            ),
        )
        val state = AppState(
            exercises = listOf(
                Exercise("press", "Press", 6, 10),
                Exercise("curl", "Curl", 6, 10),
            ),
            templates = listOf(template),
            workoutLogs = logs,
        )

        val stats = calculateSessionPeriodStats(state, "a", weeks = null, now = 300_000)!!

        assertEquals(2, stats.sessionCount)
        assertEquals(2.0, stats.averageCompletedSets, 0.001)
        assertEquals(4.0, stats.averagePlannedSets, 0.001)
        assertEquals(0.5, stats.completionRate, 0.001)
        assertEquals(1.5, stats.exercises.first { it.exerciseId == "press" }.averageCompletedSets, 0.001)
        assertEquals(0.5, stats.exercises.first { it.exerciseId == "curl" }.averageCompletedSets, 0.001)
    }
}
