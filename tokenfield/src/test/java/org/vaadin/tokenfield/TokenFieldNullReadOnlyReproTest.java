package org.vaadin.tokenfield;

import com.vaadin.ui.Button;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

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
        assertThat(f.getValue()).isNull();
        f.removeToken("nonexistent");
        assertThat(f.getValue()).isNull();
    }

    /** The same, reached by an explicit {@code setValue(null)}. */
    @Test
    void removeTokenAfterSetValueNullDoesNotThrow() {
        TestTokenField f = new TestTokenField();
        f.addToken("initial");
        assertThat(f.buttons).containsKey("initial");
        f.setValue(null);   // explicit null — buttons map is cleared
        assertThat(f.buttons).isEmpty();
        f.removeToken("initial");
        assertThat(f.buttons).isEmpty();
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
        LinkedHashSet<Object> tokenSet = new LinkedHashSet<>();
        tokenSet.add("ro-token");
        f.exposeSetInternalValue(tokenSet);

        assertWithMessage("One button should have been created for the injected token")
                .that(f.getTokenButtons()).hasSize(1);

        Button b = f.getTokenButtons().get("ro-token");
        assertWithMessage("Button for 'ro-token' must exist in the buttons map")
                .that(b).isNotNull();

        assertWithMessage("A token button created while the field is read-only must itself be read-only")
                .that(b.isReadOnly()).isTrue();
    }

    /** Round-trip guard: clearing read-only must make those buttons writable. */
    @Test
    void tokenButtonAddedWhileReadOnlyBecomesWritableAfterReadOnlyCleared() {
        TestTokenField f = new TestTokenField();
        f.setReadOnly(true);

        LinkedHashSet<Object> tokenSet = new LinkedHashSet<>();
        tokenSet.add("ro-token2");
        f.exposeSetInternalValue(tokenSet);

        f.setReadOnly(false);

        Button b = f.getTokenButtons().get("ro-token2");
        assertThat(b).isNotNull();
        assertWithMessage("After clearing read-only, token button must no longer be read-only")
                .that(b.isReadOnly()).isFalse();
    }

    /** Baseline for the contrast with 3b: the pre-existing setReadOnly path. */
    @Test
    void tokenButtonAddedBeforeReadOnlyIsMarkedReadOnlyBySetReadOnly() {
        TestTokenField f = new TestTokenField();
        f.addToken("pre-ro");
        f.setReadOnly(true);

        Button b = f.getTokenButtons().get("pre-ro");
        assertThat(b).isNotNull();
        assertWithMessage("Button added before setReadOnly(true) must be read-only afterwards")
                .that(b.isReadOnly()).isTrue();
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

        assertWithMessage("A token of a read-only field must survive being clicked")
                .that(f.getTokenButtons()).containsKey("pre-ro");
        assertWithMessage("The field's value must still hold the token that was clicked")
                .that(f.getValue()).contains("pre-ro");
    }

    /** The other creation path: a token injected while already read-only. */
    @Test
    void clickOnTokenAddedWhileReadOnlyDoesNotRemoveIt() throws Exception {
        TestTokenField f = new TestTokenField();
        f.setReadOnly(true);

        LinkedHashSet<Object> tokenSet = new LinkedHashSet<>();
        tokenSet.add("ro-token3");
        f.exposeSetInternalValue(tokenSet);

        f.simulateTokenClickRpc("ro-token3");
        assertWithMessage("That token must survive being clicked")
                .that(f.getTokenButtons()).containsKey("ro-token3");
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

        assertWithMessage("Clicking a token of a writable field must remove it")
                .that(f.getTokenButtons()).doesNotContainKey("plain");
    }

    /** Round-trip guard: one read-only toggle must not leave the field inert. */
    @Test
    void clickWorksAgainOnceReadOnlyIsCleared() throws Exception {
        TestTokenField f = new TestTokenField();
        f.addToken("toggle-me");
        f.setReadOnly(true);
        f.setReadOnly(false);

        f.simulateTokenClickRpc("toggle-me");

        assertWithMessage("Tokens must be removable again once read-only is cleared")
                .that(f.getTokenButtons()).doesNotContainKey("toggle-me");
    }
}
