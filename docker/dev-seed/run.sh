#!/bin/sh
# Dev-seed runner for the docker-compose `seed` service.
#
# The app runs Flyway on startup; this script waits for the schema to exist,
# then applies the idempotent seed. It polls `page.pages` — the LAST-migrated
# of the schemas the seed touches in Flyway's per-domain order (identity →
# reference → touroperator → media → audience → pickup → experience → page) —
# so a positive check means every table the seed needs is migrated. If the
# seed ever grows into a later domain (e.g. audit), move the poll target with
# it, or the inserts race their own migration ("relation does not exist").
set -eu

echo "dev-seed: waiting for migrations to finish..."
until [ "$(psql -h db -U vointika -d vointika -tAc \
        "SELECT to_regclass('page.pages') IS NOT NULL")" = "t" ]; do
  sleep 2
done

echo "dev-seed: applying seed..."
psql -h db -U vointika -d vointika -v ON_ERROR_STOP=1 -f /seed/dev-seed.sql
echo "dev-seed: done."
