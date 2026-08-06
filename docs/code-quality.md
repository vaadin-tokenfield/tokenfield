# Code quality

Automated review of every push and every pull request, as three parallel jobs in
[`.github/workflows/build.yml`](../.github/workflows/build.yml) — `static-analysis`, `build` and
`sonar` — with a fourth job, `publish`, that only runs once all three succeed:

| Job | Runs | Gate |
| --- | --- | --- |
| **static-analysis** | always, on Java 8 | fails on SpotBugs findings at the default *Medium* threshold, and on PMD priority 1–2 findings |
| **build** | always | unit tests, the JaCoCo coverage floor and the browser BDD suite (`mvn verify`) |
| **sonar** | only once a `SONAR_TOKEN` secret exists | Sonar's own quality gate, via `-Dsonar.qualitygate.wait=true` |

`publish` declares `needs: [static-analysis, build, sonar]`. GitHub Actions skips a job whose
`needs` didn't all succeed, so a SpotBugs/PMD finding, a build failure or a failed Sonar quality
gate blocks the snapshot deploy and the Maven Central release without any extra plumbing in the
publish job itself.

The three QA jobs run in parallel rather than staged one after another, and none of them builds
`tokenfield-demo` (the module with the slow parts — the GWT widgetset compile and the browser
suite) more than once: `static-analysis` stops at `test-compile`, `sonar` builds only the
`tokenfield` module, and `build` is the one job that runs the full `verify`. Only `build` and
`sonar` overlap — both compile and unit-test `tokenfield`, about 20 seconds of duplicated work —
because serializing `sonar` behind `build`'s ~2 minute `verify` step would cost more wall-clock
than parallel-plus-duplicate saves. That trade only makes sense at this project's current size —
a compile that took several minutes, or a browser suite an order of magnitude longer, would flip
the answer toward a shared build stage. Measured current step times (2026-08-07, run
`31166720813`): `mvn verify` end to end 105s (GWT widgetset compile 30s of that, the Cucumber
suite 20s), `static-analysis` ~90s of mostly cold-cache plugin download, `sonar`'s `test` + analyse
~50s.

## SpotBugs and PMD

Both plugins are configured in the parent POM's `pluginManagement`, so CI and a local run execute
exactly the same checks:

```shell
./mvnw test-compile spotbugs:check pmd:check
```

One invocation, on purpose: `test-compile` gives the analysers freshly compiled classes and lets
`tokenfield-demo` resolve `tokenfield` from the reactor instead of from a repository, and it stops
short of `prepare-package`, so the ~30-second GWT widgetset compile stays out of this job.

Both plugins are pinned to their last releases that still run on Java 8 (`spotbugs-maven-plugin`
4.7.3.6, `maven-pmd-plugin` 3.21.2), which is the JDK this project builds with.

Only findings serious enough to be actionable fail the build:

- **SpotBugs** runs at `Max` effort and its default `Medium` threshold. The add-on is clean at that
  threshold today. Lower-priority findings inherited from the original add-on (missing
  `serialVersionUID` on inner listener classes, an unconfirmed cast in the connector) are visible
  with `-Dspotbugs.threshold=Low` but not enforced.
- **PMD** uses the plugin's default ruleset but only fails on priority 1–2 (correctness-level)
  findings. Its style-level advice — a handful of collapsible `if`s and redundant parentheses in
  inherited code — is reported for review rather than enforced, so turning the gate on didn't
  require reformatting the fork's inherited code.

Each run uploads the full XML reports as a `static-analysis-reports` artifact, including everything
below those thresholds.

To suppress a false positive, add a narrowly scoped entry to
[`config/spotbugs-excludes.xml`](../config/spotbugs-excludes.xml) — match the class, the method and
the bug pattern, so a genuine occurrence elsewhere still fails the check — and say why in a comment.

## Enabling SonarQube

The `sonar` job degrades gracefully without credentials: its `Check for Sonar credentials` step
prints a notice and every later step is skipped until the repository has a `SONAR_TOKEN` secret,
because no workflow can create the Sonar-side project and token for us. **This repository already
has one configured**, so the job runs for real against sonarcloud.io on every push. To set it up
on a fork or a new project:

1. Create the project in [SonarQube Cloud](https://sonarcloud.io) (or on your own SonarQube server)
   and note its project key and organization.
2. Make them match the `sonar.projectKey` / `sonar.organization` properties in the parent
   [`pom.xml`](../pom.xml) — they are currently `vaadin-tokenfield_tokenfield` and
   `vaadin-tokenfield`.
3. Add the analysis token as a repository secret named `SONAR_TOKEN`
   (*Settings → Secrets and variables → Actions*).
4. For a self-hosted SonarQube, also add a repository **variable** `SONAR_HOST_URL` pointing at it;
   without one the job analyses against SonarQube Cloud.

SonarQube Cloud is free for **public** projects, which this repository is — the job is opt-in only
because the Sonar-side project and token have to be created by hand first; there's no cost reason
to keep it off.

`-Dsonar.qualitygate.wait=true` fails the `Analyse` step, and so the job, if Sonar's quality gate
doesn't pass. `publish` needs this job to succeed, so a failed gate blocks the snapshot deploy and
the Maven Central release the same way a SpotBugs or PMD finding does.

Only the `tokenfield` module is analysed: it is the code that ships, and unlike `tokenfield-demo`
its build has no GWT step that would need Java 8 (Sonar's scanner requires Java 17+, so this job
runs on Java 21 while `static-analysis` and `build` use Java 8). The job compiles and unit-tests
`tokenfield` itself — `mvn -pl tokenfield -am test` — rather than depend on the `build` job's
output, so its ~20 seconds of test time is spent twice across the two jobs. See the trade-off
noted at the top of this document.

### Inheritance depth (java:S110)

Every non-trivial class in this add-on — `TokenField`, `TokenComboBox`, `VTokenField`,
`TokenFieldConnector` — extends a Vaadin 7 or GWT base class, and that framework hierarchy alone
(`AbstractClientConnector` → `AbstractComponent` → `AbstractField` → `CustomField`, and the
matching GWT widget chain) already exceeds Sonar's default inheritance-depth limit of 5 before any
add-on code exists. The normal fix — raising the limit or excluding framework packages via the
rule's `max`/`filteredClasses` parameters — requires a custom quality profile, which needs Sonar
admin rights the free SonarQube Cloud plan doesn't grant on this project. Rule `java:S110` is
disabled instead via `sonar.issue.ignore.multicriteria` in the parent [`pom.xml`](../pom.xml).

### Lambdas in client code (java:S1604)

`org.vaadin.tokenfield.client` is not compiled by javac alone: `vaadin-maven-plugin` also hands it
to the GWT compiler bundled with Vaadin 7.7.x (GWT 2.7), which recompiles the widgetset's `.java`
sources into JavaScript using its own JDT. That compiler caps source level at Java 7 — GWT only
gained Java 8 support in 2.8 — so lambdas, method references and the like fail the widgetset build
even though `javac` (running at 1.8 for the rest of the module) accepts them without complaint.
Sonar's `java:S1604` ("replace anonymous class with lambda") doesn't know about this second compile
step and will keep suggesting the change, so the rule is disabled for `**/client/**` the same way
`java:S110` is, via `sonar.issue.ignore.multicriteria` in the parent [`pom.xml`](../pom.xml).

### Test coverage

Sonar does not measure coverage itself; it imports
[JaCoCo's XML report](https://docs.sonarsource.com/sonarqube-cloud/analyzing-source-code/test-coverage/java-test-coverage/),
and only the XML one — the binary `jacoco.exec` is not read. Three things make that work here, and
all three are load-bearing:

1. `jacoco:report` writes `tokenfield/target/site/jacoco/jacoco.xml` (XML is one of the goal's
   default formats), bound to the `test` phase — so the `sonar` job's own `mvn ... test` step
   produces it.
2. `sonar.coverage.jacoco.xmlReportPaths` in [`tokenfield/pom.xml`](../tokenfield/pom.xml) points at
   that file, via the same `${project.reporting.outputDirectory}/jacoco` expression the report goal
   defaults to, so the two can't drift apart.
3. The `sonar` job checks `test -s .../jacoco.xml` as its own step, right before the analysis — a
   report that stopped being generated fails the job there, instead of quietly publishing an
   analysis that shows no coverage.

`sonar.coverage.exclusions` in the parent POM keeps `**/client/**` out of the coverage figure, for
the same reason the JaCoCo check excludes it: GWT client-side code only runs compiled to JavaScript
in a browser, so no JVM test can cover it.

Note that this is unit-test coverage only. The Cucumber/Playwright suite exercises the add-on
server-side too, but its coverage isn't measured: the demo runs inside `jetty-maven-plugin`, so
attributing that execution to the add-on's classes would mean attaching the JaCoCo agent to the
Jetty JVM and merging a second `.exec` file into the report.

### GitHub code scanning (CodeQL)

Worth knowing as an alternative: CodeQL is free for public repositories, which this one is, so it's
available without an Advanced Security license. It isn't enabled yet — it would run as its own
`.github/workflows/codeql.yml`, in parallel with the jobs described above, and wouldn't need any
changes to `build.yml`.
