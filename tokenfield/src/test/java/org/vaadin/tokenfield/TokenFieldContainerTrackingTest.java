package org.vaadin.tokenfield;

import com.vaadin.data.Container;
import com.vaadin.data.Property;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.server.Resource;
import com.vaadin.server.ThemeResource;
import com.vaadin.ui.AbstractSelect.ItemCaptionMode;
import com.vaadin.ui.Button;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

/**
 * Pins down that a token button follows changes made <em>within</em> the bound
 * container - an item appearing, a caption or icon property changing its value,
 * the property set changing - the way {@code AbstractSelect} follows them for
 * the options it paints.
 *
 * <p>{@link TokenFieldCaptionDerivationTest} covers the other half: deriving a
 * button from the field's own configuration. Here the configuration stands
 * still and the data moves.</p>
 */
class TokenFieldContainerTrackingTest {

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

    private static TestTokenField fieldOver(Container c) {
        TestTokenField field = new TestTokenField();
        field.setContainerDataSource(c);
        field.setTokenCaptionPropertyId("name");
        return field;
    }

    // ------------------------------------------------------------------
    // Item set changes
    // ------------------------------------------------------------------

    @Test
    void anItemAddedLaterNamesTheTokenThatWasWaitingForIt() {
        IndexedContainer c = new IndexedContainer();
        c.addContainerProperty("name", String.class, null);
        TestTokenField field = fieldOver(c);
        field.addToken(IN);
        assertWithMessage("Nothing to name it with yet")
                .that(caption(field, IN)).isEqualTo(IN);

        c.addItem(IN).getItemProperty("name").setValue("Alpha");

        assertThat(caption(field, IN)).isEqualTo("Alpha");
    }

    @Test
    void anItemRemovedFromTheContainerFallsBackToTheTokenId() {
        IndexedContainer c = container();
        TestTokenField field = fieldOver(c);
        field.addToken(IN);
        assertThat(caption(field, IN)).isEqualTo("Alpha");

        c.removeItem(IN);

        assertWithMessage("A token outlives the item it was named after")
                .that(caption(field, IN)).isEqualTo(IN);
    }

    @Test
    void aPropertySetChangeReachesTheButtons() {
        IndexedContainer c = new IndexedContainer();
        c.addItem(IN);
        TestTokenField field = fieldOver(c);
        field.addToken(IN);
        assertWithMessage("No such property, so nothing to show yet")
                .that(caption(field, IN)).isEmpty();

        c.addContainerProperty("name", String.class, "Alpha");

        assertThat(caption(field, IN)).isEqualTo("Alpha");
    }

    // ------------------------------------------------------------------
    // Property value changes
    // ------------------------------------------------------------------

    @Test
    void aCaptionPropertyValueChangeReachesTheButton() {
        IndexedContainer c = container();
        TestTokenField field = fieldOver(c);
        field.addToken(IN);

        c.getContainerProperty(IN, "name").setValue("Renamed");

        assertThat(caption(field, IN)).isEqualTo("Renamed");
    }

    @Test
    void anIconPropertyValueChangeReachesTheButton() {
        Resource first = new ThemeResource("icons/first.png");
        Resource second = new ThemeResource("icons/second.png");
        IndexedContainer c = new IndexedContainer();
        c.addContainerProperty("icon", Resource.class, null);
        c.addItem(IN).getItemProperty("icon").setValue(first);

        TestTokenField field = new TestTokenField();
        field.setContainerDataSource(c);
        field.setTokenIconPropertyId("icon");
        field.addToken(IN);
        assertThat(field.getTokenButtons().get(IN).getIcon())
                .isSameInstanceAs(first);

        c.getContainerProperty(IN, "icon").setValue(second);

        assertThat(field.getTokenButtons().get(IN).getIcon())
                .isSameInstanceAs(second);
    }

    @Test
    void itemModeFollowsEveryPropertyOfTheItem() {
        IndexedContainer c = new IndexedContainer();
        c.addContainerProperty("name", String.class, null);
        c.addContainerProperty("note", String.class, null);
        c.addItem(IN).getItemProperty("name").setValue("Alpha");

        TestTokenField field = new TestTokenField();
        field.setContainerDataSource(c);
        field.setTokenCaptionMode(ItemCaptionMode.ITEM);
        field.addToken(IN);

        // ITEM renders the Item's own toString, which any property feeds into,
        // not just the one the caption property id names
        c.getContainerProperty(IN, "note").setValue("Second");

        assertThat(caption(field, IN)).isEqualTo(c.getItem(IN).toString());
    }

    @Test
    void aPropertyOfAnUntrackedModeIsNotFollowed() {
        IndexedContainer c = container();
        TestTokenField field = new TestTokenField();
        field.setContainerDataSource(c);
        field.setTokenCaptionPropertyId("name");
        field.setTokenCaptionMode(ItemCaptionMode.ID);
        field.addToken(IN);

        c.getContainerProperty(IN, "name").setValue("Renamed");

        assertWithMessage("ID mode does not read the container at all")
                .that(caption(field, IN)).isEqualTo(IN);
    }

    // ------------------------------------------------------------------
    // Detaching
    // ------------------------------------------------------------------

    @Test
    void theReplacedContainerNoLongerDrivesTheTokens() {
        IndexedContainer old = container();
        TestTokenField field = fieldOver(old);
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
    void aRemovedTokenStopsDrivingARefresh() {
        IndexedContainer c = container();
        c.addItem(OUT).getItemProperty("name").setValue("Ex");
        CountingField field = new CountingField();
        field.setContainerDataSource(c);
        field.setTokenCaptionPropertyId("name");
        field.addToken(IN);
        field.addToken(OUT);
        field.removeToken(OUT);

        int before = field.configurations;
        c.getContainerProperty(OUT, "name").setValue("Renamed");

        assertWithMessage("The removed token's property must be let go")
                .that(field.configurations).isEqualTo(before);
    }

    /** Counts how often a button is (re-)configured. */
    private static class CountingField extends TestTokenField {
        private static final long serialVersionUID = 1L;

        int configurations;

        @Override
        protected void configureTokenButton(Object tokenId, Button button) {
            super.configureTokenButton(tokenId, button);
            configurations++;
        }
    }

    // ------------------------------------------------------------------
    // Tokens the container cannot be asked about
    // ------------------------------------------------------------------

    /**
     * Tracking a token means asking the container for its properties, and a
     * container keyed by a specific type answers an id it cannot hold by
     * throwing rather than by reporting it absent (#24). Until that is handled
     * in the add-on, {@code containsToken} is the seam an application overrides
     * to keep such ids away from the container - the JPA demo panel does
     * exactly this.
     */
    @Test
    void containsTokenIsTheSeamForAnIdTheContainerRefuses() {
        TypedContainer c = new TypedContainer();
        c.addContainerProperty("name", String.class, null);
        c.addItem(1L).getItemProperty("name").setValue("Alpha");

        class GuardedField extends TestTokenField {
            private static final long serialVersionUID = 1L;

            @Override
            protected boolean containsToken(Object tokenId) {
                return tokenId instanceof Long && super.containsToken(tokenId);
            }
        }

        GuardedField field = new GuardedField();
        field.setContainerDataSource(c);
        field.setTokenCaptionPropertyId("name");
        field.addToken(1L);
        field.addToken("off-book@example.com");

        assertThat(caption(field, 1L)).isEqualTo("Alpha");
        assertThat(caption(field, "off-book@example.com"))
                .isEqualTo("off-book@example.com");

        c.getItem(1L).getItemProperty("name").setValue("Renamed");

        assertWithMessage("The contained token is still tracked")
                .that(caption(field, 1L)).isEqualTo("Renamed");
    }

    /** Refuses ids of the wrong type the way a {@code JPAContainer} does. */
    private static class TypedContainer extends IndexedContainer {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean containsId(Object itemId) {
            requireOwnKeyType(itemId);
            return super.containsId(itemId);
        }

        @Override
        public Property<?> getContainerProperty(Object itemId,
                Object propertyId) {
            requireOwnKeyType(itemId);
            return super.getContainerProperty(itemId, propertyId);
        }

        private static void requireOwnKeyType(Object itemId) {
            if (!(itemId instanceof Long)) {
                throw new IllegalArgumentException("The object [" + itemId
                        + "] could not be converted to [class java.lang.Long]");
            }
        }
    }
}
