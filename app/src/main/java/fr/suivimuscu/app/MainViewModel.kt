package fr.suivimuscu.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import fr.suivimuscu.app.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.UUID

enum class MainTab { JOURNAL, WEIGHT, NUTRITION, TRENDS, LIBRARY }

/** Offset retiré au temps écoulé quand le repos est enregistré automatiquement à la saisie de la série suivante. */
internal const val AUTO_REST_OFFSET_SECONDS = 40L
internal const val MAX_EDITABLE_DURATION_SECONDS = 24 * 60 * 60

internal fun isSetInputValid(set: WorkoutSet): Boolean =
    set.reps.toIntOrNull()?.let { it > 0 } == true &&
        set.weightKg.replace(',', '.').toDoubleOrNull()?.let { it >= 0 } == true

internal fun restSecondsAutoRecorded(restStartedAt: Long?, now: Long): Int? {
    restStartedAt ?: return null
    val elapsed = ((now - restStartedAt) / 1000L).coerceIn(0, 24 * 60 * 60)
    return (elapsed - AUTO_REST_OFFSET_SECONDS).coerceAtLeast(0L).toInt()
}

internal fun workoutChronologyTimestamp(log: WorkoutLog, zone: ZoneId = ZoneId.systemDefault()): Long {
    val time = Instant.ofEpochMilli(log.startedAt).atZone(zone).toLocalTime()
    return runCatching {
        LocalDate.parse(log.localDate).atTime(time).atZone(zone).toInstant().toEpochMilli()
    }.getOrDefault(log.startedAt)
}

internal data class ExercisePerformance(
    val workout: WorkoutLog,
    val exercise: LoggedExercise,
)

internal fun lastPerformedExercise(
    state: AppState,
    exerciseId: String,
    zone: ZoneId = ZoneId.systemDefault(),
): ExercisePerformance? = state.workoutLogs.asSequence()
    .filter { it.status == WorkoutStatus.COMPLETED && it.deletedAt == null }
    .sortedByDescending { workoutChronologyTimestamp(it, zone) }
    .flatMap { workout -> workout.exercises.asSequence().map { workout to it } }
    .firstOrNull { (_, exercise) ->
        exercise.exerciseId == exerciseId &&
            exercise.sets.any { it.completed && isSetInputValid(it) }
    }
    ?.let { (workout, exercise) -> ExercisePerformance(workout, exercise) }

internal fun ExercisePerformance.completedSetAt(order: Int): WorkoutSet? =
    exercise.sets.firstOrNull { it.order == order && it.completed && isSetInputValid(it) }

internal fun adjustedReps(current: String, delta: Int): String {
    require(delta == -1 || delta == 1) { "L’ajustement doit être de −1 ou +1" }
    val value = current.toIntOrNull() ?: 0
    return (value + delta).coerceIn(1, 999).toString()
}

internal fun movedExerciseLogs(
    exercises: List<LoggedExercise>,
    exerciseLogId: String,
    delta: Int,
): List<LoggedExercise> {
    require(delta == -1 || delta == 1) { "Le déplacement doit être de −1 ou +1" }
    val from = exercises.indexOfFirst { it.id == exerciseLogId }
    if (from < 0) return exercises
    val to = from + delta
    if (to !in exercises.indices) return exercises
    return exercises.toMutableList().also { list ->
        val item = list.removeAt(from)
        list.add(to, item)
    }
}

internal fun workoutWithDuration(
    workout: WorkoutLog,
    durationSeconds: Int,
    now: Long,
): WorkoutLog {
    val durationMillis = durationSeconds.coerceIn(0, MAX_EDITABLE_DURATION_SECONDS) * 1000L
    return if (workout.editingCompletedLog && workout.endedAt != null) {
        workout.copy(endedAt = workout.startedAt + durationMillis)
    } else {
        workout.copy(startedAt = now - durationMillis)
    }
}

internal fun workoutWithSetRest(
    workout: WorkoutLog,
    exerciseLogId: String,
    setId: String,
    restSeconds: Int?,
): WorkoutLog {
    val normalized = restSeconds?.coerceIn(0, MAX_EDITABLE_DURATION_SECONDS)
    return workout.copy(exercises = workout.exercises.map { exercise ->
        if (exercise.id != exerciseLogId) exercise else {
            val targetOrder = exercise.sets.firstOrNull { it.id == setId }?.order
            val stopsTimer = targetOrder != null && exercise.restTargetSetOrder == targetOrder
            exercise.copy(
                sets = exercise.sets.map { set ->
                    if (set.id == setId) set.copy(restBeforeSeconds = normalized) else set
                },
                restStartedAt = if (stopsTimer) null else exercise.restStartedAt,
                restTargetSetOrder = if (stopsTimer) null else exercise.restTargetSetOrder,
            )
        }
    })
}

internal fun normalizedFinishedExercises(exercises: List<LoggedExercise>): List<LoggedExercise> =
    exercises.map { exercise ->
        exercise.copy(
            sets = exercise.sets.map { set ->
                if (set.completed && !isSetInputValid(set)) set.copy(completed = false) else set
            },
            restStartedAt = null,
            restTargetSetOrder = null,
        )
    }

internal fun currentMuscleAssignments(
    state: AppState,
    exercise: LoggedExercise,
): List<Pair<String, MuscleRole>> {
    val current = state.exercises.firstOrNull { it.id == exercise.exerciseId }?.muscles
    return if (current != null) current.map { it.muscleId to it.role }
    else exercise.musclesSnapshot.map { it.muscleId to it.role }
}

internal fun muscleRoleFactor(role: MuscleRole): Double = when (role) {
    MuscleRole.PRIMARY -> 1.0
    MuscleRole.SECONDARY -> 0.5
    MuscleRole.TERTIARY -> 0.25
}

internal fun plannedSetCount(template: WorkoutTemplate?, log: WorkoutLog): Int {
    if (template == null) return log.exercises.sumOf { it.plannedSets }
    val loggedByExercise = log.exercises.associateBy { it.exerciseId }
    val templateTotal = template.exercises.sumOf { entry ->
        loggedByExercise[entry.exerciseId]?.plannedSets ?: entry.targetSets
    }
    val templateIds = template.exercises.mapTo(mutableSetOf()) { it.exerciseId }
    val extraTotal = log.exercises.filterNot { it.exerciseId in templateIds }.sumOf { it.plannedSets }
    return templateTotal + extraTotal
}

internal fun completedLogs(state: AppState): List<WorkoutLog> =
    state.workoutLogs.filter { it.status == WorkoutStatus.COMPLETED && it.deletedAt == null }

internal fun exerciseUsedInHistory(state: AppState, exerciseId: String): Boolean =
    completedLogs(state).any { log -> log.exercises.any { it.exerciseId == exerciseId } }

internal fun templateUsedInHistory(state: AppState, templateId: String): Boolean =
    completedLogs(state).any { it.templateId == templateId }

internal fun programUsedInHistory(state: AppState, programId: String): Boolean =
    completedLogs(state).any { it.programId == programId }

internal fun muscleUsedInHistory(state: AppState, muscleId: String): Boolean =
    completedLogs(state).any { log ->
        log.exercises.any { ex ->
            ex.musclesSnapshot.any { it.muscleId == muscleId } ||
                state.exercises.firstOrNull { it.id == ex.exerciseId }
                    ?.muscles?.any { it.muscleId == muscleId } == true
        }
    }

internal fun stateAfterExerciseDeletion(state: AppState, exerciseId: String): AppState {
    if (exerciseUsedInHistory(state, exerciseId)) return state
    val keptTemplates = state.templates.mapNotNull { template ->
        val entries = template.exercises.filterNot { it.exerciseId == exerciseId }
        when {
            entries.isEmpty() -> null
            entries.size != template.exercises.size -> template.copy(exercises = entries)
            else -> template
        }
    }
    val removedTemplateIds = state.templates.map { it.id }.toSet() - keptTemplates.map { it.id }.toSet()
    val programs = state.programs.map { program ->
        if (program.templateCycle.none { it in removedTemplateIds }) program
        else {
            val cycle = program.templateCycle.filterNot { it in removedTemplateIds }
            val removedBefore = program.templateCycle.take(program.nextIndex).count { it in removedTemplateIds }
            program.copy(
                templateCycle = cycle,
                nextIndex = if (cycle.isEmpty()) 0 else (program.nextIndex - removedBefore).mod(cycle.size),
            )
        }
    }
    return state.copy(
        exercises = state.exercises.filterNot { it.id == exerciseId },
        templates = keptTemplates,
        programs = programs,
    )
}

internal fun stateAfterTemplateDeletion(state: AppState, templateId: String): AppState {
    if (templateUsedInHistory(state, templateId)) return state
    val programs = state.programs.map { program ->
        val index = program.templateCycle.indexOf(templateId)
        if (index < 0) program else {
            val cycle = program.templateCycle.filterNot { it == templateId }
            val nextIndex = when {
                cycle.isEmpty() -> 0
                index < program.nextIndex -> (program.nextIndex - 1).mod(cycle.size)
                else -> program.nextIndex.mod(cycle.size)
            }
            program.copy(templateCycle = cycle, nextIndex = nextIndex)
        }
    }
    return state.copy(
        templates = state.templates.filterNot { it.id == templateId },
        programs = programs,
    )
}

internal fun stateAfterMuscleDeletion(state: AppState, muscleId: String): AppState {
    if (muscleUsedInHistory(state, muscleId)) return state
    return state.copy(
        muscles = state.muscles.filterNot { it.id == muscleId },
        exercises = state.exercises.map { exercise ->
            exercise.copy(muscles = exercise.muscles.filterNot { it.muscleId == muscleId })
        },
    )
}

internal fun calculateMusclePeriodStats(
    state: AppState,
    weeks: Int?,
    today: LocalDate = LocalDate.now(),
): Map<String, MusclePeriodStats> {
    val cutoffDate = weeks?.let {
        today.minusWeeks(it.toLong() - 1)
            .with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
    }
    data class Acc(
        var sets: Double = 0.0,
        var repsSum: Double = 0.0,
        var repsWeight: Double = 0.0,
        var rirSum: Double = 0.0,
        var rirWeight: Double = 0.0,
    )
    val totals = mutableMapOf<String, Acc>()
    state.workoutLogs
        .filter {
            it.status == WorkoutStatus.COMPLETED && it.deletedAt == null &&
                (cutoffDate == null || !LocalDate.parse(it.localDate).isBefore(cutoffDate))
        }
        .forEach { log ->
            log.exercises.forEach { exercise ->
                val muscles = currentMuscleAssignments(state, exercise)
                exercise.sets.filter { it.completed }.forEach { set ->
                    val reps = set.reps.toDoubleOrNull()
                    muscles.forEach { (muscleId, role) ->
                        val factor = muscleRoleFactor(role)
                        val acc = totals.getOrPut(muscleId) { Acc() }
                        acc.sets += factor
                        if (reps != null) {
                            acc.repsSum += reps * factor
                            acc.repsWeight += factor
                        }
                        set.rir?.let {
                            acc.rirSum += it * factor
                            acc.rirWeight += factor
                        }
                    }
                }
            }
        }
    return totals.mapValues { (_, acc) ->
        MusclePeriodStats(
            weightedSets = acc.sets,
            averageReps = acc.repsWeight.takeIf { it > 0 }?.let { acc.repsSum / it },
            averageRir = acc.rirWeight.takeIf { it > 0 }?.let { acc.rirSum / it },
        )
    }
}

internal fun calculateSessionPeriodStats(
    state: AppState,
    templateId: String,
    weeks: Int?,
    now: Long = System.currentTimeMillis(),
): SessionPeriodStats? {
    val cutoff = weeks?.let { now - it * 7L * 24 * 3600 * 1000 }
    val logs = state.workoutLogs.filter {
        it.status == WorkoutStatus.COMPLETED && it.deletedAt == null && it.templateId == templateId &&
            (cutoff == null || (it.endedAt ?: it.startedAt) >= cutoff)
    }
    if (logs.isEmpty()) return null

    val template = state.templates.firstOrNull { it.id == templateId }
    val templateEntries = template?.exercises.orEmpty().associateBy { it.exerciseId }
    val exerciseIds = linkedSetOf<String>().apply {
        addAll(template?.exercises.orEmpty().map { it.exerciseId })
        logs.forEach { log -> addAll(log.exercises.map { it.exerciseId }) }
    }
    val allCompletedSets = logs.flatMap { log ->
        log.exercises.flatMap { exercise -> exercise.sets.filter { it.completed } }
    }
    val completedCounts = logs.map { log ->
        log.exercises.sumOf { exercise -> exercise.sets.count { it.completed } }
    }
    val plannedCounts = logs.map { log -> plannedSetCount(template, log) }

    val exerciseAverages = exerciseIds.map { exerciseId ->
        val currentName = state.exercises.firstOrNull { it.id == exerciseId }?.name
        val snapshotName = logs.asSequence().flatMap { it.exercises.asSequence() }
            .firstOrNull { it.exerciseId == exerciseId }?.nameSnapshot
        val completed = logs.map { log ->
            log.exercises.firstOrNull { it.exerciseId == exerciseId }
                ?.sets?.count { it.completed } ?: 0
        }.average()
        val planned = logs.map { log ->
            log.exercises.firstOrNull { it.exerciseId == exerciseId }?.plannedSets
                ?: templateEntries[exerciseId]?.targetSets
                ?: 0
        }.average()
        SessionExerciseAverage(
            exerciseId = exerciseId,
            exerciseName = currentName ?: snapshotName ?: "Exercice",
            averageCompletedSets = completed,
            averagePlannedSets = planned,
        )
    }

    val totalPlanned = plannedCounts.sum()
    return SessionPeriodStats(
        sessionCount = logs.size,
        averageDurationSeconds = logs.map {
            ((it.endedAt ?: it.startedAt) - it.startedAt).coerceAtLeast(0) / 1000.0
        }.average(),
        averageCompletedSets = completedCounts.average(),
        averagePlannedSets = plannedCounts.average(),
        completionRate = if (totalPlanned > 0) completedCounts.sum().toDouble() / totalPlanned else 0.0,
        averageRir = allCompletedSets.mapNotNull { it.rir }.takeIf { it.isNotEmpty() }?.average(),
        averageRest = allCompletedSets.mapNotNull { it.restBeforeSeconds }.takeIf { it.isNotEmpty() }?.average(),
        exercises = exerciseAverages,
    )
}

class MainViewModel(private val repository: AppRepository) : ViewModel() {
    private val _state = MutableStateFlow<AppState?>(null)
    val state: StateFlow<AppState?> = _state.asStateFlow()
    val tab = MutableStateFlow(MainTab.JOURNAL)
    val draftMinimized = MutableStateFlow(false)
    private val saveMutex = Mutex()
    private var recentlyDeleted: WorkoutLog? = null

    init {
        viewModelScope.launch {
            val initial = repository.initialize()
            val purged = purgeDeleted(initial)
            _state.value = purged
            if (purged != initial) repository.save(purged)
        }
    }

    private fun id() = UUID.randomUUID().toString()

    private fun mutate(block: (AppState) -> AppState) {
        val next = _state.value?.let(block) ?: return
        _state.value = next
        viewModelScope.launch {
            saveMutex.withLock { repository.save(next) }
        }
    }

    fun activeDraft(): WorkoutLog? = _state.value?.workoutLogs?.firstOrNull {
        it.status == WorkoutStatus.DRAFT && it.deletedAt == null
    }

    fun activeProgram(): TrainingProgram? = _state.value?.programs?.firstOrNull {
        it.active && !it.archived
    }

    fun missedSlotCount(): Int {
        val program = activeProgram() ?: return 0
        if (program.trainingDays.isEmpty()) return 0
        val checked = program.scheduleCheckedThrough.takeIf { it.isNotBlank() }
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: LocalDate.now()
        var cursor = checked.plusDays(1)
        val yesterday = LocalDate.now().minusDays(1)
        var count = 0
        while (!cursor.isAfter(yesterday)) {
            if (cursor.dayOfWeek.value in program.trainingDays) count++
            cursor = cursor.plusDays(1)
        }
        return count
    }

    fun suggestedTemplate(): WorkoutTemplate? {
        val state = _state.value ?: return null
        val program = state.programs.firstOrNull { it.active && !it.archived } ?: return null
        if (program.templateCycle.isEmpty()) return null
        return state.templates.firstOrNull {
            it.id == program.templateCycle[program.nextIndex.mod(program.templateCycle.size)]
        }
    }

    fun startWorkout(templateId: String, advanceProgram: Boolean) {
        if (activeDraft() != null) return
        draftMinimized.value = false
        val now = System.currentTimeMillis()
        mutate { state ->
            val template = state.templates.first { it.id == templateId }
            val activeProgram = state.programs.firstOrNull { it.active && !it.archived }
            val loggedExercises = template.exercises.mapNotNull { entry ->
                val exercise = state.exercises.firstOrNull { it.id == entry.exerciseId } ?: return@mapNotNull null
                val lastPerformance = lastPerformedExercise(state, exercise.id)
                val repMin = entry.repMinOverride ?: exercise.defaultRepMin
                val repMax = entry.repMaxOverride ?: exercise.defaultRepMax
                val sets = (1..entry.targetSets).map { order ->
                    val previous = lastPerformance?.completedSetAt(order)
                    WorkoutSet(
                        id = id(),
                        order = order,
                        weightKg = previous?.weightKg.orEmpty(),
                        reps = previous?.reps.orEmpty(),
                    )
                }
                LoggedExercise(
                    id = id(),
                    exerciseId = exercise.id,
                    nameSnapshot = exercise.name,
                    instructionSnapshot = exercise.instruction,
                    repMinSnapshot = repMin,
                    repMaxSnapshot = repMax,
                    musclesSnapshot = exercise.muscles.mapNotNull { link ->
                        state.muscles.firstOrNull { it.id == link.muscleId }?.let {
                            muscle -> MuscleSnapshot(muscle.id, muscle.name, link.role)
                        }
                    },
                    plannedSets = entry.targetSets,
                    sets = sets,
                )
            }
            val log = WorkoutLog(
                id = id(),
                templateId = template.id,
                templateNameSnapshot = template.name,
                programId = activeProgram?.id?.takeIf { advanceProgram },
                programNameSnapshot = activeProgram?.name?.takeIf { advanceProgram },
                localDate = LocalDate.now().toString(),
                startedAt = now,
                exercises = loggedExercises,
                advanceProgramOnFinish = advanceProgram,
            )
            state.copy(workoutLogs = state.workoutLogs + log)
        }
    }

    fun updateWorkoutNote(note: String) = updateDraft { it.copy(note = note) }

    fun updateWorkoutDate(date: String) {
        if (runCatching { LocalDate.parse(date) }.isSuccess) updateDraft { it.copy(localDate = date) }
    }

    fun updateWorkoutDuration(durationSeconds: Int, now: Long = System.currentTimeMillis()) {
        updateDraft { workoutWithDuration(it, durationSeconds, now) }
    }

    private fun updateDraft(block: (WorkoutLog) -> WorkoutLog) {
        val draftId = activeDraft()?.id ?: return
        mutate { state ->
            state.copy(workoutLogs = state.workoutLogs.map { if (it.id == draftId) block(it) else it })
        }
    }

    fun updateSet(exerciseId: String, setId: String, weight: String? = null, reps: String? = null, rir: Int? = null, updateRir: Boolean = false) {
        updateDraft { log ->
            log.copy(exercises = log.exercises.map { exercise ->
                if (exercise.id != exerciseId) exercise else {
                    val edited = exercise.sets.firstOrNull { it.id == setId }
                    val recordsRest = edited != null &&
                        exercise.restStartedAt != null &&
                        exercise.restTargetSetOrder == edited.order
                    val autoRest = if (recordsRest) {
                        restSecondsAutoRecorded(exercise.restStartedAt, System.currentTimeMillis())
                    } else null
                    exercise.copy(
                        sets = exercise.sets.map { set ->
                            if (set.id != setId) set else set.copy(
                                weightKg = weight ?: set.weightKg,
                                reps = reps ?: set.reps,
                                rir = if (updateRir) rir else set.rir,
                                restBeforeSeconds = autoRest ?: set.restBeforeSeconds,
                                completed = false,
                            )
                        },
                        restStartedAt = if (recordsRest) null else exercise.restStartedAt,
                        restTargetSetOrder = if (recordsRest) null else exercise.restTargetSetOrder,
                    )
                }
            })
        }
    }

    fun validateSet(exerciseLogId: String, setId: String) {
        updateDraft { log ->
            val targetExercise = log.exercises.firstOrNull { it.id == exerciseLogId }
                ?: return@updateDraft log
            val target = targetExercise.sets.firstOrNull { it.id == setId }
                ?: return@updateDraft log
            if (!isSetInputValid(target) || target.completed) return@updateDraft log
            log.copy(exercises = log.exercises.map { exercise ->
                if (exercise.id != exerciseLogId) {
                    return@map exercise.copy(restStartedAt = null, restTargetSetOrder = null)
                }
                val updated = exercise.sets.map { if (it.id == setId) it.copy(completed = true) else it }
                val next = updated.firstOrNull { it.order > target.order && !it.completed }
                exercise.copy(
                    sets = updated,
                    restStartedAt = if (next != null) System.currentTimeMillis() else null,
                    restTargetSetOrder = next?.order,
                )
            })
        }
    }

    fun startNextSet(exerciseLogId: String) {
        updateDraft { log ->
            log.copy(exercises = log.exercises.map { exercise ->
                if (exercise.id != exerciseLogId || exercise.restStartedAt == null) return@map exercise
                val elapsed = ((System.currentTimeMillis() - exercise.restStartedAt) / 1000L)
                    .coerceIn(0, 24 * 60 * 60).toInt()
                exercise.copy(
                    sets = exercise.sets.map {
                        if (it.order == exercise.restTargetSetOrder) it.copy(restBeforeSeconds = elapsed) else it
                    },
                    restStartedAt = null,
                    restTargetSetOrder = null,
                )
            })
        }
    }

    fun ignoreRest(exerciseLogId: String) {
        updateDraft { log ->
            log.copy(exercises = log.exercises.map {
                if (it.id == exerciseLogId) it.copy(restStartedAt = null, restTargetSetOrder = null) else it
            })
        }
    }

    fun updateSetRest(exerciseLogId: String, setId: String, restSeconds: Int?) {
        updateDraft { workoutWithSetRest(it, exerciseLogId, setId, restSeconds) }
    }

    fun addSet(exerciseLogId: String) {
        updateDraft { log ->
            log.copy(exercises = log.exercises.map { exercise ->
                if (exercise.id != exerciseLogId) exercise else exercise.copy(
                    sets = exercise.sets + WorkoutSet(id(), (exercise.sets.maxOfOrNull { it.order } ?: 0) + 1),
                    plannedSets = exercise.plannedSets + 1,
                )
            })
        }
    }

    fun removeSet(exerciseLogId: String, setId: String) {
        updateDraft { log ->
            log.copy(exercises = log.exercises.map { exercise ->
                if (exercise.id != exerciseLogId || exercise.sets.size <= 1) exercise else {
                    val sets = exercise.sets.filterNot { it.id == setId }.mapIndexed { index, set ->
                        set.copy(order = index + 1)
                    }
                    exercise.copy(sets = sets, plannedSets = sets.size, restStartedAt = null, restTargetSetOrder = null)
                }
            })
        }
    }

    private fun loggedExerciseFor(state: AppState, exercise: Exercise): LoggedExercise = LoggedExercise(
        id = id(),
        exerciseId = exercise.id,
        nameSnapshot = exercise.name,
        instructionSnapshot = exercise.instruction,
        repMinSnapshot = exercise.defaultRepMin,
        repMaxSnapshot = exercise.defaultRepMax,
        musclesSnapshot = exercise.muscles.mapNotNull { link ->
            state.muscles.firstOrNull { it.id == link.muscleId }?.let {
                MuscleSnapshot(it.id, it.name, link.role)
            }
        },
        plannedSets = 1,
        sets = listOf(WorkoutSet(id(), 1)),
    )

    fun addExerciseToDraft(exerciseId: String) {
        val state = _state.value ?: return
        val exercise = state.exercises.firstOrNull { it.id == exerciseId } ?: return
        val logged = loggedExerciseFor(state, exercise)
        updateDraft { it.copy(exercises = it.exercises + logged) }
    }

    fun createExerciseInDraft(exercise: Exercise) {
        mutate { state ->
            val draft = state.workoutLogs.firstOrNull {
                it.status == WorkoutStatus.DRAFT && it.deletedAt == null
            } ?: return@mutate state
            val created = exercise.copy(id = id())
            state.copy(
                exercises = state.exercises + created,
                workoutLogs = state.workoutLogs.map {
                    if (it.id == draft.id) it.copy(exercises = it.exercises + loggedExerciseFor(state, created)) else it
                },
            )
        }
    }

    fun removeExerciseFromDraft(exerciseLogId: String) {
        updateDraft { it.copy(exercises = it.exercises.filterNot { ex -> ex.id == exerciseLogId }) }
    }

    fun moveExerciseInDraft(exerciseLogId: String, delta: Int) {
        updateDraft { it.copy(exercises = movedExerciseLogs(it.exercises, exerciseLogId, delta)) }
    }

    fun finishWorkout() {
        val draft = activeDraft() ?: return
        val finishedExercises = normalizedFinishedExercises(draft.exercises)
        if (finishedExercises.none { exercise -> exercise.sets.any { it.completed } }) return
        draftMinimized.value = false
        val now = System.currentTimeMillis()
        mutate { state ->
            var programs = state.programs
            var events = state.programEvents
            if (draft.advanceProgramOnFinish && !draft.editingCompletedLog && draft.programId != null && draft.templateId != null) {
                programs = programs.map { program ->
                    if (program.id != draft.programId || program.templateCycle.isEmpty()) program
                    else {
                        val usedIndex = program.templateCycle.indexOf(draft.templateId).takeIf { it >= 0 }
                            ?: program.nextIndex
                        program.copy(
                            nextIndex = (usedIndex + 1).mod(program.templateCycle.size),
                            scheduleCheckedThrough = LocalDate.now().toString(),
                        )
                    }
                }
                events = events + ProgramEvent(id(), draft.programId, draft.templateId, draft.localDate, "COMPLETED", draft.id)
            }
            state.copy(
                programs = programs,
                programEvents = events,
                workoutLogs = state.workoutLogs.map {
                    if (it.id == draft.id) draft.copy(
                        exercises = finishedExercises,
                        endedAt = if (draft.editingCompletedLog) draft.endedAt ?: now else now,
                        status = WorkoutStatus.COMPLETED,
                        editingCompletedLog = false,
                    ) else it
                }
            )
        }
    }

    fun minimizeDraft() {
        draftMinimized.value = true
    }

    fun resumeDraft() {
        draftMinimized.value = false
    }

    fun abandonDraft() {
        draftMinimized.value = false
        val draft = activeDraft() ?: return
        mutate { state ->
            state.copy(workoutLogs = if (draft.editingCompletedLog) {
                state.workoutLogs.map {
                    if (it.id == draft.id) it.copy(
                        status = WorkoutStatus.COMPLETED,
                        editingCompletedLog = false,
                        exercises = it.exercises.map { exercise ->
                            exercise.copy(restStartedAt = null, restTargetSetOrder = null)
                        },
                    ) else it
                }
            } else {
                state.workoutLogs.filterNot { it.id == draft.id }
            })
        }
    }

    fun editWorkout(logId: String) {
        if (activeDraft() != null) return
        mutate { state ->
            state.copy(workoutLogs = state.workoutLogs.map {
                if (it.id == logId && it.status == WorkoutStatus.COMPLETED) it.copy(
                    status = WorkoutStatus.DRAFT,
                    editingCompletedLog = true,
                    exercises = it.exercises.map { ex ->
                        ex.copy(restStartedAt = null, restTargetSetOrder = null)
                    },
                ) else it
            })
        }
    }

    fun deleteWorkout(logId: String) {
        val log = _state.value?.workoutLogs?.firstOrNull { it.id == logId } ?: return
        recentlyDeleted = log
        mutate { state ->
            state.copy(workoutLogs = state.workoutLogs.map {
                if (it.id == logId) it.copy(deletedAt = System.currentTimeMillis()) else it
            })
        }
    }

    fun undoDelete() {
        val deleted = recentlyDeleted ?: return
        mutate { state ->
            state.copy(workoutLogs = state.workoutLogs.map {
                if (it.id == deleted.id) deleted.copy(deletedAt = null) else it
            })
        }
        recentlyDeleted = null
    }

    fun saveBodyWeight(dateValue: String, weightValue: String): Result<Unit> = runCatching {
        val date = LocalDate.parse(dateValue)
        require(!date.isAfter(LocalDate.now())) { "La date ne peut pas être dans le futur" }
        val weight = requireNotNull(normalizedWeightKg(weightValue)) { "Poids invalide" }
        val now = System.currentTimeMillis()
        mutate { state ->
            val existing = state.bodyWeights.firstOrNull { it.date == date.toString() }
            val entry = existing?.copy(weightKg = weight, updatedAt = now)
                ?: BodyWeightEntry(id(), date.toString(), weight, now)
            state.copy(
                bodyWeights = (state.bodyWeights.filterNot { it.date == date.toString() } + entry)
                    .sortedBy { it.date },
            )
        }
    }

    fun deleteBodyWeight(date: String) = mutate { state ->
        state.copy(bodyWeights = state.bodyWeights.filterNot { it.date == date })
    }

    fun saveWeightGoal(weightValue: String): Result<Unit> = runCatching {
        val goal = if (weightValue.isBlank()) null
        else requireNotNull(normalizedWeightKg(weightValue)) { "Objectif poids invalide" }
        mutate { it.copy(weightGoalKg = goal) }
    }

    fun bodyWeightTrend(weeks: Int?): List<BodyWeightTrendPoint> =
        calculateBodyWeightTrend(_state.value?.bodyWeights.orEmpty(), weeks)

    fun saveNutritionEntry(
        entryId: String?,
        dateValue: String,
        caloriesValue: String,
        proteinValue: String,
    ): Result<Unit> = runCatching {
        val date = LocalDate.parse(dateValue)
        require(!date.isAfter(LocalDate.now())) { "La date ne peut pas être dans le futur" }
        val calories = requireNotNull(normalizedCalories(caloriesValue)) { "Calories invalides" }
        val protein = requireNotNull(normalizedProteinGrams(proteinValue)) { "Protéines invalides" }
        val now = System.currentTimeMillis()
        mutate { state ->
            val existing = entryId?.let { id -> state.nutritionEntries.firstOrNull { it.id == id } }
            require(entryId == null || existing != null) { "Apport introuvable" }
            val entry = existing?.copy(
                date = date.toString(),
                caloriesKcal = calories,
                proteinGrams = protein,
                updatedAt = now,
            ) ?: NutritionEntry(id(), date.toString(), calories, protein, now)
            state.copy(
                nutritionEntries = (state.nutritionEntries.filterNot { it.id == entry.id } + entry)
                    .sortedWith(compareBy<NutritionEntry> { it.date }.thenBy { it.createdAt }),
            )
        }
    }

    fun deleteNutritionEntry(entryId: String) = mutate { state ->
        state.copy(nutritionEntries = state.nutritionEntries.filterNot { it.id == entryId })
    }

    fun saveNutritionTargets(caloriesValue: String, proteinValue: String): Result<Unit> = runCatching {
        val calories = if (caloriesValue.isBlank()) null
        else requireNotNull(normalizedCalories(caloriesValue)) { "Objectif calories invalide" }
        val protein = if (proteinValue.isBlank()) null
        else requireNotNull(normalizedProteinGrams(proteinValue)) { "Objectif protéines invalide" }
        mutate { it.copy(nutritionTargets = NutritionTargets(calories, protein)) }
    }

    fun nutritionTrend(weeks: Int?): List<NutritionDayTotal> =
        calculateNutritionTrend(_state.value?.nutritionEntries.orEmpty(), weeks)

    fun archiveExercise(exerciseId: String) = mutate { state ->
        state.copy(exercises = state.exercises.map {
            if (it.id == exerciseId) it.copy(archived = !it.archived) else it
        })
    }

    fun deleteExercise(exerciseId: String) = mutate { stateAfterExerciseDeletion(it, exerciseId) }

    fun saveExercise(exercise: Exercise) = mutate { state ->
        val exists = state.exercises.any { it.id == exercise.id }
        state.copy(exercises = if (exists) state.exercises.map {
            if (it.id == exercise.id) exercise else it
        } else state.exercises + exercise.copy(id = id()))
    }

    fun archiveMuscle(muscleId: String) = mutate { state ->
        state.copy(muscles = state.muscles.map {
            if (it.id == muscleId) it.copy(archived = !it.archived) else it
        })
    }

    fun deleteMuscle(muscleId: String) = mutate { stateAfterMuscleDeletion(it, muscleId) }

    fun saveMuscle(muscle: MuscleGroup) = mutate { state ->
        val exists = state.muscles.any { it.id == muscle.id }
        state.copy(muscles = if (exists) state.muscles.map {
            if (it.id == muscle.id) muscle else it
        } else state.muscles + muscle.copy(id = id()))
    }

    fun saveTemplate(template: WorkoutTemplate) = mutate { state ->
        val exists = state.templates.any { it.id == template.id }
        state.copy(templates = if (exists) state.templates.map {
            if (it.id == template.id) template else it
        } else state.templates + template.copy(id = id()))
    }

    fun archiveTemplate(templateId: String) = mutate { state ->
        state.copy(templates = state.templates.map {
            if (it.id == templateId) it.copy(archived = !it.archived) else it
        })
    }

    fun deleteTemplate(templateId: String) = mutate { stateAfterTemplateDeletion(it, templateId) }

    fun saveProgram(program: TrainingProgram) = mutate { state ->
        val normalized = if (program.active) {
            state.programs.map { it.copy(active = false) }
        } else state.programs
        val exists = normalized.any { it.id == program.id }
        state.copy(programs = if (exists) normalized.map {
            if (it.id == program.id) program else it
        } else normalized + program.copy(id = id()))
    }

    fun archiveProgram(programId: String) = mutate { state ->
        state.copy(programs = state.programs.map {
            if (it.id == programId) it.copy(archived = !it.archived, active = false) else it
        })
    }

    fun activateProgram(programId: String) = mutate { state ->
        state.copy(programs = state.programs.map { program ->
            program.copy(active = program.id == programId && !program.archived)
        })
    }

    fun deleteProgram(programId: String) = mutate { state ->
        state.copy(
            programs = state.programs.filterNot { it.id == programId },
            programEvents = state.programEvents.filterNot { it.programId == programId },
        )
    }

    fun skipMissedSlots(count: Int) {
        val program = activeProgram() ?: return
        if (program.templateCycle.isEmpty() || count <= 0) return
        mutate { state ->
            var index = program.nextIndex
            val events = (0 until count).map {
                val templateId = program.templateCycle[index.mod(program.templateCycle.size)]
                index = (index + 1).mod(program.templateCycle.size)
                ProgramEvent(id(), program.id, templateId, LocalDate.now().toString(), "SKIPPED")
            }
            state.copy(
                programs = state.programs.map {
                    if (it.id == program.id) it.copy(
                        nextIndex = index,
                        scheduleCheckedThrough = LocalDate.now().minusDays(1).toString(),
                    ) else it
                },
                programEvents = state.programEvents + events,
            )
        }
    }

    fun acknowledgeMissedSlots() {
        val program = activeProgram() ?: return
        mutate { state ->
            state.copy(programs = state.programs.map {
                if (it.id == program.id) it.copy(
                    scheduleCheckedThrough = LocalDate.now().minusDays(1).toString(),
                ) else it
            })
        }
    }

    fun exportBackup(): String = _state.value?.let(repository::encode).orEmpty()

    fun restoreBackup(json: String): Result<Unit> = runCatching {
        val decoded = repository.decode(json)
        require(decoded.muscles.isNotEmpty()) { "Sauvegarde vide" }
        _state.value = decoded
        viewModelScope.launch { saveMutex.withLock { repository.save(decoded) } }
    }

    fun replaceStateForTest(state: AppState) {
        _state.value = state
    }

    private fun purgeDeleted(state: AppState): AppState {
        val cutoff = System.currentTimeMillis() - 30_000
        return state.copy(workoutLogs = state.workoutLogs.filterNot {
            it.deletedAt != null && it.deletedAt < cutoff
        })
    }

    fun exerciseHistory(exerciseId: String, weeks: Int?): List<ExerciseHistoryPoint> {
        val cutoff = weeks?.let { System.currentTimeMillis() - it * 7L * 24 * 3600 * 1000 }
        return _state.value?.workoutLogs.orEmpty()
            .filter { it.status == WorkoutStatus.COMPLETED && it.deletedAt == null && (cutoff == null || (it.endedAt ?: it.startedAt) >= cutoff) }
            .flatMap { log ->
                log.exercises.filter { it.exerciseId == exerciseId }.flatMap { exercise ->
                    exercise.sets.filter { it.completed }.mapNotNull { set ->
                        val weight = set.weightKg.replace(',', '.').toFloatOrNull() ?: return@mapNotNull null
                        val reps = set.reps.toFloatOrNull() ?: return@mapNotNull null
                        ExerciseHistoryPoint(
                            workoutChronologyTimestamp(log), log.localDate, set.order,
                            weight, reps, set.rir, set.restBeforeSeconds,
                            exercise.repMinSnapshot, exercise.repMaxSnapshot,
                        )
                    }
                }
            }.sortedBy { it.timestamp }
    }

    fun sessionSummaries(templateId: String, weeks: Int?): List<SessionSummary> {
        val cutoff = weeks?.let { System.currentTimeMillis() - it * 7L * 24 * 3600 * 1000 }
        val template = _state.value?.templates?.firstOrNull { it.id == templateId }
        return _state.value?.workoutLogs.orEmpty().filter {
            it.status == WorkoutStatus.COMPLETED && it.deletedAt == null && it.templateId == templateId &&
                (cutoff == null || (it.endedAt ?: it.startedAt) >= cutoff)
        }.sortedBy { workoutChronologyTimestamp(it) }.map { log ->
            val sets = log.exercises.flatMap { it.sets }.filter { it.completed }
            SessionSummary(
                log.templateNameSnapshot,
                log.localDate,
                workoutChronologyTimestamp(log),
                ((log.endedAt ?: log.startedAt) - log.startedAt).coerceAtLeast(0) / 1000,
                sets.size,
                plannedSetCount(template, log),
                sets.mapNotNull { it.rir }.takeIf { it.isNotEmpty() }?.average(),
                sets.mapNotNull { it.restBeforeSeconds }.takeIf { it.isNotEmpty() }?.average(),
            )
        }
    }

    fun sessionPeriodStats(templateId: String, weeks: Int?): SessionPeriodStats? {
        val state = _state.value ?: return null
        return calculateSessionPeriodStats(state, templateId, weeks)
    }

    fun muscleWeeklyVolume(weeks: Int?): List<MuscleWeekVolume> {
        val state = _state.value ?: return emptyList()
        val cutoffDate = weeks?.let { LocalDate.now().minusWeeks(it.toLong() - 1) }
        return state.workoutLogs
            .filter { it.status == WorkoutStatus.COMPLETED && it.deletedAt == null }
            .groupBy { log ->
                LocalDate.parse(log.localDate).with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
            }
            .filterKeys { cutoffDate == null || !it.isBefore(cutoffDate.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))) }
            .toSortedMap()
            .map { (week, logs) ->
                val volumes = mutableMapOf<String, Double>()
                logs.forEach { log ->
                    log.exercises.forEach { exercise ->
                        val setCount = exercise.sets.count { it.completed }
                        currentMuscleAssignments(state, exercise).forEach { muscle ->
                            val factor = muscleRoleFactor(muscle.second)
                            volumes[muscle.first] = (volumes[muscle.first] ?: 0.0) + setCount * factor
                        }
                    }
                }
                MuscleWeekVolume(week.toString(), volumes)
            }
    }

    fun musclePeriodStats(weeks: Int?): Map<String, MusclePeriodStats> {
        val state = _state.value ?: return emptyMap()
        return calculateMusclePeriodStats(state, weeks)
    }

    companion object {
        fun factory(repository: AppRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    MainViewModel(repository) as T
            }
    }
}
