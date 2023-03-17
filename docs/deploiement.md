# Formulaide : déploiement

Formulaide est composé d'une base de données MongoDB et d'un serveur web.

- MongoDB : [Site officiel](https://www.mongodb.com/) | [Docker](https://hub.docker.com/_/mongo)
- Serveur : [Zip](http://opensavvy.gitlab.io/formulaide/bin/server.zip) | [Tar](http://opensavvy.gitlab.io/formulaide/bin/server.tar) | [Docker](https://gitlab.com/opensavvy/formulaide/container_registry)

Il est possible d'installer Formulaide de plusieurs manières :

- en natif,
- via Docker,
- via Docker Compose,
- via Docker Swarm,
- sur Kubernetes via Helm.

Installer les différents composants directement sur un serveur n'est pas recommandé : vous devez installer manuellement les dépendances des différents services, les mises à jour sont plus compliquées et vous devez paramétrer vous-même la communication entre les différents services.
Installer les composants directement via Docker est possible, mais la configuration de la communication entre les services doit être faites manuellement dans ce cas.

Les méthodes d'installation recommandées sont :

- **Docker Compose** pour tester le projet ou pour une installation à petite échelle (tous les services sont exécutés sur une seule machine, pas de répliques),
- **Docker Swarm** pour une installation à moyenne échelle (les services sont répartis sur plusieurs machines, le serveur peut être répliqué),
- **Kubernetes** pour une installation à grande échelle (les services sont répartis sur plusieurs machines, le serveur et la base de données sont répliqués).

Ces trois manières d'installer Formulaide possèdent plusieurs avantages :

- l'installation se fait entièrement à partir de fichiers de configuration,
- la mise à jour des différents services consiste à modifier les fichiers de configuration et relancer l'installation, aucun téléchargement manuel n'est nécessaire,
- les différents services sont isolés du reste du système et peuvent être facilement désinstallés,
- la configuration de la communication entre les différents services est fournie,
- l'installation est possible sur n'importe quel type de machine (Windows, MacOS, Linux…).

Quelle que soit votre manière préférée d'installer Formulaide, il vous faudra ensuite :

- sécuriser le compte d'administration ([guide des premiers pas](debut.md)),
- paramétrer les sauvegardes de la base de données (non fourni),
- paramétrer les sauvegardes des logs du serveur pour faciliter la résolution des problèmes (non fourni).

## Installation native

> L'installation native n'est pas recommandée (plus d'informations ci-dessus).
>
> La possibilité d'installer Formulaide nativement est fournie pour les cas d'utilisation où les autres manières ne sont pas possibles,
> ou pour intégrer un service non supporté.
> Dans tous les autres cas, une autre solution sera plus appropriée.

Installez la base de données et le serveur (liens de téléchargement au début de ce document).
Le serveur nécessite une installation valide de Java.

Le serveur est configuré grâce à des variables d'environnement :

- connexion à la base de données :
  - `formulaide_host` et `formulaide_port` : la machine et le port que le serveur doit utiliser pour se connecter à MongoDB (le nom de la machine peut être une adresse IP ou un nom DNS),
  - `formulaide_database` : le nom de la base de données utilisée par Formulaide (permet de partager une instance MongoDB entre Formulaide et d'autres outils). Si la base n'existe pas, elle sera créée automatiquement (conseillé : `formulaide`),
  - `formulaide_username` et `formulaide_password` : le compte utilisé par le serveur pour se connecter à MongoDB. Ce compte doit avoir tous les droits sur la base fournie précédemment (aucun droit sur les autres bases n'est nécessaire),
- personnalisation :
  - `formulaide_report_email` : l'adresse mail à laquelle les signalements de bugs réalisés par les employés sont envoyés (si manquant : signalement aux développeurs de Formulaide)

## Docker Compose

[Docker](https://www.docker.com/) est une technologie permettant de facilement installer des logiciels dans des environnements isolés pour pouvoir facilement les mettre à jour ou les désinstaller (appelés « conteneurs »).
Un logiciel fourni via Docker inclus ses propres dépendances, il n'y a donc rien de plus à installer que Docker lui-même.
Docker peut être installé sur n'importe quel système d'exploitation, mais les meilleures performances sont sur Linux.

[Docker Compose](https://docs.docker.com/compose/) est une technologie permettant de facilement organiser plusieurs conteneurs sur une même machine (« orchestrer des conteneurs »).
Docker Compose est inclus dans les versions récentes de Docker (pour des versions plus anciennes, il faut l'installer en plus).

Docker Compose est la manière la plus facile d'installer Formulaide, mais tous les services s'exécutent obligatoirement sur une seule machine et il n'est pas possible d'utiliser des répliques.
Si cette machine s'éteint ou devient inaccessible pour une raison ou pour une autre, Formulaide entier ne sera plus accessible.

Pour déployer Formulaide via Docker Compose sur votre machine, il vous faut télécharger :

- le fichier de configuration : [docker-compose.yml](../docker/docker-compose.yml),
- les modifications pour l'environnement de développement (non requis pour une installation en production) : [docker-compose.override.yml](../docker/docker-compose.override.yml),
- les modifications pour l'environnement de production (non requis pendant le développement) : [docker-compose.prod.yml](../docker/docker-compose.prod.yml).

Ces fichiers sont rarement modifiés, mais il arrive que des nouvelles options de configuration soient ajoutées.
Pensez à les re-télécharger de temps en temps.

Le premier fichier contient :

- le numéro de la version de Formulaide à utiliser : `latest` par défaut pour utiliser la version la plus récente. Il est conseillé de choisir une version explicitement et de la modifier quand on le souhaite pour garder le contrôle complet.
- les différentes variables de configuration sont identiques à l'installation native ci-dessus. Les valeurs par défaut sont fonctionnelles, mais non-sécurisées (pensez à modifier le mot de passe de la base de données et le secret JWT !).

Le troisième fichier contient le nom de votre site. Il est important de le modifier pour permettre aux utilisateurs d'accéder de manière sécurisée à Formulaide. Pour le changer, il faut remplacer `localhost` par le nom complet du site (par exemple `formulaide.mon-site.fr`).

### Développement

Pour déployer Formulaide, exécutez la commande suivante dans le dossier où vous avez téléchargé les différents fichiers de configuration :

```shell
docker compose up -d
```

- Serveur : http://localhost:8002
- MongoDB sur le port 27017
- Mongo Express (interface d'administration de la base de données) : http://localhost:8081

Sur des vieilles versions de Docker Compose, remplacez `docker compose` par `docker-compose`.

### Production

Pour déployer Formulaide, exécutez la commande suivante dans le dossier où vous avez téléchargé les différents fichiers de configuration :

```shell
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d
```

- Serveur : http://localhost:8001 et `https://<votre nom de domaine>`
- La base de données n'est pas accessible

Sur des vieilles versions de Docker Compose, remplacez `docker compose` par `docker-compose`.

## Docker Swarm

[Docker Swarm](https://docs.docker.com/engine/swarm/) (« essaim ») est une amélioration de Docker Compose qui :

- utilise les mêmes fichiers de configuration que Docker Compose,
- permet de répartir les services entre plusieurs machines,
- permet de créer des répliques du serveur : le traffic sera automatiquement réparti entre les différentes machines,
- déplace les services d'une machine à l'autre s'ils deviennent inaccessibles : si une machine s'éteint, les services qui s'y exécutaient sont automatiquement relancés sur une autre machine.

En revanche, Docker Swarm n'est pas capable d'organiser des répliques de la base de données.
Docker Swarm n'est pas non plus capable de transférer la base de données d'une machine à une autre.

Docker Swarm est fourni avec Docker.
Il est nécessaire de l'activer (suivre la procédure dans le lien ci-dessus).

La configuration est identique à celle de Docker Compose fournie précédemment.
`replicas` est ajoutée dans `docker-compose.prod.yml` pour choisir le nombre de répliques du serveur et du reverse proxy.
Il n'est pas possible d'utiliser plusieurs répliques de la base de données.

Si la machine sur laquelle s'exécute la base de données a un problème, Swarm va essayer de la relancer sur une autre machine.
Les données ne seront pas déplacées : la nouvelle base de données sera vide, jusqu'à être à nouveau déplacée vers la machine d'origine.
Il est conseillé d'empêcher Swarm de déplacer la base de données (par exemple avec des [labels](https://docs.docker.com/engine/swarm/manage-nodes/#add-or-remove-label-metadata)).

Pour déployer Formulaide, exécutez la commande suivante dans le dossier où vous avez téléchargé les différents fichiers de configuration :

```shell
docker stack deploy -c docker-compose.yml -c docker-compose.prod.yml formulaide
```

## Kubernetes

[Kubernetes](https://kubernetes.io/) est un orchestrateur de conteneurs créé par Google pour les projets à grande échelle.
Il est possible d'installer un cluster Kubernetes soit-même où de louer un cluster maintenu à jour par une tierce partie (Google Cloud, Amazon Web Services, OVH…).
Gérer son propre cluster Kubernetes est considéré comme une tâche difficile.

En revanche, Kubernetes permet de répliquer la base de données et d'installer plusieurs fois Formulaide dans le même cluster.
Nous utilisons ces possibilités pour faciliter les essais de plusieurs versions en parallèle, par exemple.

La configuration de services sous Kubernetes est complexe.
Pour la faciliter, nous utilisons [Helm](https://helm.sh/).
Helm est un outil permettant de facilement configurer et déployer des services sur un cluster Kubernetes.

La configuration par défaut [est disponible ici](../helm/values.yaml).
Le contenu est similaire à l'installation native au début de ce document.
Pour modifier des paramètres, créez un fichier vide `formulaide.yaml` et ajoutez-y les valeurs que vous souhaitez mettre à la place, en conservant la structure.
Au minimum, il faut modifier le nom du serveur sur lequel vous déployez Formulaide, le mot de passe de la base de données et le secret JWT :

```yaml
config:
  host: "formulaide.mon-site.fr"

mongodb:
  auth:
    rootPassword: "le mot de passe de la base de données, à modifier !"
```

Pour pouvoir déployer Formulaide, il faut se connecter à notre dépôt :

```shell
helm repo add opensavvy https://gitlab.com/api/v4/projects/33369420/packages/helm/stable
```

Pour forcer Helm à se synchroniser avec les nouvelles versions présentes sur le dépôt :

```shell
helm repo update
```

Quand vous êtes satisfait de votre configuration, vous pouvez déployer le serveur :

```shell
helm upgrade --install -f formulaide.yaml formulaide opensavvy/formulaide
```

Si vous modifiez la configuration, il suffit de relancer cette commande pour relancer Formulaide avec la nouvelle configuration.

Il est possible d'automatiser entièrement le déploiement lors de nouvelles mises à jour.
Pour cela, contactez-nous directement (par exemple en créant une issue sur notre GitLab).
