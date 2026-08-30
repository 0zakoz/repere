# Consignes pour les agents

Ce fichier s’applique à l’ensemble du dépôt.

## Avant de modifier le projet

1. Lire [docs/README.md](docs/README.md), puis les documents qu’il indique pour la tâche.
2. Inspecter le code concerné : la documentation décrit l’état actuel, mais le code reste la
   source de vérité en cas d’écart.
3. Vérifier `git status` et préserver toute modification utilisateur sans rapport avec la
   demande.

La demande explicite de l’utilisateur est prioritaire. Les choix techniques et fonctionnels
documentés ne sont pas des interdictions : une refactorisation, une nouvelle dépendance ou
un changement d’architecture sont possibles s’ils servent réellement la demande. Éviter
seulement les changements gratuits qui élargissent inutilement le périmètre.

## Garde-fous

- Préserver les données déjà présentes sur le téléphone. Toute évolution incompatible de
  `AppState` doit prévoir une migration explicite et une restauration des anciennes
  sauvegardes lorsque cela reste raisonnablement possible.
- Ne jamais désinstaller l’application, vider ses données, remplacer sa clé de signature ou
  exécuter une commande ADB destructive sans autorisation explicite.
- Ne jamais versionner `keystore.properties`, `local.properties`, `release-private/`,
  `.tooling/`, les APK, les clés, les secrets ou les sorties de build.
- Conserver les identifiants métier stables et les snapshots historiques, ou documenter et
  migrer consciemment tout changement de cette règle.
- Ne pas supposer que le téléphone est branché. Les tests sur appareil viennent en complément
  des tests automatisés et nécessitent l’accord de l’utilisateur lorsqu’ils changent l’état
  de l’appareil.

## Pendant et après une modification

- Suivre les conventions Kotlin et Compose déjà présentes tant qu’un changement plus large
  n’apporte pas un bénéfice clair.
- Ajouter ou adapter les tests correspondant aux règles métier modifiées.
- Mettre à jour la documentation si la fonctionnalité, les données, l’architecture,
  l’outillage ou une procédure de livraison change.
- Valider proportionnellement au risque. La vérification complète habituelle est :

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\build.ps1
```

- Pour l’installation et la signature, suivre [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md).

## Repères rapides

- Application Android affichée : **Suivi Muscu** ; dépôt GitHub : **repere**.
- Version actuelle : **1.6.2** (`versionCode 10`).
- Module unique : `app`.
- État métier : `AppState`, sérialisé en JSON et stocké dans une ligne Room.
- Schéma métier actuel : version 3 ; schéma Room : version 1.
- Interface : Jetpack Compose / Material 3, quatre onglets, aucune permission Internet.
