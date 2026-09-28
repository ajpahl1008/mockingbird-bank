#!/usr/bin/env bash
# Checks that AGENTS.md hasn't drifted from the code it describes: every
# `./gradlew <task>` it mentions must still be a real Gradle task, and every
# backticked file/directory path it references must still exist.
#
# Run directly: ./scripts/validate-agents-md.sh
# Also wired into the `validateAgentsMd` Gradle task (part of `check`) and the
# pre-commit hook (.githooks/pre-commit).
set -euo pipefail

cd "$(git rev-parse --show-toplevel)"

DOC="AGENTS.md"
status=0

if [[ ! -f "$DOC" ]]; then
    echo "$DOC not found - nothing to validate."
    exit 1
fi

echo "Validating Gradle tasks referenced in $DOC..."
tasks=$(grep -oE '\./gradlew [A-Za-z0-9_.:-]+( [A-Za-z0-9_.:-]+)*' "$DOC" \
    | sed -E 's#^\./gradlew ##' \
    | tr ' ' '\n' \
    | grep -vE '^-' \
    | sort -u)

while IFS= read -r t; do
    [[ -z "$t" ]] && continue
    if ! ./gradlew help --task "$t" >/dev/null 2>&1; then
        echo "  MISSING Gradle task referenced in $DOC: $t"
        status=1
    fi
done <<<"$tasks"

echo "Validating file/directory references in $DOC..."
# Build-output paths (e.g. build/reports/...) are documented result locations,
# not repo files, so they're expected to be absent on a fresh checkout.
paths=$(grep -oE '`[A-Za-z0-9_./-]+`' "$DOC" \
    | tr -d '`' \
    | grep -E '/' \
    | grep -vE '^build/' \
    | grep -vE '^/*$' \
    | sort -u)

while IFS= read -r p; do
    [[ -z "$p" ]] && continue
    clean="${p%/}"
    if [[ ! -e "$clean" ]]; then
        echo "  MISSING path referenced in $DOC: $p"
        status=1
    fi
done <<<"$paths"

if [[ $status -eq 0 ]]; then
    echo "AGENTS.md references are all valid."
fi
exit $status
