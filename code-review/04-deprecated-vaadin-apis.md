# 04 — Deprecated Vaadin 7 API calls

| Field | Value |
|-------|-------|
| **Severity** | 🟠 Medium |
| **Complexity** | Low |
| **Status** | TODO |

## Affected Files

- `tokenfield/src/main/java/org/vaadin/tokenfield/TokenComboBox.java:37`
- `tokenfield/src/main/java/org/vaadin/tokenfield/TokenField.java:236,384`

## Problem

Three uses of deprecated Vaadin 7 API:

### 1. `requestRepaint()` → `markAsDirty()` (`TokenComboBox.java:37`)

```java
public void setTokenInsertPosition(TokenField.InsertPosition insertPosition) {
    this.insertPosition = insertPosition;
    requestRepaint();   // ← deprecated since Vaadin 7.0
}
```

`requestRepaint()` was deprecated in favour of `markAsDirty()` in Vaadin 7.0 final.

### 2. `addListener(ValueChangeListener)` (`TokenField.java:236`)

```java
cb.addListener(new ComboBox.ValueChangeListener() { ... });
```

Deprecated generic `addListener`. Replacement: `cb.addValueChangeListener(...)`.

### 3. `addListener(ClickListener)` (`TokenField.java:384`)

```java
b.addListener(new Button.ClickListener() { ... });
```

Deprecated generic `addListener`. Replacement: `b.addClickListener(...)`.

## Fix

Mechanical renames:

```java
// TokenComboBox.java:37
markAsDirty();

// TokenField.java:236
cb.addValueChangeListener(new Property.ValueChangeListener() { ... });

// TokenField.java:384
b.addClickListener(new Button.ClickListener() { ... });
```

## Checklist

- [ ] `requestRepaint()` → `markAsDirty()` in `TokenComboBox`
- [ ] `addListener(ValueChangeListener)` → `addValueChangeListener` in `TokenField`
- [ ] `addListener(ClickListener)` → `addClickListener` in `TokenField`
- [ ] Confirm no compiler deprecation warnings remain for these call sites

## Related

- [01-after-delete-key-sync.md](01-after-delete-key-sync.md) — `TokenComboBox.paintContent` in same file (suggests broader refactor of `TokenComboBox`)
