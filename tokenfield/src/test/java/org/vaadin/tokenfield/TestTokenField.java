package org.vaadin.tokenfield;

import com.vaadin.data.Property;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.Layout;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Test subclass of {@link TokenField} that exposes protected fields and
 * provides simulation helpers for server-side unit tests.
 *
 * <p>Because this class lives in the same package as TokenField, it has direct
 * access to the {@code protected} fields {@code cb}, {@code buttons},
 * {@code layout}, and {@code rememberNewTokens}.</p>
 */
public class TestTokenField extends TokenField {

    public TestTokenField() {
        super();
    }

    public TestTokenField(String caption) {
        super(caption);
    }

    public TestTokenField(Layout lo) {
        super(lo);
    }

    public TestTokenField(String caption, TokenField.InsertPosition insertPosition) {
        super(caption, insertPosition);
    }

    public TestTokenField(String caption, Layout lo) {
        super(caption, lo);
    }

    public TestTokenField(String caption, Layout lo, TokenField.InsertPosition insertPosition) {
        super(caption, lo, insertPosition);
    }

    public TestTokenField(Layout lo, TokenField.InsertPosition insertPosition) {
        super(lo, insertPosition);
    }

    // -----------------------------------------------------------------------
    // Simulation helpers
    // -----------------------------------------------------------------------

    /**
     * Simulates the user selecting an existing item from the ComboBox dropdown.
     * The {@code id} must already be present in the ComboBox container so that
     * {@code AbstractSelect.setValue} accepts it.
     */
    public void simulateSelect(Object id) {
        cb.setValue(id);
    }

    /**
     * Simulates the user typing a new (not-in-container) token and submitting it.
     * Mirrors the {@link com.vaadin.ui.AbstractSelect.NewItemHandler} logic
     * wired in the {@link TokenField} constructor.
     */
    public void simulateNewItemInput(String text) {
        if (isReadOnly()) {
            throw new Property.ReadOnlyException();
        }
        onTokenInput(text);
        if (rememberNewTokens) {
            rememberToken(text);
        }
    }

    /**
     * Simulates the delete/backspace key being pressed in an empty input,
     * reaching exactly the code the client-side {@code TokenFieldServerRpc}
     * reaches, without a browser.
     */
    public void simulateDeleteKey() {
        cb.onDelete();
    }

    /** Simulates {@link TokenField#setLayout(Layout)}, which is protected. */
    public void changeLayout(Layout lo) {
        setLayout(lo);
    }

    // -----------------------------------------------------------------------
    // Accessors for test assertions
    // -----------------------------------------------------------------------

    /** Returns the internal token-id → Button map (insertion-ordered). */
    public Map<Object, Button> getTokenButtons() {
        return buttons;
    }

    /** Returns the embedded ComboBox for container and state assertions. */
    public TokenComboBox getComboBox() {
        return cb;
    }

    /** Returns the current layout for component-order assertions. */
    public Layout getInternalLayout() {
        return layout;
    }

    /** Returns all layout components in their current order. */
    public List<Component> getLayoutComponents() {
        List<Component> result = new ArrayList<Component>();
        Iterator<Component> it = layout.getComponentIterator();
        while (it.hasNext()) {
            result.add(it.next());
        }
        return result;
    }
}
