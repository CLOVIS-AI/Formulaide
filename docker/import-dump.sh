#!/usr/bin/env bash

#
# Imports a database dump (ZIP) into MongoDB.
# Only works with the docker-compose development build (docker-compose.override.yml).
#
# Usage:
# 	./import-dump.sh dump.zip
#

set -e  # stop if any command fails
set -x  # print the executed lines

if [ "$#" -ne 1 ]; then
    echo "Please specify .zip dump"
    exit 1
fi

MONGO_SERVICE_NAME=mongo
DB_NAME=formulaide
DB_USER=root
DB_PASSWORD=development-password

DUMP_ZIPPED="$1"
DUMP_UNZIPPED=/tmp/dump
DUMP_DB=database

mkdir -p "$DUMP_UNZIPPED" "$DUMP_DB"

unzip "$DUMP_ZIPPED" -d "$DUMP_UNZIPPED"
sudo rm -r "$DUMP_DB/formulaide"
sudo mv "$DUMP_UNZIPPED/$DB_NAME" "$DUMP_DB"

docker compose exec -T "$MONGO_SERVICE_NAME" mongorestore -u "$DB_USER" -p "$DB_PASSWORD" /root/dump
