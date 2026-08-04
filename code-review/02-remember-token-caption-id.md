# 02 — `rememberToken` adds item by caption instead of id

| Field | Value |
|-------|-------|
| **Severity** | 🔴 High |
| **Complexity** | Low |
| **Status** | TODO |
| **GitHub Issues** | [#15 — JPAContainer crash](https://github.com/vaadin-tokenfield/tokenfield/issues/15) |

## Affected Files

- `tokenfield/src/main/java/org/vaadin/tokenfield/TokenField.java:275-285`

## Problem

```java
protected void rememberToken(String tokenId) {
    if (cb.addItem(getTokenCaption(tokenId)) != null) {   // ← adds by CAPTION as key
        if (getTokenCaptionPropertyId() != null) {
            cb.getContainerProperty(tokenId, getTokenCaptionPropertyId())  // ← looks up by ID
                    .setValue(tokenId);                   // ← NPE when caption ≠ id
        }
    }
}
```

`cb.addItem(x)` uses `x` as the item ID. When `getTokenCaption(tokenId)` returns something
different from `tokenId` (i.e. a caption property is configured), the item is stored under
the caption string. The subsequent `getContainerProperty(tokenId, ...)` call then uses the
original `tokenId` to look up that item — which does not exist under that key —
and returns `null`. Calling `.setValue(...)` on `null` throws a `NullPointerException`.

This is latent: it only surfaces when a `tokenCaptionPropertyId` is set AND the caption
differs from the id (the exact use-case documented in the class Javadoc as requiring a
proper `setTokenCaptionMode` setup).

## Fix

Add the item using `tokenId` as the key, then set the caption property separately:

```java
protected void rememberToken(String tokenId) {
    if (cb.addItem(tokenId) != null) {
        if (getTokenCaptionPropertyId() != null) {
            cb.getContainerProperty(tokenId, getTokenCaptionPropertyId())
                    .setValue(getTokenCaption(tokenId));
        }
    }
}
```

## Checklist

- [ ] Change `cb.addItem(getTokenCaption(tokenId))` → `cb.addItem(tokenId)`
- [ ] Verify property value set to caption (not `tokenId` again — currently sets id as caption value)
- [ ] Test with `setTokenCaptionPropertyId` configured — token button should show caption, not id
- [ ] Test without caption property — base case must still work

## Related

- [06-generics-raw-types.md](06-generics-raw-types.md) — `getTokenCaption` fallback returns `"" + tokenId` (raw type smell nearby)
