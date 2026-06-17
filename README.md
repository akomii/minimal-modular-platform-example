# minimal-modular-platform-example

A minimal Java + Vue.js platform core that dynamically installs and integrates technology-agnostic modules.

A Spring Boot 3.5 (Java 21) core serves a Vue 3 + PrimeVue SPA and runs modules as Docker containers. Each module can get its own Postgres schema with controlled access to the core schema, plus its own OAuth client and roles in a shared Keycloak realm.

## Features

- **Module lifecycle** — install, authorize, upgrade, start, stop, and purge modules from JSON manifests in `modules/`. Operations are idempotent.
- **Runtime selection** — modules run behind a `ModuleRuntime` interface; the Docker runtime is selected with `modules.runtime` (default `docker`).
- **Database provisioning** — each module gets a `mod_<id>` DB role and, optionally, its own schema, with `none`/`read`/`write` access to the core schema. Core schema and seed data are managed by Flyway.
- **Audit & undo** — a module's changes to the core schema and data are recorded in an append-only log and replayed in reverse on purge, leaving core untouched.
- **Authentication** — Keycloak OIDC with two security chains: session-based login for the SPA, bearer JWT for API clients. Realm roles map to Spring roles.
- **Module identity** — each module gets its own OAuth client, client roles, and service accounts in the shared realm, reconciled additively on upgrade.
- **Observability UI** — live streams (SSE) for HTTP requests, server logs, and per-container logs, shown in the management UI.
- **Error handling** — exceptions map to RFC 7807 `ProblemDetail` responses.

## Module provisioning & versioning

A module is identified by its manifest `id`, which also names its DB schema, DB role, OAuth client, and container. All identity lives in a shared `modular` Keycloak realm; core needs realm-management rights in it, not realm-create.

### Manifest

- **`db`** (optional)
  - `ownSchema` — `true` gives the module its own schema `mod_<id>`; omit for core-access-only modules.
  - `coreAccess` — `none` | `read` | `write`.
  - `migrations` — ordered list of SQL scripts. The first is the install; later ones are version deltas.
- **`idp`** (optional)
  - `redirectUris` — for the module's OAuth client.
  - `roles` — client roles created on the module's OAuth client.
  - `users` (optional) — service accounts the module needs, created with a generated password (never in the manifest).

Module roles are not auto-granted to humans — linking roles to real users is the site admin's job.

### Lifecycle

- **Install** (no record yet) — create the DB role + schema, run all migrations, grant `coreAccess`; create the OAuth client + client roles + default users; record the installed version, applied migrations, and core changes in the audit log.
- **Upgrade** (catalog version > installed) — run only the pending migrations; additively add any new roles, redirect URIs, and default users; redeploy the container on the new image with refreshed env (DB creds + rotated secret). The catalog must already hold the new manifest (restart core to reload it).
- **Purge** (uninstall) — replay the audit log to undo core changes; drop the module schema and role; delete the OAuth client (Keycloak removes its roles and assignments) and default users. Existing platform users stay and only lose the deleted module roles.

### Core change log

Every change a module makes to core is recorded in an append-only log (`core.module_core_audit`) and replayed newest-first on purge:

- **Schema (DDL)** — the provisioner records tables/columns/indexes added around the module's migrations; reversed by dropping them.
- **Data (DML)** — a trigger records the module's inserts/updates/deletes with before/after rows; reversed by restoring them.

The log is written only via a `SECURITY DEFINER` trigger and by the provisioner, so modules can't tamper with it.

### Access control

The module's OIDC client maps roles strictly: only users holding the module's role can use it. The site admin assigns that role to chosen users.

### Demo assumptions

- Roles are additive across versions; a role dropped from a v2 manifest is not auto-removed (only purge removes roles).
- Default-user usernames don't collide with admin-created users.
- Core schema changes are additive (reversal = drop); core data changes are reversed from the audit log.
- Downgrade is out of scope.

## Open gaps

- **User management** — no API or UI to assign module roles to real users; today this is a manual step in Keycloak.
- **Module UI integration** — module UIs (e.g. Grafana) aren't embedded in the platform SPA with shared login; goal is to show them in tabs.
- **Settings UI** — no UI to change backend configuration.
- **Database viewer** — no module with a simple UI to browse databases.

### Known shortcuts

- Module DB passwords are stored in plaintext in the provisioning ledger.
