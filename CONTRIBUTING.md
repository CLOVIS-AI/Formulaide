# Formulaide

## Organisation du projet

Ce dépôt est organisé en plusieurs projets gérés par Gradle.
Pour exécuter une tâche dans un projet, on utilise `./gradlew <project>:<task>`.
Par exemple, pour obtenir la liste des tâches dans le projet `core`, on utilise la tâche spéciale `./gradlew core:tasks`.

Les différents projets existants sont :

- `core` : déclaration des données et des fonctions
- `api` : déclaration de l'API RESTful
- `database` : implémentation de `core` via une base de données MongoDB
- `server` : implémentation d'`api` en déléguant à `database`
- `client` : implémentation de `core` en déléguant à `server` via `api`
- `ui` : ancienne interface graphique, utilisant `client` (React)
- `ui2` : nouvelle interface graphique, utilisant `client` (Compose Web, qui remplacera l'ancienne interface lors de la 2.0)

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
