package org.vaadin.tokenfield;

import com.vaadin.data.util.IndexedContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;

/**
 * Tests {@link TokenField#rememberToken(String)} via the NewItemHandler path
 * ({@link TestTokenField#simulateNewItemInput(String)}), with and without a
 * {@code tokenCaptionPropertyId}.
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
    void newTokenIsAddedUnderItsIdAndFillsTheCaptionProperty() {
        IndexedContainer c = new IndexedContainer();
        c.addContainerProperty("name", String.class, null);
        field.setContainerDataSource(c);
        field.setTokenCaptionPropertyId("name");
        // A caption that differs from the id used to break this path
        field.setTokenCaption("tag1", "Unrelated");

        field.simulateNewItemInput("tag1");

        assertThat(field.getComboBox().containsId("tag1")).isTrue();
        assertThat(c.getItem("tag1").getItemProperty("name").getValue())
                .isEqualTo("tag1");
    }

    @Test
    void rememberNewTokensFalseSkipsContainer() {
        field.setRememberNewTokens(false);
        field.simulateNewItemInput("volatile");
        assertThat(field.getComboBox().containsId("volatile")).isFalse();
    }
}
