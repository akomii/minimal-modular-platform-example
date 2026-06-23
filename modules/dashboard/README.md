# dashboard

A Grafana module that visualizes core data, embedded in the platform UI as a role-gated tab. It
demonstrates a module with **read** access to the
[core schema](../../README.md#database--audit) plus full UI and identity integration.

## What it does

`grafana/grafana` on port 8084, provisioned entirely from `manifest.json` and the mounted
`provisioning/` directory:

1. **Reads core** — a read-only Postgres datasource (`Core DWH`) wired to the core schema with the
   `MODULE_DB_*` credentials the platform injects (`db.coreAccess: read`).
2. **Ships a dashboard** — `provisioning/dashboards/patients.json`, a patients overview, loaded at
   startup.
3. **Embeds as a tab** — declared in `ui` at `/d/patients-overview?kiosk` (kiosk mode hides
   Grafana's own chrome); the SPA shows it as the **Dashboard** tab.
4. **Single sign-on** — Grafana's generic OAuth points at the realm, so signing into the platform
   signs you into Grafana. Its client roles map to Grafana roles: `dashboard-admin → Admin`,
   `dashboard-user → Viewer` (strict — no role, no access). `admin` is granted `dashboard-admin` via
   `idp.grants`.

## Run it

Install and **authorize** `dashboard` from the Modules tab (authorize so its role gains the core
read grant). The **Dashboard** tab then appears for any user holding a `dashboard-*` role while the
module is running — `admin` has it out of the box.

## Caveats

- **Keycloak issuer / hostnames.** The browser reaches Keycloak at `localhost:8081`
  (`GF_AUTH_GENERIC_OAUTH_AUTH_URL`) while Grafana's server-side calls use `host.docker.internal:8081`
  (token/userinfo URLs); the redirect URI `http://localhost:8084/login/generic_oauth` is registered
  via `idp.redirectUris`.
- Embedding relies on `GF_SECURITY_ALLOW_EMBEDDING=true` plus auto-login and the disabled login form,
  so the iframe signs in silently.
- The datasource is read-only and non-editable; `dashboard` never writes to core.
