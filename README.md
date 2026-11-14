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
│   ├── tracing/                      # Propagação do contexto do trace pelo outbox
│   └── listeners/                    # Adaptadores de entrada: consumo da fila SQS
│       ├── dto/                          # Envelope do evento e payloads
│       └── handlers/                     # Um EventHandler por tipo de evento
├── config/                       # Configurações do Spring, segurança, JWT, Swagger e tratamento global de erros
└── shared/                       # Validação do JWT (JwtService) e filtro de autenticação (JwtAuthFilter)
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
| Spring Security      | Autorização das rotas por role                                           | `spring-boot-starter-security`                                                                                            |
| JJWT                 | Validação do JWT emitido pelo core                                       | `jjwt-api`, `jjwt-impl`, `jjwt-jackson`                                                                                   |
| Spring Data JPA      | Persistência e mapeamento ORM                                            | `spring-boot-starter-data-jpa`                                                                                            |
| Spring Validation    | Validação de DTOs e beans                                                | `spring-boot-starter-validation`                                                                                          |
| Spring Actuator      | Endpoint de saúde (`/actuator/health`)                                   | `spring-boot-starter-actuator`                                                                                            |
| Micrometer Datadog   | Exportação de métricas para o Datadog (ativa só no perfil `k8s`)         | `micrometer-registry-datadog`                                                                                             |
| Logstash Encoder     | Logs em JSON com correlação de traces do Datadog no perfil `k8s`         | `logstash-logback-encoder`                                                                                                |
| OpenTelemetry API    | Propaga o contexto do trace pelo outbox, conectada ao `dd-java-agent`     | `opentelemetry-api`                                                                                                       |
| Flyway               | Versionamento do schema, com suporte ao dialeto do PostgreSQL            | `spring-boot-starter-flyway`, `flyway-database-postgresql`                                                                |
| PostgreSQL           | Driver JDBC                                                              | `postgresql`                                                                                                              |
| Docker Compose       | Sobe o `compose.db.yaml` automaticamente ao rodar a aplicação localmente | `spring-boot-docker-compose`                                                                                              |
| Lombok               | Redução de código boilerplate                                            | `lombok`                                                                                                                  |
| Spring Boot DevTools | Reinício automático durante o desenvolvimento                            | `spring-boot-devtools`                                                                                                    |
| Testcontainers       | PostgreSQL e LocalStack reais nos testes de integração                   | `spring-boot-testcontainers`, `testcontainers-postgresql`, `testcontainers-localstack`, `spring-cloud-aws-testcontainers` |
| Awaitility           | Asserções sobre o processamento assíncrono da fila                       | `awaitility`                                                                                                              |
| REST Assured         | Testes de integração da API contra a aplicação em porta aleatória        | `rest-assured`                                                                                                            |
| Spring Security Test | Usuário simulado nos testes `@WebMvcTest`                                | `spring-security-test`                                                                                                    |
| JaCoCo               | Relatório de cobertura e gate mínimo de 80%                              | `jacoco-maven-plugin`                                                                                                     |

---

## Arquivos de Configuração

### Aplicação e Build

- `application.yaml`: configuração base (datasource em `localhost:5433`, Flyway, região AWS e nome da fila). O perfil ativo por padrão é `dev`;
- `application-dev.yaml`: aponta o SDK da AWS para o LocalStack em `localhost:4566`;
- `application-docker.yaml`: configuração usada quando a aplicação roda dentro do `compose.yaml`;
- `application-k8s.yaml`: configuração usada no EKS (RDS via `POSTGRES_HOST`, probes do Kubernetes e AWS real);
- `logback-spring.xml`: logs em JSON para o Datadog no perfil `k8s` e console padrão nos demais;
- `pom.xml`: dependências e plugins do Maven.

### Docker

- `Dockerfile`: build multi-stage da aplicação. A imagem inclui o `dd-java-agent` do Datadog com versão e checksum fixos, ativado só no k8s via `JAVA_TOOL_OPTIONS`;
- `compose.yaml`: ambiente completo (PostgreSQL, LocalStack e aplicação);
- `compose.db.yaml`: apenas PostgreSQL e LocalStack, para rodar a aplicação na IDE;
- `.dockerignore`: arquivos ignorados no build da imagem.

### Kubernetes

- `k8s/`: manifestos do deploy no EKS (`configmap`, `secret`, `deployment`, `service` e `hpa`). Ver [Deploy no EKS](#deploy-no-eks).

### Mensageria e utilitários locais

- `localstack/init/ready.d/01-messaging.sh`: cria tópico, fila, DLQ e assinatura no LocalStack. É o mesmo script usado nos testes de integração;
- `scripts/publish-event.sh`: publica um evento no tópico, simulando o produtor real;
- `scripts/generate-token.sh`: gera um JWT local no mesmo formato do core (`make token ROLE=...`), para testar a API sem subir o core;
- `Makefile`: atalhos para subir o ambiente, publicar eventos, inspecionar a fila/DLQ e consultar o banco (`make help`).

### Git e Ambiente

- `.gitignore` e `.gitattributes`;
- `.env`: variáveis sensíveis. **Não é versionado**;
- `env.example`: modelo do `.env`.

### CI/CD (GitHub Actions)

- `.github/workflows/ci.yml`: build, testes e gate de cobertura (`mvn verify`) a cada push/PR na `master`;
- `.github/workflows/cd.yml`: deploy manual no EKS. Ver [Deploy no EKS](#deploy-no-eks);
- `.github/workflows/destroy-app.yml`: remove manualmente os recursos da aplicação do cluster;
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

| Método  | Rota                                         | Descrição                                                                                                       |
|---------|----------------------------------------------|-----------------------------------------------------------------------------------------------------------------|
| `GET`   | `/`                                          | Lista as execuções, paginado (`page`, `size`, `sort`; 10 por página por padrão)                                 |
| `GET`   | `/{id}`                                      | Busca uma execução. Responde `404` se não existir                                                               |
| `GET`   | `/?serviceCatalogId={id}`                    | Lista as execuções de um serviço do catálogo, paginado                                                          |
| `GET`   | `/?status={status}`                          | Lista as execuções de um status, paginado e sem diferenciar maiúsculas. Responde `400` se o status for inválido |
| `GET`   | `/service_order/{id}`                        | Lista as execuções de uma OS, paginado                                                                          |
| `GET`   | `/execution_time_average/{serviceCatalogId}` | Tempo médio, em minutos, das execuções finalizadas de um serviço do catálogo (`0.0` se não houver)              |
| `PATCH` | `/{id}/start`                                | Inicia uma execução `PENDING`. Responde `404` se não existir e `409` se não estiver `PENDING`                   |
| `PATCH` | `/{id}/complete`                             | Conclui uma execução `IN_PROGRESS`. Responde `404` se não existir e `409` se não estiver `IN_PROGRESS`          |

Não há rota de criação nem de cancelamento: as execuções são criadas pelo evento `QuoteApproved` e canceladas pelo evento `ServiceOrderCancelled` ([MESSAGING.md](docs/MESSAGING.md)).

Todas as rotas exigem um JWT emitido pelo core com role `ADMIN`, `ATTENDANT` ou `MECHANIC`. Sem token válido a resposta é `401`, e com outra role é `403` ([SECURITY.md](docs/SECURITY.md)). Para testar localmente, gere um token com `make token ROLE=MECHANIC`.

Erros seguem o formato `ProblemDetail` (RFC 9457), com `title` e `detail`.

Para ver todos os comandos disponíveis, rode `make help`.

---

## Variáveis de ambiente

| Variável                           | Descrição                                                    |
|------------------------------------|--------------------------------------------------------------|
| `POSTGRES_DB`                      | Nome do banco de dados                                       |
| `POSTGRES_USER`                    | Usuário do banco                                             |
| `POSTGRES_PASSWORD`                | Senha do banco                                               |
| `JWT_SECRET`                       | Chave HS256 para validar o JWT. Precisa ser a mesma do core  |
| `POSTGRES_HOST`                    | Host do banco no perfil `k8s`                                |
| `DD_METRICS_ENABLED`, `DD_API_KEY` | Exportação de métricas para o Datadog (desligada por padrão) |
| `DD_TRACE_OTEL_ENABLED`            | Conecta a API do OpenTelemetry ao `dd-java-agent`, necessária para o trace seguir pelo outbox ([MESSAGING.md](docs/MESSAGING.md#rastreamento-distribuído-pelo-outbox)) |

---

## Deploy no EKS

A infraestrutura vem de outros repositórios:

- `techchallenge-ofisy-eks-infra` (`eks/execution`): cluster `ms-ofisy-execution-cluster`, ECR `ms-ofisy-execution-ecr` e Datadog Agent;
- `techchallenge-ofisy-db-infra` (`execution`): RDS `ms-ofisy-execution-db`, banco `ms_ofisy_execution_db`.

### Pelo workflow (padrão)

Rode o workflow **Deploy Manual na AWS (CD)** (`cd.yml`) informando a branch, tag ou commit. Ele:

1. espera o RDS ficar disponível e busca o endereço dele. Se o banco não existir, o deploy para com erro;
2. instala o Metrics Server, usado pelo HPA;
3. builda a imagem e envia ao ECR com a tag do commit curto. Se a tag já existir, reaproveita a imagem, porque o ECR não aceita sobrescrever tags;
4. substitui os placeholders dos manifestos, aplica no cluster e espera o rollout. A annotation `checksum/config` recebe o hash do ConfigMap e do Secret, então os pods são recriados quando a imagem ou a configuração mudam, mesmo rodando o CD de novo no mesmo commit.

Usa o environment `AWS_ACADEMY` com os secrets `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_SESSION_TOKEN`, `AWS_ACCOUNT_ID`, `DB_PASSWORD`, `JWT_SECRET` e `DD_API_KEY`.

Para remover a aplicação do cluster (por exemplo, antes de destruir o EKS), rode **Destruir Aplicação no Kubernetes (EKS)** (`destroy-app.yml`). Se o cluster já não existir, ele termina com sucesso sem fazer nada, então pode ser executado de novo sem quebrar.

#### Orquestração pelo ofisy-platform

Além do `workflow_dispatch`, o `cd.yml` e o `destroy-app.yml` aceitam `workflow_call`, e o repositório [ofisy-platform](https://github.com/15SOAT-FIAP/ofisy-platform) os chama pelo `up.yml` (sobe o ambiente) e pelo `down.yml` (destrói), na etapa dos microsserviços da ordem descrita no `techchallenge-ofisy-eks-infra`. Os secrets chegam por `secrets: inherit`, e o input `ref` é o mesmo do disparo manual. O checkout sempre aponta para este repositório, mesmo quando o workflow é chamado de outro.

Nenhuma etapa recebe outputs deste repositório: o cluster, o ECR e o RDS são encontrados pelo nome, e o API Gateway encontra o NLB pela tag do `Service`.

#### Exposição pelo API Gateway

O `Service` cria um NLB interno, e o API Gateway do `techchallenge-ofisy-eks-infra` encontra esse NLB pela tag `kubernetes.io/service-name`. Depois do primeiro deploy, altere o `api-gateway/` desse repositório:

1. em `variables.tf`, inclua o serviço no default de `nlb_service_names`:
   ```hcl
   execution = "default/ms-ofisy-execution-service"
   ```
2. em `main.tf`, inclua as rotas do execution em `local.service_routes`. Sem elas, as requisições continuam caindo no `ANY /{proxy+}`, que vai para o core:
   ```hcl
   "ANY /api/v1/service_order_executions"          = { service = "execution", protected = false }
   "ANY /api/v1/service_order_executions/{proxy+}" = { service = "execution", protected = false }
   ```
   `protected = false` porque o JWT é validado pela própria aplicação, e não pelo authorizer de clientes do Gateway;
3. rode o deploy do API Gateway.

Cada vez que o `Service` é recriado (por exemplo, depois do `destroy-app.yml`), o NLB muda e o deploy do API Gateway precisa rodar de novo. Antes de destruir a aplicação sem recriá-la, tire a entrada `execution` de `nlb_service_names`, senão o próximo deploy do Gateway falha ao procurar o NLB.

### Manualmente

Os manifestos em `k8s/` têm placeholders que precisam ser substituídos antes do `kubectl apply`:

| Arquivo          | Placeholder                                                                            | Valor                                                                    |
|------------------|----------------------------------------------------------------------------------------|--------------------------------------------------------------------------|
| `configmap.yml`  | `<RDS_ADDRESS>`                                                                        | Endereço do RDS `ms-ofisy-execution-db`                                  |
| `configmap.yml`  | `<IMAGE_TAG>`                                                                          | Tag da imagem, usada como `DD_VERSION`                                   |
| `deployment.yml` | `<IMAGE_TAG>` (label `tags.datadoghq.com/version`)                                     | Mesma tag da imagem                                                      |
| `deployment.yml` | `<ECR_REPOSITORY_URL>:<IMAGE_TAG>`                                                     | `<account>.dkr.ecr.us-east-1.amazonaws.com/ms-ofisy-execution-ecr:<tag>` |
| `deployment.yml` | `<CONFIG_HASH>` (annotation `checksum/config`)                                         | SHA-256 do `configmap.yml` e do `secret.yml` já substituídos             |
| `secret.yml`     | `<BASE64_USERNAME>`, `<BASE64_PASSWORD>`, `<BASE64_JWT_SECRET>`, `<BASE64_DD_API_KEY>` | Valores em base64                                                        |

```bash
aws eks update-kubeconfig --region us-east-1 --name ms-ofisy-execution-cluster
kubectl apply -f k8s/secret.yml -f k8s/configmap.yml -f k8s/deployment.yml -f k8s/service.yml -f k8s/hpa.yml
kubectl rollout status deployment/ms-ofisy-execution-deployment --timeout=300s
```

O acesso ao SNS/SQS usa as credenciais da `LabRole` dos nós, sem chaves no manifesto. O HPA depende do Metrics Server no cluster (o workflow instala); sem ele, o deployment fica nas 2 réplicas mínimas.

---

## Documentação Adicional

### Segurança
- **[Autenticação e autorização](docs/SECURITY.md)**: contrato do JWT emitido pelo core, roles por rota, respostas 401/403 e como gerar um token local

### Mensageria
- **[Mensageria](docs/MESSAGING.md)**: topologia SNS/SQS, contrato dos eventos, idempotência, DLQ e como exercitar o fluxo localmente

### Banco de Dados
- **[Banco de Dados](docs/DATABASE.md)**: tabelas, constraints e índices
- **[Guia de Flyway](docs/FLYWAY.md)**: como criar e executar migrations

### Qualidade e Testes
- **[Guia de Testes](docs/TESTING.md)**: testes unitários e de integração, Testcontainers e cobertura