package org.vaadin.tokenfield;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;

import com.vaadin.data.util.IndexedContainer;

/**
 * Tests {@link TokenField#rememberToken(String)} via the NewItemHandler path
 * ({@link TestTokenField#simulateNewItemInput(String)}).
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

    /**
     * The new item is added under the typed text itself, so the caption
     * property write - keyed by that same text - always finds the item it
     * just created.
     */
    @Test
    void captionPropertyIsSetUnderTheTypedTextAsTheItemId() {
        IndexedContainer c = new IndexedContainer();
        c.addContainerProperty("name", String.class, null);
        field.setContainerDataSource(c);
        field.setTokenCaptionPropertyId("name");

        field.simulateNewItemInput("tag1");

        assertThat(c.getContainerProperty("tag1", "name").getValue())
                .isEqualTo("tag1");
    }
}
