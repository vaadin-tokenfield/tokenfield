package org.vaadin.tokenfield;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests the delete/backspace-on-empty-input path, i.e. {@code TokenComboBox
 * #onDelete()} → {@link TokenField#onTokenDelete(Object)}. This is the server
 * half of what the client-side {@code VTokenField.onKeyDown} RPC reaches;
 * simulated here via {@link TestTokenField#simulateDeleteKey()} so it can be
 * verified without a browser.
 */
class TokenFieldDeleteKeyTest {

    private TestTokenField field;

    @BeforeEach
    void setup() {
        field = new TestTokenField();
    }

    @Test
    void deleteRemovesLastAddedToken() {
        field.addToken("first");
        field.addToken("second");
        field.simulateDeleteKey();
        assertWithMessage("Delete must remove the most recently added token")
                .that(field.getTokenButtons()).doesNotContainKey("second");
        assertWithMessage("Delete must not touch earlier tokens")
                .that(field.getTokenButtons()).containsKey("first");
    }

    @Test
    void deleteOnEmptyFieldIsNoOp() {
        // Truth has no exception-under-test assertion; JUnit's remains the
        // idiomatic tool for control-flow checks like this one.
        assertDoesNotThrow(() -> field.simulateDeleteKey());
        assertThat(field.getTokenButtons()).isEmpty();
    }

    @Test
    void deleteCallsOnTokenDeleteHook() {
        AtomicReference<Object> deleted = new AtomicReference<Object>();
        TestTokenField custom = new TestTokenField() {
            @Override
            protected void onTokenDelete(Object tokenId) {
                deleted.set(tokenId);
                super.onTokenDelete(tokenId);
            }
        };
        custom.addToken("only");
        custom.simulateDeleteKey();
        assertThat(deleted.get()).isEqualTo("only");
    }

    @Test
    void overriddenOnTokenDeleteIsHonored() {
        // Mirrors the demo's "Full featured example" panel, which overrides
        // onTokenDelete to remove immediately without the confirm dialog it
        // uses for onTokenClick.
        TestTokenField custom = new TestTokenField() {
            @Override
            protected void onTokenClick(Object tokenId) {
                fail("onTokenClick must not be used by the delete-key path when overridden");
            }

            @Override
            protected void onTokenDelete(Object tokenId) {
                removeToken(tokenId);
            }
        };
        custom.addToken("t1");
        custom.simulateDeleteKey();
        assertThat(custom.getTokenButtons()).doesNotContainKey("t1");
    }

    @Test
    void deleteRespectsInsertionOrderAfterIntermediateRemoval() {
        field.addToken("a");
        field.addToken("b");
        field.addToken("c");
        field.removeToken("b");
        field.simulateDeleteKey();
        assertWithMessage("Delete must remove 'c', the last remaining token in insertion order")
                .that(field.getTokenButtons()).doesNotContainKey("c");
        assertThat(field.getTokenButtons()).containsKey("a");
    }
}
