package org.vaadin.tokenfield.it;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.List;
import java.util.Locale;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Steps specific to the "Address book" panel: address-book-aware token
 * captions, the off-book "emphasize" style, suggestion filtering, and the
 * New Contact / Remove confirmation windows.
 */
public class AddressBookSteps {

    private static final String EMPHASIZE_STYLE = "v-button-emphasize";

    private final DemoWorld world;

    public AddressBookSteps(DemoWorld world) {
        this.world = world;
    }

    @When("I choose {string} in the {string} window")
    public void chooseWindowButton(String buttonCaption, String windowCaption) {
        world.demo().clickWindowButton(windowCaption, buttonCaption);
    }

    @Then("a {string} window opens")
    public void windowOpens(String caption) {
        assertThat(world.demo().window(caption)).isVisible();
    }

    @Then("the {string} window closes")
    public void windowCloses(String caption) {
        assertThat(world.demo().window(caption)).hasCount(0);
    }

    @Then("no confirmation window is shown")
    public void noWindowShown() {
        assertThat(world.demo().page().locator(".v-window")).hasCount(0);
    }

    @Then("the {string} token is marked as not part of the address book")
    public void tokenIsOffBook(String text) {
        String tokenClass = world.demo().chip(world.panel(), text).getAttribute("class");
        assertWithMessage("Off-container token must use the emphasize style: " + tokenClass)
                .that(tokenClass).contains(EMPHASIZE_STYLE);
    }

    @Then("the {string} token is not marked as off-book")
    public void tokenIsNotOffBook(String text) {
        String tokenClass = world.demo().chip(world.panel(), text).getAttribute("class");
        assertWithMessage("Container-backed token must not be emphasized: " + tokenClass)
                .that(tokenClass).doesNotContain(EMPHASIZE_STYLE);
    }

    @Then("all visible suggestions contain {string}")
    public void suggestionsAllContain(String text) {
        // allTextContents() reads immediately, unlike assertThat(Locator) — it
        // doesn't auto-wait for the popup's async, server-round-tripped filter
        // query to land, so a plain read here races that query.
        assertThat(world.demo().suggestionPopup()).isVisible();
        List<String> names = world.demo().suggestions().allTextContents();
        assertThat(names).isNotEmpty();
        for (String name : names) {
            assertWithMessage("FilteringMode.CONTAINS must only surface matching suggestions: " + name)
                    .that(name.toLowerCase(Locale.ROOT)).contains(text.toLowerCase(Locale.ROOT));
        }
    }
}
