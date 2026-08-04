# 05 — Hardcoded theme (Reindeer) and UI strings

| Field | Value |
|-------|-------|
| **Severity** | 🟠 Medium |
| **Complexity** | Low |
| **Status** | TODO |
| **GitHub Issues** | [#7 — IE7 display problem (obsolete)](https://github.com/vaadin-tokenfield/tokenfield/issues/7), [#9 — styles.css collision (likely dup of #6)](https://github.com/vaadin-tokenfield/tokenfield/issues/9) |

## Affected Files

- `tokenfield/src/main/java/org/vaadin/tokenfield/TokenField.java:477-480`

## Problem

Both issues are in `configureTokenButton`:

```java
protected void configureTokenButton(Object tokenId, Button button) {
    button.setCaption(getTokenCaption(tokenId) + " ×");   // ← hardcoded suffix + remove char
    button.setIcon(getTokenIcon(tokenId));
    button.setDescription("Click to remove");              // ← hardcoded English tooltip
    button.setStyleName(Reindeer.BUTTON_LINK);             // ← locks to Reindeer theme
}
```

### 1. Hardcoded `Reindeer.BUTTON_LINK` style

Ties every token button to the legacy Reindeer theme. Under Valo (Vaadin 7 default since 7.3)
or any custom theme, this injects a Reindeer-specific style name that produces no effect or
actively conflicts. Addon consumers cannot easily change this without overriding
`configureTokenButton` entirely.

### 2. Hardcoded English UI strings

- `" ×"` — non-removable suffix appended to every token caption. Internationalised apps or
  apps using icon-only tokens can't configure this without overriding the method.
- `"Click to remove"` — hardcoded English tooltip; not i18n-friendly.

Both strings should be either injectable or overridable constants.

## Fix

1. Replace `Reindeer.BUTTON_LINK` with an addon-owned CSS class constant:

```java
public static final String STYLE_TOKEN_BUTTON = "token";
// ...
button.setStyleName(STYLE_TOKEN_BUTTON);
```

Then define `.tokenfield .token` in the addon's `tokenfield.scss`/theme.

2. Expose constants for the removable strings:

```java
protected String getTokenRemoveSuffix() { return " ×"; }
protected String getTokenRemoveDescription() { return "Click to remove"; }
```

…so subclasses can override without copying the whole `configureTokenButton`.

Or accept them as configurable fields with setters.

## Checklist

- [ ] Replace `Reindeer.BUTTON_LINK` with addon-owned style constant
- [ ] Add addon CSS for the new token button style
- [ ] Extract remove-suffix and tooltip into overridable methods or constants
- [ ] Verify token buttons render correctly under Valo theme
- [ ] Verify subclass override of suffix/description works without touching `configureTokenButton`

## Related

- [07-dead-code-and-style.md](07-dead-code-and-style.md) — other style/constant issues
