# weather

A throwaway module that demonstrates the platform's
[database provisioning](../../README.md#database--audit): it gets its own schema and
writes to the core schema through the audited, reversible lifecycle.

## What it does

`weather` ships no application logic — `nginx:alpine` on port 8082 is just an idle container. The
interesting part is what the platform provisions for it from `manifest.json` at install time:

1. A dedicated DB role and schema `mod_weather` (`db.ownSchema: true`).
2. **Write** access to the core schema (`db.coreAccess: write`), which grants its role `core_owner`
   membership — only once the module is authorized.
3. The `up.sql` migration, run as `mod_weather` with `search_path = mod_weather, core`, which:
    - creates a private `forecast` table in its own schema, and
    - adds a `mod_weather_opt_in` column to `core.patients`.

The column it adds to core is recorded in the [core change log](../../README.md#core-change-log) and
dropped again on purge, leaving core as the module never touched it.

## Run it

`weather` is the root of the demo dependency chain — `events-publisher` declares it as a prerequisite
— so install it first. Because it requests `write` access to core, it must be **authorized** (which
grants its role `core_owner` membership) for the migration's `ALTER TABLE core.patients` to succeed.

Inspect the result in the **Database** tab: the `mod_weather.forecast` table, and the new
`mod_weather_opt_in` column on `core.patients`.

## Caveats

- It has no `ui`, `endpoints`, or `idp` block: no embedded tab, and no OAuth client (so it can't use
  the event bus). The published port serves only the default nginx page.
- `coreAccess` is fixed at install — raising it later has no effect (see the demo assumptions in the
  [main README](../../README.md)).
- Purge reverses the core column via the audit log, then drops the `mod_weather` schema and role.
