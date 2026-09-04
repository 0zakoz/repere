const muscle = (id, name) => ({ id, name, archived: false });
const assignment = (muscleId, role) => ({ muscleId, role });
const exercise = (id, name, min, max, muscles, instruction = "") => ({
  id, name, defaultRepMin: min, defaultRepMax: max, instruction, muscles, archived: false,
});

export function createSeedState(today = new Date()) {
  const date = localDate(today);
  const muscles = [
    muscle("pecs", "Pectoraux"), muscle("upper_pecs", "Haut des pectoraux"),
    muscle("lats", "Dorsaux"), muscle("traps", "Trapèzes"), muscle("lower_back", "Lombaires"),
    muscle("front_delts", "Deltoïdes antérieurs"), muscle("side_delts", "Deltoïdes latéraux"),
    muscle("rear_delts", "Deltoïdes postérieurs"), muscle("biceps", "Biceps"),
    muscle("triceps", "Triceps"), muscle("forearm_flexors", "Fléchisseurs de l’avant-bras"),
    muscle("forearm_extensors", "Extenseurs de l’avant-bras"), muscle("quads", "Quadriceps"),
    muscle("hamstrings", "Ischio-jambiers"), muscle("glutes", "Fessiers"),
    muscle("adductors", "Adducteurs"), muscle("calves", "Mollets"), muscle("abs", "Abdominaux"),
  ];
  const exercises = [
    exercise("chest_press", "Press pecs", 6, 10, [assignment("pecs", "PRIMARY"), assignment("upper_pecs", "SECONDARY"), assignment("front_delts", "SECONDARY")]),
    exercise("lat_pulldown", "Tirage vertical", 6, 10, [assignment("lats", "PRIMARY"), assignment("forearm_flexors", "TERTIARY"), assignment("forearm_extensors", "TERTIARY"), assignment("traps", "SECONDARY")]),
    exercise("triceps_extension", "Extension triceps", 6, 12, [assignment("triceps", "PRIMARY")]),
    exercise("biceps_curl", "Curl biceps", 6, 12, [assignment("biceps", "PRIMARY"), assignment("forearm_flexors", "SECONDARY"), assignment("forearm_extensors", "TERTIARY")]),
    exercise("lateral_raise", "Élévations latérales", 8, 12, [assignment("side_delts", "PRIMARY")]),
    exercise("forearm_extension", "Extension avant-bras", 8, 15, [assignment("forearm_extensors", "PRIMARY"), assignment("forearm_flexors", "TERTIARY")], "Reverse curl avec sangles de tirage"),
    exercise("wrist_flexion", "Flexion poignets", 8, 15, [assignment("forearm_flexors", "PRIMARY")]),
    exercise("horizontal_row", "Tirage horizontal", 6, 10, [assignment("traps", "PRIMARY"), assignment("lats", "SECONDARY"), assignment("forearm_flexors", "TERTIARY"), assignment("forearm_extensors", "TERTIARY"), assignment("rear_delts", "SECONDARY")]),
    exercise("vertical_press", "Press vertical", 6, 10, [assignment("front_delts", "PRIMARY"), assignment("triceps", "SECONDARY"), assignment("side_delts", "SECONDARY"), assignment("upper_pecs", "SECONDARY")]),
    exercise("back_extension", "Back extension lesté", 8, 12, [assignment("lower_back", "PRIMARY"), assignment("glutes", "SECONDARY"), assignment("hamstrings", "SECONDARY")]),
    exercise("hack_squat", "Hack squat", 5, 9, [assignment("quads", "PRIMARY"), assignment("glutes", "SECONDARY"), assignment("adductors", "SECONDARY")]),
    exercise("calf_raise", "Mollets", 8, 15, [assignment("calves", "PRIMARY")]),
    exercise("rear_delt_fly", "Rear delt fly", 8, 12, [assignment("rear_delts", "PRIMARY")]),
  ];
  const template = (id, name, ids) => ({
    id, name, archived: false,
    exercises: ids.map(exerciseId => ({ exerciseId, targetSets: 2, repMinOverride: null, repMaxOverride: null })),
  });
  const templates = [
    template("session_a", "A", ["chest_press", "lat_pulldown", "triceps_extension", "biceps_curl", "lateral_raise", "forearm_extension", "wrist_flexion"]),
    template("session_b", "B", ["horizontal_row", "vertical_press", "back_extension", "hack_squat", "calf_raise", "rear_delt_fly"]),
  ];
  return {
    schemaVersion: 5,
    muscles,
    exercises,
    templates,
    programs: [{
      id: "full_body", name: "Full body", templateCycle: ["session_a", "session_b"],
      trainingDays: [1, 3, 5], nextIndex: 0, scheduleCheckedThrough: date, active: true, archived: false,
    }],
    programEvents: [], workoutLogs: [], bodyWeights: [], nutritionEntries: [],
    nutritionTargets: { caloriesKcal: null, proteinGrams: null }, weightGoalKg: null,
  };
}

export function localDate(value = new Date()) {
  const offset = value.getTimezoneOffset() * 60_000;
  return new Date(value.getTime() - offset).toISOString().slice(0, 10);
}
