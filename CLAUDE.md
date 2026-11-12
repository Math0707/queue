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
- Claim concorrente: lock otimista com `@Version` em `Task`; a cópia desatualizada gera `ObjectOptimisticLockingFailureException`, convertida em 409.

## Roadmap
O escopo é um projeto de portfólio para volume pequeno: login e alta escala ficam de fora.

Concluídos: fuso horário com `Instant` e `Clock` (PR #1); CI com job `postgres` (PR #2); claim concorrente com `@Version` e 409 (PR #3).

Para fechar o projeto:
1. Cancelar e devolver para a fila.
2. DTO de saída e mensagens de erro consistentes em português.
3. Flyway no `prod` em vez de `ddl-auto=update`.
4. README final em pt-br, com print do painel, decisões e limitações conhecidas; versão em inglês (`README.en.md`) por último.

Fora do escopo (citar no README como próximos passos): fila filtrada e ordenada no banco, aging contra starvation de LOW.
