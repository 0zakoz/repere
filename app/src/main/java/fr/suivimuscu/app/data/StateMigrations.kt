package fr.suivimuscu.app.data

internal const val LATEST_SCHEMA_VERSION = 5

internal object StateMigrations {
    private const val LEGACY_FOREARMS = "forearms"
    private const val FOREARM_FLEXORS = "forearm_flexors"
    private const val FOREARM_EXTENSORS = "forearm_extensors"

    fun toLatest(state: AppState): AppState {
        require(state.schemaVersion in 1..LATEST_SCHEMA_VERSION) {
            "Version de sauvegarde incompatible"
        }
        var migrated = state
        if (migrated.schemaVersion < 2) migrated = migrateToVersion2(migrated)
        if (migrated.schemaVersion < 3) migrated = migrateToVersion3(migrated)
        if (migrated.schemaVersion < 4) migrated = migrateToVersion4(migrated)
        if (migrated.schemaVersion < 5) migrated = migrateToVersion5(migrated)
        return migrated
    }

    private fun migrateToVersion5(state: AppState): AppState = state.copy(schemaVersion = 5)

    private fun migrateToVersion4(state: AppState): AppState = state.copy(schemaVersion = 4)

    private fun migrateToVersion3(state: AppState): AppState = state.copy(schemaVersion = 3)

    private fun migrateToVersion2(state: AppState): AppState {
        val legacyIndex = state.muscles.indexOfFirst { it.id == LEGACY_FOREARMS }
        val legacy = state.muscles.getOrNull(legacyIndex)
        val newMuscles = state.muscles
            .filterNot { it.id in setOf(LEGACY_FOREARMS, FOREARM_FLEXORS, FOREARM_EXTENSORS) }
            .toMutableList()
            .also { muscles ->
                val insertAt = legacyIndex.coerceIn(0, muscles.size)
                muscles.add(insertAt, MuscleGroup(FOREARM_FLEXORS, "Fléchisseurs de l’avant-bras", legacy?.archived ?: false))
                muscles.add(insertAt + 1, MuscleGroup(FOREARM_EXTENSORS, "Extenseurs de l’avant-bras", legacy?.archived ?: false))
            }

        fun migratedAssignments(exerciseId: String, assignments: List<MuscleAssignment>): List<MuscleAssignment> {
            val legacyRole = assignments.firstOrNull { it.muscleId == LEGACY_FOREARMS }?.role
                ?: return assignments
            val retained = assignments.filterNot { it.muscleId == LEGACY_FOREARMS }.toMutableList()
            fun add(muscleId: String, role: MuscleRole) {
                if (retained.none { it.muscleId == muscleId }) retained += MuscleAssignment(muscleId, role)
            }
            when (exerciseId) {
                "wrist_flexion" -> add(FOREARM_FLEXORS, legacyRole)
                "forearm_extension" -> {
                    add(FOREARM_EXTENSORS, legacyRole)
                    add(FOREARM_FLEXORS, MuscleRole.TERTIARY)
                }
                "biceps_curl" -> {
                    add(FOREARM_FLEXORS, legacyRole)
                    add(FOREARM_EXTENSORS, MuscleRole.TERTIARY)
                }
                "lat_pulldown", "horizontal_row" -> {
                    add(FOREARM_FLEXORS, MuscleRole.TERTIARY)
                    add(FOREARM_EXTENSORS, MuscleRole.TERTIARY)
                }
                else -> {
                    add(FOREARM_FLEXORS, legacyRole)
                    add(FOREARM_EXTENSORS, legacyRole)
                }
            }
            return retained
        }

        fun migratedSnapshots(exerciseId: String, snapshots: List<MuscleSnapshot>): List<MuscleSnapshot> {
            val assignments = migratedAssignments(
                exerciseId,
                snapshots.map { MuscleAssignment(it.muscleId, it.role) },
            )
            val names = newMuscles.associate { it.id to it.name }
            return assignments.map { assignment ->
                val old = snapshots.firstOrNull { it.muscleId == assignment.muscleId }
                MuscleSnapshot(
                    muscleId = assignment.muscleId,
                    name = old?.name ?: names[assignment.muscleId].orEmpty(),
                    role = assignment.role,
                )
            }
        }

        return state.copy(
            schemaVersion = 2,
            muscles = newMuscles,
            exercises = state.exercises.map { exercise ->
                exercise.copy(muscles = migratedAssignments(exercise.id, exercise.muscles))
            },
            workoutLogs = state.workoutLogs.map { log ->
                log.copy(exercises = log.exercises.map { exercise ->
                    exercise.copy(
                        musclesSnapshot = migratedSnapshots(exercise.exerciseId, exercise.musclesSnapshot),
                    )
                })
            },
        )
    }
}
