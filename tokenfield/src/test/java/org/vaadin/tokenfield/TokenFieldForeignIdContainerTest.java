package org.vaadin.tokenfield;

import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.server.Resource;
import com.vaadin.server.ThemeResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;

/**
 * Reproduces <a
 * href="https://github.com/vaadin-tokenfield/tokenfield/issues/24">issue
 * #24</a>: a token entered by typing is the text the user typed, and handing
 * that text to a container that is keyed by something else used to kill the
 * request.
 *
 * <p>The container here stands in for a {@code JPAContainer}, which the issue
 * was reported against. Both of the ways such a container answers an id it
 * cannot hold are reproduced, and neither is the {@code false} that
 * {@link com.vaadin.data.Container#containsId(Object)} is specified to
 * return:</p>
 *
 * <ul>
 * <li>a lookup fails converting the text to an id — the
 * {@code NumberFormatException} at the bottom of the reported stack trace,
 * wrapped by JPAContainer in a {@code PersistenceException};</li>
 * <li>{@code addItem(Object)} refuses an id it did not generate — JPAContainer
 * supports {@code addItem()} and {@code addEntity()} only.</li>
 * </ul>
 *
 * <p>Standing in for it rather than depending on it keeps these tests in the
 * add-on module, where there is no JPA on the classpath, and pins the
 * behaviour rather than one add-on's implementation of it.</p>
 */
class TokenFieldForeignIdContainerTest {

    private static final String NAME = "name";
    private static final String ICON = "icon";

    /** In the address book, so its id is a token id the container can hold. */
    private static final Long NATHAN = Long.valueOf(1);

    /** Typed in full, so the token id is this text and not an entity id. */
    private static final String TYPED = "Nathan Einstein";

    private ContactContainer contacts;
    private TestTokenField field;

    @BeforeEach
    void setup() {
        contacts = new ContactContainer();
        contacts.addContainerProperty(NAME, String.class, "");
        contacts.addContainerProperty(ICON, Resource.class, null);
        contacts.seed(NATHAN, TYPED);

        field = new TestTokenField();
        field.setContainerDataSource(contacts);
        field.setTokenCaptionPropertyId(NAME);
    }

    // -----------------------------------------------------------------------
    // getTokenCaption — the half of #24 that threw
    // -----------------------------------------------------------------------

    @Test
    void captionOfATypedTokenIsTheTypedText() {
        assertThat(field.getTokenCaption(TYPED)).isEqualTo(TYPED);
    }

    @Test
    void captionOfAContainedTokenStillComesFromTheCaptionProperty() {
        assertThat(field.getTokenCaption(NATHAN)).isEqualTo(TYPED);
    }

    @Test
    void typingATokenAddsIt() {
        field.simulateNewItemInput(TYPED);

        assertThat(field.getValue()).containsExactly(TYPED);
        assertThat(field.getTokenButtons().keySet()).containsExactly(TYPED);
        assertThat(field.getTokenButtons().get(TYPED).getCaption())
                .startsWith(TYPED);
    }

    @Test
    void aTokenPickedFromTheSuggestionsIsUnaffected() {
        field.simulateSelect(NATHAN);

        assertThat(field.getValue()).containsExactly(NATHAN);
        assertThat(field.getTokenButtons().get(NATHAN).getCaption())
                .startsWith(TYPED);
    }

    // -----------------------------------------------------------------------
    // getTokenIcon — the same lookup, one line further down
    // configureTokenButton
    // -----------------------------------------------------------------------

    @Test
    void typedTokenHasNoIconRatherThanFailingToLookOneUp() {
        field.setTokenIconPropertyId(ICON);

        assertThat(field.getTokenIcon(TYPED)).isNull();
    }

    @Test
    void anIconSetOnTheFieldIsStillFoundForATypedToken() {
        Resource icon = new ThemeResource("icons/token.png");
        field.setTokenIconPropertyId(ICON);
        field.setTokenIcon(TYPED, icon);

        assertThat(field.getTokenIcon(TYPED)).isSameInstanceAs(icon);
    }

    @Test
    void iconOfAContainedTokenStillComesFromTheIconProperty() {
        Resource icon = new ThemeResource("icons/contact.png");
        contacts.seedIcon(NATHAN, icon);
        field.setTokenIconPropertyId(ICON);

        assertThat(field.getTokenIcon(NATHAN)).isSameInstanceAs(icon);
    }

    // -----------------------------------------------------------------------
    // rememberToken — the other half, which cannot work over such a container
    // -----------------------------------------------------------------------

    @Test
    void aContainerThatWillNotTakeTheTokenStillGetsTheToken() {
        field.simulateNewItemInput(TYPED);

        assertThat(field.getValue()).containsExactly(TYPED);
    }

    @Test
    void aContainerThatWillNotTakeTheTokenIsLeftAsItWas() {
        field.simulateNewItemInput(TYPED);

        assertThat(contacts.getItemIds()).containsExactly(NATHAN);
        assertThat(contacts.getContainerProperty(NATHAN, NAME).getValue())
                .isEqualTo(TYPED);
    }

    /**
     * A container keyed by an id of its own making, in the two ways that
     * matter to {@link TokenField}.
     * <p>
     * {@link IndexedContainer} supplies the storage; the overrides supply the
     * behaviour under test. Seeding goes through {@link #seed} because
     * {@link #addItem(Object)} is exactly what such a container refuses.
     * </p>
     */
    private static class ContactContainer extends IndexedContainer {

        private static final long serialVersionUID = 1L;

        void seed(Long entityId, String name) {
            super.addItem(entityId);
            super.getContainerProperty(entityId, NAME).setValue(name);
        }

        void seedIcon(Long entityId, Resource icon) {
            super.getContainerProperty(entityId, ICON).setValue(icon);
        }

        @Override
        public boolean containsId(Object itemId) {
            return super.containsId(asEntityId(itemId));
        }

        @Override
        public Item getItem(Object itemId) {
            return super.getItem(asEntityId(itemId));
        }

        @Override
        public Property<?> getContainerProperty(Object itemId,
                Object propertyId) {
            return super.getContainerProperty(asEntityId(itemId), propertyId);
        }

        @Override
        public Item addItem(Object itemId) {
            throw new UnsupportedOperationException(
                    "ids are generated; use addItem() or addEntity()");
        }

        /**
         * Converts the way a container keyed by an entity id does, which for
         * anything that is not one means failing rather than answering.
         */
        private static Object asEntityId(Object itemId) {
            if (itemId == null || itemId instanceof Long) {
                return itemId;
            }
            return Long.valueOf(String.valueOf(itemId));
        }
    }
}
