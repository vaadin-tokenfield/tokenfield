package org.vaadin.tokenfield;

import com.vaadin.data.Container;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.server.PaintException;
import com.vaadin.ui.ComboBox;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Reproduces {@code IllegalStateException: A connector should not be marked as
 * dirty while a response is being written} from TokenField.
 *
 * <p>Vaadin throws it from {@code ConnectorTracker.markDirty}, which refuses to
 * accept a newly dirtied connector once {@code UidlWriter} has started writing
 * the response ({@code ConnectorTracker.java:503-506}). Everything a component
 * paints happens inside that window, so anything that dirties a connector while
 * painting hits it.</p>
 *
 * <p>TokenField reaches it because every token it adds or removes edits its own
 * layout, and detaching a component calls {@code markAsDirty()} directly rather
 * than through the {@code getState()} accessor Vaadin guards with
 * {@code isWritingResponse()} ({@code AbstractComponent.setParent} →
 * {@code AbstractClientConnector.markAsDirty}, versus the
 * {@code !isWritingResponse()} check in
 * {@code AbstractClientConnector.getState(boolean)}). So a token change is
 * fatal during a paint, while a caption or a prompt change is silently
 * dropped.</p>
 *
 * <p>The route in is {@link ComboBox#paintContent}: when the ComboBox is in
 * {@code ITEM_CAPTION_MODE_PROPERTY} and the user has typed something, it
 * filters through the container rather than in memory, and applies that filter
 * from inside the paint ({@code ComboBox.getOptionsWithFilter} →
 * {@code addContainerFilter}). Containers announce a filter change as an item
 * set change, so every application listener on that container runs mid-paint —
 * and if one of them touches the TokenField, the request dies.</p>
 *
 * <p>None of this is specific to a container implementation:
 * {@link IndexedContainer} is used here because it is in {@code vaadin-server},
 * but a {@code JPAContainer} fails identically.</p>
 */
class TokenFieldMarkAsDirtyWhileWritingResponseTest {

    private static final String CAPTION_PROPERTY = "name";

    private static final String EXPECTED_MESSAGE =
            "A connector should not be marked as dirty while a response is being written.";

    private TestUi ui;
    private TestTokenField field;
    private IndexedContainer contacts;
    private Object firstContact;

    @BeforeEach
    void setup() {
        contacts = new IndexedContainer();
        contacts.addContainerProperty(CAPTION_PROPERTY, String.class, null);
        firstContact = addContact("Nathan Einstein");
        addContact("Nicole Beck");
        addContact("Joe Black");

        field = new TestTokenField();
        field.setContainerDataSource(contacts);
        // Also puts the ComboBox into ITEM_CAPTION_MODE_PROPERTY, which is what
        // makes it filter through the container instead of in memory.
        field.setTokenCaptionPropertyId(CAPTION_PROPERTY);

        ui = new TestUi();
        ui.setContent(field);
    }

    private Object addContact(String name) {
        Object id = contacts.addItem();
        contacts.getContainerProperty(id, CAPTION_PROPERTY).setValue(name);
        return id;
    }

    // ------------------------------------------------------------------
    // The reproduction
    // ------------------------------------------------------------------

    /**
     * The user types, and the request fails server side.
     *
     * <p>The application here keeps itself in sync with the container it handed
     * to the TokenField, which is ordinary enough. The filter the ComboBox
     * applies while painting reaches that listener as an item set change, the
     * listener adds a token, and adding a token edits a layout that Vaadin is
     * in the middle of serialising.</p>
     */
    @Test
    void typingThrowsWhenAContainerListenerAddsAToken() {
        contacts.addItemSetChangeListener(event -> field.addToken(firstContact));

        typeIntoSuggestionBox("Ein");

        IllegalStateException thrown = assertThrows(IllegalStateException.class, this::paintAsUidlWriterWould);
        assertThat(thrown).hasMessageThat().isEqualTo(EXPECTED_MESSAGE);
    }

    /** The same listener is harmless when nothing is being written. */
    @Test
    void theSameListenerIsHarmlessOutsideAResponse() throws PaintException {
        contacts.addItemSetChangeListener(event -> field.addToken(firstContact));

        typeIntoSuggestionBox("Ein");
        field.getComboBox().paintContent(new NoopPaintTarget());

        assertThat(field.getTokenButtons()).containsKey(firstContact);
    }

    /** Without such a listener, painting a filtered TokenField is fine. */
    @Test
    void typingAloneDoesNotThrow() throws PaintException {
        typeIntoSuggestionBox("Ein");

        paintAsUidlWriterWould();

        assertThat(field.getTokenButtons()).isEmpty();
    }

    // ------------------------------------------------------------------
    // The underlying rule: which TokenField operations survive a paint
    // ------------------------------------------------------------------

    /*
     * One operation per test: the first failure leaves the layout half-edited
     * (the ComboBox detached from the UI), and a detached connector no longer
     * marks anything dirty, so batching these would only prove the first one.
     */

    @Test
    void addTokenThrowsWhileTheResponseIsWritten() {
        assertThrowsWhileWritingResponse(() -> field.addToken(firstContact));
    }

    @Test
    void removeTokenThrowsWhileTheResponseIsWritten() {
        field.addToken(firstContact);
        assertThrowsWhileWritingResponse(() -> field.removeToken(firstContact));
    }

    @Test
    void setReadOnlyThrowsWhileTheResponseIsWritten() {
        assertThrowsWhileWritingResponse(() -> field.setReadOnly(true));
    }

    @Test
    void setTokenInsertPositionThrowsWhileTheResponseIsWritten() {
        assertThrowsWhileWritingResponse(
                () -> field.setTokenInsertPosition(TokenField.InsertPosition.AFTER));
    }

    /**
     * {@code setInputPrompt} is not a layout edit, and still throws: ComboBox
     * calls {@code markAsDirty()} by hand for it ({@code ComboBox.java:180-181})
     * instead of writing through {@code getState()}. Pinned because it shows the
     * hazard is the unguarded call, not the layout rebuild.
     */
    @Test
    void setInputPromptThrowsWhileTheResponseIsWritten() {
        assertThrowsWhileWritingResponse(() -> field.setInputPrompt("Start typing"));
    }

    /**
     * By contrast, a setter that only writes shared state is safe: Vaadin
     * guards that path itself, dropping the dirty mark instead of throwing
     * ({@code AbstractClientConnector.getState(boolean)}).
     */
    @Test
    void sharedStateChangesAreToleratedWhileTheResponseIsWritten() {
        ui.startWritingResponse();
        try {
            field.setCaption("Add contact");
        } finally {
            ui.finishWritingResponse();
        }

        assertThat(field.getCaption()).isEqualTo("Add contact");
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * Simulates the user typing into the suggestion box, which reaches the
     * server as the ComboBox's {@code filter} variable.
     */
    private void typeIntoSuggestionBox(String text) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("filter", text);
        variables.put("page", 0);
        ComboBox suggestions = field.getComboBox();
        suggestions.changeVariables(suggestions, variables);
    }

    /**
     * Paints the suggestion box the way {@code UidlWriter} does: with the
     * connector tracker already writing the response.
     */
    private void paintAsUidlWriterWould() throws PaintException {
        ui.startWritingResponse();
        try {
            field.getComboBox().paintContent(new NoopPaintTarget());
        } finally {
            ui.finishWritingResponse();
        }
    }

    private void assertThrowsWhileWritingResponse(Runnable operation) {
        ui.startWritingResponse();
        try {
            IllegalStateException thrown = assertThrows(IllegalStateException.class, operation::run);
            assertThat(thrown).hasMessageThat().isEqualTo(EXPECTED_MESSAGE);
        } finally {
            ui.finishWritingResponse();
        }
    }

    /** Guards the assumption that Vaadin still filters through the container. */
    @Test
    void theSuggestionBoxFiltersThroughTheContainer() {
        assertThat(contacts).isInstanceOf(Container.Filterable.class);
        assertThat(contacts).isInstanceOf(Container.Indexed.class);
        assertThat(field.getComboBox().getItemCaptionMode())
                .isEqualTo(com.vaadin.ui.AbstractSelect.ItemCaptionMode.PROPERTY);
    }
}
