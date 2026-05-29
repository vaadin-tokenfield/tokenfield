package org.vaadin.tokenfield.it;

import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Browser coverage for the "Comma separated" demo panel, which was untouched
 * by the deleted TypeScript suite. Exercises the demo's overridden {@code
 * onTokenInput}/{@code rememberToken} that split typed input on {@code ,}.
 */
class CommaSeparatedTokenFieldIT extends AbstractDemoIT {

    @Test
    void commaSeparatedInputCreatesThreeTokens() {
        demo.addToken(DemoPage.COMMA, "a, b, c");
        assertThat(demo.chip(DemoPage.COMMA, "a")).hasCount(1);
        assertThat(demo.chip(DemoPage.COMMA, "b")).hasCount(1);
        assertThat(demo.chip(DemoPage.COMMA, "c")).hasCount(1);
    }

    @Test
    void blankSegmentsAreIgnored() {
        demo.addToken(DemoPage.COMMA, "x,,y ,");
        assertThat(demo.chips(DemoPage.COMMA)).hasCount(2);
        assertThat(demo.chip(DemoPage.COMMA, "x")).hasCount(1);
        assertThat(demo.chip(DemoPage.COMMA, "y")).hasCount(1);
    }

    @Test
    void inputPromptIsRendered() {
        // Vaadin 7's VFilterSelect renders the input prompt as the field's
        // own value (styled via the "v-filterselect-prompt" class) rather
        // than a native HTML5 placeholder attribute.
        assertThat(demo.input(DemoPage.COMMA).inputValue()).isEqualTo("tag, another, yetanother");
    }
}
