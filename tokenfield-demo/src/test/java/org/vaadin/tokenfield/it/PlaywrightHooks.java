package org.vaadin.tokenfield.it;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Tracing;
import com.microsoft.playwright.assertions.PlaywrightAssertions;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Owns the Playwright browser-context/page lifecycle for every scenario —
 * the Cucumber equivalent of the deleted {@code AbstractDemoIT} + {@code
 * DemoOptions} pair. A fresh {@link BrowserContext} per scenario (not just a
 * fresh {@link Page}) is what isolates the demo's server-side state (its
 * {@code BeanItemContainer}s, {@code ListSelect}, etc.) between scenarios —
 * the same guarantee {@code AbstractDemoIT}'s javadoc documented for the
 * deleted {@code *IT} classes; only the {@link Browser} process itself, from
 * {@link DemoBrowser}, is now shared across the whole run.
 *
 * <p>Also hand-rolls what {@code DemoOptions}'
 * {@code .setTrace(Options.Trace.RETAIN_ON_FAILURE)} used to do
 * declaratively: tracing runs for every scenario, but the trace is only
 * written to disk (under {@code target/playwright}) when the scenario fails
 * — {@link Tracing#stop(Tracing.StopOptions)} discards the recording unless
 * given a {@code path}.
 */
public class PlaywrightHooks {

    private final DemoWorld world;

    public PlaywrightHooks(DemoWorld world) {
        this.world = world;
    }

    @Before
    public void openDemo() {
        PlaywrightAssertions.setDefaultAssertionTimeout(10_000);

        BrowserContext context = DemoBrowser.get().newContext(new Browser.NewContextOptions()
                .setBaseURL(DemoServer.baseUrl()));
        context.tracing().start(new Tracing.StartOptions()
                .setScreenshots(true)
                .setSnapshots(true));

        Page page = context.newPage();
        world.open(context, page);
    }

    @After
    public void closeDemo(Scenario scenario) {
        BrowserContext context = world.context();
        if (context == null) {
            // openDemo() failed before it got a context — launching the browser,
            // or reaching the demo. Cucumber runs @After anyway; without this,
            // the hook's own NullPointerException replaces that real failure as
            // the reported cause.
            return;
        }
        if (scenario.isFailed()) {
            context.tracing().stop(new Tracing.StopOptions().setPath(tracePathFor(scenario)));
        } else {
            context.tracing().stop();
        }
        context.close();
    }

    private static Path tracePathFor(Scenario scenario) {
        String fileName = scenario.getName().replaceAll("[^a-zA-Z0-9.-]+", "-") + ".zip";
        return Paths.get("target", "playwright", fileName);
    }
}
