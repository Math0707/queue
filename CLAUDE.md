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

## Pendências da branch fix/timezone (remover após o merge)
- [ ] Adicionar `spring.jpa.properties.hibernate.jdbc.time_zone=UTC`
- [ ] Teste: data sem fuso retorna 400
- [ ] `panel.js`: `body.scheduledFor = new Date(dt).toISOString();`
- [ ] Atualizar descrição e exemplo do Swagger em `CreateTaskRequest`
- [ ] Testar manualmente no H2 e no Docker/Postgres
- [ ] README: exemplo `2026-12-02T18:00:00Z` e item de fuso no roadmap
- [ ] Push, PR, CI verde e merge

## Roadmap
1. Claim concorrente: dois atendentes podem pegar a mesma task. Resolver com lock pessimista (`FOR UPDATE SKIP LOCKED`) ou otimista (`@Version`), com teste que prove que não há duplicidade.
2. Mover filtro e ordenação da fila para o banco (hoje carrega todos os PENDING em memória).
3. Aging: prioridade sobe com o tempo de espera, evitando starvation de LOW.
4. Cancelar, re-enfileirar e usar status ERROR; campo `claimedAt` para métricas de espera e atendimento.
5. DTO de saída (hoje a entidade é exposta), mensagens de erro em português e consistentes, Flyway no prod em vez de `ddl-auto=update`.
