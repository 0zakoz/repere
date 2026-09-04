# Produit et comportements actuels

## Finalité

Repère est un carnet de musculation personnel conçu pour réduire la saisie pendant une séance,
conserver un historique exploitable et rendre visibles les tendances sans score opaque ni
recommandation automatique. L’application fonctionne hors ligne, pour un seul utilisateur et
uniquement en kilogrammes.

Le produit existe sous deux formes : l’application Android **Suivi Muscu** et la PWA
**Repère Web**, installable depuis Safari sur iPhone. Elles suivent les mêmes règles métier et
le même schéma de sauvegarde, mais conservent chacune leurs données localement et ne se
synchronisent pas automatiquement.

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
- Le repos enregistré avant chaque série peut être corrigé ou effacé manuellement. La durée
  totale de la séance est elle aussi modifiable, pendant la saisie ou en rééditant l’historique.
- Séries et exercices peuvent être ajoutés ou retirés. Le sélecteur d’exercice consulte toute
  la Bibliothèque active, avec recherche et défilement, et masque seulement les exercices déjà
  présents dans la séance. Un nouvel exercice peut être créé directement pendant la séance et
  rejoint alors aussi la Bibliothèque.
- Les exercices du brouillon peuvent être déplacés vers le haut ou le bas ; cet ordre est
  conservé dans le log et ses exports.
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
- Un poids objectif peut être saisi une seule fois : il apparaît en pointillés sur le
  graphique avec l’écart à la dernière mesure. Le laisser vide le retire.
- Une mesure existante peut être modifiée, supprimée ou exportée en CSV.

### Nutrition

- Les calories et protéines sont saisies sous forme d’apports successifs : petit-déjeuner,
  repas, collation ou toute autre portion peut être ajoutée au fil de la journée.
- Une journée peut contenir autant d’entrées que nécessaire. Chaque entrée conserve son heure,
  ses calories et ses grammes de protéines ; elle peut être modifiée ou supprimée séparément.
- L’en-tête additionne immédiatement toutes les entrées de la date sélectionnée.
- Des objectifs caloriques et protéiques peuvent être saisis une seule fois : l’en-tête
  affiche alors les calories restantes et les protéines manquantes du jour, avec mention
  explicite du dépassement ou de l’atteinte. Laisser un objectif vide le retire. Ces objectifs
  font partie de la sauvegarde JSON et traversent donc les appareils, comme le reste des
  données ; seul le thème reste propre à chaque appareil.
- Deux courbes séparées affichent les totaux quotidiens de calories et de protéines sur 4, 12,
  52 semaines ou tout l’historique. Les jours sans saisie restent simplement absents.
- L’historique brut peut être exporté en CSV ; la sauvegarde JSON et l’export Markdown complet
  incluent également toutes les entrées nutritionnelles.

### Tendances

Toutes les vues ignorent les brouillons et les séances supprimées, avec des filtres 4, 12,
52 semaines ou Tout.

- **Exercices** : graphiques séparés pour charge et répétitions, une couleur par rang de série,
  plage cible historique, axes ajustés aux valeurs observées et inspection tactile d’une date.
  Les dernières séries rappellent charge, répétitions, RIR et repos.
- **Séances** : synthèse moyenne par modèle avec durée, séries réalisées/prévues, taux de
  réalisation, RIR, repos et séries moyennes par exercice, puis détail de chaque séance.
- **Muscles** : séries pondérées par semaine et sur la période, répétitions et RIR moyens,
  barres horizontales et carte musculaire tactile avec vues avant et dos côte à côte. Toucher une zone affiche son volume pondéré, ses répétitions et son
  RIR moyens. Les poids sont principal ×1, secondaire ×0,5 et tertiaire ×0,25.

Les tendances musculaires appliquent volontairement les affectations musculaires actuelles
de l’exercice à son historique. Modifier une affectation recalcule donc le passé ; si
l’exercice n’existe plus dans la Bibliothèque, son snapshot historique sert de repli.

### Bibliothèque

- **Programmes** : nom, cycle ordonné de séances, jours indicatifs, activation unique,
  archivage/réactivation et suppression lorsque le programme n’est pas utilisé dans un log
  terminé.
- **Séances** : modèles nommés contenant une liste ordonnée et réorganisable d’exercices, un
  nombre de séries et éventuellement une plage de répétitions propre au modèle.
- **Exercices** : nom, plage de répétitions, consigne permanente et muscles principaux,
  secondaires ou tertiaires.
- **Muscles** : groupes libres, dont les fléchisseurs et extenseurs de l’avant-bras séparés.
- Sur Android, un champ de recherche filtre les quatre listes par nom.

Un élément utilisé dans une séance terminée est archivable mais pas supprimable définitivement.
La suppression d’un élément jamais utilisé met à jour ses références : exercices retirés des
modèles, modèles retirés des cycles, muscles retirés des exercices. Un modèle devenu vide peut
disparaître avec la suppression de son dernier exercice.

### Réglages et apparence

Les réglages proposent cinq thèmes : **Original**, **Kawaii**, **Pastel**, **OLED** et
**Épuré**. Chacun possède une variante claire et sombre ; le mode **Système** suit le réglage
Android ou iOS. Une galerie donne un aperçu des palettes et applique immédiatement le choix, qui est
conservé localement sur le téléphone.

Les thèmes adaptent les couleurs, formes, typographies, graphiques, cartes musculaires et
barres système sans changer les parcours. Kawaii mélange le rose pâle dominant, le jaune pâle
et le bleu pâle sur un fond pastel dégradé, avec quelques surfaces blanc nacré en clair ou noir
prune en sombre. Il n’utilise plus de vert sauge et son onglet actif est signalé en rose. Ses formes sont très arrondies et ses en-têtes,
navigation et cartes utilisent des cœurs, étoiles, lapins, pandas et un pictogramme de chat
siamois. Les décorations restent sans fonction et ne remplacent pas les libellés accessibles.
OLED utilise un fond réellement noir en mode sombre ; Épuré reste minimal avec des accents
jaune et bleu pastel.

La couleur primaire signale les actions principales et la navigation ; la couleur secondaire
porte les réussites, validations et états actifs afin que les deux accents restent visibles. Une
série validée conserve un fond neutre et se distingue par un contour coloré, sans grand aplat.
La prochaine séance est présentée dans une carte plate au fond neutre, avec un contour discret :
elle reste mise en avant sans halo opaque, en mode clair comme en mode sombre.

### Données et fichiers

L’icône de réglages du Journal ouvre les exports CSV des performances, des pesées et de la nutrition, un export
Markdown complet destiné à ChatGPT, la sauvegarde JSON complète et la restauration. Le Markdown
regroupe programmes, bibliothèque, dernières performances, historique visible, brouillon,
événements, pesées et apports nutritionnels en distinguant explicitement les séries non réalisées. La restauration
demande confirmation et remplace l’état local uniquement après décodage et migration réussis.

## Limites actuelles

- aucun compte, cloud, synchronisation, permission Internet, télémétrie ou notification ;
- aucune unité autre que le kilogramme ;
- un seul utilisateur, un seul programme actif et un seul brouillon ;
- pas de supersets, dropsets, objectifs de repos, calcul de 1RM ou recommandations de charge ;
- pas d’import CSV ;
- pas de synchronisation automatique entre Android et Web : le transfert passe par la
  sauvegarde JSON complète ;
- pas de tests instrumentés ou Compose UI dans le dépôt à ce jour ; les parcours sur téléphone
  sont validés manuellement lorsque nécessaire.

Ces limites décrivent la version 1.10.0. Elles peuvent évoluer si une nouvelle demande le
justifie.
