# 03 — Null safety & read-only state in token operations

| Field | Value |
|-------|-------|
| **Severity** | 🟠 Medium |
| **Complexity** | Low |
| **Status** | TODO |
| **GitHub Issues** | [#13 — Readonly TokenField still removes tokens (ReadOnlyException)](https://github.com/vaadin-tokenfield/tokenfield/issues/13) |

## Affected Files

- `tokenfield/src/main/java/org/vaadin/tokenfield/TokenField.java:319,381-403,447-453`

## Problem

Three closely related robustness gaps in token value/state management:

### 1. `removeToken` NPE on null value (line 447-449)

```java
public void removeToken(Object tokenId) {
    Set<Object> set = (Set<Object>) getValue();
    LinkedHashSet<Object> newSet = new LinkedHashSet<Object>(set);  // ← NPE if set == null
    newSet.remove(tokenId);
    setValue(newSet);
}
```

If the field's value is `null` (initial state or unbound), `removeToken` throws NPE.
`addToken` (line 425-426) correctly guards this with a null check; `removeToken` does not.

### 2. New token button ignores read-only state (line 381-403)

`addTokenButton` creates a `Button` but never calls `b.setReadOnly(readOnly)`.
If tokens are programmatically added while the field is in read-only mode, the resulting
button is interactive and allows click-to-remove, bypassing the read-only constraint.

`setReadOnly` (line 539) correctly handles *existing* buttons, but `addTokenButton` does not
apply the current state to the *new* button.

### 3. Dead null-check on `old` in `setInternalValue` (line 319)

```java
Set<Object> old = buttons.keySet();
// ...
if (old == null) {           // ← buttons.keySet() is never null
    old = new HashSet<Object>();
}
```

`LinkedHashMap.keySet()` always returns a non-null `Set`. The `if (old == null)` branch is
unreachable dead code that adds visual noise and implies a false assumption.

## Fix

**1.** Guard null in `removeToken`:
```java
public void removeToken(Object tokenId) {
    Set<Object> set = (Set<Object>) getValue();
    if (set == null) {
        return;
    }
    LinkedHashSet<Object> newSet = new LinkedHashSet<Object>(set);
    newSet.remove(tokenId);
    setValue(newSet);
}
```

**2.** Apply current read-only state in `addTokenButton`:
```java
private void addTokenButton(final Object val) {
    Button b = new Button();
    configureTokenButton(val, b);
    b.setReadOnly(isReadOnly());   // ← add this line
    b.addListener(...);
    ...
}
```

**3.** Remove dead null-check on `old` (keep a simple `new HashSet<>(buttons.keySet())`).

## Checklist

- [ ] Add null guard to `removeToken` (return early if `getValue() == null`)
- [ ] Apply `isReadOnly()` to new button in `addTokenButton`
- [ ] Remove unreachable `if (old == null)` branch in `setInternalValue`
- [ ] Test `removeToken` called on empty field — no exception
- [ ] Test programmatic `addToken` while field is read-only — button not clickable

## Related

- [01-after-delete-key-sync.md](01-after-delete-key-sync.md) — state sync issues in delete subsystem
