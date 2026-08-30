package fr.suivimuscu.app.data

import fr.suivimuscu.app.calculateBodyWeightTrend
import fr.suivimuscu.app.isSetInputValid
import fr.suivimuscu.app.lastPerformedExercise
import fr.suivimuscu.app.workoutChronologyTimestamp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object CompleteMarkdownExporter {
    fun export(
        state: AppState,
        appVersion: String,
        generatedAt: Instant = Instant.now(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): String = buildString {
        val completedWorkouts = state.workoutLogs
            .filter { it.status == WorkoutStatus.COMPLETED && it.deletedAt == null }
            .sortedBy { workoutChronologyTimestamp(it, zoneId) }
        val drafts = state.workoutLogs
            .filter { it.status == WorkoutStatus.DRAFT && it.deletedAt == null }
            .sortedBy { workoutChronologyTimestamp(it, zoneId) }
        val latestWeight = state.bodyWeights.maxWithOrNull(
            compareBy<BodyWeightEntry> { it.date }.thenBy { it.updatedAt },
        )

        appendLine("# Suivi Muscu — export complet pour analyse")
        appendLine()
        appendLine(
            "Ce document contient toutes les informations actuellement connues de l’application, " +
                "présentées pour être lues par un humain ou analysées dans une conversation ChatGPT. " +
                "Il n’invente aucune information non stockée (profil, objectifs, santé ou contexte personnel).",
        )
        appendLine()
        appendField("Version de l’application", appVersion)
        appendField("Version du schéma de données", state.schemaVersion.toString())
        appendField("Généré le", formatInstant(generatedAt, zoneId))
        appendField("Fuseau horaire", zoneId.id)
        appendLine()

        appendLine("## Synthèse")
        appendLine()
        appendField("Programmes", state.programs.size.toString())
        appendField("Programme actif", state.programs.firstOrNull { it.active && !it.archived }?.name?.let(::md) ?: "Aucun")
        appendField("Modèles de séance", state.templates.size.toString())
        appendField("Exercices", state.exercises.size.toString())
        appendField("Groupes musculaires", state.muscles.size.toString())
        appendField("Séances terminées visibles", completedWorkouts.size.toString())
        appendField("Séances en cours", drafts.size.toString())
        appendField("Événements de programme", state.programEvents.size.toString())
        appendField("Pesées", state.bodyWeights.size.toString())
        appendField(
            "Dernière pesée",
            latestWeight?.let { "${it.date} — ${formatNumber(it.weightKg)} kg" } ?: "Aucune",
        )
        appendLine()

        appendPrograms(state)
        appendTemplates(state, zoneId)
        appendExercises(state, zoneId)
        appendMuscles(state)
        appendWorkoutHistory(completedWorkouts, "Historique complet des séances terminées", zoneId)
        appendWorkoutHistory(drafts, "Séance en cours", zoneId)
        appendProgramEvents(state, zoneId)
        appendWeights(state, generatedAt, zoneId)

        appendLine("## Règles de lecture")
        appendLine()
        appendLine("- Une série **réalisée** a été explicitement validée dans l’application.")
        appendLine(
            "- Une série **non réalisée** peut conserver une charge et des répétitions préremplies : " +
                "ces valeurs sont des objectifs de saisie, pas une performance accomplie.",
        )
        appendLine("- Les séances supprimées et en attente de purge sont exclues de ce document.")
        appendLine("- Les associations musculaires de la bibliothèque représentent leur état actuel.")
    }

    private fun StringBuilder.appendPrograms(state: AppState) {
        appendLine("## Programmes")
        appendLine()
        if (state.programs.isEmpty()) {
            appendLine("Aucun programme.")
            appendLine()
            return
        }
        state.programs.forEachIndexed { index, program ->
            val status = buildList {
                if (program.active) add("actif")
                if (program.archived) add("archivé")
                if (isEmpty()) add("inactif")
            }.joinToString(", ")
            appendLine("### ${index + 1}. ${md(program.name)}")
            appendLine()
            appendField("Identifiant", code(program.id))
            appendField("État", status)
            appendField(
                "Jours indicatifs",
                program.trainingDays.joinToString(", ") { dayName(it) }.ifEmpty { "Aucun" },
            )
            appendField("Index de la prochaine séance", program.nextIndex.toString())
            appendField("Calendrier vérifié jusqu’au", program.scheduleCheckedThrough.ifBlank { "—" })
            val nextTemplateId = program.templateCycle
                .takeIf { it.isNotEmpty() }
                ?.get(program.nextIndex.mod(program.templateCycle.size))
            appendField(
                "Prochaine séance",
                nextTemplateId?.let { id ->
                    state.templates.firstOrNull { it.id == id }?.let { "${md(it.name)} (${code(id)})" }
                        ?: code(id)
                } ?: "Aucune",
            )
            appendLine("- **Cycle ordonné** :")
            if (program.templateCycle.isEmpty()) {
                appendLine("  - Aucun modèle.")
            } else {
                program.templateCycle.forEachIndexed { cycleIndex, templateId ->
                    val template = state.templates.firstOrNull { it.id == templateId }
                    val marker = if (cycleIndex == program.nextIndex.mod(program.templateCycle.size)) " — prochaine" else ""
                    appendLine(
                        "  ${cycleIndex + 1}. ${md(template?.name ?: "Modèle introuvable")} " +
                            "(${code(templateId)})$marker",
                    )
                }
            }
            appendLine()
        }
    }

    private fun StringBuilder.appendTemplates(state: AppState, zoneId: ZoneId) {
        appendLine("## Modèles de séance")
        appendLine()
        if (state.templates.isEmpty()) {
            appendLine("Aucun modèle de séance.")
            appendLine()
            return
        }
        state.templates.forEachIndexed { index, template ->
            appendLine("### ${index + 1}. Séance ${md(template.name)}${if (template.archived) " — archivée" else ""}")
            appendLine()
            appendField("Identifiant", code(template.id))
            appendField("Nombre d’exercices", template.exercises.size.toString())
            appendField("Séries prévues", template.exercises.sumOf { it.targetSets }.toString())
            if (template.exercises.isEmpty()) {
                appendLine("Aucun exercice.")
                appendLine()
                return@forEachIndexed
            }
            template.exercises.forEachIndexed { exerciseIndex, entry ->
                val exercise = state.exercises.firstOrNull { it.id == entry.exerciseId }
                appendLine("#### ${exerciseIndex + 1}. ${md(exercise?.name ?: "Exercice introuvable")}")
                appendLine()
                appendField("Identifiant de l’exercice", code(entry.exerciseId))
                appendField("Séries cibles", entry.targetSets.toString())
                appendField(
                    "Plage effective",
                    if (exercise == null && (entry.repMinOverride == null || entry.repMaxOverride == null)) "—"
                    else "${entry.repMinOverride ?: exercise?.defaultRepMin}–${entry.repMaxOverride ?: exercise?.defaultRepMax} reps",
                )
                appendField(
                    "Surcharge de plage dans ce modèle",
                    if (entry.repMinOverride == null && entry.repMaxOverride == null) "Aucune"
                    else "${entry.repMinOverride ?: "—"}–${entry.repMaxOverride ?: "—"}",
                )
                exercise?.let {
                    appendField("Consigne", it.instruction.takeIf { value -> value.isNotBlank() }?.let(::md) ?: "—")
                    appendField("Muscles principaux", muscleNames(state, it, MuscleRole.PRIMARY))
                    appendField("Muscles secondaires", muscleNames(state, it, MuscleRole.SECONDARY))
                    appendField("Muscles tertiaires", muscleNames(state, it, MuscleRole.TERTIARY))
                    appendLastPerformance(state, it.id, zoneId)
                }
                appendLine()
            }
        }
    }

    private fun StringBuilder.appendExercises(state: AppState, zoneId: ZoneId) {
        appendLine("## Bibliothèque des exercices")
        appendLine()
        if (state.exercises.isEmpty()) {
            appendLine("Aucun exercice.")
            appendLine()
            return
        }
        state.exercises.forEachIndexed { index, exercise ->
            appendLine("### ${index + 1}. ${md(exercise.name)}${if (exercise.archived) " — archivé" else ""}")
            appendLine()
            appendField("Identifiant", code(exercise.id))
            appendField("Plage par défaut", "${exercise.defaultRepMin}–${exercise.defaultRepMax} reps")
            appendField("Consigne", exercise.instruction.takeIf { it.isNotBlank() }?.let(::md) ?: "—")
            appendField("Muscles principaux ×1", muscleNames(state, exercise, MuscleRole.PRIMARY))
            appendField("Muscles secondaires ×0,5", muscleNames(state, exercise, MuscleRole.SECONDARY))
            appendField("Muscles tertiaires ×0,25", muscleNames(state, exercise, MuscleRole.TERTIARY))
            appendLastPerformance(state, exercise.id, zoneId)
            appendLine()
        }
    }

    private fun StringBuilder.appendLastPerformance(state: AppState, exerciseId: String, zoneId: ZoneId) {
        val performance = lastPerformedExercise(state, exerciseId, zoneId)
        if (performance == null) {
            appendField("Dernière performance réelle", "Aucune")
            return
        }
        val sets = performance.exercise.sets
            .filter { it.completed && isSetInputValid(it) }
            .joinToString(" ; ") { set ->
                "S${set.order}: ${md(set.weightKg)} kg × ${md(set.reps)} reps" +
                    (set.rir?.let { ", RIR $it" } ?: ", RIR —") +
                    (set.restBeforeSeconds?.let { ", repos ${it}s" } ?: ", repos —")
            }
        appendField(
            "Dernière performance réelle",
            "${performance.workout.localDate}, séance ${md(performance.workout.templateNameSnapshot)} " +
                "(${code(performance.workout.id)}) — $sets",
        )
    }

    private fun StringBuilder.appendMuscles(state: AppState) {
        appendLine("## Groupes musculaires")
        appendLine()
        if (state.muscles.isEmpty()) {
            appendLine("Aucun groupe musculaire.")
            appendLine()
            return
        }
        state.muscles.forEachIndexed { index, muscle ->
            appendLine(
                "${index + 1}. **${md(muscle.name)}** — identifiant ${code(muscle.id)}" +
                    if (muscle.archived) " — archivé" else " — actif",
            )
        }
        appendLine()
    }

    private fun StringBuilder.appendWorkoutHistory(
        workouts: List<WorkoutLog>,
        title: String,
        zoneId: ZoneId,
    ) {
        appendLine("## $title")
        appendLine()
        if (workouts.isEmpty()) {
            appendLine("Aucune séance.")
            appendLine()
            return
        }
        workouts.forEachIndexed { index, workout ->
            appendLine("### ${index + 1}. ${workout.localDate} — séance ${md(workout.templateNameSnapshot)}")
            appendLine()
            appendField("Identifiant", code(workout.id))
            appendField("Statut", if (workout.status == WorkoutStatus.COMPLETED) "Terminée" else "Brouillon en cours")
            appendField("Modèle", "${md(workout.templateNameSnapshot)} (${workout.templateId?.let(::code) ?: "—"})")
            appendField("Programme", workout.programNameSnapshot?.let { "${md(it)} (${workout.programId?.let(::code) ?: "—"})" } ?: "Hors programme")
            appendField("Date locale choisie", workout.localDate)
            appendField("Début", formatEpoch(workout.startedAt, zoneId))
            appendField("Fin", workout.endedAt?.let { formatEpoch(it, zoneId) } ?: "—")
            appendField("Durée", workout.endedAt?.let { formatDuration((it - workout.startedAt).coerceAtLeast(0)) } ?: "En cours")
            appendField("Fait avancer le programme", yesNo(workout.advanceProgramOnFinish))
            appendField("Édition d’une séance terminée", yesNo(workout.editingCompletedLog))
            appendField("Note", workout.note.takeIf { it.isNotBlank() }?.let(::md) ?: "—")
            appendField("Séries réalisées", workout.exercises.sumOf { ex -> ex.sets.count { it.completed } }.toString())
            appendField("Séries prévues", workout.exercises.sumOf { it.plannedSets }.toString())
            appendLine()
            workout.exercises.forEachIndexed { exerciseIndex, exercise ->
                appendLine("#### ${exerciseIndex + 1}. ${md(exercise.nameSnapshot)}")
                appendLine()
                appendField("Identifiant du log d’exercice", code(exercise.id))
                appendField("Identifiant de l’exercice", code(exercise.exerciseId))
                appendField(
                    "Consigne snapshot",
                    exercise.instructionSnapshot.takeIf { it.isNotBlank() }?.let(::md) ?: "—",
                )
                appendField("Plage snapshot", "${exercise.repMinSnapshot}–${exercise.repMaxSnapshot} reps")
                appendField("Séries prévues", exercise.plannedSets.toString())
                appendField("Muscles principaux snapshot", snapshotMuscles(exercise, MuscleRole.PRIMARY))
                appendField("Muscles secondaires snapshot", snapshotMuscles(exercise, MuscleRole.SECONDARY))
                appendField("Muscles tertiaires snapshot", snapshotMuscles(exercise, MuscleRole.TERTIARY))
                appendField("Chronomètre démarré", exercise.restStartedAt?.let { formatEpoch(it, zoneId) } ?: "—")
                appendField("Série ciblée par le chronomètre", exercise.restTargetSetOrder?.toString() ?: "—")
                appendLine()
                appendLine("| Série | État | Charge | Répétitions | RIR | Repos avant | Identifiant |")
                appendLine("|---:|---|---:|---:|---:|---:|---|")
                exercise.sets.sortedBy { it.order }.forEach { set ->
                    appendLine(
                        "| ${set.order} | ${if (set.completed) "**Réalisée**" else "**Non réalisée**"} | " +
                            "${table(set.weightKg.ifBlank { "—" })} | ${table(set.reps.ifBlank { "—" })} | " +
                            "${set.rir ?: "—"} | ${set.restBeforeSeconds?.let { "${it}s" } ?: "—"} | " +
                            "${table(code(set.id))} |",
                    )
                }
                appendLine()
            }
        }
    }

    private fun StringBuilder.appendProgramEvents(state: AppState, zoneId: ZoneId) {
        appendLine("## Événements de programme")
        appendLine()
        if (state.programEvents.isEmpty()) {
            appendLine("Aucun événement.")
            appendLine()
            return
        }
        state.programEvents.sortedWith(compareBy<ProgramEvent> { it.date }.thenBy { it.id }).forEachIndexed { index, event ->
            val program = state.programs.firstOrNull { it.id == event.programId }
            val template = state.templates.firstOrNull { it.id == event.templateId }
            appendLine("### ${index + 1}. ${event.date} — ${eventOutcome(event.outcome)}")
            appendLine()
            appendField("Identifiant", code(event.id))
            appendField("Programme", "${md(program?.name ?: "Introuvable")} (${code(event.programId)})")
            appendField("Séance", "${md(template?.name ?: "Introuvable")} (${code(event.templateId)})")
            appendField("Résultat brut", md(event.outcome))
            appendField("Log de séance lié", event.workoutLogId?.let(::code) ?: "—")
            event.workoutLogId?.let { logId ->
                state.workoutLogs.firstOrNull { it.id == logId }?.let { appendField("Début lié", formatEpoch(it.startedAt, zoneId)) }
            }
            appendLine()
        }
    }

    private fun StringBuilder.appendWeights(state: AppState, generatedAt: Instant, zoneId: ZoneId) {
        appendLine("## Suivi du poids")
        appendLine()
        if (state.bodyWeights.isEmpty()) {
            appendLine("Aucune pesée.")
            appendLine()
            return
        }
        val today = generatedAt.atZone(zoneId).toLocalDate()
        val averages = calculateBodyWeightTrend(state.bodyWeights, weeks = null, today = today, zone = zoneId)
            .associateBy { it.date }
        appendLine("| Date | Poids | Moyenne 7 jours | Créée le | Modifiée le | Identifiant |")
        appendLine("|---|---:|---:|---|---|---|")
        state.bodyWeights.sortedWith(compareBy<BodyWeightEntry> { it.date }.thenBy { it.updatedAt }).forEach { entry ->
            appendLine(
                "| ${table(entry.date)} | ${formatNumber(entry.weightKg)} kg | " +
                    "${averages[entry.date]?.let { "${formatNumber(it.average7DaysKg)} kg" } ?: "—"} | " +
                    "${table(formatEpoch(entry.createdAt, zoneId))} | ${table(formatEpoch(entry.updatedAt, zoneId))} | " +
                    "${table(code(entry.id))} |",
            )
        }
        appendLine()
    }

    private fun StringBuilder.appendField(label: String, value: String) {
        appendLine("- **${md(label)}** : ${if (value.isBlank()) "—" else value}")
    }

    private fun muscleNames(state: AppState, exercise: Exercise, role: MuscleRole): String =
        exercise.muscles.filter { it.role == role }.joinToString(", ") { assignment ->
            val name = state.muscles.firstOrNull { it.id == assignment.muscleId }?.name ?: "Muscle introuvable"
            "${md(name)} (${code(assignment.muscleId)})"
        }.ifEmpty { "—" }

    private fun snapshotMuscles(exercise: LoggedExercise, role: MuscleRole): String =
        exercise.musclesSnapshot.filter { it.role == role }.joinToString(", ") {
            "${md(it.name)} (${code(it.muscleId)})"
        }.ifEmpty { "—" }

    private fun md(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\\", "\\\\")
        .replace("`", "\\`")
        .replace("*", "\\*")
        .replace("_", "\\_")
        .replace("[", "\\[")
        .replace("]", "\\]")
        .replace("#", "\\#")
        .replace("|", "\\|")
        .replace("\r\n", "<br>")
        .replace("\r", "<br>")
        .replace("\n", "<br>")

    private fun table(value: String): String = md(value)
    private fun code(value: String): String = "`${value.replace("`", "\\`")}`"
    private fun yesNo(value: Boolean): String = if (value) "Oui" else "Non"

    private fun dayName(day: Int): String = when (day) {
        1 -> "lundi"
        2 -> "mardi"
        3 -> "mercredi"
        4 -> "jeudi"
        5 -> "vendredi"
        6 -> "samedi"
        7 -> "dimanche"
        else -> "jour $day"
    }

    private fun eventOutcome(value: String): String = when (value) {
        "COMPLETED" -> "Séance terminée"
        "SKIPPED" -> "Créneau sauté"
        else -> md(value)
    }

    private fun formatNumber(value: Double): String = String.format(Locale.FRANCE, "%.1f", value)

    private fun formatInstant(value: Instant, zoneId: ZoneId): String =
        DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(value.atZone(zoneId))

    private fun formatEpoch(value: Long, zoneId: ZoneId): String =
        formatInstant(Instant.ofEpochMilli(value), zoneId)

    private fun formatDuration(milliseconds: Long): String {
        val seconds = milliseconds / 1_000
        val hours = seconds / 3_600
        val minutes = (seconds % 3_600) / 60
        val remainingSeconds = seconds % 60
        return buildString {
            if (hours > 0) append("${hours}h ")
            append("${minutes}min ${remainingSeconds}s")
        }
    }
}
