import test from "node:test";
import assert from "node:assert/strict";
import { createSeedState } from "../js/seed.js";
import { normalizeState } from "../js/state.js";
import { adoptLegacyTargets, backupSummary, completeWorkout, hasNutritionTargets, isSetValid, lastPerformedExercise, missedSlotCount, moveWorkoutExercise, normalizeTargetCalories, normalizeTargetProtein, nutritionRemaining, nutritionTrend, saveNutrition, saveWeight, skipMissedSlots, startWorkout, weightTrend, workoutWithDuration, workoutWithSetRest } from "../js/rules.js";
import { MUSCLE_REGIONS, heatFraction, renderMuscleFigure } from "../js/muscleFigure.js";
import { markdownExport, nutritionCsv, workoutCsv } from "../js/exporters.js";

test("le seed correspond au programme Android", () => {
  const state = createSeedState(new Date("2026-08-31T12:00:00"));
  assert.equal(state.schemaVersion, 5);
  assert.deepEqual(state.programs[0].templateCycle, ["session_a", "session_b"]);
  assert.equal(state.templates[0].exercises.length, 7);
  assert.equal(state.templates[1].exercises.length, 6);
  assert.equal(state.muscles.length, 18);
});

test("les anciennes sauvegardes migrent jusqu'au schéma 5", () => {
  const old = { schemaVersion: 1, muscles: [{id:"forearms",name:"Avant-bras",archived:false}], exercises: [], templates: [], programs: [], programEvents: [], workoutLogs: [] };
  const migrated = normalizeState(old);
  assert.equal(migrated.schemaVersion, 5);
  assert.ok(migrated.muscles.some(item => item.id === "forearm_flexors"));
  assert.deepEqual(migrated.bodyWeights, []);
  assert.deepEqual(migrated.nutritionEntries, []);
  assert.deepEqual(migrated.nutritionTargets, { caloriesKcal: null, proteinGrams: null });
  assert.equal(migrated.weightGoalKg, null);
});

test("le schéma 4 gagne des objectifs nuls et conserve ses données", () => {
  const old = { schemaVersion: 4, muscles: [], exercises: [], templates: [], programs: [], programEvents: [], workoutLogs: [], bodyWeights: [{ id: "b", date: "2026-08-29", weightKg: 80.2, createdAt: 1, updatedAt: 1 }], nutritionEntries: [] };
  const migrated = normalizeState(old);
  assert.equal(migrated.schemaVersion, 5);
  assert.equal(migrated.bodyWeights[0].weightKg, 80.2);
  assert.deepEqual(migrated.nutritionTargets, { caloriesKcal: null, proteinGrams: null });
  assert.equal(migrated.weightGoalKg, null);
  const kept = normalizeState({ ...old, schemaVersion: 5, nutritionTargets: { caloriesKcal: 2200, proteinGrams: 140 }, weightGoalKg: 75.5 });
  assert.deepEqual(kept.nutritionTargets, { caloriesKcal: 2200, proteinGrams: 140 });
  assert.equal(kept.weightGoalKg, 75.5);
  const cleaned = normalizeState({ ...old, schemaVersion: 5, nutritionTargets: { caloriesKcal: 0, proteinGrams: "abc" }, weightGoalKg: "lourd" });
  assert.deepEqual(cleaned.nutritionTargets, { caloriesKcal: null, proteinGrams: null });
  assert.equal(cleaned.weightGoalKg, null);
});

test("l'adoption reprend les objectifs du stockage local une seule fois", () => {
  const base = normalizeState({ schemaVersion: 5, muscles: [], exercises: [], templates: [], programs: [], programEvents: [], workoutLogs: [], bodyWeights: [], nutritionEntries: [] });
  assert.equal(hasNutritionTargets(base.nutritionTargets), false);
  const adopted = adoptLegacyTargets(base, { caloriesKcal: 2200, proteinGrams: "140" });
  assert.equal(hasNutritionTargets(adopted.nutritionTargets), true);
  assert.deepEqual(adopted.nutritionTargets, { caloriesKcal: 2200, proteinGrams: 140 });
  assert.equal(adoptLegacyTargets(adopted, { caloriesKcal: 1800, proteinGrams: 100 }), adopted);
  assert.equal(adoptLegacyTargets(base, { caloriesKcal: null, proteinGrams: null }), base);
  assert.equal(adoptLegacyTargets(base, null), base);
});

test("le préremplissage ignore une occurrence vide plus récente", () => {
  let state = createSeedState();
  const completed = (id, date, completedSet) => ({
    id, templateId:"session_a", templateNameSnapshot:"A", localDate:date, startedAt:new Date(`${date}T18:00:00`).getTime(), endedAt:new Date(`${date}T19:00:00`).getTime(), status:"COMPLETED", deletedAt:null,
    exercises:[{ id:`ex-${id}`, exerciseId:"chest_press", nameSnapshot:"Press pecs", instructionSnapshot:"", repMinSnapshot:6, repMaxSnapshot:10, musclesSnapshot:[], plannedSets:2, restStartedAt:null, restTargetSetOrder:null,
      sets:[{id:`set-${id}`,order:1,weightKg:"80",reps:"8",rir:1,restBeforeSeconds:90,completed:completedSet}] }],
  });
  state.workoutLogs = [completed("old","2026-08-01",true), completed("empty","2026-08-08",false)];
  assert.equal(lastPerformedExercise(state,"chest_press").workout.id,"old");
  const result = startWorkout(state,"session_a");
  assert.equal(result.draft.exercises[0].sets[0].weightKg,"80");
  assert.equal(result.draft.exercises[0].sets[0].completed,false);
});

test("une séance terminée avance le programme une seule fois", () => {
  let state = createSeedState();
  ({state} = startWorkout(state,"session_a",{programId:"full_body",advanceProgram:true}));
  const draft = state.workoutLogs.at(-1);
  draft.exercises[0].sets[0] = {...draft.exercises[0].sets[0],weightKg:"75",reps:"9",completed:true};
  state = completeWorkout(state,draft.id,draft.startedAt+3_600_000);
  assert.equal(state.programs[0].nextIndex,1);
  assert.equal(state.programEvents.length,1);
  assert.ok(isSetValid(state.workoutLogs.at(-1).exercises[0].sets[0]));
});

test("les créneaux manqués avancent le cycle sans créer de performance", () => {
  let state = createSeedState(new Date("2026-08-24T12:00:00"));
  state.programs[0].scheduleCheckedThrough = "2026-08-24";
  assert.equal(missedSlotCount(state, "2026-08-31"), 2);
  state = skipMissedSlots(state, 2, "2026-08-31");
  assert.equal(state.programEvents.filter(event => event.outcome === "SKIPPED").length, 2);
  assert.equal(state.programs[0].nextIndex, 0);
  assert.equal(lastPerformedExercise(state, "chest_press"), null);
});

test("l'ordre et les durées d'une séance restent modifiables", () => {
  let state = createSeedState();
  ({state} = startWorkout(state,"session_a"));
  let draft = state.workoutLogs.at(-1);
  const firstId = draft.exercises[0].id;
  const secondId = draft.exercises[1].id;
  draft = moveWorkoutExercise(draft,secondId,-1);
  assert.deepEqual(draft.exercises.slice(0,2).map(exercise=>exercise.id),[secondId,firstId]);

  draft = workoutWithDuration(draft,3600,10_000_000);
  assert.equal(draft.startedAt,6_400_000);
  const set = draft.exercises[0].sets[0];
  draft.exercises[0] = {...draft.exercises[0],restStartedAt:9_000_000,restTargetSetOrder:set.order};
  draft = workoutWithSetRest(draft,draft.exercises[0].id,set.id,95);
  assert.equal(draft.exercises[0].sets[0].restBeforeSeconds,95);
  assert.equal(draft.exercises[0].restStartedAt,null);

  const completedEdit = {...draft,editingCompletedLog:true,endedAt:draft.startedAt+1000};
  const corrected = workoutWithDuration(completedEdit,1800,99_000_000);
  assert.equal(corrected.endedAt,corrected.startedAt+1_800_000);
});

test("poids et nutrition sont agrégés par jour", () => {
  let state = createSeedState();
  state = saveWeight(state,"2020-08-29","80,2");
  state = saveWeight(state,"2020-08-31","79.8");
  assert.equal(weightTrend(state.bodyWeights,null,"2020-08-31").at(-1).average7DaysKg,80);
  state = saveNutrition(state,{date:"2020-08-31",calories:"650",protein:"42,5"});
  state = saveNutrition(state,{date:"2020-08-31",calories:"400",protein:"18"});
  const total = nutritionTrend(state.nutritionEntries,null,"2020-08-31")[0];
  assert.deepEqual([total.caloriesKcal,total.proteinGrams,total.entryCount],[1050,60.5,2]);
  assert.match(nutritionCsv(state),/650,42.5,1050,60.5/);
});

test("les objectifs nutritionnels comparent la journée aux cibles", () => {
  assert.equal(normalizeTargetCalories(""), null);
  assert.equal(normalizeTargetCalories("2200"), 2200);
  assert.throws(() => normalizeTargetCalories("12,5"), /Objectif calories invalide/);
  assert.throws(() => normalizeTargetCalories("0"), /Objectif calories invalide/);
  assert.equal(normalizeTargetProtein(""), null);
  assert.equal(normalizeTargetProtein("42,5"), 42.5);
  assert.throws(() => normalizeTargetProtein("abc"), /Objectif protéines invalide/);
  let state = createSeedState();
  state = saveNutrition(state, { date: "2020-08-31", calories: "650", protein: "42,5" });
  state = saveNutrition(state, { date: "2020-08-31", calories: "400", protein: "18" });
  const full = nutritionRemaining(state.nutritionEntries, "2020-08-31", { caloriesKcal: 2200, proteinGrams: 140 });
  assert.deepEqual([full.caloriesIn, full.proteinIn, full.caloriesLeft, full.proteinLeft], [1050, 60.5, 1150, 79.5]);
  const exceeded = nutritionRemaining(state.nutritionEntries, "2020-08-31", { caloriesKcal: 500, proteinGrams: 30 });
  assert.deepEqual([exceeded.caloriesLeft, exceeded.proteinLeft], [-550, -30.5]);
  const none = nutritionRemaining(state.nutritionEntries, "2020-08-31", { caloriesKcal: null, proteinGrams: null });
  assert.deepEqual([none.caloriesLeft, none.proteinLeft], [null, null]);
});

test("le résumé de sauvegarde compte séances, pesées et apports", () => {
  let state = createSeedState();
  assert.deepEqual(backupSummary(state), { workouts: 0, weights: 0, nutrition: 0 });
  state = saveWeight(state, "2020-08-29", "80,2");
  state = saveNutrition(state, { date: "2020-08-31", calories: "650", protein: "42,5" });
  const log = { id: "w1", templateId: "session_a", templateNameSnapshot: "A", localDate: "2020-08-30", startedAt: 1, endedAt: 2, status: "COMPLETED", deletedAt: null, exercises: [] };
  state = { ...state, workoutLogs: [log, { ...log, id: "w2", status: "DRAFT" }, { ...log, id: "w3", deletedAt: 5 }] };
  assert.deepEqual(backupSummary(state), { workouts: 1, weights: 1, nutrition: 1 });
});

test("les exports excluent les séries non réalisées et documentent la nutrition", () => {
  let state = createSeedState();
  state = saveNutrition(state,{date:"2020-08-31",calories:500,protein:30});
  assert.equal(workoutCsv(state).trim().split("\n").length,1);
  const markdown = markdownExport(state);
  assert.match(markdown,/Suivi nutritionnel/);
  assert.match(markdown,/500 kcal/);
  assert.match(markdown,/Version du schéma.*5/);
  const withGoals = markdownExport({ ...state, nutritionTargets: { caloriesKcal: 2200, proteinGrams: 140 }, weightGoalKg: 75.5 });
  assert.match(withGoals,/Objectifs nutritionnels.*2200 kcal \/ 140\.0 g/);
  assert.match(withGoals,/Poids objectif.*75\.5 kg/);
  const weighed = saveWeight({ ...state, nutritionTargets: { caloriesKcal: 2200, proteinGrams: 140 }, weightGoalKg: 75.5 }, "2020-08-31", "80,2");
  const weighedMd = markdownExport(weighed);
  assert.match(weighedMd,/Reste kcal/);
  assert.match(weighedMd,/\+1700 kcal/);
  assert.match(weighedMd,/\+110\.0 g/);
  assert.match(weighedMd,/Écart objectif/);
  assert.match(weighedMd,/\+4\.7 kg/);
});

test("chaque muscle a exactement une région sur la carte", () => {
  const ids = [...MUSCLE_REGIONS.front, ...MUSCLE_REGIONS.back].map(region => region.id);
  assert.equal(ids.length, 18);
  assert.deepEqual(new Set(ids), new Set([
    "pecs", "upper_pecs", "lats", "traps", "lower_back",
    "front_delts", "side_delts", "rear_delts", "biceps", "triceps",
    "forearm_flexors", "forearm_extensors", "quads", "hamstrings",
    "glutes", "adductors", "calves", "abs",
  ]));
  for (const region of [...MUSCLE_REGIONS.front, ...MUSCLE_REGIONS.back]) {
    assert.match(region.d, /^M/);
    assert.match(region.d, /Z$/);
  }
});

test("la fraction heatmap suit la parité Android", () => {
  assert.equal(heatFraction(0, 10), 0);
  assert.equal(heatFraction(5, 0), 0);
  assert.equal(heatFraction(2.5, 10), 0.25);
  assert.equal(heatFraction(15, 10), 1);
});

test("la carte affiche avant et dos côte à côte avec détail", () => {
  const muscles = [{ id: "quads", name: "Quadriceps" }, { id: "abs", name: "Abdominaux" }];
  const rows = [
    { muscle: muscles[0], weightedSets: 4, averageReps: 8, averageRir: 1 },
    { muscle: muscles[1], weightedSets: 0, averageReps: null, averageRir: null },
  ];
  const html = renderMuscleFigure({ muscles, rows, selectedId: "quads" });
  assert.equal(html.split('class="muscle-figure"').length - 1, 2);
  assert.equal(html.split('data-id="quads"').length - 1, 2);
  assert.ok(html.includes("Avant") && html.includes("Dos"));
  assert.ok(html.includes("Quadriceps — 4,0 séries pond."));
  const zero = renderMuscleFigure({ muscles, rows, selectedId: "abs" });
  assert.ok(zero.includes("aucune série pondérée"));
  const none = renderMuscleFigure({ muscles, rows, selectedId: null });
  assert.ok(!none.includes("séries pond.") && !none.includes("aucune série"));
});
