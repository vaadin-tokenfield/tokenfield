package org.vaadin.tokenfield;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

/**
 * Tests for {@link TokenField#addToken}, {@link TokenField#removeToken}, and
 * the {@code setValue ↔ button reconciliation} behaviour of
 * {@link TokenField#setInternalValue}.
 */
class TokenFieldAddRemoveTest {

    private TestTokenField field;

    @BeforeEach
    void setup() {
        field = new TestTokenField();
    }

    // -----------------------------------------------------------------------
    // addToken
    // -----------------------------------------------------------------------

    @Test
    void addTokenMakesValueNonNull() {
        field.addToken("tag1");
        assertThat(field.getValue()).isNotNull();
    }

    @Test
    void addTokenValueContainsId() {
        field.addToken("alpha");
        @SuppressWarnings("unchecked")
        Set<Object> val = (Set<Object>) field.getValue();
        assertThat(val).contains("alpha");
    }

    @Test
    void addTokenCreatesButtonInMap() {
        field.addToken("x");
        assertThat(field.getTokenButtons()).hasSize(1);
        assertThat(field.getTokenButtons()).containsKey("x");
    }

    @Test
    void addTokenAddsButtonComponentToLayout() {
        field.addToken("t1");
        List<?> components = field.getLayoutComponents();
        assertWithMessage("Token button must appear in layout")
                .that(components).contains(field.getTokenButtons().get("t1"));
    }

    @Test
    void addDuplicateTokenIsNoOp() {
        field.addToken("dup");
        field.addToken("dup");
        assertThat(field.getTokenButtons()).hasSize(1);
        @SuppressWarnings("unchecked")
        Set<Object> val = (Set<Object>) field.getValue();
        assertThat(val).hasSize(1);
    }

    @Test
    void addMultipleTokensAllPresent() {
        field.addToken("a");
        field.addToken("b");
        field.addToken("c");
        assertThat(field.getTokenButtons()).hasSize(3);
        @SuppressWarnings("unchecked")
        Set<Object> val = (Set<Object>) field.getValue();
        assertThat(val).containsAtLeast("a", "b", "c");
    }

    @Test
    void insertionOrderIsPreserved() {
        field.addToken("first");
        field.addToken("second");
        field.addToken("third");
        @SuppressWarnings("unchecked")
        Set<Object> val = (Set<Object>) field.getValue();
        assertThat(val).containsExactly("first", "second", "third").inOrder();
    }

    // -----------------------------------------------------------------------
    // removeToken
    // -----------------------------------------------------------------------

    @Test
    void removeTokenRemovesFromValue() {
        field.addToken("r1");
        field.removeToken("r1");
        @SuppressWarnings("unchecked")
        Set<Object> val = (Set<Object>) field.getValue();
        assertThat(val).doesNotContain("r1");
    }

    @Test
    void removeTokenRemovesButtonFromMap() {
        field.addToken("btn1");
        field.removeToken("btn1");
        assertThat(field.getTokenButtons()).doesNotContainKey("btn1");
    }

    @Test
    void removeTokenRemovesButtonFromLayout() {
        field.addToken("x");
        field.addToken("y");
        field.removeToken("x");
        assertThat(field.getLayoutComponents()).doesNotContain(field.getTokenButtons().get("x"));
    }

    @Test
    void removeOneTokenLeavesOthersIntact() {
        field.addToken("keep");
        field.addToken("drop");
        field.removeToken("drop");
        assertThat(field.getTokenButtons()).hasSize(1);
        assertThat(field.getTokenButtons()).containsKey("keep");
    }

    @Test
    void removeUnknownTokenIsNoOp() {
        field.addToken("keep");
        field.removeToken("never-added");
        assertThat(field.getTokenButtons()).hasSize(1);
        assertThat(field.getTokenButtons()).containsKey("keep");
    }

    // -----------------------------------------------------------------------
    // setValue / setInternalValue reconciliation
    // -----------------------------------------------------------------------

    @Test
    void setValueAddsNewTokenButtons() {
        field.addToken("existing");
        LinkedHashSet<Object> newSet = new LinkedHashSet<Object>();
        newSet.add("existing");
        newSet.add("new1");
        field.setValue(newSet);
        assertThat(field.getTokenButtons()).hasSize(2);
        assertThat(field.getTokenButtons()).containsKey("new1");
    }

    @Test
    void setValueRemovesAbsentTokenButtons() {
        field.addToken("keep");
        field.addToken("remove");
        LinkedHashSet<Object> newSet = new LinkedHashSet<Object>();
        newSet.add("keep");
        field.setValue(newSet);
        assertThat(field.getTokenButtons()).hasSize(1);
        assertThat(field.getTokenButtons()).containsKey("keep");
        assertThat(field.getTokenButtons()).doesNotContainKey("remove");
    }

    @Test
    void setValueNullClearsAllButtons() {
        field.addToken("a");
        field.addToken("b");
        field.setValue(null);
        assertThat(field.getTokenButtons()).isEmpty();
    }

    @Test
    void setEmptySetClearsAllButtons() {
        field.addToken("a");
        field.setValue(new LinkedHashSet<Object>());
        assertThat(field.getTokenButtons()).isEmpty();
    }
}
