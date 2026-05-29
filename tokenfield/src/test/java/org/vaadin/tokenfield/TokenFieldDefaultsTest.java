package org.vaadin.tokenfield;

import com.vaadin.ui.CssLayout;
import com.vaadin.ui.HorizontalLayout;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

/**
 * Verifies construction defaults and that every constructor variant produces
 * a correctly initialised field.
 */
class TokenFieldDefaultsTest {

    @Test
    void defaultConstructorCreatesCssLayout() {
        TestTokenField f = new TestTokenField();
        assertThat(f.getInternalLayout()).isInstanceOf(CssLayout.class);
    }

    @Test
    void getTypeReturnsSetClass() {
        TestTokenField f = new TestTokenField();
        assertThat(f.getType()).isEqualTo(Set.class);
    }

    @Test
    void defaultInsertPositionIsBefore() {
        TestTokenField f = new TestTokenField();
        assertThat(f.getTokenInsertPosition()).isEqualTo(TokenField.InsertPosition.BEFORE);
    }

    @Test
    void newTokensAllowedByDefault() {
        TestTokenField f = new TestTokenField();
        assertThat(f.isNewTokensAllowed()).isTrue();
    }

    @Test
    void rememberNewTokensTrueByDefault() {
        TestTokenField f = new TestTokenField();
        assertThat(f.isRememberNewTokens()).isTrue();
    }

    @Test
    void noTokenButtonsInitially() {
        TestTokenField f = new TestTokenField();
        assertThat(f.getTokenButtons()).isEmpty();
    }

    @Test
    void valueIsNullInitially() {
        TestTokenField f = new TestTokenField();
        assertThat(f.getValue()).isNull();
    }

    @Test
    void captionConstructorSetsCaption() {
        TestTokenField f = new TestTokenField("My Tags");
        assertThat(f.getCaption()).isEqualTo("My Tags");
    }

    @Test
    void layoutConstructorUsesProvidedLayout() {
        HorizontalLayout hl = new HorizontalLayout();
        TestTokenField f = new TestTokenField(hl);
        assertThat(f.getInternalLayout()).isSameInstanceAs(hl);
    }

    @Test
    void layoutContainsComboBoxInitially() {
        TestTokenField f = new TestTokenField();
        assertWithMessage("Layout should contain the ComboBox input on construction")
                .that(f.getLayoutComponents()).contains(f.getComboBox());
    }

    @Test
    void notReadOnlyByDefault() {
        TestTokenField f = new TestTokenField();
        assertThat(f.isReadOnly()).isFalse();
    }
}
