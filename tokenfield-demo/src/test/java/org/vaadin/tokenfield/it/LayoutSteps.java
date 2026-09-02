package org.vaadin.tokenfield.it;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import static com.google.common.truth.Truth.assertThat;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Steps specific to the "Layout and insert position" panel: swapping the
 * field's layout, toggling where new tokens are inserted relative to the
 * input, and the read-only checkbox.
 */
public class LayoutSteps {

    // NativeSelect item order — see DemoRoot.Content and
    // DemoPage#layoutSelect/#insertPositionSelect.
    private static final int HORIZONTAL_LAYOUT = 0;
    private static final int VERTICAL_LAYOUT = 1;
    private static final int GRID_LAYOUT = 2;
    private static final int CSS_LAYOUT = 3;
    private static final int AFTER = 0;
    private static final int BEFORE = 1;

    /** One class among the several a chip carries. */
    private static final Pattern DISABLED = Pattern.compile("\\bv-disabled\\b");

    private final DemoWorld world;

    public LayoutSteps(DemoWorld world) {
        this.world = world;
    }

    @When("I switch the field's layout to {string}")
    public void switchLayout(String layoutName) {
        world.demo().selectByIndex(world.demo().layoutSelect(), layoutIndexFor(layoutName));
    }

    @Then("the field now uses a {string} layout")
    public void usesLayout(String layoutName) {
        String cssClass = "v-" + layoutName.toLowerCase(Locale.ROOT).replace(" ", "");
        assertThat(world.demo().tokenField(world.panel()).locator("." + cssClass)).hasCount(1);
    }

    @Then("both tokens {string} and {string} are still present")
    public void bothTokensPresent(String first, String second) {
        assertThat(world.demo().chip(world.panel(), first)).hasCount(1);
        assertThat(world.demo().chip(world.panel(), second)).hasCount(1);
    }

    @Given("the insert position is set to {string}")
    public void setInsertPosition(String position) {
        int index;
        switch (position) {
            case "BEFORE":
                index = BEFORE;
                break;
            case "AFTER":
                index = AFTER;
                break;
            default:
                throw new IllegalArgumentException("Unknown insert position: \"" + position + "\"");
        }
        world.demo().selectByIndex(world.demo().insertPositionSelect(), index);
    }

    @Then("the token chip appears {string} the input field")
    public void tokenChipOrder(String order) {
        List<String> actual = world.demo().tokenFieldChildOrder(world.panel());
        if ("before".equals(order)) {
            assertThat(actual).containsExactly("chip", "input").inOrder();
        } else {
            assertThat(actual).containsExactly("input", "chip").inOrder();
        }
    }

    @When("I mark the field as read-only")
    public void markReadOnly() {
        world.demo().readOnlyCheckbox().click();
        world.demo().waitForVaadin();
    }

    @When("I mark the field as editable again")
    public void markEditable() {
        world.demo().readOnlyCheckbox().click();
        world.demo().waitForVaadin();
    }

    @Then("the text input is no longer shown")
    public void inputHidden() {
        assertThat(world.demo().input(world.panel())).hasCount(0);
    }

    @Then("the text input reappears")
    public void inputVisibleAgain() {
        assertThat(world.demo().input(world.panel())).hasCount(1);
    }

    @Then("the {string} token chip is shown as disabled")
    public void chipIsDisabled(String text) {
        assertThat(world.demo().chip(world.panel(), text)).hasClass(DISABLED);
    }

    @When("I click the {string} token chip anyway")
    public void clickChipAnyway(String text) {
        world.demo().forceClick(world.demo().chip(world.panel(), text));
    }

    @Then("no error indicator is shown in the example")
    public void noErrorIndicator() {
        assertThat(world.demo().errorIndicators(world.panel())).hasCount(0);
    }

    private static int layoutIndexFor(String name) {
        switch (name) {
            case "Horizontal Layout":
                return HORIZONTAL_LAYOUT;
            case "Vertical Layout":
                return VERTICAL_LAYOUT;
            case "Grid Layout":
                return GRID_LAYOUT;
            case "Css Layout":
                return CSS_LAYOUT;
            default:
                throw new IllegalArgumentException("Unknown layout: \"" + name + "\"");
        }
    }
}
