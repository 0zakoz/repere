import { localDate } from "./seed.js";
import { uid } from "./state.js";

export const ROLE_FACTOR = { PRIMARY: 1, SECONDARY: 0.5, TERTIARY: 0.25 };

export function isSetValid(set) {
  const reps = Number(set.reps);
  const weight = Number(String(set.weightKg).replace(",", "."));
  return Number.isInteger(reps) && reps > 0 && Number.isFinite(weight) && weight >= 0;
}

export function completedLogs(state) {
  return state.workoutLogs.filter(log => log.status === "COMPLETED" && log.deletedAt == null);
}

export function chronology(log) {
  const time = new Date(log.startedAt);
  const suffix = `${String(time.getHours()).padStart(2, "0")}:${String(time.getMinutes()).padStart(2, "0")}:${String(time.getSeconds()).padStart(2, "0")}`;
  const value = new Date(`${log.localDate}T${suffix}`).getTime();
  return Number.isFinite(value) ? value : log.startedAt;
}

export function lastPerformedExercise(state, exerciseId) {
  return completedLogs(state)
    .slice().sort((a, b) => chronology(b) - chronology(a))
    .map(workout => ({ workout, exercise: workout.exercises.find(item => item.exerciseId === exerciseId) }))
    .find(item => item.exercise?.sets.some(set => set.completed && isSetValid(set))) ?? null;
}

export function activeProgram(state) {
  return state.programs.find(program => program.active && !program.archived) ?? null;
}

export function missedSlotCount(state, today = localDate()) {
  const program = activeProgram(state);
  if (!program || !program.trainingDays.length) return 0;
  const checked = /^\d{4}-\d{2}-\d{2}$/.test(program.scheduleCheckedThrough) ? program.scheduleCheckedThrough : today;
  let cursor = shiftDate(checked, 1);
  const yesterday = shiftDate(today, -1);
  let count = 0;
  while (cursor <= yesterday) {
    const day = new Date(`${cursor}T12:00:00`).getDay() || 7;
    if (program.trainingDays.includes(day)) count += 1;
    cursor = shiftDate(cursor, 1);
  }
  return count;
}

export function skipMissedSlots(state, count, today = localDate()) {
  const program = activeProgram(state);
  if (!program || !program.templateCycle.length || count <= 0) return state;
  let index = program.nextIndex;
  const events = Array.from({ length: count }, () => {
    const templateId = program.templateCycle[index % program.templateCycle.length];
    index = (index + 1) % program.templateCycle.length;
    return { id: uid(), programId: program.id, templateId, date: today, outcome: "SKIPPED", workoutLogId: null };
  });
  return {
    ...state,
    programs: state.programs.map(item => item.id === program.id ? { ...item, nextIndex: index, scheduleCheckedThrough: shiftDate(today, -1) } : item),
    programEvents: [...state.programEvents, ...events],
  };
}

export function acknowledgeMissedSlots(state, today = localDate()) {
  const program = activeProgram(state);
  if (!program) return state;
  return {
    ...state,
    programs: state.programs.map(item => item.id === program.id ? { ...item, scheduleCheckedThrough: shiftDate(today, -1) } : item),
  };
}

export function suggestedTemplate(state) {
  const program = activeProgram(state);
  if (!program?.templateCycle.length) return null;
  const id = program.templateCycle[((program.nextIndex % program.templateCycle.length) + program.templateCycle.length) % program.templateCycle.length];
  return state.templates.find(template => template.id === id && !template.archived) ?? null;
}

export function startWorkout(state, templateId, { programId = null, advanceProgram = false, date = localDate() } = {}) {
  if (state.workoutLogs.some(log => log.status === "DRAFT" && log.deletedAt == null)) throw new Error("Une séance est déjà en cours");
  const template = state.templates.find(item => item.id === templateId && !item.archived);
  if (!template) throw new Error("Séance introuvable");
  const program = programId ? state.programs.find(item => item.id === programId) : null;
  const exercises = template.exercises.map(entry => {
    const source = state.exercises.find(item => item.id === entry.exerciseId);
    if (!source) return null;
    const min = entry.repMinOverride ?? source.defaultRepMin;
    const max = entry.repMaxOverride ?? source.defaultRepMax;
    const previous = lastPerformedExercise(state, source.id)?.exercise;
    return {
      id: uid(), exerciseId: source.id, nameSnapshot: source.name, instructionSnapshot: source.instruction ?? "",
      repMinSnapshot: min, repMaxSnapshot: max,
      musclesSnapshot: (source.muscles ?? []).map(item => ({
        muscleId: item.muscleId,
        name: state.muscles.find(muscle => muscle.id === item.muscleId)?.name ?? "",
        role: item.role,
      })),
      plannedSets: entry.targetSets,
      sets: Array.from({ length: entry.targetSets }, (_, index) => {
        const old = previous?.sets.find(set => set.order === index + 1 && set.completed && isSetValid(set));
        return { id: uid(), order: index + 1, weightKg: old?.weightKg ?? "", reps: old?.reps ?? "", rir: null, restBeforeSeconds: null, completed: false };
      }),
      restStartedAt: null, restTargetSetOrder: null,
    };
  }).filter(Boolean);
  const draft = {
    id: uid(), templateId: template.id, templateNameSnapshot: template.name,
    programId: program?.id ?? null, programNameSnapshot: program?.name ?? null,
    localDate: date, startedAt: Date.now(), endedAt: null, note: "", status: "DRAFT", exercises,
    advanceProgramOnFinish: advanceProgram, editingCompletedLog: false, deletedAt: null,
  };
  return { state: { ...state, workoutLogs: [...state.workoutLogs, draft] }, draft };
}

export function completeWorkout(state, workoutId, endedAt = Date.now()) {
  const workout = state.workoutLogs.find(log => log.id === workoutId && log.deletedAt == null);
  if (!workout) throw new Error("Séance introuvable");
  const exercises = workout.exercises.map(exercise => ({
    ...exercise, restStartedAt: null, restTargetSetOrder: null,
    sets: exercise.sets.map(set => set.completed && !isSetValid(set) ? { ...set, completed: false } : set),
  }));
  if (!exercises.some(exercise => exercise.sets.some(set => set.completed && isSetValid(set)))) {
    throw new Error("Valide au moins une série avant de terminer");
  }
  const completed = { ...workout, exercises, status: "COMPLETED", endedAt, editingCompletedLog: false };
  let programs = state.programs;
  let events = state.programEvents;
  if (workout.advanceProgramOnFinish && workout.programId && workout.templateId) {
    programs = programs.map(program => program.id === workout.programId && program.templateCycle.length
      ? { ...program, nextIndex: (program.nextIndex + 1) % program.templateCycle.length, scheduleCheckedThrough: workout.localDate }
      : program);
    const existing = events.find(event => event.workoutLogId === workout.id);
    if (!existing) events = [...events, { id: uid(), programId: workout.programId, templateId: workout.templateId, date: workout.localDate, outcome: "COMPLETED", workoutLogId: workout.id }];
  }
  return {
    ...state, programs, programEvents: events,
    workoutLogs: state.workoutLogs.map(log => log.id === workout.id ? completed : log),
  };
}

export function editCompletedWorkout(state, workoutId) {
  return {
    ...state,
    workoutLogs: state.workoutLogs.map(log => log.id === workoutId && log.status === "COMPLETED"
      ? { ...log, status: "DRAFT", editingCompletedLog: true, advanceProgramOnFinish: false }
      : log),
  };
}

export function abandonDraft(state, workoutId) {
  const log = state.workoutLogs.find(item => item.id === workoutId);
  return log?.editingCompletedLog
    ? { ...state, workoutLogs: state.workoutLogs.map(item => item.id === workoutId ? { ...item, status: "COMPLETED", editingCompletedLog: false } : item) }
    : { ...state, workoutLogs: state.workoutLogs.filter(item => item.id !== workoutId) };
}

export function saveWeight(state, date, input) {
  const weightKg = normalizeWeight(input);
  if (weightKg == null) throw new Error("Poids invalide");
  if (date > localDate()) throw new Error("La date ne peut pas être dans le futur");
  const now = Date.now();
  const existing = state.bodyWeights.find(item => item.date === date);
  const entry = existing ? { ...existing, weightKg, updatedAt: now } : { id: uid(), date, weightKg, createdAt: now, updatedAt: now };
  return { ...state, bodyWeights: [...state.bodyWeights.filter(item => item.date !== date), entry].sort((a, b) => a.date.localeCompare(b.date)) };
}

export function normalizeWeight(value) {
  const number = Number(String(value).trim().replace(",", "."));
  if (!Number.isFinite(number) || number <= 0 || number > 500) return null;
  return Math.round(number * 10) / 10;
}

export function weightTrend(entries, weeks = 12, today = localDate()) {
  const cutoff = weeks == null ? null : shiftDate(today, -7 * weeks);
  const unique = new Map();
  entries.forEach(entry => {
    const old = unique.get(entry.date);
    if (!old || entry.updatedAt > old.updatedAt) unique.set(entry.date, entry);
  });
  return [...unique.values()].sort((a, b) => a.date.localeCompare(b.date)).filter(entry => !cutoff || entry.date >= cutoff).map(entry => {
    const start = shiftDate(entry.date, -6);
    const window = [...unique.values()].filter(item => item.date >= start && item.date <= entry.date);
    return { ...entry, average7DaysKg: window.reduce((sum, item) => sum + item.weightKg, 0) / window.length };
  });
}

export function saveNutrition(state, { id = null, date, calories, protein }) {
  const caloriesKcal = Number(String(calories).trim());
  const rawProtein = Number(String(protein).trim().replace(",", "."));
  if (!Number.isInteger(caloriesKcal) || caloriesKcal <= 0 || caloriesKcal > 100000) throw new Error("Calories invalides");
  if (!Number.isFinite(rawProtein) || rawProtein < 0 || rawProtein > 10000) throw new Error("Protéines invalides");
  if (date > localDate()) throw new Error("La date ne peut pas être dans le futur");
  const proteinGrams = Math.round(rawProtein * 10) / 10;
  const now = Date.now();
  const existing = id ? state.nutritionEntries.find(item => item.id === id) : null;
  if (id && !existing) throw new Error("Apport introuvable");
  const entry = existing
    ? { ...existing, date, caloriesKcal, proteinGrams, updatedAt: now }
    : { id: uid(), date, caloriesKcal, proteinGrams, createdAt: now, updatedAt: now };
  return { ...state, nutritionEntries: [...state.nutritionEntries.filter(item => item.id !== entry.id), entry].sort((a, b) => a.date.localeCompare(b.date) || a.createdAt - b.createdAt) };
}

export function nutritionTrend(entries, weeks = 12, today = localDate()) {
  const cutoff = weeks == null ? null : shiftDate(today, -7 * weeks);
  const days = new Map();
  entries.filter(item => item.date <= today && (!cutoff || item.date >= cutoff)).forEach(item => {
    const day = days.get(item.date) ?? { date: item.date, caloriesKcal: 0, proteinGrams: 0, entryCount: 0 };
    day.caloriesKcal += item.caloriesKcal;
    day.proteinGrams = Math.round((day.proteinGrams + item.proteinGrams) * 10) / 10;
    day.entryCount += 1;
    days.set(item.date, day);
  });
  return [...days.values()].sort((a, b) => a.date.localeCompare(b.date));
}

export function exerciseHistory(state, exerciseId, weeks = 12) {
  const cutoff = weeks == null ? null : shiftDate(localDate(), -7 * weeks);
  return completedLogs(state).filter(log => !cutoff || log.localDate >= cutoff).flatMap(log => {
    const exercise = log.exercises.find(item => item.exerciseId === exerciseId);
    return exercise ? exercise.sets.filter(set => set.completed && isSetValid(set)).map(set => ({
      date: log.localDate, timestamp: chronology(log), setOrder: set.order,
      weight: Number(String(set.weightKg).replace(",", ".")), reps: Number(set.reps), rir: set.rir,
      restSeconds: set.restBeforeSeconds, repMin: exercise.repMinSnapshot, repMax: exercise.repMaxSnapshot,
    })) : [];
  }).sort((a, b) => a.timestamp - b.timestamp || a.setOrder - b.setOrder);
}

export function sessionStats(state, templateId, weeks = 12) {
  const cutoff = weeks == null ? null : shiftDate(localDate(), -7 * weeks);
  const logs = completedLogs(state).filter(log => log.templateId === templateId && (!cutoff || log.localDate >= cutoff));
  if (!logs.length) return null;
  const completed = logs.map(log => log.exercises.reduce((sum, ex) => sum + ex.sets.filter(set => set.completed && isSetValid(set)).length, 0));
  const planned = logs.map(log => log.exercises.reduce((sum, ex) => sum + ex.plannedSets, 0));
  const durations = logs.map(log => Math.max(0, (log.endedAt ?? log.startedAt) - log.startedAt) / 1000);
  const sets = logs.flatMap(log => log.exercises.flatMap(ex => ex.sets.filter(set => set.completed && isSetValid(set))));
  const average = values => values.length ? values.reduce((a, b) => a + b, 0) / values.length : null;
  const exerciseIds = [...new Set(logs.flatMap(log => log.exercises.map(ex => ex.exerciseId)))];
  return {
    sessionCount: logs.length, averageDurationSeconds: average(durations),
    averageCompletedSets: average(completed), averagePlannedSets: average(planned),
    completionRate: planned.reduce((a, b) => a + b, 0) ? completed.reduce((a, b) => a + b, 0) / planned.reduce((a, b) => a + b, 0) : 0,
    averageRir: average(sets.map(set => set.rir).filter(value => value != null)),
    averageRest: average(sets.map(set => set.restBeforeSeconds).filter(value => value != null)),
    exercises: exerciseIds.map(id => {
      const rows = logs.map(log => log.exercises.find(ex => ex.exerciseId === id)).filter(Boolean);
      return {
        exerciseId: id, exerciseName: rows[0]?.nameSnapshot ?? id,
        averageCompletedSets: average(rows.map(ex => ex.sets.filter(set => set.completed && isSetValid(set)).length)),
        averagePlannedSets: average(rows.map(ex => ex.plannedSets)),
      };
    }),
    logs: logs.slice().sort((a, b) => chronology(b) - chronology(a)),
  };
}

export function muscleStats(state, weeks = 12) {
  const cutoff = weeks == null ? null : shiftDate(localDate(), -7 * weeks);
  const result = new Map(state.muscles.map(muscle => [muscle.id, { muscle, weightedSets: 0, reps: [], rir: [] }]));
  completedLogs(state).filter(log => !cutoff || log.localDate >= cutoff).forEach(log => log.exercises.forEach(exercise => {
    const current = state.exercises.find(item => item.id === exercise.exerciseId)?.muscles;
    const assignments = current ?? exercise.musclesSnapshot;
    exercise.sets.filter(set => set.completed && isSetValid(set)).forEach(set => assignments.forEach(item => {
      const row = result.get(item.muscleId);
      if (!row) return;
      const factor = ROLE_FACTOR[item.role] ?? 0;
      row.weightedSets += factor;
      row.reps.push(Number(set.reps));
      if (set.rir != null) row.rir.push(set.rir);
    }));
  }));
  const average = values => values.length ? values.reduce((a, b) => a + b, 0) / values.length : null;
  return [...result.values()].map(row => ({ ...row, averageReps: average(row.reps), averageRir: average(row.rir) })).sort((a, b) => b.weightedSets - a.weightedSets);
}

export function shiftDate(date, days) {
  const value = new Date(`${date}T12:00:00`);
  value.setDate(value.getDate() + days);
  return localDate(value);
}

export function formatDuration(seconds) {
  const minutes = Math.round(seconds / 60);
  const hours = Math.floor(minutes / 60);
  return hours ? `${hours} h ${String(minutes % 60).padStart(2, "0")}` : `${minutes} min`;
}
