#!/usr/bin/env bash
# Fails if a Java file uses an org.junit.jupiter.api.Assertions member other
# than assertThrows/fail. This project asserts via Truth
# (com.google.common.truth); assertThrows/fail stay JUnit-based because Truth
# has no equivalent for them (ExpectFailure verifies failures of Truth
# assertions themselves, not domain exceptions or bare test failures).
#
# Usage: check-junit-assertions-guard.sh <path> < content
# <path> is checked, not read; content comes from stdin - works for a staged
# git blob and for a not-yet-written editor buffer alike. Exits 0 (silently)
# for a non-.java path, or one with no disallowed Assertions member. Exits 1
# with a message on stderr otherwise. Same rule as
# scripts/check-junit-assertions-guard.jq, run by the Claude Code PreToolUse
# hook in .claude/settings.json (a separate implementation, since that hook
# has no shell step to call this script from).
set -euo pipefail

path=${1:?usage: check-junit-assertions-guard.sh <path> < content}

# Drain stdin unconditionally before any early exit - see check-jspecify-client-guard.sh.
content=$(cat -)

case "$path" in
    *.java) ;;
    *) exit 0 ;;
esac

allowed='assertThrows|fail'

bad=$(
    {
        printf '%s\n' "$content" \
            | grep -oE '(^|[^A-Za-z0-9_])Assertions\s*\.\s*[A-Za-z_][A-Za-z0-9_]*' \
            | sed -E 's/^.*Assertions\.//' \
            | grep -vE "^(${allowed})\$"
        if printf '%s\n' "$content" | grep -qE '(^|[^A-Za-z0-9_])Assertions\s*\.\s*\*'; then
            echo '*'
        fi
    } | sort -u || true
)

if [ -n "$bad" ]; then
    echo "BLOCKED: $path uses org.junit.jupiter.api.Assertions member(s) [$(printf '%s' "$bad" | tr '\n' ' ' | sed 's/ *$//')] - this project asserts via Truth (com.google.common.truth); only assertThrows/fail stay JUnit-based (no Truth equivalent)." >&2
    exit 1
fi

exit 0
