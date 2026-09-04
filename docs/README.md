# Documentation de Repère

Ce dossier décrit l’application telle qu’elle existe actuellement. Il sert de contexte aux
développeurs et aux agents ; il ne constitue ni une feuille de route figée ni une liste de
restrictions sur les évolutions futures.

## Parcours de lecture

- [PRODUCT.md](PRODUCT.md) : lire pour comprendre les parcours utilisateur, les fonctionnalités
  disponibles et les limites actuelles.
- [ARCHITECTURE.md](ARCHITECTURE.md) : lire avant de modifier l’interface, la logique métier ou
  l’organisation du code.
- [DATA.md](DATA.md) : lire avant de toucher aux modèles, statistiques, migrations, sauvegardes
  ou exports.
- [DEVELOPMENT.md](DEVELOPMENT.md) : lire avant de compiler, tester, signer, installer ou livrer
  l’application.
- [WEB.md](WEB.md) : lire avant de modifier, tester, publier ou installer la PWA sur iPhone.
- [SECURITY.md](SECURITY.md) : lire avant de manipuler des données, dépendances, secrets,
  workflows ou éléments destinés au dépôt public.

Pour une correction ou une fonctionnalité substantielle, lire au minimum ce sommaire,
`PRODUCT.md`, `ARCHITECTURE.md` et le document spécialisé correspondant à la zone touchée.
Le code et les tests restent la source de vérité si une divergence est découverte ; corriger
alors la documentation dans la même modification.

## État de référence

- dépôt : `https://github.com/0zakoz/repere` ;
- visibilité : **publique** ; le code, l’historique et les journaux Actions ne sont pas
  confidentiels ;
- branche principale : `main` ;
- libellé Android actuel : **Suivi Muscu** ;
- package et `applicationId` : `fr.suivimuscu.app` ;
- version : **1.10.0**, `versionCode 14` ;
- PWA publiée : `https://0zakoz.github.io/repere/`, version **1.11.0** ;
- date de cet état des lieux : 1er septembre 2026.

Le nom du dépôt et le libellé visible de l’application sont volontairement documentés comme
deux faits distincts. Cette documentation ne renomme ni le package ni l’application installée.
