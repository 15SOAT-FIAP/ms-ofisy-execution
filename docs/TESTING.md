# Guia de Testes

## Pré-requisitos

- Docker rodando localmente. Os testes que sobem o contexto do Spring usam **Testcontainers** para criar um PostgreSQL e um LocalStack reais, sem precisar do `compose`.

---

## Executando

| Comando                         | O que roda                                                                  |
|---------------------------------|-----------------------------------------------------------------------------|
| `make test` / `./mvnw test`     | Testes unitários (Surefire)                                                 |
| `make verify` / `./mvnw verify` | Unitários + integração (Failsafe) + relatório e gate de cobertura do JaCoCo |

A separação é feita pelo nome: classes terminadas em `IT` são excluídas do Surefire e executadas pelo Failsafe.

> O `MsOfisyExecutionApplicationTests` (context load) roda no Surefire, mas também usa Testcontainers. Por isso o Docker é necessário inclusive para `make test`.

---

## Estrutura dos testes

| Camada       | Testes                                                                                                  | Tipo                                                                        |
|--------------|---------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------|
| Domínio      | `ServiceOrderExecutionTest`, `ServiceOrderExecutionStatusTest`, `ProcessedEventTest`, `OutboxEventTest` | Unitário                                                                    |
| Casos de uso | Um `*ServiceTest` por caso de uso em `application/`                                                     | Unitário, com repositórios mockados                                         |
| Gateways     | `*MapperTest`, `*RepositoryImplTest`                                                                    | Unitário                                                                    |
| Gateways     | `JpaServiceOrderExecutionRepositoryIT`                                                                  | Integração (PostgreSQL real)                                                |
| Listeners    | `ExecutionEventsListenerTest`, `QuoteApprovedHandlerTest`                                               | Unitário                                                                    |
| Listeners    | `ExecutionEventsListenerIT`                                                                             | Integração (SQS + PostgreSQL reais)                                         |
| Publishers   | `OutboxEventPublisherTest`                                                                              | Unitário, com SNS e JPA mockados                                            |
| Publishers   | `OutboxEventPublisherIT`                                                                                | Integração (SNS + SQS + PostgreSQL reais)                                   |
| Controllers  | `ServiceOrderExecutionControllerTest`                                                                   | `@WebMvcTest` com `MockMvc` e casos de uso mockados                         |
| Controllers  | `ServiceOrderExecutionControllerIT`                                                                     | Integração com RestAssured (aplicação em porta aleatória + PostgreSQL real) |
| Presenters   | `ServiceOrderExecutionPresenterTest`                                                                    | Unitário                                                                    |
| Aplicação    | `MsOfisyExecutionApplicationTests`                                                                      | Context load                                                                |

### Testes de integração

O `ExecutionEventsListenerIT` publica mensagens direto na fila do LocalStack e usa **Awaitility** para esperar o processamento assíncrono. Cenários cobertos:

- o mesmo `QuoteApproved` entregue duas vezes cria as execuções só uma vez;
- um evento sem handler é confirmado e não altera o banco.

O `JpaServiceOrderExecutionRepositoryIT` usa `@DataJpaTest` contra o PostgreSQL do Testcontainers para validar a query nativa do tempo médio de execução. Cenários cobertos:

- retorna zero quando não há execuções;
- calcula a média em minutos considerando só execuções concluídas, ignorando as canceladas;
- trunca os minutos de cada execução antes de calcular a média;
- ignora execuções sem `started_at` ou `finished_at`;
- trata durações negativas como zero;
- ignora execuções de outros serviços do catálogo.

O `OutboxEventPublisherIT` grava um evento pelo `OutboxEventRepository` e espera recebê-lo na fila de inspeção `ofisy-execution-events-inspect.fifo`, assinada no tópico do ms. Ele confere o envelope recebido e que o registro foi marcado como publicado.

O `ServiceOrderExecutionControllerIT` sobe a aplicação em porta aleatória e chama a API com **RestAssured**, como os `*ControllerIT` do core. Cenários cobertos:

- iniciar a primeira execução de uma OS grava um `ExecutionStarted` no outbox, e iniciar a segunda não grava outro;
- iniciar uma execução já iniciada devolve `409`, sem novo evento;
- iniciar uma execução inexistente devolve `404`;
- concluir a primeira de duas execuções não grava evento, e concluir a segunda grava um `AllExecutionsCompleted`;
- concluir uma execução já concluída ou ainda `PENDING` devolve `409`, e uma inexistente devolve `404`;
- concluir as duas últimas execuções da OS em paralelo grava um único `AllExecutionsCompleted`, exercitando o lock no PostgreSQL real.

---

## Testcontainers

A configuração fica em `TestcontainersConfiguration` e é importada com `@Import(TestcontainersConfiguration.class)`:

- **PostgreSQL** via `@ServiceConnection`: o datasource é apontado para o container e o Flyway aplica as migrations ao subir o contexto.
- **LocalStack** (`sns`, `sqs`) via `@ServiceConnection`: o container recebe uma cópia do `localstack/init/ready.d/01-messaging.sh`, então fila, DLQ e assinatura são **as mesmas do ambiente local**. O container só é considerado pronto quando o init script termina.

### Rodando a aplicação com os containers de teste

O `TestMsOfisyExecutionApplication` (em `src/test`) sobe a aplicação usando os containers do Testcontainers no lugar do `compose`, o que é útil para depurar pela IDE sem precisar do `make dev-up`.

---

## Cobertura (JaCoCo)

- O relatório é gerado no `verify` em `target/site/jacoco/index.html`.
- O build **falha** se a cobertura do projeto ficar abaixo de **80% de linhas** ou **80% de branches**.
- Ficam fora da contagem: `MsOfisyExecutionApplication` e o pacote `config`.

---

## CI

O workflow `.github/workflows/ci.yml` roda `mvn -B verify` a cada push e Pull Request na `master`, com o mesmo gate de cobertura. O relatório do JaCoCo é publicado como artefato `jacoco-report` da execução, mesmo quando o build falha.