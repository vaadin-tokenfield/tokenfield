# 11 — ComboBox suggestion popup may appear when delete key removes token

| Field | Value |
|-------|-------|
| **Severity** | 🟡 Low |
| **Complexity** | Low |
| **Status** | TODO |
| **GitHub Issue** | [#4 — Attempt to prevent suggestion menu when backspacing/deleting](https://github.com/vaadin-tokenfield/tokenfield/issues/4) |

## Affected Files

- `tokenfield/src/main/java/org/vaadin/tokenfield/client/ui/VTokenField.java:17-35`

## Problem

When the input is empty and the user presses BACKSPACE/DELETE to remove a token, the
`onKeyDown` handler fires `fireDeleteListeners()` and returns early — which prevents
`super.onKeyDown` from running for that event:

```java
public void onKeyDown(KeyDownEvent event) {
    if (!enabled || readonly) { return; }
    int kc = event.getNativeKeyCode();
    if (kc == KeyCodes.KEY_BACKSPACE || kc == KeyCodes.KEY_DELETE) {
        if (event.getSource() instanceof TextBox
                && "".equals(((TextBox) event.getSource()).getText())) {
            if ((kc == KeyCodes.KEY_BACKSPACE && !after)
                    || (kc == KeyCodes.KEY_DELETE && after)) {
                fireDeleteListeners();
                return;   // ← stops keyDown processing
            }
        }
    }
    super.onKeyDown(event);
}
```

However, `VFilterSelect` (the parent) also listens to `onKeyUp` to trigger the suggestion
popup. If the ComboBox popup logic responds to the key-up event corresponding to this key-down,
the suggestion popup may still open momentarily even though a token was deleted.

The original issue (2010) requested using both `onKeyDown` and `onKeyUp`. Whether this remains
an issue depends on the Vaadin 7 `VFilterSelect` implementation — needs browser testing to
confirm.

**Note:** This is the only client-side change (GWT) that requires a widgetset recompile to test.

## Fix

Override `onKeyUp` in `VTokenField` to suppress the suggestion-popup key-up event when
the corresponding key-down triggered a delete:

```java
private boolean deleteHandled = false;

public void onKeyDown(KeyDownEvent event) {
    // ... existing logic ...
    if (/* delete condition */) {
        fireDeleteListeners();
        deleteHandled = true;
        return;
    }
    deleteHandled = false;
    super.onKeyDown(event);
}

@Override
public void onKeyUp(KeyUpEvent event) {
    if (deleteHandled) {
        deleteHandled = false;
        return;  // suppress popup trigger
    }
    super.onKeyUp(event);
}
```

Alternatively, check if `VFilterSelect` already handles this case in Vaadin 7 — if
`onKeyUp` is not used for popup triggering, no change is needed.

## Checklist

- [ ] Inspect `VFilterSelect.onKeyUp` in the Vaadin 7 source to confirm if popup triggers on keyUp
- [ ] If yes: override `onKeyUp` in `VTokenField` to suppress after a handled delete
- [ ] Add `@Override` to existing `onKeyDown` (also tracked in [07-dead-code-and-style.md](07-dead-code-and-style.md))
- [ ] Test: backspace on empty input — no suggestion popup appears
- [ ] Test: backspace on non-empty input — suggestion popup still works normally

## Related

- [01-after-delete-key-sync.md](01-after-delete-key-sync.md) — DELETE key in AFTER mode broken
  (separate but same keyboard subsystem)
- [07-dead-code-and-style.md](07-dead-code-and-style.md) — missing `@Override` on `onKeyDown`
