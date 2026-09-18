package org.vaadin.tokenfield;

import com.vaadin.server.Resource;
import com.vaadin.server.ThemeResource;
import com.vaadin.ui.AbstractSelect;
import com.vaadin.ui.AbstractSelect.ItemCaptionMode;
import com.vaadin.ui.Button;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static com.google.common.truth.Truth.assertThat;

/**
 * Pins {@code null} as a fully supported token id, across every public
 * method and protected extension point that takes one - verified against
 * actual behavior (not just source-reading) after a claimed NPE risk in
 * {@code removeTokenButton} turned out not to be reachable: Vaadin's own
 * {@code AbstractField#setValue} only proceeds on an actual value change, and
 * {@code setInternalValue}'s token-to-remove set is always sourced from a
 * {@code buttons} keySet snapshot, so a token id it hands to
 * {@code removeTokenButton} is always a key that is genuinely present.
 * <p>
 * A {@code null} token id is reachable only programmatically. The UI path is
 * guarded out on purpose: {@code cb.setNullSelectionAllowed(false)} plus the
 * value-change listener's own {@code if (tokenId != null)} check mean a user
 * can never select or type their way to a null token - only direct API calls
 * (as every test here does) can add one.
 * <p>
 * Displayed caption/icon assertions go through the actual token
 * {@code Button} in {@code buttons} - what a user would see - rather than
 * {@code getTokenCaption}/{@code getTokenIcon} directly.
 */
class TokenFieldNullTokenIdTest {

    private final TestTokenField field = new TestTokenField();

    // ------------------------------------------------------------------
    // addToken / removeToken
    // ------------------------------------------------------------------

    @Test
    void addTokenAcceptsNull() {
        field.addToken(null);

        assertThat(field.getValue()).contains(null);
        assertThat(field.getTokenButtons()).containsKey(null);
    }

    @Test
    void addingNullTwiceStaysADuplicateNoOp() {
        field.addToken(null);
        field.addToken(null);

        assertThat(field.getTokenButtons()).hasSize(1);
    }

    @Test
    void removeTokenRemovesAPreviouslyAddedNullToken() {
        field.addToken(null);

        field.removeToken(null);

        assertThat(field.getTokenButtons()).doesNotContainKey(null);
        assertThat(field.getValue()).doesNotContain(null);
    }

    @Test
    void removeTokenOnAVirginFieldIsANoOp() {
        assertThat(field.buttons).isEmpty();
        field.removeToken(null);
        assertThat(field.buttons).isEmpty();
    }

    /**
     * Removing an id (null or not) that isn't currently a token is a no-op:
     * the rebuilt set is content-identical to the current one, so
     * {@code AbstractField#setValue} never even calls
     * {@code setInternalValue}. Confirmed empirically, not assumed - this is
     * the finding that overturned the original NPE claim.
     */
    @Test
    void removingANullTokenThatWasNeverAddedIsANoOpEvenWithOtherTokensPresent() {
        field.addToken("present");

        field.removeToken(null);

        assertThat(field.getTokenButtons()).containsKey("present");
        assertThat(field.getTokenButtons()).doesNotContainKey(null);
    }

    // ------------------------------------------------------------------
    // onTokenInput / onTokenClick / onTokenDelete (default overrides)
    // ------------------------------------------------------------------

    @Test
    void onTokenInputDefaultsToAddToken() {
        field.onTokenInput(null);

        assertThat(field.getTokenButtons()).containsKey(null);
    }

    @Test
    void onTokenClickDefaultsToRemoveToken() {
        field.addToken(null);

        field.onTokenClick(null);

        assertThat(field.getTokenButtons()).doesNotContainKey(null);
    }

    @Test
    void onTokenDeleteDefaultsToOnTokenClick() {
        field.addToken(null);

        field.onTokenDelete(null);

        assertThat(field.getTokenButtons()).doesNotContainKey(null);
    }

    /**
     * The delete-key path ({@code TokenComboBox.onDelete}) always draws its
     * id from {@code buttons.keySet()} itself, so a null token added earlier
     * is exactly what backspace-in-empty-input removes last.
     */
    @Test
    void deleteKeyRemovesANullTokenWhenItIsTheLastOne() {
        field.addToken("first");
        field.addToken(null);

        field.simulateDeleteKey();

        assertThat(field.getTokenButtons()).doesNotContainKey(null);
        assertThat(field.getTokenButtons()).containsKey("first");
    }

    /** The click RPC path a real browser click on the null token's button takes. */
    @Test
    void clickingTheNullTokenButtonRemovesIt() throws Exception {
        field.addToken(null);

        field.simulateTokenClickRpc(null);

        assertThat(field.getTokenButtons()).doesNotContainKey(null);
    }

    // ------------------------------------------------------------------
    // configureTokenButton
    // ------------------------------------------------------------------

    @Test
    void configureTokenButtonAcceptsNullTokenId() {
        Button button = new Button();

        field.configureTokenButton(null, button);

        // getTokenCaption(null) is null under the default mode; string
        // concatenation renders that as the literal word "null".
        assertThat(button.getCaption()).isEqualTo("null ×");
    }

    // ------------------------------------------------------------------
    // The displayed caption on the actual token button, across every
    // ItemCaptionMode, with no explicit override set. Every mode renders
    // "null ×", by two different code paths: ITEM/PROPERTY stringify the id
    // themselves (TokenField.getTokenCaption's own branch); every other mode
    // gets actual null back from AbstractSelect#getItemCaption (which
    // special-cases a null itemId before its mode switch even runs), and
    // Java's string concatenation coerces that null to the word "null".
    // ------------------------------------------------------------------

    private static Stream<Arguments> everyModeWithNoOverride() {
        return Stream.of(ItemCaptionMode.values())
                .map(mode -> Arguments.of(mode, "null ×"));
    }

    @ParameterizedTest
    @MethodSource("everyModeWithNoOverride")
    void addingANullTokenRendersItsDisplayedCaptionPerMode(
            ItemCaptionMode mode, String expectedCaption) {
        field.setTokenCaptionMode(mode);

        field.addToken(null);

        assertThat(field.getTokenButtons().get(null).getCaption())
                .isEqualTo(expectedCaption);
    }

    @Test
    void addingANullTokenNeverShowsAnIcon() {
        field.addToken(null);

        assertThat(field.getTokenButtons().get(null).getIcon()).isNull();
    }

    // ------------------------------------------------------------------
    // setTokenCaption / setTokenIcon - explicit null-token overrides,
    // reflected on the actual token button.
    // ------------------------------------------------------------------

    /**
     * Mirrors the real-token-id contract: an explicit caption only applies
     * under EXPLICIT/EXPLICIT_DEFAULTS_ID - {@link AbstractSelect}'s own
     * switch, not a TokenField decision (see
     * {@link TokenFieldCaptionDerivationTest}). Every other mode keeps
     * rendering "null ×", exactly as with no override at all.
     */
    private static Stream<Arguments> everyModeWithACaptionOverride() {
        return Stream.of(
                Arguments.of(ItemCaptionMode.EXPLICIT, "Everyone ×"),
                Arguments.of(ItemCaptionMode.EXPLICIT_DEFAULTS_ID, "Everyone ×"),
                Arguments.of(ItemCaptionMode.ID, "null ×"),
                Arguments.of(ItemCaptionMode.ID_TOSTRING, "null ×"),
                Arguments.of(ItemCaptionMode.INDEX, "null ×"),
                Arguments.of(ItemCaptionMode.ITEM, "null ×"),
                Arguments.of(ItemCaptionMode.PROPERTY, "null ×"),
                Arguments.of(ItemCaptionMode.ICON_ONLY, "null ×"));
    }

    @ParameterizedTest
    @MethodSource("everyModeWithACaptionOverride")
    void settingACaptionOverrideAffectsTheDisplayedCaptionPerMode(
            ItemCaptionMode mode, String expectedCaption) {
        field.setTokenCaptionMode(mode);
        field.addToken(null);

        field.setTokenCaption(null, "Everyone");

        assertThat(field.getTokenButtons().get(null).getCaption())
                .isEqualTo(expectedCaption);
    }

    @Test
    void settingTheCaptionOverrideBackToNullClearsIt() {
        // this is default, but make it explicit
        field.setTokenCaptionMode(ItemCaptionMode.EXPLICIT_DEFAULTS_ID);

        field.addToken(null);

        field.setTokenCaption(null, "Everyone");
        assertThat(field.getTokenButtons().get(null).getCaption())
                .isEqualTo("Everyone ×");

        field.setTokenCaption(null, null);
        assertThat(field.getTokenButtons().get(null).getCaption())
                .isEqualTo("null ×");
    }

    /** setTokenIcon(null, ...) takes effect even after the token already exists. */
    @Test
    void settingAnIconOnAnAlreadyAddedNullTokenUpdatesItsDisplayedIcon() {
        field.addToken(null);
        Resource icon = new ThemeResource("icons/token.png");

        field.setTokenIcon(null, icon);

        assertThat(field.getTokenButtons().get(null).getIcon())
                .isSameInstanceAs(icon);
    }

    @Test
    void settingTheIconOverrideBackToNullClearsIt() {
        field.addToken(null);
        ThemeResource icon = new ThemeResource("icons/token.png");
        field.setTokenIcon(null, icon);
        assertThat(field.getTokenButtons().get(null).getIcon())
                .isSameInstanceAs(icon);

        field.setTokenIcon(null, null);

        assertThat(field.getTokenButtons().get(null).getIcon()).isNull();
    }
}
