## What does this change do, and why?

<!-- One or two sentences. Link the issue it closes, if any: "Closes #123". -->

## How was this verified?

<!--
`./gradlew build` covers compilation, the full test suite, and every static-analysis
check - say so if that's genuinely all you ran. For anything UI-facing or
infra-facing, say what you actually did beyond that (ran ./scripts/run-interactive-qa.sh,
clicked through it locally against a real Postgres, etc.) - "should work" isn't a
verification step.
-->

## Checklist

- [ ] `./gradlew build` passes locally (compiles, tests, Checkstyle/Spotless/PMD/CPD, JaCoCo thresholds)
- [ ] New/changed behavior has test coverage, or the gap is explained above
- [ ] `AGENTS.md` is updated if this changes how to build, run, test, or deploy the app
- [ ] UI-facing changes that aren't a straightforward bug fix are behind a feature flag (see [Feature flags](../AGENTS.md#feature-flags))
- [ ] No secrets, real customer data, or PII in code, migrations, fixtures, or logs
