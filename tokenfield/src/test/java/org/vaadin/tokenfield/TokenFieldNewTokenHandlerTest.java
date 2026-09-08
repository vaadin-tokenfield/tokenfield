package org.vaadin.tokenfield;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.data.Property;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.vaadin.tokenfield.TokenField.DefaultNewTokenHandler;
import org.vaadin.tokenfield.TokenField.NewTokenHandler;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests {@link TokenField.NewTokenHandler}, the counterpart of
 * {@code AbstractSelect.NewItemHandler}: what happens to text the user typed
 * that matched no suggestion.
 *
 * <p>Reference: {@code AbstractSelect.DefaultNewItemHandler} adds the item to
 * the container first and selects it afterwards; a custom handler creates the
 * item through the container's own API and selects what it created.</p>
 */
class TokenFieldNewTokenHandlerTest {

    private TestTokenField field;
    private final List<String> handled = new ArrayList<>();

    @BeforeEach
    void setup() {
        field = new TestTokenField();
    }

    @Test
    void theDefaultHandlerIsInstalled() {
        assertThat(field.getNewTokenHandler())
                .isInstanceOf(DefaultNewTokenHandler.class);
    }

    @Test
    void settingNullRestoresTheDefault() {
        field.setNewTokenHandler(handled::add);
        field.setNewTokenHandler(null);
        assertThat(field.getNewTokenHandler())
                .isInstanceOf(DefaultNewTokenHandler.class);
    }

    // ------------------------------------------------------------------
    // Default handler: container first, then the token
    // ------------------------------------------------------------------

    @Test
    void theDefaultHandlerPutsTheTextInTheContainerBeforeOnTokenInput() {
        List<Boolean> inContainerOnInput = new ArrayList<>();
        TestTokenField observing = new TestTokenField() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onTokenInput(Object tokenId) {
                inContainerOnInput.add(getComboBox().containsId(tokenId));
                super.onTokenInput(tokenId);
            }
        };

        observing.simulateNewItemInput("tag1");

        assertWithMessage("DefaultNewItemHandler order: addItem, then select")
                .that(inContainerOnInput).containsExactly(true);
        assertThat(observing.getValue()).containsExactly("tag1");
    }

    @Test
    void inObliviousModeTheDefaultHandlerLeavesTheContainerAlone() {
        field.setRememberNewTokens(false);

        field.simulateNewItemInput("tag1");

        assertThat(field.getValue()).containsExactly("tag1");
        assertThat(field.getComboBox().containsId("tag1")).isFalse();
    }

    @Test
    void aLegacyRememberTokenOverrideIsStillUsedByTheDefaultHandler() {
        List<String> remembered = new ArrayList<>();
        TestTokenField legacy = new TestTokenField() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void rememberToken(String tokenId) {
                remembered.add(tokenId);
                super.rememberToken(tokenId);
            }
        };

        legacy.simulateNewItemInput("tag1");

        assertThat(remembered).containsExactly("tag1");
        assertThat(legacy.getComboBox().containsId("tag1")).isTrue();
    }

    // ------------------------------------------------------------------
    // Custom handler
    // ------------------------------------------------------------------

    @Test
    void aCustomHandlerReceivesTheTypedTextAndNothingElseHappens() {
        field.setNewTokenHandler(handled::add);

        field.simulateNewItemInput("typed");

        assertThat(handled).containsExactly("typed");
        assertThat(field.getValue()).isNull();
        assertThat(field.getComboBox().containsId("typed")).isFalse();
    }

    @Test
    void aCustomHandlerIsNotAskedForAPickedSuggestion() {
        field.setNewTokenHandler(handled::add);
        field.getContainerDataSource().addItem("java");

        field.simulateSelect("java");

        assertThat(handled).isEmpty();
        assertThat(field.getValue()).containsExactly("java");
    }

    @Test
    void aCustomHandlerCanCreateTheItemUnderAGeneratedId() {
        GeneratedIdContainer c = new GeneratedIdContainer();
        field.setContainerDataSource(c);
        field.setTokenCaptionPropertyId(GeneratedIdContainer.NAME);
        field.setNewTokenHandler(text -> {
            Object id = c.addItem();
            c.getContainerProperty(id, GeneratedIdContainer.NAME).setValue(text);
            field.addToken(id);
        });

        field.simulateNewItemInput("new@example.com");

        assertThat(field.getValue()).containsExactly(1L);
        assertThat(c.getItemIds()).containsExactly(1L);
        assertThat(field.getTokenCaption(1L)).isEqualTo("new@example.com");
    }

    @Test
    void aCustomHandlerMayDeferAndAddTheTokenLater() {
        field.setNewTokenHandler(handled::add);
        field.simulateNewItemInput("later");
        assertThat(field.getValue()).isNull();

        field.addToken(handled.get(0));

        assertThat(field.getValue()).containsExactly("later");
    }

    @Test
    void aReadOnlyFieldRejectsTypedTextBeforeAnyHandlerSeesIt() {
        field.setNewTokenHandler(handled::add);
        field.setReadOnly(true);

        assertThrows(Property.ReadOnlyException.class,
                () -> field.simulateNewItemInput("forbidden"));
        assertThat(handled).isEmpty();
    }

    @Test
    void theHandlerIsSerializable() {
        NewTokenHandler handler = field.getNewTokenHandler();
        assertThat(handler).isInstanceOf(java.io.Serializable.class);
    }
}
