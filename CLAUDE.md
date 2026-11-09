# Queue (Fila Inteligente)

API REST de fila de atendimento com prioridade e agendamento.

## Stack
Java 21, Spring Boot 3.5 (Web, Data JPA, Validation), Lombok, springdoc/Swagger, JUnit 5, Mockito e MockMvc. Painel em HTML/JS puro em `src/main/resources/static`.

## Comandos
- Testes e build: `.\mvnw.cmd clean verify`
- Rodar local (perfil dev, H2): `.\mvnw.cmd spring-boot:run`
- Rodar com Postgres: `docker compose up --build`

Sempre use `clean` antes de confiar no resultado. Sem ele, o Maven pode reaproveitar classes antigas: já deu `NoSuchMethodError` após mudança de assinatura e já disse "Nothing to compile" com erro de sintaxe no arquivo.

## Git
- Nunca commitar direto na main: trabalhar em branch e abrir PR.
- Commits em português, no padrão Conventional Commits (`feat:`, `fix:`, `test:`, `docs:`, `ci:`, `refactor:`), com título curto no imperativo/presente e corpo explicando o porquê.

## Regras de negócio
- Ordem: HIGH > MEDIUM > LOW; empate é FIFO por `createdAt`.
- `scheduledFor`: a task só entra na fila a partir desse instante, inclusive (`!isAfter(now)`).
- Fluxo: `POST /tasks` (PENDING) → `POST /queue/claim` (PROCESSING) → `POST /tasks/{id}/complete` (DONE; 409 se não estiver PROCESSING).
- Consultas: `GET /tasks`, `GET /queue`, `GET /queue/next`.

## Decisões técnicas
- Datas são `Instant`, nunca `LocalDateTime`: `LocalDateTime` não tem fuso, e no Docker (UTC) os agendamentos seriam liberados 3h antes.
- A API rejeita (400) datas sem fuso.
- O tempo vem de um bean `Clock` (`config/TimeConfig`), injetado nos services com `Instant.now(clock)`. Testes unitários usam `Clock.fixed`.
- `@Future` usa o relógio do sistema, não o bean `Clock`.
- Perfis: `dev` (H2, padrão) e `prod` (PostgreSQL).
- `priority` é salva como STRING: `ORDER BY` no banco daria ordem alfabética.
- CI: `./mvnw verify` em push na main e em PRs.

## Roadmap
O escopo é um projeto de portfólio para volume pequeno: login e alta escala ficam de fora.

0. Fuso horário com `Instant` e `Clock`: concluído no PR #1. Faltou validar no PostgreSQL.
1. CI com PostgreSQL (branch `ci/postgres`): job no GitHub Actions com `services: postgres`, rodando os testes com `SPRING_PROFILES_ACTIVE=prod`.
2. Claim concorrente: dois atendentes podem pegar a mesma task. Resolver com lock pessimista (`FOR UPDATE SKIP LOCKED`) ou otimista (`@Version`), com teste que prove que não há duplicidade.
3. Mover filtro e ordenação da fila para o banco (hoje carrega todos os PENDING em memória).
4. Aging: prioridade sobe com o tempo de espera, evitando starvation de LOW.
5. Cancelar, re-enfileirar e usar status ERROR; campo `claimedAt` para métricas de espera e atendimento.
6. DTO de saída (hoje a entidade é exposta), mensagens de erro em português e consistentes, Flyway no prod em vez de `ddl-auto=update`.
