package org.vaadin.tokenfield;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;

/**
 * Tests {@link TokenField#rememberToken(String)} via the NewItemHandler path
 * ({@link TestTokenField#simulateNewItemInput(String)}), restricted to the
 * default configuration (no {@code tokenCaptionPropertyId}). That property is
 * intentionally out of scope here: {@code rememberToken} adds the new item
 * under its <em>caption</em> but writes the caption property under the
 * original <em>id</em>, which throws when caption and id diverge — see
 * {@code TokenField#rememberToken(String)}.
 */
class TokenFieldRememberTokenTest {

    private TestTokenField field;

    @BeforeEach
    void setup() {
        field = new TestTokenField();
    }

    @Test
    void newTokenIsAddedToContainer() {
        field.simulateNewItemInput("tag1");
        assertThat(field.getComboBox().containsId("tag1")).isTrue();
    }

    @Test
    void existingTokenIsNotDuplicated() {
        field.getContainerDataSource().addItem("dup");
        assertThat(field.getComboBox().getItemIds()).hasSize(1);
        field.simulateNewItemInput("dup");
        assertThat(field.getComboBox().getItemIds()).hasSize(1);
    }

    @Test
    void rememberNewTokensFalseSkipsContainer() {
        field.setRememberNewTokens(false);
        field.simulateNewItemInput("volatile");
        assertThat(field.getComboBox().containsId("volatile")).isFalse();
    }
}
