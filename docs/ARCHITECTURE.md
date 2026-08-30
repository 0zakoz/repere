# Architecture actuelle

## Vue d’ensemble

Le dépôt contient une application Android mono-module (`app`) et une PWA statique (`web`).
L’interface Android est écrite en Kotlin avec Jetpack Compose et Material 3 ; la version Web
utilise des modules JavaScript natifs, CSS et IndexedDB sans framework ni dépendance npm.

```text
MainActivity / écrans Compose
            ↓ actions et StateFlow
       MainViewModel
            ↓ état complet
       AppRepository
            ↓ JSON
        Room (1 ligne)
```

`SuiviMuscuApplication` crée manuellement la base et le repository. `MainActivity` obtient le
`MainViewModel` via une factory, observe son `StateFlow<AppState?>` et affiche les cinq onglets.
La navigation ne repose pas sur Navigation Compose : `MainTab` et quelques états Compose
pilotent les écrans et boîtes de dialogue.

Le client Web suit une structure parallèle plus légère :

```text
app.js / ui.js / charts.js
          ↓ actions
        rules.js
          ↓ AppState complet
        store.js
          ↓ clone structuré
     IndexedDB (1 enregistrement)
```

`index.html` charge `app.js`, qui rend les cinq onglets et délègue les événements. `rules.js`
porte les calculs purs réutilisés par les tests. `state.js` normalise et migre les sauvegardes,
`seed.js` reproduit le premier lancement Android et `exporters.js` génère les fichiers locaux.
`sw.js` met en cache le cœur statique pour les ouvertures hors ligne.

## Responsabilités principales

### Couche application

- `MainActivity.kt` observe les préférences d’apparence, assemble le thème, la barre de
  navigation, les lanceurs du Storage Access Framework et les dialogues de réglages.
- `MainViewModel.kt` contient les mutations métier, le cycle des programmes, la gestion du
  brouillon, les règles de suppression, le poids, la nutrition et les agrégations de tendances.
- `SuiviMuscuApplication.kt` construit `AppDatabase` et `AppRepository` sans conteneur DI.

Le ViewModel est aujourd’hui le principal point de coordination. Une future évolution peut
extraire des services ou cas d’usage si cela améliore réellement la lisibilité ou les tests ;
ce découpage n’est pas une contrainte d’architecture.

### Couche données

- `Models.kt` définit les objets sérialisés de `AppState` et les projections non persistées des
  tendances.
- `AppDatabase.kt` expose une table Room `app_state` contenant une seule chaîne JSON.
- `AppRepository.kt` initialise les données, observe la ligne Room, sérialise l’état et applique
  les migrations métier.
- `SeedData.kt` crée la bibliothèque, les séances A/B et le programme Full body du premier
  lancement.
- `StateMigrations.kt` fait évoluer les sauvegardes et l’état persistant jusqu’au schéma métier
  courant.
- `Exporters.kt` produit les trois CSV indépendamment de l’interface.

Room est ici un conteneur durable et observable, pas un modèle relationnel normalisé. La version
Room et la version du JSON sont donc indépendantes.

### Interface Compose

- `JournalScreen.kt` : suggestion du programme, brouillon en pause et historique.
- `WorkoutScreen.kt` : éditeur de séance, séries, RIR et chronomètre flottant.
- `WeightScreen.kt` : saisie quotidienne, historique, graphique et suppression.
- `NutritionScreen.kt` : apports progressifs, totaux quotidiens, édition et courbes calories/protéines.
- `TrendsScreen.kt` : sélecteurs et présentations des trois familles de tendances.
- `LibraryScreen.kt` : CRUD, archivage et activation des éléments de bibliothèque.
- `Charts.kt` : graphiques Canvas réutilisés pour les exercices et barres musculaires.
- `MuscleFigure.kt` : carte musculaire vectorielle dessinée en Compose Canvas.
- `Theme.kt` : dix variantes Material 3, formes, typographies, jetons visuels sémantiques et
  composants décoratifs Kawaii partagés (fond, surfaces, mascottes et icônes de navigation).
- `AppearanceSettings.kt` : galerie des cinq thèmes et choix clair/sombre/système.

Les graphiques n’utilisent aucune bibliothèque externe. Les calculs testables sont placés hors
des composables, principalement dans `MainViewModel.kt` et `Charts.kt`.

## Flux d’état

1. Au lancement, le repository lit la ligne JSON ou crée `SeedData` si elle n’existe pas.
2. `StateMigrations.toLatest` transforme les anciens schémas reconnus.
3. Le ViewModel expose une copie de l’état via `StateFlow`.
4. Une action UI appelle une méthode du ViewModel.
5. `mutate` met immédiatement à jour le flux en mémoire, puis sérialise le nouvel `AppState`
   dans Room sous un mutex de sauvegarde.
6. Les tendances sont recalculées à la demande depuis l’état courant ; aucun agrégat n’est
   persisté.

Une seule séance `DRAFT` non supprimée est attendue. Lorsqu’elle est affichée et non minimisée,
`WorkoutScreen` remplace temporairement l’interface principale. Le bouton retour Android passe
par le dialogue de sortie de cet écran.

Sur le Web, le même invariant est appliqué par `activeDraft`. Chaque mutation remplace
immédiatement l’état en mémoire et met en file une sauvegarde du snapshot complet dans
IndexedDB. Les préférences d’apparence restent dans `localStorage`; elles ne traversent pas la
sauvegarde JSON. Le service worker ne met en cache que le code et les ressources, jamais les
données personnelles.

## Choix techniques actuels

- JDK 17, Gradle 9.5, Android Gradle Plugin 9.3.0 et Kotlin 2.3.21 ;
- `compileSdk`/`targetSdk` 36, `minSdk` 26 ;
- Compose BOM 2026.06.00, Lifecycle, coroutines, kotlinx.serialization et Room ;
- Material 3 avec cinq identités claires/sombres, couleurs étendues sémantiques et contrôles
  tactiles adaptés au téléphone ;
- aucune permission Internet dans le manifeste ;
- `android:allowBackup="false"` : la portabilité repose sur la sauvegarde JSON explicite.
- PWA sans étape de build, installable en mode `standalone`, avec chemins relatifs compatibles
  avec le sous-répertoire GitHub Pages `/repere/`.

Les versions exactes déclarées dans `app/build.gradle.kts` et le Gradle Wrapper restent les
sources de vérité.

Les préférences de thème sont stockées séparément dans Preferences DataStore par
`AppearanceRepository`. Elles ne traversent pas `MainViewModel` et ne font pas partie de
`AppState`, des sauvegardes ou des exports. Les polices Fredoka et Nunito sont embarquées avec
leurs licences OFL afin de rester disponibles hors ligne.

## Points d’attention

- `MainViewModel.kt` centralise beaucoup de responsabilités : vérifier l’impact transversal
  d’une modification et couvrir les règles extraites par des tests.
- Les écritures remplacent l’état JSON complet. Une évolution vers un stockage relationnel
  demanderait une migration explicite et testée des données existantes.
- Les identifiants relient l’historique aux éléments actuels, tandis que les snapshots gardent
  les libellés et objectifs saisis à l’époque. Choisir consciemment entre valeur actuelle et
  snapshot dans toute nouvelle statistique.
- Les sélecteurs de fichiers Android sont gérés dans `MainActivity`; ils n’exigent pas de
  permission globale de stockage.
- Android et Web partagent la forme de `AppState`, pas une bibliothèque de code. Toute règle
  métier commune modifiée dans un client doit donc être vérifiée dans l’autre et documentée si
  leur comportement diverge volontairement.
- IndexedDB et le stockage Safari ne constituent pas une sauvegarde. La restauration JSON est
  le seul mécanisme actuel de transfert entre appareils.
