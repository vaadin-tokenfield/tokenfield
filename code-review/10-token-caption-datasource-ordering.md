# 10 — Token caption lost when container set after property datasource

| Field | Value |
|-------|-------|
| **Severity** | 🟠 Medium |
| **Complexity** | Low |
| **Status** | TODO |
| **GitHub Issue** | [#8 — Button caption not properly displayed when using container data source](https://github.com/vaadin-tokenfield/tokenfield/issues/8) |

## Affected Files

- `tokenfield/src/main/java/org/vaadin/tokenfield/TokenField.java:673-679`

## Problem

`getTokenCaption` checks `cb.containsId(tokenId)` before retrieving the item caption:

```java
public String getTokenCaption(Object tokenId) {
    if (cb.containsId(tokenId)) {
        return cb.getItemCaption(tokenId);
    } else {
        return "" + tokenId;    // ← fallback: id.toString()
    }
}
```

Token buttons are created during `setValue`/`setPropertyDataSource`. If `setContainerDataSource`
is called **after** `setPropertyDataSource`, the container is not yet populated when
`configureTokenButton` → `getTokenCaption` runs — `cb.containsId` returns false and captions
fall back to the raw item id string.

Steps that trigger the bug:
1. `setTokenCaption(itemId, "Friendly Name")` — sets caption in ComboBox
2. `setPropertyDataSource(someProperty)` — triggers token buttons to render
3. `setContainerDataSource(container)` — container added too late

Pre-added tokens via `setPropertyDataSource` show id instead of caption. Switching call order
(set container first, then property datasource) works around it but is unintuitive.

## Fix

Two approaches:

**Option A — Remove `containsId` guard** (as suggested in the original issue workaround):
```java
public String getTokenCaption(Object tokenId) {
    String caption = cb.getItemCaption(tokenId);
    return (caption != null && !caption.isEmpty()) ? caption : "" + tokenId;
}
```
`ComboBox.getItemCaption` returns `null` or empty string when item not found — safe fallback.

**Option B — Re-render buttons after container change:**
Override `setContainerDataSource` to call `rebuild()` after delegating to `cb`:
```java
public void setContainerDataSource(Container c) {
    cb.setContainerDataSource(c);
    rebuild();
}
```
This re-runs `configureTokenButton` for all existing tokens after the container is set.

Recommendation: Option A (simpler, no full rebuild needed).

## Checklist

- [ ] Change `getTokenCaption` to call `cb.getItemCaption(tokenId)` directly, fallback on null/empty
- [ ] Test: set container *after* property datasource — buttons show correct captions
- [ ] Test: token not in container — falls back to id.toString() (no regression)
- [ ] Test: `setTokenCaptionPropertyId` configured — property-based caption used correctly

## Related

- [02-remember-token-caption-id.md](02-remember-token-caption-id.md) — related caption/id confusion
  in `rememberToken`
- [06-generics-raw-types.md](06-generics-raw-types.md) — raw Object types make caption handling
  harder to verify
