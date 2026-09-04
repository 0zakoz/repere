# Repère

Repère est un carnet personnel de musculation, de poids et de nutrition. Le dépôt contient
deux clients hors ligne partageant le même modèle de sauvegarde :

- l’application Android **Suivi Muscu**, version **1.10.1** (`versionCode 15`) ;
- la PWA **Repère Web**, version **1.11.1**, destinée notamment à l’iPhone.

Les deux versions sont en français, mono-utilisateur et sans compte. Les données restent sur
l’appareil ; il n’existe ni serveur applicatif, ni télémétrie, ni synchronisation automatique.
Le dépôt source est public, mais il ne contient aucune donnée personnelle : les historiques
réels restent exclusivement dans les stockages locaux et les sauvegardes choisies par
l’utilisateur.

## Installer sur iPhone

La PWA est publiée à l’adresse **https://0zakoz.github.io/repere/**. Dans Safari sur l’iPhone :

1. ouvrir l’adresse une première fois avec une connexion Internet ;
2. toucher **Partager**, puis **Sur l’écran d’accueil** ;
3. activer **Ouvrir comme app web**, puis toucher **Ajouter** ;
4. lancer ensuite Repère depuis son icône, comme une application classique.

Après le premier chargement, l’application et les données courantes fonctionnent hors ligne.
Il est prudent de créer régulièrement une **Sauvegarde complète JSON** depuis les réglages et
de la conserver dans Fichiers/iCloud. Supprimer les données de Safari ou la PWA peut supprimer
la base locale.

## Fonctionnalités

- bibliothèque modifiable de programmes, modèles de séance, exercices et muscles ;
- bibliothèque recherchable par nom sur Android ;
- programme actif, cycle A/B, jours indicatifs et gestion des créneaux manqués ;
- séance autosauvegardée, préremplie depuis la dernière performance réelle de chaque exercice ;
- sélection recherchable dans toute la bibliothèque et ordre des exercices modifiable ;
- charges, répétitions, RIR, séries prévues/réalisées et chronomètre de repos flottant ;
- historique éditable avec date, heure, durée, temps de repos et note ;
- tendances par exercice, séance et muscle ;
- volume musculaire principal ×1, secondaire ×0,5 et tertiaire ×0,25 ;
- poids, objectif affiché sur le graphique, moyenne mobile sur sept jours et graphique ;
- apports progressifs de calories/protéines, objectifs quotidiens avec restes, et courbes quotidiennes ;
- thèmes Original, Kawaii, Pastel, OLED et Épuré, en clair, sombre ou système ;
- exports CSV, export Markdown complet pour ChatGPT et sauvegarde/restauration JSON.

Une description détaillée des parcours se trouve dans [docs/PRODUCT.md](docs/PRODUCT.md).

## Données entre Android et iPhone

Android et la PWA possèdent chacun leur base locale : une saisie faite sur un téléphone
n’apparaît pas automatiquement sur l’autre. Pour transférer ou recopier l’état complet,
exporter une **Sauvegarde complète JSON** sur l’appareil source puis la restaurer sur l’autre.
La restauration remplace l’état du client cible après confirmation. Les préférences de thème
restent propres à chaque appareil ; les objectifs nutritionnels et de poids font partie de
la sauvegarde et traversent les appareils.

La structure et les règles de compatibilité sont décrites dans [docs/DATA.md](docs/DATA.md).

## Développer

### Android

Le projet utilise JDK 17, Gradle Wrapper et le SDK Android local sous `.tooling/` :

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\build.ps1
```

Avec un téléphone autorisé en débogage USB, `install.ps1` utilise `adb install -r` pour mettre
à jour l’application sans effacer ses données. Les clés et fichiers privés restent exclus de
Git.

### Web

La PWA est statique, sans dépendance npm ni compilation :

```powershell
cd web
npm test
npm run check
npm run serve
```

Le serveur local répond sur `http://127.0.0.1:4173/`. Un workflow GitHub Actions teste puis
publie le dossier `web/` sur GitHub Pages à chaque modification de la branche `main`.

Le guide complet est [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) et l’architecture est décrite
dans [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md). `AGENTS.md` est le point d’entrée des agents
travaillant dans ce dépôt ; la documentation décrit l’état présent sans figer les évolutions.
Les règles de sécurité du dépôt public sont détaillées dans
[docs/SECURITY.md](docs/SECURITY.md).
