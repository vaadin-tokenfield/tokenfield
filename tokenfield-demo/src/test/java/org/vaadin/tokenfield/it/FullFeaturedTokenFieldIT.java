package org.vaadin.tokenfield.it;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Browser coverage for the "Full featured example" demo panel. Only the
 * filtering behaviour of this panel was exercised by the deleted TypeScript
 * suite; everything else here — the address-book windows, the emphasize
 * style, the confirm-free delete path — was untested.
 *
 * <p>The panel is seeded from {@code DemoRoot.generateTestContainer()}, which
 * uses {@code new Random(5)}: deterministic across JVMs. The first two
 * generated contacts (in insertion/iteration order) are pre-added as tokens,
 * alongside one off-container contact — see {@code DemoRoot.java:216-219}:</p>
 * <pre>
 * 0: Nicole Smith  -&gt; nicole.smith@example.com   (in container)
 * 1: Robert McGoff -&gt; robert.mcgoff@example.com  (in container)
 *    thatnewguy@example.com                       (off container)
 * </pre>
 */
class FullFeaturedTokenFieldIT extends AbstractDemoIT {

    private static final String NICOLE = "Nicole Smith";
    private static final String NICOLE_EMAIL = "nicole.smith@example.com";
    private static final String ROBERT = "Robert McGoff";
    private static final String ROBERT_EMAIL = "robert.mcgoff@example.com";
    private static final String OFF_CONTAINER = "thatnewguy@example.com";

    @Test
    void preAddedTokensAreRenderedWithNameAndEmailCaption() {
        assertThat(demo.chips(DemoPage.FULL)).hasCount(3);
        assertThat(demo.chip(DemoPage.FULL, NICOLE + " <" + NICOLE_EMAIL + ">")).hasCount(1);
        assertThat(demo.chip(DemoPage.FULL, ROBERT + " <" + ROBERT_EMAIL + ">")).hasCount(1);
        assertThat(demo.chip(DemoPage.FULL, OFF_CONTAINER + " <" + OFF_CONTAINER + ">")).hasCount(1);
    }

    @Test
    void offContainerTokenIsEmphasized() {
        String offContainerClass = demo.chip(DemoPage.FULL, OFF_CONTAINER).getAttribute("class");
        assertWithMessage("Off-container token must use the emphasize style: " + offContainerClass)
                .that(offContainerClass).contains("v-button-emphasize");

        String nicoleClass = demo.chip(DemoPage.FULL, NICOLE).getAttribute("class");
        assertWithMessage("Container-backed token must not be emphasized: " + nicoleClass)
                .that(nicoleClass).doesNotContain("v-button-emphasize");
    }

    @Test
    void typingFiltersSuggestions() {
        demo.type(DemoPage.FULL, "e");
        assertThat(demo.suggestionPopup()).isVisible();
        List<String> names = demo.suggestions().allTextContents();
        assertThat(names).isNotEmpty();
        for (String name : names) {
            assertWithMessage("FilteringMode.CONTAINS must only surface matching suggestions: " + name)
                    .that(name.toLowerCase(Locale.ROOT)).contains("e");
        }
    }

    @Test
    void selectingSuggestionAddsTokenImmediately() {
        // "Fielding" uniquely matches "Sarah Fielding", not yet a token.
        demo.type(DemoPage.FULL, "Fielding");
        assertThat(demo.suggestionPopup()).isVisible();
        demo.clickSuggestion("Fielding");

        assertThat(demo.chip(DemoPage.FULL, "Sarah Fielding")).hasCount(1);
        assertThat(demo.chips(DemoPage.FULL)).hasCount(4);
        assertThat(demo.page().locator(".v-window")).hasCount(0);
    }

    @Test
    void unknownAddressOpensEditContactWindowAndDontAddKeepsItOffContainer() {
        demo.addToken(DemoPage.FULL, "new@example.com");

        assertThat(demo.window("New Contact")).isVisible();
        demo.clickWindowButton("New Contact", "Don't add");

        assertThat(demo.window("New Contact")).hasCount(0);
        assertThat(demo.chip(DemoPage.FULL, "new@example.com")).hasCount(1);
        String addedClass = demo.chip(DemoPage.FULL, "new@example.com").getAttribute("class");
        assertWithMessage("'Don't add' must keep the contact off the container, so it stays emphasized")
                .that(addedClass).contains("v-button-emphasize");
    }

    @Test
    void addToContactsMakesTokenPartOfAddressBook() {
        demo.addToken(DemoPage.FULL, "another@example.com");

        assertThat(demo.window("New Contact")).isVisible();
        demo.clickWindowButton("New Contact", "Add to contacts");

        assertThat(demo.window("New Contact")).hasCount(0);
        assertThat(demo.chip(DemoPage.FULL, "another@example.com")).hasCount(1);
        String addedClass = demo.chip(DemoPage.FULL, "another@example.com").getAttribute("class");
        assertWithMessage("'Add to contacts' must add the contact to the container, clearing the emphasize style")
                .that(addedClass).doesNotContain("v-button-emphasize");
    }

    @Test
    void clickingChipOpensRemoveWindowAndCancelKeepsToken() {
        demo.chip(DemoPage.FULL, NICOLE).click();
        demo.waitForVaadin();

        assertThat(demo.window("Remove " + NICOLE)).isVisible();
        demo.clickWindowButton("Remove " + NICOLE, "Cancel");

        assertThat(demo.window("Remove " + NICOLE)).hasCount(0);
        assertThat(demo.chip(DemoPage.FULL, NICOLE)).hasCount(1);
    }

    @Test
    void removeWindowRemoveDeletesToken() {
        demo.chip(DemoPage.FULL, ROBERT).click();
        demo.waitForVaadin();

        assertThat(demo.window("Remove " + ROBERT)).isVisible();
        demo.clickWindowButton("Remove " + ROBERT, "Remove");

        assertThat(demo.window("Remove " + ROBERT)).hasCount(0);
        assertThat(demo.chip(DemoPage.FULL, ROBERT)).hasCount(0);
    }

    @Test
    void backspaceRemovesLastTokenWithoutConfirmationWindow() {
        // Insertion order is Nicole, Robert, then the off-container contact —
        // onTokenDelete is overridden to bypass RemoveWindow entirely.
        demo.pressInEmptyInput(DemoPage.FULL, "Backspace");

        assertThat(demo.page().locator(".v-window")).hasCount(0);
        assertThat(demo.chip(DemoPage.FULL, OFF_CONTAINER)).hasCount(0);
        assertThat(demo.chip(DemoPage.FULL, NICOLE)).hasCount(1);
        assertThat(demo.chip(DemoPage.FULL, ROBERT)).hasCount(1);
    }
}
