package org.vaadin.tokenfield;

import com.vaadin.data.util.IndexedContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

/**
 * Tests {@link TokenField#rememberToken(String)} via the NewItemHandler path
 * ({@link TestTokenField#simulateNewItemInput(String)}), including the
 * {@code tokenCaptionPropertyId} configuration: the new item is keyed by its
 * <em>id</em>, which is also what the caption property is written under.
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

    @Test
    void newTokenIsKeyedByItsIdNotByItsCaption() {
        field.setTokenCaption("new@example.com", "New Guy");
        field.simulateNewItemInput("new@example.com");

        assertWithMessage("The container item must be keyed by the token id")
                .that(field.getComboBox().getItemIds())
                .containsExactly("new@example.com");
    }

    @Test
    void newTokenGetsItsCaptionPropertyWritten() {
        IndexedContainer c = new IndexedContainer();
        c.addContainerProperty("name", String.class, null);
        field.setContainerDataSource(c);
        field.setTokenCaptionPropertyId("name");

        field.simulateNewItemInput("tag1");

        assertThat(c.getContainerProperty("tag1", "name").getValue()).isEqualTo("tag1");
        assertThat(field.getTokenButtons().get("tag1").getCaption()).contains("tag1");
    }

    @Test
    void newTokenIsNotWrittenToAContainerThatRejectsIt() {
        // addItem returns null for a container that refuses the id; the caption
        // property must not be written in that case.
        IndexedContainer c = new IndexedContainer();
        c.addContainerProperty("name", String.class, null);
        c.addItem("taken");
        field.setContainerDataSource(c);
        field.setTokenCaptionPropertyId("name");

        field.simulateNewItemInput("taken");

        assertThat(c.getContainerProperty("taken", "name").getValue()).isNull();
    }
}
