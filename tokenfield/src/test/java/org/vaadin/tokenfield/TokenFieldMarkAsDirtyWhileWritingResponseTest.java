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
 * Covers {@code IllegalStateException: A connector should not be marked as
 * dirty while a response is being written} — issue #15 — and the deferral that
 * now keeps TokenField out of it.
 *
 * <p>Vaadin throws from {@code ConnectorTracker.markDirty}, which refuses a
 * newly dirtied connector once {@code UidlWriter} has started writing the
 * response ({@code ConnectorTracker.java:503-506}). Everything a component
 * paints happens inside that window.</p>
 *
 * <p>The route in is {@link ComboBox#paintContent}: in
 * {@code ITEM_CAPTION_MODE_PROPERTY}, with something typed, it filters through
 * the container rather than in memory and applies that filter from inside the
 * paint ({@code ComboBox.getOptionsWithFilter} → {@code addContainerFilter}).
 * A container reports a filter change as an item set change, so every
 * application listener on that container runs mid-paint — and a listener that
 * edits the field used to kill the request. None of this is specific to a
 * container implementation: {@link IndexedContainer} is used here because it
 * ships with {@code vaadin-server}, but a {@code JPAContainer} behaved
 * identically.</p>
 *
 * <p>TokenField now holds such a change back and applies it from
 * {@code beforeClientResponse}, which runs before the next response is
 * written. That is the only option available to it: both halves of a token
 * change mark a connector dirty unconditionally — {@code AbstractField}
 * ends every value change with {@code markAsDirty()}
 * ({@code AbstractField.fireValueChange}), and every layout edit marks the
 * layout dirty ({@code AbstractComponentContainer.addComponent}/
 * {@code removeComponent}) — so neither can simply be made safe in place.</p>
 *
 * <p>The cost is that the change lands one response later, and the tests below
 * pin that as the contract rather than hiding it.</p>
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
     * The user types, and the request survives.
     *
     * <p>The application here keeps itself in sync with the container it handed
     * to the TokenField, which is ordinary enough. The filter the ComboBox
     * applies while painting reaches that listener as an item set change, and
     * the listener adds a token — from inside the response. TokenField holds
     * that change back rather than dying on it, and applies it at the start of
     * the next response.</p>
     */
    @Test
    void typingWithAContainerListenerAddsTheTokenOnTheNextResponse() throws PaintException {
        contacts.addItemSetChangeListener(event -> field.addToken(firstContact));

        typeIntoSuggestionBox("Ein");
        paintAsUidlWriterWould();

        // Held back: the value cannot change while the response is written.
        assertThat(field.getTokenButtons()).isEmpty();

        field.beforeClientResponse(false);

        assertThat(field.getTokenButtons()).containsKey(firstContact);
        assertThat(field.getValue()).contains(firstContact);
        assertThat(field.getLayoutComponents()).hasSize(2);
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
    void addTokenIsDeferredWhileTheResponseIsWritten() {
        whileWritingResponse(() -> field.addToken(firstContact));
        assertThat(field.getTokenButtons()).isEmpty();

        field.beforeClientResponse(false);

        assertThat(field.getTokenButtons()).containsKey(firstContact);
    }

    @Test
    void removeTokenIsDeferredWhileTheResponseIsWritten() {
        field.addToken(firstContact);

        whileWritingResponse(() -> field.removeToken(firstContact));
        assertThat(field.getTokenButtons()).containsKey(firstContact);

        field.beforeClientResponse(false);

        assertThat(field.getTokenButtons()).isEmpty();
    }

    /** Successive changes coalesce into the last one, not a backlog. */
    @Test
    void repeatedTokenChangesWhileWritingCoalesce() {
        whileWritingResponse(() -> {
            field.addToken(firstContact);
            field.addToken(contacts.getIdByIndex(1));
            field.removeToken(firstContact);
        });

        field.beforeClientResponse(false);

        assertThat(field.getTokenButtons().keySet())
                .containsExactly(contacts.getIdByIndex(1));
    }

    @Test
    void setReadOnlyIsDeferredWhileTheResponseIsWritten() {
        whileWritingResponse(() -> field.setReadOnly(true));

        field.beforeClientResponse(false);

        // Read-only hides the input, leaving the (empty) token list behind.
        assertThat(field.getLayoutComponents()).isEmpty();
    }

    @Test
    void setTokenInsertPositionIsDeferredWhileTheResponseIsWritten() {
        field.addToken(firstContact);

        whileWritingResponse(
                () -> field.setTokenInsertPosition(TokenField.InsertPosition.AFTER));

        field.beforeClientResponse(false);

        assertThat(field.getTokenInsertPosition())
                .isEqualTo(TokenField.InsertPosition.AFTER);
        assertThat(field.getLayoutComponents().get(0))
                .isEqualTo(field.getComboBox());
    }

    /**
     * Not everything can be rescued. {@code setInputPrompt} is ComboBox's own
     * setter, and it marks dirty by hand ({@code ComboBox.java:180-181})
     * instead of writing through {@code getState()} — there is no seam for this
     * component to defer it. Pinned so the remaining gap is explicit rather
     * than discovered.
     */
    @Test
    void setInputPromptStillThrowsWhileTheResponseIsWritten() {
        assertThrowsWhileWritingResponse(() -> field.setInputPrompt("Start typing"));
    }

    /**
     * {@code setInputPrompt} is not a layout edit, and still throws: ComboBox
     * calls {@code markAsDirty()} by hand for it ({@code ComboBox.java:180-181})
     * instead of writing through {@code getState()}. Pinned because it shows the
     * hazard is the unguarded call, not the layout rebuild.
     */
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

    private void whileWritingResponse(Runnable operation) {
        ui.startWritingResponse();
        try {
            operation.run();
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
