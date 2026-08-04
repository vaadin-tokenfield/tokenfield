# 06 — Raw types and unchecked casts

| Field | Value |
|-------|-------|
| **Severity** | 🟠 Medium |
| **Complexity** | High |
| **Status** | TODO |

## Affected Files

- `tokenfield/src/main/java/org/vaadin/tokenfield/TokenField.java:314,425,448,720,868`

## Problem

`TokenField` uses raw types and unchecked casts throughout because `CustomField` and the
Vaadin 7 `Container` API are not generic-friendly. This suppresses compile-time type safety
and risks `ClassCastException` at runtime if incorrect types are bound.

### Key sites

| Line | Code | Issue |
|------|------|-------|
| 314 | `Set<Object> vals = (Set<Object>) newValue;` | Unchecked cast from `Object` |
| 425 | `Set<Object> set = (Set<Object>) getValue();` | Unchecked cast in `addToken` |
| 448 | `Set<Object> set = (Set<Object>) getValue();` | Unchecked cast in `removeToken` |
| 720 | `public Collection getTokenIds()` | Raw `Collection` return type |
| 868 | `public Class<?> getType() { return Set.class; }` | Raw `Set.class`, not `Set<Object>.class` |

### Why complex

Full genericization would require `TokenField<T>` and threading the type parameter through
`CustomField<Set<T>>`, `addToken(T)`, `removeToken(T)`, etc. — a public API-breaking change
requiring callers to update. Intermediate option: suppress + document, or partial generic where
`getTokenIds()` returns `Collection<?>`.

## Fix Options

**Option A — Minimal (low risk, low impact):**
- Add `@SuppressWarnings("unchecked")` with explanatory comments at each cast site.
- Change `Collection getTokenIds()` → `Collection<?>`.
- Document the raw `Set.class` return in `getType()` Javadoc.

**Option B — Full generic (API-breaking):**
- Introduce `TokenField<T>` extending `CustomField<Set<T>>`.
- Type all public methods (`addToken(T)`, `removeToken(T)`, `getTokenIds()→Collection<T>`).
- Bump major version.

Recommendation: Option A now, Option B in next major version.

## Checklist

**Option A (recommended for now):**
- [ ] Add `@SuppressWarnings("unchecked")` + comment to `setInternalValue` cast (line 314)
- [ ] Add `@SuppressWarnings("unchecked")` + comment to `addToken` cast (line 425)
- [ ] Add `@SuppressWarnings("unchecked")` + comment to `removeToken` cast (line 448)
- [ ] Change `Collection getTokenIds()` return type to `Collection<?>`
- [ ] Add Javadoc note to `getType()` explaining raw `Set.class` limitation

**Option B (future major version):**
- [ ] Introduce `TokenField<T>` type parameter
- [ ] Update all method signatures
- [ ] Update demo and README

## Related

- [02-remember-token-caption-id.md](02-remember-token-caption-id.md) — related Object-typed container item handling
- [03-null-and-state-handling.md](03-null-and-state-handling.md) — some null risks amplified by raw types
