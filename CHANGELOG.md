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
- JUnit 5 unit test suite (114 tests) covering container handling, buffering, read-only state,
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
- Automated code-quality review on every push and pull request (`Code Quality` workflow): SpotBugs
  (Max effort, default Medium threshold) and PMD (fails on priority 1–2 findings), both runnable
  locally as `./mvnw test-compile spotbugs:check pmd:check`, plus a SonarQube job that stays inert
  until a `SONAR_TOKEN` secret is configured. The Sonar analysis imports unit-test line coverage from
  JaCoCo's XML report. See [docs/code-quality.md](docs/code-quality.md).

### Fixed

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
