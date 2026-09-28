# Runbook: Rolling back a bad deploy

Symptoms: the newest deploy is the actual problem - errors/crashes started right after a push to
`master`, not because of an external outage.

## 1. Confirm which deploy is bad, and find the last good one

Every push to `master` that gets built produces a GHCR image tagged both `latest` and
`sha-<full commit sha>` (`.github/workflows/publish-image.yml`), and records a real [GitHub
Deployment](https://github.com/ajpahl1008/mockingbird-bank/deployments) with a `success`/
`failure` status from that workflow's post-publish smoke test (it actually boots the image
against Postgres and checks `/actuator/health` before marking it successful - a deploy marked
`failure` there never passed even that basic bar). Check that list (or `git log --oneline
master`) to find the last commit sha whose deployment was healthy.

```bash
gh api repos/ajpahl1008/mockingbird-bank/deployments --jq '.[] | {sha: .sha, environment: .environment}' | head
```

## 2. Immediate mitigation: roll the Kubernetes Deployment back

If the cluster's `mockingbird-app` Deployment was updated in place (not recreated) for the bad
release, a standard rollback works without needing to rebuild anything:

```bash
kubectl -n mockingbird-bank rollout history deploy/mockingbird-app
kubectl -n mockingbird-bank rollout undo deploy/mockingbird-app          # back one revision
# or: kubectl -n mockingbird-bank rollout undo deploy/mockingbird-app --to-revision=<n>
kubectl -n mockingbird-bank rollout status deploy/mockingbird-app        # watch it come back healthy
```

This is fastest but only recovers the *previous* revision, and only if that revision is actually
still known-good - check the deployment history from step 1 first.

## 3. Redeploy a specific known-good image

`k8s/values.yaml`'s `image: <+artifact.image>` is resolved by whatever deploy pipeline promotes
an image (the values file is deliberately the only place a Harness expression is written - see
the comment in that file). Point that same pipeline at the last known-good `sha-<commit>` tag
from step 1 instead of `latest`, and re-run it. This is the right choice when more than one bad
revision has landed since the last good one, since `rollout undo` alone won't skip past a bad
intermediate revision cleanly.

## 4. Fix it properly: revert the source, don't just roll back the running process

A live rollback (steps 2-3) buys time, but `master` still has the bad commit on it - the next
unrelated deploy would reintroduce the same bug. Revert it through the normal process:

```bash
git revert <bad-commit-sha>
git push origin <a-branch>   # master is protected: this still needs a PR + passing CI
```

Open a PR, let CI (`.github/workflows/ci.yml`) and the automated Droid review run, merge once
green. That push to `master` triggers `publish-image.yml` again, producing a new image and a
new, genuinely-verified GitHub Deployment - at which point steps 2-3 are no longer standing on a
reverted-but-not-actually-fixed codebase.

## 5. Afterward

If the bad deploy passed CI and the post-publish smoke test but still broke something in
production, that's a real gap in test/smoke-test coverage worth its own follow-up issue - the
smoke test only checks that the app *starts* and reports healthy, not that every feature works
(see [Interactive QA](../../AGENTS.md#interactive-qa-browser-driven-smoke-test) for the closer,
real-browser check, which currently only runs on demand rather than gating every deploy).
