# GitHub Issues Evaluation

All 15 imported issues from `vaadin-tokenfield/tokenfield`. Evaluated against current implementation
and code-review findings. Outdated `https://github.com/vaadin/vaadin/` links remapped to
`https://github.com/vaadin/framework/`.

---

## Issue #15 — JPAContainer crash
**URL:** https://github.com/vaadin-tokenfield/tokenfield/issues/15
**State:** OPEN

**Reported:** `IllegalStateException: A connector should not be marked as dirty while a response is being written.`
when using JPAContainer + `TokenCaptionPropertyId` + typing to filter.

**Evaluation:** Real bug. `rememberToken` calls `cb.addItem(...)` which marks the connector dirty.
When `addNewItem` (the `NewItemHandler`) is invoked synchronously during the same request-response
cycle that is already writing, this violates Vaadin 7's connector state contract.
Compounded by finding #2 in this review: `rememberToken` adds by caption instead of id,
meaning the container mutation is doubly wrong.

**Status:** NOT FIXED — root cause still present in code.

**Matches:** [02-remember-token-caption-id.md](02-remember-token-caption-id.md),
[01-after-delete-key-sync.md](01-after-delete-key-sync.md) (connector dirty/state subsystem),
[09-new-item-input-not-cleared.md](09-new-item-input-not-cleared.md) (same `addNewItem` code path)

---

## Issue #14 — Add new token will not delete the input field's content
**URL:** https://github.com/vaadin-tokenfield/tokenfield/issues/14
**State:** OPEN

**Reported:** After adding a new (typed) token, the input field retains the typed text instead
of clearing.

**Evaluation:** Real bug. The `NewItemHandler.addNewItem` does not call `cb.setValue(null)` after
processing, whereas the `ValueChangeListener` (for selecting existing items) does:

```java
// ValueChangeListener — clears correctly:
onTokenInput(tokenId);
cb.setValue(null);   // ← present
cb.focus();

// NewItemHandler.addNewItem — does NOT clear:
onTokenInput(tokenId);
if (rememberNewTokens) { rememberToken(tokenId); }
cb.focus();          // ← cb.setValue(null) missing
```

This is a distinct bug from existing code-review findings. New review doc created.

**Status:** NOT FIXED — `cb.setValue(null)` still absent in `addNewItem` (`TokenField.java:264-267`).

**Matches:** [09-new-item-input-not-cleared.md](09-new-item-input-not-cleared.md) ← new doc

---

## Issue #13 — Readonly TokenField still can remove token causing ReadOnlyException
**URL:** https://github.com/vaadin-tokenfield/tokenfield/issues/13
**State:** OPEN

**Reported:** Stack trace:
```
com.vaadin.data.Property$ReadOnlyException
    at TokenField.removeToken
    at TokenField.onTokenClick
    at TokenField$4.buttonClick   ← anonymous ClickListener
```
when field is set read-only via `setReadOnly(true)`.

**Evaluation:** Real bug — confirmed in current code. `setReadOnly` calls `b.setReadOnly(true)` on
existing buttons, but `Button.setReadOnly` does NOT disable the button — it only marks a Vaadin
property read-only flag. The button remains visually clickable and the click listener fires, calling
`onTokenClick` → `removeToken` → `setValue` → `ReadOnlyException`. Fix is `b.setEnabled(false)`.

Directly extends code-review finding #8 (new button ignores read-only). The reported issue covers
*existing* buttons; finding #8 covers *newly added* buttons while read-only. Same root: read-only
≠ disabled for Buttons.

**Status:** NOT FIXED — `setReadOnly` in `TokenField.java:539-552` uses `setReadOnly` not `setEnabled`.

**Matches:** [03-null-and-state-handling.md](03-null-and-state-handling.md)

---

## Issue #12 — Make possible to generate filters to null values
**URL:** https://github.com/vaadin-tokenfield/tokenfield/issues/12
**State:** OPEN

**Reported:** FilterTable not passing null values to FilterGenerator.

**Evaluation:** WRONG PROJECT. This issue is about the FilterTable addon, not TokenField.
Imported in error from Google Code archive.

**Status:** IRRELEVANT — not a TokenField issue. Should be closed as `invalid`.

**Matches:** none

---

## Issue #11 — Compatibility with Vaadin 7
**URL:** https://github.com/vaadin-tokenfield/tokenfield/issues/11
**State:** OPEN

**Reported:** Needs updating against Vaadin 7 changes introduced in commit:
`https://github.com/vaadin/vaadin/commit/e18177bac62e8367829dfc5ec3083a09973e621e`
→ remapped: https://github.com/vaadin/framework/commit/e18177bac62e8367829dfc5ec3083a09973e621e

**Commit content:** Mass rename `com.vaadin.terminal` → `com.vaadin.server` (#9431), 2012-08-29.
Also touched `ComboBoxConnector.java` which `TokenFieldConnector` extends.

**Evaluation:** The package rename is already incorporated in current code:
`TokenComboBox.java` imports `com.vaadin.server.PaintException` and `com.vaadin.server.PaintTarget` ✓.
However, the `ComboBoxConnector` changes in that Vaadin commit may have introduced API changes that
the addon's `TokenFieldConnector` and `VTokenField` (extending `VFilterSelect`) had to adapt to.
Current code uses `VFilterSelect` as parent which is correct for Vaadin 7.
Issue is most likely stale — the Vaadin 7 compat work appears done. Could be closed.

**Status:** LIKELY ADDRESSED — package renames incorporated. Issue can probably be closed.

**Matches:** [01-after-delete-key-sync.md](01-after-delete-key-sync.md) (connector model for context)

---

## Issue #10 — Sortable TokenField
**URL:** https://github.com/vaadin-tokenfield/tokenfield/issues/10
**State:** OPEN

**Reported:** Request to use `LinkedHashSet` instead of `HashSet` in `setInternalValue` to preserve
insertion order for sortable token use cases.

**Evaluation:** Partially addressed. Current `addToken` creates `new LinkedHashSet<Object>(set)`
(preserves order) but declares it as `HashSet<Object>` — which is the misleading type declaration
identified in code-review finding #11. In `setInternalValue`, the diff sets (`remove`, `add`) use
`HashSet` which is correct (diff sets don't need ordering). The value itself flows through as
`LinkedHashSet` when set via `addToken`. Attachment with `SortableTokenField.java` not preserved by
Google Code archive.

**Status:** CORE REQUEST ADDRESSED (LinkedHashSet used for value), but misleading declaration
persists as code smell.

**Matches:** [07-dead-code-and-style.md](07-dead-code-and-style.md)

---

## Issue #9 — Move CSS file to subfolder — styles.css collision
**URL:** https://github.com/vaadin-tokenfield/tokenfield/issues/9
**State:** OPEN

**Reported:** `styles.css` in `public/` root collides with other addon stylesheets.

**Evaluation:** Issue #6 (same problem) was marked Fixed in r19 — resources moved to subfolder.
This issue (#9) duplicates #6. Since the Java source doesn't include CSS, cannot fully verify
current CSS layout without checking the widgetset/resources tree. Likely already fixed as part
of #6. Could be closed as duplicate.

**Status:** LIKELY DUPLICATE of closed #6. Should be closed.

**Matches:** [05-hardcoded-theme-and-strings.md](05-hardcoded-theme-and-strings.md) (CSS/theming context)

---

## Issue #8 — Button caption not properly displayed when using container data source
**URL:** https://github.com/vaadin-tokenfield/tokenfield/issues/8
**State:** OPEN

**Reported:** When `setTokenCaption(itemId, caption)` is called, then `setPropertyDataSource` and
`setContainerDataSource` with the same itemIds, the token buttons show the itemId instead of
the caption.

**Evaluation:** Real bug — still present. `getTokenCaption` (line 673):

```java
public String getTokenCaption(Object tokenId) {
    if (cb.containsId(tokenId)) {         // ← false if container not yet set
        return cb.getItemCaption(tokenId);
    } else {
        return "" + tokenId;              // ← falls through to id.toString()
    }
}
```

When `setPropertyDataSource` triggers `setValue` which calls `addTokenButton` → `configureTokenButton`
→ `getTokenCaption`, the container may not yet contain the token items (e.g. if `setContainerDataSource`
is called after `setPropertyDataSource`). The `containsId` check returns false, so the caption
falls back to `"" + tokenId`.

Workaround in issue: call `cb.getItemCaption(tokenId)` directly (which returns null → fallback
instead of throwing). Root fix: check caption even when id not in container, or document ordering
requirement. New review doc created.

**Status:** NOT FIXED — ordering dependency still present.

**Matches:** [10-token-caption-datasource-ordering.md](10-token-caption-datasource-ordering.md) ← new doc

---

## Issue #7 — IE7 display problem
**URL:** https://github.com/vaadin-tokenfield/tokenfield/issues/7
**State:** OPEN

**Reported:** Token button height larger than text field in IE7/IE8 compatibility mode.
Suggested fix: adjust `padding-bottom` CSS, use `em` instead of `px` for button height.

**Evaluation:** IE7 is effectively obsolete (EOL 2016). This is a CSS issue in the addon's
stylesheet, not in Java code. Related to the broader theme/CSS hardcoding problem (finding #6
in review). No current code change would address this without CSS fixes + potentially dropping
IE7 support as out-of-scope.

**Status:** OBSOLETE — IE7 EOL. Low value to fix. CSS improvements from
[05-hardcoded-theme-and-strings.md](05-hardcoded-theme-and-strings.md) may incidentally help.

**Matches:** [05-hardcoded-theme-and-strings.md](05-hardcoded-theme-and-strings.md)

---

## Issue #6 — CSS resources should be in its own subfolder
**URL:** https://github.com/vaadin-tokenfield/tokenfield/issues/6
**State:** CLOSED

**Reported:** Resources in `public/` clash with other addons.

**Evaluation:** Fixed in r19 (comment confirms). CLOSED correctly.

**Status:** VERIFIED FIXED.

**Matches:** none (resolved)

---

## Issue #5 — How to enter number through click event of NSButton in NSTokenField
**URL:** https://github.com/vaadin-tokenfield/tokenfield/issues/5
**State:** OPEN

**Reported:** macOS Cocoa `NSTokenField` question about `NSButton` click events. Mac OS X 10.6.3.

**Evaluation:** WRONG PROJECT. This is a macOS Cocoa question about Apple's NSTokenField, not the
Vaadin addon. Imported in error from Google Code (name collision).

**Status:** IRRELEVANT. Should be closed as `invalid`.

**Matches:** none

---

## Issue #4 — Attempt to prevent suggestion menu when backspacing/deleting
**URL:** https://github.com/vaadin-tokenfield/tokenfield/issues/4
**State:** OPEN

**Reported:** Must use both `onKeyDown` and `onKeyUp` to properly prevent the ComboBox suggestion
popup from opening when backspace/delete triggers token deletion.

**Evaluation:** Relevant open issue. Current `VTokenField.onKeyDown` calls `return` early after
`fireDeleteListeners()` which prevents `super.onKeyDown` from running — this should stop the
popup. But `onKeyUp` is not overridden. If the ComboBox popup logic triggers on `keyUp` events,
the popup may still appear after a delete action.
Partially related to `01-after-delete-key-sync.md` (same keyboard subsystem). New review doc created.

**Status:** POTENTIALLY UNFIXED — `onKeyUp` not overridden in `VTokenField`. Needs browser testing.

**Matches:** [11-suggestion-popup-on-delete.md](11-suggestion-popup-on-delete.md) ← new doc,
[01-after-delete-key-sync.md](01-after-delete-key-sync.md)

---

## Issue #3 — TextField lookalike-mode should support Reindeer 'black'
**URL:** https://github.com/vaadin-tokenfield/tokenfield/issues/3
**State:** CLOSED

**Reported:** Reindeer 'black' style shows white background on right side of input.

**Evaluation:** Fixed (comment: "Fixed"). CLOSED correctly. The broader issue of hardcoded
Reindeer styles is tracked in [05-hardcoded-theme-and-strings.md](05-hardcoded-theme-and-strings.md).

**Status:** VERIFIED FIXED (Reindeer black). Broader Reindeer coupling tracked separately.

**Matches:** [05-hardcoded-theme-and-strings.md](05-hardcoded-theme-and-strings.md)

---

## Issue #2 — Backspace in empty input could delete last token
**URL:** https://github.com/vaadin-tokenfield/tokenfield/issues/2
**State:** CLOSED

**Reported:** Feature request: pressing backspace when ComboBox is empty should delete last token.
Should also work with DELETE key when insert position is AFTER.

**Evaluation:** Marked fixed ("Implemented, should be tested w/ more browsers"). BACKSPACE (BEFORE
mode) is implemented and works. However — **the DELETE key in AFTER mode is broken** due to
`VTokenField.after` never being populated (code-review finding #1). The second half of the
original feature request remains broken. The issue was prematurely closed.

**Status:** PARTIALLY FIXED — BACKSPACE/BEFORE works, DELETE/AFTER broken. Regression tracked in
[01-after-delete-key-sync.md](01-after-delete-key-sync.md).

**Matches:** [01-after-delete-key-sync.md](01-after-delete-key-sync.md)

---

## Issue #1 — Try floats for CSS layout
**URL:** https://github.com/vaadin-tokenfield/tokenfield/issues/1
**State:** CLOSED

**Reported:** Feature request: use `float:left` in CSS layout for better default.

**Evaluation:** Fixed. `CssLayout` is now the default layout (`TokenField.java:177`).
Comment confirms: "float:left works nicely, CssLayout is now the default." CLOSED correctly.

**Status:** VERIFIED FIXED.

**Matches:** none (resolved)

---

## Summary Table

| # | Title | State | Eval Result | Review Doc |
|---|-------|-------|-------------|------------|
| 1 | Try floats for CSS layout | CLOSED | ✅ Verified fixed | — |
| 2 | Backspace deletes last token | CLOSED | ⚠️ Partial — AFTER/DELETE broken | [01](01-after-delete-key-sync.md) |
| 3 | Reindeer 'black' support | CLOSED | ✅ Verified fixed | [05](05-hardcoded-theme-and-strings.md) |
| 4 | Prevent popup on backspace/delete | OPEN | ❓ Likely unfixed (onKeyUp) | [11](11-suggestion-popup-on-delete.md) |
| 5 | NSTokenField macOS question | OPEN | 🚫 Wrong project | — |
| 6 | CSS in subfolder | CLOSED | ✅ Verified fixed | — |
| 7 | IE7 display problem | OPEN | 💤 Obsolete (IE7 EOL) | [05](05-hardcoded-theme-and-strings.md) |
| 8 | Button caption with datasource | OPEN | ❌ Not fixed | [10](10-token-caption-datasource-ordering.md) |
| 9 | styles.css collision | OPEN | ✅ Likely dup of #6, fixed | [05](05-hardcoded-theme-and-strings.md) |
| 10 | Sortable TokenField | OPEN | ⚠️ Partially addressed | [07](07-dead-code-and-style.md) |
| 11 | Compatibility with Vaadin 7 | OPEN | ✅ Likely stale, can close | [01](01-after-delete-key-sync.md) |
| 12 | FilterTable null values | OPEN | 🚫 Wrong project | — |
| 13 | Readonly allows token removal | OPEN | ❌ Not fixed | [03](03-null-and-state-handling.md) |
| 14 | New token input not cleared | OPEN | ❌ Not fixed | [09](09-new-item-input-not-cleared.md) |
| 15 | JPAContainer crash | OPEN | ❌ Not fixed | [02](02-remember-token-caption-id.md), [01](01-after-delete-key-sync.md) |

### Legend
- ✅ Fixed / resolved
- ❌ Not fixed — active bug
- ⚠️ Partial fix or workaround only
- ❓ Uncertain — needs browser/runtime testing
- 💤 Obsolete / low value
- 🚫 Wrong project / invalid
