package org.vaadin.tokenfield;

import com.vaadin.ui.Button;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Reproduction tests for issue 03 — Null safety &amp; read-only state in token
 * operations.
 *
 * <p><strong>These tests are expected to FAIL against the current production code.</strong>
 * They document the two bugs described in code-review/03-null-and-state-handling.md
 * and must remain failing until the bugs are fixed.</p>
 *
 * <ul>
 *   <li><strong>3a</strong>: {@code removeToken} throws NPE when the field value is null
 *       (field was never given a value).  {@link TokenField#removeToken} does
 *       not guard against {@code getValue() == null}, unlike {@code addToken}
 *       which does.  Fix: add an early-return guard before constructing the
 *       {@code LinkedHashSet}.</li>
 *   <li><strong>3b</strong>: Token buttons created while the field is already in
 *       read-only mode are not themselves read-only.  {@code addTokenButton}
 *       never calls {@code b.setReadOnly(isReadOnly())}, so if tokens are
 *       injected via {@code setInternalValue} (bypassing the
 *       {@code AbstractField.setValue} read-only guard) the new button is left
 *       interactive.  Fix: call {@code b.setReadOnly(isReadOnly())} inside
 *       {@code addTokenButton}.</li>
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
     * <p>This test FAILS with the current code because the NPE is thrown.
     * After the fix (null guard added to {@code removeToken}) it must pass.
     */
    @Test
    void removeTokenOnNullValueDoesNotThrow() {
        TestTokenField f = new TestTokenField();
        // getValue() is null — no token was ever added or set
        // BUG: currently throws NullPointerException inside removeToken
        assertDoesNotThrow(() -> f.removeToken("nonexistent"),
                "removeToken on a null-value field must not throw NPE");
    }

    /**
     * Edge case for 3a: value is explicitly set to null via setValue(null),
     * then removeToken is called.
     *
     * <p>This test FAILS with the current code for the same NPE reason.
     */
    @Test
    void removeTokenAfterSetValueNullDoesNotThrow() {
        TestTokenField f = new TestTokenField();
        f.addToken("initial");
        f.setValue(null);   // explicit null — buttons map is cleared
        // BUG: the next call constructs new LinkedHashSet<>(null) → NPE
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
     * <p>{@code addTokenButton} creates the button but never calls
     * {@code b.setReadOnly(isReadOnly())}.  Therefore the button is left in
     * its default (writable) state, which allows the user to click it and
     * trigger {@code removeToken} even though the field is read-only.
     *
     * <p>This test FAILS with the current code because the button's
     * {@code isReadOnly()} returns {@code false} even though the field is
     * read-only.  After the fix it must pass.
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

        // BUG: b.isReadOnly() is currently false — addTokenButton never calls
        //      b.setReadOnly(isReadOnly()).  The assertion below will FAIL.
        assertTrue(b.isReadOnly(),
                "A token button created while the field is read-only must itself be read-only");
    }

    /**
     * Secondary check for 3b: after adding tokens while read-only,
     * toggling the field back to writable must make those same buttons writable.
     *
     * <p>This verifies the round-trip behaviour once sub-issue 3b is fixed.
     * With the current (broken) code this test happens to pass by accident
     * (the button was already writable), so it is included only as a guard
     * for the post-fix state and does not need to fail now.  We include it
     * here for completeness.
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
    // 3c — read-only does not stop a token button from accepting clicks (#13)
    //
    // This is the sub-issue the reported stack trace in GitHub issue #13 is
    // about:
    //
    //   com.vaadin.data.Property$ReadOnlyException
    //       at TokenField.removeToken
    //       at TokenField.onTokenClick
    //       at TokenField$4.buttonClick   ← the token button's ClickListener
    //
    // Marking a Button read-only does not stop the client from reaching its
    // click listener. Vaadin drops an incoming RPC call only when the target
    // connector is *disabled*: ServerRpcHandler consults
    // ClientConnector#isConnectorEnabled(), and AbstractComponent implements
    // that as isVisible() && isEnabled() && <parent chain> — the read-only
    // flag is not part of it. So a read-only-but-enabled token button still
    // has its ClickListener invoked, which calls removeToken -> setValue on a
    // read-only field -> ReadOnlyException.
    //
    // (isConnectorEnabled() itself is not assertable here: it walks up to the
    // UI, which a detached unit-test field does not have. isEnabled() is the
    // property under this add-on's control and the one that gate reads.)
    // -----------------------------------------------------------------------

    /**
     * Repro for sub-issue 3c: a token button that existed before the field was
     * put into read-only mode must be disabled, not merely marked read-only.
     *
     * <p>FAILS with the current code: {@code setReadOnly} calls only
     * {@code b.setReadOnly(true)}, leaving {@code isEnabled()} true.
     */
    @Test
    void tokenButtonIsDisabledWhileFieldIsReadOnly() {
        TestTokenField f = new TestTokenField();
        f.addToken("pre-ro");
        f.setReadOnly(true);

        Button b = f.getTokenButtons().get("pre-ro");
        assertNotNull(b);
        assertFalse(b.isEnabled(),
                "A token button must be disabled while the field is read-only, so that "
                        + "Vaadin drops the click RPC before it reaches removeToken (#13)");
    }

    /**
     * Repro for sub-issue 3c on the other path: a token button created
     * <em>while</em> the field is already read-only must also be disabled.
     *
     * <p>FAILS with the current code: {@code addTokenButton} applies no state
     * at all to the freshly created button.
     */
    @Test
    void tokenButtonAddedWhileReadOnlyIsDisabled() {
        TestTokenField f = new TestTokenField();
        f.setReadOnly(true);

        LinkedHashSet<Object> tokenSet = new LinkedHashSet<Object>();
        tokenSet.add("ro-token3");
        f.exposeSetInternalValue(tokenSet);

        Button b = f.getTokenButtons().get("ro-token3");
        assertNotNull(b, "Button for 'ro-token3' must exist in the buttons map");
        assertFalse(b.isEnabled(),
                "A token button created while the field is read-only must be disabled");
    }

    /**
     * Round-trip guard: clearing read-only must re-enable the token buttons,
     * otherwise the fix for 3c would leave the field permanently unusable
     * after a single read-only toggle.
     */
    @Test
    void tokenButtonIsReEnabledWhenReadOnlyIsCleared() {
        TestTokenField f = new TestTokenField();
        f.addToken("toggle-me");
        f.setReadOnly(true);
        f.setReadOnly(false);

        Button b = f.getTokenButtons().get("toggle-me");
        assertNotNull(b);
        assertTrue(b.isEnabled(),
                "Token buttons must be clickable again once read-only is cleared");
    }

    /**
     * Baseline: a token button on a writable field is enabled. Guards against a
     * fix that disables buttons unconditionally.
     */
    @Test
    void tokenButtonOnWritableFieldIsEnabled() {
        TestTokenField f = new TestTokenField();
        f.addToken("plain");

        Button b = f.getTokenButtons().get("plain");
        assertNotNull(b);
        assertTrue(b.isEnabled(),
                "Token buttons on a writable field must remain enabled");
    }
}
