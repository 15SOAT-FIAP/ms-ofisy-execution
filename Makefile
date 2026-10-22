.PHONY: help dev-up dev-down run test verify token \
	event-quote-approved event-service-order-cancelled event-ignored event-invalid \
	queue-status dlq-peek dlq-redrive dlq-purge events-peek \
	db-executions db-processed-events db-outbox

LOCALSTACK_CONTAINER ?= ms-ofisy-execution-local-localstack
DB_CONTAINER ?= ms-ofisy-execution-local-db

AWS := docker exec $(LOCALSTACK_CONTAINER) awslocal
QUEUE_NAME := ofisy-execution.fifo
DLQ_NAME := ofisy-execution-dlq.fifo
INSPECT_QUEUE_NAME := ofisy-execution-events-inspect.fifo
QUEUE_ARN := arn:aws:sqs:us-east-1:000000000000:$(QUEUE_NAME)
DLQ_ARN := arn:aws:sqs:us-east-1:000000000000:$(DLQ_NAME)

queue_url = $$($(AWS) sqs get-queue-url --queue-name $(1) --query QueueUrl --output text)
psql = set -a && . ./.env && set +a && \
	docker exec $(DB_CONTAINER) psql -U "$$POSTGRES_USER" -d "$$POSTGRES_DB" -c

help:
	@grep -E '^[a-z-]+:.*## ' $(MAKEFILE_LIST) | awk -F':.*## ' '{printf "  %-22s %s\n", $$1, $$2}'

dev-up: ## Sobe Postgres e LocalStack (compose.db.yaml)
	@docker compose -f compose.db.yaml up -d --wait

dev-down: ## Derruba o ambiente local
	@docker compose -f compose.db.yaml down

run: ## Roda a aplicação com o perfil dev
	@./mvnw spring-boot:run

test: ## Roda os testes unitários
	@./mvnw test

verify: ## Roda testes unitários, de integração e o gate de cobertura
	@./mvnw verify

token: ## Gera um JWT local no formato do core (ROLE obrigatório: ADMIN, ATTENDANT, MECHANIC ou STOCKMAN)
	@ROLE=$(ROLE) EMAIL=$(EMAIL) scripts/generate-token.sh

event-quote-approved: ## Publica um QuoteApproved no SNS (EVENT_ID, SERVICE_ORDER_ID, CATALOG_IDS opcionais)
	@LOCALSTACK_CONTAINER=$(LOCALSTACK_CONTAINER) EVENT_ID=$(EVENT_ID) SERVICE_ORDER_ID=$(SERVICE_ORDER_ID) \
	CATALOG_IDS="$(CATALOG_IDS)" TYPE=QuoteApproved scripts/publish-event.sh

event-service-order-cancelled: ## Publica um ServiceOrderCancelled no tópico de OS (SERVICE_ORDER_ID obrigatório, EVENT_ID opcional)
	@test -n "$(SERVICE_ORDER_ID)" || (echo "Informe SERVICE_ORDER_ID=<uuid>" && exit 1)
	@LOCALSTACK_CONTAINER=$(LOCALSTACK_CONTAINER) EVENT_ID=$(EVENT_ID) SERVICE_ORDER_ID=$(SERVICE_ORDER_ID) \
	TOPIC=ofisy-service-order-events.fifo TYPE=ServiceOrderCancelled scripts/publish-event.sh

event-ignored: ## Publica um QuoteRejected no SNS, que o filtro da assinatura deve descartar
	@LOCALSTACK_CONTAINER=$(LOCALSTACK_CONTAINER) TYPE=QuoteRejected scripts/publish-event.sh

event-invalid: ## Envia uma mensagem inválida direto na fila, que termina na DLQ após 5 tentativas
	@$(AWS) sqs send-message --queue-url $(call queue_url,$(QUEUE_NAME)) \
		--message-group-id invalid-$$(uuidgen) --message-deduplication-id $$(uuidgen) \
		--message-body '{"eventId":"not-a-uuid","type":"QuoteApproved"}' > /dev/null
	@echo "Mensagem inválida enviada. Acompanhe com 'make queue-status' (vai para a DLQ em ~2,5 min)"

queue-status: ## Mostra quantas mensagens há na fila e na DLQ
	@for q in $(QUEUE_NAME) $(DLQ_NAME); do \
		url=$$($(AWS) sqs get-queue-url --queue-name $$q --query QueueUrl --output text); \
		$(AWS) sqs get-queue-attributes --queue-url $$url \
			--attribute-names ApproximateNumberOfMessages ApproximateNumberOfMessagesNotVisible \
			--query 'Attributes.[ApproximateNumberOfMessages,ApproximateNumberOfMessagesNotVisible]' --output text \
		| awk -v q=$$q '{printf "  %-28s visíveis=%s  em processamento=%s\n", q, $$1, $$2}'; \
	done

dlq-peek: ## Lista as mensagens da DLQ sem removê-las
	@$(AWS) sqs receive-message --queue-url $(call queue_url,$(DLQ_NAME)) \
		--max-number-of-messages 10 --visibility-timeout 0 --attribute-names All \
		--query 'Messages[].{body:Body,receiveCount:Attributes.ApproximateReceiveCount}' --output json

dlq-redrive: ## Devolve as mensagens da DLQ para a fila principal
	@$(AWS) sqs start-message-move-task --source-arn $(DLQ_ARN) --destination-arn $(QUEUE_ARN) > /dev/null
	@echo "Redrive iniciado. Acompanhe com 'make queue-status'"
	@echo "Atenção: a mensagem volta com o mesmo deduplication id; se faz menos de 5 min do envio original, a fila FIFO a descarta"

dlq-purge: ## Apaga todas as mensagens da DLQ
	@$(AWS) sqs purge-queue --queue-url $(call queue_url,$(DLQ_NAME))
	@echo "DLQ esvaziada"

events-peek: ## Lista os eventos publicados no tópico do ms, pela fila de inspeção local
	@$(AWS) sqs receive-message --queue-url $(call queue_url,$(INSPECT_QUEUE_NAME)) \
		--max-number-of-messages 10 --visibility-timeout 0 \
		--query 'Messages[].Body' --output json

db-executions: ## Lista as execuções de uma OS (SERVICE_ORDER_ID obrigatório)
	@test -n "$(SERVICE_ORDER_ID)" || (echo "Informe SERVICE_ORDER_ID=<uuid>" && exit 1)
	@$(psql) "select id, service_catalog_id, status, created_at from service_order_executions where service_order_id = '$(SERVICE_ORDER_ID)';"

db-processed-events: ## Lista os últimos eventos processados
	@$(psql) "select * from processed_events order by processed_at desc limit 10;"

db-outbox: ## Lista os últimos eventos do outbox e se já foram publicados
	@$(psql) "select event_id, event_type, service_order_id, occurred_at, published_at from outbox_events order by occurred_at desc limit 10;"
