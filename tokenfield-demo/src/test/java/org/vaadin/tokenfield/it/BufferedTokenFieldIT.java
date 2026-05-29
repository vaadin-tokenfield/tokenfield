package org.vaadin.tokenfield.it;

import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Browser coverage for the "Data binding and buffering" demo panel, untouched
 * by the deleted TypeScript suite. The panel's {@link
 * org.vaadin.tokenfield.TokenField} shares its container with a {@code
 * ListSelect} ("One".."Five") and is buffered with its value bound to that
 * same {@code ListSelect} — see {@code DemoRoot.java:321-366}. New tokens are
 * written into the shared container immediately (visible to both widgets),
 * but the buffered value itself is only written back to the {@code
 * ListSelect} when the "&lt;&lt;" button calls {@code TokenField#commit()}.
 */
class BufferedTokenFieldIT extends AbstractDemoIT {

    @Test
    void newTokenAppearsInSharedContainerButIsNotSelectedUntilCommit() {
        demo.addToken(DemoPage.BUFFERED, "Six");

        assertThat(demo.chip(DemoPage.BUFFERED, "Six")).hasCount(1);
        assertThat(demo.listSelectOption("Six")).hasCount(1);
        assertThat(demo.listSelectSelected()).hasCount(0);
    }

    @Test
    void clickingCommitWritesSelectionToListSelect() {
        demo.addToken(DemoPage.BUFFERED, "Six");

        demo.commitButton().click();
        demo.waitForVaadin();

        assertThat(demo.listSelectSelected()).hasText("Six");
    }

    @Test
    void selectingSuggestionFromSharedContainerAddsToken() {
        demo.type(DemoPage.BUFFERED, "Three");
        assertThat(demo.suggestionPopup()).isVisible();
        demo.clickSuggestion("Three");

        assertThat(demo.chip(DemoPage.BUFFERED, "Three")).hasCount(1);
        assertThat(demo.listSelectSelected()).hasCount(0);
    }
}
