# Code quality

Automated review of every push and every pull request, in
[`.github/workflows/code-quality.yml`](../.github/workflows/code-quality.yml). Two jobs:

| Job | Runs | Gate |
| --- | --- | --- |
| **SpotBugs & PMD** | always, on Java 8 | fails on SpotBugs findings at the default *Medium* threshold, and on PMD priority 1–2 findings |
| **SonarQube analysis** | only once a `SONAR_TOKEN` secret exists | Sonar's own quality gate |

The build itself (unit tests, coverage floor, browser suite) lives in
[`build.yml`](../.github/workflows/build.yml) and is deliberately kept separate, so a
static-analysis finding is distinguishable at a glance from a broken build or a failing test.

## SpotBugs and PMD

Both plugins are configured in the parent POM's `pluginManagement`, so CI and a local run execute
exactly the same checks:

```shell
./mvnw test-compile spotbugs:check pmd:check
```

One invocation, on purpose: `test-compile` gives the analysers freshly compiled classes and lets
`tokenfield-demo` resolve `tokenfield` from the reactor instead of from a repository, and it stops
short of `prepare-package`, so the minutes-long GWT widgetset compile stays out of the quality job.

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

The Sonar job is wired up but inert: it prints a notice and passes until the repository has a
`SONAR_TOKEN` secret, because no workflow can create the Sonar-side project and token for us. To
turn it on:

1. Create the project in [SonarQube Cloud](https://sonarcloud.io) (or on your own SonarQube server)
   and note its project key and organization.
2. Make them match the `sonar.projectKey` / `sonar.organization` properties in the parent
   [`pom.xml`](../pom.xml) — they are currently `vaadin-tokenfield_tokenfield` and
   `vaadin-tokenfield`.
3. Add the analysis token as a repository secret named `SONAR_TOKEN`
   (*Settings → Secrets and variables → Actions*).
4. For a self-hosted SonarQube, also add a repository **variable** `SONAR_HOST_URL` pointing at it;
   without one the job analyses against SonarQube Cloud.

Note that SonarQube Cloud is only free for **public** projects. This repository is private, so
analysis needs either a paid SonarQube Cloud plan or a self-hosted SonarQube — which is the other
reason the job is opt-in rather than on by default.

The job analyses the `tokenfield` module only: it is the code that ships, and unlike
`tokenfield-demo` its build has no GWT step that would need Java 8 (Sonar's scanner requires Java
17+, so that job runs on Java 21 while everything else builds on Java 8). It stops at the `test`
phase, which is enough to run the tests under the JaCoCo agent.

### Test coverage

Sonar does not measure coverage itself; it imports
[JaCoCo's XML report](https://docs.sonarsource.com/sonarqube-cloud/analyzing-source-code/test-coverage/java-test-coverage/),
and only the XML one — the binary `jacoco.exec` is not read. Three things make that work here, and
all three are load-bearing:

1. `jacoco:report` writes `tokenfield/target/site/jacoco/jacoco.xml` (XML is one of the goal's
   default formats), bound to the `test` phase.
2. `sonar.coverage.jacoco.xmlReportPaths` in [`tokenfield/pom.xml`](../tokenfield/pom.xml) points at
   that file, via the same `${project.reporting.outputDirectory}/jacoco` expression the report goal
   defaults to, so the two can't drift apart.
3. The workflow runs `mvn test` and the analysis as separate steps with a
   `test -s .../jacoco.xml` check between them — a report that stopped being generated fails the job
   there, instead of quietly publishing an analysis that shows no coverage.

`sonar.coverage.exclusions` in the parent POM keeps `**/client/**` out of the coverage figure, for
the same reason the JaCoCo check excludes it: GWT client-side code only runs compiled to JavaScript
in a browser, so no JVM test can cover it.

Note that this is unit-test coverage only. The Cucumber/Playwright suite exercises the add-on
server-side too, but its coverage isn't measured: the demo runs inside `jetty-maven-plugin`, so
attributing that execution to the add-on's classes would mean attaching the JaCoCo agent to the
Jetty JVM and merging a second `.exec` file into the report.

### GitHub code scanning (CodeQL)

Worth knowing as an alternative: CodeQL is free for public repositories, but on a private
repository it is part of GitHub Advanced Security, so it isn't enabled here.
