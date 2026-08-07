# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository layout

This repo has two independent parts:

- `avant/` — the Spring Boot backend (Maven project). All Java code lives here; this is the module you build/test.
- `front-end/` — a static HTML/CSS/JS institutional landing page (no build tooling, no framework). Consumes only the public GET endpoints (`/barbeiros/publico`, `/servicos-desejados/publico`) — it does not let the client book an appointment; that still happens over WhatsApp, per `PROJECT_CONTEXT.md`.

There is no root-level build file — always run Maven commands from inside `avant/`.

## Common commands

Run from the `avant/` directory:

```
# Build + run tests
./mvnw.cmd clean package        # Windows
./mvnw clean package            # Unix

# Run tests only
./mvnw.cmd test

# Run a single test class
./mvnw.cmd test -Dtest=AvantApplicationTests

# Run the app locally
./mvnw.cmd spring-boot:run
```

Always invoke the wrapper with a `./` prefix (`./mvnw.cmd`), not bare `mvnw`.

### Required environment variables

The app will not start without a datasource and OAuth2 config (see `avant/src/main/resources/application.yaml`):

- `DB_URL` (default `jdbc:postgresql://localhost:5432/avant_barbearia`)
- `DB_USERNAME` (default `postgres`)
- `DB_PASSWORD` — required, no default
- `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` — required for Google OAuth2 login, no default

A local PostgreSQL instance (`avant_barbearia` database) must be reachable. CI (`.github/workflows/pipeline.yaml`) spins up `postgres:15` as a service container, configures Java 25 via `setup-java`, and runs `mvn -B package --file avant/pom.xml` — this now matches `avant/pom.xml`'s `java.version` (25).

`spring.jpa.hibernate.ddl-auto` is `update`, so schema is derived from JPA entities directly; there are no migration scripts (no Flyway/Liquibase).

## Architecture

Standard layered Spring Boot MVC app, package-by-layer under `com.avantbarber.avant`:

```
controller/  → @RestController endpoints (JSON in/out), one per aggregate (Agendamento, Barbeiro, Cliente, ServicoDesejado) + a Thymeleaf-backed LoginController
service/     → @Service business logic, orchestrates repositories, maps entity <-> DTO
repository/  → Spring Data JPA interfaces
model/       → @Entity classes (Lombok @Getter/@Setter/@Builder)
dto/         → request/response DTOs (services never return entities to controllers)
exception/   → domain exceptions (BusinessException, ChaveDuplicadaException, HorarioFuncionamentoException, RecursoNaoEncontradoException)
infra/       → RestExceptionHandler (@ControllerAdvice) mapping domain exceptions to RestErrorMessage/HTTP status
config/      → SpringConfig: security filter chain, OAuth2 login, CORS for the public endpoints
```

Core domain: a barbershop booking system.
- `Cliente` (customer), `Barbeiro` (barber, has a `Perfil` of ADMIN/BARBEIRO), `ServicoDesejado` (service + price) are the base entities.
- `Agendamento` (appointment) ties a `Cliente` + `Barbeiro` + `ServicoDesejado` to a datetime, with a `StatusAgendamento` (PENDENTE, CONFIRMADO, CANCELADO, CONCLUIDO, REAGENDADO).

`AgendamentoService` is the most involved service — it owns all appointment business rules:
- Rejects appointments in the past.
- Enforces per-day operating hours directly in code (switch on `DayOfWeek`): closed Saturdays, Sunday 9h–14h, Monday 13h30–19h, Tue–Fri 10h–19h. `listarHorariosDisponiveis` mirrors these same hours to compute free 30-minute slots for a barber on a given date — if you change the hours in one place, update the other.
- Prevents double-booking: a barber or client can't have two appointments at the same timestamp.
- `salvar`/`cancelar`/`reagendar` are `@Transactional`; reads are not.

Auth: Spring Security with Google OAuth2 login only (`SpringConfig`). Public (no auth): `/`, `/login`, and two read-only GET endpoints for the landing page — `/barbeiros/publico` (returns `BarbeiroPublicoDTO`: id + nome only, never cpf/numero) and `/servicos-desejados/publico`. Everything else requires authentication. CORS is enabled only for those two public endpoints. There is no username/password login flow wired into Security despite `Cliente`/`Barbeiro` having a `senha` field and `spring-boot-starter-security-oauth2-authorization-server` being a dependency — that authorization-server piece isn't wired up in `SpringConfig` yet.

Error handling: domain exceptions thrown from services are translated centrally by `RestExceptionHandler` into a consistent `RestErrorMessage` JSON body (timestamp, status, error, message, path) — controllers never catch these themselves.

DTO mapping is manual (no MapStruct/ModelMapper): each service has private `toDTO`/`toEntity` methods.
