package org.vaadin.tokenfield;

import com.vaadin.data.util.IndexedContainer;
import com.vaadin.data.util.ObjectProperty;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Reproduction tests for issue 10: token caption lost when container is set
 * after property datasource.
 *
 * <p>When {@code setPropertyDataSource} is called before
 * {@code setContainerDataSource}, token buttons are created while the
 * container is still empty (or the default container). {@code getTokenCaption}
 * then falls back to {@code tokenId.toString()} instead of the friendly caption
 * defined in the container.</p>
 *
 * <p>Test 10a ({@link #captionLostWhenContainerSetAfterPropertyDatasource}) is
 * expected to FAIL with the current production code — it documents the bug.</p>
 *
 * <p>Test 10b ({@link #captionCorrectWhenContainerSetBeforePropertyDatasource})
 * is expected to PASS — it verifies the workaround (correct call order) works,
 * and serves as a regression guard for any future fix.</p>
 */
class TokenFieldCaptionOrderingReproTest {

    private static final String TOKEN_ID = "id-123";
    private static final String FRIENDLY_CAPTION = "Pretty Name";

    /**
     * Builds an {@link IndexedContainer} with a single item whose caption is
     * set to {@value #FRIENDLY_CAPTION}.
     */
    private IndexedContainer buildContainer() {
        IndexedContainer container = new IndexedContainer();
        container.addItem(TOKEN_ID);
        // IndexedContainer does not support item captions directly; captions are
        // managed by the ComboBox (AbstractSelect) layer via setItemCaption.
        return container;
    }

    // -----------------------------------------------------------------------
    // Test 10a — reproduces the bug (expected to FAIL with current code)
    // -----------------------------------------------------------------------

    /**
     * Issue 10 reproduction: property datasource set BEFORE container datasource.
     *
     * <p>Steps:
     * <ol>
     *   <li>Create {@link IndexedContainer} with item "id-123".</li>
     *   <li>Create a {@link TestTokenField}.</li>
     *   <li>Pre-configure the friendly caption on the field's ComboBox so it
     *       would be available IF the container were already set.</li>
     *   <li>Create an {@link ObjectProperty} holding a set containing "id-123".</li>
     *   <li>Call {@code setPropertyDataSource} — this triggers
     *       {@code setInternalValue} → {@code addTokenButton} →
     *       {@code configureTokenButton} → {@code getTokenCaption}. At this
     *       point the container is still the default (empty) one, so the caption
     *       falls back to "id-123".</li>
     *   <li>Call {@code setContainerDataSource} with the prepared container and
     *       set the friendly caption — too late, buttons were already created.</li>
     *   <li>Assert the button caption contains "Pretty Name".</li>
     * </ol>
     *
     * <p>This assertion currently FAILS: the button caption is "id-123 ×" instead
     * of "Pretty Name ×", because the container was not yet set when
     * {@code configureTokenButton} ran.</p>
     */
    @Test
    void captionLostWhenContainerSetAfterPropertyDatasource() {
        // Step 1 — prepare container
        IndexedContainer container = new IndexedContainer();
        container.addItem(TOKEN_ID);

        // Step 2 — create field
        TestTokenField field = new TestTokenField();

        // Step 3 — build the property value (Set containing the token id)
        Set<Object> tokenSet = new LinkedHashSet<>();
        tokenSet.add(TOKEN_ID);

        @SuppressWarnings("unchecked")
        ObjectProperty<Set> property = new ObjectProperty<>(tokenSet, Set.class);

        // Step 4 — set property datasource FIRST (triggers button creation with
        //          the default/empty container → caption falls back to id string)
        field.setPropertyDataSource(property);

        // Step 5 — set container datasource AFTER (too late for button creation)
        field.setContainerDataSource(container);
        field.getComboBox().setItemCaption(TOKEN_ID, FRIENDLY_CAPTION);

        // Step 6 — verify: button caption should contain the friendly name
        assertTrue(field.getTokenButtons().containsKey(TOKEN_ID),
                "Token button for '" + TOKEN_ID + "' should have been created");

        String buttonCaption = field.getTokenButtons().get(TOKEN_ID).getCaption();

        // This assertion FAILS with current code: caption is "id-123 ×" not "Pretty Name ×"
        assertTrue(buttonCaption.contains(FRIENDLY_CAPTION),
                "Expected button caption to contain '" + FRIENDLY_CAPTION
                        + "' but was: '" + buttonCaption + "'"
                        + " — bug: container set after setPropertyDataSource, caption fell back to id string");
    }

    // -----------------------------------------------------------------------
    // Test 10b — complementary passing test (correct call order)
    // -----------------------------------------------------------------------

    /**
     * Complementary / regression test: container datasource set BEFORE property
     * datasource.
     *
     * <p>This is the workaround order that already works. The test must PASS with
     * both the current (buggy) code and any future fix.</p>
     *
     * <p>Steps:
     * <ol>
     *   <li>Create {@link IndexedContainer} with item "id-123".</li>
     *   <li>Create a {@link TestTokenField}.</li>
     *   <li>Call {@code setContainerDataSource} FIRST and set the friendly
     *       caption on the ComboBox.</li>
     *   <li>Create an {@link ObjectProperty} holding a set containing "id-123".</li>
     *   <li>Call {@code setPropertyDataSource} — at this point the container is
     *       already populated, so {@code getTokenCaption} finds the friendly
     *       caption.</li>
     *   <li>Assert the button caption contains "Pretty Name".</li>
     * </ol>
     */
    @Test
    void captionCorrectWhenContainerSetBeforePropertyDatasource() {
        // Step 1 — prepare container
        IndexedContainer container = new IndexedContainer();
        container.addItem(TOKEN_ID);

        // Step 2 — create field
        TestTokenField field = new TestTokenField();

        // Step 3 — set container datasource FIRST and configure the caption
        field.setContainerDataSource(container);
        field.getComboBox().setItemCaption(TOKEN_ID, FRIENDLY_CAPTION);

        // Step 4 — build property value
        Set<Object> tokenSet = new LinkedHashSet<>();
        tokenSet.add(TOKEN_ID);

        @SuppressWarnings("unchecked")
        ObjectProperty<Set> property = new ObjectProperty<>(tokenSet, Set.class);

        // Step 5 — set property datasource AFTER container is ready
        field.setPropertyDataSource(property);

        // Step 6 — verify: button caption should contain the friendly name
        assertTrue(field.getTokenButtons().containsKey(TOKEN_ID),
                "Token button for '" + TOKEN_ID + "' should have been created");

        String buttonCaption = field.getTokenButtons().get(TOKEN_ID).getCaption();

        assertTrue(buttonCaption.contains(FRIENDLY_CAPTION),
                "Expected button caption to contain '" + FRIENDLY_CAPTION
                        + "' but was: '" + buttonCaption + "'");
    }
}
