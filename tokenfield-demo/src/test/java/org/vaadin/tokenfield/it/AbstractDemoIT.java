package org.vaadin.tokenfield.it;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.assertions.PlaywrightAssertions;
import com.microsoft.playwright.junit.UsePlaywright;
import org.junit.jupiter.api.BeforeEach;

/**
 * Base class for the demo's browser integration tests. Each test method gets
 * a fresh {@link Page} (and thus a fresh {@code BrowserContext}/session) from
 * the Playwright JUnit extension, so server-side state — the demo's
 * {@code BeanItemContainer}s, {@code ListSelect}, etc. — cannot leak between
 * tests; no manual cleanup or {@code page.reload()} is needed.
 *
 * <p>{@code @UsePlaywright} is {@code @Inherited}, so subclasses pick it up
 * without repeating the annotation.</p>
 */
@UsePlaywright(DemoOptions.class)
abstract class AbstractDemoIT {

    protected DemoPage demo;

    @BeforeEach
    void openDemo(Page page) {
        PlaywrightAssertions.setDefaultAssertionTimeout(10_000);
        demo = new DemoPage(page);
        demo.open();
    }
}
