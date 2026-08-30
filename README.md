# Repère

Repère est le dépôt de l’application Android personnelle actuellement affichée sous le nom
**Suivi Muscu** sur le téléphone. Elle sert à enregistrer rapidement les séances de
musculation, suivre les performances et le poids corporel, puis exporter les données pour
une analyse externe.

La version actuelle est la **1.5.0** (`versionCode 7`). L’application est en français,
mono-utilisateur et entièrement locale : elle ne demande aucun compte et ne déclare aucune
permission Internet.

## Fonctionnalités actuelles

- bibliothèque modifiable de programmes, séances, exercices et groupes musculaires ;
- programme actif avec cycle de séances et jours d’entraînement indicatifs ;
- séance en cours autosauvegardée, préremplie depuis la dernière performance connue ;
- saisie des charges, répétitions, RIR et temps de repos, avec ajout ou retrait de séries ;
- historique éditable avec date, heure, durée, note et suppression temporairement annulable ;
- tendances par exercice, séance et muscle sur 4, 12, 52 semaines ou tout l’historique ;
- volume musculaire pondéré : principal ×1, secondaire ×0,5, tertiaire ×0,25 ;
- suivi du poids avec saisie libre, moyenne glissante sur 7 jours et graphique ;
- exports CSV séparés pour les performances et les pesées ;
- export Markdown complet de tout le contexte pour une analyse dans ChatGPT ;
- sauvegarde et restauration JSON complètes via le sélecteur de fichiers Android.

Une description fonctionnelle détaillée se trouve dans [docs/PRODUCT.md](docs/PRODUCT.md).

## Données et sauvegarde

Les données vivent dans le stockage privé de l’application. Désinstaller l’application ou
effacer ses données Android les supprime du téléphone.

Avant une réinstallation ou un changement de téléphone, ouvrir les réglages depuis le
Journal, créer une **Sauvegarde complète**, puis copier le fichier JSON hors du téléphone.
Les CSV sont destinés à l’analyse ; seul le JSON permet une restauration complète.

La structure des données et les règles de compatibilité sont décrites dans
[docs/DATA.md](docs/DATA.md).

## Compiler et installer

Pré-requis : Windows PowerShell et l’outillage local conservé dans `.tooling/`. Le projet
utilise JDK 17, Gradle Wrapper et le SDK Android local sans modifier l’installation Java du
système.

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\build.ps1
```

Le script exécute les tests unitaires, le lint Android et la compilation release. L’APK est
produit dans `app/build/outputs/apk/release/app-release.apk`.

Avec un téléphone autorisé en débogage USB :

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\install.ps1
```

Le script utilise `adb install -r` afin de mettre à jour l’application sans effacer ses
données. Une mise à jour doit être signée avec la même clé que l’installation existante.
Les fichiers `release-private/`, `keystore.properties` et `local.properties` sont donc
conservés localement et exclus de Git.

Le guide complet de développement et de livraison se trouve dans
[docs/DEVELOPMENT.md](docs/DEVELOPMENT.md).

## Documentation du projet

Le sommaire destiné aux développeurs et aux agents est [docs/README.md](docs/README.md).
L’architecture du code est décrite dans [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

`AGENTS.md` est le point d’entrée des agents travaillant dans ce dépôt. La documentation
décrit l’état présent du projet ; elle n’interdit pas de le faire évoluer lorsque la demande
de l’utilisateur le justifie.
