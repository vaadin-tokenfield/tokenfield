package org.vaadin.tokenfield.it;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;

/**
 * Per-scenario state, instantiated fresh for every scenario by {@code
 * cucumber-picocontainer} and injected into {@link PlaywrightHooks} and each
 * step class — the Cucumber equivalent of the {@code protected DemoPage demo}
 * field {@code AbstractDemoIT} used to give every {@code *IT} test method.
 *
 * <p>Must be {@code public}: {@code cucumber-picocontainer}'s {@code
 * PicoFactory} only registers glue classes it considers "instantiable",
 * which requires {@code public} (see {@code PicoFactory.isInstantiable}) —
 * a package-private {@code DemoWorld} silently fails to be added as a
 * constructor dependency for {@link PlaywrightHooks} and the step classes.
 */
public class DemoWorld {

    private BrowserContext context;
    private DemoPage demo;
    private int panel = -1;

    void open(BrowserContext context, Page page) {
        this.context = context;
        this.demo = new DemoPage(page);
        demo.open();
    }

    BrowserContext context() {
        return context;
    }

    DemoPage demo() {
        if (demo == null) {
            throw new IllegalStateException(
                    "DemoWorld.demo() used before PlaywrightHooks opened the demo page for this scenario");
        }
        return demo;
    }

    /** The demo panel ({@link DemoPage#BASIC} etc.) the current scenario is working in. */
    int panel() {
        if (panel < 0) {
            throw new IllegalStateException(
                    "No demo panel selected yet — the scenario is missing a 'Given the ... example' step");
        }
        return panel;
    }

    void usePanel(int panel) {
        this.panel = panel;
    }
}
