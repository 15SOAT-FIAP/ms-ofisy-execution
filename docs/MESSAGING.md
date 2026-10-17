# Mensageria

O ms-ofisy-execution se comunica com os demais microsserviços por eventos, usando **Amazon SNS + SQS** ([ADR-0011](https://github.com/15SOAT-FIAP/techchallenge-ofisy/blob/master/docs/adr/0011-utilizar-sns-e-sqs-como-mensageria.md)). Cada produtor publica no seu próprio tópico, e este ms recebe os eventos que lhe interessam por uma única fila FIFO. Os eventos que ele produz saem por um *transactional outbox* para o tópico `ofisy-execution-events.fifo`.

---

## Topologia

```mermaid
flowchart LR
    billing[ms de billing] -->|publica| topic[(SNS<br/>ofisy-billing-events.fifo)]
    topic -->|assinatura<br/>filtro type = QuoteApproved<br/>raw delivery| queue[(SQS<br/>ofisy-execution.fifo)]
    queue -->|após 5 recebimentos| dlq[(SQS<br/>ofisy-execution-dlq.fifo)]
    queue --> app[ms-ofisy-execution]
    app -->|outbox + publisher| execTopic[(SNS<br/>ofisy-execution-events.fifo)]
    execTopic -.->|só local, raw delivery| inspect[(SQS<br/>ofisy-execution-events-inspect.fifo)]
```

| Recurso          | Nome                                  | Observação                                                                               |
|------------------|---------------------------------------|------------------------------------------------------------------------------------------|
| Tópico           | `ofisy-billing-events.fifo`           | Pertence ao ms de billing na AWS. Localmente é criado só para exercitar o fluxo          |
| Assinatura       | tópico → fila                         | `RawMessageDelivery=true` e `FilterPolicy` aplicada ao corpo da mensagem (`MessageBody`) |
| Fila             | `ofisy-execution.fifo`                | Configurada em `ofisy.messaging.queues.execution` (`application.yaml`)                   |
| DLQ              | `ofisy-execution-dlq.fifo`            | `RedrivePolicy` com `maxReceiveCount=5`                                                  |
| Tópico           | `ofisy-execution-events.fifo`         | Tópico deste ms, configurado em `ofisy.messaging.topics.execution`                       |
| Fila de inspeção | `ofisy-execution-events-inspect.fifo` | Só local. Assinada no tópico deste ms para ver o que foi publicado                       |

Localmente, tudo é criado pelo `localstack/init/ready.d/01-messaging.sh`, que roda quando o LocalStack sobe. Com `queue-not-found-strategy: fail`, a aplicação não inicia se a fila não existir.

Na AWS, os recursos vêm do repositório de infraestrutura. O tópico é configurado pelo **nome**: o `SnsConfig` registra um `TopicsListingTopicArnResolver`, que descobre o ARN listando os tópicos (`sns:ListTopics`) em vez de chamar `CreateTopic`, e guarda o resultado em cache. A aplicação precisa de `sns:ListTopics` e `sns:Publish`.

---

## Contrato dos eventos

Todo evento segue o mesmo envelope (`adapters/listeners/dto/EventEnvelope.java`). O `payload` varia conforme o `type`:

| Campo            | Tipo      | Descrição                                                    |
|------------------|-----------|--------------------------------------------------------------|
| `eventId`        | UUID      | Identificador único do evento, usado para idempotência       |
| `type`           | String    | Tipo do evento, usado no filtro da assinatura e no despacho  |
| `serviceOrderId` | UUID      | Ordem de serviço à qual o evento se refere                   |
| `occurredAt`     | Instant   | Momento em que o evento ocorreu no produtor                  |
| `payload`        | JSON      | Dados específicos do tipo de evento                          |

### Eventos consumidos

| Evento          | Produtor       | Efeito                                                                           |
|-----------------|----------------|----------------------------------------------------------------------------------|
| `QuoteApproved` | ms de billing  | Cria uma execução `PENDING` para cada `serviceCatalogId` dos itens do orçamento  |

#### `QuoteApproved`

```json
{
  "eventId": "3f1c2a9e-8d4b-4b7a-9c1e-2f6d5a7b8c90",
  "type": "QuoteApproved",
  "serviceOrderId": "c3d4e5f6-a7b8-9012-cdef-123456780001",
  "occurredAt": "2026-10-13T10:00:00Z",
  "payload": {
    "quoteId": "7a8b9c0d-1e2f-4a3b-8c4d-5e6f7a8b9c0d",
    "serviceItems": [
      { "serviceCatalogId": "b1c2d3e4-f5a6-7890-bcde-f12345678901" },
      { "serviceCatalogId": "b1c2d3e4-f5a6-7890-bcde-f12345678902" }
    ]
  }
}
```

### Eventos publicados

Todos saem pelo outbox, descrito em [Publicação de eventos](#publicação-de-eventos-outbox).

| Evento             | Quando é publicado                                                 | Quem reage                                                 |
|--------------------|--------------------------------------------------------------------|------------------------------------------------------------|
| `ExecutionStarted` | Ao iniciar a **primeira** execução de uma OS (`PATCH /{id}/start`) | Core: move a OS de `AWAITING_EXECUTION` para `IN_PROGRESS` |

#### `ExecutionStarted`

No core, o início de uma execução só muda o status da OS quando ela ainda está em `AWAITING_EXECUTION`, ou seja, no início da primeira execução. O ms reproduz essa regra sem conhecer o status da OS: o evento sai apenas se **nenhuma execução da OS tem `startedAt` preenchido** antes deste início. Uma execução iniciada e depois cancelada mantém o `startedAt`, então um novo início não publica o evento outra vez, assim como o core não volta a mudar a OS.

O payload é vazio: o core só precisa do `serviceOrderId`, que já vem no envelope.

```json
{
  "eventId": "9b2f6c1e-4a7d-4e8b-a1c3-5d6e7f8a9b0c",
  "type": "ExecutionStarted",
  "serviceOrderId": "c3d4e5f6-a7b8-9012-cdef-123456780001",
  "occurredAt": "2026-10-17T13:00:00.123456Z",
  "payload": {}
}
```

**Concorrência:** dois inícios simultâneos de execuções da mesma OS podem gravar dois `ExecutionStarted`. Não há efeito duplicado, porque o core só faz a transição a partir de `AWAITING_EXECUTION` e ignora o segundo evento.

---

## Fluxo de processamento

```mermaid
sequenceDiagram
    participant SQS as SQS ofisy-execution.fifo
    participant L as ExecutionEventsListener
    participant H as QuoteApprovedHandler
    participant UC as CreateServiceOrderExecutionService
    participant DB as PostgreSQL

    SQS->>L: EventEnvelope
    L->>L: busca o handler pelo type
    alt sem handler para o type
        L-->>SQS: ack (log de warning, evento ignorado)
    else QuoteApproved
        L->>H: handle(envelope)
        H->>UC: execute(eventId, type, serviceOrderId, serviceCatalogIds)
        UC->>DB: processed_events contém o eventId?
        alt já processado
            UC-->>H: nenhuma execução criada
        else novo evento
            loop cada serviceCatalogId distinto ainda sem execução na OS
                UC->>DB: insert service_order_executions (PENDING)
            end
            UC->>DB: insert processed_events
        end
        H-->>L: ok
        L-->>SQS: ack
    end
```

O `ExecutionEventsListener` não conhece os eventos: ele recebe todos os `EventHandler` registrados no Spring e despacha pelo `type`. Cada tipo de evento tem seu próprio handler em `adapters/listeners/handlers/`.

### Ordenação

As mensagens são publicadas com `message-group-id = serviceOrderId`. Assim, eventos de uma mesma OS são processados em ordem, e OSs diferentes são processadas em paralelo.

### Idempotência

A entrega do SQS é *at-least-once*, então um mesmo evento pode chegar mais de uma vez (por exemplo, reentrega fora da janela de deduplicação de 5 minutos da fila FIFO). A proteção tem três camadas:

1. **`processed_events`**: o `eventId` é registrado na mesma transação que cria as execuções. Se já existir, o evento é descartado.
2. **Checagem por OS + serviço**: antes de criar cada execução, o caso de uso verifica se já existe uma para o par `serviceOrderId` + `serviceCatalogId`. Também remove IDs de catálogo duplicados do payload.
3. **Constraint `UNIQUE (service_order_id, service_catalog_id)`**: última barreira no banco ([DATABASE.md](DATABASE.md)).

### Falhas e DLQ

- **Tipo sem handler**: o evento é logado como ignorado e confirmado, sem passar pelo banco. Na prática isso só acontece se a `FilterPolicy` deixar passar um tipo que ainda não tem handler.
- **Mensagem inválida ou erro no processamento**: a exceção impede o ack, a mensagem volta para a fila depois do *visibility timeout* e, depois de 5 recebimentos, vai para a DLQ.

---

## Como adicionar um novo evento consumido

1. Criar o DTO do payload em `adapters/listeners/dto/`.
2. Criar um `@Component` em `adapters/listeners/handlers/` implementando `EventHandler`, com `type()` retornando o nome do evento.
3. Incluir o tipo na `FilterPolicy` da assinatura em `localstack/init/ready.d/01-messaging.sh` (e na infraestrutura da AWS). Se o evento vier de outro produtor, criar a assinatura no tópico correspondente.
4. Cobrir com teste unitário do handler e, se fizer sentido, com um cenário no `ExecutionEventsListenerIT`.
5. Adicionar a linha na tabela de [eventos consumidos](#eventos-consumidos) deste documento.

---

## Publicação de eventos (outbox)

Nenhum caso de uso publica direto no SNS. Seguindo a [RFC-0008](https://github.com/15SOAT-FIAP/techchallenge-ofisy/blob/master/docs/rfc/0008-coordenar-a-saga-entre-os-microsservicos.md), o caso de uso grava o evento na tabela `outbox_events`, na mesma transação da mudança de estado, e um publicador agendado o envia ao tópico. Se a transação falhar, o evento não é gravado; se o SNS estiver fora, o evento espera no banco.

```mermaid
sequenceDiagram
    participant UC as Caso de uso
    participant DB as PostgreSQL
    participant P as OutboxEventPublisher
    participant SNS as SNS ofisy-execution-events.fifo

    UC->>DB: muda o estado + insert outbox_events (mesma transação)
    loop a cada publish-interval
        P->>DB: select pendentes for update, em ordem de occurred_at
        loop cada evento do lote
            P->>SNS: envelope (groupId = serviceOrderId, deduplicationId = eventId)
            P->>DB: published_at = now()
        end
    end
```

| Peça                                    | Papel                                                                     |
|-----------------------------------------|---------------------------------------------------------------------------|
| `domain/OutboxEvent`                    | O evento a publicar: tipo, OS e payload. Id e `occurredAt` são gerados    |
| `domain/OutboxEventRepository`          | Porta usada pelos casos de uso. Só tem `save`                             |
| `adapters/gateways/OutboxEventRepositoryImpl` | Serializa o payload para JSON e grava em `outbox_events`            |
| `adapters/publishers/OutboxEventPublisher`    | Lê os pendentes, publica o envelope no SNS e marca `published_at`   |

O envelope publicado é o mesmo `EventEnvelope` que este ms consome, então quem assina o tópico recebe o mesmo contrato descrito em [Contrato dos eventos](#contrato-dos-eventos).

### Garantias

- **Ordem**: os pendentes são publicados em ordem de `occurred_at`, com `message-group-id = serviceOrderId`. Quando um evento falha, o publicador loga e **pula os eventos seguintes da mesma OS** naquele lote, para não publicá-los fora de ordem. Os eventos das outras OSs continuam sendo publicados normalmente. O evento que falhou é tentado de novo no próximo ciclo.
- **Evento que sempre falha**: um evento com falha permanente (por exemplo, payload inválido) é tentado a cada ciclo e segura os eventos seguintes da sua OS, mas não bloqueia as outras OSs. Ainda não há limite de tentativas nem status de erro.
- **Mais de uma réplica**: a busca dos pendentes usa `SELECT ... FOR UPDATE`. Uma segunda réplica espera o lote da primeira terminar e já encontra os eventos marcados, sem publicar fora de ordem.
- **Duplicidade**: se a aplicação cair entre publicar e gravar `published_at`, o evento é publicado de novo. Dentro da janela de 5 minutos, a deduplicação FIFO (`deduplicationId = eventId`) descarta a cópia; depois disso, os consumidores precisam ser idempotentes pelo `eventId`.
- **Retenção**: os eventos publicados continuam na tabela, com `published_at` preenchido. Ainda não há limpeza automática.

### Configuração

| Propriedade                               | Padrão                        | Descrição                              |
|-------------------------------------------|-------------------------------|----------------------------------------|
| `ofisy.messaging.topics.execution`        | `ofisy-execution-events.fifo` | Nome do tópico onde os eventos saem    |
| `ofisy.messaging.outbox.publish-interval` | `1s`                          | Intervalo entre o fim de um ciclo e o início do próximo |
| `ofisy.messaging.outbox.batch-size`       | `100`                         | Máximo de eventos publicados por ciclo |

### Como publicar um novo evento

1. Criar um `record` com o payload do evento.
2. No caso de uso, dentro da transação que muda o estado, chamar `outboxEventRepository.save(OutboxEvent.create("NomeDoEvento", serviceOrderId, payload))`.
3. Cobrir no teste do caso de uso que o evento é gravado e adicionar a linha na tabela de [eventos publicados](#eventos-publicados) deste documento.

---

## Exercitando o fluxo localmente

Com o ambiente de desenvolvimento no ar (`make dev-up` + `make run`):

| Comando                     | O que faz                                                                                                          |
|-----------------------------|--------------------------------------------------------------------------------------------------------------------|
| `make event-quote-approved` | Publica um `QuoteApproved` no tópico. Aceita `EVENT_ID`, `SERVICE_ORDER_ID` e `CATALOG_IDS` (separados por espaço) |
| `make event-ignored`        | Publica um `QuoteRejected`, que a `FilterPolicy` descarta antes de chegar na fila                                  |
| `make event-invalid`        | Envia uma mensagem inválida direto na fila, que vai para a DLQ após 5 tentativas (~2,5 min)                        |
| `make queue-status`         | Mostra as mensagens visíveis e em processamento na fila e na DLQ                                                   |
| `make dlq-peek`             | Lista as mensagens da DLQ sem removê-las                                                                           |
| `make dlq-redrive`          | Devolve as mensagens da DLQ para a fila principal                                                                  |
| `make dlq-purge`            | Apaga todas as mensagens da DLQ                                                                                    |
| `make db-executions`        | Lista as execuções de uma OS (`SERVICE_ORDER_ID` obrigatório)                                                      |
| `make db-processed-events`  | Lista os últimos eventos processados                                                                               |
| `make db-outbox`            | Lista os últimos eventos do outbox e se já foram publicados                                                        |
| `make events-peek`          | Lista os eventos publicados no tópico deste ms, pela fila de inspeção                                              |

### Exemplo: verificando a idempotência

```bash
make event-quote-approved
# o script imprime o comando para reenviar o mesmo evento, por exemplo:
make event-quote-approved EVENT_ID=<id> SERVICE_ORDER_ID=<os> CATALOG_IDS="<c1> <c2>"

make db-executions SERVICE_ORDER_ID=<os>   # continua com 2 execuções
make db-processed-events                   # o eventId aparece uma única vez
```

> **Atenção ao `dlq-redrive`:** a mensagem volta com o mesmo *deduplication id*. Se o envio original tiver menos de 5 minutos, a fila FIFO a descarta como duplicada.