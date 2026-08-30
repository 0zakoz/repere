# Données, compatibilité et exports

## Persistance

Room contient une table unique `app_state` et une ligne d’identifiant `1`. Sa colonne `json`
est la sérialisation complète de `AppState` par `kotlinx.serialization` avec valeurs par défaut,
clés inconnues ignorées et format lisible.

Deux versions coexistent :

- schéma Room : **1**, décrit dans `app/schemas/fr.suivimuscu.app.data.AppDatabase/1.json` ;
- schéma métier `AppState.schemaVersion` : **3**, migré par `StateMigrations`.

Une modification des classes sérialisées ne nécessite pas forcément une migration Room. Elle
peut en revanche nécessiter une nouvelle version métier, des valeurs par défaut et une étape de
migration séquentielle. Ne jamais compter sur une réinitialisation de l’application pour faire
évoluer les données de l’utilisateur.

## Modèle persistant

`AppState` contient :

- `muscles` : groupes musculaires nommés et archivables ;
- `exercises` : plage de reps, consigne et affectations `PRIMARY`, `SECONDARY`, `TERTIARY` ;
- `templates` : modèles de séance et liste ordonnée d’exercices avec objectifs ;
- `programs` : cycles de modèles, jours indicatifs, position suivante et état actif ;
- `programEvents` : séances accomplies ou créneaux sautés dans un programme ;
- `workoutLogs` : brouillons, séances terminées et suppressions temporaires ;
- `bodyWeights` : une mesure datée, avec dates de création et modification.

Un `WorkoutLog` conserve notamment la date locale choisie, les horodatages de début/fin, le
modèle et le programme éventuels, une note et les exercices. Chaque `LoggedExercise` stocke
l’identifiant stable de l’exercice ainsi que des snapshots de son nom, sa consigne, sa plage de
reps et ses muscles. Chaque `WorkoutSet` stocke charge et répétitions sous forme de chaînes,
RIR, repos et état de validation.

Les chaînes de charge permettent une saisie française à virgule sans perte de l’entrée. Les
calculs et exports la normalisent au besoin avec un point décimal.

## Historique et statistiques

- Seuls les logs `COMPLETED`, non marqués par `deletedAt`, alimentent les exports et tendances.
- La chronologie combine `localDate` avec l’heure du `startedAt` d’origine ; éditer une séance
  ne la fait donc pas remonter artificiellement.
- Une séance terminée conserve les exercices et séries planifiés non réalisés. Le booléen
  `completed` distingue le prévu du réalisé.
- Les tendances par exercice regroupent sur `exerciseId` et utilisent les snapshots de plage
  de répétitions du log.
- Les tendances par séance utilisent le modèle actuel pour compléter les exercices planifiés
  absents d’un ancien log, puis incluent aussi les exercices ponctuels du log.
- Les tendances musculaires utilisent l’affectation actuelle de l’exercice lorsqu’il existe,
  sinon son snapshot. Les facteurs sont 1, 0,5 et 0,25.
- Les moyennes de RIR et de repos ignorent les valeurs absentes.
- Le poids est unique par date dans les règles métier ; une nouvelle sauvegarde du même jour
  remplace l’entrée logique et conserve sa date de création.

## Migrations existantes

- v1 → v2 : remplace l’ancien groupe générique `forearms` par les fléchisseurs et extenseurs de
  l’avant-bras, adapte les exercices et leurs snapshots historiques sans perdre les rôles.
- v2 → v3 : introduit le schéma comprenant les pesées ; les valeurs par défaut assurent la
  compatibilité des anciens JSON.

`StateMigrations.toLatest` refuse les versions hors de l’intervalle supporté. Toute nouvelle
migration doit être déterministe, testée depuis les versions réellement existantes et appliquée
aussi bien au chargement local qu’à la restauration d’une sauvegarde.

## Sauvegarde JSON

La sauvegarde est l’encodage complet de `AppState`. La restauration :

1. lit le fichier choisi via le Storage Access Framework ;
2. décode et migre l’intégralité du JSON ;
3. demande confirmation avant remplacement ;
4. ne modifie l’état qu’en cas de validation réussie.

Le JSON n’est pas chiffré. Il peut contenir notes, performances, poids et historique complet.
Pour valider une évolution, ajouter au minimum un test de lecture d’un ancien état et un test
d’aller-retour du schéma courant.

## Export des performances

`CsvExporter` produit un CSV UTF-8 avec BOM, séparateur virgule et une ligne par série validée
d’une séance terminée. Les champs sont échappés selon les règles CSV usuelles.

Colonnes actuelles :

```text
export_version,session_id,session_date,started_at,ended_at,duration_seconds,
program_name,workout_name,session_note,exercise_order,exercise_id,exercise_name,
target_rep_min,target_rep_max,primary_muscles,secondary_muscles,tertiary_muscles,
set_number,weight_kg,reps,rir,rest_before_seconds
```

`export_version` vaut actuellement `2`. Les horodatages sont ISO-8601, les muscles multiples
sont séparés par `|` et les décimales utilisent un point.

## Export des pesées

`WeightCsvExporter` produit un CSV UTF-8 avec BOM trié par date :

```text
date,weight_kg,average_7_days_kg,created_at,updated_at
```

La moyenne utilise les mesures disponibles entre la date et ses six jours précédents ; elle
n’invente aucune valeur pour les jours sans pesée.

Changer une colonne ou sa sémantique peut casser les analyses externes de l’utilisateur. Une
évolution doit donc être explicite, testée et accompagnée d’une mise à jour de la version
d’export lorsque la compatibilité l’exige.

## Export Markdown complet

`CompleteMarkdownExporter` produit un document `.md` destiné à être lu directement ou joint à
une conversation ChatGPT. Il couvre tout l’état utile de l’application : programmes actifs et
archivés, modèles, bibliothèque, dernière performance réelle de chaque exercice, séances
terminées visibles, brouillon, événements de cycle et pesées.

La dernière performance réelle est l’occurrence chronologiquement la plus récente qui contient
au moins une série validée et valide. Les brouillons, séances supprimées et exercices laissés
vides ne la remplacent pas. Une occurrence partielle reste la référence, mais seuls ses rangs de
série réalisés sont proposés au prochain démarrage.

Les séries non validées restent présentes dans l’historique Markdown avec l’état **Non
réalisée**. Leurs éventuelles valeurs préremplies ne doivent jamais être interprétées comme une
performance. Les séances marquées supprimées sont exclues, conformément aux autres vues.
