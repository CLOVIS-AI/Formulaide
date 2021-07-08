# Gestion Publique

Système de gestion publique pour la Mairie d'Arcachon, pour permettre aux habitants de faire des requêtes auprès de la Mairie et les suivre.

Pour contribuer, voir [CONTRIBUTING.md](CONTRIBUTING.md). La documentation du code [est disponible ici](http://arcachon-ville.gitlab.io/formulaide/documentation).

## Déploiement

Ce projet nécessite une base de données [MongoDB](https://www.mongodb.com/). Formulaide est constitué d'un serveur et d'une interface web. L'interface web est incluse dans l'exécutable du serveur.

Il est possible d'utiliser plusieurs serveurs connectés à la même base de données (pour répartir les requêtes). Dans ce cas, c'est la responsabilité de l'administrateur réseau de gérer la répartition des requêtes entre les différents serveurs.

Il est théoriquement possible d'utiliser des répliques Mongo pour utiliser plusieurs bases de données, mais ce n'est pas testé.

Liens de téléchargement :

- MongoDB : [Site officiel](https://www.mongodb.com/) | [Docker](https://hub.docker.com/_/mongo)
- Serveur : [Zip](http://arcachon-ville.gitlab.io/formulaide/bin/server.zip) | [Tar](http://arcachon-ville.gitlab.io/formulaide/bin/server.tar) | [Docker](https://gitlab.com/arcachon-ville/formulaide/container_registry)

Les binaires du serveur (zip, tar) nécessitent une installation valide de Java. Tous les liens donnés ci-dessus fonctionnent sur Linux, MacOS et Windows.

Nous recommandons l'utilisation de Docker (gestion des dépendances automatisée, gestion des mises à jours simplifiée).

### Configuration du serveur

Le serveur nécessite le paramétrage de variables d'environnement :

```shell
# Les coordonnées de MongoDB
# (peut être une IP, un nom de domaine…)
export formulaide_host="mongo"
export formulaide_port="27017"

# Le nom de la base à utiliser (sera créée si elle n'existe pas)
export formulaide_database="formulaide"

# Les informations pour se connecter à la base de données
export formulaide_username="root"
export formulaide_password="some-password"

# Le secret utilisé par le protocole JWT
export formulaide_jwt_secret="some secret"
```

Par défaut, le serveur écoute sur le port 8000.

### Initialisation de la base de données

Lors de la première exécution, la base de données est vide. Pour créer les comptes par défaut, il faut ajouter la variable d'environnement :

```shell
export formulaide_allow_init="true"
```

et donner `--init` comme paramètre au serveur lors de son exécution.

Si le serveur affiche `Responding at http://0.0.0.0:8000` dans sa sortie standard, la création des comptes a réussi. Si elle échoue, une erreur est affichée et le serveur plante.

Les comptes créés sont :

- un compte administrateur, login `admin@formulaide` et mot de passe `admin-development-password`,
- un compte employé, login `employee@formulaide` et mot de passe `employee-development-password`.

Pour des raisons de sécurité, il est très fortement conseillé de désactiver ces comptes.

### Docker Compose & Docker Swarm

Une configuration pour Docker Compose est disponible dans le fichier [docker-compose.yml](docker-compose.yml). Pour l'utiliser, il suffit de télécharger les 4 fichiers appelés `docker.compose[…].yml` à la racine du dépôt. Les explications sont données en commentaire dans le fichier principal.

Cette configuration est encouragée pour les environnements de développement. Pour un environnement de production, il faut changer les mots de passe et ajouter des sauvegardes de la base de données.

### Maintenance

Le serveur ne requiert aucune maintenance particulière, à part le mettre à jour lorsqu'une nouvelle version est disponible (il n'y a aucune sauvegarde nécessaire). Il est encouragé de sauvegarder les logs, pour aider à la résolution des problèmes.

La base de données est une installation standard de MongoDB. La documentation pour sa maintenance [se trouve ici](https://docs.mongodb.com/manual/administration/). En particulier, il est essentiel de paramétrer la sauvegarde de la base de données.
