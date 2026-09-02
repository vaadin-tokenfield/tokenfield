package org.vaadin.tokenfield;

import com.vaadin.ui.Button;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression tests for issue 03 — null safety &amp; read-only state in token
 * operations, reported as
 * <a href="https://github.com/vaadin-tokenfield/tokenfield/issues/13">#13</a>.
 *
 * <p>Each test below was written against the unfixed code and observed to fail
 * there; they stand now as guards against the three defects coming back.</p>
 *
 * <ul>
 *   <li><strong>3a</strong>: {@code removeToken} threw an NPE when the field
 *       value was null (a field never given a value). It cast {@code getValue()}
 *       straight into {@code new LinkedHashSet<>(set)} without the null guard
 *       {@code addToken} already had.</li>
 *   <li><strong>3b</strong>: token buttons created while the field was already
 *       read-only were not themselves read-only. {@code addTokenButton} applied
 *       no state to the fresh button, so a token injected via
 *       {@code setInternalValue} — bypassing the {@code AbstractField.setValue}
 *       read-only guard — was left marked writable.</li>
 *   <li><strong>3c</strong>: clicking a token of a read-only field removed it,
 *       or rather tried to and threw {@code Property.ReadOnlyException} out of
 *       the button's {@code ClickListener}. This is the one the stack trace in
 *       #13 shows.</li>
 * </ul>
 */
class TokenFieldNullReadOnlyReproTest {

    // -----------------------------------------------------------------------
    // 3a — removeToken NPE on null value
    // -----------------------------------------------------------------------

    /**
     * Repro for sub-issue 3a.
     *
     * <p>A freshly constructed {@link TokenField} has {@code getValue() == null}.
     * Calling {@link TokenField#removeToken} on such a field immediately
     * constructs {@code new LinkedHashSet<>(null)}, which throws
     * {@link NullPointerException}.
     *
     */
    @Test
    void removeTokenOnNullValueDoesNotThrow() {
        TestTokenField f = new TestTokenField();
        // getValue() is null — no token was ever added or set
        assertDoesNotThrow(() -> f.removeToken("nonexistent"),
                "removeToken on a null-value field must not throw NPE");
    }

    /**
     * Edge case for 3a: value is explicitly set to null via setValue(null),
     * then removeToken is called.
     *
     */
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
     * Repro for sub-issue 3b.
     *
     * <p>When a token is programmatically added while the field is already in
     * read-only mode (via the internal {@code setInternalValue} path, bypassing
     * the {@code AbstractField.setValue} guard), the resulting token button
     * must be read-only.
     *
     * <p>Before the fix {@code addTokenButton} applied no state to the fresh
     * button, leaving it marked writable on a read-only field.
     */
    @Test
    void tokenButtonAddedWhileReadOnlyIsItselfReadOnly() {
        TestTokenField f = new TestTokenField();
        f.setReadOnly(true);

        // Inject a token via setInternalValue, bypassing the AbstractField
        // read-only guard (which would throw ReadOnlyException for setValue).
        // This directly exercises addTokenButton while isReadOnly() == true.
        LinkedHashSet<Object> tokenSet = new LinkedHashSet<Object>();
        tokenSet.add("ro-token");
        f.exposeSetInternalValue(tokenSet);

        // The field must have created exactly one button for the injected token.
        assertEquals(1, f.getTokenButtons().size(),
                "One button should have been created for the injected token");

        Button b = f.getTokenButtons().get("ro-token");
        assertNotNull(b, "Button for 'ro-token' must exist in the buttons map");

        assertTrue(b.isReadOnly(),
                "A token button created while the field is read-only must itself be read-only");
    }

    /**
     * Secondary check for 3b: after adding tokens while read-only,
     * toggling the field back to writable must make those same buttons writable.
     *
     * <p>A round-trip guard rather than a repro: this one passed before the
     * fix too, since the button was already writable.
     */
    @Test
    void tokenButtonAddedWhileReadOnlyBecomesWritableAfterReadOnlyCleared() {
        TestTokenField f = new TestTokenField();
        f.setReadOnly(true);

        LinkedHashSet<Object> tokenSet = new LinkedHashSet<Object>();
        tokenSet.add("ro-token2");
        f.exposeSetInternalValue(tokenSet);

        // Now clear read-only — setReadOnly(false) must iterate all buttons
        // and call b.setReadOnly(false), regardless of their previous state.
        f.setReadOnly(false);

        Button b = f.getTokenButtons().get("ro-token2");
        assertNotNull(b);
        assertFalse(b.isReadOnly(),
                "After clearing read-only, token button must no longer be read-only");
    }

    /**
     * Confirm that the existing {@code setReadOnly(true)} path correctly marks
     * buttons read-only when tokens were added BEFORE the field became read-only.
     * This behaviour is already tested in {@link TokenFieldReadOnlyTest}; we
     * repeat it here as a baseline so the contrast with 3b is clear.
     */
    @Test
    void tokenButtonAddedBeforeReadOnlyIsMarkedReadOnlyBySetReadOnly() {
        TestTokenField f = new TestTokenField();
        f.addToken("pre-ro");
        f.setReadOnly(true);

        Button b = f.getTokenButtons().get("pre-ro");
        assertNotNull(b);
        // This already works today — setReadOnly iterates existing buttons.
        assertTrue(b.isReadOnly(),
                "Button added before setReadOnly(true) must be read-only afterwards");
    }

    // -----------------------------------------------------------------------
    // 3c — a click on a token of a read-only field must not remove it (#13)
    //
    // This is the sub-issue the reported stack trace in GitHub issue #13 is
    // about:
    //
    //   com.vaadin.data.Property$ReadOnlyException
    //       at TokenField.removeToken
    //       at TokenField.onTokenClick
    //       at TokenField$4.buttonClick   ← the token button's ClickListener
    //
    // Nothing upstream of that listener checks the read-only flag.
    // ButtonServerRpc#click is a bare fireClick(details), and the framework
    // drops an incoming RPC only for a connector that is not
    // connector-enabled — AbstractComponent implements that as isVisible() &&
    // isEnabled() && <parent chain>, which never consults read-only. So the
    // listener has to guard itself, the way CheckBox#setChecked does.
    //
    // The tests below drive the RPC rather than asserting on the buttons'
    // isEnabled()/isReadOnly() state, because that state is how the current
    // implementation happens to keep the click from being sent, not what a
    // caller is promised. Button#click() would not do: it returns early on a
    // read-only button and so passes whether or not the guard exists.
    // -----------------------------------------------------------------------

    /**
     * Repro for sub-issue 3c, on the path the issue was reported from: a token
     * added while the field was writable, then clicked after it went read-only.
     *
     * <p>FAILS without the guard: the click reaches {@code removeToken}, which
     * calls {@code setValue} on a read-only field and throws.
     */
    @Test
    void clickOnReadOnlyFieldDoesNotThrow() {
        TestTokenField f = new TestTokenField();
        f.addToken("pre-ro");
        f.setReadOnly(true);

        assertDoesNotThrow(() -> f.simulateTokenClickRpc("pre-ro"),
                "Clicking a token of a read-only field must not throw ReadOnlyException (#13)");
    }

    /**
     * The other half of 3c: the token must still be there afterwards. Asserted
     * separately from the exception because the two failure modes are
     * independent — a guard could swallow the exception and still remove the
     * token.
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

    /**
     * Repro for 3c on the second creation path: a token injected while the
     * field is already read-only, bypassing the {@code AbstractField.setValue}
     * guard, must be just as inert.
     */
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
     * Baseline: the guard must block read-only clicks only. On a writable field
     * the very same call still removes the token — otherwise a fix that simply
     * swallowed every click would pass the tests above.
     */
    @Test
    void clickOnWritableFieldRemovesTheToken() throws Exception {
        TestTokenField f = new TestTokenField();
        f.addToken("plain");

        f.simulateTokenClickRpc("plain");

        assertFalse(f.getTokenButtons().containsKey("plain"),
                "Clicking a token of a writable field must remove it");
    }

    /**
     * Round-trip guard: clearing read-only makes the tokens clickable again,
     * so a fix cannot leave the field permanently inert after one toggle.
     */
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
