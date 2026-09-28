# AGENTS.md

Instructions for AI coding agents (and humans) working in this repository.

## What this is

Mockingbird Bank - a read-only account dashboard SPA. Spring Boot 4 backend with a
Vaadin 25 (Flow + React) frontend, backed by PostgreSQL. There is no public
registration; it seeds one demo account holder on first run (see
[Demo login](#demo-login-local-dev-only) below).

## Tech stack

- Java 21, Spring Boot 4.1.1, Spring Data JPA, Spring Security
- Vaadin 25.3.0 (Flow + React components) for the UI - frontend build is handled
  entirely by the Vaadin Gradle plugin; there's no separate `npm run dev`.
- PostgreSQL, schema-managed by Flyway (`src/main/resources/db/migration`)
- Gradle (wrapper checked in - always use `./gradlew`, not a system-installed Gradle)

## Prerequisites

- JDK 21 available locally (Gradle's toolchain support auto-detects an installed
  JDK 21; install one via sdkman/homebrew/Corretto if `./gradlew -version` can't
  find one).
- Docker, running locally - required for the Testcontainers-backed integration
  tests (`ApplicationSmokeTest`, `AccountTransactionQueryPerformanceTest`), which
  spin up a throwaway `postgres:16-alpine` container per test class.
- A reachable PostgreSQL instance if you want to run the app itself (`bootRun`);
  not required just to build or run the unit/integration test suite.

## Build

```
./gradlew build
```

Runs compilation, the full test suite, and every code-quality check described
below (Checkstyle, Spotless, PMD, CPD) as part of `check`. This is the single
command CI and this repo's pre-commit hook both rely on.

## Run locally

The app needs a Postgres connection. Either point it at an existing instance via
env vars, or copy `.env.example` to `.env` and adjust it (see that file's
comments) if you're running via `runDockerImage.zsh`.

```
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/mydatabase \
SPRING_DATASOURCE_USERNAME=admin \
SPRING_DATASOURCE_PASSWORD=secret \
./gradlew bootRun
```

Without those env vars it falls back to the same defaults (see
`src/main/resources/application.yml`), so `./gradlew bootRun` alone works if you
already have a local Postgres on `localhost:5432` with those credentials.
Flyway migrates the schema automatically on startup. The app listens on
`:8080` (override with `SERVER_PORT`).

### Demo login (local/dev only)

On an empty database, `DataInitializer` seeds one account holder, a matching
login, and a few sample accounts/transactions so the dashboard has something to
show:

- Username: `jordan.ellis`
- Password: `mockingbird` (override via the `MOCKINGBIRD_DEV_PASSWORD` env var)

## Testing

```
./gradlew test
```

Includes fast unit tests (Mockito-based service/model tests) and slower
`@SpringBootTest` + Testcontainers integration tests that boot the full Spring
context against a real Postgres container - Docker must be running. Test
reports land in `build/reports/tests/test/index.html`.

To run a single test class:

```
./gradlew test --tests "com.mockingbirdbank.service.AccountServiceTest"
```

## Code quality tooling

All of the following are wired into `./gradlew check` (and therefore `build`),
so they run on every CI build and every local commit via the Git hook below.
Run them individually while iterating:

| Tool | Command | Purpose |
|---|---|---|
| Spotless (google-java-format) | `./gradlew spotlessCheck` / `spotlessApply` | Enforces one Java formatting style; `spotlessApply` auto-fixes violations. |
| Checkstyle | `./gradlew checkstyleMain checkstyleTest` | Naming, imports, complexity, file length, TODO tracking. Config: `config/checkstyle/checkstyle.xml`. |
| PMD | `./gradlew pmdMain pmdTest` | Dead/unused code (unused fields, params, locals, private methods). Config: `config/pmd/dead-code.xml`. |
| CPD | `./gradlew cpdCheck` | Copy-paste/duplicate code detection across `src/main/java` and `src/test/java`. |

A checked-in Git hook (`.githooks/pre-commit`) runs Spotless + Checkstyle before
every commit. It's wired up automatically the first time you run
`./gradlew build` (via the `installGitHooks` task) - no manual `git config`
step needed.

## Project structure

```
src/main/java/com/mockingbirdbank/
  Application.java          Spring Boot entry point
  config/                    Startup seeding (DataInitializer)
  model/                     JPA entities (Account, AccountHolder, Transaction, AppUser, ...)
  repository/                Spring Data JPA repositories
  service/                    Application services (AccountService, TransactionService)
  security/                   Spring Security + Vaadin security wiring
  ui/                          Vaadin views (layout/ and view/)
src/main/resources/
  application.yml              Spring config
  db/migration/                 Flyway migrations (V1__init_schema.sql, ...)
src/test/java/com/mockingbirdbank/  Mirrors the main package layout
config/checkstyle/, config/pmd/     Static analysis rule configs
k8s/                                    Kubernetes manifests for deployment
```

## Conventions

- Package-by-layer (`model`, `repository`, `service`, `security`, `ui`), not
  package-by-feature - follow the existing layout for new classes.
- Constructors, not `@Autowired` fields, for dependency injection (see any
  `@Service`/`@Component`).
- Lombok `@Getter`/`@Setter`/`@NoArgsConstructor` on entities; avoid hand-writing
  boilerplate accessors.
- Explicit imports only - no wildcard imports (`AvoidStarImport` in Checkstyle).
- New JPA collection associations that might be iterated across multiple parent
  entities (e.g., another `@OneToMany`) should get `@BatchSize` to avoid N+1
  query patterns - see `Account.transactions` and
  `AccountTransactionQueryPerformanceTest` for the pattern and its regression
  guard.

## Known non-issue

`ApplicationSmokeTest.seededUserCanLogIn` deliberately stops at "login
succeeded, session holds the right principal" rather than also asserting a
follow-up `GET /accounts` returns 200. Under MockMvc, Vaadin's Spring servlet
initializes lazily on first real dispatch, which makes an otherwise-correct
authenticated request 403 in that test harness only. The full login -> dashboard
-> logout flow has been verified against a live `bootRun` instance; see the
comment on that test for details before "fixing" it.
