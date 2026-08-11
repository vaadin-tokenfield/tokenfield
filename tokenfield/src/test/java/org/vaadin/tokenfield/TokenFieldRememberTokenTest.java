package org.vaadin.tokenfield;

import com.vaadin.data.Item;
import com.vaadin.data.util.IndexedContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests {@link TokenField#rememberToken(String)} via the NewItemHandler path
 * ({@link TestTokenField#simulateNewItemInput(String)}), over containers that
 * take an explicit item id and over containers that assign ids themselves.
 */
class TokenFieldRememberTokenTest {

    private TestTokenField field;

    @BeforeEach
    void setup() {
        field = new TestTokenField();
    }

    // ------------------------------------------------------------------
    // A container that takes the typed text as the item id
    // ------------------------------------------------------------------

    @Test
    void newTokenIsAddedToContainer() {
        field.simulateNewItemInput("tag1");
        assertThat(field.getComboBox().containsId("tag1")).isTrue();
    }

    @Test
    void existingTokenIsNotDuplicated() {
        field.getContainerDataSource().addItem("dup");
        assertThat(field.getComboBox().getItemIds()).hasSize(1);
        field.simulateNewItemInput("dup");
        assertThat(field.getComboBox().getItemIds()).hasSize(1);
    }

    @Test
    void rememberNewTokensFalseSkipsContainer() {
        field.setRememberNewTokens(false);
        field.simulateNewItemInput("volatile");
        assertThat(field.getComboBox().containsId("volatile")).isFalse();
    }

    @Test
    void theCaptionPropertyIsFilledUnderTheIdTheItemWasAddedWith() {
        IndexedContainer c = new IndexedContainer();
        c.addContainerProperty("name", String.class, null);
        field.setContainerDataSource(c);
        field.setTokenCaptionPropertyId("name");
        // A caption that differs from the id used to break this path
        field.setTokenCaption("tag1", "Unrelated");

        field.simulateNewItemInput("tag1");

        assertThat(field.getComboBox().containsId("tag1")).isTrue();
        assertThat(c.getItem("tag1").getItemProperty("name").getValue())
                .isEqualTo("tag1");
    }

    // ------------------------------------------------------------------
    // A container that assigns its own ids
    // ------------------------------------------------------------------

    @Test
    void aContainerThatAssignsIdsGetsTheTypedTextInItsCaptionProperty() {
        GeneratedIdContainer c = new GeneratedIdContainer();
        field.setContainerDataSource(c);
        field.setTokenCaptionPropertyId("name");

        field.simulateNewItemInput("new@example.com");

        assertWithMessage("Container.addItem() is the second path to try")
                .that(c.getItemIds()).containsExactly(1L);
        assertThat(c.getItem(1L).getItemProperty("name").getValue())
                .isEqualTo("new@example.com");
    }

    @Test
    void theTokenKeepsTheTypedTextWhenTheContainerAssignsIds() {
        GeneratedIdContainer c = new GeneratedIdContainer();
        field.setContainerDataSource(c);
        field.setTokenCaptionPropertyId("name");

        field.simulateNewItemInput("new@example.com");

        assertWithMessage("The token is not re-pointed at the new item")
                .that(field.getValue()).containsExactly("new@example.com");
        assertThat(field.getTokenButtons().keySet())
                .containsExactly("new@example.com");
    }

    @Test
    void theSameTextTypedTwiceAddsASecondItemWhenTheContainerAssignsIds() {
        GeneratedIdContainer c = new GeneratedIdContainer();
        field.setContainerDataSource(c);
        field.setTokenCaptionPropertyId("name");

        field.simulateNewItemInput("new@example.com");
        field.simulateNewItemInput("new@example.com");

        assertWithMessage("Documented: an id cannot answer whether the text is"
                + " already there, and rememberToken does not go looking")
                .that(c.getItemIds()).containsExactly(1L, 2L).inOrder();
    }

    @Test
    void withoutACaptionPropertyTheGeneratedItemStaysBlank() {
        GeneratedIdContainer c = new GeneratedIdContainer();
        field.setContainerDataSource(c);

        field.simulateNewItemInput("new@example.com");

        assertThat(c.getItemIds()).containsExactly(1L);
        assertWithMessage("Nowhere to put the typed text")
                .that(c.getItem(1L).getItemProperty("name").getValue())
                .isNull();
    }

    @Test
    void aContainerThatSupportsNeitherWaySaysSo() {
        field.setContainerDataSource(new NoAddContainer());

        assertWithMessage("Nothing to remember, so setRememberNewTokens(false)"
                + " is the answer - not a silent no-op")
                .that(assertThrows(UnsupportedOperationException.class,
                        () -> field.simulateNewItemInput("new@example.com")))
                .isNotNull();
    }

    /**
     * Refuses an explicit item id and assigns {@code Long} ids of its own, the
     * way a {@code JPAContainer} does.
     */
    private static class GeneratedIdContainer extends IndexedContainer {
        private static final long serialVersionUID = 1L;

        private final AtomicLong nextId = new AtomicLong();

        GeneratedIdContainer() {
            addContainerProperty("name", String.class, null);
        }

        @Override
        public Item addItem(Object itemId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object addItem() {
            Long itemId = nextId.incrementAndGet();
            return super.addItem(itemId) == null ? null : itemId;
        }
    }

    /** Supports neither way of adding an item. */
    private static class NoAddContainer extends IndexedContainer {
        private static final long serialVersionUID = 1L;

        @Override
        public Item addItem(Object itemId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object addItem() {
            throw new UnsupportedOperationException();
        }
    }
}
