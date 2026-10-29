# ms-ofisy-execution

FIAP 15SOAT - Tech Challenge - Grupo 81

Microsserviço responsável pela **execução dos serviços de uma ordem de serviço** da Ofisy. Ele consome o evento `QuoteApproved`, publicado pelo ms de billing quando um orçamento é aprovado, e cria uma execução `PENDING` para cada serviço do catálogo que foi aprovado. Também consome o `ServiceOrderCancelled`, publicado pelo core quando a OS é cancelada, e cancela as execuções ainda ativas da OS.

O contexto de execução foi extraído do core da Ofisy ([techchallenge-ofisy](https://github.com/15SOAT-FIAP/techchallenge-ofisy)), que continua existindo enquanto alguns contextos são estrangulados para microsserviços. Este ms participa da saga coordenada por coreografia, e as decisões por trás dessa arquitetura estão registradas nos ADRs do core:

- [ADR-0010 - Dividir a aplicação em microsserviços por bounded context](https://github.com/15SOAT-FIAP/techchallenge-ofisy/blob/master/docs/adr/0010-dividir-a-aplicacao-em-microsservicos-por-bounded-context.md)
- [ADR-0011 - Utilizar SNS e SQS como mensageria](https://github.com/15SOAT-FIAP/techchallenge-ofisy/blob/master/docs/adr/0011-utilizar-sns-e-sqs-como-mensageria.md)
- [ADR-0012 - Coordenar a saga por coreografia](https://github.com/15SOAT-FIAP/techchallenge-ofisy/blob/master/docs/adr/0012-coordenar-a-saga-por-coreografia.md)

---

## Estrutura do Projeto

O código segue a mesma organização em camadas do core: **Clean Architecture** com conceitos de **DDD**. Como o microsserviço tem um único agregado, os pacotes não são subdivididos por agregado.

```
br.com.ofisy.ms_ofisy_execution
├── domain/                       # Entidades, máquina de estados, portas de repositório e exceções de domínio
├── application/                  # Casos de uso
│   └── <caso-de-uso>/                # Interface do UseCase + Service com a implementação
├── adapters/                     # Conexão do domínio/aplicação com o mundo externo
│   ├── controllers/                  # Adaptadores de entrada: API REST
│   │   └── dto/                          # DTOs de resposta
│   ├── presenters/                   # Conversão do domínio para os DTOs de resposta
│   ├── gateways/                     # Adaptadores de saída: Entity JPA, Mapper, RepositoryImpl e interface Spring Data
│   ├── publishers/                   # Adaptadores de saída: publicação do outbox no SNS
│   └── listeners/                    # Adaptadores de entrada: consumo da fila SQS
│       ├── dto/                          # Envelope do evento e payloads
│       └── handlers/                     # Um EventHandler por tipo de evento
└── config/                       # Configurações do Spring e tratamento global de erros
```

---

## Stack do Projeto

| Tecnologia       | Versão   | Uso                                                                |
|------------------|----------|--------------------------------------------------------------------|
| Java             | 21 (LTS) | Linguagem da aplicação                                             |
| Spring Boot      | 4.1.1    | Framework base                                                     |
| Spring Cloud AWS | 4.2.0    | Integração com o Amazon SQS                                        |
| PostgreSQL       | 16       | Banco de dados das execuções                                       |
| LocalStack       | 4.13.1   | Emulação de SNS e SQS no ambiente local e nos testes de integração |
| Maven            | 3.9.x    | Build e gerenciamento de dependências                              |
| Docker           | —        | Containerização da aplicação e dos serviços dependentes            |

---

## Dependências

| Dependência          | Descrição                                                                | Artefato                                                                                                                  |
|----------------------|--------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------|
| Spring Cloud AWS SQS | Consumo das mensagens da fila via `@SqsListener`                         | `spring-cloud-aws-starter-sqs`                                                                                            |
| Spring Web MVC       | Servidor web e API REST                                                  | `spring-boot-starter-webmvc`                                                                                              |
| OpenAPI / Swagger    | Documentação da API e Swagger UI                                         | `springdoc-openapi-starter-webmvc-ui`                                                                                     |
| Spring Data JPA      | Persistência e mapeamento ORM                                            | `spring-boot-starter-data-jpa`                                                                                            |
| Spring Validation    | Validação de DTOs e beans                                                | `spring-boot-starter-validation`                                                                                          |
| Spring Actuator      | Endpoint de saúde (`/actuator/health`)                                   | `spring-boot-starter-actuator`                                                                                            |
| Flyway               | Versionamento do schema, com suporte ao dialeto do PostgreSQL            | `spring-boot-starter-flyway`, `flyway-database-postgresql`                                                                |
| PostgreSQL           | Driver JDBC                                                              | `postgresql`                                                                                                              |
| Docker Compose       | Sobe o `compose.db.yaml` automaticamente ao rodar a aplicação localmente | `spring-boot-docker-compose`                                                                                              |
| Lombok               | Redução de código boilerplate                                            | `lombok`                                                                                                                  |
| Spring Boot DevTools | Reinício automático durante o desenvolvimento                            | `spring-boot-devtools`                                                                                                    |
| Testcontainers       | PostgreSQL e LocalStack reais nos testes de integração                   | `spring-boot-testcontainers`, `testcontainers-postgresql`, `testcontainers-localstack`, `spring-cloud-aws-testcontainers` |
| Awaitility           | Asserções sobre o processamento assíncrono da fila                       | `awaitility`                                                                                                              |
| REST Assured         | Testes de integração da API contra a aplicação em porta aleatória        | `rest-assured`                                                                                                            |
| JaCoCo               | Relatório de cobertura e gate mínimo de 80%                              | `jacoco-maven-plugin`                                                                                                     |

---

## Arquivos de Configuração

### Aplicação e Build

- `application.yaml`: configuração base (datasource em `localhost:5433`, Flyway, região AWS e nome da fila). O perfil ativo por padrão é `dev`;
- `application-dev.yaml`: aponta o SDK da AWS para o LocalStack em `localhost:4566`;
- `application-docker.yaml`: configuração usada quando a aplicação roda dentro do `compose.yaml`;
- `pom.xml`: dependências e plugins do Maven.

### Docker

- `Dockerfile`: build multi-stage da aplicação;
- `compose.yaml`: ambiente completo (PostgreSQL, LocalStack e aplicação);
- `compose.db.yaml`: apenas PostgreSQL e LocalStack, para rodar a aplicação na IDE;
- `.dockerignore`: arquivos ignorados no build da imagem.

### Mensageria e utilitários locais

- `localstack/init/ready.d/01-messaging.sh`: cria tópico, fila, DLQ e assinatura no LocalStack. É o mesmo script usado nos testes de integração;
- `scripts/publish-event.sh`: publica um evento no tópico, simulando o produtor real;
- `Makefile`: atalhos para subir o ambiente, publicar eventos, inspecionar a fila/DLQ e consultar o banco (`make help`).

### Git e Ambiente

- `.gitignore` e `.gitattributes`;
- `.env`: variáveis sensíveis. **Não é versionado**;
- `env.example`: modelo do `.env`.

### CI (GitHub Actions)

- `.github/workflows/ci.yml`: build, testes e gate de cobertura (`mvn verify`) a cada push/PR na `master`;
- `.github/workflows/notify-pr.yml`: notifica abertura e fechamento de Pull Requests no Discord.

---

## Configuração do ambiente para execução

### 1. Clone o repositório

```bash
git clone git@github.com:15SOAT-FIAP/ms-ofisy-execution.git
```

### 2. Crie o arquivo `.env`

```bash
cp env.example .env
```

---

## Subindo a aplicação

### Opção 1: desenvolvimento local (dependências no Docker + aplicação na IDE)

```bash
make dev-up   # PostgreSQL em localhost:5433 e LocalStack em localhost:4566
make run      # aplicação com o perfil dev em localhost:8080
```

### Opção 2: ambiente completo via Docker

```bash
docker compose up --build
```

A aplicação fica disponível em `http://localhost:8081`.

### Health check

- `GET /actuator/health`

### API

A documentação interativa fica no Swagger UI, em `http://localhost:8080/swagger-ui.html` (ou na porta `8081` pelo `compose.yaml`).

Base: `/api/v1/service_order_executions`

| Método  | Rota                                         | Descrição                                                                                              |
|---------|----------------------------------------------|--------------------------------------------------------------------------------------------------------|
| `GET`   | `/`                                          | Lista as execuções, paginado (`page`, `size`, `sort`; 10 por página por padrão)                        |
| `GET`   | `/{id}`                                      | Busca uma execução. Responde `404` se não existir                                                      |
| `GET`   | `/?serviceCatalogId={id}`                    | Lista as execuções de um serviço do catálogo, paginado                                                 |
| `GET`   | `/?status={status}`                          | Lista as execuções de um status, paginado e sem diferenciar maiúsculas. Responde `400` se o status for inválido |
| `GET`   | `/service_order/{id}`                        | Lista as execuções de uma OS, paginado                                                                 |
| `GET`   | `/execution_time_average/{serviceCatalogId}` | Tempo médio, em minutos, das execuções finalizadas de um serviço do catálogo (`0.0` se não houver)     |
| `PATCH` | `/{id}/start`                                | Inicia uma execução `PENDING`. Responde `404` se não existir e `409` se não estiver `PENDING`          |
| `PATCH` | `/{id}/complete`                             | Conclui uma execução `IN_PROGRESS`. Responde `404` se não existir e `409` se não estiver `IN_PROGRESS` |

Não há rota de criação nem de cancelamento: as execuções são criadas pelo evento `QuoteApproved` e canceladas pelo evento `ServiceOrderCancelled` ([MESSAGING.md](docs/MESSAGING.md)).

Erros seguem o formato `ProblemDetail` (RFC 9457), com `title` e `detail`. A autenticação JWT ainda não está aplicada às rotas.

Para ver todos os comandos disponíveis, rode `make help`.

---

## Variáveis de ambiente

| Variável            | Descrição              |
|---------------------|------------------------|
| `POSTGRES_DB`       | Nome do banco de dados |
| `POSTGRES_USER`     | Usuário do banco       |
| `POSTGRES_PASSWORD` | Senha do banco         |

---

## Documentação Adicional

### Mensageria
- **[Mensageria](docs/MESSAGING.md)**: topologia SNS/SQS, contrato dos eventos, idempotência, DLQ e como exercitar o fluxo localmente

### Banco de Dados
- **[Banco de Dados](docs/DATABASE.md)**: tabelas, constraints e índices
- **[Guia de Flyway](docs/FLYWAY.md)**: como criar e executar migrations

### Qualidade e Testes
- **[Guia de Testes](docs/TESTING.md)**: testes unitários e de integração, Testcontainers e cobertura