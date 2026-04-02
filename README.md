## Queue Manager (Fila Inteligente)

Backend (API REST) para gerenciamento de filas com **prioridade**, **status** e **agendamento**. Inclui **painel web** em português, **Swagger**, perfis **dev** (H2) e **prod** (PostgreSQL), **Docker** e **CI no GitHub**.

### Stack

- Java 21
- Spring Boot 3.5.x
- Spring Web + Spring Data JPA + Validation
- Banco: **H2** (desenvolvimento) ou **PostgreSQL** (Docker/produção)
- Maven Wrapper (`mvnw`)

### Clonar e subir no GitHub

Na pasta do projeto:

```powershell
git init
git branch -M main
git add .
git commit -m "Initial commit: API, painel, Docker e CI"
```

Crie um repositório vazio no GitHub e conecte:

```powershell
git remote add origin https://github.com/SEU_USUARIO/SEU_REPO.git
git push -u origin main
```

O workflow **CI** (`.github/workflows/ci.yml`) roda `mvn verify` em cada push/PR para `main` ou `master`.

### Perfis Spring

| Perfil | Uso | Banco |
|--------|-----|--------|
| `dev` (padrão) | `mvn spring-boot:run` local | H2 em memória + console `/h2` |
| `prod` | Docker ou servidor | PostgreSQL (`SPRING_DATASOURCE_*`) |

Arquivos: `application.properties` (comum), `application-dev.properties`, `application-prod.properties`.

### Rodar local (Windows)

- **JDK 21** e **JAVA_HOME** configurados ([instalação com winget](https://learn.microsoft.com/windows/package-manager/winget/) — pacote `Microsoft.OpenJDK.21`).

```powershell
cd queue
.\mvnw.cmd spring-boot:run
```

- **Painel:** `http://localhost:8080/`
- **Swagger:** `http://localhost:8080/swagger-ui.html`
- **H2 (só dev):** `http://localhost:8080/h2` — JDBC `jdbc:h2:mem:queue`, user `sa`, senha vazia.

Perfil explícito:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

Produção local com Postgres já rodando:

```powershell
$env:SPRING_PROFILES_ACTIVE="prod"
$env:SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/queue"
$env:SPRING_DATASOURCE_USERNAME="queue"
$env:SPRING_DATASOURCE_PASSWORD="queue"
.\mvnw.cmd spring-boot:run
```

### Docker (API + PostgreSQL)

Na **raiz** do repositório (onde está `docker-compose.yml`):

```powershell
docker compose up --build
```

A API fica em `http://localhost:8080` com perfil `prod` e dados persistentes no volume `pgdata`.

### Porta 8080 em uso

1. `netstat -ano | findstr :8080` → `taskkill /PID <pid> /F`
2. Ou `.\mvnw.cmd spring-boot:run "-Dspring-boot.run.jvmArguments=-Dserver.port=8081"`

### Endpoints (resumo)

**Solicitações:** `POST /tasks`, `GET /tasks`, `POST /tasks/{id}/complete`  
**Fila:** `GET /queue`, `GET /queue/next`, `POST /queue/claim`

Exemplo `POST /tasks`:

```json
{
  "description": "Atender cliente do caixa 3",
  "priority": "HIGH",
  "scheduledFor": "2026-04-02T15:00:00"
}
```

(`scheduledFor` opcional; se informado, deve ser futuro na criação.)

### Próximos passos (roadmap)

- Locks no `claim` para vários atendentes
- Cancelar / re-enfileirar
- Flyway/Liquibase no `prod` em vez de `ddl-auto=update`
- Testes de integração adicionais e scan de dependências
