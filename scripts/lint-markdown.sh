#!/usr/bin/env bash
# Runs markdownlint-cli2 (config: .markdownlint.jsonc) on a Markdown file.
#
# Usage: lint-markdown.sh <path> < content
# Same contract as the check-*-guard.sh scripts: <path> is checked, not read; the
# content comes from stdin, so this works for a staged git blob and a file on disk.
# Exits 0 (silently) for a non-Markdown path, or when npx is unavailable (warns).
# Exits 1 with the findings (or npx's own error) on stderr otherwise. Shared by
# .githooks/pre-commit and .agents/hooks/lint-text.sh.
set -euo pipefail

path=${1:?usage: lint-markdown.sh <path> < content}

# Drain stdin before any early exit, avoiding SIGPIPE in the caller (see
# check-jspecify-client-guard.sh).
content=$(cat -)

case "$path" in
    *.md) ;;
    *) exit 0 ;;
esac

if ! command -v npx >/dev/null 2>&1; then
    echo "WARNING: npx not found, skipping markdownlint for $path" >&2
    exit 0
fi

# Run from the repo root so the config is picked up for stdin input.
cd "$(dirname "$0")/.."
if ! out=$(printf '%s\n' "$content" | npx --yes markdownlint-cli2@0.23.3 - 2>&1); then
    # Name the file in findings; pass npx/network failures through unchanged.
    findings=$(printf '%s\n' "$out" | awk -v p="$path" 'sub(/^stdin:/, p ":")')
    printf '%s\n' "${findings:-$out}" >&2
    exit 1
fi
