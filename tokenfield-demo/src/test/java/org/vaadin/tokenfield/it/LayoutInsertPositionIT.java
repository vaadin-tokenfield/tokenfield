package org.vaadin.tokenfield.it;

import org.junit.jupiter.api.Test;

import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Browser coverage for the "Layout and InsertPosition" demo panel, untouched
 * by the deleted TypeScript suite. The panel starts on {@code CssLayout},
 * {@link org.vaadin.tokenfield.TokenField.InsertPosition#BEFORE}, not
 * read-only — see {@code DemoRoot.java:240-317}.
 *
 * <p>Note: this class deliberately does not assert delete-key behaviour under
 * {@code InsertPosition.AFTER}. The client-side {@code VTokenField.after}
 * flag is never assigned from server state ({@code TokenComboBox.java:45-47},
 * {@code VTokenField.java:28}), so Backspace always fires the delete RPC
 * regardless of insert position — a known, out-of-scope production gap, not
 * something this suite pins or documents as passing.</p>
 */
class LayoutInsertPositionIT extends AbstractDemoIT {

    // Layout NativeSelect item order: HorizontalLayout, VerticalLayout, GridLayout, CssLayout
    private static final int HORIZONTAL_LAYOUT = 0;
    private static final int GRID_LAYOUT = 2;

    // InsertPosition NativeSelect item order: AFTER, BEFORE
    private static final int AFTER = 0;

    @Test
    void switchingToHorizontalLayoutPreservesTokens() {
        demo.addToken(DemoPage.LAYOUT, "a");
        demo.addToken(DemoPage.LAYOUT, "b");

        demo.selectByIndex(demo.layoutSelect(), HORIZONTAL_LAYOUT);

        assertThat(demo.tokenField(DemoPage.LAYOUT).locator(".v-horizontallayout")).hasCount(1);
        assertThat(demo.chip(DemoPage.LAYOUT, "a")).hasCount(1);
        assertThat(demo.chip(DemoPage.LAYOUT, "b")).hasCount(1);
    }

    @Test
    void switchingToGridLayoutPreservesTokens() {
        demo.addToken(DemoPage.LAYOUT, "a");
        demo.addToken(DemoPage.LAYOUT, "b");

        demo.selectByIndex(demo.layoutSelect(), GRID_LAYOUT);

        assertThat(demo.tokenField(DemoPage.LAYOUT).locator(".v-gridlayout")).hasCount(1);
        assertThat(demo.chip(DemoPage.LAYOUT, "a")).hasCount(1);
        assertThat(demo.chip(DemoPage.LAYOUT, "b")).hasCount(1);
    }

    @Test
    void insertPositionBeforePutsChipsBeforeInput() {
        demo.addToken(DemoPage.LAYOUT, "x");
        List<String> order = demo.tokenFieldChildOrder(DemoPage.LAYOUT);
        assertThat(order).containsExactly("chip", "input").inOrder();
    }

    @Test
    void insertPositionAfterPutsChipsAfterInput() {
        demo.selectByIndex(demo.insertPositionSelect(), AFTER);
        demo.addToken(DemoPage.LAYOUT, "x");
        List<String> order = demo.tokenFieldChildOrder(DemoPage.LAYOUT);
        assertThat(order).containsExactly("input", "chip").inOrder();
    }

    @Test
    void readOnlyCheckboxHidesTheInput() {
        demo.addToken(DemoPage.LAYOUT, "kept");

        demo.readOnlyCheckbox().click();
        demo.waitForVaadin();
        assertThat(demo.input(DemoPage.LAYOUT)).hasCount(0);
        assertThat(demo.chip(DemoPage.LAYOUT, "kept")).hasCount(1);

        demo.readOnlyCheckbox().click();
        demo.waitForVaadin();
        assertThat(demo.input(DemoPage.LAYOUT)).hasCount(1);
    }
}
