#!/bin/sh
# Render nginx config with the container's real DNS (Podman vs Docker) then exec nginx.
set -eu

TEMPLATE="${MLAIR_NGINX_TEMPLATE:-/etc/nginx/templates/default.conf.template}"
OUTPUT="${MLAIR_NGINX_OUTPUT:-/etc/nginx/conf.d/default.conf}"

RESOLVER="${MLAIR_DNS_RESOLVER:-}"
if [ -z "$RESOLVER" ]; then
  RESOLVER=$(awk '/^nameserver/{print $2; exit}' /etc/resolv.conf 2>/dev/null || true)
fi
if [ -z "$RESOLVER" ]; then
  RESOLVER="127.0.0.11"
fi

if [ ! -f "$TEMPLATE" ]; then
  echo "mlair-browser-gateway: missing template $TEMPLATE" >&2
  exit 1
fi

sed "s|@MLAIR_DNS_RESOLVER@|${RESOLVER}|g" "$TEMPLATE" > "$OUTPUT"
echo "mlair-browser-gateway: resolver=${RESOLVER} config=${OUTPUT}"

exec nginx -g 'daemon off;'
