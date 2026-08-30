import test from "node:test";
import assert from "node:assert/strict";
import { createSeedState } from "../js/seed.js";
import { normalizeState } from "../js/state.js";
import { completeWorkout, isSetValid, lastPerformedExercise, missedSlotCount, nutritionTrend, saveNutrition, saveWeight, skipMissedSlots, startWorkout, weightTrend } from "../js/rules.js";
import { markdownExport, nutritionCsv, workoutCsv } from "../js/exporters.js";

test("le seed correspond au programme Android", () => {
  const state = createSeedState(new Date("2026-08-31T12:00:00"));
  assert.equal(state.schemaVersion, 4);
  assert.deepEqual(state.programs[0].templateCycle, ["session_a", "session_b"]);
  assert.equal(state.templates[0].exercises.length, 7);
  assert.equal(state.templates[1].exercises.length, 6);
  assert.equal(state.muscles.length, 18);
});

test("les anciennes sauvegardes migrent jusqu'au schéma 4", () => {
  const old = { schemaVersion: 1, muscles: [{id:"forearms",name:"Avant-bras",archived:false}], exercises: [], templates: [], programs: [], programEvents: [], workoutLogs: [] };
  const migrated = normalizeState(old);
  assert.equal(migrated.schemaVersion, 4);
  assert.ok(migrated.muscles.some(item => item.id === "forearm_flexors"));
  assert.deepEqual(migrated.bodyWeights, []);
  assert.deepEqual(migrated.nutritionEntries, []);
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

test("les exports excluent les séries non réalisées et documentent la nutrition", () => {
  let state = createSeedState();
  state = saveNutrition(state,{date:"2020-08-31",calories:500,protein:30});
  assert.equal(workoutCsv(state).trim().split("\n").length,1);
  const markdown = markdownExport(state);
  assert.match(markdown,/Suivi nutritionnel/);
  assert.match(markdown,/500 kcal/);
  assert.match(markdown,/Version du schéma.*4/);
});
