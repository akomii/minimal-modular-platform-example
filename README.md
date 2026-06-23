# minimal-modular-platform-example

A minimal Java + Vue.js platform core that dynamically installs and integrates technology-agnostic modules.

A Spring Boot 3.5 (Java 21) core serves a Vue 3 + PrimeVue SPA and runs modules as Docker containers. Each module can get its own Postgres schema with controlled access to the core schema, plus its
own OAuth client and roles in a shared Keycloak realm.

## Features

- **Module lifecycle** — install, authorize, upgrade, start, stop, and purge modules from JSON manifests in `modules/`. Operations are idempotent.
- **Runtime selection** — modules run behind a `ModuleRuntime` interface; the Docker runtime is selected with `modules.runtime` (default `docker`).
- **Database provisioning** — each module gets a `mod_<id>` DB role and, optionally, its own schema, with `none`/`read`/`write` access to the core schema. Core schema and seed data are managed by
  Flyway.
- **Audit & undo** — a module's changes to the core schema and data are recorded in an append-only log and replayed in reverse on purge, leaving core untouched.
- **Authentication** — Keycloak OIDC with two security chains: session-based login for the SPA, bearer JWT for API clients. Realm roles map to Spring roles, and only `platform-admin` users can reach
  the management API.
- **Module identity** — each module gets its own OAuth client, client roles, and service accounts in the shared realm, reconciled additively on upgrade.
- **User management** — an admin Users tab (backed by `/api/users` and `/api/roles`) lists platform users and grants/revokes their roles — `platform-admin` plus each installed module's client roles —
  through a checkbox matrix.
- **Module communication** — a content-agnostic pub/sub event bus, mediated by core over HTTP+SSE: durable and replayable, behind a swappable `EventStore` (Postgres by default, selected with
  `events.backend`).
- **Observability UI** — live streams (SSE) for HTTP requests, server logs, and per-container logs, shown in the management UI.
- **Database viewer (demo)** — an admin-only, read-only tab that browses every non-system schema and table and pages through row data, for debugging and demonstration only. Disable with
  `dbviewer.enabled=false`.
- **Module UI integration** — modules can declare web pages (`ui`) that the SPA embeds as tabs, and HTTP endpoints (`endpoints`) listed read-only for discovery. UI tabs are role-gated per user: a user
  sees a running module's tab only if they hold one of its roles, so a non-admin signs in to just the module tab(s) they may use (admins also get the Modules and Users tabs).
- **Error handling** — exceptions map to RFC 7807 `ProblemDetail` responses.

## Module provisioning & versioning

A module is identified by its manifest `id`, which also names its DB schema, DB role, OAuth client, and container. All identity lives in a shared `modular` Keycloak realm; core needs realm-management
rights in it, not realm-create.

### Realm vs client

Keycloak organizes identity in two layers. A **realm** is the security boundary: one self-contained set of users, roles, and signing keys with its own token issuer (`…/realms/modular`) — tokens are
valid only within it. A **client** is an application registered with that realm; it has no users of its own but defines how one app takes part in login (its secret, redirect URIs, and enabled flows).
One realm holds many clients.

This platform runs a single `modular` realm. `core` is one client in it (browser login plus a service account for provisioning), and every installed module gets its own client — its OAuth client,
client roles, and service accounts — in that same realm. So all identities share one user store while each app keeps its own credentials and redirect URIs.

### Manifest

- **`image`**, **`ports`**, **`env`**, **`mounts`** — the container image to run, its published ports, environment variables, and read-only file mounts.
- **`db`** (optional)
    - `ownSchema` — `true` gives the module its own schema `mod_<id>`; omit for core-access-only modules.
    - `coreAccess` — `none` | `read` | `write`.
    - `migrations` — ordered list of SQL scripts. The first is the install; later ones are version deltas.
- **`idp`** (optional)
    - `redirectUris` — for the module's OAuth client.
    - `roles` — client roles created on the module's OAuth client.
    - `users` (optional) — service accounts the module needs, created with a generated password (never in the manifest).
    - `grants` (optional) — client roles to assign to pre-existing platform users (matched by username, e.g. `admin`) at install; the user is never created or removed by the module.
- **`dependsOn`** (optional) — prerequisite modules, a list of `{ id, version }`. Each `id` must be installed at a version satisfying the constraint (`>=`, `>`, `<=`, `<`, `=`; a bare version means
  `>=`, e.g. `>=1.0.0`) before this module installs — and a module can't be removed while an installed module still depends on it.
- **`ui`** (optional) — web pages to embed as tabs, a list of `{ name, path }` (each `path` relative to the module's published port). A page's tab is shown to users holding one of the module's roles
  while the module is running, served by `GET /api/ui`.
- **`endpoints`** (optional) — HTTP endpoints the module exposes, a list of `{ label, method, path }`, listed read-only in the Modules tab for discovery; core records them but does not call them.

A manifest can pre-grant module roles to existing platform users via `idp.grants`; beyond that, the admin assigns roles to users from the Users tab.

### Lifecycle

- **Install** (no record yet) — create the DB role + schema, run all migrations, grant `coreAccess`; create the OAuth client + client roles + default users, and grant declared client roles to existing
  platform users; pull the image and start the container with
  the DB credentials and OIDC client secret injected as env vars; record the installed version, applied migrations, and core changes in the audit log.
- **Upgrade** (catalog version > installed) — run only the pending migrations; additively add any new roles, redirect URIs, default users, and role grants; redeploy the container on the new image with
  refreshed
  env (DB creds + rotated secret). The catalog must already hold the new manifest (restart core to reload it).
- **Purge** (uninstall) — replay the audit log to undo core changes; drop the module schema and role; delete the OAuth client (Keycloak removes its roles and assignments) and default users. Existing
  platform users stay and only lose the deleted module roles.

### Core change log

Every change a module makes to core is recorded in an append-only log (`core.module_core_audit`) and replayed newest-first on purge:

- **Schema (DDL)** — the provisioner records tables/columns/indexes added around the module's migrations; reversed by dropping them.
- **Data (DML)** — a trigger records the module's inserts/updates/deletes with before/after rows; reversed by restoring them.

The log is written only via a `SECURITY DEFINER` trigger and by the provisioner, so modules can't tamper with it.

### Access control

The module's OIDC client maps roles strictly: only users holding the module's role can use it. The admin assigns that role to chosen users from the Users tab (or directly in Keycloak).

### Demo assumptions

- Migrations are append-only — new versions only append scripts; existing ones are never edited or reordered (pending migrations are found by count).
- Roles are additive across versions; a role dropped from a v2 manifest is not auto-removed (only purge removes roles).
- `coreAccess` is fixed at install — raising it in a later version has no effect on upgrade (only install/authorize grant core access).
- Default-user usernames don't collide with admin-created users.
- Core schema changes are additive (reversal = drop); core data changes are reversed from the audit log.
- Downgrade is out of scope.

### Known shortcuts

- Module DB passwords are stored in plaintext in the provisioning ledger.

## Module communication (event bus)

Core mediates a content-agnostic pub/sub bus so modules can exchange information with core and with each other. Modules only ever speak HTTP/SSE; core owns a durable, replayable log behind an
`EventStore` seam (Postgres by default, selected with `events.backend`), so the backing store can be swapped (e.g. for Kafka) without changing modules. Topics are dynamic — there is no manifest
declaration; any authenticated identity may publish or subscribe.

### How it works

- **Publish** — `POST /api/events/{topic}` with a bearer token and an opaque JSON body returns `{"seq": N}`. The publisher is taken from the token (`azp` = the module's OAuth client id). A topic is a
  single path segment — use dots, not slashes (e.g. `patient.admitted`).
- **Subscribe** — `GET /api/events/{topic}/stream` (SSE): core replays the topic's events after the caller's cursor, then tails new ones live. Each message carries its `seq` as the SSE `id`.
- **Resume** — reconnect with the `Last-Event-ID` header (or `?since=<seq>`) to continue exactly where you left off. Delivery is at-least-once; the subscriber dedups by `seq`.

A module authenticates as itself with a client-credentials token from its own OAuth client. That client is created with a **Keycloak service account** enabled (Keycloak's machine-to-machine feature),
so the module can exchange its injected `MODULE_OIDC_CLIENT_ID`/`MODULE_OIDC_CLIENT_SECRET` for a token; the token's `azp` is the module id, which becomes the event's `publisher`.

The same mechanism serves all three directions — core→module, module→core, and module→module — since core and every module are just publishers/subscribers on named topics. Core publishes in-process
through the bus; modules go through the HTTP/SSE endpoints.

Under the hood, events are appended to a `core.events` log through a single serialized writer (so `seq` is gap-free), and a Postgres `LISTEN/NOTIFY` listener pushes them to live subscribers. Events
are kept indefinitely and survive a module's purge.

### Known limitations

- Only modules with an `idp` block can use the bus — they need an OAuth client to obtain a token, so DB-only modules can't publish or subscribe.
- No per-topic authorization: any authenticated module may subscribe to any topic, including core's (e.g. patient events). Access is gated only by a valid token.

## Portability

Identity is provider-agnostic; the database is not. The IdP sits behind an `idp.provider` seam (`IdpProvisioner`/`IdpRoleExtractor`) and speaks standard OIDC, so it can point at any OIDC-compliant
provider. Postgres has no such seam: core uses raw JDBC with hand-written SQL (no JPA/Hibernate dialect layer) and leans on Postgres as more than a store — as the module isolation boundary (login
roles
and schemas with `GRANT`s), the audit/undo engine (a `SECURITY DEFINER` trigger over `JSONB`), and the event bus's live transport (`LISTEN/NOTIFY`). The event bus has a nominal `EventStore` seam
(`events.backend`), but only a Postgres implementation exists and its live path is NOTIFY-based. Swapping databases would mean rewriting the migrations and the provisioning/audit/event code, not
flipping
a property.
