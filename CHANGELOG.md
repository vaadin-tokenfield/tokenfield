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

### Added

- Add Maven-based Project packaging
- Maven Central release path (GPG signing and Central Portal publishing), in addition to the
  Directory ZIP bundle.
- JUnit 5 unit test suite (124 tests) covering container handling, buffering, read-only state,
  insert position, button configuration, delegation, sizing, captions/icons, layout swapping,
  the delete-key path, and UI input behavior.
- JaCoCo coverage reporting and a build-failing line-coverage threshold (90%) on the add-on module.
- A Cucumber-JVM BDD browser suite (27 scenarios across all five demo panels), driven by
  Playwright for Java and run under `maven-failsafe-plugin` against a `jetty-maven-plugin`-managed
  instance of the demo as part of `mvn verify`/`install` by default, locally and in CI alike
  (`-DskipITs=true` to skip). The spec
  itself — `tokenfield-demo/src/test/resources/features/token_field.feature` — reads as living
  documentation of how a user works with `TokenField`; `RunCucumberIT` (JUnit Platform Suite +
  `cucumber-junit-platform-engine`) is what failsafe actually discovers, `PlaywrightHooks` owns the
  browser/page lifecycle per scenario (Jupiter extensions like `@UsePlaywright` don't reach the
  Cucumber engine), and `cucumber-picocontainer` shares state into the step classes through a
  `DemoWorld`. Replaces the five `*IT.java` classes (26 tests) from the initial Playwright-for-Java
  port, and before that an earlier, disconnected TypeScript/Node Playwright suite that never ran in
  CI and required a manually pre-started server.
- `RunCucumberIT` also runs directly from an IDE, without the Maven-managed server: `DemoServer` boots
  an embedded Jetty on a free port when `it.baseUrl` isn't set by failsafe. Requires one `mvn package`
  to build the widgetset first; see the README's "Running the browser tests from an IDE" section.
- CI now builds every branch, not just `main` and pull requests, so work in progress is verified
  before it reaches a pull request. Publishing stays limited to `main` (snapshots) and `v*` tags
  (releases), and the build job is capped at 30 minutes so a hung browser or server fails the run
  instead of occupying a runner. Test reports, the Cucumber HTML report and the Playwright traces of
  failed scenarios are uploaded as an artifact when a build fails.
- A sixth demo panel, "JPAContainer": a `TokenField` bound to a `JPAContainer` over an
  H2 in-memory database (EclipseLink; see `org.vaadin.tokenfield.jpa` and
  `tokenfield-demo/src/main/resources/META-INF/persistence.xml`) with a token caption property id
  set, showing the component against a lazy, database-backed container. Committing a *typed* token
  against it fails — `TokenField.getTokenCaption` hands the raw `String` to `containsId` and
  `rememberToken` hands it to `addItem`, but a `JPAContainer` is keyed by entity id — so that case is
  a browser scenario tagged `@issue-15`, excluded from the build gate through the new
  `it.cucumber.tags` property and tracked as its own issue (#24). Picking an existing suggestion
  works and is pinned too.
- A reproduction for [#15](https://github.com/vaadin-tokenfield/tokenfield/issues/15),
  `IllegalStateException: A connector should not be marked as dirty while a response is being
  written`: `TokenFieldMarkAsDirtyWhileWritingResponseTest` (11 tests) in the add-on module, plus a
  seventh demo panel, "Container listener", and a browser scenario that drives it through a real
  request. The exception was reachable from `TokenField` on the report's own three steps, and was not
  specific to `JPAContainer` — a plain `IndexedContainer` failed identically. With a token caption
  property id set and a filter typed, `ComboBox.paintContent` applies that filter to the container
  *while Vaadin is writing the response*, so every application listener on the container runs
  mid-paint; `ComboBox` guards only its own handler, and a listener that edited the field killed the
  request. Typing on its own does not crash — on JPAContainer 3.2.0/Vaadin 7.7.17 or on the versions
  current when the report was filed (JPAContainer 3.1.0 on Vaadin 7.1.15 and 7.2.0) — and stays pinned
  by its own scenario.

### Fixed

- Add missing Apache license file headers
- Add-on JAR manifest `Implementation-Title` now matches the new Directory listing name.
- Add-on JAR no longer carries an unresolvable `Class-Path` manifest entry.
- Directory ZIP manifest now includes `Vaadin-Widgetsets` and `Vaadin-License-File`.
- Assembly ZIP build no longer overwrites the JAR as the project's main artifact.
- Fixed "Layout and InsertPosition" Demo
- **[#15](https://github.com/vaadin-tokenfield/tokenfield/issues/15)** — editing a `TokenField` while
  Vaadin is writing the response no longer throws `IllegalStateException: A connector should not be
  marked as dirty while a response is being written`. That is easy to hit without meaning to: a
  `ComboBox` with a token caption property id filters through its container from inside its own paint
  (`ComboBox.getOptionsWithFilter` → `addContainerFilter`), a container reports a filter change as an
  item set change, and so any application listener on that container runs mid-paint. Neither half of a
  token change could be made safe in place — `AbstractField` ends every value change with an
  unconditional `markAsDirty()`, and every layout edit marks the layout dirty — so `TokenField` now
  holds the change back and applies it from `beforeClientResponse`, before the next response is
  written. `addToken`, `removeToken`, `setReadOnly` and `setTokenInsertPosition` are safe to call at
  any time; successive changes coalesce, and the change lands one response later rather than
  immediately. `setInputPrompt` is the one remaining gap and is pinned by a test: it is `ComboBox`'s
  own setter and marks dirty by hand, leaving no seam to defer it.
- Adding a token no longer detaches and re-attaches the input. With the default
  `InsertPosition.BEFORE`, `addTokenButton` swapped the input out and re-appended it on every token,
  costing a full repaint of the input each time; the button is now inserted at the input's index.

## [7.0.1]

- Updated to work with 7.0 final. 
- Note: CSS selectors are made stronger, you might have to do the same if you have styled TokenField.

## [7.0.0]

- Converted to work with Vaadin 7. 
- Added a possibility to customize the adding of new items to the dropdown.
