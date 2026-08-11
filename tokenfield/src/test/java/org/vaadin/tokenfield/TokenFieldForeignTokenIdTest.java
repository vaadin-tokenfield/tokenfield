package org.vaadin.tokenfield;

import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.server.Resource;
import com.vaadin.ui.AbstractSelect.ItemCaptionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

/**
 * A token may be one the container cannot hold at all. Containers keyed by a
 * specific type answer a lookup for such an id by throwing rather than by
 * reporting it absent - a {@code JPAContainer} keyed by {@code Long}, asked
 * about a {@code String}, fails converting it. TokenField must still render
 * those tokens.
 */
class TokenFieldForeignTokenIdTest {

    /** Stands in for a typed container: only {@link Long} ids are answerable. */
    private static class LongKeyedContainer extends IndexedContainer {

        private static final long serialVersionUID = 1L;

        private void requireLongId(Object itemId) {
            if (!(itemId instanceof Long)) {
                throw new IllegalArgumentException("The object [" + itemId
                        + "] could not be converted to [class java.lang.Long]");
            }
        }

        @Override
        public Item getItem(Object itemId) {
            requireLongId(itemId);
            return super.getItem(itemId);
        }

        @Override
        public Property<?> getContainerProperty(Object itemId,
                Object propertyId) {
            requireLongId(itemId);
            return super.getContainerProperty(itemId, propertyId);
        }
    }

    private static final Long CONTAINED = 1L;
    private static final String FOREIGN = "new@example.com";

    private TestTokenField field;
    private LongKeyedContainer container;

    @BeforeEach
    void setup() {
        container = new LongKeyedContainer();
        container.addContainerProperty("name", String.class, null);
        container.addItem(CONTAINED).getItemProperty("name").setValue("Alpha");

        field = new TestTokenField();
        field.setContainerDataSource(container);
        field.setTokenCaptionPropertyId("name");
    }

    private String caption(Object tokenId) {
        return field.getTokenButtons().get(tokenId).getCaption()
                .replaceAll(" ×$", "");
    }

    @Test
    void aTokenTheContainerCannotHoldStillRenders() {
        field.addToken(CONTAINED);
        field.addToken(FOREIGN);

        assertThat(caption(CONTAINED)).isEqualTo("Alpha");
        assertWithMessage("A token id the container rejects falls back to itself")
                .that(caption(FOREIGN)).isEqualTo(FOREIGN);
    }

    @Test
    void getTokenCaptionDoesNotPropagateTheContainersRefusal() {
        // The refusal reads as "not contained", so the PROPERTY-mode fallback
        // for tokens outside the container applies
        assertThat(field.getTokenCaption(FOREIGN)).isEqualTo(FOREIGN);
    }

    @Test
    void aContainedTokenIsStillRecognisedAsContained() {
        // The membership check reads a refusal as "not contained", so it must
        // not turn every token into an outsider
        assertThat(field.getTokenCaption(CONTAINED)).isEqualTo("Alpha");
    }

    @Test
    void itemModeAlsoToleratesAForeignTokenId() {
        field.setTokenCaptionMode(ItemCaptionMode.ITEM);
        field.addToken(CONTAINED);
        field.addToken(FOREIGN);

        assertThat(caption(CONTAINED))
                .isEqualTo(container.getItem(CONTAINED).toString());
        assertThat(caption(FOREIGN)).isEqualTo(FOREIGN);
    }

    @Test
    void anIconPropertyIsResolvedWithoutPropagatingTheRefusal() {
        container.addContainerProperty("icon", Resource.class, null);
        field.setTokenIconPropertyId("icon");
        field.addToken(FOREIGN);

        assertThat(field.getTokenIcon(FOREIGN)).isNull();
        assertThat(caption(FOREIGN)).isEqualTo(FOREIGN);
    }

    @Test
    void containedTokensStillFollowTheirCaptionProperty() {
        field.addToken(CONTAINED);
        field.addToken(FOREIGN);

        container.getItem(CONTAINED).getItemProperty("name").setValue("Renamed");

        assertWithMessage("A foreign token must not stop the others tracking")
                .that(caption(CONTAINED)).isEqualTo("Renamed");
    }
}
