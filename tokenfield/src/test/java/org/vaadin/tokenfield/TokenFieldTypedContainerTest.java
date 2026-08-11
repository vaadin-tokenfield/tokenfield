package org.vaadin.tokenfield;

import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.data.util.IndexedContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

/**
 * A container that refuses ids it cannot hold (issue #24): a typed container
 * such as {@code JPAContainer} answers an id of the wrong type by throwing
 * rather than by reporting it as absent. Caption resolution must treat that as
 * "not in the container" instead of failing.
 */
class TokenFieldTypedContainerTest {

    private TestTokenField field;

    @BeforeEach
    void setup() {
        field = new TestTokenField();
        field.setContainerDataSource(new TypedContainer());
        field.setTokenCaptionPropertyId("name");
    }

    @Test
    void captionSurvivesAContainerThatRejectsForeignIds() {
        assertWithMessage("A container that throws on a foreign id must not break the caption")
                .that(field.getTokenCaption("new@example.com")).isEqualTo("new@example.com");
    }

    @Test
    void tokenButtonSurvivesAContainerThatRejectsForeignIds() {
        field.addToken("new@example.com");

        assertThat(field.getTokenButtons().get("new@example.com").getCaption())
                .contains("new@example.com");
    }

    @Test
    void anIdTheContainerHoldsStillResolvesThroughIt() {
        assertWithMessage("The guard must not swallow a legitimate lookup")
                .that(field.getTokenCaption(Long.valueOf(1L))).isEqualTo("One");
    }

    /** Stands in for a typed container such as JPAContainer. */
    private static class TypedContainer extends IndexedContainer {

        private static final long serialVersionUID = 1L;

        @SuppressWarnings("unchecked")
        private TypedContainer() {
            addContainerProperty("name", String.class, null);
            addItem(Long.valueOf(1L)).getItemProperty("name").setValue("One");
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
        public Property<?> getContainerProperty(Object itemId, Object propertyId) {
            reject(itemId);
            return super.getContainerProperty(itemId, propertyId);
        }

        @Override
        public Item getItem(Object itemId) {
            reject(itemId);
            return super.getItem(itemId);
        }
    }
}
