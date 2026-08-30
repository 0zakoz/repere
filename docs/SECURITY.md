# Sécurité et dépôt public

## Modèle d’exposition

Le dépôt `0zakoz/repere` est public. Son code, ses commits, ses branches publiées, les auteurs
Git et les journaux GitHub Actions doivent être considérés comme accessibles durablement sur
Internet. La PWA GitHub Pages est elle aussi publique : son JavaScript ne peut contenir aucun
secret côté serveur puisqu’il est exécuté et inspectable dans le navigateur.

Cette visibilité ne publie pas les données saisies dans l’application. Android les conserve
dans son stockage privé ; la PWA les conserve dans IndexedDB pour l’origine
`https://0zakoz.github.io/repere/`. Chaque navigateur possède sa propre base. Le dépôt ne
contient ni compte utilisateur, ni backend, ni mécanisme de synchronisation.

## Contenu interdit dans Git

- clés Android, keystores, mots de passe de signature et fichiers `keystore.properties` ;
- tokens GitHub, clés API, identifiants cloud ou secrets de déploiement ;
- sauvegardes JSON, exports CSV/Markdown, bases Room/IndexedDB ou autres données réelles ;
- notes, poids, nutrition, captures d’écran ou arborescences UI provenant d’un utilisateur ;
- APK, AAB, sorties de build, caches ou journaux susceptibles de contenir des chemins ou
  valeurs privées.

Les tests doivent employer des identifiants et valeurs synthétiques. Un exemple de
configuration ne contient que des marqueurs comme `CHANGE_ME` ou `...`.

## Contrôle avant publication

1. vérifier `git status --short` et chaque fichier staged ;
2. lire `git diff --cached` et exécuter `git diff --cached --check` ;
3. confirmer que l’adresse d’auteur Git utilise `@users.noreply.github.com` ;
4. vérifier les nouveaux workflows et dépendances, y compris ce qu’ils affichent dans les
   journaux ;
5. exécuter les tests appropriés avant le push.

`.gitignore` est une protection secondaire, pas une autorisation à placer des données réelles
dans le répertoire du projet.

## En cas d’exposition accidentelle

Arrêter le push ou la publication. Si la donnée a déjà été poussée, la retirer du dernier état
ne suffit pas : révoquer ou remplacer immédiatement tout secret concerné, nettoyer toutes les
références Git pertinentes, forcer la mise à jour distante de façon contrôlée et vérifier les
forks, artefacts et journaux Actions. Une donnée personnelle exportée doit être traitée comme
compromise même après réécriture de l’historique.
