# events-collector

A throwaway module that **collects** events from the platform
[event bus](../../README.md#module-communication-event-bus) and prints them to its console — the
consumer counterpart to [`events-publisher`](../events-publisher/README.md).

## What it does

On start it runs `collector.sh`, which:

1. Obtains a **client-credentials** token using the `MODULE_OIDC_CLIENT_ID` / `MODULE_OIDC_CLIENT_SECRET`
   the platform injects (works because module OAuth clients are service-account enabled).
2. Subscribes to the `demo.ping` topic with `?since=1`, so core replays events **after** global
   seq 1 and then tails live ones.
3. Echoes every received event to the container's stdout.

Watch this module's **live logs in the management UI** to see the `<- id:… / data:…` lines as
events-publisher's events (and any later ones) arrive.

`nginx:alpine` is only a vehicle: its `/docker-entrypoint.d` hook runs `collector.sh` at startup,
and the subscriber runs in the background so nginx can start and keep the container alive.

## "Starting from the second"

The bus exposes a resume cursor: `?since=<seq>` replays events whose `seq` is greater than the given
value. `seq` is a **single global sequence across all topics**, not per-topic. On a fresh bus,
events-publisher's three events get seq 1, 2 and 3, so `?since=1` skips the first and collects from
the second onward. Trade-off: after re-runs or other publishers the global numbering shifts, so
`since=1` means "everything after the first event ever", not "the second of the latest batch".

## Run it

This module declares `events-publisher` as a prerequisite (`dependsOn: events-publisher >=1.0.0`),
so install it after events-publisher — extending the demo's dependency chain to
**weather → events-publisher → events-collector**. It also relies on replay: even if
events-publisher published before this module starts, the `?since=1` replay backfills the stored
events.

Then install and start `events-collector` from the management UI (or the module API) and open its
log stream.

## Caveats

- **Keycloak issuer must match.** Like events-publisher, the container fetches its token from
  `host.docker.internal:8081` while core validates against its configured issuer-uri
  (`localhost:8081`); see [`events-publisher/README.md`](../events-publisher/README.md).
- **`collector.sh` must stay executable** so the nginx hook runs it directly.
- Reachability assumes core (`:8080`) and Keycloak (`:8081`) are on the Docker host, reachable via
  `host.docker.internal` (the runtime adds the host-gateway mapping).
