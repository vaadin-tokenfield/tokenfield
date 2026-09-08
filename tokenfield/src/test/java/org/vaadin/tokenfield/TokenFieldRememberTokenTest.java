package org.vaadin.tokenfield;

import com.vaadin.data.util.IndexedContainer;
import com.vaadin.ui.AbstractSelect.ItemCaptionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests {@link TokenField#rememberToken(String)} via the NewItemHandler path
 * ({@link TestTokenField#simulateNewItemInput(String)}): the Writeable mode
 * over a container that takes the typed text as the item id.
 *
 * <p>Reference: {@code AbstractSelect.DefaultNewItemHandler} adds the item
 * under the typed text and writes the caption property under that same key,
 * whatever the caption mode says (issue #39).</p>
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
    void theCaptionPropertyIsFilledUnderTheIdTheItemWasAddedWith() {
        IndexedContainer c = withNameProperty();
        field.setTokenCaptionPropertyId("name");
        // A caption that differs from the id used to break this path
        field.setTokenCaption("tag1", "Unrelated");

        field.simulateNewItemInput("tag1");

        assertThat(field.getComboBox().containsId("tag1")).isTrue();
        assertThat(c.getItem("tag1").getItemProperty("name").getValue())
                .isEqualTo("tag1");
    }

    // ------------------------------------------------------------------
    // #39: the item id is the typed text, whatever the caption says
    // ------------------------------------------------------------------

    @Test
    void anExplicitCaptionDoesNotChangeTheItemId() {
        field.setTokenCaption("tag1", "Unrelated");

        field.simulateNewItemInput("tag1");

        assertThat(field.getComboBox().getItemIds()).containsExactly("tag1");
    }

    @Test
    void indexCaptionModeDoesNotChangeTheItemId() {
        field.setTokenCaptionMode(ItemCaptionMode.INDEX);

        field.simulateNewItemInput("tag1");

        assertThat(field.getComboBox().getItemIds()).containsExactly("tag1");
    }

    @Test
    void iconOnlyCaptionModeDoesNotChangeTheItemId() {
        field.setTokenCaptionMode(ItemCaptionMode.ICON_ONLY);

        field.simulateNewItemInput("tag1");

        assertThat(field.getComboBox().getItemIds()).containsExactly("tag1");
    }

    @Test
    void aCaptionPropertyIsWrittenUnderTheItemIdInExplicitMode() {
        IndexedContainer c = withNameProperty();
        field.setTokenCaptionPropertyId("name"); // switches to PROPERTY
        field.setTokenCaptionMode(ItemCaptionMode.EXPLICIT_DEFAULTS_ID);
        field.setTokenCaption("tag1", "Unrelated");

        field.simulateNewItemInput("tag1");

        assertThat(field.getComboBox().getItemIds()).containsExactly("tag1");
        assertThat(c.getItem("tag1").getItemProperty("name").getValue())
                .isEqualTo("tag1");
    }

    // ------------------------------------------------------------------
    // A container that assigns its own ids
    // ------------------------------------------------------------------

    @Test
    void aContainerThatRefusesExplicitIdsSaysSo() {
        field.setContainerDataSource(new GeneratedIdContainer());

        assertWithMessage("No silent fallback: such a container needs a"
                + " NewTokenHandler, or setRememberNewTokens(false)")
                .that(assertThrows(UnsupportedOperationException.class,
                        () -> field.simulateNewItemInput("new@example.com")))
                .isNotNull();
    }

    private IndexedContainer withNameProperty() {
        IndexedContainer c = new IndexedContainer();
        c.addContainerProperty("name", String.class, null);
        field.setContainerDataSource(c);
        return c;
    }
}
