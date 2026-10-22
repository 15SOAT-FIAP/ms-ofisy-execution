# Autenticação e autorização

As rotas da API exigem um JWT emitido pelo **core** ([techchallenge-ofisy](https://github.com/15SOAT-FIAP/techchallenge-ofisy)), que continua responsável pelo login (`POST /api/v1/login`) e pelos usuários. Este microsserviço não emite tokens nem tem banco de usuários: ele **valida** o token e autoriza a requisição pela role que vem nele.

A estrutura segue a do core: `SecurityConfig` stateless, filtro `JwtAuthFilter`, `@PreAuthorize` nos endpoints e as mesmas respostas de 401 e 403.

---

## Contrato do token

| Item      | Valor                                                                   |
|-----------|-------------------------------------------------------------------------|
| Algoritmo | HS256, assinado com o `JWT_SECRET` compartilhado entre o core e este ms |
| `iss`     | `techchallenge-ofisy-api`. Tokens de outros emissores são recusados     |
| `sub`     | Email do usuário                                                        |
| `role`    | `ADMIN`, `ATTENDANT`, `MECHANIC` ou `STOCKMAN`                          |
| `exp`     | Definido pelo core (24h hoje)                                           |
| Header    | `Authorization: Bearer <token>`                                         |

Os tokens de cliente, emitidos pela Lambda de autenticação por CPF (`iss = techchallenge-ofisy-auth`), são recusados com 401, assim como acontece no core.

### Pré-requisito no core

No core, a role não fica no token: ela é buscada no banco a cada requisição, pelo `OfisyUserDetailsService`. Para este ms conseguir autorizar sem acessar esse banco, o core precisa incluir a claim `role` ao gerar o token (`JwtService.generateToken`). Um token sem a claim `role` é aceito como autenticado, mas sem nenhuma role, e por isso recebe 403 em todas as rotas.

### Usuário inativo

O core bloqueia usuários inativos em cada requisição. Aqui não há essa consulta, então um usuário inativado no core continua com acesso a este ms até o token expirar.

---

## Regras de acesso

| Rota                                                 | Roles autorizadas                |
|------------------------------------------------------|----------------------------------|
| Todas as rotas de `/api/v1/service_order_executions` | `ADMIN`, `ATTENDANT`, `MECHANIC` |

As regras são as mesmas do core e ficam no `@PreAuthorize` de cada método do `ServiceOrderExecutionController`. O summary de cada operação no Swagger também lista as roles.

### Rotas públicas

Configuradas em `security.public-paths` (`application.yaml`):

- `/swagger-ui/**`, `/swagger-ui.html` e `/v3/api-docs/**`
- `/actuator/health/**`, usado pelas probes

### Respostas de erro

| Situação                                                | Status | `title`                  |
|---------------------------------------------------------|--------|--------------------------|
| Sem token, token inválido, expirado ou de outro emissor | 401    | `Usuário não autorizado` |
| Token válido, mas sem uma role autorizada               | 403    | `Usuário sem permissão`  |

---

## Configuração

| Propriedade             | Variável     | Descrição                                |
|-------------------------|--------------|------------------------------------------|
| `jwt.secret`            | `JWT_SECRET` | Chave HS256. Precisa ser a mesma do core |
| `security.public-paths` | —            | Rotas liberadas sem token                |

O `env.example` já traz o mesmo `JWT_SECRET` do `env.example` do core. Assim, um token gerado pelo core local também funciona aqui.

---

## Testando localmente

Sem subir o core, dá para gerar um token no mesmo formato:

```bash
make token ROLE=MECHANIC
```

O script `scripts/generate-token.sh` lê o `JWT_SECRET` do ambiente ou do `.env` e gera um token válido por 1 hora. Aceita `EMAIL` opcional.

```bash
TOKEN=$(make -s token ROLE=MECHANIC)
curl -H "Authorization: Bearer $TOKEN" localhost:8080/api/v1/service_order_executions
```

No Swagger UI, use o botão **Authorize** e cole o token. Com `ROLE=STOCKMAN`, as rotas respondem 403.