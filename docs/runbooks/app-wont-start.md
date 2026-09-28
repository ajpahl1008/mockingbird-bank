# Runbook: App won't start

Symptoms: the pod is `CrashLoopBackOff` / restarting repeatedly, or `bootRun`/the container exits
right after startup logs instead of reaching `Started Application in ... seconds`.

## 1. Get the actual failure, not just "it's down"

```bash
kubectl -n mockingbird-bank logs deploy/mockingbird-app --tail=200 --previous
```

`--previous` matters - once it's crashed and restarted, the current container's logs are empty
or just starting up again. Look at the end of the log for a stack trace; Spring Boot always logs
`APPLICATION FAILED TO START` above the actual cause.

## 2. Match the failure to a known cause

| Log signature | Cause | Fix |
|---|---|---|
| `SchemaManagementException: Schema validation: missing table [...]` | Flyway migrations didn't run against this database, or ran against the wrong one | Confirm `SPRING_DATASOURCE_URL` actually points at the database you think it does; check `flyway_schema_history` in that database. If Flyway itself failed (see below), fix that first - Hibernate's validation only runs after Flyway succeeds. |
| `FlywayException: Validate failed` / checksum mismatch | Someone edited an already-applied migration file in `src/main/resources/db/migration/` | Never edit a migration that's already run anywhere. Add a new one instead, or if this is a throwaway/dev database, `docker compose down -v` and let it re-migrate from empty. |
| `Connection to localhost:5432 refused` / `PSQLException: Connection refused` | Postgres isn't reachable at all - see [Database is down](./database-down.md) instead. |
| `Port 8080 was already in use` | Another instance (or an old `bootRun`) is still holding the port | `lsof -i :8080` locally, or check for a duplicate Deployment/pod in the cluster. |
| `BeanCreationException` for `entityManagerFactory` with no Flyway/connection cause above | Usually a real schema/entity mismatch, not infra - read the nested cause carefully | Compare the entity (`src/main/java/com/mockingbirdbank/model/`) against the migration that's supposed to have created its table/columns. |
| Nothing in the log past `Starting Application using Java ...` and no error at all | Container OOM-killed before Spring even logged a failure | `kubectl -n mockingbird-bank describe pod -l app=mockingbird-app` and check `Last State: Terminated, Reason: OOMKilled`; raise the container's memory limit. |
| `Vaadin is running in DEVELOPMENT mode` in a container that's supposed to be production | The image was built without `-Pvaadin.productionMode=true` (see `Dockerfile`) | Not a startup failure per se, but flags a bad image - rebuild properly, or see [Rollback](./rollback.md) to get back to a known-good one meanwhile. |

## 3. Local repro

If the same failure isn't obviously infra-specific (i.e. not "wrong database" or "OOM"), reproduce
it locally rather than iterating against the cluster:

```bash
docker compose up -d      # matching local Postgres
./gradlew bootRun
```

This is almost always faster to iterate on than redeploying, and the stack trace is identical.

## 4. If nothing above explains it

Check `/actuator/health` isn't the thing actually failing (a healthy process that fails its
*readiness probe* looks identical to "won't start" from `kubectl get pods`, but the logs will
show the app fully started with no errors). If so this is a networking/probe config issue, not
an application startup issue - check the `readinessProbe` block in `k8s/03-app.yaml` and that the
Service/Pod ports line up (`containerPort: 8080` on both sides).
