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
- **Breaking:** `configureTokenButton(Object, Button)` is now called again whenever the data a
  token button is derived from changes, not only when the button is created. Overrides must be
  idempotent — configure the button from the `tokenId` alone and don't accumulate state such as
  extra listeners. A button property set from outside the method is lost at the next refresh.
- **Breaking:** a caption set with `setTokenCaption` now also applies in the container-derived
  caption modes (`PROPERTY`, `ITEM`, `INDEX`), as the fallback for a token the container cannot
  caption. `AbstractSelect` ignores explicit captions in those modes; TokenField deviates
  deliberately, because tokens outside the container are a supported case here and a token button
  the user cannot read is not. `ICON_ONLY` still hides captions.
- New `refreshTokenButtons()` / `refreshTokenButton(Object)` (both `protected`) re-apply
  `configureTokenButton` to the existing buttons.

### Added

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

- Token captions and icons no longer depend on the order of the initialization calls. They were
  computed once, when the token button was created, so a caption set before the container data
  source arrived — or a caption property configured after the token was added — never showed up.
  They are now resolved from the data source on demand, and the buttons follow later changes:
  container item set and property set changes, and edits to the caption or icon property of a
  token that is shown. This mirrors `AbstractSelect`, which caches no caption and re-resolves on
  every paint.
- `getTokenCaption` no longer requires the token to be in the container before it will consult the
  caption mode, and no longer asks the container whether it holds the id. That probe made a typed
  container such as `JPAContainer` throw on a token id of a foreign type instead of answering
  ([#24](https://github.com/vaadin-tokenfield/tokenfield/issues/24)). A container that refuses an
  id is now treated as one that cannot caption it. The documented fallback for a token that cannot
  be captioned is `String.valueOf(tokenId)`; `INDEX` mode no longer surfaces `-1` for a token the
  container does not hold.
- `rememberToken` added the new item to the container keyed by its *caption* while writing the
  caption property under the *id* — which only worked as long as the caption degenerated to the
  id, and threw once they diverged. It now keys the item by the token id.
- Tokens set at once, as when binding a property data source, now become buttons in the value's
  own order rather than in hash order.
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
