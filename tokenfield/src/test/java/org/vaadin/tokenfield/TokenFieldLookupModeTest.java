package org.vaadin.tokenfield;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

/**
 * Lookup mode: {@code setNewTokensAllowed(false)}. Only container items become
 * tokens through the input; typed text that matches no suggestion is dropped
 * by {@code ComboBox.changeVariables} before any handler sees it, and
 * {@code setRememberNewTokens} has no effect.
 *
 * <p>Reference: {@code ComboBox.changeVariables} only consults the
 * {@code newitem} variable when {@code isNewItemsAllowed()}
 * ({@code ComboBox.java:757}, vaadin-server 7.7.17).</p>
 */
class TokenFieldLookupModeTest {

    private TestTokenField field;

    @BeforeEach
    void setup() {
        field = new TestTokenField();
        field.getContainerDataSource().addItem("Finland");
        field.setNewTokensAllowed(false);
    }

    @Test
    void typedTextThatIsNotInTheContainerIsDropped() {
        field.simulateTypedInput("Atlantis");

        assertThat(field.getValue()).isNull();
        assertThat(field.getComboBox().getItemIds()).containsExactly("Finland");
    }

    @Test
    void rememberNewTokensHasNoEffect() {
        field.setRememberNewTokens(false);
        field.simulateTypedInput("Atlantis");
        field.setRememberNewTokens(true);
        field.simulateTypedInput("Lemuria");

        assertThat(field.getValue()).isNull();
        assertThat(field.getComboBox().getItemIds()).containsExactly("Finland");
    }

    @Test
    void theNewTokenHandlerIsNeverAsked() {
        field.setNewTokenHandler(text -> {
            throw new AssertionError("asked for " + text);
        });

        field.simulateTypedInput("Atlantis");

        assertThat(field.getValue()).isNull();
    }

    @Test
    void aPickedSuggestionBecomesAToken() {
        field.simulateSelect("Finland");

        assertThat(field.getValue()).containsExactly("Finland");
        assertThat(field.getTokenButtons()).containsKey("Finland");
    }

    @Test
    void aTokenCanStillBeAddedProgrammatically() {
        field.addToken("Atlantis");

        assertWithMessage("addToken is not gated by newTokensAllowed")
                .that(field.getValue()).containsExactly("Atlantis");
        assertThat(field.getTokenCaption("Atlantis")).isEqualTo("Atlantis");
    }

    @Test
    void theSameTypedInputIsAcceptedOnceNewTokensAreAllowedAgain() {
        field.setNewTokensAllowed(true);

        field.simulateTypedInput("Atlantis");

        assertWithMessage("sanity check that simulateTypedInput reaches the handler")
                .that(field.getValue()).containsExactly("Atlantis");
    }
}
