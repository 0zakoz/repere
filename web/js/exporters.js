import { chronology, completedLogs, isSetValid, lastPerformedExercise, nutritionTrend, weightTrend } from "./rules.js";

const csvCell = value => {
  const raw = String(value ?? "");
  return /[",\n\r]/.test(raw) ? `"${raw.replaceAll('"', '""')}"` : raw;
};
const csvRow = values => values.map(csvCell).join(",");

export function workoutCsv(state) {
  const headers = ["export_version","session_id","session_date","started_at","ended_at","duration_seconds","program_name","workout_name","session_note","exercise_order","exercise_id","exercise_name","target_rep_min","target_rep_max","primary_muscles","secondary_muscles","tertiary_muscles","set_number","weight_kg","reps","rir","rest_before_seconds"];
  const rows = [headers.join(",")];
  completedLogs(state).sort((a, b) => chronology(a) - chronology(b)).forEach(log => log.exercises.forEach((exercise, index) => {
    const muscle = role => exercise.musclesSnapshot.filter(item => item.role === role).map(item => item.name).join("|");
    exercise.sets.filter(set => set.completed && isSetValid(set)).forEach(set => rows.push(csvRow([
      2, log.id, log.localDate, new Date(log.startedAt).toISOString(), new Date(log.endedAt ?? log.startedAt).toISOString(),
      Math.max(0, (log.endedAt ?? log.startedAt) - log.startedAt) / 1000, log.programNameSnapshot, log.templateNameSnapshot,
      log.note, index + 1, exercise.exerciseId, exercise.nameSnapshot, exercise.repMinSnapshot, exercise.repMaxSnapshot,
      muscle("PRIMARY"), muscle("SECONDARY"), muscle("TERTIARY"), set.order, String(set.weightKg).replace(",", "."), set.reps, set.rir, set.restBeforeSeconds,
    ])));
  }));
  return `\ufeff${rows.join("\n")}\n`;
}

export function weightCsv(state) {
  const averages = new Map(weightTrend(state.bodyWeights, null).map(item => [item.date, item.average7DaysKg]));
  const rows = ["date,weight_kg,average_7_days_kg,created_at,updated_at"];
  state.bodyWeights.slice().sort((a, b) => a.date.localeCompare(b.date)).forEach(item => rows.push(csvRow([
    item.date, item.weightKg.toFixed(1), averages.get(item.date)?.toFixed(1), new Date(item.createdAt).toISOString(), new Date(item.updatedAt).toISOString(),
  ])));
  return `\ufeff${rows.join("\n")}\n`;
}

export function nutritionCsv(state) {
  const totals = new Map(nutritionTrend(state.nutritionEntries, null).map(item => [item.date, item]));
  const rows = ["date,time,calories_kcal,protein_g,daily_calories_kcal,daily_protein_g,created_at,updated_at,entry_id"];
  state.nutritionEntries.slice().sort((a, b) => a.date.localeCompare(b.date) || a.createdAt - b.createdAt).forEach(item => {
    const total = totals.get(item.date);
    rows.push(csvRow([item.date, new Date(item.createdAt).toTimeString().slice(0, 8), item.caloriesKcal, item.proteinGrams.toFixed(1), total?.caloriesKcal, total?.proteinGrams.toFixed(1), new Date(item.createdAt).toISOString(), new Date(item.updatedAt).toISOString(), item.id]));
  });
  return `\ufeff${rows.join("\n")}\n`;
}

export function markdownExport(state, version = "Web 1.9.3") {
  const drafts = state.workoutLogs.filter(log => log.status === "DRAFT" && log.deletedAt == null);
  const weightsTrend = weightTrend(state.bodyWeights, null);
  const nutritionTotals = nutritionTrend(state.nutritionEntries, null);
  const latestWeight = state.bodyWeights.slice().sort((a,b)=>b.date.localeCompare(a.date)||b.updatedAt-a.updatedAt)[0];
  const latestNutrition = nutritionTotals.at(-1);
  const lines = ["# Repère — export complet pour analyse", "", "Ce document contient toutes les informations connues de l’application, présentées pour un humain ou une analyse dans ChatGPT. Il n’invente aucun profil, objectif, donnée de santé ou contexte non stocké.", "", `- **Version de l’application** : ${version}`, `- **Version du schéma de données** : ${state.schemaVersion}`, `- **Généré le** : ${new Date().toLocaleString("fr-FR",{timeZoneName:"short"})}`, `- **Fuseau horaire** : ${Intl.DateTimeFormat().resolvedOptions().timeZone}`, "", "## Synthèse", "", `- **Programmes** : ${state.programs.length}`, `- **Programme actif** : ${md(state.programs.find(p=>p.active&&!p.archived)?.name??"Aucun")}`, `- **Modèles de séance** : ${state.templates.length}`, `- **Exercices** : ${state.exercises.length}`, `- **Groupes musculaires** : ${state.muscles.length}`, `- **Séances terminées visibles** : ${completedLogs(state).length}`, `- **Séances en cours** : ${drafts.length}`, `- **Événements de programme** : ${state.programEvents.length}`, `- **Pesées** : ${state.bodyWeights.length}`, `- **Dernière pesée** : ${latestWeight?`${latestWeight.date} — ${latestWeight.weightKg.toFixed(1)} kg`:"Aucune"}`, `- **Apports nutritionnels** : ${state.nutritionEntries.length}`, `- **Dernier jour nutritionnel** : ${latestNutrition?`${latestNutrition.date} — ${latestNutrition.caloriesKcal} kcal, ${latestNutrition.proteinGrams.toFixed(1)} g de protéines (${latestNutrition.entryCount} apport(s))`:"Aucun"}`, ""];
  lines.push("## Programmes", "");
  state.programs.forEach((program, i) => { const nextId=program.templateCycle.length?program.templateCycle[program.nextIndex%program.templateCycle.length]:null; lines.push(`### ${i + 1}. ${md(program.name)}`, "", `- **Identifiant** : \`${program.id}\``, `- **État** : ${program.active ? "actif" : "inactif"}${program.archived ? ", archivé" : ""}`, `- **Jours indicatifs** : ${program.trainingDays.map(dayName).join(", ") || "Aucun"}`, `- **Index de la prochaine séance** : ${program.nextIndex}`, `- **Calendrier vérifié jusqu’au** : ${program.scheduleCheckedThrough||"—"}`, `- **Prochaine séance** : ${nextId?`${md(state.templates.find(t=>t.id===nextId)?.name??"Introuvable")} (\`${nextId}\`)`:"Aucune"}`, "- **Cycle ordonné** :", ...(program.templateCycle.length?program.templateCycle.map((id,index)=>`  ${index+1}. ${md(state.templates.find(t=>t.id===id)?.name??"Introuvable")} (\`${id}\`)${id===nextId?" — prochaine":""}`):["  - Aucun modèle."]), ""); });
  lines.push("## Modèles de séance", "");
  state.templates.forEach((template, i) => {
    lines.push(`### ${i + 1}. ${md(template.name)}${template.archived ? " — archivée" : ""}`, "", `- **ID** : \`${template.id}\``, "");
    template.exercises.forEach((entry, j) => {
      const ex = state.exercises.find(item => item.id === entry.exerciseId);
      const last=lastPerformedExercise(state,entry.exerciseId); const muscle=role=>ex?.muscles.filter(a=>a.role===role).map(a=>state.muscles.find(m=>m.id===a.muscleId)?.name??a.muscleId).join(", ")||"—";
      lines.push(`#### ${j + 1}. ${md(ex?.name ?? "Exercice introuvable")}`, "", `- **Identifiant de l’exercice** : \`${entry.exerciseId}\``, `- **Séries cibles** : ${entry.targetSets}`, `- **Plage effective** : ${entry.repMinOverride ?? ex?.defaultRepMin ?? "—"}–${entry.repMaxOverride ?? ex?.defaultRepMax ?? "—"} reps`, `- **Surcharge propre au modèle** : ${entry.repMinOverride==null&&entry.repMaxOverride==null?"Aucune":`${entry.repMinOverride??"—"}–${entry.repMaxOverride??"—"}`}`, `- **Consigne** : ${md(ex?.instruction||"—")}`, `- **Muscles principaux** : ${md(muscle("PRIMARY"))}`, `- **Muscles secondaires** : ${md(muscle("SECONDARY"))}`, `- **Muscles tertiaires** : ${md(muscle("TERTIARY"))}`, `- **Dernière performance réelle** : ${last?lastSummary(last):"Aucune"}`, "");
    }); lines.push("");
  });
  lines.push("## Bibliothèque des exercices", "");
  state.exercises.forEach((exercise, i) => {
    const muscles = role => exercise.muscles.filter(item => item.role === role).map(item => state.muscles.find(m => m.id === item.muscleId)?.name ?? item.muscleId).join(", ") || "—";
    const last = lastPerformedExercise(state, exercise.id);
    lines.push(`### ${i + 1}. ${md(exercise.name)}${exercise.archived ? " — archivé" : ""}`, "", `- **Identifiant** : \`${exercise.id}\``, `- **Plage par défaut** : ${exercise.defaultRepMin}–${exercise.defaultRepMax} reps`, `- **Consigne** : ${md(exercise.instruction || "—")}`, `- **Principaux ×1** : ${md(muscles("PRIMARY"))}`, `- **Secondaires ×0,5** : ${md(muscles("SECONDARY"))}`, `- **Tertiaires ×0,25** : ${md(muscles("TERTIARY"))}`, `- **Dernière performance réelle** : ${last ? lastSummary(last) : "Aucune"}`, "");
  });
  lines.push("## Groupes musculaires", "", ...(state.muscles.length?state.muscles.map((muscle,index)=>`${index+1}. **${md(muscle.name)}** — identifiant \`${muscle.id}\` — ${muscle.archived?"archivé":"actif"}`):["Aucun groupe musculaire."]), "");
  lines.push("## Historique complet des séances", "");
  [...completedLogs(state), ...drafts].sort((a,b) => chronology(a)-chronology(b)).forEach((log, i) => {
    const duration=log.endedAt==null?"En cours":formatMs(Math.max(0,log.endedAt-log.startedAt));
    lines.push(`### ${i + 1}. ${log.localDate} — séance ${md(log.templateNameSnapshot)}`, "", `- **Identifiant** : \`${log.id}\``, `- **Statut** : ${log.status === "COMPLETED" ? "Terminée" : "Séance en cours"}`, `- **Modèle** : ${md(log.templateNameSnapshot)} (${log.templateId?`\`${log.templateId}\``:"—"})`, `- **Programme** : ${log.programNameSnapshot?`${md(log.programNameSnapshot)} (${log.programId?`\`${log.programId}\``:"—"})`:"Hors programme"}`, `- **Date locale choisie** : ${log.localDate}`, `- **Début** : ${dateTime(log.startedAt)}`, `- **Fin** : ${log.endedAt ? dateTime(log.endedAt) : "—"}`, `- **Durée** : ${duration}`, `- **Fait avancer le programme** : ${yesNo(log.advanceProgramOnFinish)}`, `- **Édition d’une séance terminée** : ${yesNo(log.editingCompletedLog)}`, `- **Note** : ${md(log.note || "—")}`, `- **Séries réalisées** : ${log.exercises.reduce((sum,ex)=>sum+ex.sets.filter(set=>set.completed).length,0)}`, `- **Séries prévues** : ${log.exercises.reduce((sum,ex)=>sum+ex.plannedSets,0)}`, "");
    log.exercises.forEach((ex,exerciseIndex) => {
      const snapshot=role=>ex.musclesSnapshot.filter(a=>a.role===role).map(a=>`${md(a.name)} (\`${a.muscleId}\`)`).join(", ")||"—";
      lines.push(`#### ${exerciseIndex+1}. ${md(ex.nameSnapshot)}`, "", `- **Identifiant du log d’exercice** : \`${ex.id}\``, `- **Identifiant de l’exercice** : \`${ex.exerciseId}\``, `- **Consigne snapshot** : ${md(ex.instructionSnapshot||"—")}`, `- **Plage snapshot** : ${ex.repMinSnapshot}–${ex.repMaxSnapshot} reps`, `- **Séries prévues** : ${ex.plannedSets}`, `- **Muscles principaux snapshot** : ${snapshot("PRIMARY")}`, `- **Muscles secondaires snapshot** : ${snapshot("SECONDARY")}`, `- **Muscles tertiaires snapshot** : ${snapshot("TERTIARY")}`, `- **Chronomètre démarré** : ${ex.restStartedAt?dateTime(ex.restStartedAt):"—"}`, `- **Série ciblée par le chronomètre** : ${ex.restTargetSetOrder??"—"}`, "", "| Série | État | Charge | Reps | RIR | Repos avant | Identifiant |", "|---:|---|---:|---:|---:|---:|---|");
      ex.sets.slice().sort((a,b)=>a.order-b.order).forEach(set => lines.push(`| ${set.order} | ${set.completed ? "**Réalisée**" : "**Non réalisée**"} | ${md(set.weightKg || "—")} | ${md(set.reps || "—")} | ${set.rir ?? "—"} | ${set.restBeforeSeconds == null ? "—" : `${set.restBeforeSeconds}s`} | \`${set.id}\` |`)); lines.push("");
    });
  });
  lines.push("## Événements de programme", "");
  if(!state.programEvents.length)lines.push("Aucun événement.",""); else state.programEvents.slice().sort((a,b)=>a.date.localeCompare(b.date)||a.id.localeCompare(b.id)).forEach((event,index)=>lines.push(`### ${index+1}. ${event.date} — ${event.outcome==="COMPLETED"?"Séance terminée":event.outcome==="SKIPPED"?"Créneau sauté":md(event.outcome)}`,"",`- **Identifiant** : \`${event.id}\``,`- **Programme** : ${md(state.programs.find(p=>p.id===event.programId)?.name??"Introuvable")} (\`${event.programId}\`)`,`- **Séance** : ${md(state.templates.find(t=>t.id===event.templateId)?.name??"Introuvable")} (\`${event.templateId}\`)`,`- **Résultat brut** : ${md(event.outcome)}`,`- **Log de séance lié** : ${event.workoutLogId?`\`${event.workoutLogId}\``:"—"}`,""));
  lines.push("## Suivi du poids", "", "| Date | Poids | Moyenne 7 jours | Créée le | Modifiée le | Identifiant |", "|---|---:|---:|---|---|---|");
  const weights = new Map(weightsTrend.map(item => [item.date, item]));
  state.bodyWeights.slice().sort((a,b)=>a.date.localeCompare(b.date)||a.updatedAt-b.updatedAt).forEach(item => lines.push(`| ${item.date} | ${item.weightKg.toFixed(1)} kg | ${weights.get(item.date)?.average7DaysKg.toFixed(1) ?? "—"} kg | ${dateTime(item.createdAt)} | ${dateTime(item.updatedAt)} | \`${item.id}\` |`));
  lines.push("", "## Suivi nutritionnel", "", "Chaque ligne représente un apport saisi au fil de la journée ; les totaux additionnent toutes les entrées de la date.", "", "| Date | Heure | Calories | Protéines | Total kcal | Total protéines | Créée le | Modifiée le | Identifiant |", "|---|---|---:|---:|---:|---:|---|---|---|");
  const nutritionByDate=new Map(nutritionTotals.map(item=>[item.date,item]));
  state.nutritionEntries.slice().sort((a,b)=>a.date.localeCompare(b.date)||a.createdAt-b.createdAt).forEach(item => {const total=nutritionByDate.get(item.date);lines.push(`| ${item.date} | ${new Date(item.createdAt).toLocaleTimeString("fr-FR", {hour:"2-digit",minute:"2-digit"})} | ${item.caloriesKcal} kcal | ${item.proteinGrams.toFixed(1)} g | ${total?.caloriesKcal??item.caloriesKcal} kcal | ${(total?.proteinGrams??item.proteinGrams).toFixed(1)} g | ${dateTime(item.createdAt)} | ${dateTime(item.updatedAt)} | \`${item.id}\` |`)});
  lines.push("", "## Règles de lecture", "", "- Une série **réalisée** a été explicitement validée dans l’application.", "- Une série **non réalisée** peut conserver des valeurs préremplies : elles ne constituent pas une performance.", "- Les séances supprimées sont exclues de ce document.", "- Les associations musculaires de la bibliothèque représentent leur état actuel.");
  return `${lines.join("\n")}\n`;
}

const md = value => String(value).replaceAll("&","&amp;").replaceAll("<","&lt;").replaceAll(">","&gt;").replaceAll("\\", "\\\\").replace(/([`*_\[\]#|])/g, "\\$1").replaceAll("\r\n", "<br>").replaceAll("\r", "<br>").replaceAll("\n", "<br>");
const yesNo = value => value ? "Oui" : "Non";
const dateTime = value => new Date(value).toLocaleString("fr-FR",{timeZoneName:"short"});
const formatMs = milliseconds => { const seconds=Math.floor(milliseconds/1000),hours=Math.floor(seconds/3600),minutes=Math.floor((seconds%3600)/60);return `${hours?`${hours}h `:""}${minutes}min ${seconds%60}s`; };
const dayName = day => ["","lundi","mardi","mercredi","jeudi","vendredi","samedi","dimanche"][day] ?? `jour ${day}`;
const lastSummary = last => `${last.workout.localDate}, séance ${md(last.workout.templateNameSnapshot)} (\`${last.workout.id}\`) — ${last.exercise.sets.filter(set=>set.completed&&isSetValid(set)).map(set=>`S${set.order}: ${md(set.weightKg)} kg × ${md(set.reps)} reps, RIR ${set.rir??"—"}, repos ${set.restBeforeSeconds==null?"—":`${set.restBeforeSeconds}s`}`).join(" ; ")}`;
