package org.vaadin.tokenfield;

import com.vaadin.data.util.IndexedContainer;
import com.vaadin.ui.AbstractSelect.ItemCaptionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

/**
 * Pins {@link TokenField#getTokenCaption(Object)} across every
 * {@link ItemCaptionMode}, for tokens the container holds and for tokens it does
 * not - the latter being a supported case of this component, and the one
 * {@code AbstractSelect} has no answer for.
 */
class TokenFieldCaptionModeTest {

    private TestTokenField field;
    private IndexedContainer container;

    @BeforeEach
    void setup() {
        field = new TestTokenField();
        container = new IndexedContainer();
        container.addContainerProperty("name", String.class, null);
        container.addItem("known");
        container.getContainerProperty("known", "name").setValue("Known Name");
        field.setContainerDataSource(container);
    }

    // -----------------------------------------------------------------------
    // Tokens the container does not hold
    // -----------------------------------------------------------------------

    @ParameterizedTest
    @EnumSource(ItemCaptionMode.class)
    void unknownTokenFallsBackToItsId(ItemCaptionMode mode) {
        field.setTokenCaptionMode(mode);

        String expected = mode == ItemCaptionMode.ICON_ONLY ? "" : "ghost";
        assertWithMessage("Caption for a token outside the container, in mode " + mode)
                .that(field.getTokenCaption("ghost")).isEqualTo(expected);
    }

    /**
     * The explicit caption is the fallback for a token the data source cannot
     * caption. It does not apply to {@code ICON_ONLY}, which hides captions, nor
     * to the two id modes, which resolve from the id itself and so always
     * succeed - exactly as {@code AbstractSelect} ignores explicit captions
     * there.
     */
    @ParameterizedTest
    @EnumSource(ItemCaptionMode.class)
    void unknownTokenUsesItsExplicitCaption(ItemCaptionMode mode) {
        field.setTokenCaption("ghost", "Ghost Token");
        field.setTokenCaptionMode(mode);

        String expected;
        if (mode == ItemCaptionMode.ICON_ONLY) {
            expected = "";
        } else if (mode == ItemCaptionMode.ID || mode == ItemCaptionMode.ID_TOSTRING) {
            expected = "ghost";
        } else {
            expected = "Ghost Token";
        }
        assertWithMessage("Explicit caption must apply outside the container, in mode " + mode)
                .that(field.getTokenCaption("ghost")).isEqualTo(expected);
    }

    @ParameterizedTest
    @EnumSource(ItemCaptionMode.class)
    void captionIsNeverNull(ItemCaptionMode mode) {
        field.setTokenCaptionMode(mode);
        assertThat(field.getTokenCaption(null)).isEmpty();
    }

    // -----------------------------------------------------------------------
    // Tokens the container holds: each mode resolves as AbstractSelect does
    // -----------------------------------------------------------------------

    @Test
    void idModeUsesTheId() {
        field.setTokenCaptionMode(ItemCaptionMode.ID);
        assertThat(field.getTokenCaption("known")).isEqualTo("known");
    }

    @Test
    void idToStringModeUsesTheId() {
        field.setTokenCaptionMode(ItemCaptionMode.ID_TOSTRING);
        assertThat(field.getTokenCaption("known")).isEqualTo("known");
    }

    @Test
    void explicitDefaultsIdModePrefersTheExplicitCaption() {
        field.setTokenCaption("known", "Explicit");
        assertThat(field.getTokenCaptionMode()).isEqualTo(ItemCaptionMode.EXPLICIT_DEFAULTS_ID);
        assertThat(field.getTokenCaption("known")).isEqualTo("Explicit");
    }

    @Test
    void explicitDefaultsIdModeFallsBackToTheId() {
        assertThat(field.getTokenCaption("known")).isEqualTo("known");
    }

    @Test
    void explicitModeUsesTheExplicitCaption() {
        field.setTokenCaptionMode(ItemCaptionMode.EXPLICIT);
        field.setTokenCaption("known", "Explicit");
        assertThat(field.getTokenCaption("known")).isEqualTo("Explicit");
    }

    @Test
    void indexModeUsesTheContainerIndex() {
        field.setTokenCaptionMode(ItemCaptionMode.INDEX);
        assertThat(field.getTokenCaption("known")).isEqualTo("0");
    }

    @Test
    void indexModeFallsBackRatherThanReportingMinusOne() {
        field.setTokenCaptionMode(ItemCaptionMode.INDEX);
        assertWithMessage("An id the container does not hold must not surface as \"-1\"")
                .that(field.getTokenCaption("ghost")).isEqualTo("ghost");
    }

    @Test
    void propertyModeUsesTheCaptionProperty() {
        field.setTokenCaptionPropertyId("name");
        assertThat(field.getTokenCaption("known")).isEqualTo("Known Name");
    }

    @Test
    void propertyModeFallsBackWhenThePropertyIsEmpty() {
        container.addItem("nameless");
        field.setTokenCaptionPropertyId("name");
        assertWithMessage("An item whose caption property is null must still be readable")
                .that(field.getTokenCaption("nameless")).isEqualTo("nameless");
    }

    @Test
    void itemModeUsesTheItemItself() {
        field.setTokenCaptionMode(ItemCaptionMode.ITEM);
        assertThat(field.getTokenCaption("known"))
                .isEqualTo(container.getItem("known").toString());
    }

    @Test
    void iconOnlyModeHidesTheCaption() {
        field.setTokenCaptionMode(ItemCaptionMode.ICON_ONLY);
        field.setTokenCaption("known", "Explicit");
        assertWithMessage("ICON_ONLY means captions are hidden, explicit ones included")
                .that(field.getTokenCaption("known")).isEmpty();
    }

    // -----------------------------------------------------------------------
    // A container that refuses ids it cannot hold (issue #24)
    // -----------------------------------------------------------------------

    @Test
    void captionSurvivesAContainerThatRejectsForeignIds() {
        field.setContainerDataSource(new TypedContainer());
        field.setTokenCaptionPropertyId("name");

        assertWithMessage("A container that throws on a foreign id must not break the caption")
                .that(field.getTokenCaption("new@example.com")).isEqualTo("new@example.com");
    }

    @Test
    void tokenButtonSurvivesAContainerThatRejectsForeignIds() {
        field.setContainerDataSource(new TypedContainer());
        field.setTokenCaptionPropertyId("name");
        field.addToken("new@example.com");

        assertThat(field.getTokenButtons().get("new@example.com").getCaption())
                .contains("new@example.com");
    }

    /**
     * Stands in for a typed container such as JPAContainer, which answers an id
     * of the wrong type by throwing rather than by reporting it as absent.
     */
    private static class TypedContainer extends IndexedContainer {

        private static final long serialVersionUID = 1L;

        private TypedContainer() {
            addContainerProperty("name", String.class, null);
            addItem(Long.valueOf(1L));
        }

        private static void reject(Object itemId) {
            if (!(itemId instanceof Long)) {
                throw new IllegalArgumentException(
                        "The object [" + itemId + "] could not be converted to Long");
            }
        }

        @Override
        public boolean containsId(Object itemId) {
            reject(itemId);
            return super.containsId(itemId);
        }

        @Override
        public com.vaadin.data.Property<?> getContainerProperty(Object itemId,
                Object propertyId) {
            reject(itemId);
            return super.getContainerProperty(itemId, propertyId);
        }

        @Override
        public com.vaadin.data.Item getItem(Object itemId) {
            reject(itemId);
            return super.getItem(itemId);
        }
    }
}
