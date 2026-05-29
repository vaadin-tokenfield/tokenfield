package org.vaadin.tokenfield;

import com.vaadin.shared.ui.combobox.FilteringMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Tests that getter/setter pairs on {@link TokenField} correctly delegate
 * to (or are consistent with) the embedded ComboBox: new-tokens-allowed,
 * input prompt, filtering mode, tab index, and rememberNewTokens.
 */
class TokenFieldDelegationTest {

    private TestTokenField field;

    @BeforeEach
    void setup() {
        field = new TestTokenField();
    }

    // -----------------------------------------------------------------------
    // New tokens allowed
    // -----------------------------------------------------------------------

    @Test
    void setNewTokensAllowedFalseReflectedByGetter() {
        field.setNewTokensAllowed(false);
        assertThat(field.isNewTokensAllowed()).isFalse();
    }

    @Test
    void setNewTokensAllowedFalseDelegatesToComboBox() {
        field.setNewTokensAllowed(false);
        assertThat(field.getComboBox().isNewItemsAllowed()).isFalse();
    }

    @Test
    void setNewTokensAllowedTrueReflectedByGetter() {
        field.setNewTokensAllowed(false);
        field.setNewTokensAllowed(true);
        assertThat(field.isNewTokensAllowed()).isTrue();
    }

    // -----------------------------------------------------------------------
    // Input prompt
    // -----------------------------------------------------------------------

    @Test
    void setInputPromptReflectedByGetter() {
        field.setInputPrompt("Add tags...");
        assertThat(field.getInputPrompt()).isEqualTo("Add tags...");
    }

    @Test
    void setInputPromptDelegatesToComboBox() {
        field.setInputPrompt("Type here");
        assertThat(field.getComboBox().getInputPrompt()).isEqualTo("Type here");
    }

    @Test
    void defaultInputPromptIsNull() {
        assertThat(field.getInputPrompt()).isNull();
    }

    // -----------------------------------------------------------------------
    // Filtering mode
    // -----------------------------------------------------------------------

    @Test
    void setFilteringModeReflectedByGetter() {
        field.setFilteringMode(FilteringMode.CONTAINS);
        assertThat(field.getFilteringMode()).isEqualTo(FilteringMode.CONTAINS);
    }

    @Test
    void setFilteringModeDelegatesToComboBox() {
        field.setFilteringMode(FilteringMode.CONTAINS);
        assertThat(field.getComboBox().getFilteringMode()).isEqualTo(FilteringMode.CONTAINS);
    }

    // -----------------------------------------------------------------------
    // Tab index
    // -----------------------------------------------------------------------

    @Test
    void setTabIndexReflectedByGetter() {
        field.setTabIndex(5);
        assertThat(field.getTabIndex()).isEqualTo(5);
    }

    @Test
    void setTabIndexDelegatesToComboBox() {
        field.setTabIndex(3);
        assertThat(field.getComboBox().getTabIndex()).isEqualTo(3);
    }

    // -----------------------------------------------------------------------
    // Remember new tokens
    // -----------------------------------------------------------------------

    @Test
    void setRememberNewTokensFalse() {
        field.setRememberNewTokens(false);
        assertThat(field.isRememberNewTokens()).isFalse();
    }

    @Test
    void setRememberNewTokensTrueRoundTrips() {
        field.setRememberNewTokens(false);
        field.setRememberNewTokens(true);
        assertThat(field.isRememberNewTokens()).isTrue();
    }

    // -----------------------------------------------------------------------
    // Focus
    // -----------------------------------------------------------------------

    @Test
    void focusDoesNotThrowWithNoUiAttached() {
        // Truth has no exception-under-test assertion; JUnit's remains the
        // idiomatic tool for control-flow checks like this one.
        assertDoesNotThrow(() -> field.focus());
    }
}
