package org.vaadin.tokenfield;

import com.vaadin.data.util.IndexedContainer;
import com.vaadin.data.util.ObjectProperty;
import com.vaadin.ui.AbstractSelect.ItemCaptionMode;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Reproduction tests for issue 10 / GitHub issue #8: token caption lost when
 * the container data source is set after the property data source.
 *
 * <p>{@code getTokenCaption} guards on {@code cb.containsId(tokenId)} and
 * otherwise returns {@code "" + tokenId}. Token buttons are built during
 * {@code setPropertyDataSource}; if the container arrives only afterwards, that
 * guard is false at build time and every button is captioned with the raw item
 * id — even when an explicit caption was registered up front via
 * {@code setTokenCaption}, which {@code AbstractSelect} keeps in its own
 * caption map and would happily return regardless of container membership.</p>
 *
 * <p>{@link #captionLostWhenContainerSetAfterPropertyDatasource()} is expected
 * to FAIL against the current production code. The other three document
 * behaviour that must not regress.</p>
 */
class TokenFieldCaptionOrderingReproTest {

    private static final String TOKEN_ID = "id-123";
    private static final String FRIENDLY_CAPTION = "Pretty Name";
    private static final String CAPTION_PROPERTY = "label";

    private static ObjectProperty<Set> propertyHolding(String tokenId) {
        Set<Object> tokenSet = new LinkedHashSet<Object>();
        tokenSet.add(tokenId);
        return new ObjectProperty<Set>(tokenSet, Set.class);
    }

    private static IndexedContainer containerWith(String tokenId) {
        IndexedContainer container = new IndexedContainer();
        container.addItem(tokenId);
        return container;
    }

    // -----------------------------------------------------------------------
    // 10a — reproduces the bug (expected to FAIL with current code)
    // -----------------------------------------------------------------------

    /**
     * Reproduces GitHub issue #8 in the order it was reported: the caption is
     * registered first, then the property data source, then the container.
     *
     * <ol>
     *   <li>{@code setTokenCaption(TOKEN_ID, "Pretty Name")} — the caption is
     *       now known to the ComboBox, stored in {@code AbstractSelect}'s
     *       explicit-caption map. It does not require the item to be in the
     *       container.</li>
     *   <li>{@code setPropertyDataSource(property)} — this triggers
     *       {@code setInternalValue} → {@code addTokenButton} →
     *       {@code configureTokenButton} → {@code getTokenCaption}. The
     *       container is still the default empty one, so
     *       {@code cb.containsId(TOKEN_ID)} is false and the caption falls back
     *       to the raw id string.</li>
     *   <li>{@code setContainerDataSource(container)} — arrives after the
     *       buttons were already captioned.</li>
     * </ol>
     *
     * <p>The explicit caption was available the whole time; only the
     * {@code containsId} guard stopped it from being used.</p>
     */
    @Test
    void captionLostWhenContainerSetAfterPropertyDatasource() {
        TestTokenField field = new TestTokenField();

        // Step 1 — the friendly caption is registered up front.
        field.setTokenCaption(TOKEN_ID, FRIENDLY_CAPTION);

        // Step 2 — property data source first: builds the token button while
        //          the container is still empty.
        field.setPropertyDataSource(propertyHolding(TOKEN_ID));

        // Step 3 — container data source afterwards.
        field.setContainerDataSource(containerWith(TOKEN_ID));

        assertTrue(field.getTokenButtons().containsKey(TOKEN_ID),
                "Token button for '" + TOKEN_ID + "' should have been created");

        String buttonCaption = field.getTokenButtons().get(TOKEN_ID).getCaption();

        assertTrue(buttonCaption.contains(FRIENDLY_CAPTION),
                "Expected button caption to contain '" + FRIENDLY_CAPTION
                        + "' but was: '" + buttonCaption + "'"
                        + " — bug: container set after setPropertyDataSource, so the"
                        + " containsId guard discarded the registered caption");
    }

    // -----------------------------------------------------------------------
    // 10b — the documented workaround order; must keep working
    // -----------------------------------------------------------------------

    /**
     * The workaround order from the issue thread — container first, then
     * caption, then property data source. This already passes today and is
     * kept as a regression guard for the fix.
     */
    @Test
    void captionCorrectWhenContainerSetBeforePropertyDatasource() {
        TestTokenField field = new TestTokenField();

        field.setContainerDataSource(containerWith(TOKEN_ID));
        field.setTokenCaption(TOKEN_ID, FRIENDLY_CAPTION);
        field.setPropertyDataSource(propertyHolding(TOKEN_ID));

        assertTrue(field.getTokenButtons().containsKey(TOKEN_ID),
                "Token button for '" + TOKEN_ID + "' should have been created");

        String buttonCaption = field.getTokenButtons().get(TOKEN_ID).getCaption();

        assertTrue(buttonCaption.contains(FRIENDLY_CAPTION),
                "Expected button caption to contain '" + FRIENDLY_CAPTION
                        + "' but was: '" + buttonCaption + "'");
    }

    // -----------------------------------------------------------------------
    // Fallback guards — the id string must still be used when there is no
    // caption to be had, whichever caption mode is in play.
    // -----------------------------------------------------------------------

    /**
     * A token that is in neither the container nor the caption map must still
     * fall back to {@code tokenId.toString()}, not to an empty caption.
     */
    @Test
    void unknownTokenStillFallsBackToIdString() {
        TestTokenField field = new TestTokenField();

        assertEquals("no-such-token", field.getTokenCaption("no-such-token"),
                "A token with no container entry and no explicit caption must"
                        + " fall back to its id string");
    }

    /**
     * With {@code setTokenCaptionPropertyId} configured, the caption must come
     * from that container property — and an item that is not in the container
     * (so has no such property) must still fall back to its id string rather
     * than to the empty string {@code ComboBox#getItemCaption} returns.
     */
    @Test
    void captionPropertyIsUsedAndMissingItemsFallBackToIdString() {
        TestTokenField field = new TestTokenField();

        IndexedContainer container = new IndexedContainer();
        container.addContainerProperty(CAPTION_PROPERTY, String.class, "");
        container.addItem(TOKEN_ID);
        container.getContainerProperty(TOKEN_ID, CAPTION_PROPERTY)
                .setValue(FRIENDLY_CAPTION);

        field.setContainerDataSource(container);
        field.setTokenCaptionMode(ItemCaptionMode.PROPERTY);
        field.setTokenCaptionPropertyId(CAPTION_PROPERTY);

        assertEquals(FRIENDLY_CAPTION, field.getTokenCaption(TOKEN_ID),
                "Caption must be read from the configured caption property");
        assertEquals("absent", field.getTokenCaption("absent"),
                "An item outside the container has no caption property, so the"
                        + " id string must be used instead of an empty caption");
    }
}
