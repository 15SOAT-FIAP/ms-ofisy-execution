#!/bin/bash
set -euo pipefail

# Gera um JWT igual ao emitido pelo core (issuer, sub e claim role), para testar a API sem subir o core
# Variáveis: ROLE (obrigatória), EMAIL e TTL_SECONDS (opcionais). O JWT_SECRET vem do ambiente ou do .env

ROLE=${ROLE:?Informe ROLE=ADMIN|ATTENDANT|MECHANIC|STOCKMAN}
EMAIL=${EMAIL:-local@ofisy.com}
TTL_SECONDS=${TTL_SECONDS:-3600}

if [ -z "${JWT_SECRET:-}" ] && [ -f .env ]; then
  JWT_SECRET=$(grep -E '^JWT_SECRET=' .env | cut -d= -f2-)
fi
JWT_SECRET=${JWT_SECRET:?JWT_SECRET não encontrado no ambiente nem no .env}

base64url() {
  openssl base64 -A | tr '+/' '-_' | tr -d '='
}

NOW=$(date +%s)
HEADER=$(printf '{"alg":"HS256","typ":"JWT"}' | base64url)
PAYLOAD=$(printf '{"iss":"techchallenge-ofisy-api","sub":"%s","role":"%s","iat":%s,"exp":%s}' \
  "$EMAIL" "$ROLE" "$NOW" "$((NOW + TTL_SECONDS))" | base64url)
SIGNATURE=$(printf '%s.%s' "$HEADER" "$PAYLOAD" | openssl dgst -sha256 -hmac "$JWT_SECRET" -binary | base64url)

echo "$HEADER.$PAYLOAD.$SIGNATURE"