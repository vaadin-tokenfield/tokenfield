package org.vaadin.tokenfield;

import com.vaadin.ui.Component;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Layout;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

/**
 * Tests {@link TokenField#setLayout(Layout)}, simulated via
 * {@link TestTokenField#changeLayout(Layout)}. Note: this only covers moving
 * components between layouts (what {@code DemoRoot}'s "Layout and
 * InsertPosition" panel relies on, by constructing a new {@code TokenField}
 * rather than calling this after the field is attached) — {@code setLayout}
 * called after {@code initContent()} has already rendered the composition
 * root does not re-root the UI; see the {@code // TODO setCompositionRoot}
 * at {@code TokenField.java:518}.
 */
class TokenFieldLayoutTest {

    @Test
    void setLayoutMovesComponentsToTheNewLayout() {
        TestTokenField f = new TestTokenField();
        f.addToken("a");
        f.addToken("b");

        Layout newLayout = new HorizontalLayout();
        f.changeLayout(newLayout);

        assertThat(f.getInternalLayout()).isSameInstanceAs(newLayout);
        List<Component> comps = f.getLayoutComponents();
        assertThat(comps).contains(f.getComboBox());
        assertThat(comps).contains(f.getTokenButtons().get("a"));
        assertThat(comps).contains(f.getTokenButtons().get("b"));
    }

    @Test
    void oldLayoutIsEmptiedAfterSetLayout() {
        TestTokenField f = new TestTokenField();
        f.addToken("a");
        Layout oldLayout = f.getInternalLayout();

        f.changeLayout(new HorizontalLayout());

        assertWithMessage("The previous layout must be emptied when the layout is swapped")
                .that(oldLayout.getComponentCount()).isEqualTo(0);
    }

    @Test
    void horizontalLayoutGetsExpandRatioOnComboBox() {
        TestTokenField f = new TestTokenField();
        HorizontalLayout hl = new HorizontalLayout();
        f.changeLayout(hl);
        assertThat(hl.getExpandRatio(f.getComboBox())).isEqualTo(1.0f);
    }

    @Test
    void horizontalLayoutExpandRatioSurvivesAddToken() {
        TestTokenField f = new TestTokenField();
        HorizontalLayout hl = new HorizontalLayout();
        f.changeLayout(hl);
        f.addToken("x");
        assertThat(hl.getExpandRatio(f.getComboBox())).isEqualTo(1.0f);
    }

    @Test
    void gridLayoutKeepsInsertionOrder() {
        TestTokenField f = new TestTokenField();
        GridLayout gl = new GridLayout(3, 1);
        f.changeLayout(gl);
        f.addToken("first");
        f.addToken("second");

        List<Component> comps = f.getLayoutComponents();
        int cbIndex = comps.indexOf(f.getComboBox());
        int firstIndex = comps.indexOf(f.getTokenButtons().get("first"));
        int secondIndex = comps.indexOf(f.getTokenButtons().get("second"));
        // default InsertPosition.BEFORE: token buttons precede the input, in
        // the order they were added
        assertThat(firstIndex).isLessThan(secondIndex);
        assertThat(secondIndex).isLessThan(cbIndex);
    }
}
