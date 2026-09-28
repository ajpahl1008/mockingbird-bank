# Runbooks

Practical, copy-pasteable steps for the failure modes this app can actually hit - written
against the real topology in [`../architecture.md`](../architecture.md) (single Spring Boot
process, one external PostgreSQL, deployed as `mockingbird-app` in the `mockingbird-bank`
namespace). Each one assumes you're starting from a Slack alert in `#mbb-ops` (see
[Alerting](../../AGENTS.md#observability--resilience)) or someone noticing the app is behaving
badly.

- [App won't start](./app-wont-start.md)
- [Database is down or unreachable](./database-down.md)
- [Error spike / elevated failure rate](./error-spike.md)
- [Rolling back a bad deploy](./rollback.md)

## Useful before you start

```bash
kubectl -n mockingbird-bank get pods                       # is it even running?
kubectl -n mockingbird-bank logs deploy/mockingbird-app --tail=200
kubectl -n mockingbird-bank describe pod -l app=mockingbird-app   # events, restart count, probe failures
curl -s http://<service-ip>/actuator/health | jq            # from inside the LAN
```

Every runbook below also assumes you can reach the running instance's actuator endpoints
(`/actuator/health`, `/actuator/metrics/product.events`, `/actuator/profiling` - all
authenticated except `health`, see `SecurityConfig`) and the analytics event log
(`build/logs/analytics-events.log` in the container, or wherever your log shipper forwards it).
