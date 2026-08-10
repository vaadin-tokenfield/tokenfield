package org.vaadin.tokenfield;

import com.vaadin.data.util.IndexedContainer;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.UI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

/**
 * Covers what has to happen to the <em>input</em> when the user types a token
 * that is not in the container and presses Enter — the
 * {@link com.vaadin.ui.AbstractSelect.NewItemHandler} path.
 *
 * <p>The token itself landing in the field is covered by
 * {@link TokenFieldUiInputTest} and {@link TokenFieldRememberTokenTest}. What
 * is pinned here is the other half of that interaction: the typed text has to
 * disappear from the input afterwards (issue #14).</p>
 *
 * <p>Clearing the input is not a server-side value change — the ComboBox never
 * selects the new token, so its value is null before and after. The client
 * widget wipes the text box when it receives a repaint carrying an empty
 * selection ({@code ComboBoxConnector.resetSelection()}), so on the server the
 * observable requirement is "the ComboBox is marked dirty", which is what these
 * tests assert.</p>
 */
class TokenFieldNewTokenInputTest {

    private static final String NAME = "name";
    private static final String TYPED = "urgent";

    /** Minimal UI, only ever used for its {@code ConnectorTracker}. */
    private static class TestUi extends UI {

        private static final long serialVersionUID = 1L;

        @Override
        protected void init(VaadinRequest request) {
            // Nothing to build: the tests attach the field themselves.
        }
    }

    private TestUi ui;
    private TestTokenField field;

    @BeforeEach
    void setup() {
        ui = new TestUi();
        field = new TestTokenField();
        field.attachTo(ui);
        ui.getConnectorTracker().markAllConnectorsClean();
    }

    private boolean inputWillBeRepainted() {
        return ui.getConnectorTracker().isDirty(field.getComboBox());
    }

    // -----------------------------------------------------------------------
    // Issue #14: the input keeps the typed text after a new token is added
    // -----------------------------------------------------------------------

    @Test
    void newTokenClearsTheInputWhenNewTokensAreNotRemembered() {
        field.setRememberNewTokens(false);
        ui.getConnectorTracker().markAllConnectorsClean();

        field.simulateNewItemInput(TYPED);

        assertWithMessage("Adding a new token must repaint the ComboBox, otherwise"
                + " the browser keeps showing the text that was just turned into a token")
                .that(inputWillBeRepainted()).isTrue();
    }

    @Test
    void newTokenClearsTheInputWhenNewTokensAreRemembered() {
        assertWithMessage("rememberNewTokens should default to true")
                .that(field.isRememberNewTokens()).isTrue();

        field.simulateNewItemInput(TYPED);

        assertThat(inputWillBeRepainted()).isTrue();
    }

    @Test
    void newTokenClearsTheInputWhenATokenCaptionPropertyIsUsed() {
        useAddressBookContainer();
        ui.getConnectorTracker().markAllConnectorsClean();

        field.simulateNewItemInput("new@example.com");

        assertThat(inputWillBeRepainted()).isTrue();
    }

    @Test
    void newTokenLeavesTheComboBoxUnselected() {
        field.simulateNewItemInput(TYPED);

        assertWithMessage("The new token becomes a token button, it is never the"
                + " ComboBox's own selection")
                .that(field.getComboBox().getValue()).isNull();
    }

    /**
     * A container shaped like the demo's address book: item ids are email
     * addresses, and the caption shown in the input comes from a separate
     * {@code name} property.
     */
    private void useAddressBookContainer() {
        IndexedContainer contacts = new IndexedContainer();
        contacts.addContainerProperty(NAME, String.class, null);
        field.setContainerDataSource(contacts);
        field.setTokenCaptionPropertyId(NAME);
    }
}
