#!/bin/bash
set -euo pipefail

# Publica um evento no tópico do produtor, simulando o produtor real (filtro por type + raw delivery)
# Variáveis opcionais: EVENT_ID, SERVICE_ORDER_ID, CATALOG_IDS (separados por espaço), TYPE, TOPIC
# TOPIC é o nome do tópico (padrão: ofisy-billing-events.fifo)

LOCALSTACK_CONTAINER=${LOCALSTACK_CONTAINER:-ms-ofisy-execution-local-localstack}
TOPIC=${TOPIC:-ofisy-billing-events.fifo}
TOPIC_ARN=arn:aws:sns:us-east-1:000000000000:$TOPIC

EVENT_ID=${EVENT_ID:-$(uuidgen)}
SERVICE_ORDER_ID=${SERVICE_ORDER_ID:-$(uuidgen)}
CATALOG_IDS=${CATALOG_IDS:-"$(uuidgen) $(uuidgen)"}
TYPE=${TYPE:-QuoteApproved}

if [ "$TYPE" = "ServiceOrderCancelled" ]; then
  PAYLOAD="{}"
  RESEND="make event-service-order-cancelled EVENT_ID=$EVENT_ID SERVICE_ORDER_ID=$SERVICE_ORDER_ID"
else
  ITEMS=""
  for id in $CATALOG_IDS; do
    ITEMS+="{\"serviceCatalogId\":\"$id\"},"
  done
  PAYLOAD="{\"quoteId\":\"$(uuidgen)\",\"serviceItems\":[${ITEMS%,}]}"
  RESEND="make event-quote-approved EVENT_ID=$EVENT_ID SERVICE_ORDER_ID=$SERVICE_ORDER_ID CATALOG_IDS=\"$CATALOG_IDS\""
fi

MESSAGE=$(cat <<EOF
{"eventId":"$EVENT_ID","type":"$TYPE","serviceOrderId":"$SERVICE_ORDER_ID","occurredAt":"$(date -u +%Y-%m-%dT%H:%M:%SZ)","payload":$PAYLOAD}
EOF
)

docker exec "$LOCALSTACK_CONTAINER" awslocal sns publish \
  --topic-arn "$TOPIC_ARN" \
  --message-group-id "$SERVICE_ORDER_ID" \
  --message-deduplication-id "$(uuidgen)" \
  --message "$MESSAGE" > /dev/null

echo "Evento $TYPE publicado em $TOPIC"
echo "  EVENT_ID=$EVENT_ID"
echo "  SERVICE_ORDER_ID=$SERVICE_ORDER_ID"
[ "$TYPE" = "ServiceOrderCancelled" ] || echo "  CATALOG_IDS=\"$CATALOG_IDS\""
echo
echo "Reenviar o mesmo evento: $RESEND"
echo "Ver execuções da OS:     make db-executions SERVICE_ORDER_ID=$SERVICE_ORDER_ID"