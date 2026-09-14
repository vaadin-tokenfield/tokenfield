# Changelog

All notable changes to TokenField Reloaded are documented here.

## [7.0.2] - Unreleased

First release of the fork. Forked from the original TokenField add-on's last upstream release,
7.0.1 (2013-02-14, by Marc Englund), and migrated from Google Code to GitHub.

### Changed

- **Breaking:** Maven groupId changed from `org.vaadin.addons` to `org.vaadin.addons.tokenfield`.
  Published as a new Vaadin Directory listing, "TokenField Reloaded" — this fork does not own and
  cannot update the original "TokenField" listing.
- Rebuilt on Vaadin 7.7.17 and Java 8.
- Optimize `InsertPosition.BEFORE` token inserts on known layouts to avoid full repaints of the input.
- **Breaking:** `getTokenCaption(Object)` now follows the caption mode for every token, instead of
  forcing the raw tokenId for tokens the container does not hold.
- **Breaking:** `configureTokenButton(Object, Button)` is called again whenever the field is
  reconfigured, so overrides must be idempotent.
- In `ItemCaptionMode.ITEM` and `PROPERTY`, a token the container does not hold is now named after
  itself instead of being left blank — a documented deviation from `AbstractSelect`.
- **Breaking:** `rememberToken` now adds a new token to the container under the typed text itself,
  not a caption derived from it. Previously, a `tokenCaptionPropertyId` write after adding the item
  looked the item up by the typed text while it had been added under its derived caption instead —
  throwing whenever the two diverged (any caption mode other than the default). `null` is now also
  fully supported as a token id across every method that takes one; see `AGENTS.md`.

### Added

- `setTokenCaption(null, ...)`/`setTokenIcon(null, ...)` now set an explicit caption/icon override
  for the `null` token id, previously a silent no-op (`AbstractSelect#setItemCaption`/`setItemIcon`
  ignore a null itemId, which `TokenField` can no longer delegate to for this case). The caption
  override applies under the same modes an explicit caption applies under for any other token id
  (`EXPLICIT`/`EXPLICIT_DEFAULTS_ID`); the icon override is mode-independent, matching
  `AbstractSelect#getItemIcon`.
- `TokenField.refreshTokens()`, to re-derive the token buttons after a data change the field
  cannot see by itself.
- `TokenField.initTokenCaption(String)` and `initTokenIcon(String)`, overridable hooks for what
  `rememberToken(String)` writes into the caption/icon property of a newly entered item. Default
  behavior is unchanged from before this release: the typed text as caption, no icon.
- `TokenField.NewTokenHandler` and `setNewTokenHandler`, the counterpart of
  `ComboBox.setNewItemHandler`: how text the user typed becomes a token. Needed for a container
  that names or validates its items itself (`JPAContainer`, `BeanItemContainer`); the demo's
  address-book panels show it.
- Documented and demoed the workaround for a container keyed by something other than the token id
  (e.g. `JPAContainer` by `Long`): a `NewTokenHandler` plus a `getTokenCaption`/`getTokenIcon`
  override, since the component itself cannot resolve such a token without asking the container
  about an id it cannot hold. Out-of-the-box support for such containers is not a goal
  ([#24](https://github.com/vaadin-tokenfield/tokenfield/issues/24), closed as won't-fix) — see the
  JPA address-book demo panel.
- Maven-based Project packaging
- Maven Central release path (GPG signing and Central Portal publishing), in addition to the
  Directory ZIP bundle.
- JUnit 5 unit test suite covering container handling, buffering, read-only state,
  insert position, button configuration, delegation, sizing, captions/icons, layout swapping,
  the delete-key path, and UI input behavior.
- JaCoCo coverage reporting and a build-failing line-coverage threshold (90%) on the add-on module.
- A Cucumber-JVM BDD browser suite, driven by Playwright for Java and 
  run under `maven-failsafe-plugin` against the Demo application.
- A demo panel showing the component usage with a JPAContainer (using an in-memory H2 database)
- Automated CI build and code-quality review on every push and pull request
- JSpecify `@Nullable` annotations on `TokenField`'s public getters/setters that can accept or
  return null, documenting existing behavior — not a behavior change.
  (using SpotBugs, PMD, and SonarQube). 
  See [docs/code-quality.md](docs/code-quality.md).

### Fixed

- Token captions and icons no longer depend on the order of the initialisation calls
  ([#8](https://github.com/vaadin-tokenfield/tokenfield/issues/8)).
- Clicking a token of a read-only field no longer throws `Property.ReadOnlyException`.
- `removeToken` no longer throws on a field that has no value
  ([#13](https://github.com/vaadin-tokenfield/tokenfield/issues/13)).
- Add missing Apache license file headers
- Add-on JAR manifest `Implementation-Title` now matches the new Directory listing name.
- Add-on JAR no longer carries an unresolvable `Class-Path` manifest entry.
- Directory ZIP manifest now includes `Vaadin-Widgetsets` and `Vaadin-License-File`.
- Assembly ZIP build no longer overwrites the JAR as the project's main artifact.
- Fixed "Layout and InsertPosition" Demo

## [7.0.1]

- Updated to work with 7.0 final. 
- Note: CSS selectors are made stronger, you might have to do the same if you have styled TokenField.

## [7.0.0]

- Converted to work with Vaadin 7. 
- Added a possibility to customize the adding of new items to the dropdown.
