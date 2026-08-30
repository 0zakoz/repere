package fr.suivimuscu.app.data

import java.time.Instant

object CsvExporter {
    private val headers = listOf(
        "export_version", "session_id", "session_date", "started_at", "ended_at",
        "duration_seconds", "program_name", "workout_name", "session_note",
        "exercise_order", "exercise_id", "exercise_name", "target_rep_min",
        "target_rep_max", "primary_muscles", "secondary_muscles", "tertiary_muscles", "set_number",
        "weight_kg", "reps", "rir", "rest_before_seconds",
    )

    private fun cell(value: Any?): String {
        val raw = value?.toString().orEmpty()
        return if (raw.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"${raw.replace("\"", "\"\"")}\""
        } else raw
    }

    fun export(state: AppState): String = buildString {
        append('\uFEFF')
        appendLine(headers.joinToString(","))
        state.workoutLogs
            .filter { it.status == WorkoutStatus.COMPLETED && it.deletedAt == null }
            .sortedBy { it.startedAt }
            .forEach { log ->
                val ended = log.endedAt ?: log.startedAt
                log.exercises.forEachIndexed { exerciseIndex, exercise ->
                    val primary = exercise.musclesSnapshot
                        .filter { it.role == MuscleRole.PRIMARY }.joinToString("|") { it.name }
                    val secondary = exercise.musclesSnapshot
                        .filter { it.role == MuscleRole.SECONDARY }.joinToString("|") { it.name }
                    val tertiary = exercise.musclesSnapshot
                        .filter { it.role == MuscleRole.TERTIARY }.joinToString("|") { it.name }
                    exercise.sets.filter { it.completed }.forEach { set ->
                        val row = listOf(
                            2, log.id, log.localDate, Instant.ofEpochMilli(log.startedAt),
                            Instant.ofEpochMilli(ended), (ended - log.startedAt) / 1000,
                            log.programNameSnapshot, log.templateNameSnapshot, log.note,
                            exerciseIndex + 1, exercise.exerciseId, exercise.nameSnapshot,
                            exercise.repMinSnapshot, exercise.repMaxSnapshot,
                            primary, secondary, tertiary, set.order, set.weightKg.replace(',', '.'),
                            set.reps, set.rir, set.restBeforeSeconds,
                        )
                        appendLine(row.joinToString(",") { cell(it) })
                    }
                }
            }
    }
}

object WeightCsvExporter {
    fun export(state: AppState): String = buildString {
        append('\uFEFF')
        appendLine("date,weight_kg,average_7_days_kg,created_at,updated_at")
        val averages = state.bodyWeights
            .sortedBy { it.date }
            .mapNotNull { entry ->
                val date = runCatching { java.time.LocalDate.parse(entry.date) }.getOrNull()
                    ?: return@mapNotNull null
                val values = state.bodyWeights.filter { candidate ->
                    val candidateDate = runCatching { java.time.LocalDate.parse(candidate.date) }.getOrNull()
                        ?: return@filter false
                    !candidateDate.isBefore(date.minusDays(6)) && !candidateDate.isAfter(date)
                }.map { it.weightKg }
                entry.date to values.average()
            }.toMap()
        state.bodyWeights.sortedBy { it.date }.forEach { entry ->
            appendLine(
                listOf(
                    entry.date,
                    formatWeight(entry.weightKg),
                    averages[entry.date]?.let(::formatWeight).orEmpty(),
                    Instant.ofEpochMilli(entry.createdAt),
                    Instant.ofEpochMilli(entry.updatedAt),
                ).joinToString(",")
            )
        }
    }

    private fun formatWeight(value: Double): String =
        String.format(java.util.Locale.ROOT, "%.1f", value)
}
