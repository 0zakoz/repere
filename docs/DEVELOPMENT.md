# Développement, tests et livraison

## Environnement

Le projet cible Windows PowerShell et conserve un environnement isolé sous `.tooling/` :

- JDK 17 dans `.tooling/jdk` ;
- SDK Android et `adb` dans `.tooling/android-sdk` ;
- répertoire utilisateur Android local dans `.tooling/android-user` ;
- Gradle 9.5 fourni par le Wrapper.

`.tooling/` et `local.properties` ne sont pas versionnés. Sur une autre machine, il est aussi
possible d’utiliser un JDK 17 et un SDK Android standards, à condition que Gradle puisse trouver
le SDK.

La PWA sous `web/` demande seulement une version récente de Node.js pour les tests et le serveur
local. Elle n’a aucune dépendance npm et aucune étape de compilation.

## Commandes usuelles

Depuis la racine du dépôt, si `JAVA_HOME` et le SDK Android sont déjà configurés :

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
.\gradlew.bat assembleRelease
```

Le raccourci suivant exécute les trois étapes avec le JDK et le SDK de `.tooling/`. Le
contournement s’applique uniquement à ce processus PowerShell et ne modifie pas la stratégie
d’exécution du système :

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\build.ps1
```

L’APK résultant est `app/build/outputs/apk/release/app-release.apk`. Les rapports de tests et de
lint sont placés sous `app/build/reports/`.

Pour la version Web :

```powershell
cd web
npm test
npm run check
npm run serve
```

`npm test` couvre les règles métier pures, les migrations, exports et ressources PWA.
`npm run check` analyse la syntaxe des modules principaux. `npm run serve` expose le dossier sur
`http://127.0.0.1:4173/` pour une vérification mobile locale.

## Couverture automatisée actuelle

Les tests JVM sous `app/src/test` couvrent notamment :

- validité des séries, chronomètre et conservation des éléments planifiés ;
- réordonnancement des exercices et correction des durées de séance ou de repos ;
- chronologie de l’historique et agrégations de séances/muscles ;
- rôles musculaires et suppressions en cascade de la Bibliothèque ;
- saisie du poids, préremplissage, moyenne sur sept jours, objectif affiché sur le
  graphique et CSV ;
- saisie nutritionnelle cumulative, validation, agrégation quotidienne, objectifs et restes
  du jour, et CSV ;
- recherche textuelle de la Bibliothèque ;
- seed initial, migrations du schéma métier (jusqu’à v5 : objectifs et poids objectif) et
  export Markdown complet incluant les objectifs ;
- domaines et contraste des graphiques Canvas ;
- décodage des préférences d’apparence, lisibilité des dix palettes et distinction des séries
  de graphiques.

Les polices Fredoka et Nunito sous `app/src/main/res/font` proviennent du dépôt officiel Google
Fonts. Leurs licences OFL sont conservées sous `app/src/main/res/raw` et doivent rester livrées
avec les fichiers de police.

Il n’existe actuellement aucun test sous `app/src/androidTest`. Pour une modification visuelle
ou dépendante du système Android, compléter les tests unitaires par une vérification manuelle
sur appareil, ou ajouter des tests instrumentés si leur coût est justifié.

## Signature release

`app/build.gradle.kts` charge facultativement `keystore.properties`. Quand les quatre valeurs
sont présentes, `assembleRelease` signe l’APK ; sinon il produit une variante release non
signée utilisable pour certaines vérifications mais non installable comme mise à jour de l’app
existante.

Créer la configuration locale à partir de `keystore.properties.example` :

```properties
storeFile=release-private/suivi-muscu.jks
storePassword=...
keyAlias=...
keyPassword=...
```

La clé existante doit être sauvegardée séparément et ne doit jamais être publiée. Perdre ou
remplacer cette clé empêche Android d’installer une mise à jour par-dessus l’application déjà
présente.

## Installation sur le téléphone

Pré-requis : débogage USB activé, ordinateur autorisé par le téléphone et appareil visible avec
`adb devices`.

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\install.ps1
```

Le script appelle `adb install -r`, qui conserve normalement les données de l’application. Il
ne faut pas utiliser `adb uninstall`, `pm clear` ni désinstaller manuellement l’app pour résoudre
un problème d’installation sans accord explicite et sauvegarde préalable.

Pour une installation manuelle, transférer l’APK signé sur le téléphone, l’ouvrir avec
l’application Fichiers et autoriser temporairement cette source si Android le demande.

## Procédure de modification

1. Lire `AGENTS.md` et les documents liés à la zone concernée.
2. Vérifier l’état Git et inspecter le comportement existant avant de modifier.
3. Implémenter la demande au plus petit périmètre cohérent, sans empêcher une refactorisation
   utile.
4. Ajouter ou adapter les tests, puis mettre à jour la documentation concernée.
5. Exécuter au minimum les tests ciblés ; avant livraison, exécuter la chaîne complète.
6. Vérifier `git diff --check`, l’absence de secrets et les fichiers réellement inclus.
7. Installer sur le Realme GT6 seulement lorsqu’un test appareil apporte une vraie couverture
   supplémentaire et que l’utilisateur l’a rendu disponible.

Le dépôt étant public, vérifier également l’identité Git `noreply`, puis examiner
`git diff --cached` avant tout push. Les jeux de données de test doivent être synthétiques. Ne
jamais utiliser une sauvegarde réelle, même temporairement : supprimer le fichier du dernier
commit ne l’efface pas de l’historique Git.

Pour une modification Web, ajouter aux étapes 4 à 6 les tests Node, une vérification à largeur
iPhone et une nouvelle clé de cache dans `sw.js` à chaque livraison : le service worker ne
met à jour les fichiers statiques (dont `js/app.js`) que si `sw.js` lui-même change, sinon
les appareils conservent indéfiniment l’ancienne version en cache. Ne jamais tester la restauration
avec les seules données réelles d’un utilisateur : employer une sauvegarde de test.

## Publication Web

Le workflow `.github/workflows/pages.yml` est déclenché par les changements de `web/` sur
`main` ou manuellement. Il utilise Node.js 24, exécute les tests et contrôles de syntaxe, puis
publie directement `web/` avec GitHub Pages. Le dépôt GitHub doit avoir sa source Pages réglée
sur **GitHub Actions**.

Après le push, vérifier le succès du workflow et ouvrir
`https://0zakoz.github.io/repere/`. Contrôler au minimum le manifeste, le chargement des polices,
le démarrage d’une séance, une écriture IndexedDB et une réouverture hors ligne. La PWA peut
conserver temporairement une ancienne version tant que le service worker n’a pas été activé ;
fermer puis rouvrir l’application permet généralement d’appliquer la mise à jour.

## Contrôle avant commit ou livraison

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\build.ps1
git diff --check
git status --short
```

Pour une livraison touchant la PWA, ajouter :

```powershell
cd web
npm test
npm run check
```

Contrôler aussi que `keystore.properties`, `local.properties`, `release-private/`, `.tooling/`,
les APK et les répertoires `build/` restent ignorés. Ne pas inclure de captures ou dumps UI de
test sauf s’ils deviennent volontairement des ressources de documentation maintenues.

Contrôler enfin que les fichiers staged ne contiennent ni export utilisateur, ni sauvegarde,
ni adresse e-mail personnelle d’auteur. Les workflows ne doivent jamais écrire de valeur
secrète dans leurs journaux.
