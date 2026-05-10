# Authserver — Especificação para porte em Node.js

Este documento descreve a aplicação atual (Spring Boot 4 / Kotlin) com o nível de
detalhe necessário para reimplementá-la em Node.js mantendo o mesmo contrato HTTP,
o mesmo modelo de dados e o mesmo comportamento de autenticação/autorização.

---

## 1. Visão geral

API REST com autenticação JWT (HS256) e armazenamento de senhas com BCrypt. O
domínio de negócio expõe **Produtos** e **Categorias** (relação muitos-para-muitos):
produtos só podem ser associados a categorias previamente cadastradas.

Há um usuário `admin` criado automaticamente no startup, com role `ADMIN`.

Context-path padrão: **`/api`**.

---

## 2. Stack atual × proposta Node.js

| Camada | Atual (Spring) | Proposta Node.js (recomendada) | Alternativa mais leve |
| --- | --- | --- | --- |
| Linguagem | Kotlin 2.2 | TypeScript 5.x | TypeScript |
| Framework | Spring Boot 4 / WebMVC | **NestJS 10+** | Express 4 + decorators manuais |
| ORM | Spring Data JPA + Hibernate | **TypeORM** (decoradores casam com JPA) | Prisma |
| Banco (dev) | H2 em memória | SQLite em memória (`:memory:`) | Postgres + Docker |
| Validação | jakarta.validation (`@NotBlank`, `@Size`, ...) | **class-validator + class-transformer** | zod |
| JWT | spring-security-oauth2-resource-server (Nimbus) | **`@nestjs/jwt`** (jsonwebtoken) | `jsonwebtoken` |
| BCrypt | `BCryptPasswordEncoder` | **`bcrypt`** (10 rounds) | `bcryptjs` |
| Logging | SLF4J + Logback | **`nestjs-pino`** ou `pino` | `winston` |
| OpenAPI | springdoc-openapi-ui | **`@nestjs/swagger`** | `swagger-jsdoc` |
| Hash de tabelas | DDL automático (Hibernate) | TypeORM `synchronize: true` (dev) | `prisma migrate dev` |

A arquitetura abaixo segue a estrutura de **NestJS**, por ser a mais próxima do
Spring (módulos, controllers, services, providers, DI).

---

## 3. Estrutura de pacotes/módulos sugerida

```
src/
├── main.ts                       # bootstrap
├── app.module.ts                 # raiz
├── config/
│   └── configuration.ts          # carrega env vars
├── auth/
│   ├── auth.module.ts
│   ├── auth.service.ts           # login, currentUser, issueToken
│   ├── auth.controller.ts        # /auth/login, /auth/me
│   ├── jwt.strategy.ts           # passport-jwt strategy
│   ├── jwt-auth.guard.ts         # @UseGuards(JwtAuthGuard)
│   └── dto/
│       ├── login-request.dto.ts
│       └── token-response.dto.ts
├── users/
│   ├── user.entity.ts
│   ├── user.repository.ts
│   ├── users.service.ts
│   ├── users.controller.ts
│   └── users.module.ts
├── roles/
│   ├── role.entity.ts
│   ├── role.repository.ts
│   ├── roles.service.ts
│   ├── roles.controller.ts
│   └── roles.module.ts
├── categories/
│   ├── category.entity.ts
│   ├── category.repository.ts
│   ├── categories.service.ts
│   ├── categories.controller.ts
│   ├── categories.module.ts
│   └── dto/{create,update}-category.dto.ts
├── products/
│   ├── product.entity.ts
│   ├── product-status.enum.ts
│   ├── product.repository.ts
│   ├── products.service.ts
│   ├── products.controller.ts
│   ├── products.module.ts
│   └── dto/{create,update,query}-product.dto.ts
├── exceptions/
│   ├── api-error.ts                # contrato de erro
│   ├── *.exception.ts              # ResourceNotFound, Forbidden, etc.
│   └── global-exception.filter.ts  # @Catch() — equivalente ao GlobalExceptionHandler
└── seed/
    └── data-seed.service.ts        # OnModuleInit -> cria roles + admin
```

---

## 4. Configuração (variáveis de ambiente)

| Variável | Padrão | Descrição |
| --- | --- | --- |
| `PORT` | `8080` | Porta HTTP |
| `CONTEXT_PATH` | `/api` | Prefixo global |
| `JWT_SECRET` | `troque-este-segredo-por-uma-string-com-pelo-menos-32-bytes` | **mínimo 32 bytes** (HS256) |
| `JWT_EXPIRATION_SECONDS` | `3600` | Validade do access token |
| `JWT_ISSUER` | `authserver` | Claim `iss` |
| `ADMIN_EMAIL` | `admin@authserver.local` | Email do admin do seed |
| `ADMIN_PASSWORD` | `admin123` | Senha do admin (será BCrypt encoded) |
| `ADMIN_NAME` | `Administrador` | Nome do admin |
| `DB_URL` | `:memory:` (SQLite) | Conexão do banco |

Em NestJS, expor via `@nestjs/config` e validar com `Joi` ou `zod`.

---

## 5. Modelo de dados

### 5.1 Entidades e relações

```
┌──────────┐      ┌──────────────┐      ┌──────────┐
│  User    │◄────►│  user_roles  │◄────►│  Role    │   M:N
│  (id PK) │      │              │      │  (id PK) │
└────┬─────┘      └──────────────┘      └──────────┘
     │
     │ owns (1:N)
     ▼
┌──────────┐      ┌────────────────────┐      ┌────────────┐
│ Product  │◄────►│ product_categories │◄────►│  Category  │   M:N
│  (id PK) │      │                    │      │  (id PK)   │
└──────────┘      └────────────────────┘      └────────────┘
```

### 5.2 Schema (DDL conceitual — TypeORM `synchronize: true` cria automaticamente)

**users**
| coluna | tipo | constraints |
| --- | --- | --- |
| id | bigint | PK auto |
| name | varchar | not null |
| email | varchar | not null, **unique** |
| password | varchar | not null (hash BCrypt, ~60 chars) |

**roles**
| coluna | tipo | constraints |
| --- | --- | --- |
| id | bigint | PK auto |
| name | varchar | not null, **unique** (uppercase) |
| description | varchar | not null |

**user_roles** (join)
| coluna | tipo | constraints |
| --- | --- | --- |
| id_user | bigint | FK users.id |
| id_role | bigint | FK roles.id |

**categories**
| coluna | tipo | constraints |
| --- | --- | --- |
| id | bigint | PK auto |
| name | varchar(80) | not null, **unique** (case-insensitive) |
| description | varchar(500) | |

**products**
| coluna | tipo | constraints |
| --- | --- | --- |
| id | bigint | PK auto |
| name | varchar(200) | not null |
| description | varchar(1000) | |
| price | numeric(12,2) | not null, ≥ 0 |
| stock | int | not null, ≥ 0 |
| status | varchar(20) | not null, enum (`ACTIVE` \| `INACTIVE` \| `OUT_OF_STOCK`) |
| created_at | timestamp | not null, default now |
| owner_id | bigint | FK users.id, not null |

**product_categories** (join)
| coluna | tipo | constraints |
| --- | --- | --- |
| product_id | bigint | FK products.id |
| category_id | bigint | FK categories.id |

### 5.3 Exemplo TypeORM (Product)

```ts
@Entity('products')
export class Product {
  @PrimaryGeneratedColumn() id: number;
  @Column({ length: 200 }) name: string;
  @Column({ length: 1000, default: '' }) description: string;
  @Column('decimal', { precision: 12, scale: 2 }) price: string; // string p/ precisão
  @Column({ type: 'int', default: 0 }) stock: number;
  @Column({ length: 20, default: ProductStatus.ACTIVE }) status: ProductStatus;
  @CreateDateColumn({ name: 'created_at' }) createdAt: Date;

  @ManyToOne(() => User, { eager: false, nullable: false })
  @JoinColumn({ name: 'owner_id' })
  owner: User;

  @ManyToMany(() => Category, { eager: false })
  @JoinTable({
    name: 'product_categories',
    joinColumn: { name: 'product_id' },
    inverseJoinColumn: { name: 'category_id' },
  })
  categories: Category[];
}
```

---

## 6. Autenticação

### 6.1 BCrypt
- Senha do usuário é sempre persistida como hash BCrypt (`$2a$10$...`).
- Cadastro (`POST /users`): receba a senha em texto puro, faça `bcrypt.hash(raw, 10)`
  e salve.
- Login (`POST /auth/login`): faça `bcrypt.compare(raw, hash)`.
- **Nunca** retorne o hash em respostas externas (preferir omitir o campo `password`
  via `class-transformer`/`@Exclude()` ou DTO específico).

### 6.2 JWT
- Algoritmo: **HS256** (HMAC-SHA256), chave simétrica em `JWT_SECRET` (≥ 32 bytes).
- Validade: `JWT_EXPIRATION_SECONDS` (default 3600s).
- Claims:
  ```json
  {
    "iss": "authserver",
    "sub": "user@example.com",
    "iat": 1778417520,
    "exp": 1778421120,
    "userId": 1,
    "roles": ["ADMIN"]
  }
  ```
- Header obrigatório nas rotas protegidas:
  `Authorization: Bearer <token>`

### 6.3 Resposta do `/auth/login`

```json
{
  "accessToken": "eyJhbGciOi...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "email": "admin@authserver.local",
  "roles": ["ADMIN"]
}
```

### 6.4 NestJS (esqueleto)

```ts
@Injectable()
export class AuthService {
  constructor(
    private users: UsersRepository,
    private jwt: JwtService,
    @Inject('JWT_CFG') private cfg: JwtConfig,
  ) {}

  async login(email: string, raw: string): Promise<TokenResponse> {
    const user = await this.users.findByEmail(email);
    if (!user || !(await bcrypt.compare(raw, user.password))) {
      throw new UnauthorizedException('Email ou senha inválidos');
    }
    return this.issueToken(user);
  }

  issueToken(user: User): TokenResponse {
    const roles = user.roles.map(r => r.name);
    const payload = { sub: user.email, userId: user.id, roles };
    const accessToken = this.jwt.sign(payload, {
      secret: this.cfg.secret,
      expiresIn: this.cfg.expirationSeconds,
      issuer: this.cfg.issuer,
      algorithm: 'HS256',
    });
    return { accessToken, tokenType: 'Bearer', expiresIn: this.cfg.expirationSeconds, email: user.email, roles };
  }

  async currentUser(jwt: { sub: string }): Promise<User> {
    const user = await this.users.findByEmail(jwt.sub);
    if (!user) throw new UnauthorizedException('Usuário do token não encontrado');
    return user;
  }
}
```

---

## 7. Autorização

Regras aplicadas no service (não em annotations), espelhando o Spring atual:

| Recurso | Quem pode modificar (PUT/DELETE/associar) |
| --- | --- |
| Categoria | Qualquer usuário autenticado pode **criar**; **atualizar/remover** liberado para qualquer autenticado (sem RBAC fina, mas a regra `cannot delete with products` se aplica). |
| Produto | **Apenas o `owner` ou um usuário com role `ADMIN`**. |
| User (DELETE / PUT roles) | Qualquer autenticado (na implementação atual). |
| Role (POST) | Qualquer autenticado. |

Helper de checagem (pseudo-código):

```ts
function ensureCanModify(product: Product, requester: User) {
  const isOwner = product.owner.id === requester.id;
  const isAdmin = requester.roles.some(r => r.name.toUpperCase() === 'ADMIN');
  if (!isOwner && !isAdmin) {
    throw new ForbiddenException('Apenas o dono do produto ou um ADMIN pode modificá-lo');
  }
}
```

---

## 8. Bootstrap / seed

Equivalente ao `DataSeed` (Spring `ApplicationRunner`). Em NestJS, use o lifecycle
`OnModuleInit` (ou `OnApplicationBootstrap`).

Comportamento:

1. Garante existência das roles `ADMIN` e `USER` (cria se não existir).
2. Procura usuário por `ADMIN_EMAIL`.
3. Se não existir: cria com `bcrypt.hash(ADMIN_PASSWORD, 10)` e role `ADMIN`.
4. Se existir mas sem role `ADMIN`: adiciona a role.
5. Logs:
   - `Role criada no startup: ADMIN`
   - `Usuário admin criado no startup: email=...`
   - `Role ADMIN garantida para usuário existente email=...`

```ts
@Injectable()
export class DataSeedService implements OnModuleInit {
  // ...injeções
  async onModuleInit() {
    const adminRole = await this.ensureRole('ADMIN', 'Administrador do sistema');
    await this.ensureRole('USER', 'Usuário comum');

    const existing = await this.users.findByEmail(this.cfg.adminEmail);
    if (!existing) {
      await this.users.save({
        name: this.cfg.adminName,
        email: this.cfg.adminEmail,
        password: await bcrypt.hash(this.cfg.adminPassword, 10),
        roles: [adminRole],
      });
      this.logger.log(`Usuário admin criado: ${this.cfg.adminEmail}`);
    } else if (!existing.roles.some(r => r.name === 'ADMIN')) {
      existing.roles.push(adminRole);
      await this.users.save(existing);
    }
  }
}
```

---

## 9. Catálogo de endpoints

Todos os paths abaixo são prefixados por `/api`. **Auth: Bearer** indica que o
header `Authorization: Bearer <jwt>` é obrigatório.

### 9.1 Auth

| Método | Path | Auth | Descrição |
| --- | --- | --- | --- |
| POST | `/auth/login` | público | Recebe `{email, password}` e devolve `TokenResponse` |
| GET  | `/auth/me`    | Bearer | Retorna `{id, name, email, roles[]}` do usuário do token |

### 9.2 Users

| Método | Path | Auth | Body / Query | Notas |
| --- | --- | --- | --- | --- |
| POST | `/users` | público | `{name, email, password}` | Senha vai com BCrypt. Email único. |
| GET  | `/users` | público | `?sortDir=ASC|DESC` | Lista todos. |
| GET  | `/users/{id}` | público | — | 404 se não existir. |
| GET  | `/users/ping` | público | — | `{"status":"ok"}` |
| DELETE | `/users/{id}` | Bearer | — | 204. |
| PUT  | `/users/{id}/roles/{role}` | Bearer | — | Concede a role nomeada (uppercase). 200 se concedida, 204 se já tinha. |

### 9.3 Roles

| Método | Path | Auth | Body | Notas |
| --- | --- | --- | --- | --- |
| POST | `/roles` | Bearer | `{name, description}` | Nome único, persistido em uppercase. 409 se duplicada. |
| GET  | `/roles` | público | — | Lista todas. |

### 9.4 Categories

| Método | Path | Auth | Body | Notas |
| --- | --- | --- | --- | --- |
| POST | `/categories` | Bearer | `CategoryRequest` | 409 se nome duplicado (case-insensitive). |
| GET  | `/categories` | público | — | Lista todas. |
| GET  | `/categories/{id}` | público | — | 404 se não existir. |
| PUT  | `/categories/{id}` | Bearer | `CategoryRequest` | 409 se nome conflita com outra categoria. |
| DELETE | `/categories/{id}` | Bearer | — | **422** se houver produtos associados. |

`CategoryRequest`:
```json
{ "name": "Eletrônicos", "description": "Aparelhos e acessórios" }
```

`CategoryResponse`:
```json
{ "id": 1, "name": "Eletrônicos", "description": "Aparelhos e acessórios" }
```

### 9.5 Products

| Método | Path | Auth | Body / Query | Notas |
| --- | --- | --- | --- | --- |
| POST | `/products` | Bearer | `ProductRequest` | `categoryIds` opcional; **todos os ids precisam existir** (422 caso contrário). `owner` = usuário do JWT. |
| GET  | `/products` | público | filtros + sort (abaixo) | Lista filtrada. |
| GET  | `/products/{id}` | público | — | 404 se não existir. |
| PUT  | `/products/{id}` | Bearer | `ProductRequest` | Owner ou ADMIN (403). Se `categoryIds` vier não vazio, substitui o conjunto atual. |
| DELETE | `/products/{id}` | Bearer | — | Owner ou ADMIN. |
| POST | `/products/{id}/categories/{categoryId}` | Bearer | — | Associa categoria existente. 404 se categoria não existe; 422 se já associada. |
| DELETE | `/products/{id}/categories/{categoryId}` | Bearer | — | Desassocia. 422 se não estava associada. |

**Filtros e ordenação de `GET /products`:**

| Query param | Tipo | Descrição |
| --- | --- | --- |
| `name` | string | LIKE case-insensitive (`%name%`) |
| `status` | enum | `ACTIVE` \| `INACTIVE` \| `OUT_OF_STOCK` |
| `categoryId` | long | Produtos que tenham essa categoria |
| `priceMin` | decimal | preço ≥ valor |
| `priceMax` | decimal | preço ≤ valor |
| `sortBy` | string | `name` \| `price` \| `createdAt` \| `stock` \| `status` \| `id` (default `createdAt`) |
| `sortDir` | string | `ASC` \| `DESC` (default `DESC`) |

Validações de query:
- `sortBy` fora da lista → **422** `Campo de ordenação inválido '...'`
- `sortDir` fora de ASC/DESC → **422** `Direção de ordenação inválida '...'`
- `priceMin > priceMax` → **422** `priceMin não pode ser maior que priceMax`

`ProductRequest`:
```json
{
  "name": "Notebook Pro 14",
  "description": "i7 16GB SSD 512",
  "price": 7999.90,
  "stock": 12,
  "status": "ACTIVE",
  "categoryIds": [1, 3]
}
```

`ProductResponse`:
```json
{
  "id": 4,
  "name": "Notebook Pro 14",
  "description": "i7 16GB SSD 512",
  "price": 7999.90,
  "stock": 12,
  "status": "ACTIVE",
  "createdAt": "2026-05-10T09:52:13.269619",
  "ownerId": 1,
  "ownerName": "Administrador",
  "categories": [
    { "id": 1, "name": "Eletrônicos" },
    { "id": 3, "name": "Livros" }
  ]
}
```

---

## 10. Formato de erro padronizado (`ApiError`)

Todo handler de exceção devolve este envelope:

```json
{
  "timestamp": "2026-05-10T08:00:12.737281-03:00",
  "status": 422,
  "error": "Entidade não processável",
  "message": "Campo de ordenação inválido 'foo'. Valores permitidos: name, price, ...",
  "details": null
}
```

Quando o erro vem de validação, `details` traz `{campo: mensagem}`:

```json
{
  "timestamp": "2026-05-10T08:00:12.737281-03:00",
  "status": 400,
  "error": "Requisição inválida",
  "message": "Falha de validação",
  "details": { "name": "o nome é obrigatório" }
}
```

### 10.1 Tabela de exceções

| Exceção (Spring) | HTTP | `error` (PT-BR) | Quando |
| --- | --- | --- | --- |
| `ResourceNotFoundException` | 404 | "Não encontrado" | Recurso pelo id não existe |
| `UnauthorizedException` | 401 | "Não autorizado" | Sem JWT, JWT inválido, login errado |
| `ForbiddenException` | 403 | "Proibido" | Não dono nem ADMIN |
| `DuplicateResourceException` | 409 | "Conflito" | Email/role/categoria/SKU já existe |
| `BusinessRuleException` | 422 | "Entidade não processável" | Regra de negócio violada (ex.: categoria não cadastrada) |
| `MethodArgumentNotValidException` | 400 | "Requisição inválida" | Bean validation falhou; preencher `details` |
| `HttpMessageNotReadableException` | 400 | "Requisição inválida" | JSON malformado ou campo obrigatório faltando |
| `IllegalArgumentException` | 400 | "Requisição inválida" | Argumento inválido genérico |
| (qualquer outra) | 500 | "Erro interno do servidor" | Loga `ERROR` no servidor |

Em NestJS, criar `HttpExceptionFilter` global (`APP_FILTER`) que monta esse
envelope. Nas exceções customizadas, estender `HttpException` com a mensagem em
PT-BR e o `error` correto.

### 10.2 Handlers correspondentes em NestJS

```ts
@Catch()
export class GlobalExceptionFilter implements ExceptionFilter {
  catch(exception: unknown, host: ArgumentsHost) {
    const ctx = host.switchToHttp();
    const res = ctx.getResponse<Response>();

    let status = 500;
    let error = 'Erro interno do servidor';
    let message = 'Erro inesperado';
    let details: Record<string, string> | null = null;

    if (exception instanceof BadRequestException) {
      status = 400; error = 'Requisição inválida';
      const r = exception.getResponse() as any;
      if (Array.isArray(r.message)) {
        // class-validator devolve mensagens em array; converter para details {field:msg}
        details = Object.fromEntries(r.message.map(parseClassValidatorMsg));
        message = 'Falha de validação';
      } else {
        message = r.message ?? 'Argumento inválido';
      }
    } else if (exception instanceof UnauthorizedException) {
      status = 401; error = 'Não autorizado';
      message = (exception.getResponse() as any).message ?? exception.message;
      res.setHeader('WWW-Authenticate', 'Bearer realm="authserver"');
    }
    // ...e por diante para 403, 404, 409, 422

    res.status(status).json({
      timestamp: new Date().toISOString(),
      status, error, message, details,
    });
  }
}
```

---

## 11. Mensagens de validação (PT-BR)

Manter o texto exatamente para que clientes que dependem das mensagens não
quebrem.

| Campo | Restrição | Mensagem |
| --- | --- | --- |
| `LoginRequest.email` | NotBlank + Email | `o email é obrigatório` / `email inválido` |
| `LoginRequest.password` | NotBlank | `a senha é obrigatória` |
| `CategoryRequest.name` | NotBlank, ≤80 | `o nome é obrigatório` / `o nome deve ter no máximo 80 caracteres` |
| `CategoryRequest.description` | ≤500 | `a descrição deve ter no máximo 500 caracteres` |
| `ProductRequest.name` | NotBlank, ≤200 | `o nome é obrigatório` / `o nome deve ter no máximo 200 caracteres` |
| `ProductRequest.description` | ≤1000 | `a descrição deve ter no máximo 1000 caracteres` |
| `ProductRequest.price` | DecimalMin 0.00 | `o preço deve ser maior ou igual a 0` |
| `ProductRequest.stock` | Min 0 | `o estoque não pode ser negativo` |

Outras mensagens de regra de negócio (lançadas pelos services):

- `Já existe uma categoria com o nome '<X>'`
- `Já existe um usuário com o email '<X>'`
- `A role '<X>' já existe`
- `Categoria(s) não cadastrada(s): <ids>`
- `Não é possível remover a categoria '<id>' pois há produtos associados`
- `O produto <id> já está associado à categoria <catId>`
- `O produto <id> não está associado à categoria <catId>`
- `Apenas o dono do produto ou um ADMIN pode modificá-lo`
- `Email ou senha inválidos`
- `Cabeçalho Authorization ausente`
- `Token sem subject` / `Usuário do token não encontrado`

---

## 12. Comportamentos especiais

1. **`open-in-view: false`** — não há sessão Hibernate em controllers. No port
   Node a coisa é mais simples: faça eager-load explícito (`relations: ['roles']`,
   `relations: ['categories', 'owner']`) onde a serialização precisar.
2. **Senhas nunca retornadas** — usar DTO de saída sem o campo, ou
   `@Exclude({ toPlainOnly: true })`.
3. **Roles em uppercase** — `ROLE.name` é normalizado em uppercase tanto no
   cadastro quanto na concessão (`PUT /users/{id}/roles/{role}`).
4. **Categoria deletável só sem produtos** — fazer um `count` ou tentar `delete`
   e capturar o erro de FK; preferir o count explícito (mais previsível).
5. **`PUT /products/{id}`** — se `categoryIds` vier **vazio**, mantém as
   categorias atuais; se vier com pelo menos um id, substitui.
6. **Filtros com Specifications** — em TypeORM equivale a montar um
   `QueryBuilder` adicionando `.andWhere(...)` por filtro presente. Em Prisma
   equivale a montar um objeto `where` opcional.
7. **JWT exp** — Spring usa `Instant.now().plusSeconds(...)`. No Node, use
   `expiresIn: <segundos>` da `jsonwebtoken`.
8. **CORS** — atualmente desabilitado. Habilitar em Node conforme necessidade.
9. **CSRF** — desabilitado (API stateless). Manter assim.
10. **H2 console** — exposto em `/h2-console`. No port Node, considerar usar
    Adminer ou `sqlite-web` em desenvolvimento.

---

## 13. Mapeamento Spring → NestJS (cheatsheet)

| Spring | NestJS |
| --- | --- |
| `@RestController` | `@Controller()` |
| `@RequestMapping("/x")` | `@Controller('x')` |
| `@GetMapping("/x")` | `@Get('x')` |
| `@PostMapping`, `@PutMapping`, `@DeleteMapping` | `@Post`, `@Put`, `@Delete` |
| `@PathVariable id: Long` | `@Param('id') id: number` |
| `@RequestParam name` | `@Query('name') name?: string` |
| `@RequestBody @Valid` | `@Body() dto: CreateXDto` (com `ValidationPipe` global) |
| `@AuthenticationPrincipal Jwt` | `@Req() req` + `req.user` (passport) ou `@CurrentUser()` decorator |
| `@Service` | `@Injectable()` |
| `@Repository extends JpaRepository` | `@InjectRepository(Entity)` + `Repository<Entity>` |
| `JpaSpecificationExecutor` | `repo.createQueryBuilder('p').where(...)` |
| `@Transactional` | `dataSource.transaction(async manager => ...)` ou nada (TypeORM gerencia transações implícitas) |
| `@RestControllerAdvice` + `@ExceptionHandler` | `@Catch() ExceptionFilter` registrado como `APP_FILTER` |
| `BCryptPasswordEncoder` | `bcrypt.hash` / `bcrypt.compare` |
| `JwtEncoder` (Nimbus) | `JwtService.sign(payload, options)` |
| `oauth2ResourceServer.jwt()` | `@nestjs/passport` + `JwtStrategy` |
| `SecurityFilterChain.authorizeHttpRequests` | `@UseGuards(JwtAuthGuard)` ou guard global + `@Public()` decorator |
| `springdoc-openapi-ui` | `@nestjs/swagger` + `SwaggerModule.setup()` |
| `application.yaml` | `.env` + `@nestjs/config` |
| `ApplicationRunner` | `OnApplicationBootstrap` lifecycle hook |

---

## 14. Notas de implementação para o port

1. **Comece pelo skeleton de auth** — JWT + BCrypt + login + guard. Isso
   destrava todo o resto.
2. **Implemente `GlobalExceptionFilter` cedo** — assim os outros services já
   podem lançar exceções corretas.
3. **Modele entidades + relações** com TypeORM e habilite `synchronize: true`
   apenas em dev. Para prod, use migrations (`typeorm migration:generate`).
4. **Proteja senhas no DTO de saída** — `class-transformer` com
   `@Exclude()` no campo password ou DTOs separados (`UserResponseDto`).
5. **Specs de filtro** — em TypeORM:
   ```ts
   const qb = this.repo.createQueryBuilder('p').leftJoinAndSelect('p.categories', 'c');
   if (name) qb.andWhere('LOWER(p.name) LIKE :n', { n: `%${name.toLowerCase()}%` });
   if (status) qb.andWhere('p.status = :s', { s: status });
   if (categoryId) qb.andWhere('c.id = :cid', { cid: categoryId });
   if (priceMin) qb.andWhere('p.price >= :pmin', { pmin: priceMin });
   if (priceMax) qb.andWhere('p.price <= :pmax', { pmax: priceMax });
   qb.orderBy(`p.${sortBy}`, sortDir.toUpperCase() as 'ASC' | 'DESC');
   return qb.getMany();
   ```
6. **Seed** — execute na inicialização do `AppModule` via `OnApplicationBootstrap`
   ou um script `npm run seed`.
7. **Testes** — replicar o smoke test e2e:
   - login do admin → 200 + JWT
   - cadastro sem JWT → 401
   - duplicidade → 409
   - validação → 400 com `details`
   - `categoryId` inexistente em `POST /products` → 422
   - usuário comum tentando deletar produto alheio → 403
   - associação repetida → 422
   - filtros e ordenação cobrem todos os campos permitidos
8. **Logging** — formate igual: nível, timestamp, classe/módulo, mensagem.
   Exemplo recomendado com `nestjs-pino`:
   ```
   { "level":"info", "time":"...", "context":"ProductService",
     "msg":"Criando produto nome='Notebook Pro' donoId=1 categorias=[1]" }
   ```

---

## 15. Smoke test sugerido (curl)

Salvar como `smoke.sh` e rodar contra `http://localhost:8080/api`:

```bash
BASE=http://localhost:8080/api

TOKEN=$(curl -s -X POST $BASE/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@authserver.local","password":"admin123"}' \
  | jq -r .accessToken)

curl -s -X POST $BASE/categories \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Eletrônicos","description":"x"}'

curl -s -X POST $BASE/products \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Notebook","price":7999.90,"stock":5,"categoryIds":[1]}'

curl -s "$BASE/products?categoryId=1&sortBy=price&sortDir=ASC"
```

---

**Fim da especificação.** Implementando todos os endpoints e regras acima, o
serviço Node.js será **funcionalmente equivalente** ao Spring Boot atual e
poderá ser substituído sem alteração no cliente HTTP.
