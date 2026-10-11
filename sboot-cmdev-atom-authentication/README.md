# API de Autenticação - sboot-cmdev-atom-authentication

API REST para gerenciamento de usuários e autenticação JWT baseada em padrões modernos de segurança, desenvolvida com Spring Boot e integrada ao banco de dados MySQL (`AUTH_DB`) via JDBI 3.

---

## Tecnologias e Versões

- **Java**: 25 (LTS)
- **Spring Boot**: 4.1.1
- **Banco de Dados**: MySQL 8.x / 9.x com Driver oficial `com.mysql:mysql-connector-j` e pool de conexões HikariCP
- **Persistência**: JDBI 3 (`jdbi3-core`, `jdbi3-sqlobject`, `jdbi3-spring` v3.55.0) com `TransactionAwareDataSourceProxy`
- **Segurança**: Spring Security com JJWT (`jjwt-api`, `jjwt-impl`, `jjwt-jackson` v0.13.0) e algoritmo BCrypt (`BCryptPasswordEncoder`)
- **Auditoria de Acesso**: Registro em `login_audit` (sucessos e falhas com IP, User-Agent e data/hora) e `logout_audit` (blacklist de hashes SHA-256 de tokens de acesso invalidados)
- **Documentação da API**: SpringDoc OpenAPI / Swagger UI (v3.1.1)
- **Sem Lombok**: Uso estrito de Java Records nativos do Java 25 para modelos e DTOs imutáveis.

---

## Regras de Negócio e Perfis de Acesso

### Perfis (`RolesEnum`)

A aplicação conta com dois perfis de acesso definidos no enum `RolesEnum`:

| Perfil | Nome no Banco | `isAdmin` | Descrição |
|---|---|---|---|
| Administrador | `ROLE_ADMIN` | `true` | Acesso completo, incluindo cadastro de novos usuários |
| Usuário Padrão | `ROLE_USER` | `false` | Acesso às operações cotidianas de usuários autenticados |

> **Nota:** Nas respostas da API (`UserResponse`), a role é apresentada simplificada sem o prefixo `ROLE_` (ex: `"ADMIN"` ou `"USER"`). O campo `createdAt` é formatado no fuso horário `America/Sao_Paulo` (padrão ISO-8601).

### Regra do Cadastro de Usuários (`/api/auth/register`)

- **Acesso Restrito:** O endpoint de cadastro **não é público**. Ele exige autenticação prévia com perfil de **Administrador (`ROLE_ADMIN`)**.
- **Definição de Perfil no Registro:** O payload de criação aceita o atributo opcional `isAdmin` (booleano):
  - `isAdmin: true` -> atribui o perfil `ROLE_ADMIN`.
  - `isAdmin: false` ou não informado -> atribui o perfil `ROLE_USER`.

---

## Estrutura do Banco de Dados (`AUTH_DB`)

O script de criação e carga inicial está localizado em [`src/main/resources/db-scripts.sql`](src/main/resources/db-scripts.sql). O banco de dados utilizado é o **`AUTH_DB`**, contendo as seguintes tabelas:

1. **`users`**: Cadastro de usuários com colunas `id`, `uuid`, `name`, `email`, `password_hash`, `enabled`, `account_non_expired`, `account_non_locked`, `credentials_non_expired`, `last_login`, `created_at` e `updated_at`.
2. **`roles`**: Perfis de acesso (`ROLE_ADMIN`, `ROLE_USER`).
3. **`user_roles`**: Associação N:N entre usuários e papéis com exclusão em cascata.
4. **`refresh_tokens`**: Tokens de renovação vinculados a usuários com `issued_at`, `expires_at`, `revoked` e `revoked_at`.
5. **`login_audit`**: Histórico detalhado de tentativas de login contendo `user_id`, `email`, `ip_address`, `user_agent`, `success` e `login_timestamp`.
6. **`logout_audit`**: Histórico de logouts e blacklist de tokens de acesso invalidados (`access_token_hash`, `user_id`, `logout_timestamp`).

---

## Configuração da Aplicação (`application.yml`)

As configurações de banco de dados e JWT suportam variáveis de ambiente:

```yaml
server:
  port: 8080
  servlet:
    context-path: /api

spring:
  application:
    name: sboot-cmdev-atom-authentication
  datasource:
    url: ${DB_URL:jdbc:mysql://localhost:3306/AUTH_DB?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8}
    driver-class-name: com.mysql.cj.jdbc.Driver
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 10
      minimum-idle: 2
      idle-timeout: 30000
      connection-timeout: 20000
      max-lifetime: 1800000

jwt:
  secret: ${JWT_SECRET:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}
  access-token-expiration-seconds: 300      # 5 minutos
  refresh-token-expiration-seconds: 3600     # 1 hora
```

### Variáveis de Ambiente Suportadas:

- `DB_URL`: URL de conexão JDBC (Padrão: `jdbc:mysql://localhost:3306/AUTH_DB?...`)
- `DB_USERNAME`: Usuário do MySQL (obrigatório em produção/execução local)
- `DB_PASSWORD`: Senha do MySQL (obrigatório em produção/execução local)
- `JWT_SECRET`: Chave secreta de 256 bits para assinatura HMAC dos tokens JWT

---

## Usuário Administrador Inicial

A aplicação inclui a classe [`DataInitializer`](src/main/java/com/cmdev/atom/authentication/config/DataInitializer.java), que é executada automaticamente na inicialização (`CommandLineRunner`) para garantir a criação do usuário administrador caso não exista:

- **Nome:** `CMDEV - Administrador`
- **Email:** `admin@cmdev.com.br`
- **Senha:** `Admin@123`
- **Role:** `ROLE_ADMIN`

*(O script `src/main/resources/db-scripts.sql` também contém a carga inicial das tabelas e perfis).*

---

## Endpoints da API (Context-Path: `/api`)

### Autenticação (`/api/auth`)

| Método | Endpoint | Descrição | Permissão / Autenticação |
|---|---|---|---|
| `POST` | `/api/auth/register` | Cadastrar novo usuário (permite definir `isAdmin`) | **Requer Bearer Token (`ROLE_ADMIN`)** |
| `POST` | `/api/auth/login` | Realizar login, registrar auditoria e gerar tokens | **Pública** |
| `POST` | `/api/auth/refresh-token` | Renovar par de tokens JWT via refresh token ativo | **Pública** |
| `POST` | `/api/auth/logout` | Realizar logout e invalidar token na blacklist | **Requer Bearer Token** |

### Usuários (`/api/users`)

| Método | Endpoint | Descrição | Permissão / Autenticação |
|---|---|---|---|
| `GET` | `/api/users` | Listar todos os usuários cadastrados | **Requer Bearer Token** |
| `GET` | `/api/users/{id}` | Buscar dados de um usuário por ID | **Requer Bearer Token** |
| `PUT` | `/api/users/{id}` | Atualizar dados cadastrais (`name`, `email`) | **Requer Bearer Token** |
| `DELETE` | `/api/users/{id}` | Excluir usuário pelo ID | **Requer Bearer Token** |

---

## Exemplos de Payloads

### 1. Registrar Usuário (`POST /api/auth/register`)
> **Atenção:** Requer cabeçalho `Authorization: Bearer <token_admin>`.

```json
{
  "name": "Carlos Santos",
  "email": "carlos@empresa.com",
  "password": "Senha@123",
  "isAdmin": false
}
```

**Resposta (`201 Created`):**
```json
{
  "id": 2,
  "name": "Carlos Santos",
  "email": "carlos@empresa.com",
  "role": "USER",
  "createdAt": "2026-10-10T22:51:34-03:00"
}
```

---

### 2. Login (`POST /api/auth/login`)

```json
{
  "email": "admin@cmdev.com.br",
  "password": "Admin@123"
}
```

**Resposta (`200 OK`):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 300
}
```

---

### 3. Renovação de Token (`POST /api/auth/refresh-token`)

```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

---

### 4. Logout (`POST /api/auth/logout`)
> Requer cabeçalho `Authorization: Bearer <access_token>`.

**Resposta (`200 OK`):**
```json
{
  "message": "Logout realizado com sucesso"
}
```

O hash SHA-256 do token é inserido na tabela `logout_audit` e os refresh tokens do usuário são revogados. Tentativas subsequentes de requisição com esse token retornarão status `401 Unauthorized`.

---

## Como Executar

### 1. Criar o Banco de Dados no MySQL

Execute o script [`src/main/resources/db-scripts.sql`](src/main/resources/db-scripts.sql) no servidor MySQL:

```bash
mysql -u root -p < src/main/resources/db-scripts.sql
```

### 2. Executar os Testes Automatizados

Os testes de integração utilizam o profile `@ActiveProfiles("test")` com H2 em memória configurado no modo de compatibilidade MySQL (`src/test/resources/application-test.yml` e `src/test/resources/schema-test.sql`):

```bash
mvn clean test
```

### 3. Iniciar a Aplicação

Defina as variáveis de ambiente com o usuário e senha do seu banco de dados:

```bash
export DB_USERNAME=root
export DB_PASSWORD=root
mvn spring-boot:run
```

Ou diretamente via argumentos do Spring Boot:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.datasource.username=root --spring.datasource.password=root"
```

A aplicação iniciará na porta `8080` com o context-path `/api`.

### 4. Acessar a Documentação Interativa

- **Swagger UI:** [http://localhost:8080/api/swagger-ui.html](http://localhost:8080/api/swagger-ui.html)
- **OpenAPI JSON:** [http://localhost:8080/api/v3/api-docs](http://localhost:8080/api/v3/api-docs)
