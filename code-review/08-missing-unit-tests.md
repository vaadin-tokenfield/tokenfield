# 08 — No unit tests

| Field | Value |
|-------|-------|
| **Severity** | 🟡 Low |
| **Complexity** | Medium |
| **Status** | TODO |

## Affected Files

- `tokenfield/tokenfield/src/test/` — does not exist

## Problem

The addon module has no unit or integration tests. For an addon published to Vaadin Directory
this is a significant best-practice gap:

- Regressions in corner cases (null value, read-only state, AFTER-mode keyboard) go undetected.
- Several findings in this review (issues #1, #2, #3) would have been caught by tests.
- Addon consumers cannot verify behaviour on their target Vaadin 7 version.

## Fix

Add a `src/test/java` tree with JUnit 4 tests (matches Vaadin 7 era).

Priority test cases (aligned with other findings in this review):

| Test | Covers |
|------|--------|
| `addToken` on empty field creates token button | basic smoke |
| `addToken` duplicate is silently ignored | de-dup logic |
| `removeToken` on empty/null field does not throw | issue #3 |
| `addToken` while read-only — button not clickable | issue #3 |
| `rememberToken` with caption property — item key is id not caption | issue #2 |
| `setTokenInsertPosition(AFTER)` — DELETE key fires delete | issue #1 |
| `setReadOnly(true)` hides input, `setReadOnly(false)` shows it | read-only toggle |
| `setValue(set)` → tokens rendered; `setValue(null)` → all removed | value binding |

### Notes

- Vaadin 7 server-side tests use `com.vaadin.testbench` or plain JUnit without a running server
  for pure-logic tests. Logic under test (token add/remove/state) is server-side only — no GWT
  compilation needed for most cases.
- GWT/client-side behaviour (keyboard) requires TestBench or a Selenium approach.

## Checklist

- [ ] Create `tokenfield/src/test/java/org/vaadin/tokenfield/` directory
- [ ] Add JUnit 4 dependency to `tokenfield/pom.xml` (test scope)
- [ ] Write `TokenFieldTest` covering smoke + de-dup + null-safety cases
- [ ] Write test for `rememberToken` caption-vs-id (covers issue #2)
- [ ] Write test for `setReadOnly` toggle
- [ ] (Optional) TestBench test for keyboard DELETE in AFTER mode

## Related

- [01-after-delete-key-sync.md](01-after-delete-key-sync.md) — keyboard test would catch issue #1
- [02-remember-token-caption-id.md](02-remember-token-caption-id.md) — rememberToken test would catch issue #2
- [03-null-and-state-handling.md](03-null-and-state-handling.md) — null/state tests would catch issue #3
