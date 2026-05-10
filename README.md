# Authserver

API REST de autenticação e catálogo de produtos, escrita em **Kotlin + Spring
Boot 4**, com autenticação **JWT (HS256)**, senhas em **BCrypt** e relação
**muitos-para-muitos** entre produtos e categorias.

Projeto desenvolvido para a disciplina de **Pós-graduação em Desenvolvimento
Mobile (PUC-PR)**.

#Video do Projeto: https://youtu.be/XtPnuvhZZww

---

## Funcionalidades

- Autenticação **JWT** com login via `email + senha`
- Senhas armazenadas com **BCrypt**
- **CRUD completo de Categorias**
- **CRUD completo de Produtos** com:
  - Filtragem por nome, status, categoria, faixa de preço
  - Ordenação por qualquer campo permitido (`name`, `price`, `createdAt`, ...)
  - Endpoints para **associar/desassociar** categorias previamente cadastradas
- **Autorização por dono** (apenas o criador do produto ou um `ADMIN` pode
  modificá-lo)
- **Seed automático** de roles (`ADMIN`, `USER`) e do usuário administrador
  no startup
- Tratamento de erros centralizado com payload padronizado em **PT-BR**
- Documentação **OpenAPI/Swagger UI** integrada

---

## Stack

| Camada | Tecnologia |
| --- | --- |
| Linguagem | Kotlin 2.2 (JVM 21) |
| Framework | Spring Boot 4.0.5 (WebMVC) |
| Persistência | Spring Data JPA + Hibernate |
| Banco de dados | H2 in-memory (dev) |
| Segurança | Spring Security 7 + OAuth2 Resource Server (JWT) |
| Validação | Jakarta Bean Validation |
| Documentação | springdoc-openapi (Swagger UI) |
| Build | Gradle 9 (Kotlin DSL) |

---

## Pré-requisitos

- **JDK 21+**
- **Gradle Wrapper** (já incluso, basta usar `./gradlew`)

Não há configuração adicional de banco — o H2 em memória é provisionado
automaticamente.

---

## Como executar

```bash
# Clone
git clone https://github.com/wellrodrigues1983/authserver_kotlin_PUC_PR.git
cd authserver_kotlin_PUC_PR

# Build
./gradlew build -x test

# Run
./gradlew bootRun
```

A aplicação sobe em `http://localhost:8080` com context-path `/api`.

### Endereços úteis

| Recurso | URL |
| --- | --- |
| API base | http://localhost:8080/api |
| Swagger UI | http://localhost:8080/api/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/api/v3/api-docs |
| Console H2 | http://localhost:8080/api/h2-console |

H2 console (dev): `JDBC URL = jdbc:h2:mem:db`, `user = sa`, `senha = sa`.

---

## Configuração (variáveis de ambiente)

Todos os parâmetros têm valores padrão; sobrescreva via variável de ambiente
quando necessário.

| Variável | Padrão | Descrição |
| --- | --- | --- |
| `JWT_SECRET` | `troque-este-segredo-por-uma-string-com-pelo-menos-32-bytes` | Chave HMAC do JWT (**mínimo 32 bytes**) |
| `ADMIN_EMAIL` | `admin@authserver.local` | Email do admin do seed |
| `ADMIN_PASSWORD` | `admin123` | Senha do admin (criada com BCrypt) |

Demais opções (validade do token, issuer, nome do admin) ficam em
`src/main/resources/application.yaml`.

---

## Usuário admin

Criado automaticamente no startup:

```
email:    admin@authserver.local
senha:    admin123
role:     ADMIN
```

---

## Modelo de dados

```
┌──────────┐      ┌──────────────┐      ┌──────────┐
│  User    │◄────►│  user_roles  │◄────►│  Role    │   M:N
└────┬─────┘      └──────────────┘      └──────────┘
     │
     │ owns (1:N)
     ▼
┌──────────┐      ┌────────────────────┐      ┌────────────┐
│ Product  │◄────►│ product_categories │◄────►│  Category  │   M:N
└──────────┘      └────────────────────┘      └────────────┘
```

---

## Endpoints

Todos prefixados por `/api`. Endpoints marcados com 🔒 exigem header
`Authorization: Bearer <jwt>`.

### Auth

| Método | Path | Descrição |
| --- | --- | --- |
| `POST` | `/auth/login` | Recebe `{email, password}` e devolve um JWT |
| `GET` | 🔒 `/auth/me` | Dados do usuário autenticado |

### Users / Roles

| Método | Path | Descrição |
| --- | --- | --- |
| `POST` | `/users` | Cadastro público (senha vai com BCrypt) |
| `GET` | `/users` | Lista usuários |
| `GET` | `/users/{id}` | Busca usuário |
| `DELETE` | 🔒 `/users/{id}` | Remove usuário |
| `PUT` | 🔒 `/users/{id}/roles/{role}` | Concede uma role |
| `POST` | 🔒 `/roles` | Cria role |
| `GET` | `/roles` | Lista roles |

### Categorias

| Método | Path | Descrição |
| --- | --- | --- |
| `POST` | 🔒 `/categories` | Cadastra categoria |
| `GET` | `/categories` | Lista categorias |
| `GET` | `/categories/{id}` | Busca categoria |
| `PUT` | 🔒 `/categories/{id}` | Atualiza categoria |
| `DELETE` | 🔒 `/categories/{id}` | Remove (falha se houver produtos associados) |

### Produtos

| Método | Path | Descrição |
| --- | --- | --- |
| `POST` | 🔒 `/products` | Cadastra produto (`categoryIds` opcional, mas todas precisam existir) |
| `GET` | `/products` | Lista com filtros e ordenação |
| `GET` | `/products/{id}` | Busca produto |
| `PUT` | 🔒 `/products/{id}` | Atualiza (apenas dono ou ADMIN) |
| `DELETE` | 🔒 `/products/{id}` | Remove (apenas dono ou ADMIN) |
| `POST` | 🔒 `/products/{id}/categories/{categoryId}` | Associa categoria existente |
| `DELETE` | 🔒 `/products/{id}/categories/{categoryId}` | Desassocia categoria |

#### Filtros e ordenação de `GET /products`

| Query | Descrição |
| --- | --- |
| `name` | Busca parcial case-insensitive |
| `status` | `ACTIVE`, `INACTIVE`, `OUT_OF_STOCK` |
| `categoryId` | Produtos que tenham essa categoria |
| `priceMin` / `priceMax` | Faixa de preço |
| `sortBy` | `name`, `price`, `createdAt`, `stock`, `status`, `id` (default `createdAt`) |
| `sortDir` | `ASC`, `DESC` (default `DESC`) |

---

## Exemplo de uso (curl)

```bash
BASE=http://localhost:8080/api

# 1) Login do admin -> obtém o JWT
TOKEN=$(curl -s -X POST $BASE/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@authserver.local","password":"admin123"}' \
  | jq -r .accessToken)

# 2) Cadastrar uma categoria (necessário antes de associar a um produto)
curl -s -X POST $BASE/categories \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Eletrônicos","description":"Aparelhos e acessórios"}'

# 3) Cadastrar um produto associado à categoria 1
curl -s -X POST $BASE/products \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
        "name": "Notebook Pro 14",
        "description": "i7 16GB SSD 512",
        "price": 7999.90,
        "stock": 12,
        "status": "ACTIVE",
        "categoryIds": [1]
      }'

# 4) Buscar produtos da categoria 1, ordenados por preço crescente
curl -s "$BASE/products?categoryId=1&sortBy=price&sortDir=ASC"
```

---

## Formato dos erros

Todas as exceções tratadas devolvem um envelope padronizado:

```json
{
  "timestamp": "2026-05-10T08:00:12.737281-03:00",
  "status": 422,
  "error": "Entidade não processável",
  "message": "Categoria(s) não cadastrada(s): 99",
  "details": null
}
```

| HTTP | `error` | Quando |
| --- | --- | --- |
| 400 | Requisição inválida | Validação ou JSON malformado (`details` traz `{campo: msg}`) |
| 401 | Não autorizado | JWT ausente, inválido ou login com credenciais erradas |
| 403 | Proibido | Não é dono nem ADMIN |
| 404 | Não encontrado | Recurso pelo id |
| 409 | Conflito | Recurso duplicado (email, role, categoria) |
| 422 | Entidade não processável | Regra de negócio (categoria inexistente, ordenação inválida, etc.) |
| 500 | Erro interno do servidor | Não esperado |

---

## Estrutura do código

```
src/main/kotlin/br/tec/wrcode/authserver/
├── AuthserverApplication.kt
├── auth/                # SecurityConfig, AuthService, AuthController, DTOs
├── categories/          # Category + CRUD
├── products/            # Product + CRUD + Specifications + DTOs
├── users/               # User + cadastro
├── roles/               # Role + cadastro
├── exceptions/          # exceções customizadas + GlobalExceptionHandler
├── init/                # DataSeed (admin no startup)
└── config/              # OpenApiConfig (Swagger)
```

---

## Testes

```bash
./gradlew test
```

---

## Documentação adicional

- [`SPEC-NODEJS.md`](./SPEC-NODEJS.md) — especificação detalhada do projeto
  para reimplementação em Node.js (NestJS / TypeORM)

---

## Autor

**Wellington Rodrigues** — Pós-graduação em Desenvolvimento Mobile, PUC-PR
