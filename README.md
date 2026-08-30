# Suivi Muscu

Application Android personnelle et entièrement hors ligne pour enregistrer les
séances, suivre les performances et exporter les données.

## Installer sur Android

L’APK et les clés de signature ne sont volontairement pas versionnés dans Git.
Pour produire puis installer l’application depuis cette copie de travail :

```powershell
.\build.ps1
.\install.ps1
```

Pour une installation manuelle :

1. Copier `app/build/outputs/apk/release/app-release.apk` sur le téléphone.
2. Ouvrir le fichier depuis l'application **Fichiers**.
3. Si Android le demande, autoriser temporairement **Installer des applications
   inconnues** pour cette application.
4. Appuyer sur **Installer**.

Les futures mises à jour d’une même installation doivent être signées avec la
même clé. Les fichiers `release-private/suivi-muscu.jks` et
`keystore.properties` doivent donc rester conservés séparément et ne jamais
être publiés.

## Données

- Toutes les données restent dans le stockage privé Android.
- L'icône Réglages permet d'exporter un CSV, créer une sauvegarde JSON complète
  ou restaurer une sauvegarde.
- Avant une réinstallation ou un changement de téléphone, créer une sauvegarde
  JSON et la copier hors du téléphone.

## Compiler

L'outillage isolé est installé dans `.tooling/` et n'altère pas l'installation
Java du système.

```powershell
.\build.ps1
```

La commande exécute les tests, le lint Android et produit :

`app/build/outputs/apk/release/app-release.apk`

Pour installer par USB après activation du débogage USB (l'APK release est
alors installé directement) :

```powershell
.\install.ps1
```

## Architecture

- Kotlin et Jetpack Compose / Material 3
- Room comme stockage local
- sérialisation JSON versionnée pour la sauvegarde
- graphiques Compose Canvas
- aucune permission Internet

Le cahier des charges complet se trouve dans `PLAN.md`.
