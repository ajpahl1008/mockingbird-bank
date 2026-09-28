# Architecture

Mockingbird Bank is a single Spring Boot process that serves a server-rendered Vaadin UI and
talks to one external PostgreSQL database. There are no other backing services.

## Request flow

```mermaid
flowchart LR
    Browser -->|HTTPS / WebSocket| Vaadin[Vaadin Flow + React UI\n(server-side views)]
    Vaadin --> Security[Spring Security\n+ VaadinSecurityConfigurer]
    Security --> Services[Application services\nAccountService / TransactionService]
    Services --> Repos[Spring Data JPA repositories]
    Repos --> DB[(PostgreSQL)]
    Flyway[Flyway migrations] -.->|schema, on startup| DB
    Actuator[/actuator/health/] -.->|readiness probe| K8s[Kubernetes]
```

- **Browser <-> Vaadin**: Vaadin Flow keeps UI state server-side; the browser talks to it over
  HTTPS plus a WebSocket for push updates. There is no separate REST/JSON API for the frontend
  to call - views (`src/main/java/com/mockingbirdbank/ui/`) invoke application services
  directly, in-process.
- **Spring Security**: every route except `/actuator/health/**` requires an authenticated
  session; unauthenticated requests are redirected to `LoginView`. See
  `security/SecurityConfig.java`.
- **Application services**: `AccountService` and `TransactionService`
  (`service/`) are the one seam between the UI and persistence - they contain the only business
  logic (e.g., resolving "current holder" from the security context, summing balances).
- **Persistence**: Spring Data JPA repositories (`repository/`) over a single PostgreSQL
  database. Flyway (`src/main/resources/db/migration/`) owns the schema; Hibernate's
  `ddl-auto: validate` only checks entity mappings still match what Flyway created - it never
  changes the schema itself.

## Deployment topology

```mermaid
flowchart LR
    LB[LoadBalancer Service\n(MetalLB, fixed LAN IP)] --> Pod[mockingbird-app Pod\n(single replica)]
    Pod -->|SPRING_DATASOURCE_* from Secret| ExternalPG[(External LAB PostgreSQL\nnot in-cluster)]
    Pod -->|/actuator/health| Probe[Kubernetes readiness probe]
```

The app runs as a single-replica Deployment in the `mockingbird-bank` namespace, exposed via a
`LoadBalancer` Service pinned to a static MetalLB-assigned IP (see `k8s/03-app.yaml`).
Datasource credentials come from a Kubernetes Secret; the database itself is an external
Postgres instance the cluster doesn't manage (see `k8s/01-app-db-secret.yaml` and
`scripts/bootstrap-lab-db.sql`), not an in-cluster database. There is no in-cluster cache, queue,
or additional microservice - this is intentionally a single-service application.

## External dependencies

| Dependency | Used for | Failure mode |
|---|---|---|
| PostgreSQL | All persistence (accounts, holders, transactions, app users) | App fails health checks and can't serve authenticated views; Flyway also blocks startup if migrations can't run. |
| None else | - | The app makes no calls to third-party APIs or other internal services. |

See also [AGENTS.md](../AGENTS.md) for build/run/test commands and code layout.
