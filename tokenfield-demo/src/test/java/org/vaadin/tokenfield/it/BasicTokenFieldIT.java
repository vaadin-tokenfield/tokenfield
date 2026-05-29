package org.vaadin.tokenfield.it;

import com.microsoft.playwright.Locator;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Browser coverage for the "Basic" demo panel (all {@link
 * org.vaadin.tokenfield.TokenField} defaults). Ports the five smoke tests
 * from the deleted {@code tokenfield.smoke.spec.ts}, plus a couple of gaps
 * that suite left in this panel.
 */
class BasicTokenFieldIT extends AbstractDemoIT {

    @Test
    void demoPageRendersAllFivePanels() {
        assertThat(demo.page().locator(".v-panel")).hasCount(5);
        for (int i = 0; i <= DemoPage.LAYOUT; i++) {
            assertThat(demo.input(i)).isVisible();
        }
    }

    @Test
    void typingTagAndPressingEnterCreatesTokenChip() {
        demo.addToken(DemoPage.BASIC, "mytag");
        assertThat(demo.chip(DemoPage.BASIC, "mytag")).hasCount(1);
    }

    @Test
    void clickingTokenChipRemovesIt() {
        demo.addToken(DemoPage.BASIC, "removeme");
        assertThat(demo.chip(DemoPage.BASIC, "removeme")).hasCount(1);
        demo.chip(DemoPage.BASIC, "removeme").click();
        demo.waitForVaadin();
        assertThat(demo.chip(DemoPage.BASIC, "removeme")).hasCount(0);
    }

    @Test
    void backspaceOnEmptyInputRemovesLastToken() {
        demo.addToken(DemoPage.BASIC, "first");
        demo.addToken(DemoPage.BASIC, "last");
        demo.pressInEmptyInput(DemoPage.BASIC, "Backspace");
        assertThat(demo.chip(DemoPage.BASIC, "last")).hasCount(0);
        assertThat(demo.chip(DemoPage.BASIC, "first")).hasCount(1);
    }

    @Test
    void newTokenIsRememberedAsSuggestion() {
        demo.addToken(DemoPage.BASIC, "mytag");
        demo.type(DemoPage.BASIC, "myt");
        assertThat(demo.suggestionPopup()).isVisible();
        assertThat(demo.suggestions().filter(new Locator.FilterOptions().setHasText("mytag")))
                .hasCount(1);
    }

    @Test
    void duplicateTokenIsNotAddedTwice() {
        demo.addToken(DemoPage.BASIC, "dup");
        demo.addToken(DemoPage.BASIC, "dup");
        assertThat(demo.chip(DemoPage.BASIC, "dup")).hasCount(1);
    }
}
