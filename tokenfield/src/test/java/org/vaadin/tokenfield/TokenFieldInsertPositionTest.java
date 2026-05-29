package org.vaadin.tokenfield;

import com.vaadin.ui.Component;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

/**
 * Verifies that token buttons and the ComboBox input are placed in the correct
 * layout order for both {@link TokenField.InsertPosition#BEFORE} and
 * {@link TokenField.InsertPosition#AFTER}.
 */
class TokenFieldInsertPositionTest {

    @Test
    void defaultPositionBeforePutsTokenButtonsBeforeCb() {
        TestTokenField f = new TestTokenField();
        assertThat(f.getTokenInsertPosition()).isEqualTo(TokenField.InsertPosition.BEFORE);

        f.addToken("t1");
        List<Component> comps = f.getLayoutComponents();
        int cbIndex = comps.indexOf(f.getComboBox());
        int btnIndex = comps.indexOf(f.getTokenButtons().get("t1"));
        assertWithMessage("Token button must be in layout").that(btnIndex).isAtLeast(0);
        assertWithMessage("With BEFORE position, token button should appear before the ComboBox")
                .that(btnIndex).isLessThan(cbIndex);
    }

    @Test
    void afterPositionPutsCbBeforeTokenButtons() {
        TestTokenField f = new TestTokenField();
        f.setTokenInsertPosition(TokenField.InsertPosition.AFTER);

        f.addToken("t1");
        List<Component> comps = f.getLayoutComponents();
        int cbIndex = comps.indexOf(f.getComboBox());
        int btnIndex = comps.indexOf(f.getTokenButtons().get("t1"));
        assertThat(cbIndex).isAtLeast(0);
        assertThat(btnIndex).isAtLeast(0);
        assertWithMessage("With AFTER position, ComboBox should appear before the token button")
                .that(cbIndex).isLessThan(btnIndex);
    }

    @Test
    void switchingPositionRebuildsMaintainsAllComponents() {
        TestTokenField f = new TestTokenField();
        f.addToken("a");
        f.addToken("b");
        f.setTokenInsertPosition(TokenField.InsertPosition.AFTER);

        List<Component> comps = f.getLayoutComponents();
        // cb + 2 buttons = 3 components
        assertWithMessage("After rebuild, layout must contain exactly 3 components (cb + 2 buttons)")
                .that(comps).hasSize(3);
        assertThat(comps).contains(f.getComboBox());
        assertThat(comps).contains(f.getTokenButtons().get("a"));
        assertThat(comps).contains(f.getTokenButtons().get("b"));
    }

    @Test
    void multipleTokensBeforeAreAllBeforeCb() {
        TestTokenField f = new TestTokenField();
        f.addToken("first");
        f.addToken("second");
        List<Component> comps = f.getLayoutComponents();
        int cbIndex = comps.indexOf(f.getComboBox());
        assertThat(comps.indexOf(f.getTokenButtons().get("first"))).isLessThan(cbIndex);
        assertThat(comps.indexOf(f.getTokenButtons().get("second"))).isLessThan(cbIndex);
    }

    @Test
    void switchingPositionBack() {
        TestTokenField f = new TestTokenField();
        f.addToken("x");
        f.setTokenInsertPosition(TokenField.InsertPosition.AFTER);
        f.setTokenInsertPosition(TokenField.InsertPosition.BEFORE);

        List<Component> comps = f.getLayoutComponents();
        int cbIndex = comps.indexOf(f.getComboBox());
        int btnIndex = comps.indexOf(f.getTokenButtons().get("x"));
        assertWithMessage("After switching back to BEFORE, button precedes cb")
                .that(btnIndex).isLessThan(cbIndex);
    }
}
