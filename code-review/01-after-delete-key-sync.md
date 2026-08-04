# 01 — AFTER-mode delete key broken (client/server sync)

| Field | Value |
|-------|-------|
| **Severity** | 🔴 High |
| **Complexity** | Medium |
| **Status** | TODO |
| **GitHub Issues** | [#2 — Backspace deletes last token (partially fixed)](https://github.com/vaadin-tokenfield/tokenfield/issues/2), [#11 — Vaadin 7 compat (stale)](https://github.com/vaadin-tokenfield/tokenfield/issues/11), [#15 — JPAContainer crash](https://github.com/vaadin-tokenfield/tokenfield/issues/15) |

## Affected Files

- `tokenfield/src/main/java/org/vaadin/tokenfield/client/ui/VTokenField.java:13,25-26`
- `tokenfield/src/main/java/org/vaadin/tokenfield/client/ui/TokenFieldConnector.java:17`
- `tokenfield/src/main/java/org/vaadin/tokenfield/TokenComboBox.java:27-33`

## Problem

Three tightly connected issues in the same server→client delete-key subsystem:

### 1. `VTokenField.after` never set — keyboard delete silent in AFTER mode

`VTokenField.after` is declared (`VTokenField.java:13`) but never written. The key-handler
(`VTokenField.java:25-26`) branches on it to decide which key triggers deletion (BACKSPACE for
BEFORE, DELETE for AFTER). Because `after` is always `false`, the AFTER-mode branch is
unreachable — pressing DELETE when `InsertPosition.AFTER` is selected does nothing.

### 2. `paintContent` still used for the `after` attribute — not consumed

`TokenComboBox.paintContent` sends the insert-position via legacy UIDL painting:

```java
target.addAttribute("after", true);  // TokenComboBox.java:30
```

The connector (`TokenFieldConnector`) never overrides `onStateChanged` to read this attribute,
so the painted value is discarded. The Connector model (`TokenFieldConnector`) and the legacy
UIDL paint model (`paintContent`) are mixed — both used, but the connector side wins and the
paint side has no effect.

Additionally, `addVariable(this, "del", false)` (line 29) is dead: the delete flow now goes
through `TokenFieldServerRpc.deleteToken()`, making the variable round-trip unused.

### 3. `TokenFieldConnector.after` — declared but never used

`TokenFieldConnector.java:17` declares `protected boolean after = false;` — clearly a leftover
from a refactoring attempt. It is never written, never read, and does not propagate to
`VTokenField.after`.

## Fix

1. Create a `TokenFieldState` class extending `ComboBoxState` (or `AbstractComponentState`)
   with an `boolean after` field.
2. In `TokenComboBox`, set `getState().after = (insertPosition == InsertPosition.AFTER)` instead
   of using `paintContent`/`addAttribute`.
3. In `TokenFieldConnector`, override `onStateChanged(StateChangeEvent e)` and sync
   `getWidget().after = getState().after`.
4. Remove `paintContent` override from `TokenComboBox` entirely (or keep only genuinely needed
   super-call content).
5. Remove the dead `TokenFieldConnector.after` field.

## Checklist

- [ ] Create `TokenFieldState.java` with `boolean after` field
- [ ] Set `after` in `TokenComboBox` via state, not `paintContent`
- [ ] Remove `addVariable(this, "del", false)` from `paintContent`
- [ ] Remove `paintContent` override entirely (unless other attributes needed)
- [ ] Implement `onStateChanged` in `TokenFieldConnector` to sync `getWidget().after`
- [ ] Remove unused `TokenFieldConnector.after` field
- [ ] Verify DELETE key removes last token in AFTER mode

## Related

- [03-null-and-state-handling.md](03-null-and-state-handling.md) — other state management issues
- [04-deprecated-vaadin-apis.md](04-deprecated-vaadin-apis.md) — `requestRepaint()` in same `TokenComboBox`
