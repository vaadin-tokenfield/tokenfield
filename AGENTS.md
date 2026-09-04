# TokenField Reloaded

Vaadin 7.7 add-on (fork of the abandoned TokenField): a multi-select ComboBox that renders
selections as clickable "token" buttons. Two Maven modules: `tokenfield` (the add-on, what
ships) and `tokenfield-demo` (WAR with six example panels; also hosts the browser suite).
Builds on **Java 8** (`.java-version`). README covers install/usage; `docs/code-quality.md`
covers the CI gates and Sonar; `docs/directory-listing.md` is the Directory copy.

# Commands

```shell
./mvnw -pl tokenfield test                                   # unit tests (fast inner loop)
./mvnw -pl tokenfield test -Dtest=TokenFieldAddRemoveTest    # one class; add #method for one test
./mvnw clean verify                                          # + JaCoCo 90% line floor, widgetset compile, browser BDD suite
./mvnw clean verify -DskipITs=true                           # same without the browser suite
./mvnw test-compile spotbugs:check pmd:check                 # the static-analysis CI gate
./mvnw -pl tokenfield-demo -am package -DskipTests && ./mvnw -pl tokenfield-demo jetty:run   # demo at :8080
```

`jetty:run` alone stops at `test-compile`, so the widgetset (bound to `prepare-package`) is
stale after client-side changes: run the `package` step first. Same `package` step is the
one-time setup before running `RunCucumberIT` from an IDE (it then boots its own embedded
Jetty via `DemoServer`; `-Dit.headed=true` shows the browser).

# Architecture

Server side (`tokenfield/src/main/java/org/vaadin/tokenfield`):

- `TokenField extends CustomField<Set<?>>`. Value = set of tokenIds. `setInternalValue` diffs
  old vs new set and adds/removes one `Button` per token; `buttons` (a `LinkedHashMap`) is the
  insertion-ordered source of truth for what is shown. Buttons plus the input live in a
  swappable `Layout` (`CssLayout` default); `rebuild()` re-adds everything, honouring
  `InsertPosition` and read-only (input removed entirely when read-only).
- `TokenComboBox` is the input: a `ComboBox` subclass that registers `TokenFieldServerRpc` and
  paints an `after` attribute so the client knows which key (Backspace vs Delete) removes.
- Customisation is by overriding hooks: `onTokenInput` → `addToken`, `onTokenClick` →
  `removeToken`, `onTokenDelete` → `onTokenClick`, and `configureTokenButton`. The last one is
  re-run on every reconfiguration (`refreshTokens()`), so overrides must be idempotent:
  `setStyleName` not `addStyleName`, no listener registration.
- Captions/icons are *derived* from the ComboBox's `ItemCaptionMode`/container each time, never
  cached. `getTokenCaption` deliberately deviates from `AbstractSelect` for tokens outside the
  container (see its Javadoc).

Client side (`org.vaadin.tokenfield.client.ui`, compiled by GWT 2.7 into the widgetset):

- `TokenFieldConnector` (`@Connect(TokenComboBox.class)`) wraps `VTokenField extends
  VFilterSelect`, which intercepts Backspace/Delete on an empty input and fires
  `TokenFieldServerRpc.deleteToken()` → `TokenComboBox.onDelete()` → `TokenField.onTokenDelete`.
- **Java 7 source level only** in `client/**`: GWT 2.7 rejects lambdas and method references
  there, even though javac at 1.8 accepts them. Sonar's `S1604` is disabled for this package.
- Only `client/**` sources and the `.gwt.xml` ship inside the jar (`tokenfield/pom.xml`
  resources); the GWT compiler in consuming projects recompiles them.

# Tests

- Unit tests sit in the same package as `TokenField` and go through `TestTokenField`, which
  exposes `cb`/`buttons`/`layout` and offers `simulateSelect`, `simulateNewItemInput`,
  `simulateDeleteKey`, `simulateTokenClickRpc` (a real RPC, since `Button.click()` skips
  read-only). Add new simulation helpers there rather than reaching into internals per test.
- Coverage floor is on the `tokenfield` bundle with `client/**` excluded (browser-only code).
- Browser suite: `tokenfield-demo/src/test/resources/features/token_field.feature` is the spec
  (living documentation, readable standalone). `RunCucumberIT` is the only class failsafe
  discovers; step classes are the `it` package, sharing state through picocontainer-managed
  `DemoWorld`. `DemoPage` addresses panels by index in the order `DemoRoot` adds them, and
  waits on `vaadin.clients[*].isActive()` rather than the loading-indicator CSS.
- Playwright drives Chromium; `PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1` is set so a missing browser
  fails fast instead of pulling all bundles. Traces of failed scenarios land in
  `tokenfield-demo/target/playwright`, HTML report in `tokenfield-demo/target/cucumber`.

# Bug reports and fixes

Treat an issue's diagnosis and proposed fix as a hypothesis, not a spec. Before proposing a fix:

1. **Reproduce** with a failing unit test through `TestTokenField` (or a `.feature` scenario if
   only a browser shows it). A report that cannot be reproduced is reported back as such.
2. **Classify**: genuine bug, or invalid usage of the component? Compare against how the
   standard Vaadin 7 components behave in the same situation (`ComboBox`, `AbstractSelect`,
   `AbstractField`, `CustomField`): the add-on mirrors their contracts, so their behaviour is
   the reference. Read the actual framework source/bytecode of the pinned `vaadin.version`
   (sources jar in `~/.m2`, or `javap` against `vaadin-server`), and the Vaadin 7 docs via the
   `vaadin` MCP server; the Javadoc of `TokenField` records where it deliberately deviates.
3. **Judge the proposed solution** against Vaadin practice: does the framework already offer a
   hook or pattern for this (`markAsDirty`, `setInternalValue`, RPC/state, `ItemCaptionMode`
   handling, ...)? Prefer the framework's way over an add-on-local workaround, and say so when
   the issue's suggestion differs from what you implement.

The fix is done when the reproduction test passes, the reference-behaviour comparison is
written down (test name or a 1–3 line comment), and any deviation from the reporter's
suggestion is explained in the PR/commit.

# Conventions

- User-visible changes go into `CHANGELOG.md` under the Unreleased section; mark API behaviour
  changes **Breaking**.
- Suppress a SpotBugs false positive narrowly in `config/spotbugs-excludes.xml` (class +
  method + pattern, with a reason). PMD only fails on priority 1–2.
- `addon.name` (`TokenField Reloaded`) in `tokenfield/pom.xml` is the immutable Directory
  listing name; groupId `org.vaadin.addons.tokenfield` is an intentional breaking change from
  upstream.
- Non-goal: Vaadin 8+ features beyond the planned Vaadin 8 port. No Vaadin 9+.

# Code comments

Keep comments terse — 1-3 lines max. Only explain non-obvious WHY (hidden
constraint, workaround, invariant), never WHAT the code does. Prefer a short
inline comment near the relevant line over a long JavaDoc paragraph.
