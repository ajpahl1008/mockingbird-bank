# Runbook: Database is down or unreachable

Symptoms: `/actuator/health` reports `"status":"DOWN"` with a `db` component failure, every
authenticated page fails to load account/transaction data, and (once the circuit breakers trip -
see step 3) requests start failing fast with `ServiceUnavailableException` instead of hanging.

This app has exactly one external dependency (see [`../architecture.md`](../architecture.md)) -
PostgreSQL - so "database down" is close to "app down" for anything beyond the login page itself.

## 1. Confirm it's actually the database, not the app

```bash
curl -s http://<service-ip>/actuator/health | jq
```

A `"db":{"status":"DOWN", ...}` entry (with the underlying `PSQLException`/timeout in its
`details`) confirms this. If `health` itself is `UP` but pages are erroring, this isn't a
database problem - check [Error spike](./error-spike.md) instead.

## 2. Check the database itself

```bash
# From wherever it's reachable (the external LAB Postgres per architecture.md, or docker compose
# for local dev):
pg_isready -h <host> -p 5432 -U admin
psql -h <host> -U admin -d mydatabase -c 'select 1;'
```

Common causes: the Postgres process/container is actually down, it's out of connections
(`FATAL: too many connections`, check `max_connections` vs. HikariCP's pool size), it's out of
disk space, or a network/firewall change cut off the app's egress to it.

## 3. What the app does on its own while the database is down

- `AccountService`/`TransactionService` wrap their repository calls in a Resilience4j circuit
  breaker (see `com.mockingbirdbank.resilience`). After enough consecutive failures (50% of the
  last 10 calls, minimum 10 calls - see `ResilienceConfig`), the breaker opens: further calls
  fail immediately with `ServiceUnavailableException` instead of piling up waiting on a dead
  connection pool. It automatically tries a few trial calls again after 10 seconds to see if the
  database has recovered - no manual reset needed once the database is back.
- Every failed login/account/error event is still tracked via `AnalyticsEventService` (metrics +
  `build/logs/analytics-events.log`), so the blast radius is visible even during the outage.
- CI/CD is unaffected - this is a production data-plane outage, not a build or pipeline problem.

## 4. Mitigation while the database is being fixed

There isn't a fallback data source for this app (see the "External dependencies" table in
`architecture.md` - Postgres is the only one, and by design there's no local cache to fail over
to for a read-only account dashboard). Once the database is confirmed back:

```bash
kubectl -n mockingbird-bank rollout restart deploy/mockingbird-app
```

This isn't strictly necessary (the circuit breaker's automatic half-open retries would recover on
their own within ~10s), but forces an immediate fresh connection pool rather than waiting out the
breaker's own retry timing if you want the fastest possible recovery.

## 5. Afterward

Check whether the outage was self-inflicted (a migration that locked a table for too long, a
connection leak from a bug) vs. external (the LAB Postgres host itself had a problem) and file
whichever follow-up is appropriate - this repo's issue templates
(`.github/ISSUE_TEMPLATE/bug_report.yml`) cover that.
