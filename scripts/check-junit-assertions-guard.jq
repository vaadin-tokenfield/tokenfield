# Claude Code PreToolUse guard: denies an Edit/Write/MultiEdit that would put
# a disallowed org.junit.jupiter.api.Assertions member (anything but
# assertThrows/fail) into a .java file. Same rule as
# scripts/check-junit-assertions-guard.sh, which enforces it at commit time
# via .githooks/pre-commit; this is a separate implementation because this
# hook has no shell step to call that script from - the tool-call JSON
# arrives on stdin and is filtered here directly.
#
# Invoked as: jq -c -f scripts/check-junit-assertions-guard.jq
def isJava: ((.tool_input.file_path // "") | test("\\.java$"));

def texts:
  ([.tool_input.content, .tool_input.new_string]
    + ((.tool_input.edits // []) | map(.new_string)))
  | map(select(. != null));

def badMembers:
  texts
  | map([match("(?:^|[^A-Za-z0-9_])Assertions\\s*\\.\\s*([A-Za-z_][A-Za-z0-9_]*)"; "g") | .captures[0].string])
  | flatten
  | map(select(. != "assertThrows" and . != "fail"))
  | unique;

def hasWildcard: texts | any(test("(?:^|[^A-Za-z0-9_])Assertions\\s*\\.\\s*\\*"));

if isJava and ((badMembers | length) > 0 or hasWildcard) then
  {
    hookSpecificOutput: {
      hookEventName: "PreToolUse",
      permissionDecision: "deny",
      permissionDecisionReason: (
        "org.junit.jupiter.api.Assertions member(s) not allowed: "
        + ((badMembers + (if hasWildcard then ["*"] else [] end)) | join(", "))
        + " - this project asserts via Truth (com.google.common.truth); only assertThrows/fail stay JUnit-based (no Truth equivalent)."
      )
    }
  }
else {} end
