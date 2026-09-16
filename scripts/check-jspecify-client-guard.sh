#!/usr/bin/env bash
# Fails if a file under org/vaadin/tokenfield/client/** references org.jspecify.
# That package is GWT-compiled (Java 7 source level) and ships no
# JSpecify-translatable source, so importing org.jspecify there breaks the
# widgetset compile in every consuming project - see AGENTS.md.
#
# Usage: check-jspecify-client-guard.sh <path> < content
# <path> is checked, not read; the file's content comes from stdin, so this
# works equally for a staged git blob and for a not-yet-written editor buffer.
# Exits 0 (silently) for a path outside org/vaadin/tokenfield/client/, or one
# that doesn't reference org.jspecify. Exits 1 with a message on stderr
# otherwise. Shared by .githooks/pre-commit and .claude/settings.json.
set -euo pipefail

path=${1:?usage: check-jspecify-client-guard.sh <path> < content}

# Drain stdin unconditionally before any early exit: a caller piping into this
# script (git show ... | guard) gets SIGPIPE - and, under pipefail, a false
# failure - if this process exits while its end of the pipe is still open.
content=$(cat -)

case "$path" in
    */org/vaadin/tokenfield/client/* | org/vaadin/tokenfield/client/*) ;;
    *) exit 0 ;;
esac

if printf '%s' "$content" | grep -q 'org\.jspecify'; then
    echo "BLOCKED: $path references org.jspecify, but org.vaadin.tokenfield.client is GWT-compiled (Java 7 source level, see AGENTS.md) and ships no JSpecify-translatable source; importing org.jspecify there breaks the widgetset compile in every consuming project." >&2
    exit 1
fi

exit 0
