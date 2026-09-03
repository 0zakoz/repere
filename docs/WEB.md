# Version Web et installation iPhone

## Positionnement

`web/` contient une Progressive Web App statique destinée à fournir Repère sur iPhone sans
passer par l’App Store. GitHub Pages ne sert que les fichiers HTML, CSS, JavaScript, polices et
icônes. Aucune donnée sportive n’est envoyée au dépôt ou à GitHub : la base reste dans
IndexedDB sur l’appareil.

Le dépôt, le site et tout le code JavaScript livré sont publics. Aucun mot de passe, token,
secret applicatif ou jeu de données personnel ne doit donc être placé dans `web/`, injecté au
build ou publié comme ressource statique.

La PWA reprend les cinq onglets Android, les règles de programme et de préremplissage, le suivi
poids/nutrition, les tendances, les cinq thèmes et les mêmes formats d’export. La sauvegarde
JSON est conçue pour passer d’Android au Web et inversement. Il n’existe pas de synchronisation
automatique entre les deux installations.

La saisie de séance reprend également le sélecteur complet et recherchable de la Bibliothèque,
le réordonnancement des exercices et la correction manuelle de la durée totale ou du repos de
chaque série.

## Installation sur iPhone

1. Ouvrir `https://0zakoz.github.io/repere/` dans Safari avec une connexion Internet.
2. Toucher **Partager**, puis **Sur l’écran d’accueil**.
3. Activer **Ouvrir comme app web** et confirmer avec **Ajouter**.
4. Ouvrir Repère depuis l’icône créée. Les visites suivantes peuvent fonctionner hors ligne.

L’installation n’est pas un paquet IPA : c’est Safari qui crée une application Web autonome.
La première ouverture et les mises à jour nécessitent Internet. Une nouvelle version publiée
est récupérée par le service worker ; elle peut demander une fermeture/réouverture de la PWA.

## Données et précautions

- IndexedDB stocke un `AppState` complet dans la base `repere-pwa`.
- Le navigateur reçoit une demande de stockage persistant, sans garantie absolue du système.
- Effacer les données de site Safari ou supprimer le stockage de la PWA peut supprimer les
  données locales.
- Créer régulièrement une sauvegarde JSON dans Fichiers ou iCloud reste indispensable.
- La restauration remplace toute la base locale après confirmation ; les thèmes restent
  propres à l’appareil.
- Le thème par défaut sur le Web est **Kawaii** (sombre) ; un choix explicite reste conservé
  dans `localStorage` et n’est jamais écrasé par une mise à jour.
- Sur iPhone, les exports CSV/Markdown et la sauvegarde JSON passent par la feuille de
  partage du système quand le téléchargement direct est indisponible en PWA autonome.
- Toute erreur JavaScript non gérée affiche un bandeau rouge persistant en haut de l’écran
  (fermable) afin de permettre un diagnostic depuis le seul téléphone.

## Architecture du dossier

- `index.html`, `manifest.webmanifest`, `sw.js` : coquille installable et cache hors ligne ;
- `js/state.js`, `seed.js`, `store.js` : schéma, migration, seed et IndexedDB ;
- `js/rules.js` : règles métier pures ;
- `js/app.js`, `ui.js`, `charts.js`, `styles.css` : interface mobile et thèmes ;
- `js/exporters.js` : CSV, Markdown et sauvegarde côté interface ;
- `tests/` : tests Node du métier et de la configuration PWA ;
- `scripts/serve.mjs` : serveur statique local sans dépendance.

## Publication

`.github/workflows/pages.yml` exécute `npm test` et `npm run check`, envoie `web/` comme
artefact Pages puis le publie. Le dépôt doit avoir **Settings → Pages → Source : GitHub
Actions**. Le workflow peut aussi être lancé manuellement depuis l’onglet Actions.
