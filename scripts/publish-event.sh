#!/bin/bash
set -euo pipefail

# Publica um evento no tópico do billing, simulando o produtor real (filtro por type + raw delivery)
# Variáveis opcionais: EVENT_ID, SERVICE_ORDER_ID, CATALOG_IDS (separados por espaço), TYPE

LOCALSTACK_CONTAINER=${LOCALSTACK_CONTAINER:-ms-ofisy-execution-local-localstack}
TOPIC_ARN=arn:aws:sns:us-east-1:000000000000:ofisy-billing-events.fifo

EVENT_ID=${EVENT_ID:-$(uuidgen)}
SERVICE_ORDER_ID=${SERVICE_ORDER_ID:-$(uuidgen)}
CATALOG_IDS=${CATALOG_IDS:-"$(uuidgen) $(uuidgen)"}
TYPE=${TYPE:-QuoteApproved}

ITEMS=""
for id in $CATALOG_IDS; do
  ITEMS+="{\"serviceCatalogId\":\"$id\"},"
done

MESSAGE=$(cat <<EOF
{"eventId":"$EVENT_ID","type":"$TYPE","serviceOrderId":"$SERVICE_ORDER_ID","occurredAt":"$(date -u +%Y-%m-%dT%H:%M:%SZ)","payload":{"quoteId":"$(uuidgen)","serviceItems":[${ITEMS%,}]}}
EOF
)

docker exec "$LOCALSTACK_CONTAINER" awslocal sns publish \
  --topic-arn "$TOPIC_ARN" \
  --message-group-id "$SERVICE_ORDER_ID" \
  --message-deduplication-id "$(uuidgen)" \
  --message "$MESSAGE" > /dev/null

echo "Evento $TYPE publicado"
echo "  EVENT_ID=$EVENT_ID"
echo "  SERVICE_ORDER_ID=$SERVICE_ORDER_ID"
echo "  CATALOG_IDS=\"$CATALOG_IDS\""
echo
echo "Reenviar o mesmo evento: make event-quote-approved EVENT_ID=$EVENT_ID SERVICE_ORDER_ID=$SERVICE_ORDER_ID CATALOG_IDS=\"$CATALOG_IDS\""
echo "Ver execuções da OS:     make db-executions SERVICE_ORDER_ID=$SERVICE_ORDER_ID"