package fr.suivimuscu.app.data

import kotlinx.serialization.Serializable

@Serializable
enum class MuscleRole { PRIMARY, SECONDARY, TERTIARY }

@Serializable
enum class WorkoutStatus { DRAFT, COMPLETED }

@Serializable
data class MuscleGroup(
    val id: String,
    val name: String,
    val archived: Boolean = false,
)

@Serializable
data class MuscleAssignment(
    val muscleId: String,
    val role: MuscleRole,
)

@Serializable
data class Exercise(
    val id: String,
    val name: String,
    val defaultRepMin: Int,
    val defaultRepMax: Int,
    val instruction: String = "",
    val muscles: List<MuscleAssignment> = emptyList(),
    val archived: Boolean = false,
)

@Serializable
data class TemplateExercise(
    val exerciseId: String,
    val targetSets: Int = 2,
    val repMinOverride: Int? = null,
    val repMaxOverride: Int? = null,
)

@Serializable
data class WorkoutTemplate(
    val id: String,
    val name: String,
    val exercises: List<TemplateExercise>,
    val archived: Boolean = false,
)

@Serializable
data class TrainingProgram(
    val id: String,
    val name: String,
    val templateCycle: List<String>,
    val trainingDays: List<Int> = emptyList(),
    val nextIndex: Int = 0,
    val scheduleCheckedThrough: String = "",
    val active: Boolean = false,
    val archived: Boolean = false,
)

@Serializable
data class ProgramEvent(
    val id: String,
    val programId: String,
    val templateId: String,
    val date: String,
    val outcome: String,
    val workoutLogId: String? = null,
)

@Serializable
data class MuscleSnapshot(
    val muscleId: String,
    val name: String,
    val role: MuscleRole,
)

@Serializable
data class WorkoutSet(
    val id: String,
    val order: Int,
    val weightKg: String = "",
    val reps: String = "",
    val rir: Int? = null,
    val restBeforeSeconds: Int? = null,
    val completed: Boolean = false,
)

@Serializable
data class LoggedExercise(
    val id: String,
    val exerciseId: String,
    val nameSnapshot: String,
    val instructionSnapshot: String = "",
    val repMinSnapshot: Int,
    val repMaxSnapshot: Int,
    val musclesSnapshot: List<MuscleSnapshot>,
    val plannedSets: Int,
    val sets: List<WorkoutSet>,
    val restStartedAt: Long? = null,
    val restTargetSetOrder: Int? = null,
)

@Serializable
data class WorkoutLog(
    val id: String,
    val templateId: String?,
    val templateNameSnapshot: String,
    val programId: String? = null,
    val programNameSnapshot: String? = null,
    val localDate: String,
    val startedAt: Long,
    val endedAt: Long? = null,
    val note: String = "",
    val status: WorkoutStatus = WorkoutStatus.DRAFT,
    val exercises: List<LoggedExercise>,
    val advanceProgramOnFinish: Boolean = false,
    val editingCompletedLog: Boolean = false,
    val deletedAt: Long? = null,
)

@Serializable
data class BodyWeightEntry(
    val id: String,
    val date: String,
    val weightKg: Double,
    val createdAt: Long,
    val updatedAt: Long = createdAt,
)

@Serializable
data class NutritionEntry(
    val id: String,
    val date: String,
    val caloriesKcal: Int,
    val proteinGrams: Double,
    val createdAt: Long,
    val updatedAt: Long = createdAt,
)

@Serializable
data class AppState(
    val schemaVersion: Int = 4,
    val muscles: List<MuscleGroup> = emptyList(),
    val exercises: List<Exercise> = emptyList(),
    val templates: List<WorkoutTemplate> = emptyList(),
    val programs: List<TrainingProgram> = emptyList(),
    val programEvents: List<ProgramEvent> = emptyList(),
    val workoutLogs: List<WorkoutLog> = emptyList(),
    val bodyWeights: List<BodyWeightEntry> = emptyList(),
    val nutritionEntries: List<NutritionEntry> = emptyList(),
)

data class ExerciseHistoryPoint(
    val timestamp: Long,
    val date: String,
    val setOrder: Int,
    val weight: Float,
    val reps: Float,
    val rir: Int?,
    val restSeconds: Int?,
    val repMin: Int,
    val repMax: Int,
)

data class SessionSummary(
    val templateName: String,
    val date: String,
    val timestamp: Long,
    val durationSeconds: Long,
    val completedSets: Int,
    val plannedSets: Int,
    val averageRir: Double?,
    val averageRest: Double?,
)

data class SessionExerciseAverage(
    val exerciseId: String,
    val exerciseName: String,
    val averageCompletedSets: Double,
    val averagePlannedSets: Double,
)

data class SessionPeriodStats(
    val sessionCount: Int,
    val averageDurationSeconds: Double,
    val averageCompletedSets: Double,
    val averagePlannedSets: Double,
    val completionRate: Double,
    val averageRir: Double?,
    val averageRest: Double?,
    val exercises: List<SessionExerciseAverage>,
)

data class MuscleWeekVolume(
    val weekLabel: String,
    val volumes: Map<String, Double>,
)

data class MusclePeriodStats(
    val weightedSets: Double,
    val averageReps: Double?,
    val averageRir: Double?,
)

data class BodyWeightTrendPoint(
    val date: String,
    val timestamp: Long,
    val weightKg: Double,
    val average7DaysKg: Double,
)

data class NutritionDayTotal(
    val date: String,
    val timestamp: Long,
    val caloriesKcal: Int,
    val proteinGrams: Double,
    val entryCount: Int,
)
