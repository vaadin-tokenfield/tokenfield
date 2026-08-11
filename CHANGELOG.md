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
- **Breaking:** `getTokenCaption(Object)` now works as
  `AbstractSelect.getItemCaption(Object)` for every tokenId. It no longer short-circuits to
  `String.valueOf(tokenId)` for tokens the container does not contain, so the `ItemCaptionMode` is
  honoured for those too — an explicit caption on such a token is now used instead of ignored.
  As in Vaadin, the result may be empty when the mode resolves to nothing (`ICON_ONLY`, or
  `EXPLICIT` without a caption); what a token *button* shows in that case is decided by the new
  `getTokenButtonCaption(Object)`. Unchanged in the default `EXPLICIT_DEFAULTS_ID` mode.
  **Migration:** an override that builds a custom caption from `getTokenCaption` should call
  `getTokenButtonCaption` instead, or it will render an empty label where it used to get the
  tokenId — both demo panels show the pattern.
- **Documented deviation from `AbstractSelect`:** in `ItemCaptionMode.ITEM` and
  `ItemCaptionMode.PROPERTY` the caption is read off the container item, so a select answers with
  the empty string for an id it does not hold. A token outside the container is a supported case
  here rather than an anomaly, so `getTokenCaption(Object)` stands those two modes in with
  `String.valueOf(tokenId)`. Scoped to absence from the container: a token the container *does*
  hold keeps the select's answer, an empty caption from an unset property included. `INDEX` is
  deliberately not covered — it answers `-1` for an id the container does not hold, which is
  `indexOfId` speaking and not an empty caption.
- **Breaking:** `configureTokenButton(Object, Button)` is now called again on the same button
  whenever the field is reconfigured, not only once at creation. Overrides must be idempotent —
  assign state rather than accumulate it. The default implementation resets the style name first,
  so an override that follows `super` with `addStyleName` stays correct.
- `rememberToken(String)` adds the new item under its own id rather than under its caption,
  matching `AbstractSelect.DefaultNewItemHandler`. It no longer throws when a token's caption and
  id differ.

### Added

- `TokenField.refreshTokens()`, re-deriving every token button from the current data — the
  counterpart of `Table.refreshRowCache()`. Needed after changing data the field does not
  reconfigure on, such as the value of a caption property inside the container.
- `TokenField.getTokenButtonCaption(Object)`, the documented fallback for what a token button
  displays: the caption per `ItemCaptionMode`, falling back to the string representation of the
  tokenId only when that caption is empty *and* no icon was resolved, so `ICON_ONLY` keeps working
  and a token is never rendered as an anonymous chip.
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
  (using SpotBugs, PMD, and SonarQube). 
  See [docs/code-quality.md](docs/code-quality.md).

### Fixed

- Token captions and icons no longer depend on the order of the initialisation calls
  ([#8](https://github.com/vaadin-tokenfield/tokenfield/issues/8)). They were computed once, when
  the token button was created, so an explicit caption, a container, a caption mode or a
  caption/icon property id arriving afterwards was never picked up. They are now re-derived
  whenever the field is reconfigured — the same invalidate-and-re-derive contract `AbstractSelect`
  implements by re-deriving on every paint. Changes *within* the container are not observed;
  `refreshTokens()` picks those up.
- A container that answers a lookup for a token id it cannot hold by throwing rather than by
  reporting it absent — a `JPAContainer` keyed by `Long` asked about a `String`, for instance — no
  longer propagates that exception out of caption and icon resolution; the refusal is read as "no
  such item" ([#24](https://github.com/vaadin-tokenfield/tokenfield/issues/24), for the lookups
  caption and icon resolution performs). Applications over a typed container no longer need to
  resolve such captions themselves.
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
