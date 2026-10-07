package org.vaadin.tokenfield;

import com.vaadin.server.Resource;
import com.vaadin.server.ThemeResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;

/**
 * Oblivious mode: new tokens allowed but not remembered
 * ({@code setRememberNewTokens(false)}). Typed text becomes a token of the
 * field only; the container is never touched, so the text is not offered as a
 * suggestion afterwards. Captions and icons for such a token come from the
 * explicit maps or, in {@code ITEM}/{@code PROPERTY} mode, fall back to the
 * text (see {@link TokenField#getTokenCaption(Object)}).
 *
 * <p>The same mode over a container keyed by another type is covered by
 * {@link TokenFieldForeignIdContainerTest}.</p>
 */
class TokenFieldObliviousModeTest {

    private TestTokenField field;

    @BeforeEach
    void setup() {
        field = new TestTokenField();
        field.getContainerDataSource().addItem("java");
        field.setRememberNewTokens(false);
    }

    @Test
    void typedTextBecomesATokenOfTheFieldOnly() {
        field.simulateTypedInput("kotlin");

        assertThat(field.getValue()).containsExactly("kotlin");
        assertThat(field.getTokenButtons()).containsKey("kotlin");
        assertThat(field.getComboBox().getItemIds()).containsExactly("java");
    }

    @Test
    void typingTheSameTextTwiceKeepsOneToken() {
        field.simulateTypedInput("kotlin");
        field.simulateTypedInput("kotlin");

        assertThat(field.getValue()).containsExactly("kotlin");
        assertThat(field.getTokenButtons()).hasSize(1);
    }

    @Test
    void aPickedSuggestionStillBecomesAToken() {
        field.simulateSelect("java");

        assertThat(field.getValue()).containsExactly("java");
    }

    @Test
    void anExplicitCaptionAndIconApplyToTheTypedToken() {
        Resource icon = new ThemeResource("icons/token.png");
        field.setTokenCaption("kotlin", "Kotlin");
        field.setTokenIcon("kotlin", icon);

        field.simulateTypedInput("kotlin");

        assertThat(field.getTokenCaption("kotlin")).isEqualTo("Kotlin");
        assertThat(field.getTokenIcon("kotlin")).isSameInstanceAs(icon);
        assertThat(field.getTokenButtons().get("kotlin").getIcon())
                .isSameInstanceAs(icon);
    }

    @Test
    void theTypedTokenSurvivesAContainerSwap() {
        field.simulateTypedInput("kotlin");

        field.setContainerDataSource(new com.vaadin.data.util.IndexedContainer());

        assertThat(field.getValue()).containsExactly("kotlin");
        assertThat(field.getTokenCaption("kotlin")).isEqualTo("kotlin");
    }
}
