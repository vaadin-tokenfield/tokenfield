package org.vaadin.tokenfield.it;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Steps specific to the "Data binding and buffering" panel: the shared
 * container backing both the field and a {@code ListSelect}, and the
 * buffered field's commit button.
 */
public class BufferedFieldSteps {

    private final DemoWorld world;

    public BufferedFieldSteps(DemoWorld world) {
        this.world = world;
    }

    @When("I click the commit button")
    public void clickCommit() {
        world.demo().commitButton().click();
        world.demo().waitForVaadin();
    }

    @Then("{string} appears as an available option in the linked selection")
    public void appearsAsOption(String text) {
        assertThat(world.demo().listSelectOption(text)).hasCount(1);
    }

    @Then("no option is yet marked as selected in the linked selection")
    public void nothingSelectedYet() {
        assertThat(world.demo().listSelectSelected()).hasCount(0);
    }

    @Then("{string} becomes the selected entry in the linked selection")
    public void becomesSelected(String text) {
        assertThat(world.demo().listSelectSelected()).hasText(text);
    }
}
