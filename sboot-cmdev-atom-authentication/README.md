# API de Autenticação - sboot-cmdev-atom-authentication

API REST para gerenciamento de usuários e autenticação JWT desenvolvida com base no contrato `openapi.yaml` e integrada
ao banco de dados MySQL (`auth_db`) conforme especificado em `src/main/resources/db-scripts.sql`.

## Tecnologias e Versões

- **Java**: 25 (LTS)
- **Spring Boot**: 4.1.1 (última versão estável)
- **Banco de Dados**: MySQL 8.x / 9.x com Driver oficial `com.mysql:mysql-connector-j` e pool de conexões HikariCP
- **Persistência**: JDBI 3 (`jdbi3-core`, `jdbi3-sqlobject`, `jdbi3-spring` v3.55.0) com
  `TransactionAwareDataSourceProxy`
- **Segurança**: Spring Security com JJWT (`jjwt-api`, `jjwt-impl`, `jjwt-jackson` v0.13.0) e criptografia BCrypt
- **Auditoria de Acesso**: Registro em `login_audit` (sucessos e falhas com IP e User-Agent) e `logout_audit` (revogação
  de tokens)
- **Documentação da API**: SpringDoc OpenAPI / Swagger UI (v3.1.1)
- **Sem Lombok**: Uso estrito de Java Records e classes nativas do Java 25.

---

## Estrutura do Banco de Dados (`db-scripts.sql`)

A aplicação conecta-se à base de dados MySQL **`auth_db`** utilizando as seguintes tabelas:

1. **`users`**: Cadastro de usuários com colunas `id`, `uuid`, `name`, `email`, `password_hash`, `enabled`,
   `account_non_expired`, `account_non_locked`, `credentials_non_expired`, `last_login`, `created_at` e `updated_at`.
2. **`roles`**: Perfis de acesso (`ROLE_ADMIN`, `ROLE_USER`).
3. **`user_roles`**: Tabela de associação N:N entre usuários e papéis.
4. **`refresh_tokens`**: Tokens de renovação vinculados a usuários com `issued_at`, `expires_at`, `revoked` e
   `revoked_at`.
5. **`login_audit`**: Histórico de tentativas de login com `ip_address`, `user_agent`, `success` e `login_timestamp`.
6. **`logout_audit`**: Histórico de logouts e blacklist de tokens invalidados (`access_token_hash`, `logout_timestamp`).

---

## Configuração do Banco de Dados (`application.yml`)

As credenciais do MySQL estão configuradas com valores padrão e suporte a substituição por variáveis de ambiente:

```yaml
spring:
  datasource:
    url: ${DB_URL:jdbc:mysql://localhost:3306/auth_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8}
    driver-class-name: com.mysql.cj.jdbc.Driver
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD:root}
```

### Variáveis de Ambiente suportadas:

- `DB_URL`: URL de conexão JDBC (Padrão: `jdbc:mysql://localhost:3306/auth_db?...`)
- `DB_USERNAME`: Usuário do MySQL (Padrão: `root`)
- `DB_PASSWORD`: Senha do MySQL (Padrão: `root`)
- `JWT_SECRET`: Chave secreta de 256 bits para o JWT

---

## Endpoints da API (Context-Path: `/api`)

### Autenticação (`/api/auth`)

| Método | Endpoint                  | Descrição                                                                       | Autenticação        |
|--------|---------------------------|---------------------------------------------------------------------------------|---------------------|
| `POST` | `/api/auth/register`      | Cadastrar novo usuário (atribui `ROLE_USER`)                                    | Pública             |
| `POST` | `/api/auth/login`         | Realizar login (registra em `login_audit`, atualiza `last_login` e gera tokens) | Pública             |
| `POST` | `/api/auth/refresh-token` | Renovar tokens JWT com refresh token                                            | Pública             |
| `POST` | `/api/auth/logout`        | Realizar logout (registra em `logout_audit` e revoga tokens)                    | Requer Bearer Token |

### Usuários (`/api/users`)

| Método   | Endpoint          | Descrição                         | Autenticação        |
|----------|-------------------|-----------------------------------|---------------------|
| `GET`    | `/api/users`      | Listar todos os usuários          | Requer Bearer Token |
| `GET`    | `/api/users/{id}` | Buscar usuário por ID             | Requer Bearer Token |
| `PUT`    | `/api/users/{id}` | Atualizar dados do usuário por ID | Requer Bearer Token |
| `DELETE` | `/api/users/{id}` | Remover usuário por ID            | Requer Bearer Token |

---

## Usuário Administrador Inicial

Conforme inserido no script `db-scripts.sql`:

- **Email:** `admin@empresa.com`
- **Senha:** `Admin@123`
- **Role:** `ROLE_ADMIN`

---

## Como Executar

### 1. Criar e Popular o Banco no MySQL

Execute o script [
`src/main/resources/db-scripts.sql`](file:///D:/cmdev/workspace/sboot-cmdev-atom-authentication/src/main/resources/db-scripts.sql)
no seu servidor MySQL:

```bash
mysql -u root -p < src/main/resources/db-scripts.sql
```

### 2. Executar os Testes Automatizados

Os testes de integração utilizam o perfil `@ActiveProfiles("test")` com H2 em modo de compatibilidade MySQL para
isolamento completo:

```bash
mvn clean test
```

### 3. Iniciar a Aplicação

```bash
# Com as credenciais padrão (root/root na porta 3306)
mvn spring-boot:run

# Ou informando credenciais personalizadas via variáveis de ambiente
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.datasource.username=seu_usuario --spring.datasource.password=sua_senha"
```

A aplicação iniciará na porta `8080` com o prefixo `/api`.

### 4. Acessar o Swagger UI / Documentação

- **Swagger UI:** [http://localhost:8080/api/swagger-ui.html](http://localhost:8080/api/swagger-ui.html)
- **OpenAPI JSON:** [http://localhost:8080/api/v3/api-docs](http://localhost:8080/api/v3/api-docs)
