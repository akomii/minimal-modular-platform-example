#!/bin/sh
# config-demo — prints its runtime configuration once, then exits so the container stops itself.
#
# Each value below is delivered by core as an env var named after the manifest "config" key, so this
# also exercises every config type end to end: string, number, boolean and secret.

echo "[config-demo] one-shot config dump:"
echo "[config-demo]   GREETING  (string)  = ${GREETING}"
echo "[config-demo]   REPEAT    (number)  = ${REPEAT}"
echo "[config-demo]   VERBOSE   (boolean) = ${VERBOSE}"
echo "[config-demo]   API_TOKEN (secret)  = ${API_TOKEN}"

if [ "${VERBOSE}" = "true" ]; then
  i=1
  while [ "${i}" -le "${REPEAT:-1}" ]; do
    echo "[config-demo]   ${i}/${REPEAT}: ${GREETING}"
    i=$((i + 1))
  done
fi

echo "[config-demo] done — the container now exits."
