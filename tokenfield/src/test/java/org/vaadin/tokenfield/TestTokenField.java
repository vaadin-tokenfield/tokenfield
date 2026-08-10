package org.vaadin.tokenfield;

import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.Layout;
import com.vaadin.ui.UI;

import java.util.ArrayList;
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
     *
     * <p>This invokes the very {@link com.vaadin.ui.AbstractSelect.NewItemHandler}
     * that the {@link TokenField} constructor wires into the ComboBox — the same
     * object {@code ComboBox.changeVariables} calls when the browser sends a
     * {@code newitem} variable. Re-implementing the handler's body here instead
     * would leave the handler itself untested.</p>
     */
    public void simulateNewItemInput(String text) {
        cb.getNewItemHandler().addNewItem(text);
    }

    /**
     * Simulates the delete/backspace key being pressed in an empty input,
     * reaching exactly the code the client-side {@code TokenFieldServerRpc}
     * reaches, without a browser.
     */
    public void simulateDeleteKey() {
        cb.onDelete();
    }

    /**
     * Puts this field into the given UI so that the embedded ComboBox is
     * reachable from {@link UI#getConnectorTracker()}, which is what tests
     * asserting on repaints need.
     *
     * <p>{@link com.vaadin.ui.CustomField} builds its content in
     * {@code attach()}, and Vaadin only calls {@code attach()} once the UI has
     * a {@code VaadinSession} — far more machinery than a unit test wants.
     * Calling {@code getContent()} directly links the same
     * ComboBox → layout → field → UI parent chain that {@code getUI()} walks.</p>
     */
    public void attachTo(UI ui) {
        ui.setContent(this);
        getContent();
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
        for (Component component : layout) {
            result.add(component);
        }
        return result;
    }
}
