import { createSeedState, localDate } from "./seed.js";
import { loadAppearance, loadState, requestPersistentStorage, saveAppearance, saveState } from "./store.js";
import { uid } from "./state.js";
import {
  abandonDraft, activeProgram, chronology, completeWorkout, editCompletedWorkout, exerciseHistory,
  acknowledgeMissedSlots, formatDuration, isSetValid, lastPerformedExercise, missedSlotCount, muscleStats,
  nutritionTrend, saveNutrition, saveWeight, sessionStats, shiftDate, skipMissedSlots, startWorkout,
  suggestedTemplate, weightTrend, moveWorkoutExercise, workoutWithDuration, workoutWithSetRest,
} from "./rules.js";
import { horizontalBars, lineChart } from "./charts.js";
import { markdownExport, nutritionCsv, weightCsv, workoutCsv } from "./exporters.js";
import { button, dialog, downloadFile, emptyState, formatDate, formatTime, html, icon, number, toast } from "./ui.js";

const root = document.querySelector("#app");
let state;
let appearance = loadAppearance();
let tab = "journal";
let libraryTab = "programs";
let trendTab = "exercises";
let range = 12;
let modal = null;
let selectedWeightDate = localDate();
let selectedNutritionDate = localDate();
let editingNutritionId = null;
let timerTick = null;
let saveQueue = Promise.resolve();

boot().catch(error => {
  root.innerHTML = emptyState("⚠️", "Impossible d’ouvrir Repère", error.message);
});

async function boot() {
  state = await loadState();
  applyAppearance();
  matchMedia("(prefers-color-scheme: dark)").addEventListener("change", () => {
    if (appearance.mode === "system") { applyAppearance(); render(); }
  });
  await requestPersistentStorage().catch(() => false);
  if ("serviceWorker" in navigator) navigator.serviceWorker.register("./sw.js").catch(() => {});
  window.addEventListener("online", render);
  window.addEventListener("offline", render);
  document.addEventListener("click", onClick);
  document.addEventListener("input", onInput);
  document.addEventListener("change", onChange);
  document.addEventListener("submit", onSubmit);
  window.addEventListener("popstate", () => {
    const draft = activeDraft();
    if (modal) { modal = null; render(); }
    else if (draft) { modal = { type: "leave-workout" }; render(); history.pushState({}, ""); }
  });
  history.replaceState({}, "");
  render();
}

function applyAppearance() {
  const dark = appearance.mode === "system" ? matchMedia("(prefers-color-scheme: dark)").matches : appearance.mode === "dark";
  document.documentElement.dataset.theme = appearance.theme;
  document.documentElement.dataset.mode = dark ? "dark" : "light";
  document.querySelector('meta[name="theme-color"]')?.setAttribute("content", getComputedStyle(document.documentElement).getPropertyValue("--bg").trim());
}

function persist(next = state) {
  state = next;
  const snapshot = next;
  saveQueue = saveQueue.then(() => saveState(snapshot)).catch(error => toast(error.message, "error"));
  render();
}

function activeDraft() { return state.workoutLogs.find(log => log.status === "DRAFT" && log.deletedAt == null) ?? null; }

function render() {
  clearInterval(timerTick);
  const draft = activeDraft();
  root.innerHTML = `${!navigator.onLine ? '<div class="offline-banner">Mode hors ligne</div>' : ""}<main class="app-shell">${draft ? renderWorkout(draft) : renderScreen()}</main>${draft ? "" : renderNav()}${renderModal()}`;
  if (draft) timerTick = setInterval(updateTimer, 1000);
}

function renderScreen() {
  if (tab === "weight") return renderWeight();
  if (tab === "nutrition") return renderNutrition();
  if (tab === "trends") return renderTrends();
  if (tab === "library") return renderLibrary();
  return renderJournal();
}

function renderNav() {
  const items = [["journal","🏋️","Journal"],["weight","⚖️","Poids"],["nutrition","🍓","Nutrition"],["trends","📈","Tendances"],["library","📚","Biblio"]];
  return `<nav class="nav" aria-label="Navigation principale">${items.map(([id, emoji, label]) => `<button data-action="tab" data-tab="${id}" class="${tab === id ? "active" : ""}"><span class="nav-icon">${appearance.theme === "kawaii" ? emoji : navIcon(id)}</span><span>${label}</span></button>`).join("")}</nav>`;
}

const navIcon = id => ({ journal:"●", weight:"◆", nutrition:"♥", trends:"↗", library:"▤" })[id];
const kawaii = emoji => appearance.theme === "kawaii" ? `<span class="mascot">${emoji}</span>` : "";

function header(title, subtitle, actions = "", emoji = "") {
  return `<header class="screen-header"><div><h1>${html(title)}${kawaii(emoji)}</h1><p>${html(subtitle)}</p></div><div class="header-actions">${actions}</div></header>`;
}

function renderJournal() {
  const program = activeProgram(state);
  const template = suggestedTemplate(state);
  const logs = state.workoutLogs.filter(log => log.status === "COMPLETED" && log.deletedAt == null).sort((a,b) => chronology(b)-chronology(a));
  const install = !matchMedia("(display-mode: standalone)").matches ? `<section class="card install-hint"><strong>Installer Repère sur l’iPhone</strong><p>Dans Safari : Partager → Sur l’écran d’accueil → Ouvrir comme app web.</p></section>` : "";
  const next = template ? `<section class="card hero"><div class="card-title"><span class="eyebrow">Prochaine séance</span>${kawaii("🐰")}</div><h2>${html(program?.name ?? "Programme")} · Séance ${html(template.name)}</h2><p>${template.exercises.length} exercices · ${template.exercises.reduce((sum, ex) => sum + ex.targetSets, 0)} séries prévues</p>${button(`Démarrer ${template.name}`, "start-suggested", { iconName:"play" })}</section>` : emptyState("🗓️", "Aucune séance suggérée", program ? "Ajoute une séance au cycle du programme actif." : "Crée ou active un programme dans la Bibliothèque.", "open-library-programs", "Ouvrir les programmes");
  const missed = missedSlotCount(state);
  const missedCard = missed ? `<section class="card warning-card"><strong>${missed} créneau${missed > 1 ? "x" : ""} manqué${missed > 1 ? "s" : ""}</strong><p>Tu peux avancer le cycle ou conserver la prochaine séance actuelle.</p><div class="actions">${button("Conserver", "ack-missed", {kind:"ghost"})}${button("Avancer le cycle", "skip-missed", {kind:"secondary"})}</div></section>` : "";
  return `<section class="screen">${header("Repère", "Ton carnet, sans friction.", `<button class="icon-btn" data-action="settings" aria-label="Réglages">${icon("settings")}</button>`, "🐰")}${install}${missedCard}${next}<div class="actions">${button("Nouvelle séance", "new-workout", {kind:"secondary",iconName:"add"})}</div><div class="section-title"><h2>Historique</h2><span>${logs.length} séance(s)</span></div>${logs.length ? `<div class="list">${logs.map((log,i) => renderLog(log,i)).join("")}</div>` : emptyState("✨","Première séance","Tes séances terminées apparaîtront ici.")}</section>`;
}

function renderLog(log, index) {
  const completed = log.exercises.reduce((sum, ex) => sum + ex.sets.filter(set => set.completed && isSetValid(set)).length, 0);
  const duration = Math.max(0, (log.endedAt ?? log.startedAt)-log.startedAt)/1000;
  return `<article class="list-item ${appearance.theme === "kawaii" ? `tint-${index%3}` : ""}"><div class="grow"><strong>${html(log.templateNameSnapshot)}</strong><small>${formatDate(log.localDate)} à ${formatTime(log.startedAt)} · ${formatDuration(duration)} · ${completed} séries${log.programNameSnapshot ? ` · ${html(log.programNameSnapshot)}` : ""}</small></div><div class="row-actions"><button class="icon-btn" data-action="edit-log" data-id="${log.id}" aria-label="Modifier">${icon("edit")}</button><button class="icon-btn" data-action="delete-log" data-id="${log.id}" aria-label="Supprimer">${icon("delete")}</button></div></article>`;
}

function renderWorkout(workout) {
  const timer = workout.exercises.find(ex => ex.restStartedAt);
  const targetSet = timer?.sets.find(set => set.order === timer.restTargetSetOrder);
  const durationEnd = workout.endedAt ?? Date.now();
  return `<section class="screen workout"><header class="workout-header"><button class="icon-btn" data-action="leave-workout">${icon("back")}</button><h1>Séance ${html(workout.templateNameSnapshot)}</h1><button class="icon-btn" data-action="finish-workout" aria-label="Terminer">${icon("check")}</button></header><div class="form-grid"><label class="field"><span>Date</span><input type="date" max="${localDate()}" value="${workout.localDate}" data-workout-field="localDate"></label><label class="field"><span>Début</span><input value="${formatTime(workout.startedAt)}" disabled></label></div><button class="duration-edit" data-action="edit-workout-duration"><span>${icon("timer")} Durée</span><strong data-workout-elapsed data-start="${workout.startedAt}" data-end="${workout.endedAt ?? ""}">${formatClock((durationEnd-workout.startedAt)/1000,true)}</strong>${icon("edit")}</button><label class="field"><span>Note</span><textarea data-workout-field="note" placeholder="Ressenti, contexte…">${html(workout.note)}</textarea></label>${workout.exercises.map((exercise,i) => renderWorkoutExercise(workout,exercise,i)).join("")}<div class="actions">${button("Ajouter un exercice","add-workout-exercise",{kind:"secondary",iconName:"add"})}${button("Terminer","finish-workout",{iconName:"check"})}</div>${timer ? `<div class="timer-float" data-start="${timer.restStartedAt}"><div class="timer-copy">${icon("timer")}<span><strong data-timer>${elapsed(timer.restStartedAt)}</strong><small>avant ${html(timer.nameSnapshot)} · S${targetSet?.order ?? "—"}</small></span></div><div class="timer-actions"><button data-action="focus-next-set" data-exercise-id="${timer.id}" data-set-id="${targetSet?.id ?? ""}">Série suivante</button><button data-action="stop-timer" data-exercise-id="${timer.id}" aria-label="Ignorer le chrono">×</button></div></div>` : ""}</section>`;
}

function renderWorkoutExercise(workout, exercise, index) {
  return `<article class="card ${index%3 ? `tint-${index%3}` : ""}" data-exercise="${exercise.id}"><div class="card-title"><div><h2>${html(exercise.nameSnapshot)}</h2><small>${exercise.repMinSnapshot}–${exercise.repMaxSnapshot} reps · ${exercise.plannedSets} séries prévues</small></div><div class="row-actions"><button class="icon-btn" data-action="move-workout-exercise" data-exercise-id="${exercise.id}" data-delta="-1" aria-label="Monter" ${index===0?"disabled":""}>↑</button><button class="icon-btn" data-action="move-workout-exercise" data-exercise-id="${exercise.id}" data-delta="1" aria-label="Descendre" ${index===workout.exercises.length-1?"disabled":""}>↓</button><button class="icon-btn" data-action="remove-workout-exercise" data-exercise-id="${exercise.id}" aria-label="Retirer">${icon("delete")}</button></div></div>${exercise.instructionSnapshot ? `<p class="subtitle">${html(exercise.instructionSnapshot)}</p>` : ""}<div class="set-labels set-row"><span></span><small>kg</small><small>reps</small><small>RIR</small><span></span></div>${exercise.sets.slice().sort((a,b)=>a.order-b.order).map(set => renderSet(exercise,set)).join("")}${button("Ajouter une série","add-set",{kind:"secondary",iconName:"add",extra:`data-exercise-id="${exercise.id}"`})}</article>`;
}

function renderSet(exercise,set) {
  return `<div class="set-row ${set.completed ? "completed" : ""}"><span class="set-number">${set.order}</span><input inputmode="decimal" aria-label="Charge série ${set.order}" value="${html(set.weightKg)}" data-set-field="weightKg" data-exercise-id="${exercise.id}" data-set-id="${set.id}"><input inputmode="numeric" aria-label="Répétitions série ${set.order}" value="${html(set.reps)}" data-set-field="reps" data-exercise-id="${exercise.id}" data-set-id="${set.id}"><select aria-label="RIR série ${set.order}" data-set-field="rir" data-exercise-id="${exercise.id}" data-set-id="${set.id}"><option value="">—</option>${[0,1,2,3].map(v=>`<option ${set.rir===v?"selected":""}>${v}</option>`).join("")}</select><button class="icon-btn" data-action="toggle-set" data-exercise-id="${exercise.id}" data-set-id="${set.id}" aria-label="${set.completed ? "Invalider" : "Valider"}">${set.completed ? "✓" : "○"}</button></div><div class="step-row compact">${button("−1 rep","adjust-rep",{kind:"ghost",extra:`data-exercise-id="${exercise.id}" data-set-id="${set.id}" data-delta="-1"`})}${button("+1 rep","adjust-rep",{kind:"ghost",extra:`data-exercise-id="${exercise.id}" data-set-id="${set.id}" data-delta="1"`})}${button("Retirer","remove-set",{kind:"danger",extra:`data-exercise-id="${exercise.id}" data-set-id="${set.id}"`})}</div><button class="rest-edit" data-action="edit-set-rest" data-exercise-id="${exercise.id}" data-set-id="${set.id}">${icon("timer")} ${set.restBeforeSeconds == null ? "Repos —" : `Repos ${formatClock(set.restBeforeSeconds)}`} ${icon("edit")}</button>`;
}

function renderWeight() {
  const entry = state.bodyWeights.find(item => item.date === selectedWeightDate);
  const previous = state.bodyWeights.filter(item => item.date < selectedWeightDate).sort((a,b)=>b.date.localeCompare(a.date))[0];
  const value = entry?.weightKg ?? previous?.weightKg ?? "";
  const trend = weightTrend(state.bodyWeights, range);
  const series = [{label:"Poids brut",points:trend.map(item=>({date:item.date,value:item.weightKg}))},{label:"Moyenne 7 j",points:trend.map(item=>({date:item.date,value:item.average7DaysKg}))}];
  return `<section class="screen">${header("Poids","Une mesure quand tu veux, sans jour obligatoire.",`<button class="icon-btn" data-action="export-weight">${icon("download")}</button>`,"🐼")}<section class="card tint-2"><label class="field"><span>Date</span><input id="weight-date" type="date" max="${localDate()}" value="${selectedWeightDate}"></label><label class="field"><span>Poids</span><input id="weight-input" inputmode="decimal" value="${value}" placeholder="0,0"><small>kg</small></label><div class="step-row">${[1,.5,.1].map(v=>button(`+${String(v).replace(".",",")}`,"weight-step",{kind:"ghost",extra:`data-delta="${v}"`})).join("")}</div><div class="step-row">${[-1,-.5,-.1].map(v=>button(`−${String(Math.abs(v)).replace(".",",")}`,"weight-step",{kind:"ghost",extra:`data-delta="${v}"`})).join("")}</div><div class="actions">${entry?button("Supprimer","delete-weight",{kind:"danger",iconName:"delete"}):""}${button(entry?"Mettre à jour":"Enregistrer","save-weight",{iconName:"check"})}</div></section><div class="section-title"><h2>Évolution</h2></div>${rangeChips()}<section class="card">${lineChart(series,{unit:"kg"})}</section>${state.bodyWeights.length?`<div class="list">${state.bodyWeights.slice().sort((a,b)=>b.date.localeCompare(a.date)).slice(0,12).map(item=>`<button class="list-item" data-action="select-weight" data-date="${item.date}"><div class="grow"><strong>${number(item.weightKg)} kg</strong><small>${formatDate(item.date)}</small></div>${icon("edit")}</button>`).join("")}</div>`:""}</section>`;
}

function renderNutrition() {
  const entries = state.nutritionEntries.filter(item=>item.date===selectedNutritionDate).sort((a,b)=>b.createdAt-a.createdAt);
  const editing = entries.find(item=>item.id===editingNutritionId);
  const totals = entries.reduce((a,b)=>({calories:a.calories+b.caloriesKcal,protein:a.protein+b.proteinGrams}),{calories:0,protein:0});
  const trend = nutritionTrend(state.nutritionEntries,range);
  return `<section class="screen">${header("Nutrition","Ajoute tes apports au fil de la journée.",`<button class="icon-btn" data-action="export-nutrition">${icon("download")}</button>`,"🍓")}<section class="card tint-1"><label class="field"><span>Date</span><input id="nutrition-date" type="date" max="${localDate()}" value="${selectedNutritionDate}"></label><div class="metrics"><div class="metric"><small>Calories</small><strong>${totals.calories} kcal</strong></div><div class="metric"><small>Protéines</small><strong>${number(totals.protein)} g</strong></div></div><small>${entries.length} apport(s) enregistré(s)</small></section><form id="nutrition-form" class="card tint-2"><h2>${editing?"Modifier l’apport":"Ajouter un apport"}${kawaii("🐰")}</h2><input type="hidden" name="id" value="${editing?.id??""}"><div class="form-grid"><label class="field"><span>Calories</span><input name="calories" inputmode="numeric" required value="${editing?.caloriesKcal??""}" placeholder="kcal"></label><label class="field"><span>Protéines</span><input name="protein" inputmode="decimal" required value="${editing?.proteinGrams??""}" placeholder="g"></label></div><div class="actions">${editing?button("Annuler","cancel-nutrition",{kind:"ghost"}):""}<button class="btn primary" type="submit">${icon(editing?"check":"add")} ${editing?"Enregistrer":"Ajouter"}</button></div></form>${entries.length?`<div class="section-title"><h2>Apports du jour</h2></div><div class="list">${entries.map(item=>`<article class="list-item"><div class="grow"><strong>${item.caloriesKcal} kcal · ${number(item.proteinGrams)} g protéines</strong><small>${formatTime(item.createdAt)}</small></div><button class="icon-btn" data-action="edit-nutrition" data-id="${item.id}">${icon("edit")}</button><button class="icon-btn" data-action="delete-nutrition" data-id="${item.id}">${icon("delete")}</button></article>`).join("")}</div>`:""}<div class="section-title"><div><h2>Historique</h2><span>Totaux quotidiens</span></div></div>${rangeChips()}<section class="card"><h3>Calories</h3>${lineChart([{label:"Calories",points:trend.map(item=>({date:item.date,value:item.caloriesKcal}))}],{unit:"kcal"})}</section><section class="card"><h3>Protéines</h3>${lineChart([{label:"Protéines",points:trend.map(item=>({date:item.date,value:item.proteinGrams}))}],{unit:"g"})}</section></section>`;
}

function rangeChips() { return `<div class="chip-row">${[4,12,52,null].map(value=>`<button class="chip ${range===value?"active":""}" data-action="range" data-range="${value??"all"}">${value?`${value} sem.`:"Tout"}</button>`).join("")}</div>`; }

function renderTrends() {
  const tabs = [["exercises","Exercices"],["sessions","Séances"],["muscles","Muscles"]];
  return `<section class="screen">${header("Tendances","Des données brutes, lisibles et comparables.","","🐱")}<div class="tabs">${tabs.map(([id,label])=>`<button class="${trendTab===id?"active":""}" data-action="trend-tab" data-tab="${id}">${label}</button>`).join("")}</div>${rangeChips()}${trendTab==="sessions"?renderSessionTrends():trendTab==="muscles"?renderMuscleTrends():renderExerciseTrends()}</section>`;
}

function renderExerciseTrends() {
  const available = state.exercises.filter(ex=>exerciseHistory(state,ex.id,range).length);
  if(!available.length)return emptyState("📈","Pas encore de tendance","Termine quelques séries pour afficher les courbes.");
  const selectedId = document.documentElement.dataset.selectedExercise && available.some(ex=>ex.id===document.documentElement.dataset.selectedExercise)?document.documentElement.dataset.selectedExercise:available[0].id;
  const exercise=state.exercises.find(ex=>ex.id===selectedId); const history=exerciseHistory(state,selectedId,range);
  const orders=[...new Set(history.map(p=>p.setOrder))];
  const series=selector=>orders.map(order=>({label:`Série ${order}`,points:history.filter(p=>p.setOrder===order).map(p=>({date:p.date,value:selector(p)}))}));
  const latest=history.slice().sort((a,b)=>b.timestamp-a.timestamp||a.setOrder-b.setOrder).slice(0,4);
  return `<label class="field"><span>Exercice</span><select id="trend-exercise">${available.map(ex=>`<option value="${ex.id}" ${ex.id===selectedId?"selected":""}>${html(ex.name)}</option>`).join("")}</select></label><section class="card"><h3>Charge</h3>${lineChart(series(p=>p.weight),{unit:"kg"})}</section><section class="card"><h3>Répétitions</h3>${lineChart(series(p=>p.reps),{unit:"reps",target:[exercise.defaultRepMin,exercise.defaultRepMax]})}</section><section class="card"><h3>Dernières séries</h3>${latest.map(p=>`<div class="list-item"><div class="grow"><strong>S${p.setOrder} · ${p.weight} kg × ${p.reps}</strong><small>${formatDate(p.date)} · RIR ${p.rir??"—"} · repos ${p.restSeconds??"—"} s</small></div></div>`).join("")}</section>`;
}

function renderSessionTrends(){const cards=state.templates.map(template=>({template,stats:sessionStats(state,template.id,range)})).filter(x=>x.stats);if(!cards.length)return emptyState("📊","Pas encore de séance","Les moyennes apparaîtront après une séance terminée.");return cards.map(({template,stats})=>`<section class="card"><h2>Séance ${html(template.name)}</h2><p class="subtitle">${stats.sessionCount} séance(s) sur la période</p><div class="metrics"><div class="metric"><small>Durée moyenne</small><strong>${formatDuration(stats.averageDurationSeconds)}</strong></div><div class="metric"><small>Réalisation</small><strong>${Math.round(stats.completionRate*100)} %</strong></div><div class="metric"><small>Séries moy.</small><strong>${number(stats.averageCompletedSets)} / ${number(stats.averagePlannedSets)}</strong></div><div class="metric"><small>RIR moyen</small><strong>${stats.averageRir==null?"—":number(stats.averageRir)}</strong></div></div>${horizontalBars(stats.exercises.map(ex=>({label:ex.exerciseName,value:ex.averageCompletedSets,meta:`${number(ex.averageCompletedSets)} / ${number(ex.averagePlannedSets)} séries`})),Math.max(...stats.exercises.map(ex=>ex.averagePlannedSets),1))}</section>`).join("");}
function renderMuscleTrends(){const stats=muscleStats(state,range).filter(row=>row.weightedSets>0);if(!stats.length)return emptyState("🫶","Pas encore de volume","Les sollicitations musculaires apparaîtront ici.");return `<section class="card"><h2>Volume musculaire pondéré</h2><p class="subtitle">Principal ×1 · secondaire ×0,5 · tertiaire ×0,25</p>${horizontalBars(stats.map(row=>({label:row.muscle.name,value:row.weightedSets,meta:`${number(row.averageReps??0)} reps moy. · RIR ${row.averageRir==null?"—":number(row.averageRir)}`})))}</section>`;}

function renderLibrary(){const tabs=[["programs","Programmes"],["templates","Séances"],["exercises","Exercices"],["muscles","Muscles"]];return `<section class="screen">${header("Bibliothèque","Tout reste modifiable.","","🎀")}<div class="tabs">${tabs.map(([id,label])=>`<button class="${libraryTab===id?"active":""}" data-action="library-tab" data-tab="${id}">${label}</button>`).join("")}</div>${renderLibraryList()}</section>`;}
function renderLibraryList(){const config={programs:{label:"programme",items:state.programs,title:x=>x.name,sub:x=>`${x.templateCycle.length} séance(s) · ${x.active?"Actif":"Inactif"}${x.archived?" · Archivé":""}`},templates:{label:"séance",items:state.templates,title:x=>x.name,sub:x=>`${x.exercises.length} exercice(s)${x.archived?" · Archivée":""}`},exercises:{label:"exercice",items:state.exercises,title:x=>x.name,sub:x=>`${x.defaultRepMin}–${x.defaultRepMax} reps · ${x.muscles.length} muscle(s)${x.archived?" · Archivé":""}`},muscles:{label:"muscle",items:state.muscles,title:x=>x.name,sub:x=>x.archived?"Archivé":"Actif"}}[libraryTab];return `${button(`Créer un ${config.label}`,"create-library",{kind:"secondary",iconName:"add",extra:`data-kind="${libraryTab}"`})}<div class="section-title"><h2>${config.items.length} élément(s)</h2></div><div class="list">${config.items.map((item,i)=>`<article class="list-item"><div class="grow"><strong>${html(config.title(item))}${item.active?" · ★":""}</strong><small>${html(config.sub(item))}</small></div><button class="icon-btn" data-action="edit-library" data-kind="${libraryTab}" data-id="${item.id}">${icon("edit")}</button><button class="icon-btn" data-action="archive-library" data-kind="${libraryTab}" data-id="${item.id}">${item.archived?"↻":"◎"}</button><button class="icon-btn" data-action="delete-library" data-kind="${libraryTab}" data-id="${item.id}">${icon("delete")}</button></article>`).join("")}</div>`;}

function renderModal() {
  if (!modal) return "";
  if (modal.type === "new-workout") return dialog({
    title: "Nouvelle séance",
    content: `<div class="list">${state.templates.filter(t => !t.archived).map(t => `<button class="list-item" data-action="start-template" data-id="${t.id}"><div class="grow"><strong>Séance ${html(t.name)}</strong><small>${t.exercises.length} exercices</small></div>${icon("play")}</button>`).join("")}</div><div class="actions dialog-inline-action">${button("Créer une séance", "create-workout-template", { kind:"secondary", iconName:"add" })}</div>`,
  });
  if (modal.type === "alternate-workout") return dialog({
    title: `Séance ${modal.templateName} à la place de ${modal.suggestedName} ?`,
    content: "<p>Choisis si cette séance doit modifier l’avancement du programme.</p>",
    actions: `${button("Hors programme", "start-alternate-outside", {kind:"ghost"})}${button("Remplace la séance prévue", "start-alternate-program")}`,
  });
  if (modal.type === "confirm") return dialog({ title:modal.title, content:`<p>${html(modal.text)}</p>`, actions:`${button("Annuler","dismiss-dialog",{kind:"ghost"})}${button(modal.label??"Confirmer","confirm-modal",{kind:modal.danger?"danger":"primary"})}` });
  if (modal.type === "settings") return settingsDialog();
  if (modal.type === "library-form") return libraryDialog(modal.kind, modal.id);
  if (modal.type === "add-exercise") return dialog({
    title: "Ajouter un exercice",
    content: `<label class="field"><span>Rechercher dans la bibliothèque</span><input data-exercise-search placeholder="Nom de l’exercice"></label><div class="list">${state.exercises.filter(ex=>!ex.archived&&!activeDraft().exercises.some(x=>x.exerciseId===ex.id)).sort((a,b)=>a.name.localeCompare(b.name,"fr",{sensitivity:"base"})).map(ex=>`<button class="list-item" data-picker-exercise="${html(ex.name.toLocaleLowerCase("fr"))}" data-action="choose-workout-exercise" data-id="${ex.id}"><div class="grow"><strong>${html(ex.name)}</strong><small>${ex.defaultRepMin}–${ex.defaultRepMax} reps</small></div>${icon("add")}</button>`).join("")}</div><div class="actions dialog-inline-action">${button("Créer un exercice", "create-workout-exercise", {kind:"secondary",iconName:"add"})}</div>`,
  });
  if (modal.type === "workout-duration") return durationDialog("Durée de la séance", modal.seconds, false);
  if (modal.type === "set-rest") return durationDialog(`Repos avant la série ${modal.setOrder}`, modal.seconds, true);
  if (modal.type === "leave-workout") return dialog({ title:"Quitter la saisie ?", content:"<p>La séance est autosauvegardée. Tu pourras la reprendre depuis le Journal.</p>", actions:`${button("Continuer","dismiss-dialog",{kind:"ghost"})}${button("Mettre en pause","minimize-workout")}${button("Abandonner","abandon-workout",{kind:"danger"})}` });
  return "";
}

function settingsDialog(){const themes=[["original","Original",["#b7f34a","#56d6e7","#b89cff"]],["kawaii","Kawaii",["#ff9fc8","#ffd878","#8dc8fa"]],["pastel","Pastel",["#ff9fc5","#90caef","#f5d166"]],["oled","OLED",["#65e6ff","#c0a6ff","#ff82c2"]],["clean","Épuré",["#f3dd8e","#b9dbf5","#d9c9e8"]]];return dialog({title:"Réglages",wide:true,content:`<h3>Apparence</h3><div class="theme-grid">${themes.map(([id,label,colors])=>`<button class="theme-card ${appearance.theme===id?"active":""}" data-action="theme" data-theme="${id}"><strong>${label}</strong><div class="swatches">${colors.map(c=>`<i style="background:${c}"></i>`).join("")}</div></button>`).join("")}</div><div class="chip-row" style="margin-top:12px">${["light","dark","system"].map(mode=>`<button class="chip ${appearance.mode===mode?"active":""}" data-action="mode" data-mode="${mode}">${{light:"Clair",dark:"Sombre",system:"Système"}[mode]}</button>`).join("")}</div><h3>Données et fichiers</h3><div class="list">${settingsAction("export-workout","Performances CSV","Une ligne par série validée","📊")}${settingsAction("export-weight","Pesées CSV","Mesures et moyenne 7 jours","⚖️")}${settingsAction("export-nutrition","Nutrition CSV","Apports et totaux quotidiens","🍓")}${settingsAction("export-markdown","Tout pour ChatGPT","Document Markdown complet","🤖")}${settingsAction("backup","Sauvegarde complète","JSON compatible Android et Web","💾")}${settingsAction("restore","Restaurer un fichier","Remplace les données après validation","↻")}</div><input class="sr-only" id="restore-file" type="file" accept="application/json,.json">`});}
const settingsAction=(action,title,sub,emoji)=>`<button class="list-item" data-action="${action}"><span style="font-size:1.4rem">${emoji}</span><div class="grow"><strong>${title}</strong><small>${sub}</small></div>${icon("download")}</button>`;

function legacyLibraryDialog(kind,id){const collection=state[kind];const item=collection.find(x=>x.id===id);const title=`${item?"Modifier":"Créer"} ${{programs:"un programme",templates:"une séance",exercises:"un exercice",muscles:"un muscle"}[kind]}`;let content="";if(kind==="muscles")content=`${baseFields(item)}<label class="check-row"><input type="checkbox" name="archived" ${item?.archived?"checked":""}> Archivé</label>`;if(kind==="exercises")content=`${baseFields(item)}<div class="form-grid"><label class="field"><span>Reps min.</span><input name="defaultRepMin" type="number" min="1" required value="${item?.defaultRepMin??6}"></label><label class="field"><span>Reps max.</span><input name="defaultRepMax" type="number" min="1" required value="${item?.defaultRepMax??12}"></label></div><label class="field"><span>Consigne</span><textarea name="instruction">${html(item?.instruction??"")}</textarea></label><h3>Muscles sollicités</h3><div class="list">${state.muscles.filter(m=>!m.archived).map(m=>{const role=item?.muscles.find(a=>a.muscleId===m.id)?.role??"";return `<label class="field"><span>${html(m.name)}</span><select name="muscle:${m.id}"><option value="">Non sollicité</option><option value="PRIMARY" ${role==="PRIMARY"?"selected":""}>Principal ×1</option><option value="SECONDARY" ${role==="SECONDARY"?"selected":""}>Secondaire ×0,5</option><option value="TERTIARY" ${role==="TERTIARY"?"selected":""}>Tertiaire ×0,25</option></select></label>`}).join("")}</div>`;if(kind==="templates")content=`${baseFields(item)}<h3>Exercices et séries</h3><div class="list">${state.exercises.filter(ex=>!ex.archived).map(ex=>{const current=item?.exercises.find(x=>x.exerciseId===ex.id);return `<div class="check-row"><input type="checkbox" name="exercise:${ex.id}" ${current?"checked":""}><span class="grow">${html(ex.name)}</span><input aria-label="Séries" style="width:64px" name="sets:${ex.id}" type="number" min="1" max="20" value="${current?.targetSets??2}"></div>`}).join("")}</div>`;if(kind==="programs")content=`${baseFields(item)}<h3>Cycle de séances</h3><div class="checkbox-grid">${state.templates.filter(t=>!t.archived).map(t=>`<label class="check-row"><input type="checkbox" name="template:${t.id}" ${item?.templateCycle.includes(t.id)?"checked":""}> ${html(t.name)}</label>`).join("")}</div><h3>Jours indicatifs</h3><div class="checkbox-grid">${["Lun","Mar","Mer","Jeu","Ven","Sam","Dim"].map((d,i)=>`<label class="check-row"><input type="checkbox" name="day:${i+1}" ${item?.trainingDays.includes(i+1)?"checked":""}> ${d}</label>`).join("")}</div><label class="check-row"><input type="checkbox" name="active" ${item?.active?"checked":""}> Programme actif</label>`;return dialog({title,wide:true,content:`<form id="library-form" data-kind="${kind}" data-id="${id??""}" data-return-workout="${modal?.returnToWorkout===true}">${content}<div class="actions" style="margin-top:16px"><button class="btn primary" type="submit">${icon("check")} Enregistrer</button></div></form>`});}
const baseFields=item=>`<label class="field"><span>Nom</span><input name="name" required value="${html(item?.name??"")}"></label>`;

function libraryDialog(kind, id) {
  const item = state[kind].find(value => value.id === id);
  const title = `${item ? "Modifier" : "Créer"} ${{programs:"un programme",templates:"une séance",exercises:"un exercice",muscles:"un muscle"}[kind]}`;
  let content = baseFields(item);
  if (kind === "muscles") content += `<label class="check-row"><input type="checkbox" name="archived" ${item?.archived?"checked":""}> Archivé</label>`;
  if (kind === "exercises") content += `<div class="form-grid"><label class="field"><span>Reps min.</span><input name="defaultRepMin" type="number" min="1" required value="${item?.defaultRepMin??6}"></label><label class="field"><span>Reps max.</span><input name="defaultRepMax" type="number" min="1" required value="${item?.defaultRepMax??12}"></label></div><label class="field"><span>Consigne</span><textarea name="instruction">${html(item?.instruction??"")}</textarea></label><h3>Muscles sollicités</h3><div class="list">${state.muscles.filter(m=>!m.archived).map(m=>{const role=item?.muscles.find(a=>a.muscleId===m.id)?.role??"";return `<label class="field"><span>${html(m.name)}</span><select name="muscle:${m.id}"><option value="">Non sollicité</option><option value="PRIMARY" ${role==="PRIMARY"?"selected":""}>Principal ×1</option><option value="SECONDARY" ${role==="SECONDARY"?"selected":""}>Secondaire ×0,5</option><option value="TERTIARY" ${role==="TERTIARY"?"selected":""}>Tertiaire ×0,25</option></select></label>`}).join("")}</div>`;
  if (kind === "templates") {
    const selected = item?.exercises.map(entry=>entry.exerciseId) ?? [];
    const ordered = [...selected.map(id=>state.exercises.find(ex=>ex.id===id)).filter(Boolean), ...state.exercises.filter(ex=>!ex.archived&&!selected.includes(ex.id))];
    content += `<h3>Exercices et séries</h3><p class="subtitle">Les flèches règlent l’ordre ; les plages vides utilisent celles de l’exercice.</p><div class="list sortable-list">${ordered.map(ex=>{const current=item?.exercises.find(x=>x.exerciseId===ex.id);return `<div class="ordered-form-row" data-order-item data-item-id="${ex.id}"><div class="check-row"><input type="checkbox" name="exercise:${ex.id}" ${current?"checked":""}><span class="grow">${html(ex.name)}</span><button type="button" class="mini-btn" data-action="move-form-item" data-direction="up" aria-label="Monter">↑</button><button type="button" class="mini-btn" data-action="move-form-item" data-direction="down" aria-label="Descendre">↓</button></div><div class="form-grid triple"><label class="field"><span>Séries</span><input name="sets:${ex.id}" type="number" min="1" max="20" value="${current?.targetSets??2}"></label><label class="field"><span>Reps min.</span><input name="min:${ex.id}" type="number" min="1" placeholder="${ex.defaultRepMin}" value="${current?.repMinOverride??""}"></label><label class="field"><span>Reps max.</span><input name="max:${ex.id}" type="number" min="1" placeholder="${ex.defaultRepMax}" value="${current?.repMaxOverride??""}"></label></div></div>`}).join("")}</div>`;
  }
  if (kind === "programs") {
    const selected = item?.templateCycle ?? [];
    const ordered = [...selected.map(id=>state.templates.find(t=>t.id===id)).filter(Boolean), ...state.templates.filter(t=>!t.archived&&!selected.includes(t.id))];
    content += `<h3>Cycle de séances</h3><div class="list sortable-list">${ordered.map(template=>`<div class="check-row" data-order-item data-item-id="${template.id}"><input type="checkbox" name="template:${template.id}" ${selected.includes(template.id)?"checked":""}><span class="grow">${html(template.name)}</span><button type="button" class="mini-btn" data-action="move-form-item" data-direction="up" aria-label="Monter">↑</button><button type="button" class="mini-btn" data-action="move-form-item" data-direction="down" aria-label="Descendre">↓</button></div>`).join("")}</div><h3>Jours indicatifs</h3><div class="checkbox-grid">${["Lun","Mar","Mer","Jeu","Ven","Sam","Dim"].map((d,i)=>`<label class="check-row"><input type="checkbox" name="day:${i+1}" ${item?.trainingDays.includes(i+1)?"checked":""}> ${d}</label>`).join("")}</div><label class="check-row"><input type="checkbox" name="active" ${item?.active?"checked":""}> Programme actif</label>`;
  }
  return dialog({title,wide:true,content:`<form id="library-form" data-kind="${kind}" data-id="${id??""}" data-return-workout="${modal?.returnToWorkout===true}">${content}<div class="actions form-submit"><button class="btn primary" type="submit">${icon("check")} Enregistrer</button></div></form>`});
}

async function onClick(event){const target=event.target.closest("[data-action]");if(!target)return;const action=target.dataset.action;if(action==="dismiss-dialog"&&event.target.closest("[data-dialog]")&&!event.target.closest("button"))return;try{
  if(action==="tab"){tab=target.dataset.tab;render();scrollTo(0,0)}
  else if(action==="settings"){modal={type:"settings"};render()}
  else if(action==="dismiss-dialog"){modal=null;render()}
  else if(action==="open-library-programs"){tab="library";libraryTab="programs";render()}
  else if(action==="new-workout"){modal={type:"new-workout"};render()}
  else if(action==="start-suggested"){const p=activeProgram(state),t=suggestedTemplate(state);persist(startWorkout(state,t.id,{programId:p.id,advanceProgram:true}).state)}
  else if(action==="start-template"){const p=activeProgram(state),expected=suggestedTemplate(state),selected=state.templates.find(t=>t.id===target.dataset.id);if(!expected||expected.id===selected.id){modal=null;persist(startWorkout(state,selected.id,{programId:p?.id??null,advanceProgram:Boolean(p&&expected)}).state)}else{modal={type:"alternate-workout",templateId:selected.id,templateName:selected.name,suggestedName:expected.name};render()}}
  else if(action==="start-alternate-program"){const p=activeProgram(state),id=modal.templateId;modal=null;persist(startWorkout(state,id,{programId:p.id,advanceProgram:true}).state)}
  else if(action==="start-alternate-outside"){const id=modal.templateId;modal=null;persist(startWorkout(state,id).state)}
  else if(action==="create-workout-template"){modal={type:"library-form",kind:"templates",id:null};render()}
  else if(action==="ack-missed")persist(acknowledgeMissedSlots(state))
  else if(action==="skip-missed")persist(skipMissedSlots(state,missedSlotCount(state)))
  else if(action==="edit-log"){persist(editCompletedWorkout(state,target.dataset.id))}
  else if(action==="delete-log")confirmAction("Supprimer cette séance ?","Elle disparaîtra de l’historique et des tendances.",()=>{const id=target.dataset.id;persist({...state,workoutLogs:state.workoutLogs.map(log=>log.id===id?{...log,deletedAt:Date.now()}:log)});toast("Séance supprimée","success",{label:"Annuler",run:()=>persist({...state,workoutLogs:state.workoutLogs.map(log=>log.id===id?{...log,deletedAt:null}:log)})})})
  else if(action==="leave-workout"){modal={type:"leave-workout"};render()}
  else if(action==="minimize-workout"){modal=null;tab="journal";render()}
  else if(action==="abandon-workout"){modal=null;persist(abandonDraft(state,activeDraft().id))}
  else if(action==="finish-workout"){persist(completeWorkout(state,activeDraft().id));toast("Séance terminée")}
  else if(action==="toggle-set")toggleSet(target.dataset.exerciseId,target.dataset.setId)
  else if(action==="adjust-rep")adjustRep(target.dataset.exerciseId,target.dataset.setId,Number(target.dataset.delta))
  else if(action==="remove-set")removeSet(target.dataset.exerciseId,target.dataset.setId)
  else if(action==="add-set")addSet(target.dataset.exerciseId)
  else if(action==="stop-timer")stopTimer(target.dataset.exerciseId)
  else if(action==="focus-next-set")focusNextSet(target.dataset.exerciseId,target.dataset.setId)
  else if(action==="remove-workout-exercise")confirmAction("Retirer cet exercice ?","Ses séries saisies seront retirées du brouillon.",()=>mutateDraft(d=>({...d,exercises:d.exercises.filter(ex=>ex.id!==target.dataset.exerciseId)})))
  else if(action==="move-workout-exercise")mutateDraft(d=>moveWorkoutExercise(d,target.dataset.exerciseId,Number(target.dataset.delta)))
  else if(action==="add-workout-exercise"){modal={type:"add-exercise"};render()}
  else if(action==="choose-workout-exercise"){modal=null;addWorkoutExercise(target.dataset.id)}
  else if(action==="create-workout-exercise"){modal={type:"library-form",kind:"exercises",id:null,returnToWorkout:true};render()}
  else if(action==="edit-workout-duration"){const draft=activeDraft(),end=draft.endedAt??Date.now();modal={type:"workout-duration",seconds:Math.max(0,Math.floor((end-draft.startedAt)/1000))};render()}
  else if(action==="edit-set-rest"){const exercise=activeDraft().exercises.find(ex=>ex.id===target.dataset.exerciseId),set=exercise?.sets.find(item=>item.id===target.dataset.setId);modal={type:"set-rest",exerciseId:target.dataset.exerciseId,setId:target.dataset.setId,setOrder:set?.order??"—",seconds:set?.restBeforeSeconds??0};render()}
  else if(action==="save-duration"){const seconds=durationFromDialog();const current=modal;if(current.type==="workout-duration")mutateDraft(d=>workoutWithDuration(d,seconds));else mutateDraft(d=>workoutWithSetRest(d,current.exerciseId,current.setId,seconds));modal=null;render()}
  else if(action==="clear-duration"){const current=modal;mutateDraft(d=>workoutWithSetRest(d,current.exerciseId,current.setId,null));modal=null;render()}
  else if(action==="range"){range=target.dataset.range==="all"?null:Number(target.dataset.range);render()}
  else if(action==="weight-step")stepWeight(Number(target.dataset.delta))
  else if(action==="save-weight")saveWeightFromForm()
  else if(action==="delete-weight")confirmAction("Supprimer cette mesure ?",formatDate(selectedWeightDate),()=>persist({...state,bodyWeights:state.bodyWeights.filter(x=>x.date!==selectedWeightDate)}))
  else if(action==="select-weight"){selectedWeightDate=target.dataset.date;render();scrollTo(0,0)}
  else if(action==="cancel-nutrition"){editingNutritionId=null;render()}
  else if(action==="edit-nutrition"){editingNutritionId=target.dataset.id;render()}
  else if(action==="delete-nutrition")confirmAction("Supprimer cet apport ?","Le total quotidien sera recalculé.",()=>persist({...state,nutritionEntries:state.nutritionEntries.filter(x=>x.id!==target.dataset.id)}))
  else if(action==="trend-tab"){trendTab=target.dataset.tab;render()}
  else if(action==="library-tab"){libraryTab=target.dataset.tab;render()}
  else if(action==="create-library"){modal={type:"library-form",kind:target.dataset.kind,id:null};render()}
  else if(action==="edit-library"){modal={type:"library-form",kind:target.dataset.kind,id:target.dataset.id};render()}
  else if(action==="move-form-item"){const row=target.closest("[data-order-item]"),sibling=target.dataset.direction==="up"?row.previousElementSibling:row.nextElementSibling;if(sibling)row.parentElement.insertBefore(target.dataset.direction==="up"?row:sibling,target.dataset.direction==="up"?sibling:row)}
  else if(action==="archive-library")archiveLibrary(target.dataset.kind,target.dataset.id)
  else if(action==="delete-library")deleteLibrary(target.dataset.kind,target.dataset.id)
  else if(action==="theme"){appearance.theme=target.dataset.theme;saveAppearance(appearance);applyAppearance();render()}
  else if(action==="mode"){appearance.mode=target.dataset.mode;saveAppearance(appearance);applyAppearance();render()}
  else if(action==="export-workout")downloadFile(`repere-performances-${localDate()}.csv`,workoutCsv(state),"text/csv;charset=utf-8")
  else if(action==="export-weight")downloadFile(`repere-poids-${localDate()}.csv`,weightCsv(state),"text/csv;charset=utf-8")
  else if(action==="export-nutrition")downloadFile(`repere-nutrition-${localDate()}.csv`,nutritionCsv(state),"text/csv;charset=utf-8")
  else if(action==="export-markdown")downloadFile(`repere-complet-${localDate()}.md`,markdownExport(state),"text/markdown;charset=utf-8")
  else if(action==="backup")downloadFile(`repere-backup-${localDate()}.json`,JSON.stringify(state,null,2),"application/json")
  else if(action==="restore")document.querySelector("#restore-file").click()
  else if(action==="confirm-modal"){const run=modal.run;modal=null;run()}
}catch(error){toast(error.message,"error")}}

function updateSetInput(input){mutateSet(input.dataset.exerciseId,input.dataset.setId,set=>({...set,[input.dataset.setField]:input.dataset.setField==="rir"?(input.value===""?null:Number(input.value)):input.value,completed:false}),false)}
function onInput(event){const input=event.target;if(input.dataset.workoutField)mutateDraft(d=>({...d,[input.dataset.workoutField]:input.value}),false);if(input.dataset.setField&&input.tagName!=="SELECT")updateSetInput(input);if(input.dataset.exerciseSearch!==undefined){const query=input.value.trim().toLocaleLowerCase("fr");document.querySelectorAll("[data-picker-exercise]").forEach(row=>row.hidden=!row.dataset.pickerExercise.includes(query))}}
async function onChange(event){if(event.target.dataset.setField&&event.target.tagName==="SELECT")updateSetInput(event.target);if(event.target.id==="weight-date"){selectedWeightDate=event.target.value;render()}if(event.target.id==="nutrition-date"){selectedNutritionDate=event.target.value;editingNutritionId=null;render()}if(event.target.id==="trend-exercise"){document.documentElement.dataset.selectedExercise=event.target.value;render()}if(event.target.id==="restore-file"&&event.target.files[0]){try{const parsed=JSON.parse(await event.target.files[0].text());const {normalizeState}=await import("./state.js");const next=normalizeState(parsed);confirmAction("Restaurer cette sauvegarde ?","Toutes les données locales seront remplacées.",()=>{modal=null;persist(next);toast("Sauvegarde restaurée")})}catch(error){toast(`Fichier invalide : ${error.message}`,"error")}}}
function onSubmit(event){event.preventDefault();if(event.target.id==="nutrition-form"){const data=new FormData(event.target);editingNutritionId=null;persist(saveNutrition(state,{id:data.get("id")||null,date:selectedNutritionDate,calories:data.get("calories"),protein:data.get("protein")}));toast("Apport enregistré")}if(event.target.id==="library-form")saveLibraryForm(event.target)}

function confirmAction(title,text,run,danger=true){modal={type:"confirm",title,text,label:"Confirmer",danger,run};render()}
function mutateDraft(change,rerender=true){const draft=activeDraft();state={...state,workoutLogs:state.workoutLogs.map(log=>log.id===draft.id?change(log):log)};const snapshot=state;saveQueue=saveQueue.then(()=>saveState(snapshot)).catch(error=>toast(error.message,"error"));if(rerender)render()}
function mutateSet(exerciseId,setId,change,rerender=true){mutateDraft(d=>({...d,exercises:d.exercises.map(ex=>ex.id===exerciseId?{...ex,sets:ex.sets.map(set=>set.id===setId?change(set):set)}:ex)}),rerender)}
function toggleSet(exerciseId,setId){const draft=activeDraft(),exercise=draft.exercises.find(ex=>ex.id===exerciseId),set=exercise.sets.find(x=>x.id===setId);if(!set.completed&&!isSetValid(set))throw new Error("Renseigne la charge et les répétitions");const now=Date.now();const rest=!set.completed&&exercise.restStartedAt&&exercise.restTargetSetOrder===set.order?Math.max(0,Math.floor((now-exercise.restStartedAt)/1000)-40):set.restBeforeSeconds;const hasNext=exercise.sets.some(x=>x.order===set.order+1);mutateDraft(d=>({...d,exercises:d.exercises.map(ex=>ex.id===exerciseId?{...ex,sets:ex.sets.map(x=>x.id===setId?{...x,completed:!x.completed,restBeforeSeconds:rest}:x),restStartedAt:!set.completed&&hasNext?now:null,restTargetSetOrder:!set.completed&&hasNext?set.order+1:null}:ex)}));document.activeElement?.blur()}
function adjustRep(exerciseId,setId,delta){if(!setId)return;mutateSet(exerciseId,setId,set=>({...set,reps:String(Math.min(999,Math.max(1,(Number(set.reps)||0)+delta))),completed:false}))}
function addSet(exerciseId){mutateDraft(d=>({...d,exercises:d.exercises.map(ex=>ex.id===exerciseId?{...ex,plannedSets:ex.plannedSets+1,sets:[...ex.sets,{id:uid(),order:ex.sets.length+1,weightKg:ex.sets.at(-1)?.weightKg??"",reps:ex.sets.at(-1)?.reps??"",rir:null,restBeforeSeconds:null,completed:false}]}:ex)}))}
function removeSet(exerciseId,setId){const exercise=activeDraft().exercises.find(ex=>ex.id===exerciseId);if(exercise.sets.length<=1)throw new Error("Un exercice doit garder au moins une série");const set=exercise.sets.find(item=>item.id===setId);const run=()=>mutateDraft(d=>({...d,exercises:d.exercises.map(ex=>{if(ex.id!==exerciseId)return ex;const sets=ex.sets.filter(item=>item.id!==setId).map((item,index)=>({...item,order:index+1}));return{...ex,sets,plannedSets:sets.length,restStartedAt:ex.restTargetSetOrder===set.order?null:ex.restStartedAt,restTargetSetOrder:ex.restTargetSetOrder===set.order?null:ex.restTargetSetOrder}})}));if(set.completed||set.weightKg!==""||set.reps!=="")confirmAction("Retirer cette série ?","Les valeurs saisies seront supprimées.",run);else run()}
function stopTimer(exerciseId){mutateDraft(d=>({...d,exercises:d.exercises.map(ex=>ex.id===exerciseId?{...ex,restStartedAt:null,restTargetSetOrder:null}:ex)}))}
function focusNextSet(exerciseId,setId){const selector=`[data-exercise-id="${CSS.escape(exerciseId)}"][data-set-id="${CSS.escape(setId)}"][data-set-field="weightKg"]`;const input=document.querySelector(selector);input?.scrollIntoView({behavior:"smooth",block:"center"});setTimeout(()=>input?.focus(),250)}
function addWorkoutExercise(exerciseId){const source=state.exercises.find(ex=>ex.id===exerciseId);const previous=lastPerformedExercise(state,exerciseId)?.exercise;mutateDraft(d=>({...d,exercises:[...d.exercises,{id:uid(),exerciseId:source.id,nameSnapshot:source.name,instructionSnapshot:source.instruction,repMinSnapshot:source.defaultRepMin,repMaxSnapshot:source.defaultRepMax,musclesSnapshot:source.muscles.map(a=>({...a,name:state.muscles.find(m=>m.id===a.muscleId)?.name??""})),plannedSets:2,sets:[1,2].map(order=>{const old=previous?.sets.find(s=>s.order===order&&s.completed&&isSetValid(s));return{id:uid(),order,weightKg:old?.weightKg??"",reps:old?.reps??"",rir:null,restBeforeSeconds:null,completed:false}}),restStartedAt:null,restTargetSetOrder:null}]}))}
function stepWeight(delta){const input=document.querySelector("#weight-input"),value=Number(String(input.value).replace(",","."));if(Number.isFinite(value))input.value=(Math.max(.1,Math.min(500,value+delta))).toFixed(1).replace(".",",")}
function saveWeightFromForm(){persist(saveWeight(state,selectedWeightDate,document.querySelector("#weight-input").value));toast("Mesure enregistrée")}
function archiveLibrary(kind,id){let next=state[kind].map(item=>item.id===id?{...item,archived:!item.archived,active:kind==="programs"?false:item.active}:item);persist({...state,[kind]:next})}
function deleteLibrary(kind,id){const used={exercises:state.workoutLogs.some(log=>log.exercises.some(ex=>ex.exerciseId===id)),templates:state.workoutLogs.some(log=>log.templateId===id),programs:state.workoutLogs.some(log=>log.programId===id),muscles:state.workoutLogs.some(log=>log.exercises.some(ex=>ex.musclesSnapshot.some(m=>m.muscleId===id)))}[kind]??false;if(used)throw new Error("Cet élément est utilisé dans l’historique : archive-le plutôt");confirmAction("Supprimer définitivement ?","Cette action ne pourra pas être annulée.",()=>{let next={...state,[kind]:state[kind].filter(item=>item.id!==id)};if(kind==="exercises")next.templates=next.templates.map(t=>({...t,exercises:t.exercises.filter(ex=>ex.exerciseId!==id)})).filter(t=>t.exercises.length);if(kind==="templates")next.programs=next.programs.map(p=>({...p,templateCycle:p.templateCycle.filter(x=>x!==id),nextIndex:0}));if(kind==="muscles")next.exercises=next.exercises.map(ex=>({...ex,muscles:ex.muscles.filter(m=>m.muscleId!==id)}));persist(next)})}
function saveLibraryForm(form){const data=new FormData(form),kind=form.dataset.kind,id=form.dataset.id||uid(),existing=state[kind].find(x=>x.id===id),returnToWorkout=form.dataset.returnWorkout==="true",orderedIds=[...form.querySelectorAll("[data-order-item]")].map(row=>row.dataset.itemId);let item;if(kind==="muscles")item={id,name:data.get("name"),archived:data.has("archived")};if(kind==="exercises"){const min=Number(data.get("defaultRepMin")),max=Number(data.get("defaultRepMax"));if(min>max)throw new Error("La plage de répétitions est invalide");item={id,name:data.get("name"),defaultRepMin:min,defaultRepMax:max,instruction:data.get("instruction"),archived:existing?.archived??false,muscles:state.muscles.map(m=>({muscleId:m.id,role:data.get(`muscle:${m.id}`)})).filter(x=>x.role)}}if(kind==="templates"){const exercises=orderedIds.filter(exerciseId=>data.has(`exercise:${exerciseId}`)).map(exerciseId=>{const source=state.exercises.find(ex=>ex.id===exerciseId),minRaw=data.get(`min:${exerciseId}`),maxRaw=data.get(`max:${exerciseId}`),min=minRaw===""?null:Number(minRaw),max=maxRaw===""?null:Number(maxRaw);if((min??source.defaultRepMin)>(max??source.defaultRepMax))throw new Error(`Plage invalide pour ${source.name}`);return{exerciseId,targetSets:Number(data.get(`sets:${exerciseId}`))||2,repMinOverride:min,repMaxOverride:max}});item={id,name:data.get("name"),archived:existing?.archived??false,exercises}}if(kind==="programs"){const active=data.has("active");item={id,name:data.get("name"),templateCycle:orderedIds.filter(templateId=>data.has(`template:${templateId}`)),trainingDays:[1,2,3,4,5,6,7].filter(d=>data.has(`day:${d}`)),nextIndex:existing?.nextIndex??0,scheduleCheckedThrough:existing?.scheduleCheckedThrough??localDate(),active,archived:existing?.archived??false};if(active)state={...state,programs:state.programs.map(p=>({...p,active:false}))}}if((kind==="templates"||kind==="programs")&&!((item.exercises??item.templateCycle).length))throw new Error("Ajoute au moins un élément");modal=null;persist({...state,[kind]:[...state[kind].filter(x=>x.id!==id),item]});if(returnToWorkout&&kind==="exercises")addWorkoutExercise(id);toast("Enregistré")}
function elapsed(start){const total=Math.max(0,Math.floor((Date.now()-start)/1000));return `${String(Math.floor(total/60)).padStart(2,"0")}:${String(total%60).padStart(2,"0")}`}
function formatClock(rawSeconds,withHours=false){const total=Math.max(0,Math.floor(rawSeconds)),hours=Math.floor(total/3600),minutes=Math.floor((total%3600)/60),seconds=total%60;return withHours||hours?`${String(hours).padStart(2,"0")}:${String(minutes).padStart(2,"0")}:${String(seconds).padStart(2,"0")}`:`${String(minutes).padStart(2,"0")}:${String(seconds).padStart(2,"0")}`}
function durationDialog(title,totalSeconds,allowClear){const total=Math.max(0,Math.min(86400,Math.floor(totalSeconds||0))),hours=Math.floor(total/3600),minutes=Math.floor((total%3600)/60),seconds=total%60;return dialog({title,content:`<div class="form-grid triple"><label class="field"><span>h</span><input id="duration-hours" inputmode="numeric" type="number" min="0" max="24" value="${hours}"></label><label class="field"><span>min</span><input id="duration-minutes" inputmode="numeric" type="number" min="0" max="59" value="${minutes}"></label><label class="field"><span>s</span><input id="duration-seconds" inputmode="numeric" type="number" min="0" max="59" value="${seconds}"></label></div>`,actions:`${allowClear?button("Effacer","clear-duration",{kind:"ghost"}):""}${button("Annuler","dismiss-dialog",{kind:"ghost"})}${button("Enregistrer","save-duration")}`})}
function durationFromDialog(){const hours=Number(document.querySelector("#duration-hours")?.value),minutes=Number(document.querySelector("#duration-minutes")?.value),seconds=Number(document.querySelector("#duration-seconds")?.value);if(!Number.isInteger(hours)||hours<0||hours>24||!Number.isInteger(minutes)||minutes<0||minutes>59||!Number.isInteger(seconds)||seconds<0||seconds>59)throw new Error("Durée invalide");const total=hours*3600+minutes*60+seconds;if(total>86400)throw new Error("La durée ne peut pas dépasser 24 h");return total}
function updateTimer(){const timer=document.querySelector("[data-timer]"),start=Number(timer?.closest("[data-start]")?.dataset.start);if(timer&&start)timer.textContent=elapsed(start);const session=document.querySelector("[data-workout-elapsed]");if(session){const sessionStart=Number(session.dataset.start),sessionEnd=Number(session.dataset.end)||Date.now();session.textContent=formatClock((sessionEnd-sessionStart)/1000,true)}}
