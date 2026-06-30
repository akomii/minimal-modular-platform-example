#!/bin/sh
# events-collector — subscribes to the platform event bus and prints received events to its console.
#
# Runs once at container start via the nginx /docker-entrypoint.d hook; the subscriber runs in the
# background so this script returns and nginx starts, keeping the container alive while events keep
# streaming into this container's logs (watch them live in the management UI). nginx is just a
# vehicle to run this script and keep the container alive.

CORE="http://host.docker.internal:8080"
KEYCLOAK="http://host.docker.internal:8081/realms/modular"
TOPIC="demo.ping"
# Resume cursor: core replays events with seq > SINCE, then tails live. seq is a single GLOBAL
# sequence across topics, so on a fresh bus events-publisher's three events are seq 1/2/3 and
# SINCE=1 collects from the second event onward. See this module's README.md.
SINCE=1

# nginx:alpine ships without curl
apk add --no-cache curl >/dev/null 2>&1

echo "[events-collector] requesting a client-credentials token as ${MODULE_OIDC_CLIENT_ID}"
TOKEN=$(curl -s -X POST "${KEYCLOAK}/protocol/openid-connect/token" \
  -d grant_type=client_credentials \
  -d "client_id=${MODULE_OIDC_CLIENT_ID}" \
  -d "client_secret=${MODULE_OIDC_CLIENT_SECRET}" \
  | sed -n 's/.*"access_token":"\([^"]*\)".*/\1/p')

if [ -z "${TOKEN}" ]; then
  echo "[events-collector] could not obtain a token — check that Keycloak is reachable and its issuer"
  echo "[events-collector] matches core's (see events-publisher/README.md); skipping the collector"
else
  echo "[events-collector] subscribing to '${TOPIC}' from seq > ${SINCE}; printing each received event"

  # Subscribe in the background and echo every SSE line into this container's log; the script then
  # returns so nginx starts and keeps the container alive while events keep arriving.
  curl -sN -H "Authorization: Bearer ${TOKEN}" \
    "${CORE}/api/events/${TOPIC}/stream?since=${SINCE}" \
    | while IFS= read -r line; do
        [ -n "${line}" ] && echo "[events-collector] <- ${line}"
      done &

  echo "[events-collector] subscriber running in the background; nginx keeps the container alive."
fi
