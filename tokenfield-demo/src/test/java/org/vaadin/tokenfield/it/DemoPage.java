package org.vaadin.tokenfield.it;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.SelectOption;

import java.util.List;

/**
 * Page object for the seven-panel Vaadin 7 demo rendered by {@code DemoRoot}.
 * Replaces the deleted TypeScript {@code waitForVaadin}/{@code panelContent}
 * helpers: idle detection here polls {@code vaadin.clients[*].isActive()} —
 * the same contract Vaadin TestBench uses — instead of the {@code
 * .v-loading-indicator} CSS class, which Vaadin 7 only shows ~300 ms after a
 * request starts and so can read "idle" before a round-trip has begun.
 */
final class DemoPage {

    /** Panel indices, in the order {@code DemoRoot.Content} adds them. */
    static final int BASIC = 0;
    static final int COMMA = 1;
    static final int FULL = 2;
    static final int LAYOUT = 3;
    static final int BUFFERED = 4;
    static final int JPA = 5;
    static final int LISTENER = 6;

    private static final String VAADIN_IDLE =
            "() => {"
          + "  if (!window.vaadin || !window.vaadin.clients) return false;"
          + "  for (var k in window.vaadin.clients) {"
          + "    if (window.vaadin.clients[k].isActive()) return false;"
          + "  }"
          + "  return true;"
          + "}";

    private final Page page;

    DemoPage(Page page) {
        this.page = page;
    }

    Page page() {
        return page;
    }

    void open() {
        page.navigate("/");
        page.waitForSelector(".v-app");
        waitForVaadin();
    }

    void waitForVaadin() {
        page.waitForFunction(VAADIN_IDLE);
    }

    // -----------------------------------------------------------------------
    // Structure
    // -----------------------------------------------------------------------

    Locator panel(int index) {
        return page.locator(".v-panel").nth(index);
    }

    Locator panelContent(int index) {
        return panel(index).locator(".v-panel-content");
    }

    Locator tokenField(int index) {
        return panelContent(index).locator(".v-widget.tokenfield").first();
    }

    Locator input(int index) {
        return panelContent(index).locator(".v-filterselect input").first();
    }

    Locator chips(int index) {
        return panelContent(index).locator(".v-button-link");
    }

    Locator chip(int index, String text) {
        return chips(index).filter(new Locator.FilterOptions().setHasText(text));
    }

    // -----------------------------------------------------------------------
    // Interaction
    // -----------------------------------------------------------------------

    void addToken(int index, String text) {
        Locator in = input(index);
        in.click();
        in.fill(text);
        in.press("Enter");
        waitForVaadin();
    }

    void pressInEmptyInput(int index, String key) {
        Locator in = input(index);
        in.click();
        in.fill("");
        in.press(key);
        waitForVaadin();
    }

    /**
     * Types character-by-character (real keydown/keyup events), unlike
     * {@code fill()} — Vaadin 7's ComboBoxConnector schedules its filtered
     * suggestion query from a {@code KeyUpHandler}, which a synthetic
     * {@code fill()} value change does not fire.
     */
    void type(int index, String text) {
        Locator in = input(index);
        in.click();
        in.pressSequentially(text);
    }

    // -----------------------------------------------------------------------
    // Suggestion popup
    // -----------------------------------------------------------------------

    Locator suggestionPopup() {
        return page.locator(".v-filterselect-suggestpopup");
    }

    Locator suggestions() {
        return suggestionPopup().locator(".gwt-MenuItem");
    }

    void clickSuggestion(String text) {
        suggestions().filter(new Locator.FilterOptions().setHasText(text)).first().click();
        waitForVaadin();
    }

    // -----------------------------------------------------------------------
    // Sub-windows (EditContactWindow / RemoveWindow)
    // -----------------------------------------------------------------------

    Locator window(String captionFragment) {
        return page.locator(".v-window").filter(new Locator.FilterOptions().setHasText(captionFragment));
    }

    void clickWindowButton(String windowCaption, String buttonCaption) {
        window(windowCaption).locator(".v-button")
                .filter(new Locator.FilterOptions().setHasText(buttonCaption))
                .first().click();
        waitForVaadin();
    }

    // -----------------------------------------------------------------------
    // Panel 3 controls (DOM order: Layout select, then InsertPosition select)
    // -----------------------------------------------------------------------

    Locator layoutSelect() {
        return panelContent(LAYOUT).locator("select").nth(0);
    }

    Locator insertPositionSelect() {
        return panelContent(LAYOUT).locator("select").nth(1);
    }

    Locator readOnlyCheckbox() {
        return panelContent(LAYOUT).locator(".v-checkbox input[type=checkbox]");
    }

    /**
     * Selects the option at {@code index} (the NativeSelects in this panel
     * have no explicit item captions, so their rendered labels default to
     * {@code Class#toString()}/{@code enum#toString()} — index-based
     * selection, matching {@code DemoRoot}'s {@code addItem} order, is far
     * more robust than matching on that text).
     */
    void selectByIndex(Locator nativeSelect, int index) {
        nativeSelect.selectOption(new SelectOption().setIndex(index));
        waitForVaadin();
    }

    // -----------------------------------------------------------------------
    // Panel 4 controls
    // -----------------------------------------------------------------------

    Locator listSelect() {
        return panelContent(BUFFERED).locator("select[multiple]");
    }

    Locator listSelectOption(String text) {
        return listSelect().locator("option").filter(new Locator.FilterOptions().setHasText(text));
    }

    Locator listSelectSelected() {
        return listSelect().locator("option:checked");
    }

    Locator commitButton() {
        return panelContent(BUFFERED).locator(".v-button")
                .filter(new Locator.FilterOptions().setHasText("<<"));
    }

    // -----------------------------------------------------------------------
    // DOM-order assertions (BEFORE vs AFTER insert position)
    // -----------------------------------------------------------------------

    /**
     * Renders the token field's children as a list of "input"/"chip" markers
     * in DOM order — the only reliable way to assert InsertPosition BEFORE
     * vs AFTER.
     */
    @SuppressWarnings("unchecked")
    List<String> tokenFieldChildOrder(int index) {
        return (List<String>) tokenField(index).evaluate(
                "el => Array.from(el.querySelectorAll('.v-filterselect, .v-button-link'))"
              + "        .map(n => n.classList.contains('v-filterselect') ? 'input' : 'chip')");
    }
}
