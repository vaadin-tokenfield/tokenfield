package org.vaadin.tokenfield.it;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Steps that pick which of the demo's seven example panels a scenario works
 * in, plus the one page-level check that isn't scoped to any single panel.
 */
public class DemoSteps {

    private final DemoWorld world;

    public DemoSteps(DemoWorld world) {
        this.world = world;
    }

    @Given("the {string} example")
    public void selectExample(String name) {
        world.usePanel(panelIndexFor(name));
    }

    @Then("the demo page shows all seven example panels, each with its own input")
    public void demoShowsAllSevenPanels() {
        assertThat(world.demo().page().locator(".v-panel")).hasCount(7);
        for (int i = DemoPage.BASIC; i <= DemoPage.LISTENER; i++) {
            assertThat(world.demo().input(i)).isVisible();
        }
    }

    /**
     * Maps the scenarios' business-language panel names to {@link
     * DemoPage}'s indices — deliberately not the demo's own {@code Panel}
     * captions (e.g. "Full featured example" for the address book), so the
     * feature file reads as a user-facing spec rather than mirroring
     * internal UI labels.
     */
    static int panelIndexFor(String name) {
        switch (name) {
            case "Basic":
                return DemoPage.BASIC;
            case "Comma separated":
                return DemoPage.COMMA;
            case "Address book":
                return DemoPage.FULL;
            case "Layout and insert position":
                return DemoPage.LAYOUT;
            case "Data binding and buffering":
                return DemoPage.BUFFERED;
            case "JPAContainer":
                return DemoPage.JPA;
            case "Container listener":
                return DemoPage.LISTENER;
            default:
                throw new IllegalArgumentException("Unknown demo example: \"" + name + "\"");
        }
    }
}
