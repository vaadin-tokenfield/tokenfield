package org.vaadin.tokenfield.it;

import com.microsoft.playwright.Locator;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Generic add/remove/suggest steps shared by every panel: entering a value,
 * removing a token by click or Backspace, and the recall/duplicate-
 * prevention behaviour common to every {@link org.vaadin.tokenfield.TokenField}.
 */
public class TokenEntrySteps {

    private final DemoWorld world;

    public TokenEntrySteps(DemoWorld world) {
        this.world = world;
    }

    @Given("I have added the token {string}")
    public void addToken(String text) {
        world.demo().addToken(world.panel(), text);
    }

    @Given("I have added the tokens {string} and {string}, in that order")
    public void addTokens(String first, String second) {
        world.demo().addToken(world.panel(), first);
        world.demo().addToken(world.panel(), second);
    }

    @When("I type {string} and press Enter")
    public void typeAndPressEnter(String text) {
        world.demo().addToken(world.panel(), text);
    }

    @When("the input is empty and I press Backspace")
    public void backspaceOnEmptyInput() {
        world.demo().pressInEmptyInput(world.panel(), "Backspace");
    }

    @When("I start typing {string}")
    public void startTyping(String text) {
        world.demo().type(world.panel(), text);
    }

    @When("I click the {string} token chip")
    public void clickToken(String text) {
        world.demo().chip(world.panel(), text).click();
        world.demo().waitForVaadin();
    }

    @When("I type {string} and pick the matching suggestion")
    public void typeAndPickSuggestion(String text) {
        world.demo().type(world.panel(), text);
        world.demo().clickSuggestion(text);
    }

    @Then("a token chip labeled {string} appears in the field")
    public void tokenAppears(String text) {
        assertThat(world.demo().chip(world.panel(), text)).hasCount(1);
    }

    @Then("the {string} token is removed from the field")
    public void tokenRemoved(String text) {
        assertThat(world.demo().chip(world.panel(), text)).hasCount(0);
    }

    @Then("the {string} token remains")
    public void tokenRemains(String text) {
        assertThat(world.demo().chip(world.panel(), text)).hasCount(1);
    }

    @Then("the field still contains exactly one {string} token")
    public void exactlyOneToken(String text) {
        assertThat(world.demo().chip(world.panel(), text)).hasCount(1);
    }

    @Then("only the tokens {string} and {string} appear in the field")
    public void onlyTheseTokens(String first, String second) {
        assertThat(world.demo().chips(world.panel())).hasCount(2);
        assertThat(world.demo().chip(world.panel(), first)).hasCount(1);
        assertThat(world.demo().chip(world.panel(), second)).hasCount(1);
    }

    @Then("{string} appears in the suggestion list")
    public void appearsAsSuggestion(String text) {
        assertThat(world.demo().suggestionPopup()).isVisible();
        assertThat(world.demo().suggestions()
                .filter(new Locator.FilterOptions().setHasText(text))).hasCount(1);
    }

    @Then("the input is empty again, ready for the next value")
    public void inputIsEmpty() {
        assertThat(world.demo().input(world.panel())).hasValue("");
    }

    /**
     * Weaker than {@link #inputIsEmpty()} on purpose, for panels that set an
     * input prompt: Vaadin 7 renders the prompt as the input's value, so an
     * input that has been cleared reads as either "" or the prompt depending on
     * whether the field still has focus. What matters is that the text the user
     * turned into a token is gone.
     */
    @Then("the input no longer shows {string}")
    public void inputNoLongerShows(String text) {
        assertThat(world.demo().input(world.panel())).not().hasValue(text);
    }

    @Then("the input shows the placeholder {string}")
    public void inputShowsPlaceholder(String text) {
        assertThat(world.demo().input(world.panel())).hasValue(text);
    }
}
