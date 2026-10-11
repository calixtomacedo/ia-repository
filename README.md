# ia-repository

Repositório destinado a armazenar os meus projetos de estudos sobre Inteligência Artificial na Programação.

---

## 📦 Projetos Disponíveis

### 1. [sboot-cmdev-atom-authentication](./sboot-cmdev-atom-authentication) (Backend)
- **Tecnologias:** Java 25 LTS, Spring Boot 4.1.1, JDBI 3, Spring Security, JJWT, MySQL 8/9.
- **Descrição:** API REST com endpoints de autenticação JWT (`/api/auth`) e gestão completa de usuários (`/api/users`).
- **Porta padrão:** `8080` (Context-path: `/api`).

### 2. [angular-cmdev-authentication](./angular-cmdev-authentication) (Frontend)
- **Tecnologias:** Angular 19 (última versão estável), Bootstrap 5, Bootstrap Icons, Signals reativos, SCSS.
- **Descrição:** Interface web completa consumindo todos os endpoints da API de autenticação e gerenciamento de usuários.
- **Porta padrão:** `4200` (com proxy reverso integrado para `http://localhost:8080`).
