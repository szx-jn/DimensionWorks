#!/usr/bin/env bash
# Keep Java's HTTPS validation aligned with the hosted runner's trusted OS CAs.
set -euo pipefail
test -n "${JAVA_HOME:-}"
dw_system_trust=/etc/ssl/certs/java/cacerts
if [[ ! -r "$dw_system_trust" ]]; then
  sudo apt-get update -qq
  sudo apt-get install -y ca-certificates-java
fi
dw_trust_log="$(mktemp)"
if ! sudo "$JAVA_HOME/bin/keytool" -importkeystore -noprompt \
  -srckeystore "$dw_system_trust" -srcstorepass changeit \
  -destkeystore "$JAVA_HOME/lib/security/cacerts" -deststorepass changeit \
  >"$dw_trust_log" 2>&1; then
  tail -30 "$dw_trust_log"
  rm -f "$dw_trust_log"
  exit 1
fi
rm -f "$dw_trust_log"
# Check the actual dependency endpoint with certificate verification enabled.
curl -fsSI --retry 3 --connect-timeout 20 \
  https://maven.createmod.net/com/simibubi/create/create-1.20.1/6.0.8-291/create-1.20.1-6.0.8-291-slim.jar \
  >/dev/null
echo 'Java system trust prepared; Maven HTTPS verified.'
