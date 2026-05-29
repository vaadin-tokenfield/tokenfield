package org.vaadin.tokenfield;

import com.vaadin.data.Property;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests that simulate user interaction via the embedded ComboBox:
 * <ul>
 *   <li>Selecting an existing container item (dropdown pick)</li>
 *   <li>Typing a new token (NewItemHandler path)</li>
 * </ul>
 */
class TokenFieldUiInputTest {

    private TestTokenField field;

    @BeforeEach
    void setup() {
        field = new TestTokenField();
    }

    // -----------------------------------------------------------------------
    // Simulated dropdown selection (existing item)
    // -----------------------------------------------------------------------

    @Test
    void simulateSelectAddsToken() {
        field.getContainerDataSource().addItem("java");
        field.simulateSelect("java");
        @SuppressWarnings("unchecked")
        Set<Object> val = (Set<Object>) field.getValue();
        assertThat(val).isNotNull();
        assertThat(val).contains("java");
    }

    @Test
    void simulateSelectCreatesButton() {
        field.getContainerDataSource().addItem("kotlin");
        field.simulateSelect("kotlin");
        assertThat(field.getTokenButtons()).containsKey("kotlin");
    }

    @Test
    void simulateSelectDuplicateIsNoOp() {
        field.getContainerDataSource().addItem("scala");
        field.simulateSelect("scala");
        field.simulateSelect("scala");
        assertThat(field.getTokenButtons()).hasSize(1);
    }

    // -----------------------------------------------------------------------
    // Simulated new typed token (NewItemHandler path)
    // -----------------------------------------------------------------------

    @Test
    void simulateNewItemInputAddsToken() {
        field.simulateNewItemInput("newtag");
        @SuppressWarnings("unchecked")
        Set<Object> val = (Set<Object>) field.getValue();
        assertThat(val).isNotNull();
        assertThat(val).contains("newtag");
    }

    @Test
    void simulateNewItemInputWithRememberTrueAddsToContainer() {
        assertWithMessage("rememberNewTokens should default to true")
                .that(field.isRememberNewTokens()).isTrue();
        field.simulateNewItemInput("persisted");
        assertWithMessage("Token should appear in container when rememberNewTokens=true")
                .that(field.getComboBox().containsId("persisted")).isTrue();
    }

    @Test
    void simulateNewItemInputWithRememberFalseDoesNotAddToContainer() {
        field.setRememberNewTokens(false);
        field.simulateNewItemInput("volatile");
        assertWithMessage("Token must not land in container when rememberNewTokens=false")
                .that(field.getComboBox().containsId("volatile")).isFalse();
    }

    @Test
    void simulateNewItemInputOnReadOnlyThrows() {
        field.setReadOnly(true);
        // Truth has no exception-under-test assertion (ExpectFailure captures
        // failures from Truth assertions themselves, not domain exceptions
        // like this one), so JUnit's assertThrows remains the tool here.
        assertThrows(Property.ReadOnlyException.class,
                () -> field.simulateNewItemInput("forbidden"));
    }

    @Test
    void addTokenOnReadOnlyFieldThrows() {
        field.setReadOnly(true);
        assertThrows(Property.ReadOnlyException.class,
                () -> field.addToken("blocked"));
    }
}
