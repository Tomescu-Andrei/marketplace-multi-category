#!/bin/sh
set -eu

# This instance exists only for this build step. No Docker socket or external database is needed.
codegen_data=$(mktemp -d /tmp/marketplace-codegen.XXXXXX)
chown postgres:postgres "$codegen_data"
gosu postgres initdb -D "$codegen_data" --username=codegen --auth=trust > /tmp/codegen-init.log
trap 'gosu postgres pg_ctl -D "$codegen_data" -m immediate stop > /dev/null 2>&1 || true' EXIT
gosu postgres pg_ctl -D "$codegen_data" -o "-h 127.0.0.1 -p 55432 -k /tmp" -w start
createdb -h 127.0.0.1 -p 55432 -U codegen codegen
export CODEGEN_DB_URL=jdbc:postgresql://127.0.0.1:55432/codegen
export CODEGEN_DB_USERNAME=codegen
export CODEGEN_DB_PASSWORD=unused
mvn -B -DskipTests package
