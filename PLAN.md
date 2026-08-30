# Cahier des charges — Suivi Muscu v1

## 1. Objectif et principes

Créer dans `D:\0. Scripts\suivi_muscu` une application Android native nommée **Suivi Muscu**, installable localement par APK sur le Realme GT6.

Critères de réussite :

- démarrer la prochaine séance en deux gestes ;
- préremplir les charges et répétitions de la dernière occurrence de chaque exercice ;
- enregistrer une série avec très peu de saisie ;
- ne jamais perdre une séance en cours après fermeture de l’application ;
- fournir des tendances fondées sur les données brutes, sans score opaque ni recommandation automatique ;
- fonctionner entièrement hors ligne, sans compte, serveur, publicité, télémétrie ou permission Internet ;
- permettre une sauvegarde complète, une restauration et un export CSV exploitable dans Excel ou par une IA.

## 2. Parcours et interface

### Navigation

Barre inférieure à trois sections :

- **Journal** : prochaine séance, brouillon actif, bouton Nouvelle séance, historique et export CSV.
- **Tendances** : vues Séances, Exercices et Muscles.
- **Bibliothèque** : Programmes, Séances, Exercices et Muscles.
- Les réglages et la sauvegarde/restauration seront accessibles par une icône dédiée.

### Programmes et planification

- Un programme contient un nom, un cycle ordonné de séances et des jours d’entraînement indicatifs.
- Un seul programme est actif ; les autres sont archivables et réactivables.
- Programme initial : **Full body**, cycle `A → B`, jours lundi/mercredi/vendredi, prochaine séance initiale A.
- Le cycle avance lors d’une séance terminée ou explicitement sautée.
- Si plusieurs jours sont manqués, une seule question propose :
  - reprendre la séance attendue ;
  - sauter tous les créneaux manqués et avancer le cycle.
- Si l’utilisateur démarre une autre séance que celle attendue, demander si elle est :
  - hors programme, sans modifier le cycle ;
  - un remplacement, qui repositionne le cycle après la séance choisie.
- Modifier l’ordre d’un programme actif demandera de choisir explicitement la prochaine séance.
- Les jours restent indicatifs : aucune notification ni rappel système en v1.

### Enregistrement d’une séance

- Une seule séance brouillon peut être active.
- Date et heures de début/fin préremplies mais modifiables.
- Sauvegarde locale instantanée après chaque changement et reprise automatique du brouillon.
- Exercices créés selon le modèle, avec le nombre de séries prévu par celui-ci.
- Pour chaque exercice, reprendre la dernière occurrence globale :
  - charge et répétitions préremplies ;
  - RIR et repos laissés vides ;
  - nombre de séries dicté par le modèle, pas par la séance précédente.
- Une valeur préremplie reste un objectif non réalisé jusqu’à validation de la série.
- Modifier une valeur valide automatiquement la série ; un bouton permet aussi de la valider sans changement.
- Saisie :
  - répétitions entières positives ;
  - charge en kg, jusqu’à deux décimales, clavier numérique et boutons `−0,5 / +0,5` ;
  - RIR facultatif, entier de 0 à 3 ;
  - couleur neutre indiquant si les reps sont sous, dans ou au-dessus de la plage, sans conseil de charge.
- Ajouter/supprimer une série ou un exercice pendant la séance.
- Un exercice ponctuel est ajouté uniquement au log, avec une action séparée pour l’ajouter au modèle ; il commence avec une série vide.
- Terminer une séance conserve uniquement les séries validées ; les éléments préremplis mais non réalisés sont écartés.
- Annuler un brouillon ou retirer un exercice contenant des séries validées demande confirmation.
- L’écran suit le délai de veille normal du téléphone.

### Chronomètre de repos

- Après validation d’une série, démarrer un chronomètre montant si une série suivante existe pour le même exercice.
- Juste avant l’effort suivant, le bouton **Commencer la série** arrête le chrono et enregistre le repos avant cette série.
- Le chrono reste exact après verrouillage ou passage en arrière-plan grâce aux horodatages.
- Une mesure oubliée peut être marquée « Ignorer ».
- Aucun compte à rebours, objectif, vibration ou notification.
- Pas de gestion spéciale des supersets, dropsets ou exercices entrelacés en v1.

### Historique et bibliothèque

- Historique antéchronologique : séance, programme éventuel, date, durée et résumé.
- Une séance terminée est entièrement éditable ; toute modification recalcule les tendances.
- Suppression : confirmation, puis action **Annuler** temporaire avant purge définitive.
- Exercices, séances, programmes et muscles déjà utilisés sont archivés plutôt que supprimés.
  Règle v1.4.1 : tout élément de bibliothèque **non utilisé dans l'historique** (aucune séance
  terminée) est aussi **supprimable définitivement**, avec cascades confirmées : un exercice est
  retiré des modèles qui le référencent (modèle vide supprimé), un modèle est retiré du cycle des
  programmes, un muscle est retiré des associations d'exercices, un programme supprime ses
  événements de cycle. Les brouillons actifs ne comptent pas comme historique (leurs instantanés
  restent insensibles à une suppression).
- Renommer ou modifier un élément ne réécrit pas rétroactivement les séances passées.
- Note libre facultative par séance et consigne permanente par exercice.

### Direction visuelle

- Material Design 3 sombre, gros contrôles tactiles et interface française.
- Couleurs principales : fond `#0F1115`, surfaces `#191C22`, texte `#F2F5F0`, accent lime `#B7F34A`.
- Icône vectorielle adaptative : symbole simple haltère/validation lime sur fond sombre.
- Contrôles d’au moins 48 dp et prise en charge correcte des tailles de police Android.

## 3. Données, analyses et fichiers

### Modèle métier

Types principaux :

- `MuscleGroup` : nom unique, état actif/archivé.
- `Exercise` : nom, plage de reps par défaut, consigne, état, muscles principaux/secondaires.
- `WorkoutTemplate` : nom et liste ordonnée d’exercices avec séries cibles et éventuelle surcharge de plage de reps.
- `Program` : nom, cycle de modèles, jours indicatifs, état actif/archivé.
- `ProgramEvent` : séance complétée ou créneau sauté, afin de déterminer la prochaine séance.
- `WorkoutLog` : brouillon/terminé, date, heures, note et rattachement éventuel au programme.
- `WorkoutExerciseLog` : ordre et instantané du nom, de la plage cible et des muscles.
- `SetLog` : rang, charge, reps, RIR facultatif, repos facultatif et état validé.

Les logs gardent des instantanés historiques, tout en conservant les identifiants stables nécessaires pour regrouper un exercice renommé dans ses tendances.

### Tendances

Filtres rapides communs : **4, 12, 52 semaines et Tout**.

- **Exercice** :
  - deux graphiques synchronisés : charge puis répétitions ;
  - une courbe par rang de série ;
  - plage de reps historique en arrière-plan ;
  - toucher d’une date pour afficher charge, reps, RIR, repos et note de séance.
- **Séance A/B** :
  - durée ;
  - séries réalisées/prévues ;
  - RIR moyen en ignorant les valeurs vides ;
  - repos moyen en ignorant les valeurs absentes ou invalidées.
- **Muscles** :
  - séries pondérées par semaine, du lundi au dimanche ;
  - muscle principal `×1`, secondaire `×0,5` pour chaque série validée ;
  - affichage par groupe et comparaison avec les semaines précédentes ;
  - pas de total anatomique global entre groupes qui se recouvrent, notamment Pectoraux/Haut des pectoraux.
- Les brouillons, séances supprimées et créneaux sautés sont exclus des statistiques.
- Pas de tonnage, force estimée, seuil de volume recommandé ou instruction de progression dans les vues principales de v1.
- Graphiques dessinés directement avec Compose Canvas pour éviter une dépendance graphique externe.

### Export CSV

Un fichier UTF-8 compatible RFC 4180, une ligne par série terminée, avec au minimum :

`export_version, session_id, session_date, started_at, ended_at, duration_seconds, program_name, workout_name, session_note, exercise_order, exercise_id, exercise_name, target_rep_min, target_rep_max, primary_muscles, secondary_muscles, set_number, weight_kg, reps, rir, rest_before_seconds`

- Dates ISO 8601, séparateur virgule, point décimal.
- Muscles multiples séparés par `|` dans leur cellule.
- Champs correctement échappés pour les accents, virgules et retours à la ligne.
- Export de tout l’historique terminé ; pas d’import CSV en v1.

### Sauvegarde et restauration

- Sauvegarde JSON versionnée contenant bibliothèque, programmes, événements de cycle, réglages et historique complet.
- Restauration atomique : validation intégrale du fichier avant remplacement des données locales.
- Si l’application contient déjà des données, confirmation explicite avant remplacement.
- Un fichier invalide ou d’une version incompatible ne modifie rien.
- Fichiers choisis via le sélecteur Android, qui ne nécessite pas de permission générale de stockage. [Documentation Android sur le Storage Access Framework](https://developer.android.com/training/data-storage/shared/documents-files)
- CSV et sauvegardes sont lisibles et non chiffrés ; la base interne reste protégée par le bac à sable Android.

## 4. Initialisation fournie

### Groupes musculaires

Pectoraux, Haut des pectoraux, Dorsaux, Trapèzes, Lombaires, Deltoïdes antérieurs, Deltoïdes latéraux, Deltoïdes postérieurs, Biceps, Triceps, Avant-bras, Quadriceps, Ischio-jambiers, Fessiers, Adducteurs, Mollets et Abdominaux.

La liste est modifiable : ajout, renommage et archivage.

### Séances

Chaque exercice commence avec deux séries.

- **A** : Press pecs 6–10, Tirage vertical 6–10, Extension triceps 6–12, Curl biceps 6–12, Élévations latérales 8–12, Extension avant-bras 8–15, Flexion poignets 8–15.
- **B** : Tirage horizontal 6–10, Press vertical 6–10, Back extension lesté 8–12, Hack squat 5–9, Mollets 8–15, Rear delt fly 8–12.

### Associations musculaires initiales

| Exercice | Principal | Secondaire |
|---|---|---|
| Press pecs | Pectoraux | Haut des pectoraux, deltoïdes antérieurs |
| Tirage vertical | Dorsaux | Avant-bras, trapèzes |
| Extension triceps | Triceps | — |
| Curl biceps | Biceps | Avant-bras |
| Élévations latérales | Deltoïdes latéraux | — |
| Extension avant-bras | Avant-bras | — |
| Flexion poignets | Avant-bras | — |
| Tirage horizontal | Trapèzes | Dorsaux, avant-bras, deltoïdes postérieurs |
| Press vertical | Deltoïdes antérieurs | Triceps, deltoïdes latéraux, haut des pectoraux |
| Back extension lesté | Lombaires | Fessiers, ischio-jambiers |
| Hack squat | Quadriceps | Fessiers, adducteurs |
| Mollets | Mollets | — |
| Rear delt fly | Deltoïdes postérieurs | — |

## 5. Implémentation et livraison

### Base technique

- Application mono-activité Kotlin, Jetpack Compose et flux d’état unidirectionnel.
- Architecture UI/ViewModel/repositories, Room comme source de vérité hors ligne et DataStore pour les préférences, conformément aux [recommandations d’architecture Android](https://developer.android.com/topic/architecture/recommendations).
- Socle figé au démarrage de l’implémentation :
  - JDK 17 ;
  - Gradle 9.5 et Android Gradle Plugin 9.3.0 ;
  - Kotlin 2.3.21 ;
  - `compileSdk/targetSdk 37`, `minSdk 26` ;
  - Compose BOM stable `2026.06.00` ;
  - Navigation 3 `1.1.5`, Lifecycle `2.11.0`, Room `2.8.4`, DataStore `1.2.1`.
- Ces versions sont compatibles avec le socle Android stable actuel : [AGP 9.3](https://developer.android.com/build/releases/agp-9-3-0-release-notes), [Compose BOM](https://developer.android.com/develop/ui/compose/bom), [Room](https://developer.android.com/jetpack/androidx/releases/room).
- Injection manuelle légère, sans framework Hilt, et aucun module réseau.
- Identifiant d’application : `fr.suivimuscu.app`, version initiale `1.0.0`.

### Ordre de réalisation

1. Installer localement l’outillage Android absent, créer le projet et la base Room versionnée.
2. Implémenter les bibliothèques, programmes, archivage et données initiales.
3. Construire le journal, le brouillon automatique, le préremplissage et le chronomètre.
4. Ajouter historique, édition, suppression réversible, CSV et sauvegarde/restauration.
5. Ajouter les agrégations, graphiques et finition visuelle.
6. Exécuter les tests, produire l’APK signé et vérifier l’installation/mise à jour.

### Livrables

- Code source complet dans le dossier partagé.
- Gradle Wrapper et instructions reproductibles de compilation.
- APK release signé.
- Clé de signature stable et configuration exclues du code versionné, à sauvegarder avec le projet pour permettre les mises à jour sans désinstaller l’application.
- Guide français : installation APK, autorisation de source inconnue, mise à jour, sauvegarde, restauration et export.
- Pour un futur durcissement mondial des installations hors store, documenter aussi l’installation par ADB ou le flux Android avancé ; le déploiement mondial de la vérification est annoncé à partir de 2027. [FAQ officielle Android](https://developer.android.com/developer-verification/guides/faq?hl=fr)

## 6. Tests et acceptation

- Création, modification, archivage et réactivation de chaque élément de bibliothèque.
- Cycle A/B sur trois jours, séance manquée, plusieurs jours manqués, séance hors cycle et remplacement.
- Préremplissage depuis la dernière occurrence globale, avec RIR/repr repos vides et nombre de séries issu du modèle.
- Fermeture forcée, verrouillage et reprise du brouillon sans perte.
- Validation d’une série inchangée, modification automatique, séance partielle et abandon confirmé.
- Chronomètre : démarrage, arrêt avant la série suivante, arrière-plan, mesure ignorée et ajout/suppression de séries.
- Édition d’un ancien log et recalcul immédiat de toutes les tendances.
- Exactitude des séries musculaires pondérées et des moyennes ignorant les valeurs absentes.
- Renommage ou modification d’un exercice sans altérer ses instantanés historiques.
- Suppression confirmée, annulation puis purge définitive.
- CSV : nombre de lignes égal au nombre de séries terminées et parsing correct des notes/accents/décimales.
- Sauvegarde/restauration aller-retour produisant des données identiques ; rejet atomique d’un fichier corrompu.
- Tests unitaires des règles métier et agrégations, tests Room/migrations, tests UI Compose des parcours critiques.
- Vérification visuelle sur petit écran Android et test final d’installation/mise à jour sur le Realme GT6 sans perte de données.

### Hypothèses figées pour v1

- Un seul utilisateur, une seule unité de poids (kg), une seule séance brouillon et un seul programme actif.
- Nouveau départ sans import de l’ancien Excel.
- Les charges représentent la résistance saisie ; pas de calcul spécial du poids du corps.
- Pas de cloud, compte, synchronisation, rappels, supersets, dropsets ou recommandations automatiques.
