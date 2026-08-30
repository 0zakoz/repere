package fr.suivimuscu.app.data

import java.time.LocalDate

object SeedData {
    private fun assignment(id: String, role: MuscleRole) = MuscleAssignment(id, role)

    fun create(): AppState {
        val muscles = listOf(
            MuscleGroup("pecs", "Pectoraux"),
            MuscleGroup("upper_pecs", "Haut des pectoraux"),
            MuscleGroup("lats", "Dorsaux"),
            MuscleGroup("traps", "Trapèzes"),
            MuscleGroup("lower_back", "Lombaires"),
            MuscleGroup("front_delts", "Deltoïdes antérieurs"),
            MuscleGroup("side_delts", "Deltoïdes latéraux"),
            MuscleGroup("rear_delts", "Deltoïdes postérieurs"),
            MuscleGroup("biceps", "Biceps"),
            MuscleGroup("triceps", "Triceps"),
            MuscleGroup("forearm_flexors", "Fléchisseurs de l’avant-bras"),
            MuscleGroup("forearm_extensors", "Extenseurs de l’avant-bras"),
            MuscleGroup("quads", "Quadriceps"),
            MuscleGroup("hamstrings", "Ischio-jambiers"),
            MuscleGroup("glutes", "Fessiers"),
            MuscleGroup("adductors", "Adducteurs"),
            MuscleGroup("calves", "Mollets"),
            MuscleGroup("abs", "Abdominaux"),
        )

        val exercises = listOf(
            Exercise("chest_press", "Press pecs", 6, 10, muscles = listOf(
                assignment("pecs", MuscleRole.PRIMARY),
                assignment("upper_pecs", MuscleRole.SECONDARY),
                assignment("front_delts", MuscleRole.SECONDARY),
            )),
            Exercise("lat_pulldown", "Tirage vertical", 6, 10, muscles = listOf(
                assignment("lats", MuscleRole.PRIMARY),
                assignment("forearm_flexors", MuscleRole.TERTIARY),
                assignment("forearm_extensors", MuscleRole.TERTIARY),
                assignment("traps", MuscleRole.SECONDARY),
            )),
            Exercise("triceps_extension", "Extension triceps", 6, 12, muscles = listOf(
                assignment("triceps", MuscleRole.PRIMARY),
            )),
            Exercise("biceps_curl", "Curl biceps", 6, 12, muscles = listOf(
                assignment("biceps", MuscleRole.PRIMARY),
                assignment("forearm_flexors", MuscleRole.SECONDARY),
                assignment("forearm_extensors", MuscleRole.TERTIARY),
            )),
            Exercise("lateral_raise", "Élévations latérales", 8, 12, muscles = listOf(
                assignment("side_delts", MuscleRole.PRIMARY),
            )),
            Exercise("forearm_extension", "Extension avant-bras", 8, 15,
                instruction = "Reverse curl avec sangles de tirage",
                muscles = listOf(
                    assignment("forearm_extensors", MuscleRole.PRIMARY),
                    assignment("forearm_flexors", MuscleRole.TERTIARY),
                )),
            Exercise("wrist_flexion", "Flexion poignets", 8, 15, muscles = listOf(
                assignment("forearm_flexors", MuscleRole.PRIMARY),
            )),
            Exercise("horizontal_row", "Tirage horizontal", 6, 10, muscles = listOf(
                assignment("traps", MuscleRole.PRIMARY),
                assignment("lats", MuscleRole.SECONDARY),
                assignment("forearm_flexors", MuscleRole.TERTIARY),
                assignment("forearm_extensors", MuscleRole.TERTIARY),
                assignment("rear_delts", MuscleRole.SECONDARY),
            )),
            Exercise("vertical_press", "Press vertical", 6, 10, muscles = listOf(
                assignment("front_delts", MuscleRole.PRIMARY),
                assignment("triceps", MuscleRole.SECONDARY),
                assignment("side_delts", MuscleRole.SECONDARY),
                assignment("upper_pecs", MuscleRole.SECONDARY),
            )),
            Exercise("back_extension", "Back extension lesté", 8, 12, muscles = listOf(
                assignment("lower_back", MuscleRole.PRIMARY),
                assignment("glutes", MuscleRole.SECONDARY),
                assignment("hamstrings", MuscleRole.SECONDARY),
            )),
            Exercise("hack_squat", "Hack squat", 5, 9, muscles = listOf(
                assignment("quads", MuscleRole.PRIMARY),
                assignment("glutes", MuscleRole.SECONDARY),
                assignment("adductors", MuscleRole.SECONDARY),
            )),
            Exercise("calf_raise", "Mollets", 8, 15, muscles = listOf(
                assignment("calves", MuscleRole.PRIMARY),
            )),
            Exercise("rear_delt_fly", "Rear delt fly", 8, 12, muscles = listOf(
                assignment("rear_delts", MuscleRole.PRIMARY),
            )),
        )

        val sessionA = WorkoutTemplate(
            id = "session_a",
            name = "A",
            exercises = listOf(
                "chest_press", "lat_pulldown", "triceps_extension", "biceps_curl",
                "lateral_raise", "forearm_extension", "wrist_flexion",
            ).map { TemplateExercise(it, 2) },
        )
        val sessionB = WorkoutTemplate(
            id = "session_b",
            name = "B",
            exercises = listOf(
                "horizontal_row", "vertical_press", "back_extension",
                "hack_squat", "calf_raise", "rear_delt_fly",
            ).map { TemplateExercise(it, 2) },
        )
        val program = TrainingProgram(
            id = "full_body",
            name = "Full body",
            templateCycle = listOf(sessionA.id, sessionB.id),
            trainingDays = listOf(1, 3, 5),
            scheduleCheckedThrough = LocalDate.now().toString(),
            active = true,
        )
        return AppState(
            muscles = muscles,
            exercises = exercises,
            templates = listOf(sessionA, sessionB),
            programs = listOf(program),
        )
    }
}
