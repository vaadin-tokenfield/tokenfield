#!/usr/bin/env bash
# Agent PostToolUse hook: lints a file right after the agent edited it - markdownlint
# for .md, cspell for everything. Reads the hook JSON on stdin (tool_input.file_path);
# exit 2 feeds the findings back to the agent so it fixes them before moving on.
set -euo pipefail

file=$(jq -r '.tool_input.file_path // empty')
[ -f "$file" ] || exit 0

root=$(git -C "$(dirname "$0")" rev-parse --show-toplevel)
rel=${file#"$root"/}
status=0
for lint in lint-markdown.sh spellcheck.sh; do
    "$root/scripts/$lint" "$rel" < "$file" || status=$?
done
if [ "$status" -ne 0 ]; then
    echo "Lint failed (.markdownlint.jsonc / .cspell.json); fix the findings above, or add a" \
        "genuine project term to project-words.txt." >&2
    exit 2
fi
