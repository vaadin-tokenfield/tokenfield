# 09 — New typed token does not clear input field

| Field | Value |
|-------|-------|
| **Severity** | 🟠 Medium |
| **Complexity** | Trivial |
| **Status** | TODO |
| **GitHub Issue** | [#14 — Add new token will not delete the input field's content](https://github.com/vaadin-tokenfield/tokenfield/issues/14) |

## Affected Files

- `tokenfield/src/main/java/org/vaadin/tokenfield/TokenField.java:251-268`

## Problem

After the user types a new token and presses Enter, the input field retains the typed text.
The `NewItemHandler` in the constructor does not call `cb.setValue(null)` after handling
the new item, while the `ValueChangeListener` (which handles selecting an *existing* item)
does:

```java
// ValueChangeListener — correctly clears input after selecting existing item:
public void valueChange(ValueChangeEvent event) {
    final Object tokenId = event.getProperty().getValue();
    if (tokenId != null) {
        onTokenInput(tokenId);
        cb.setValue(null);   // ← clears input
        cb.focus();
    }
}

// NewItemHandler — does NOT clear input after typing a new item:
public void addNewItem(String tokenId) {
    if (isReadOnly()) {
        throw new Property.ReadOnlyException();
    }
    onTokenInput(tokenId);
    if (rememberNewTokens) {
        rememberToken(tokenId);
    }
    cb.focus();              // ← cb.setValue(null) missing
}
```

Result: typed text stays in the input box after adding, requiring manual clearing.

## Fix

Add `cb.setValue(null)` before `cb.focus()` in the `NewItemHandler`:

```java
public void addNewItem(String tokenId) {
    if (isReadOnly()) {
        throw new Property.ReadOnlyException();
    }
    onTokenInput(tokenId);
    if (rememberNewTokens) {
        rememberToken(tokenId);
    }
    cb.setValue(null);   // ← add this
    cb.focus();
}
```

## Checklist

- [ ] Add `cb.setValue(null)` before `cb.focus()` in `NewItemHandler.addNewItem`
- [ ] Test: type new token → Enter → input clears
- [ ] Test: select existing token → input clears (regression check)
- [ ] Test: comma-separated demo (overrides `onTokenInput`) — still works after fix

## Related

- [01-after-delete-key-sync.md](01-after-delete-key-sync.md) — same `addNewItem` code path is
  involved in JPAContainer crash (#15)
- [02-remember-token-caption-id.md](02-remember-token-caption-id.md) — `rememberToken` called
  in same block
