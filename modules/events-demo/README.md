# events-demo

A throwaway module that exercises the [event bus](../../README.md#module-communication-event-bus)
end-to-end as a real module identity.

## What it does

On start it runs `demo.sh`, which:

1. Obtains a **client-credentials** token using the `MODULE_OIDC_CLIENT_ID` / `MODULE_OIDC_CLIENT_SECRET`
   the platform injects (works because module OAuth clients are service-account enabled).
2. Subscribes to the `demo.ping` topic (`?since=0`, so it replays from the start) in the background.
3. Publishes three events to `demo.ping`.

Both the published events and the ones received back over the subscription are written to the
container's stdout — watch this module's **live logs in the management UI** to see the round trip
(`-> published …` and `<- id:… / data:…`). This demonstrates the full path: module → core → module,
durable storage, and replay.

`nginx:alpine` is only a vehicle: its `/docker-entrypoint.d` hook runs `demo.sh` at startup, and
nginx then stays up so the background subscriber keeps streaming.

## Run it

This module also demonstrates **module dependencies**: it declares `weather` as a prerequisite
(`dependsOn: weather >=1.0.0`), so install `weather` first — otherwise the platform refuses to
install `events-demo`, and `weather` can't be removed while `events-demo` is installed. (events-demo
doesn't actually use weather; the prerequisite is purely to exercise the gate.)

Then install and start `events-demo` from the management UI (or the module API) and open its log
stream. Reinstalling replays the prior `demo.ping` events first (they are kept), then the new ones.

## Caveats

- **Keycloak issuer must match.** The module fetches its token from `host.docker.internal:8081`, but
  core validates tokens against its configured `spring.security.oauth2.resourceserver.jwt.issuer-uri`
  (`localhost:8081`). If Keycloak issues an `iss` that doesn't match, core rejects the token and the
  script logs that it couldn't authenticate. Align Keycloak's frontend/hostname (or core's issuer-uri)
  if so.
- **`demo.sh` must stay executable** so the nginx hook runs it directly; it's also written to be safe
  if merely sourced.
- Reachability assumes core (`:8080`) and Keycloak (`:8081`) are on the Docker host, reachable via
  `host.docker.internal` (the runtime adds the host-gateway mapping).
