package org.vaadin.tokenfield.it;

import com.microsoft.playwright.junit.Options;
import com.microsoft.playwright.junit.OptionsFactory;

import java.nio.file.Paths;

/**
 * Playwright-for-Java configuration for the demo's browser integration
 * tests — the Java equivalent of the deleted {@code playwright.config.ts}.
 * Must be public with a no-arg constructor: {@link com.microsoft.playwright.junit.UsePlaywright}
 * instantiates it reflectively.
 */
public class DemoOptions implements OptionsFactory {

    @Override
    public Options getOptions() {
        return new Options()
                // set by failsafe from the port build-helper reserved; 8080 is the
                // manual fallback for `mvn -Dit.test=... -DskipITs=false` runs against
                // a hand-started `jetty:run`
                .setBaseUrl(System.getProperty("it.baseUrl", "http://localhost:8080"))
                .setBrowserName("chromium")
                .setHeadless(!Boolean.getBoolean("it.headed"))
                .setTrace(Options.Trace.RETAIN_ON_FAILURE)
                .setOutputDir(Paths.get("target", "playwright"));
    }
}
