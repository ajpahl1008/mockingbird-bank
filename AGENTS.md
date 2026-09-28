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

### Dev container

`.devcontainer/` gives you all of the above pre-installed (JDK 21, Node,
Docker CLI, Postgres) without touching your host machine. In VS Code, use
"Dev Containers: Reopen in Container"; from a plain terminal:

```
npx @devcontainers/cli up --workspace-folder .
npx @devcontainers/cli exec --workspace-folder . -- ./gradlew build
```

It builds on `mcr.microsoft.com/devcontainers/java:1-21-bookworm`, adds Node
via a devcontainer feature, and uses the `docker-outside-of-docker` feature so
Testcontainers-backed tests can start sibling containers against the host's
Docker daemon (`TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal` in
`.devcontainer/docker-compose.yml` is what makes the reaper/sibling containers
reachable from inside the dev container). `.devcontainer/docker-compose.yml`
merges with the root `docker-compose.yml`, so the same Postgres service
definition is reused rather than duplicated - it starts automatically and
`bootRun` inside the container reaches it at `postgres:5432`. Ports 8080 and
5432 are published to the host, so `curl localhost:8080/actuator/health`
works from outside the container too.

## Build

```
./gradlew build
```

Runs compilation, the full test suite, and every code-quality check described
below (Checkstyle, Spotless, PMD, CPD) as part of `check`. This is the single
command CI and this repo's pre-commit hook both rely on.

## Run locally

The app needs a Postgres connection. The quickest way to get one with the
right credentials/database name already set up:

```
docker compose up -d
```

`docker-compose.yml` at the repo root starts a `postgres:16-alpine` matching
the defaults in `src/main/resources/application.yml` (`localhost:5432`,
database `mydatabase`, user `admin` / password `secret`), with a named volume
so data survives a restart. Stop it with `docker compose down` (add `-v` to
also drop the volume and start from an empty database next time).

Alternatively, point the app at any other reachable Postgres via env vars, or
copy `.env.example` to `.env` and adjust it if you're running via
`runDockerImage.zsh`:

```
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/mydatabase \
SPRING_DATASOURCE_USERNAME=admin \
SPRING_DATASOURCE_PASSWORD=secret \
./gradlew bootRun
```

Without those env vars it falls back to the same defaults, so `./gradlew
bootRun` alone works once `docker compose up -d` (or any Postgres matching
those defaults) is running. Flyway migrates the schema automatically on
startup. The app listens on `:8080` (override with `SERVER_PORT`).

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

Every run also writes `build/reports/tests/test/timings.txt`, a per-test-method
duration report sorted slowest-first, and logs a warning for any test over 10s
- a lightweight way to notice the suite (or one test) getting slower over time
without a dedicated test-analytics platform.

The `test` task also uses the [Gradle Test Retry
plugin](https://github.com/gradle/test-retry-gradle-plugin): a failing test is
automatically re-run up to 2 more times, and a test that fails on one attempt
but passes on another (i.e. flaky, not broken) does not fail the build.
Every run writes `build/reports/tests/test/flaky.txt` listing any test whose
attempts disagreed, so instability is visible even though it isn't blocking.
A clean run with no retries writes a "no flaky tests detected" message there.

To run a single test class:

```
./gradlew test --tests "com.mockingbirdbank.service.AccountServiceTest"
```

### Test coverage

```
./gradlew jacocoTestReport              # HTML report at build/reports/jacoco/test/html/index.html
./gradlew jacocoTestCoverageVerification  # enforced thresholds, part of `check`
```

The Vaadin UI packages (`com.mockingbirdbank.ui.*`) are declarative view/layout
code with no branching logic - not meaningfully unit-testable without a full
browser fixture - so coverage is tracked but not gated there. Instead
`jacocoTestCoverageVerification` enforces two rules:

- A 30% project-wide line-coverage floor, so overall coverage can't regress.
- A 90% per-class line-coverage minimum on the business logic packages
  (`model`, `service`, `security`, `config`), which sit at 94-100% today.

### Interactive QA (browser-driven smoke test)

```
./scripts/run-interactive-qa.sh
```

Unlike `./gradlew test` (which never renders a real page), this drives an
actual Chromium browser against the actually-running app: starts a throwaway
Postgres on port 5433 (so it can't collide with or be confused by whatever a
developer already has on 5432), starts `bootRun` against it, waits for
`/actuator/health`, then uses Playwright (`qa/`, a separate npm project from
the Vaadin-managed root `package.json`) to sign in with the seeded demo login,
confirm the dashboard and an account's transactions render, sign out, and
confirm a wrong password is rejected. Tears down everything it started
(app process + Postgres container) on exit, success or failure.

First run needs `cd qa && npm install && npx playwright install chromium`
(one-time; downloads the Chromium binary Playwright drives).

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
| JaCoCo | `./gradlew jacocoTestReport` / `jacocoTestCoverageVerification` | Coverage report (`build/reports/jacoco/test/html/index.html`) and enforced thresholds - see [Test coverage](#test-coverage) below. |

A checked-in Git hook (`.githooks/pre-commit`) runs Spotless + Checkstyle before
every commit. It's wired up automatically the first time you run
`./gradlew build` (via the `installGitHooks` task) - no manual `git config`
step needed.

## Security & CI

- **CI** (`.github/workflows/ci.yml`) runs `./gradlew build` on every push/PR to
  `master`.
- **CodeQL** (`.github/workflows/codeql.yml`) runs static security analysis on
  every push/PR plus a weekly schedule; results land in the repo's Security
  tab.
- **Secret scanning**: GitHub's native secret scanning + push protection are
  enabled on this repo, plus `.github/workflows/secret-scan.yml` runs
  [gitleaks](https://github.com/gitleaks/gitleaks) on every push/PR as a
  second, independently-configured check.
- **DAST** (`.github/workflows/dast.yml`) builds a production-mode jar, boots
  it against a real Postgres, and runs an OWASP ZAP baseline scan against it
  on every push to `master` and weekly; the HTML/JSON report is uploaded as a
  workflow artifact for manual triage (it doesn't fail the build - baseline
  findings need a human to separate "noise" from "actual problem").
- **Dependabot** (`.github/dependabot.yml`) covers Gradle, both npm projects
  (root and `qa/`), Docker, and GitHub Actions, weekly, with a cooldown period
  (3-14 days depending on SemVer bump size) so a newly published version has
  time for the community to catch a compromised or broken release before it
  shows up here as a PR.
- **CODEOWNERS** (`.github/CODEOWNERS`) requires review from the repo owner on
  every path.
- **Branch protection** on `master` requires the CI status check to pass,
  blocks force-pushes and branch deletion, and requires conversation
  resolution; repo admins can still bypass it for direct pushes when needed.
- **Log scrubbing**: `logback-spring.xml` routes every log message through
  `ScrubbingMessageConverter`
  (`src/main/java/com/mockingbirdbank/config/logging/`), which redacts
  password/token/API-key-shaped `key=value` pairs, `Authorization: Bearer/Basic`
  headers, and card-number-like digit sequences before a line is written
  anywhere - not just at individual call sites. `DataInitializer` itself no
  longer logs the seeded demo password (see git history if curious what that
  looked like before).

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
