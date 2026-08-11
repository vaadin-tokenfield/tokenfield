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
import org.junit.jupiter.params.provider.ValueSource;

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
            IndexedContainer c, boolean withExplicitCaptions) {
        field.setContainerDataSource(c);
        field.setTokenCaptionPropertyId("name");
        field.setTokenCaptionMode(mode);
        if (withExplicitCaptions) {
            field.setTokenCaption(IN, "Explicit A");
            field.setTokenCaption(OUT, "Explicit X");
        }
    }

    private static void configure(TestTokenField field, ItemCaptionMode mode,
            IndexedContainer c) {
        configure(field, mode, c, true);
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

    private static TestTokenField field(ItemCaptionMode mode,
            boolean withExplicitCaptions) {
        TestTokenField field = new TestTokenField();
        configure(field, mode, container(), withExplicitCaptions);
        addTokens(field);
        return field;
    }

    /**
     * What {@link TokenField#getTokenCaption(Object)} answers, per mode, for a
     * token the container holds and one it does not, with and without an
     * explicit caption. Only {@code EXPLICIT} and {@code EXPLICIT_DEFAULTS_ID}
     * read explicit captions at all; the other modes ignore them, which is
     * {@code AbstractSelect}'s switch and not a TokenField decision.
     * <p>
     * {@code INDEX} answering {@code -1} outside the container is
     * {@link IndexedContainer#indexOfId(Object)} speaking, exactly as it does
     * for a Vaadin select; that mode is only meaningful for contained tokens.
     * {@code ITEM} is covered separately - its caption is an Item's toString.
     */
    @ParameterizedTest
    @CsvSource({
            // mode,              in,         out,  in+caption, out+caption
            "ID,                  id-a,       id-x, id-a,       id-x",
            "ID_TOSTRING,         id-a,       id-x, id-a,       id-x",
            "INDEX,               0,          -1,   0,          -1",
            "EXPLICIT_DEFAULTS_ID,id-a,       id-x, Explicit A, Explicit X",
            "EXPLICIT,            '',         '',   Explicit A, Explicit X",
            "ICON_ONLY,           '',         '',   '',         ''",
            "PROPERTY,            Alpha,      id-x, Alpha,      id-x",
    })
    void tokenCaptionPerMode(ItemCaptionMode mode, String in, String out,
            String inWithCaption, String outWithCaption) {
        TestTokenField plain = field(mode, false);
        assertWithMessage("contained token, " + mode)
                .that(plain.getTokenCaption(IN)).isEqualTo(in);
        assertWithMessage("token outside the container, " + mode)
                .that(plain.getTokenCaption(OUT)).isEqualTo(out);

        TestTokenField captioned = field(mode, true);
        assertWithMessage("contained token with explicit caption, " + mode)
                .that(captioned.getTokenCaption(IN)).isEqualTo(inWithCaption);
        assertWithMessage("outside token with explicit caption, " + mode)
                .that(captioned.getTokenCaption(OUT))
                .isEqualTo(outWithCaption);
    }

    /**
     * The same matrix at the button, where the empty-caption fallback applies -
     * no token may render nameless.
     */
    @ParameterizedTest
    @CsvSource({
            // mode,              in,         out,  in+caption, out+caption
            "ID,                  id-a,       id-x, id-a,       id-x",
            "ID_TOSTRING,         id-a,       id-x, id-a,       id-x",
            "INDEX,               0,          -1,   0,          -1",
            "EXPLICIT_DEFAULTS_ID,id-a,       id-x, Explicit A, Explicit X",
            "EXPLICIT,            id-a,       id-x, Explicit A, Explicit X",
            "ICON_ONLY,           id-a,       id-x, id-a,       id-x",
            "PROPERTY,            Alpha,      id-x, Alpha,      id-x",
    })
    void buttonCaptionPerMode(ItemCaptionMode mode, String in, String out,
            String inWithCaption, String outWithCaption) {
        TestTokenField plain = field(mode, false);
        assertWithMessage("contained token, " + mode)
                .that(caption(plain, IN)).isEqualTo(in);
        assertWithMessage("token outside the container, " + mode)
                .that(caption(plain, OUT)).isEqualTo(out);

        TestTokenField captioned = field(mode, true);
        assertWithMessage("contained token with explicit caption, " + mode)
                .that(caption(captioned, IN)).isEqualTo(inWithCaption);
        assertWithMessage("outside token with explicit caption, " + mode)
                .that(caption(captioned, OUT)).isEqualTo(outWithCaption);
    }

    /** ITEM, which the tables above cannot express: the caption is an Item. */
    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void itemModeUsesTheItemForContainedTokensAndFallsBackForTheRest(
            boolean withExplicitCaptions) {
        IndexedContainer c = container();
        TestTokenField field = new TestTokenField();
        configure(field, ItemCaptionMode.ITEM, c, withExplicitCaptions);
        addTokens(field);

        assertWithMessage("ITEM renders the Item, explicit captions are ignored")
                .that(caption(field, IN)).isEqualTo(c.getItem(IN).toString());
        assertWithMessage("No Item to render, so the tokenId carries the token")
                .that(caption(field, OUT)).isEqualTo(OUT);
    }

    // ------------------------------------------------------------------
    // The empty-caption fallback
    // ------------------------------------------------------------------

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
    void refreshTokensPicksUpChangesWithinTheContainer() {
        IndexedContainer c = container();
        TestTokenField field = new TestTokenField();
        field.setTokenCaptionPropertyId("name");
        field.setContainerDataSource(c);
        field.addToken(IN);

        // Changing a value inside the container is not something the field
        // reconfigures on, so the button keeps the caption it was given
        c.getItem(IN).getItemProperty("name").setValue("Renamed");
        assertThat(caption(field, IN)).isEqualTo("Alpha");

        field.refreshTokens();

        assertThat(caption(field, IN)).isEqualTo("Renamed");
    }

    // ------------------------------------------------------------------
    // Icons
    // ------------------------------------------------------------------

    @Test
    void iconsAreDerivedJustLikeCaptions() {
        Resource icon = new ThemeResource("icons/first.png");
        IndexedContainer c = new IndexedContainer();
        c.addContainerProperty("icon", Resource.class, null);
        c.addItem(IN).getItemProperty("icon").setValue(icon);

        TestTokenField field = new TestTokenField();
        field.addToken(IN);
        field.setContainerDataSource(c);
        field.setTokenIconPropertyId("icon");

        assertWithMessage("Icon property id set after the token was created")
                .that(field.getTokenButtons().get(IN).getIcon())
                .isSameInstanceAs(icon);
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
