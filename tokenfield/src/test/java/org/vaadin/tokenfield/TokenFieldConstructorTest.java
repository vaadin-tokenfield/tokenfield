package org.vaadin.tokenfield;

import com.vaadin.ui.CssLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Layout;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;

/**
 * Exercises every {@link TokenField} constructor overload and asserts caption,
 * layout, and {@link TokenField#getTokenInsertPosition()} come out as
 * documented.
 */
class TokenFieldConstructorTest {

    @Test
    void noArgConstructor() {
        TestTokenField f = new TestTokenField();
        assertThat(f.getCaption()).isNull();
        assertThat(f.getLayout()).isInstanceOf(CssLayout.class);
        assertThat(f.getTokenInsertPosition()).isEqualTo(TokenField.InsertPosition.BEFORE);
    }

    @Test
    void captionOnlyConstructor() {
        TestTokenField f = new TestTokenField("My Tags");
        assertThat(f.getCaption()).isEqualTo("My Tags");
        assertThat(f.getLayout()).isInstanceOf(CssLayout.class);
        assertThat(f.getTokenInsertPosition()).isEqualTo(TokenField.InsertPosition.BEFORE);
    }

    @Test
    void captionAndInsertPositionConstructor() {
        TestTokenField f = new TestTokenField("My Tags", TokenField.InsertPosition.AFTER);
        assertThat(f.getCaption()).isEqualTo("My Tags");
        assertThat(f.getLayout()).isInstanceOf(CssLayout.class);
        assertThat(f.getTokenInsertPosition()).isEqualTo(TokenField.InsertPosition.AFTER);
    }

    @Test
    void layoutOnlyConstructor() {
        Layout lo = new HorizontalLayout();
        TestTokenField f = new TestTokenField(lo);
        assertThat(f.getLayout()).isSameInstanceAs(lo);
        assertThat(f.getTokenInsertPosition()).isEqualTo(TokenField.InsertPosition.BEFORE);
    }

    @Test
    void captionAndLayoutConstructor() {
        Layout lo = new HorizontalLayout();
        TestTokenField f = new TestTokenField("My Tags", lo);
        assertThat(f.getCaption()).isEqualTo("My Tags");
        assertThat(f.getLayout()).isSameInstanceAs(lo);
        assertThat(f.getTokenInsertPosition()).isEqualTo(TokenField.InsertPosition.BEFORE);
    }

    @Test
    void captionLayoutAndInsertPositionConstructor() {
        Layout lo = new HorizontalLayout();
        TestTokenField f = new TestTokenField("My Tags", lo, TokenField.InsertPosition.AFTER);
        assertThat(f.getCaption()).isEqualTo("My Tags");
        assertThat(f.getLayout()).isSameInstanceAs(lo);
        assertThat(f.getTokenInsertPosition()).isEqualTo(TokenField.InsertPosition.AFTER);
    }

    @Test
    void layoutAndInsertPositionConstructor() {
        Layout lo = new HorizontalLayout();
        TestTokenField f = new TestTokenField(lo, TokenField.InsertPosition.AFTER);
        assertThat(f.getLayout()).isSameInstanceAs(lo);
        assertThat(f.getTokenInsertPosition()).isEqualTo(TokenField.InsertPosition.AFTER);
    }
}
