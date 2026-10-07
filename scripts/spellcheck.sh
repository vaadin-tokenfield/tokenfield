#!/usr/bin/env bash
# Runs cspell (config: .cspell.json, en-US).
#
# Usage: spellcheck.sh <path> < content   (one file; .agents/hooks/lint-text.sh)
#        spellcheck.sh --staged           (all staged files; .githooks/pre-commit)
# Single-file mode has the lint-markdown.sh contract: <path> is checked, not read; the
# content comes from stdin. Binary content and .cspell.json ignorePaths pass silently.
# Exits 0 (with a warning) when npx is unavailable, 1 with the findings on stderr.
set -euo pipefail

arg=${1:?usage: spellcheck.sh <path> < content | spellcheck.sh --staged}

if ! command -v npx >/dev/null 2>&1; then
    [ "$arg" = --staged ] || cat >/dev/null # drain stdin, see check-jspecify-client-guard.sh
    echo "WARNING: npx not found, skipping cspell" >&2
    exit 0
fi

cspell() {
    npx --yes cspell@10.3.6 --no-progress --no-summary --no-must-find-files "$@"
}

cd "$(git -C "$(dirname "$0")" rev-parse --show-toplevel)"
if [ "$arg" = --staged ]; then
    # One cspell run (npx startup dominates) over the index snapshot, staged config
    # included, so unstaged edits neither hide nor cause findings.
    tmp=$(mktemp -d)
    trap 'rm -rf "$tmp"' EXIT
    git diff --cached --name-only --diff-filter=ACM -z > "$tmp/.files"
    [ -s "$tmp/.files" ] || exit 0
    git checkout-index --prefix="$tmp/" -z --stdin < "$tmp/.files"
    git checkout-index --prefix="$tmp/" -f .cspell.json project-words.txt
    cd "$tmp"
    if ! out=$(tr '\0' '\n' < .files | cspell --file-list stdin 2>&1); then
        printf '%s\n' "$out" >&2
        exit 1
    fi
    exit 0
fi

# stdin://<path> makes cspell apply ignorePaths and pick the file type by name.
if ! out=$(cspell "stdin://$arg" 2>&1); then
    printf '%s\n' "$out" >&2
    exit 1
fi
