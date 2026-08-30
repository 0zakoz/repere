# Produit et comportements actuels

## Finalité

Repère est un carnet de musculation personnel conçu pour réduire la saisie pendant une séance,
conserver un historique exploitable et rendre visibles les tendances sans score opaque ni
recommandation automatique. L’application fonctionne hors ligne, pour un seul utilisateur et
uniquement en kilogrammes.

Au premier lancement, elle crée un programme **Full body** actif qui alterne les séances A et
B les lundi, mercredi et vendredi. Les exercices et associations musculaires fournis sont
définis dans `SeedData.kt` ; ils restent entièrement modifiables depuis la Bibliothèque.

## Navigation principale

### Journal

- Affiche la séance suggérée par le programme actif et l’historique antéchronologique.
- Signale les créneaux indicatifs manqués et propose de reprendre le cycle ou de les sauter.
- Permet de démarrer la séance attendue, une autre séance comme remplacement, ou une séance
  hors programme qui ne fait pas avancer le cycle.
- Une seule séance brouillon peut exister. Elle est sauvegardée après chaque modification,
  peut être mise en pause, reprise ou abandonnée.
- L’historique affiche la date, l’heure de départ, la durée, le nombre de séries, le programme
  et la note. Une séance terminée peut être rééditée ou supprimée avec une courte possibilité
  d’annulation.
- Sans programme actif ou sans modèle adapté, des raccourcis ouvrent directement la bonne
  section de la Bibliothèque.

### Saisie d’une séance

- Le modèle fournit l’ordre des exercices, le nombre prévu de séries et la plage de
  répétitions.
- Charge et répétitions sont préremplies, par rang de série, depuis la dernière occurrence
  terminée où le même exercice contient au moins une série réellement validée. Une séance où
  l’exercice est resté vide ne masque donc pas la performance précédente. Ces valeurs ne sont
  pas considérées comme réalisées.
- Une série n’est validable que si elle contient une charge numérique positive ou nulle et un
  nombre entier de répétitions strictement positif. Le RIR est facultatif, de 0 à 3.
- Les boutons `−1 rep` et `+1 rep` ajustent rapidement les répétitions entre 1 et 999 ; `+1`
  part de 1 lorsque le champ est vide.
- Modifier une série déjà validée l’invalide ; le bouton de validation la confirme et masque
  le clavier. La série suivante du même exercice déclenche un chronomètre de repos flottant.
- Le repos peut être enregistré avec **Série suivante**, ignoré, ou enregistré automatiquement
  lors de la saisie de la série cible. Ce dernier chemin retranche actuellement une constante
  de 40 secondes destinée au temps d’exécution estimé.
- Séries et exercices peuvent être ajoutés ou retirés. Un nouvel exercice peut être créé
  directement pendant la séance et rejoint alors aussi la Bibliothèque.
- Terminer exige au moins une série valide. Les exercices et séries prévus restent dans le log,
  même vides, afin de conserver le dénominateur prévu des tendances.
- La date et la note sont modifiables. L’heure initiale de départ reste le critère secondaire
  d’ordre lorsque plusieurs séances partagent la même date.

### Poids

- Une mesure peut être enregistrée pour n’importe quel jour jusqu’à aujourd’hui ; les jours
  manquants ne sont pas un problème.
- Une date vide est préremplie avec la mesure antérieure la plus récente.
- La saisie accepte virgule ou point, est normalisée à 0,1 kg et doit être comprise entre
  0,1 et 500 kg.
- Des boutons ajustent la valeur de ±1 kg, ±0,5 kg ou ±0,1 kg.
- Le graphique juxtapose les mesures brutes et la moyenne glissante sur les sept derniers
  jours disponibles, avec filtres 4, 12, 52 semaines ou Tout.
- Une mesure existante peut être modifiée, supprimée ou exportée en CSV.

### Tendances

Toutes les vues ignorent les brouillons et les séances supprimées, avec des filtres 4, 12,
52 semaines ou Tout.

- **Exercices** : graphiques séparés pour charge et répétitions, une couleur par rang de série,
  plage cible historique, axes ajustés aux valeurs observées et inspection tactile d’une date.
  Les dernières séries rappellent charge, répétitions, RIR et repos.
- **Séances** : synthèse moyenne par modèle avec durée, séries réalisées/prévues, taux de
  réalisation, RIR, repos et séries moyennes par exercice, puis détail de chaque séance.
- **Muscles** : séries pondérées par semaine et sur la période, répétitions et RIR moyens,
  barres horizontales et carte musculaire avant/dos. Les poids sont principal ×1,
  secondaire ×0,5 et tertiaire ×0,25.

Les tendances musculaires appliquent volontairement les affectations musculaires actuelles
de l’exercice à son historique. Modifier une affectation recalcule donc le passé ; si
l’exercice n’existe plus dans la Bibliothèque, son snapshot historique sert de repli.

### Bibliothèque

- **Programmes** : nom, cycle ordonné de séances, jours indicatifs, activation unique,
  archivage/réactivation et suppression lorsque le programme n’est pas utilisé dans un log
  terminé.
- **Séances** : modèles nommés contenant une liste ordonnée d’exercices, un nombre de séries et
  éventuellement une plage de répétitions propre au modèle.
- **Exercices** : nom, plage de répétitions, consigne permanente et muscles principaux,
  secondaires ou tertiaires.
- **Muscles** : groupes libres, dont les fléchisseurs et extenseurs de l’avant-bras séparés.

Un élément utilisé dans une séance terminée est archivable mais pas supprimable définitivement.
La suppression d’un élément jamais utilisé met à jour ses références : exercices retirés des
modèles, modèles retirés des cycles, muscles retirés des exercices. Un modèle devenu vide peut
disparaître avec la suppression de son dernier exercice.

### Réglages et apparence

Les réglages proposent cinq thèmes : **Original**, **Kawaii**, **Pastel**, **OLED** et
**Épuré**. Chacun possède une variante claire et sombre ; le mode **Système** suit le réglage
Android. Une galerie donne un aperçu des palettes et applique immédiatement le choix, qui est
conservé localement sur le téléphone.

Les thèmes adaptent les couleurs, formes, typographies, graphiques, cartes musculaires et
barres système sans changer les parcours. Kawaii ajoute des détails décoratifs discrets et des
polices rondes embarquées ; OLED utilise un fond réellement noir en mode sombre ; Épuré reste
minimal avec des accents jaune et bleu pastel.

La couleur primaire signale les actions principales et la navigation ; la couleur secondaire
porte les réussites, validations et états actifs afin que les deux accents restent visibles. Une
série validée conserve un fond neutre et se distingue par un contour coloré, sans grand aplat.

### Données et fichiers

L’icône de réglages du Journal ouvre les exports CSV des performances et des pesées, un export
Markdown complet destiné à ChatGPT, la sauvegarde JSON complète et la restauration. Le Markdown
regroupe programmes, bibliothèque, dernières performances, historique visible, brouillon,
événements et pesées en distinguant explicitement les séries non réalisées. La restauration
demande confirmation et remplace l’état local uniquement après décodage et migration réussis.

## Limites actuelles

- aucun compte, cloud, synchronisation, permission Internet, télémétrie ou notification ;
- aucune unité autre que le kilogramme ;
- un seul utilisateur, un seul programme actif et un seul brouillon ;
- pas de supersets, dropsets, objectifs de repos, calcul de 1RM ou recommandations de charge ;
- pas d’import CSV ;
- pas de tests instrumentés ou Compose UI dans le dépôt à ce jour ; les parcours sur téléphone
  sont validés manuellement lorsque nécessaire.

Ces limites décrivent la version 1.6.1. Elles peuvent évoluer si une nouvelle demande le
justifie.
