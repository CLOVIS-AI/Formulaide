# Formulaide

## Organisation du projet

Ce dépôt est organisé en plusieurs projets gérés par Gradle.
Pour exécuter une tâche dans un projet, on utilise `./gradlew <project>:<task>`.
Par exemple, pour obtenir la liste des tâches dans le projet `core`, on utilise la tâche spéciale `./gradlew core:tasks`.

Les différents projets existants sont :

- `app` : l'application web
- `backend` : le serveur web servant l'application
- `core` : déclaration des données et des fonctions
- `fake` : fausse implémentation de l'application, utilisée dans les tests
- `test` : déclaration des tests communs pour vérifier toutes les implémentations de l'application
- `test-structure` : utilitaires pour l'écriture de tests dynamiques
- `mongo` : implémentation utilisant une base de données MongoDB
- `remote` : déclaration de l'API HTTP
- `remote-client` : implémentation côté client de l'API
- `remote-server` : implémentation côté serveur de l'API

## Langues utilisées

Français :

- Documents de présentation (README…)
- Gestion de projet (liste des tâches…)

Anglais :

- Code
- Git
- Documentation

## Conventions et styles

Le projet suit les [conventions OpenSavvy](https://gitlab.com/opensavvy/wiki/-/blob/main/README.md).
