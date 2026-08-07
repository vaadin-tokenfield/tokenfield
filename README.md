# TokenField Reloaded

TokenField is a [Vaadin](http://vaadin.com/) component for selecting multiple 'tokens'
using a ComboBox (which by default actually looks like a TextField w/ suggestions) –
essentially a multi-select ComboBox.
Example use cases include tagging and email address selection.

**TokenField Reloaded** is an updated fork of the original, now-abandoned
[TokenField](https://vaadin.com/directory/component/tokenfield) add-on by Marc Englund (@emarc),
migrated from Google Code to GitHub. It is not affiliated with the original Directory listing —
see [NOTICE](NOTICE.txt) for full attribution.

Goals of the fork:

- [x] add a proper testing harness (JUnit 5 unit tests + a Cucumber/Playwright-for-Java BDD browser suite)
- [ ] fix inherited issues
- [ ] port to Vaadin 8

Non-Goal:

- port to Vaadin 9 or later;
  for most projects it will make most sense to upgrade directly
  to the latest Vaadin version, which contains a "Multi-Select Combo Box" out of the box,
  that should be a sufficient replacement for TokenField.

Discussion thread at the [Vaadin Forum](https://vaadin.com/forum/t/tokenfield-new-component/153084)
(original add-on thread).

## Compatibility

Vaadin 7.7.x, Java 8+. (The original add-on also listed Vaadin 6.x support; this fork is not
tested against Vaadin 6 and only claims Vaadin 7.)

## Installation

```xml
<dependency>
    <groupId>org.vaadin.addons.tokenfield</groupId>
    <artifactId>tokenfield</artifactId>
    <version>7.0.2</version>
</dependency>
```

```xml
<repository>
    <id>vaadin-addons</id>
    <url>https://maven.vaadin.com/vaadin-addons</url>
</repository>
```

After adding the dependency, recompile your project's widgetset (`mvn vaadin:update-widgetset
vaadin:compile`, or let `vaadin-maven-plugin`'s `update-widgetset`/`compile` goals do it as part of
your build) so the client-side code is picked up.

> **Note:** the Maven groupId is `org.vaadin.addons.tokenfield`, not `org.vaadin.addons` as used by
> the original 7.0.1 release — this is a deliberate, breaking change since this fork is published
> under a separate Directory listing. Update your dependency coordinates when switching from the
> original add-on. Beyond that change, this fork should be a drop-in replacement.

## Usage

```java
// Most basic use case: empty to start, user-entered tokens are
// automatically added to the (in-memory) container.
TokenField f = new TokenField("Add tags");
addComponent(f);
```

See the [demo module](tokenfield-demo) for a full-featured example, including custom token
rendering, a backing `Container` ('address book'), and confirm-on-remove dialogs.

The field is very configurable, as can be seen in the [demo](http://marc.virtuallypreinstalled.com/TokenField/).

Features include:

- tokens can be inserted before/after input (over/under/etc depending on layout)
- the layout can be changed
- suggestions from container
- auto add new to container
- disallow tokens not in container
- custom action on add (+ detect if token is in container)
- custom configuring of the token button (style, caption, etc)
- custom action on remove
- built-in style for either TextField or ComboBox look
- built-in styles for buttons, default and "emphasize"

## Building from source

```shell
./mvnw clean verify                        # unit tests + coverage check + browser BDD suite
                                            # (downloads Chromium once; -DskipITs=true to skip it)
./mvnw -pl tokenfield-demo -am package -DskipTests && ./mvnw -pl tokenfield-demo jetty:run
                                            # run the demo at http://localhost:8080/
```

`jetty:run` alone only builds up to `test-compile`, so the widgetset (bound to `prepare-package`)
won't have been rebuilt yet — hence the explicit `package` first.

Browser tests are BDD scenarios: the spec lives in
`tokenfield-demo/src/test/resources/features/token_field.feature` (readable on its own — it's meant as
living documentation of how a user works with `TokenField`), and `RunCucumberIT` in
`tokenfield-demo/src/test/java/org/vaadin/tokenfield/it/` runs it under `maven-failsafe-plugin` against a
`jetty-maven-plugin`-managed instance of the demo; they run by default on `mvn verify`/`install`, locally
and in CI alike (`-DskipITs=true` for a fast inner loop without them). A scenario-by-scenario HTML report
is written to `tokenfield-demo/target/cucumber/report.html`.

### Bug reproductions

The demo doubles as the reproduction harness for open bugs: a panel per bug, plus a scenario. When a
scenario reproduces a bug it describes what *should* happen, so it fails until the bug is fixed; tag it
`@issue-<n>` to keep it out of the build gate (the `it.cucumber.tags` property in
`tokenfield-demo/pom.xml` defaults to `not @issue-15`), and run it on demand with:

```shell
./mvnw -pl tokenfield-demo verify -Dit.cucumber.tags="@issue-15"
```

#### [#15 JPAContainer crash](https://github.com/vaadin-tokenfield/tokenfield/issues/15)

The "JPAContainer" panel binds a `TokenField` to a `JPAContainer` over an H2 in-memory
database (`org.vaadin.tokenfield.jpa`, `tokenfield-demo/src/main/resources/META-INF/persistence.xml`)
and sets a token caption property id — the report's three steps exactly.

**The reported symptom does not reproduce**, on any version combination tried — including the ones
current when the report was filed (2014-05-19: Vaadin 7.2.0 was 5 days old, 7.1.15 was the previous
7.1, and JPAContainer 3.1.0 was the newest release). Typing filters and suggests normally, and no
`IllegalStateException: A connector should not be marked as dirty while a response is being written`
reaches the server log:

| Vaadin | JPAContainer | Typing (the report's steps) | Committing a typed token |
|---|---|---|---|
| 7.1.15 (2014-05-02) | 3.1.0 | works | fails, see below |
| 7.2.0 (2014-05-14) | 3.1.0 | works | fails |
| 7.7.17 (current) | 3.2.0 | works | fails |

That scenario runs in the default suite as the regression test pinning it. (Reproducing the old
combinations needs era-appropriate scaffolding the project no longer carries — `vaadin-bom` starts at
7.4.0, and `AppWidgetset` auto-generation at 7.4 — so those runs were done with local, uncommitted
POM and `web.xml` changes and JPAContainer built from its own git tags.)

**A different, real crash does — tracked separately as
[#24](https://github.com/vaadin-tokenfield/tokenfield/issues/24).** Commit a typed token — type a
value and press Enter — and the request dies server side:

```
javax.persistence.PersistenceException: ... The object [Nathan Einstein], of class
[class java.lang.String] ... could not be converted to [class java.lang.Long]
  at com.vaadin.addon.jpacontainer.JPAContainer.containsId(JPAContainer.java:700)
  at com.vaadin.ui.AbstractSelect.containsId(AbstractSelect.java:809)
  at org.vaadin.tokenfield.TokenField.getTokenCaption(TokenField.java:688)
  at org.vaadin.tokenfield.TokenField.configureTokenButton(TokenField.java:489)
```

That is a `TokenField` bug, not a JPAContainer one: `getTokenCaption` passes the raw typed `String`
to `containsId`, and `rememberToken` passes it to `addItem`, but a `JPAContainer` is keyed by entity
id. It reproduces on every row of the table above, so it is not a regression. Picking an existing
suggestion instead of typing a whole value works, because then the ComboBox supplies a real entity
id — the two scenarios beside the `@issue-15` one pin exactly that boundary.

**What does produce the reported exception.** It is reachable from `TokenField`, on the report's own
three steps, and it has nothing to do with JPAContainer — a plain `IndexedContainer` fails the same
way. `TokenFieldMarkAsDirtyWhileWritingResponseTest` in the add-on module reproduces it
deterministically, without a browser.

Vaadin throws from `ConnectorTracker.markDirty`, which refuses a newly dirtied connector once
`UidlWriter` has begun writing the response. Painting happens inside that window, and
`ComboBox.paintContent` does more than serialise:

```java
// ComboBox.getOptionsWithFilter, called from paintContent
if (filter != null) {
    filterable.addContainerFilter(filter);   // mid-paint, mid-response
}
```

It takes that branch precisely when a **token caption property id** is set (which selects
`ITEM_CAPTION_MODE_PROPERTY`) and the user has **typed something** — steps 2 and 3 of the report.
Containers announce a filter change as an item set change, so every application listener on the
container runs mid-paint. `ComboBox` guards its own handler with `isPainting`, but nothing guards
anyone else's, and if one of them touches the `TokenField` the request dies:

```
java.lang.IllegalStateException: A connector should not be marked as dirty while a response is being written.
  at com.vaadin.ui.ConnectorTracker.markDirty(ConnectorTracker.java:504)
  at com.vaadin.server.AbstractClientConnector.markAsDirty(AbstractClientConnector.java:143)
  at com.vaadin.ui.AbstractComponent.setParent(AbstractComponent.java:591)
  at com.vaadin.ui.AbstractComponentContainer.removeComponent(AbstractComponentContainer.java:229)
  at com.vaadin.ui.CssLayout.replaceComponent(CssLayout.java:286)
  at org.vaadin.tokenfield.TokenField.addTokenButton(TokenField.java:407)
  at org.vaadin.tokenfield.TokenField.setInternalValue(TokenField.java:349)
  at org.vaadin.tokenfield.TokenField.addToken(TokenField.java:447)
  ...
  at com.vaadin.ui.ComboBox.getOptionsWithFilter(ComboBox.java:441)
  at com.vaadin.ui.ComboBox.paintContent(ComboBox.java:261)
```

The sink is that every token `TokenField` adds or removes edits its own layout, and detaching a
component calls `markAsDirty()` directly instead of going through `getState()`, the accessor Vaadin
does guard with `isWritingResponse()`. So `addToken`, `removeToken`, `setReadOnly`,
`setTokenInsertPosition` and `setInputPrompt` are all fatal during a paint, while a shared-state
setter such as `setCaption` is merely dropped.

This also explains why the panel alone never reproduced it: the demo registers no container listener,
and neither did any version combination in the table. The reporter's application must have had one —
or anything else that reaches a `TokenField` mutator while the response is being written.

**A related JPAContainer bug, ruled out.** JPAContainer once had this, in `JPAContainerItem`:

```java
public void removeValueChangeListener(ValueChangeListener listener) {
    addListener(listener);   // adds instead of removes
}
```

`AbstractSelect.CaptionChangeListener.clear()` — which `ComboBox.paintContent` calls on every paint —
removes its listeners through exactly that method, and `CaptionChangeListener.valueChange()` calls
`markAsDirty()` with no guard either, so stale listeners accumulating on item properties are a second
route to the same exception. Upstream fixed it in
[`cd0d3d0`](https://github.com/vaadin/jpacontainer/commit/cd0d3d0) (2013-10-29, Vaadin ticket #12155);
3.1.0 was already out (2013-08) and 3.2.0 only landed in 2014-12, so a report filed in 2014-05 sits in
the window where every released JPAContainer carried it. This project depends on 3.2.0, which has the
fix — and the mechanism above needs neither the bug nor JPAContainer.

### Running the browser tests from an IDE

`RunCucumberIT` is a plain JUnit 5 (Platform Suite) test, so an IDE can run/debug it directly without going
through Maven at all — note that "run all tests" in a module only matches `*Test`, not `*IT`, so it won't
be picked up that way. An IDE with a Cucumber plugin can also run a single scenario straight from the
`.feature` file. On a
direct run, with no server started, `DemoServer` boots its own embedded Jetty on a free port instead of
relying on the Maven-managed instance above. One-time setup:

```shell
./mvnw -pl tokenfield-demo -am package -DskipTests   # builds the widgetset the embedded server needs
```

Re-run that whenever client-side (widgetset) code changes; server-side changes only need a recompile.
Then run/debug any `*IT` class straight from the IDE. Useful VM options:

- `-Dit.headed=true` — show the browser instead of running headless.
- Set the run configuration's working directory to `tokenfield-demo` so Playwright traces land in
  `tokenfield-demo/target/playwright` as expected.

### Code quality

SpotBugs and PMD gate every push and pull request, and run the same way locally:

```shell
./mvnw test-compile spotbugs:check pmd:check
```

See [docs/code-quality.md](docs/code-quality.md) for what they do and don't fail on, how to
suppress a false positive, and how SonarQube analysis is configured.

## Continuous integration

One workflow, [`build.yml`](.github/workflows/build.yml), on every push to any branch and on pull
requests from forks — a branch in this repository is already covered by its push, so pull requests
from it aren't built twice. Four jobs:

- **static-analysis** — SpotBugs and PMD (see above).
- **build** — `mvn verify`: unit tests, the coverage floor and the browser BDD suite. A failing run
  uploads the surefire/failsafe reports, the Cucumber report and the Playwright traces of failed
  scenarios as a `test-reports` artifact; a `v*` tag also uploads the Directory add-on ZIP.
- **sonar** — SonarQube analysis (see above), skipped without a `SONAR_TOKEN` secret.
- **publish** — needs all three of the above to succeed; deploys a snapshot to GitHub Packages on
  `main`, or publishes a release to Maven Central on `v*` tags. Any static-analysis finding, build
  failure, or failed Sonar quality gate blocks it.

The first three jobs run in parallel rather than one after another, since none of them depends on
another's output.

## Credits

Originally created by Marc Englund. See [NOTICE](NOTICE.txt) for the full attribution required by the
Apache License, and the git history for the complete list of contributors.

## License

Apache License 2.0 — see [LICENSE](LICENSE.txt).
