# Frontend de Autenticação e Gestão de Usuários - angular-cmdev-authentication

Aplicação Frontend moderna desenvolvida em **Angular 19** (versão estável mais recente) como interface de usuário para o microserviço backend **`sboot-cmdev-atom-authentication`** (Spring Boot 4.1.1 / Java 25 / MySQL).

---

## 🚀 Tecnologias e Recursos

- **Framework**: Angular 19 (Standalone Components, Signals reativos, Typed Reactive Forms)
- **Roteamento**: Angular Router com Lazy Loading e View Transitions nativas
- **Segurança & HTTP**: Interceptor funcional (`HttpInterceptorFn`) com injeção automática de Bearer Token e renovação automática de sessão em caso de erro 401
- **Proteção de Rotas**: `authGuard` (rotas protegidas) e `guestGuard` (redirecionamento de usuários já autenticados)
- **Estilização & UI**: Bootstrap 5 + Bootstrap Icons + SCSS personalizado com gradientes e cartões responsivos
- **Notificações**: Serviço de Toasts com auto-dismiss para feedbacks de operações de sucesso, erro e alertas

---

## 🖥️ Mapeamento de Telas vs. Endpoints da API

Todas as operações especificadas no contrato `openapi.yaml` e documentadas no `README.md` do backend possuem telas e fluxos dedicados:

| Método | Endpoint | Descrição no Backend | Tela / Rota no Frontend |
|---|---|---|---|
| `POST` | `/api/auth/login` | Realizar login e obter tokens JWT | `/login` (com preenchimento automático para o admin inicial) |
| `POST` | `/api/auth/register` | Cadastrar novo usuário (permite flag opcional `isAdmin` para `ROLE_ADMIN` / `ROLE_USER`) | `/register` e Modal "Novo Usuário" em `/users` |
| `POST` | `/api/auth/refresh-token` | Renovar tokens JWT expirados | `/tokens` (com botão de renovação sob demanda e interceptor automático) |
| `POST` | `/api/auth/logout` | Revogar tokens na blacklist e logout | Navbar, Menu de Perfil e Painel de Tokens |
| `GET` | `/api/users` | Listar todos os usuários cadastrados | `/users` e Painel Resumo em `/dashboard` |
| `GET` | `/api/users/{id}` | Buscar detalhes do usuário por ID | `/users/:id` |
| `PUT` | `/api/users/{id}` | Atualizar dados do usuário (nome, e-mail) | `/users/:id/edit` |
| `DELETE` | `/api/users/{id}` | Excluir usuário da base MySQL | Ação com Modal de Confirmação em `/users` e `/users/:id` |
| `*` | *Todos os endpoints* | Exploração e testes diretos da API | `/api-explorer` (Console interativo com visualizador JSON) |

---

## 🔑 Credenciais Padrão do Administrador

Conforme registrado no script `db-scripts.sql` do microserviço backend:

- **E-mail:** `admin@empresa.com`
- **Senha:** `Admin@123`
- **Perfil:** `ROLE_ADMIN`

> 💡 **Dica:** Na tela de Login (`/login`), há um botão **"Preencher"** que insere essas credenciais automaticamente.

---

## ⚙️ Pré-requisitos e Como Executar

### 1. Iniciar o Backend
Certifique-se de que o backend `sboot-cmdev-atom-authentication` está em execução na porta `8080`:
```bash
cd sboot-cmdev-atom-authentication
mvn spring-boot:run
```
A API estará acessível em `http://localhost:8080/api`.

### 2. Instalar Dependências do Frontend
```bash
cd angular-cmdev-authentication
npm install
```

### 3. Iniciar o Servidor de Desenvolvimento
```bash
npm start
# ou: ng serve
```
Acesse a aplicação no navegador em: **[http://localhost:4200](http://localhost:4200)**.

> O arquivo `proxy.conf.json` está configurado para encaminhar requisições `/api` diretamente para `http://localhost:8080`, eliminando qualquer restrição de CORS em ambiente de desenvolvimento.

### 4. Build de Produção
```bash
npm run build
```
Os arquivos otimizados serão gerados no diretório `dist/angular-cmdev-authentication`.

---

## 📁 Estrutura de Pastas

```
src/app/
├── core/
│   ├── guards/          # authGuard e guestGuard
│   ├── interceptors/    # authInterceptor (Bearer token & refresh token)
│   ├── models/          # auth.model.ts, user.model.ts
│   └── services/        # auth.service, user.service, notification.service, config.service
├── features/
│   ├── auth/
│   │   ├── login/       # Tela de login
│   │   └── register/    # Tela de cadastro público
│   ├── dashboard/       # Visão geral, estatísticas e status da API
│   ├── users/
│   │   ├── user-list/   # Tabela com busca, filtros, criação e exclusão
│   │   ├── user-detail/ # Detalhes do perfil do usuário
│   │   └── user-edit/   # Edição de nome e e-mail
│   ├── tokens/          # Inspeção de claims JWT, tempo restante e renovação
│   └── api-explorer/    # Testador interativo de todos os 8 endpoints
├── layout/
│   ├── navbar/          # Cabeçalho de navegação com menu de usuário
│   └── footer/          # Rodapé com status da API e configuração dinâmica
└── shared/
    └── components/
        └── toast/       # Mensagens flutuantes de feedback
```
