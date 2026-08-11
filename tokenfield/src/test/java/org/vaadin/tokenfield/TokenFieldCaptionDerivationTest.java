package org.vaadin.tokenfield;

import com.vaadin.data.util.IndexedContainer;
import com.vaadin.server.Resource;
import com.vaadin.server.ThemeResource;
import com.vaadin.ui.AbstractSelect.ItemCaptionMode;
import com.vaadin.ui.Button;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

/**
 * Pins down how a token button derives its caption and icon: identically for
 * every {@link ItemCaptionMode}, for tokens inside and outside the container,
 * and independently of the order in which the field is configured.
 *
 * <p>Two tokens are used throughout: {@code id-a} is in the container and has a
 * {@code name} property, {@code id-x} is not - the case TokenField explicitly
 * supports.</p>
 */
class TokenFieldCaptionDerivationTest {

    private static final String IN = "id-a";
    private static final String OUT = "id-x";

    private static IndexedContainer container() {
        IndexedContainer c = new IndexedContainer();
        c.addContainerProperty("name", String.class, null);
        c.addItem(IN).getItemProperty("name").setValue("Alpha");
        return c;
    }

    /** Caption of a token button, without the trailing remove glyph. */
    private static String caption(TestTokenField field, Object tokenId) {
        return field.getTokenButtons().get(tokenId).getCaption()
                .replaceAll(" ×$", "");
    }

    private static Map<Object, String> captions(TestTokenField field) {
        Map<Object, String> result = new LinkedHashMap<Object, String>();
        for (Object tokenId : field.getTokenButtons().keySet()) {
            result.put(tokenId, caption(field, tokenId));
        }
        return result;
    }

    /** Applies the caption configuration; the mode is set last so it wins. */
    private static void configure(TestTokenField field, ItemCaptionMode mode,
            IndexedContainer c) {
        field.setContainerDataSource(c);
        field.setTokenCaptionPropertyId("name");
        field.setTokenCaptionMode(mode);
        field.setTokenCaption(IN, "Explicit A");
        field.setTokenCaption(OUT, "Explicit X");
    }

    private static void addTokens(TestTokenField field) {
        field.addToken(IN);
        field.addToken(OUT);
    }

    private static TestTokenField configureFirst(ItemCaptionMode mode,
            IndexedContainer c) {
        TestTokenField field = new TestTokenField();
        configure(field, mode, c);
        addTokens(field);
        return field;
    }

    private static TestTokenField valueFirst(ItemCaptionMode mode,
            IndexedContainer c) {
        TestTokenField field = new TestTokenField();
        addTokens(field);
        configure(field, mode, c);
        return field;
    }

    // ------------------------------------------------------------------
    // Order independence
    // ------------------------------------------------------------------

    @ParameterizedTest
    @EnumSource(ItemCaptionMode.class)
    void captionsDoNotDependOnConfigurationOrder(ItemCaptionMode mode) {
        IndexedContainer c = container();
        assertWithMessage("Configuring the value before or after the container"
                + " must give the same captions in " + mode + " mode")
                .that(captions(valueFirst(mode, c)))
                .isEqualTo(captions(configureFirst(mode, c)));
    }

    @ParameterizedTest
    @EnumSource(ItemCaptionMode.class)
    void captionsSurviveASecondRefresh(ItemCaptionMode mode) {
        TestTokenField field = configureFirst(mode, container());
        Map<Object, String> before = captions(field);

        field.refreshTokens();

        assertWithMessage("configureTokenButton must be idempotent")
                .that(captions(field)).isEqualTo(before);
    }

    // ------------------------------------------------------------------
    // Per-mode expectations
    // ------------------------------------------------------------------

    /**
     * The full mode table. {@code INDEX} resolving to {@code -1} for a token the
     * container does not hold is {@link IndexedContainer#indexOfId(Object)}
     * speaking, exactly as it does for a Vaadin select; that mode is only
     * meaningful for tokens the container holds.
     */
    @ParameterizedTest
    @CsvSource({
            "ID,                   id-a,       id-x",
            "ID_TOSTRING,          id-a,       id-x",
            "INDEX,                0,          -1",
            "EXPLICIT_DEFAULTS_ID, Explicit A, Explicit X",
            "EXPLICIT,             Explicit A, Explicit X",
            "ICON_ONLY,            id-a,       id-x",
            "PROPERTY,             Alpha,      id-x",
    })
    void captionPerMode(ItemCaptionMode mode, String expectedIn,
            String expectedOut) {
        TestTokenField field = configureFirst(mode, container());

        assertWithMessage("token in the container, " + mode + " mode")
                .that(caption(field, IN)).isEqualTo(expectedIn);
        assertWithMessage("token outside the container, " + mode + " mode")
                .that(caption(field, OUT)).isEqualTo(expectedOut);
    }

    @Test
    void itemModeUsesTheItemForContainedTokensAndFallsBackForTheRest() {
        IndexedContainer c = container();
        TestTokenField field = configureFirst(ItemCaptionMode.ITEM, c);

        assertThat(caption(field, IN)).isEqualTo(c.getItem(IN).toString());
        assertWithMessage("No Item to render, so the tokenId carries the token")
                .that(caption(field, OUT)).isEqualTo(OUT);
    }

    // ------------------------------------------------------------------
    // The empty-caption fallback
    // ------------------------------------------------------------------

    @Test
    void getTokenCaptionFollowsTheModeAndMayBeEmpty() {
        TestTokenField field = new TestTokenField();
        field.setTokenCaptionMode(ItemCaptionMode.EXPLICIT);

        assertWithMessage("getTokenCaption must not second-guess the mode")
                .that(field.getTokenCaption("no-caption")).isEmpty();
    }

    /**
     * ITEM and PROPERTY read the caption off the container item, so a select
     * answers with the empty string for an id it does not hold. A token outside
     * the container is a supported case here, so those two modes stand it in
     * with the tokenId - the one documented deviation from AbstractSelect.
     */
    @ParameterizedTest
    @EnumSource(value = ItemCaptionMode.class, names = { "ITEM", "PROPERTY" })
    void containerBackedModesNameATokenTheContainerDoesNotHold(
            ItemCaptionMode mode) {
        TestTokenField field = configureFirst(mode, container());

        assertWithMessage(mode + " must not leave an outside token nameless")
                .that(field.getTokenCaption(OUT)).isEqualTo(OUT);
    }

    /**
     * The deviation is scoped to tokens the container does not hold. A token it
     * does hold keeps the select's answer — for PROPERTY with an unset property
     * that is the empty string, and the tokenId must not stand in for it.
     */
    @Test
    void aContainedTokenWithAnUnsetPropertyKeepsItsEmptyCaption() {
        IndexedContainer c = new IndexedContainer();
        c.addContainerProperty("name", String.class, null);
        c.addItem(IN); // "name" stays null

        TestTokenField field = new TestTokenField();
        field.setContainerDataSource(c);
        field.setTokenCaptionPropertyId("name");
        field.addToken(IN);

        assertWithMessage("The fallback is about absence from the container,"
                + " not about an empty property value")
                .that(field.getTokenCaption(IN)).isEmpty();
        assertWithMessage("The button still needs something to show")
                .that(caption(field, IN)).isEqualTo(IN);
    }

    @ParameterizedTest
    @EnumSource(value = ItemCaptionMode.class, names = { "EXPLICIT",
            "ICON_ONLY" })
    void modesThatAreEmptyByDesignStayEmptyOutsideTheContainer(
            ItemCaptionMode mode) {
        TestTokenField field = new TestTokenField();
        field.setTokenCaptionMode(mode);

        assertWithMessage(mode + " resolves to nothing by design, not for lack"
                + " of a container item")
                .that(field.getTokenCaption(OUT)).isEmpty();
    }

    @Test
    void anIconDoesNotSuppressTheNameOfAnOutsideToken() {
        TestTokenField field = new TestTokenField();
        field.setTokenCaptionPropertyId("name");
        field.setTokenIcon(OUT, new ThemeResource("icons/token.png"));
        field.addToken(OUT);

        assertWithMessage("PROPERTY now yields a caption, so the icon-only"
                + " shortcut in getTokenButtonCaption does not apply")
                .that(caption(field, OUT)).isEqualTo(OUT);
    }

    @Test
    void anIconCarriesTheTokenSoTheCaptionStaysEmpty() {
        TestTokenField field = new TestTokenField();
        field.setTokenCaptionMode(ItemCaptionMode.ICON_ONLY);
        field.setTokenIcon(OUT, new ThemeResource("icons/token.png"));
        field.addToken(OUT);

        assertWithMessage("ICON_ONLY must not be overridden by the fallback")
                .that(caption(field, OUT)).isEmpty();
    }

    @Test
    void withoutCaptionOrIconTheTokenIdIsShown() {
        TestTokenField field = new TestTokenField();
        field.setTokenCaptionMode(ItemCaptionMode.ICON_ONLY);
        field.addToken(OUT);

        assertWithMessage("A token must never render as an anonymous chip")
                .that(caption(field, OUT)).isEqualTo(OUT);
    }

    // ------------------------------------------------------------------
    // Following the data source
    // ------------------------------------------------------------------

    @Test
    void anItemAddedToTheContainerLaterUpdatesItsToken() {
        IndexedContainer c = new IndexedContainer();
        c.addContainerProperty("name", String.class, null);
        TestTokenField field = new TestTokenField();
        field.setContainerDataSource(c);
        field.setTokenCaptionPropertyId("name");
        field.addToken(IN);

        assertThat(caption(field, IN)).isEqualTo(IN);

        c.addItem(IN).getItemProperty("name").setValue("Alpha");

        assertThat(caption(field, IN)).isEqualTo("Alpha");
    }

    @Test
    void swappingTheContainerRederivesTheTokens() {
        TestTokenField field = new TestTokenField();
        field.setTokenCaptionPropertyId("name");
        field.setContainerDataSource(container());
        addTokens(field);
        assertThat(caption(field, IN)).isEqualTo("Alpha");

        IndexedContainer replacement = new IndexedContainer();
        replacement.addContainerProperty("name", String.class, null);
        replacement.addItem(IN).getItemProperty("name").setValue("Beta");
        field.setContainerDataSource(replacement);

        assertThat(caption(field, IN)).isEqualTo("Beta");
        assertWithMessage("A token outside the container must stay a token")
                .that(field.getTokenButtons()).containsKey(OUT);
        assertThat(caption(field, OUT)).isEqualTo(OUT);
    }

    @Test
    void theOldContainerNoLongerDrivesTheTokens() {
        IndexedContainer old = container();
        TestTokenField field = new TestTokenField();
        field.setTokenCaptionPropertyId("name");
        field.setContainerDataSource(old);
        field.addToken(IN);

        IndexedContainer replacement = new IndexedContainer();
        replacement.addContainerProperty("name", String.class, null);
        replacement.addItem(IN).getItemProperty("name").setValue("Beta");
        field.setContainerDataSource(replacement);

        old.getContainerProperty(IN, "name").setValue("Stale");

        assertWithMessage("Listeners on the replaced container must be dropped")
                .that(caption(field, IN)).isEqualTo("Beta");
    }

    @Test
    void aRemovedTokenStopsFollowingTheContainer() {
        IndexedContainer c = container();
        TestTokenField field = new TestTokenField();
        field.setTokenCaptionPropertyId("name");
        field.setContainerDataSource(c);
        field.addToken(IN);
        field.removeToken(IN);

        c.getContainerProperty(IN, "name").setValue("Renamed");

        assertThat(field.getTokenButtons()).isEmpty();
    }

    @Test
    void refreshTokensPicksUpChangesTheFieldCannotObserve() {
        TestTokenField field = new TestTokenField();
        field.addToken(IN);
        // Bypasses TokenField's own setter, so nothing invalidated the button
        field.getComboBox().setItemCaption(IN, "Alpha");
        assertThat(caption(field, IN)).isEqualTo(IN);

        field.refreshTokens();

        assertThat(caption(field, IN)).isEqualTo("Alpha");
    }

    // ------------------------------------------------------------------
    // Icons
    // ------------------------------------------------------------------

    @Test
    void iconsFollowTheContainerJustLikeCaptions() {
        Resource first = new ThemeResource("icons/first.png");
        Resource second = new ThemeResource("icons/second.png");
        IndexedContainer c = new IndexedContainer();
        c.addContainerProperty("icon", Resource.class, null);
        c.addItem(IN).getItemProperty("icon").setValue(first);

        TestTokenField field = new TestTokenField();
        field.addToken(IN);
        field.setContainerDataSource(c);
        field.setTokenIconPropertyId("icon");

        assertWithMessage("Icon property set after the token was created")
                .that(field.getTokenButtons().get(IN).getIcon())
                .isSameInstanceAs(first);

        c.getContainerProperty(IN, "icon").setValue(second);

        assertWithMessage("Icon property value change must reach the button")
                .that(field.getTokenButtons().get(IN).getIcon())
                .isSameInstanceAs(second);
    }

    // ------------------------------------------------------------------
    // The documented override point
    // ------------------------------------------------------------------

    @Test
    void configureTokenButtonOverrideIsUsedOnEveryRefresh() {
        class CustomField extends TestTokenField {
            private static final long serialVersionUID = 1L;

            @Override
            protected void configureTokenButton(Object tokenId, Button button) {
                super.configureTokenButton(tokenId, button);
                button.setCaption("[" + getTokenButtonCaption(tokenId) + "]");
            }
        }

        CustomField field = new CustomField();
        field.addToken(IN);
        assertThat(field.getTokenButtons().get(IN).getCaption())
                .isEqualTo("[id-a]");

        field.setTokenCaption(IN, "Alpha");

        assertWithMessage("A refresh must go through the subclass hook")
                .that(field.getTokenButtons().get(IN).getCaption())
                .isEqualTo("[Alpha]");
    }
}
