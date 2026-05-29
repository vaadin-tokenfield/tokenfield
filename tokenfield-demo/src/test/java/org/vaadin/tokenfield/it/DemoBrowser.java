package org.vaadin.tokenfield.it;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;

/**
 * One Chromium {@link Browser} shared by every scenario in the run. {@code
 * @UsePlaywright} used to launch a fresh browser per JUnit Jupiter test
 * method; that cost (~1s) is fine amortized over a handful of {@code *IT}
 * classes, but not over ~20 individual scenarios. {@link PlaywrightHooks}
 * still opens a fresh {@link com.microsoft.playwright.BrowserContext} per
 * scenario for isolation — only the (expensive, isolation-irrelevant)
 * browser process itself is shared.
 *
 * <p>Never explicitly closed: a JVM-exit shutdown hook does that, same as
 * {@link DemoServer}'s embedded Jetty fallback.
 */
final class DemoBrowser {

    private static Playwright playwright;
    private static Browser browser;

    private DemoBrowser() {
    }

    static synchronized Browser get() {
        if (browser == null) {
            playwright = Playwright.create();
            browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                    .setHeadless(!Boolean.getBoolean("it.headed")));
            Playwright p = playwright;
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    browser.close();
                } catch (Exception e) {
                    // best-effort shutdown, JVM is exiting anyway
                }
                try {
                    p.close();
                } catch (Exception e) {
                    // best-effort shutdown, JVM is exiting anyway
                }
            }));
        }
        return browser;
    }
}
