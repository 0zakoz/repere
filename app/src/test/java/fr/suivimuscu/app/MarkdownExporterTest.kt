package fr.suivimuscu.app

import fr.suivimuscu.app.data.AppState
import fr.suivimuscu.app.data.BodyWeightEntry
import fr.suivimuscu.app.data.CompleteMarkdownExporter
import fr.suivimuscu.app.data.Exercise
import fr.suivimuscu.app.data.LoggedExercise
import fr.suivimuscu.app.data.MuscleAssignment
import fr.suivimuscu.app.data.MuscleGroup
import fr.suivimuscu.app.data.MuscleRole
import fr.suivimuscu.app.data.MuscleSnapshot
import fr.suivimuscu.app.data.NutritionEntry
import fr.suivimuscu.app.data.ProgramEvent
import fr.suivimuscu.app.data.TemplateExercise
import fr.suivimuscu.app.data.TrainingProgram
import fr.suivimuscu.app.data.WorkoutLog
import fr.suivimuscu.app.data.WorkoutSet
import fr.suivimuscu.app.data.WorkoutStatus
import fr.suivimuscu.app.data.WorkoutTemplate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class MarkdownExporterTest {
    @Test
    fun completeExportContainsAllVisibleDataAndUsesLastRealPerformance() {
        val muscles = listOf(
            MuscleGroup("pecs", "Pectoraux | haut"),
            MuscleGroup("biceps", "Biceps"),
            MuscleGroup("forearms", "Avant-bras", archived = true),
        )
        val exercise = Exercise(
            id = "curl",
            name = "Curl *barre*",
            defaultRepMin = 6,
            defaultRepMax = 12,
            instruction = "Garder les coudes fixes\nSans élan",
            muscles = listOf(
                MuscleAssignment("biceps", MuscleRole.PRIMARY),
                MuscleAssignment("forearms", MuscleRole.SECONDARY),
                MuscleAssignment("pecs", MuscleRole.TERTIARY),
            ),
        )
        val archivedExercise = Exercise("old", "Ancien exercice", 8, 15, archived = true)
        val template = WorkoutTemplate(
            id = "a",
            name = "A",
            exercises = listOf(TemplateExercise("curl", 2, 8, 10)),
        )
        val archivedTemplate = WorkoutTemplate("old-template", "Ancienne séance", emptyList(), archived = true)
        val program = TrainingProgram(
            id = "program",
            name = "Full body",
            templateCycle = listOf("a"),
            trainingDays = listOf(1, 3, 5),
            active = true,
        )
        val archivedProgram = TrainingProgram(
            id = "old-program",
            name = "Ancien programme",
            templateCycle = listOf("old-template"),
            archived = true,
        )

        fun logged(id: String, firstCompleted: Boolean, secondCompleted: Boolean) = LoggedExercise(
            id = "logged-$id",
            exerciseId = "curl",
            nameSnapshot = "Curl historique",
            instructionSnapshot = "Consigne historique",
            repMinSnapshot = 6,
            repMaxSnapshot = 12,
            musclesSnapshot = listOf(MuscleSnapshot("biceps", "Biceps", MuscleRole.PRIMARY)),
            plannedSets = 2,
            sets = listOf(
                WorkoutSet("$id-1", 1, "20", "10", rir = 1, restBeforeSeconds = 90, completed = firstCompleted),
                WorkoutSet("$id-2", 2, "20", "8", rir = 2, restBeforeSeconds = 120, completed = secondCompleted),
            ),
        )
        fun workout(
            id: String,
            date: String,
            exerciseLog: LoggedExercise,
            status: WorkoutStatus = WorkoutStatus.COMPLETED,
            deletedAt: Long? = null,
        ) = WorkoutLog(
            id = id,
            templateId = "a",
            templateNameSnapshot = "A",
            programId = "program",
            programNameSnapshot = "Full body",
            localDate = date,
            startedAt = Instant.parse("${date}T18:00:00Z").toEpochMilli(),
            endedAt = if (status == WorkoutStatus.COMPLETED) Instant.parse("${date}T19:00:00Z").toEpochMilli() else null,
            note = "Note | importante *ici*",
            status = status,
            exercises = listOf(exerciseLog),
            deletedAt = deletedAt,
        )

        val state = AppState(
            muscles = muscles,
            exercises = listOf(exercise, archivedExercise),
            templates = listOf(template, archivedTemplate),
            programs = listOf(program, archivedProgram),
            programEvents = listOf(
                ProgramEvent("event-done", "program", "a", "2026-07-01", "COMPLETED", "performed"),
                ProgramEvent("event-skip", "program", "a", "2026-07-08", "SKIPPED"),
            ),
            workoutLogs = listOf(
                workout("performed", "2026-07-01", logged("performed", true, false)),
                workout("empty-later", "2026-07-08", logged("empty", false, false)),
                workout("deleted-newer", "2026-07-15", logged("deleted", true, true), deletedAt = 10),
                workout("draft-current", "2026-07-22", logged("draft", false, false), WorkoutStatus.DRAFT),
            ),
            bodyWeights = listOf(
                BodyWeightEntry("weight-1", "2026-07-01", 80.0, 1_000),
                BodyWeightEntry("weight-2", "2026-07-03", 82.0, 2_000),
            ),
            nutritionEntries = listOf(
                NutritionEntry("meal-1", "2026-07-03", 650, 42.5, 3_000),
                NutritionEntry("meal-2", "2026-07-03", 400, 18.0, 4_000),
            ),
        )

        val markdown = CompleteMarkdownExporter.export(
            state = state,
            appVersion = "1.5.0",
            generatedAt = Instant.parse("2026-07-30T12:00:00Z"),
            zoneId = ZoneId.of("UTC"),
        )

        assertTrue(markdown.contains("Version de l’application** : 1.5.0"))
        assertTrue(markdown.contains("Curl \\*barre\\*"))
        assertTrue(markdown.contains("Garder les coudes fixes<br>Sans élan"))
        assertTrue(markdown.contains("Muscles tertiaires ×0,25"))
        assertTrue(markdown.contains("Ancien programme"))
        assertTrue(markdown.contains("Ancienne séance — archivée"))
        assertTrue(markdown.contains("Ancien exercice — archivé"))
        assertTrue(markdown.contains("Dernière performance réelle** : 2026-07-01"))
        assertTrue(markdown.contains("S1: 20 kg × 10 reps, RIR 1, repos 90s"))
        assertTrue(markdown.contains("empty-later"))
        assertTrue(markdown.contains("draft-current"))
        assertTrue(markdown.contains("**Non réalisée**"))
        assertTrue(markdown.contains("Créneau sauté"))
        assertTrue(markdown.contains("81,0 kg"))
        assertTrue(markdown.contains("Pectoraux \\| haut"))
        assertTrue(markdown.contains("Suivi nutritionnel"))
        assertTrue(markdown.contains("1 050 kcal").or(markdown.contains("1050 kcal")))
        assertTrue(markdown.contains("60,5 g"))
        assertTrue(markdown.contains("meal-2"))
        assertFalse(markdown.contains("deleted-newer"))
    }
}
