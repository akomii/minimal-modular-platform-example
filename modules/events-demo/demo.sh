#!/bin/sh
# events-demo — exercises the platform event bus end-to-end as a real module identity.
#
# Runs once at container start via the nginx /docker-entrypoint.d hook; nginx then stays up so the
# background subscriber keeps streaming received events into this container's logs (watch them live
# in the management UI). nginx is just a vehicle to run this script and keep the container alive.

CORE="http://host.docker.internal:8080"
KEYCLOAK="http://host.docker.internal:8081/realms/modular"
TOPIC="demo.ping"

# nginx:alpine ships without curl
apk add --no-cache curl >/dev/null 2>&1

echo "[events-demo] requesting a client-credentials token as ${MODULE_OIDC_CLIENT_ID}"
TOKEN=$(curl -s -X POST "${KEYCLOAK}/protocol/openid-connect/token" \
  -d grant_type=client_credentials \
  -d "client_id=${MODULE_OIDC_CLIENT_ID}" \
  -d "client_secret=${MODULE_OIDC_CLIENT_SECRET}" \
  | sed -n 's/.*"access_token":"\([^"]*\)".*/\1/p')

if [ -z "${TOKEN}" ]; then
  echo "[events-demo] could not obtain a token — check that Keycloak is reachable and its issuer"
  echo "[events-demo] matches core's (see this module's README.md); skipping the demo"
else
  echo "[events-demo] subscribing to '${TOPIC}' (replaying from the start), then publishing 3 events"

  # Subscribe in the background and echo every SSE line into this container's log.
  curl -sN -H "Authorization: Bearer ${TOKEN}" \
    "${CORE}/api/events/${TOPIC}/stream?since=0" \
    | while IFS= read -r line; do
        [ -n "${line}" ] && echo "[events-demo] <- ${line}"
      done &

  # Let the subscription attach, then publish a few events; each call returns its assigned seq.
  sleep 2
  n=1
  while [ "${n}" -le 3 ]; do
    result=$(curl -s -X POST "${CORE}/api/events/${TOPIC}" \
      -H "Authorization: Bearer ${TOKEN}" \
      -H "Content-Type: application/json" \
      -d "{\"from\":\"events-demo\",\"n\":${n}}")
    echo "[events-demo] -> published #${n} ${result}"
    n=$((n + 1))
    sleep 1
  done

  echo "[events-demo] done publishing; the subscriber stays live — watch this module's logs."
fi
