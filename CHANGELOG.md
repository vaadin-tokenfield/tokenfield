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
- **Breaking:** `getTokenCaption(Object)` now works exactly as
  `AbstractSelect.getItemCaption(Object)` for every tokenId. It no longer short-circuits to
  `String.valueOf(tokenId)` for tokens the container does not contain, so the `ItemCaptionMode` is
  honoured for those too — an explicit caption on such a token is now used instead of ignored.
  As in Vaadin, the result may be empty when the mode resolves to nothing (`ICON_ONLY`, `EXPLICIT`
  without a caption, or `PROPERTY` for a token the container does not hold); what a token *button*
  shows in that case is decided by the new `getTokenButtonCaption(Object)`. Unchanged in the default
  `EXPLICIT_DEFAULTS_ID` mode. **Migration:** an override that builds a custom caption from
  `getTokenCaption` should call `getTokenButtonCaption` instead, or it will render an empty label
  where it used to get the tokenId — both demo panels show the pattern.
- **Breaking:** `configureTokenButton(Object, Button)` is now called again on the same button
  whenever the data it derives from changes, not only once at creation. Overrides must be
  idempotent — assign state rather than accumulate it. The default implementation resets the style
  name first, so an override that follows `super` with `addStyleName` stays correct.
- `rememberToken(String)` adds the new item under its own id rather than under its caption,
  matching `AbstractSelect.DefaultNewItemHandler`. It no longer throws when a token's caption and
  id differ.

### Added

- `TokenField.refreshTokens()`, re-deriving every token button from the current data — the
  counterpart of `Table.refreshRowCache()`, for data the component cannot observe.
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

- Token captions and icons no longer depend on the order of the initialisation calls. They were
  computed once, when the token button was created, so an explicit caption, a container, a caption
  mode or a caption/icon property arriving afterwards was never picked up. They are now re-derived
  whenever any of those change, and the tracked container properties are followed while the token
  is on screen — the same invalidate-and-re-derive contract `AbstractSelect` implements by
  re-deriving on every paint.
- A container that answers a lookup for a token id it cannot hold by throwing rather than by
  reporting it absent — a `JPAContainer` keyed by `Long` asked about a `String`, for instance — no
  longer propagates that exception out of caption and icon resolution; the refusal is read as "no
  such item" ([#24](https://github.com/vaadin-tokenfield/tokenfield/issues/24), caption and icon
  paths; `containsId` is unchanged).
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
