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

- Conventions de code : [BrainDot code style](https://gitlab.com/braindot/legal/-/tree/master/coding-style)
- Conventions de commits : [BrainDot commit style](https://gitlab.com/braindot/legal/-/blob/master/coding-style/STYLE_Git.md)

## Gestion de projet

La gestion du projet a lieu sur [le dépôt](https://gitlab.com/clovis-ai/formulaide).

On utilise:

- Les [requirements](https://gitlab.com/clovis-ai/formulaide/-/requirements_management/requirements) comme liste d'objectifs du projet
- Les [issues](https://gitlab.com/clovis-ai/formulaide/-/issues) comme liste des tâches
- Les [milestones](https://gitlab.com/clovis-ai/formulaide/-/milestones) pour suivre l'avancement des tâches par rapport aux jalons donnés
- Les [merge requests](https://gitlab.com/clovis-ai/formulaide/-/merge_requests) pour les modifications de code

Les contributions suivent les conventions [BrainDot](https://gitlab.com/braindot/legal/-/blob/master/contrib/CONTRIBUTING.md).
