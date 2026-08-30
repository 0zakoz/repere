const LISTS = ["muscles", "exercises", "templates", "programs", "programEvents", "workoutLogs", "bodyWeights", "nutritionEntries"];

export function normalizeState(input) {
  if (!input || typeof input !== "object") throw new Error("Sauvegarde illisible");
  const version = Number(input.schemaVersion ?? 1);
  if (!Number.isInteger(version) || version < 1 || version > 4) throw new Error("Version de sauvegarde incompatible");
  let state = structuredClone(input);
  for (const key of LISTS) if (!Array.isArray(state[key])) state[key] = [];
  if (version < 2) state = migrateForearms(state);
  if (version < 3) state = { ...state, schemaVersion: 3, bodyWeights: state.bodyWeights ?? [] };
  if (version < 4) state = { ...state, schemaVersion: 4, nutritionEntries: state.nutritionEntries ?? [] };
  state.schemaVersion = 4;
  return state;
}

function migrateForearms(state) {
  const legacy = state.muscles.find(item => item.id === "forearms");
  const muscles = state.muscles.filter(item => !["forearms", "forearm_flexors", "forearm_extensors"].includes(item.id));
  muscles.push(
    { id: "forearm_flexors", name: "Fléchisseurs de l’avant-bras", archived: legacy?.archived ?? false },
    { id: "forearm_extensors", name: "Extenseurs de l’avant-bras", archived: legacy?.archived ?? false },
  );
  const assignments = (exerciseId, source = []) => {
    const role = source.find(item => item.muscleId === "forearms")?.role;
    if (!role) return source;
    const result = source.filter(item => item.muscleId !== "forearms");
    const add = (muscleId, nextRole) => { if (!result.some(item => item.muscleId === muscleId)) result.push({ muscleId, role: nextRole }); };
    if (exerciseId === "wrist_flexion") add("forearm_flexors", role);
    else if (exerciseId === "forearm_extension") { add("forearm_extensors", role); add("forearm_flexors", "TERTIARY"); }
    else if (exerciseId === "biceps_curl") { add("forearm_flexors", role); add("forearm_extensors", "TERTIARY"); }
    else if (["lat_pulldown", "horizontal_row"].includes(exerciseId)) { add("forearm_flexors", "TERTIARY"); add("forearm_extensors", "TERTIARY"); }
    else { add("forearm_flexors", role); add("forearm_extensors", role); }
    return result;
  };
  return {
    ...state, schemaVersion: 2, muscles,
    exercises: state.exercises.map(item => ({ ...item, muscles: assignments(item.id, item.muscles) })),
    workoutLogs: state.workoutLogs.map(log => ({
      ...log,
      exercises: log.exercises.map(item => ({
        ...item,
        musclesSnapshot: assignments(item.exerciseId, item.musclesSnapshot).map(a => ({
          ...a, name: muscles.find(m => m.id === a.muscleId)?.name ?? a.name ?? "",
        })),
      })),
    })),
  };
}

export function uid() {
  return globalThis.crypto?.randomUUID?.() ?? `${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

export function deepClone(value) { return structuredClone(value); }
