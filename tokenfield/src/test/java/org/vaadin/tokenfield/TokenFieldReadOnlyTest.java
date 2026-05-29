package org.vaadin.tokenfield;

import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

/**
 * Tests for read-only mode: ComboBox visibility, button state, and
 * toggling back to editable.
 */
class TokenFieldReadOnlyTest {

    @Test
    void setReadOnlyTrueRemovesCbFromLayout() {
        TestTokenField f = new TestTokenField();
        f.setReadOnly(true);
        List<Component> comps = f.getLayoutComponents();
        assertWithMessage("ComboBox must be removed from layout when read-only is set")
                .that(comps).doesNotContain(f.getComboBox());
    }

    @Test
    void setReadOnlyFalseRestoresCbToLayout() {
        TestTokenField f = new TestTokenField();
        f.setReadOnly(true);
        f.setReadOnly(false);
        assertWithMessage("ComboBox must reappear in layout after read-only is cleared")
                .that(f.getLayoutComponents()).contains(f.getComboBox());
    }

    @Test
    void setReadOnlyTrueMakesButtonsReadOnly() {
        TestTokenField f = new TestTokenField();
        f.addToken("t1");
        f.addToken("t2");
        f.setReadOnly(true);
        for (Button b : f.getTokenButtons().values()) {
            assertWithMessage("All token buttons must be read-only").that(b.isReadOnly()).isTrue();
        }
    }

    @Test
    void setReadOnlyFalseMakesButtonsWritable() {
        TestTokenField f = new TestTokenField();
        f.addToken("t1");
        f.setReadOnly(true);
        f.setReadOnly(false);
        for (Button b : f.getTokenButtons().values()) {
            assertWithMessage("Buttons must be writable after read-only is cleared")
                    .that(b.isReadOnly()).isFalse();
        }
    }

    @Test
    void toggleReadOnlyDoesNotDuplicateButtons() {
        TestTokenField f = new TestTokenField();
        f.addToken("a");
        f.addToken("b");
        f.setReadOnly(true);
        f.setReadOnly(false);
        assertWithMessage("Toggling read-only must not create duplicate token buttons")
                .that(f.getTokenButtons()).hasSize(2);
        // cb + 2 buttons
        assertThat(f.getLayoutComponents()).hasSize(3);
    }

    @Test
    void readOnlyNoopWhenAlreadyReadOnly() {
        TestTokenField f = new TestTokenField();
        f.setReadOnly(true);
        f.setReadOnly(true); // should be a no-op
        assertThat(f.isReadOnly()).isTrue();
    }

    @Test
    void addedTokenAfterReadOnlyToggle() {
        TestTokenField f = new TestTokenField();
        f.setReadOnly(true);
        f.setReadOnly(false);
        f.addToken("post-toggle");
        assertWithMessage("Should be able to add tokens after toggling read-only off")
                .that(f.getTokenButtons()).containsKey("post-toggle");
    }
}
