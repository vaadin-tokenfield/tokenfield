# 07 — Dead code and style issues

| Field | Value |
|-------|-------|
| **Severity** | 🟡 Low |
| **Complexity** | Trivial |
| **Status** | TODO |
| **GitHub Issues** | [#10 — Sortable TokenField (LinkedHashSet request, partially addressed)](https://github.com/vaadin-tokenfield/tokenfield/issues/10) |

## Affected Files

- `tokenfield/src/main/java/org/vaadin/tokenfield/TokenField.java:432,503-504,649,733-745`
- `tokenfield/src/main/java/org/vaadin/tokenfield/TokenComboBox.java:15`
- `tokenfield/src/main/java/org/vaadin/tokenfield/client/ui/VTokenField.java:17`

## Problem

Three unrelated but trivial cleanup items:

### 1. Commented-out code blocks (`TokenField.java`)

**Lines 503-504** — `setCompositionRoot` TODO comment:
```java
// TODO
// setCompositionRoot(layout);
```
Vaadin 7 `CustomField` does not use `setCompositionRoot` (that's `CustomComponent`). The
comment is misleading and the call would be wrong. Dead.

**Lines 733-745** — large `/*- ... -*/` block with commented `setHeight(String)` /
`setWidth(String)` overrides. Replaced by the float/Unit overrides just below. Dead.

### 2. Misleading variable type (`TokenField.java:432`)

```java
HashSet<Object> newSet = new LinkedHashSet<Object>(set);
```

Declared as `HashSet` but constructed as `LinkedHashSet` (subtype). Compiles fine but
misleads readers into thinking insertion order is not preserved (it is).
Fix: declare as `LinkedHashSet<Object>`.

### 3. Missing `serialVersionUID` on anonymous RPC instance (`TokenComboBox.java:15`)

```java
private TokenFieldServerRpc rpc = new TokenFieldServerRpc() { ... };
```

The anonymous class implements `ServerRpc` which extends `Serializable`. Missing
`serialVersionUID` triggers a compiler warning (if `-Xlint:serial` is active).

### 4. Missing `@Override` annotations (`VTokenField.java:17`, `TokenField.java:649`)

- `VTokenField.onKeyDown` overrides `VFilterSelect.onKeyDown` — no `@Override`.
- `TokenField.focus()` overrides `AbstractComponent.focus()` — no `@Override`.

These are not errors but suppress the safety net that would catch signature drift.

## Checklist

- [ ] Remove `// TODO / setCompositionRoot` comment block (`TokenField.java:503-504`)
- [ ] Remove commented-out `setHeight(String)` / `setWidth(String)` block (`TokenField.java:733-745`)
- [ ] Change `HashSet<Object> newSet` → `LinkedHashSet<Object> newSet` (`TokenField.java:432`)
- [ ] Add `serialVersionUID` to anonymous `TokenFieldServerRpc` in `TokenComboBox.java`
- [ ] Add `@Override` to `VTokenField.onKeyDown`
- [ ] Add `@Override` to `TokenField.focus()`

## Related

- [05-hardcoded-theme-and-strings.md](05-hardcoded-theme-and-strings.md) — other style/presentation issues
