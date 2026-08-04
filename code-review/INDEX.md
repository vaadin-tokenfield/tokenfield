# TokenField Addon — Code Review Findings

| # | File | Title | Severity | Complexity | Status |
|---|------|-------|----------|------------|--------|
| 1 | [01-after-delete-key-sync.md](01-after-delete-key-sync.md) | AFTER-mode delete key broken (client/server sync) | 🔴 High | Medium | TODO |
| 2 | [02-remember-token-caption-id.md](02-remember-token-caption-id.md) | `rememberToken` adds item by caption instead of id | 🔴 High | Low | TODO |
| 3 | [03-null-and-state-handling.md](03-null-and-state-handling.md) | Null safety & read-only state in token operations | 🟠 Medium | Low | TODO |
| 4 | [04-deprecated-vaadin-apis.md](04-deprecated-vaadin-apis.md) | Deprecated Vaadin 7 API calls | 🟠 Medium | Low | TODO |
| 5 | [05-hardcoded-theme-and-strings.md](05-hardcoded-theme-and-strings.md) | Hardcoded theme (Reindeer) and UI strings | 🟠 Medium | Low | TODO |
| 6 | [06-generics-raw-types.md](06-generics-raw-types.md) | Raw types and unchecked casts | 🟠 Medium | High | TODO |
| 7 | [07-dead-code-and-style.md](07-dead-code-and-style.md) | Dead code and style issues | 🟡 Low | Trivial | TODO |
| 8 | [08-missing-unit-tests.md](08-missing-unit-tests.md) | No unit tests | 🟡 Low | Medium | TODO |
| 9 | [09-new-item-input-not-cleared.md](09-new-item-input-not-cleared.md) | New typed token does not clear input field | 🟠 Medium | Trivial | TODO |
| 10 | [10-token-caption-datasource-ordering.md](10-token-caption-datasource-ordering.md) | Token caption lost when container set after property datasource | 🟠 Medium | Low | TODO |
| 11 | [11-suggestion-popup-on-delete.md](11-suggestion-popup-on-delete.md) | ComboBox popup may appear when delete removes token | 🟡 Low | Low | TODO |

*Issues 9–11 discovered via GitHub issue analysis — not in original static code review.*

## Fix Priority
1. → 2./9. → 3./10. → 4./5. → 6./11. → 7./8.

## GitHub Issues
See [github-issues-evaluation.md](github-issues-evaluation.md) for full evaluation of all 15 imported GitHub issues.
