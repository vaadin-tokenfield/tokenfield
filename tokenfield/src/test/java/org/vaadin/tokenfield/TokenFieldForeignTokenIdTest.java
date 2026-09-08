package org.vaadin.tokenfield;

import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.server.Resource;
import com.vaadin.ui.AbstractSelect.ItemCaptionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

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
        public boolean containsId(Object itemId) {
            requireLongId(itemId);
            return super.containsId(itemId);
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
    void theMembershipCheckReadsTheRefusalAsNotContained() {
        // containsId is asked before the container is read at all, and a typed
        // container refuses a foreign id there too - the caption mode's
        // fallback for an outsider must survive that, not propagate it
        field.setTokenCaptionMode(ItemCaptionMode.ITEM);

        assertThat(field.getTokenCaption(FOREIGN)).isEqualTo(FOREIGN);
        assertWithMessage("Only the refused id is an outsider")
                .that(field.getTokenCaption(CONTAINED))
                .isEqualTo(container.getItem(CONTAINED).toString());
    }

    @Test
    void eachRefusalIsLoggedAtFineNamingTheRejectedId() {
        container.addContainerProperty("icon", Resource.class, null);
        field.setTokenIconPropertyId("icon");

        try (RecordedLog membership = RecordedLog.on(TokenField.class);
                RecordedLog lookup = RecordedLog.on(TokenComboBox.class)) {
            field.getTokenCaption(FOREIGN);
            field.getTokenIcon(FOREIGN);

            assertWithMessage("The membership check logs the id it gave up on")
                    .that(membership.messages()).contains("Container rejected "
                            + "the token id " + FOREIGN
                            + "; treating it as not contained");
            assertWithMessage("So does the lookup behind caption and icon")
                    .that(lookup.messages()).contains("Container rejected "
                            + "the token id " + FOREIGN
                            + "; treating it as not contained");
        }
    }

    /**
     * Captures one logger's {@code FINE} records for the duration of a test.
     * The messages are built by a supplier, so nothing evaluates them unless
     * the level is actually enabled.
     */
    private static final class RecordedLog extends Handler
            implements AutoCloseable {

        private final Logger logger;
        private final Level previousLevel;
        private final boolean previousUseParentHandlers;
        private final List<String> messages = new ArrayList<>();

        private RecordedLog(Logger logger) {
            this.logger = logger;
            this.previousLevel = logger.getLevel();
            this.previousUseParentHandlers = logger.getUseParentHandlers();
        }

        static RecordedLog on(Class<?> loggingClass) {
            RecordedLog recorded = new RecordedLog(
                    Logger.getLogger(loggingClass.getName()));
            recorded.logger.setLevel(Level.FINE);
            recorded.logger.setUseParentHandlers(false);
            recorded.logger.addHandler(recorded);
            return recorded;
        }

        List<String> messages() {
            return messages;
        }

        @Override
        public void publish(LogRecord record) {
            messages.add(record.getMessage());
        }

        @Override
        public void flush() {
            // Nothing is buffered.
        }

        @Override
        public void close() {
            logger.removeHandler(this);
            logger.setLevel(previousLevel);
            logger.setUseParentHandlers(previousUseParentHandlers);
        }
    }

    @Test
    void containedTokensAreStillRefreshedNextToAForeignOne() {
        field.addToken(CONTAINED);
        field.addToken(FOREIGN);

        container.getItem(CONTAINED).getItemProperty("name").setValue("Renamed");
        field.refreshTokens();

        assertWithMessage("A foreign token must not stop the others refreshing")
                .that(caption(CONTAINED)).isEqualTo("Renamed");
    }
}
