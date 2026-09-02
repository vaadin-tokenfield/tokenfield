package org.vaadin.tokenfield;

import com.vaadin.ui.Button;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression tests for
 * <a href="https://github.com/vaadin-tokenfield/tokenfield/issues/13">#13</a> —
 * null safety and read-only state in token operations. Three defects, each
 * observed to fail here before it was fixed:
 *
 * <ul>
 *   <li><strong>3a</strong>: {@code removeToken} threw an NPE on a field with
 *       no value.</li>
 *   <li><strong>3b</strong>: a token button created while the field was
 *       already read-only was left marked writable.</li>
 *   <li><strong>3c</strong>: clicking a token of a read-only field threw
 *       {@code Property.ReadOnlyException} — the stack trace #13 reported.</li>
 * </ul>
 */
class TokenFieldNullReadOnlyReproTest {

    // -----------------------------------------------------------------------
    // 3a — removeToken NPE on null value
    // -----------------------------------------------------------------------

    /** A freshly constructed field has {@code getValue() == null}. */
    @Test
    void removeTokenOnNullValueDoesNotThrow() {
        TestTokenField f = new TestTokenField();
        // getValue() is null — no token was ever added or set
        assertDoesNotThrow(() -> f.removeToken("nonexistent"),
                "removeToken on a null-value field must not throw NPE");
    }

    /** The same, reached by an explicit {@code setValue(null)}. */
    @Test
    void removeTokenAfterSetValueNullDoesNotThrow() {
        TestTokenField f = new TestTokenField();
        f.addToken("initial");
        f.setValue(null);   // explicit null — buttons map is cleared
        assertDoesNotThrow(() -> f.removeToken("initial"),
                "removeToken after setValue(null) must not throw NPE");
    }

    // -----------------------------------------------------------------------
    // 3b — addTokenButton does not apply current read-only state
    // -----------------------------------------------------------------------

    /**
     * A token added while the field is already read-only must produce a
     * read-only button. Before the fix {@code addTokenButton} applied no state
     * to the fresh button at all.
     */
    @Test
    void tokenButtonAddedWhileReadOnlyIsItselfReadOnly() {
        TestTokenField f = new TestTokenField();
        f.setReadOnly(true);

        // setInternalValue bypasses the AbstractField read-only guard that
        // setValue would hit, so addTokenButton runs while isReadOnly().
        LinkedHashSet<Object> tokenSet = new LinkedHashSet<Object>();
        tokenSet.add("ro-token");
        f.exposeSetInternalValue(tokenSet);

        assertEquals(1, f.getTokenButtons().size(),
                "One button should have been created for the injected token");

        Button b = f.getTokenButtons().get("ro-token");
        assertNotNull(b, "Button for 'ro-token' must exist in the buttons map");

        assertTrue(b.isReadOnly(),
                "A token button created while the field is read-only must itself be read-only");
    }

    /** Round-trip guard: clearing read-only must make those buttons writable. */
    @Test
    void tokenButtonAddedWhileReadOnlyBecomesWritableAfterReadOnlyCleared() {
        TestTokenField f = new TestTokenField();
        f.setReadOnly(true);

        LinkedHashSet<Object> tokenSet = new LinkedHashSet<Object>();
        tokenSet.add("ro-token2");
        f.exposeSetInternalValue(tokenSet);

        f.setReadOnly(false);

        Button b = f.getTokenButtons().get("ro-token2");
        assertNotNull(b);
        assertFalse(b.isReadOnly(),
                "After clearing read-only, token button must no longer be read-only");
    }

    /** Baseline for the contrast with 3b: the pre-existing setReadOnly path. */
    @Test
    void tokenButtonAddedBeforeReadOnlyIsMarkedReadOnlyBySetReadOnly() {
        TestTokenField f = new TestTokenField();
        f.addToken("pre-ro");
        f.setReadOnly(true);

        Button b = f.getTokenButtons().get("pre-ro");
        assertNotNull(b);
        assertTrue(b.isReadOnly(),
                "Button added before setReadOnly(true) must be read-only afterwards");
    }

    // -----------------------------------------------------------------------
    // 3c — a click on a token of a read-only field must not remove it (#13)
    //
    //   com.vaadin.data.Property$ReadOnlyException
    //       at TokenField.removeToken
    //       at TokenField.onTokenClick
    //       at TokenField$4.buttonClick   ← the token button's ClickListener
    //
    // These drive the click RPC rather than asserting on the buttons'
    // isEnabled()/isReadOnly() state: that state is how the click happens to be
    // kept from being sent, not what a caller is promised.
    // -----------------------------------------------------------------------

    /** The path #13 was reported from: a token added, then the field locked. */
    @Test
    void clickOnReadOnlyFieldDoesNotThrow() {
        TestTokenField f = new TestTokenField();
        f.addToken("pre-ro");
        f.setReadOnly(true);

        assertDoesNotThrow(() -> f.simulateTokenClickRpc("pre-ro"),
                "Clicking a token of a read-only field must not throw ReadOnlyException (#13)");
    }

    /**
     * Asserted separately from the exception: a guard could swallow the
     * throw and still remove the token.
     */
    @Test
    void clickOnReadOnlyFieldKeepsTheToken() throws Exception {
        TestTokenField f = new TestTokenField();
        f.addToken("pre-ro");
        f.setReadOnly(true);

        f.simulateTokenClickRpc("pre-ro");

        assertTrue(f.getTokenButtons().containsKey("pre-ro"),
                "A token of a read-only field must survive being clicked");
        assertTrue(((Set<?>) f.getValue()).contains("pre-ro"),
                "The field's value must still hold the token that was clicked");
    }

    /** The other creation path: a token injected while already read-only. */
    @Test
    void clickOnTokenAddedWhileReadOnlyDoesNotRemoveIt() {
        TestTokenField f = new TestTokenField();
        f.setReadOnly(true);

        LinkedHashSet<Object> tokenSet = new LinkedHashSet<Object>();
        tokenSet.add("ro-token3");
        f.exposeSetInternalValue(tokenSet);

        assertDoesNotThrow(() -> f.simulateTokenClickRpc("ro-token3"),
                "A token created while the field is read-only must also be inert");
        assertTrue(f.getTokenButtons().containsKey("ro-token3"),
                "That token must survive being clicked");
    }

    /**
     * Baseline: the same call still removes the token on a writable field,
     * so a fix that swallowed every click would not pass.
     */
    @Test
    void clickOnWritableFieldRemovesTheToken() throws Exception {
        TestTokenField f = new TestTokenField();
        f.addToken("plain");

        f.simulateTokenClickRpc("plain");

        assertFalse(f.getTokenButtons().containsKey("plain"),
                "Clicking a token of a writable field must remove it");
    }

    /** Round-trip guard: one read-only toggle must not leave the field inert. */
    @Test
    void clickWorksAgainOnceReadOnlyIsCleared() throws Exception {
        TestTokenField f = new TestTokenField();
        f.addToken("toggle-me");
        f.setReadOnly(true);
        f.setReadOnly(false);

        f.simulateTokenClickRpc("toggle-me");

        assertFalse(f.getTokenButtons().containsKey("toggle-me"),
                "Tokens must be removable again once read-only is cleared");
    }
}
