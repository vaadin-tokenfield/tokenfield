package org.vaadin.tokenfield;

import com.vaadin.ui.Button;
import com.vaadin.ui.themes.Reindeer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Repro tests that expose (or document) code quality issues in Group E
 * (issues 04, 05, 06, 07) as identified in the code-review analysis.
 *
 * <p>Tests 05a / 05b / 05c are intentionally <em>failing</em> against the
 * current implementation because they assert the <em>correct</em> behaviour
 * rather than what the code currently does.  When the underlying issues are
 * fixed, these tests will start to pass.</p>
 *
 * <p>Tests 06a and 07a are documentation tests; they pass today and capture
 * the current observable behaviour together with explanatory comments about
 * the code quality concern.</p>
 */
class TokenFieldCodeQualityReproTest {

    private TestTokenField field;

    @BeforeEach
    void setup() {
        field = new TestTokenField();
    }

    // -----------------------------------------------------------------------
    // Issue 04 — Deprecated Vaadin 7 API calls
    // (compile-only concern; no behavioural test needed)
    // -----------------------------------------------------------------------

    /**
     * ISSUE-04 documentation comment only — no failing test.
     *
     * <p>Three places in the production code call deprecated Vaadin 7 APIs:</p>
     * <ul>
     *   <li>{@code TokenComboBox.java}: {@code requestRepaint()} should be
     *       {@code markAsDirty()}.</li>
     *   <li>{@code TokenField.java:236}: {@code cb.addListener(ValueChangeListener)}
     *       should be {@code cb.addValueChangeListener(...)}.</li>
     *   <li>{@code TokenField.java:384}: {@code b.addListener(ClickListener)}
     *       should be {@code b.addClickListener(...)}.</li>
     * </ul>
     * These compile successfully but produce deprecation warnings.  No
     * runtime behavioural difference exists, so no assertion is made here.
     */
    @Test
    void issue04_deprecatedApiCallsAreDocumented() {
        // The deprecated calls compile and work — this test exists solely as
        // a living comment.  When the deprecated APIs are replaced with their
        // modern equivalents, this test continues to pass and the comment
        // serves as evidence that the migration has been reviewed.
        field.addToken("any");
        assertNotNull(field.getTokenButtons().get("any"),
                "Token button was created, meaning addListener path executed");
    }

    // -----------------------------------------------------------------------
    // Issue 05 — Hardcoded theme (Reindeer) and UI strings
    // -----------------------------------------------------------------------

    /**
     * ISSUE-05a — EXPECTED TO FAIL against current code.
     *
     * <p>The current implementation calls
     * {@code button.setStyleName(Reindeer.BUTTON_LINK)} in
     * {@code configureTokenButton}, which sets the CSS class name {@code "link"}
     * (the value of {@link com.vaadin.ui.themes.BaseTheme#BUTTON_LINK}).
     * This hard-codes a dependency on the legacy Reindeer theme.</p>
     *
     * <p>The <em>correct</em> behaviour is for the button to carry an
     * addon-owned style name (e.g. {@code "token-remove"} or similar) instead
     * of a theme-specific constant.  This test asserts that the style name does
     * NOT equal the raw Reindeer link style, thereby failing today and acting as
     * a red-bar reminder to fix the issue.</p>
     */
    @Test
    void issue05a_tokenButtonStyleShouldUseAddonOwnedStyle() {
        field.addToken("java");
        Button btn = field.getTokenButtons().get("java");

        String style = btn.getStyleName();

        // Reindeer.BUTTON_LINK == "link" (inherited from BaseTheme).
        // The button currently has exactly this style, proving the hard-coded
        // Reindeer dependency.
        //
        // FIX: configureTokenButton should set an addon-owned style such as
        //      "token-remove" instead of Reindeer.BUTTON_LINK.
        assertFalse(
                style.equals(Reindeer.BUTTON_LINK),
                "Token button style must NOT be the Reindeer-specific 'link' constant. "
                        + "Current value: '" + style + "'. "
                        + "The addon should own its style name to be theme-agnostic."
        );
    }

    /**
     * ISSUE-05b — EXPECTED TO FAIL against current code.
     *
     * <p>The current implementation hard-codes the English string
     * {@code "Click to remove"} as the button tooltip in
     * {@code configureTokenButton}.  This prevents localisation.</p>
     *
     * <p>The <em>correct</em> behaviour is either to leave the description
     * empty by default (letting callers configure it), or to support
     * configurable/internationalised descriptions.  This test asserts that the
     * button description is NOT the hardcoded English string.</p>
     */
    @Test
    void issue05b_tokenButtonDescriptionShouldBeConfigurable() {
        field.addToken("python");
        Button btn = field.getTokenButtons().get("python");

        String description = btn.getDescription();

        // FIX: configureTokenButton should not hard-code "Click to remove".
        //      The description should be empty by default (overridable by
        //      callers), or provided through an i18n mechanism.
        assertNotEquals(
                "Click to remove",
                description,
                "Token button description must not be hardcoded to the English "
                        + "string 'Click to remove'. Current value: '" + description + "'"
        );
    }

    /**
     * ISSUE-05c — EXPECTED TO FAIL against current code.
     *
     * <p>The current implementation appends the literal Unicode character
     * {@code " ×"} to the button caption in {@code configureTokenButton}.
     * This hard-codes both the remove indicator and its whitespace prefix,
     * making it impossible for embedders to change without overriding the
     * entire method.</p>
     *
     * <p>The <em>correct</em> behaviour is for the caption to be either the
     * raw token caption (allowing the UI layer to add remove indicators), or
     * to expose the remove suffix as a configurable field.</p>
     */
    @Test
    void issue05c_tokenButtonCaptionShouldNotHardcodeRemoveSuffix() {
        field.addToken("rust");
        Button btn = field.getTokenButtons().get("rust");

        String caption = btn.getCaption();

        // FIX: configureTokenButton appends " ×" unconditionally.
        //      The remove suffix should be configurable or omitted by default.
        assertFalse(
                caption.contains(" ×"),
                "Token button caption must not contain the hardcoded remove "
                        + "suffix ' ×'. Current caption: '" + caption + "'"
        );
    }

    // -----------------------------------------------------------------------
    // Issue 06 — Raw types and unchecked casts
    // -----------------------------------------------------------------------

    /**
     * ISSUE-06a — Documentation test (passes).
     *
     * <p>{@link TokenField#getTokenIds()} is declared as returning the raw
     * type {@code Collection} (no type parameter).  At runtime this still
     * returns a {@code Collection<?>}, but the missing generic declaration
     * suppresses compile-time type-safety checks for callers.</p>
     *
     * <p>This test documents the current behaviour: the method returns a
     * non-null {@code Collection} after tokens have been added to the
     * container.  The comment explains the expected signature after fixing.</p>
     *
     * <p>NOTE: {@code getTokenIds()} exposes <em>container</em> items, not
     * the currently selected tokens returned by {@code getValue()}.  After
     * wiring items into the ComboBox container the collection is non-empty.</p>
     */
    @Test
    @SuppressWarnings("unchecked") // mirrors the raw-type issue being documented
    void issue06a_getTokenIdsReturnsRawCollection() {
        // Seed the combo-box container so that getTokenIds() is non-empty.
        field.getComboBox().addItem("alpha");
        field.getComboBox().addItem("beta");

        // Raw-typed return value — the unchecked cast below mirrors what every
        // caller must do today because the method signature is raw.
        //
        // FIX: change the signature to
        //      public Collection<?> getTokenIds()
        //      (or better: public Collection<Object> getTokenIds())
        Collection rawCollection = field.getTokenIds();

        assertNotNull(rawCollection, "getTokenIds() must return a non-null Collection");
        assertEquals(2, rawCollection.size(),
                "getTokenIds() must reflect items added to the container");

        // Demonstrate that unchecked usage compiles without even a cast warning
        // only because the type is raw — a typed call would require no cast.
        List<Object> typed = new ArrayList<Object>(rawCollection);
        assertTrue(typed.contains("alpha"));
        assertTrue(typed.contains("beta"));
    }

    /**
     * ISSUE-06b — Documentation test (passes).
     *
     * <p>{@link TokenField#getType()} returns the raw class literal
     * {@code Set.class}.  The return type is declared as {@code Class<?>} in
     * the {@code CustomField} API, so no warning is emitted today, but the
     * returned value carries no information about the element type.</p>
     *
     * <p>This test documents that {@code getType()} currently returns
     * {@code Set.class} rather than a more specific type such as
     * {@code LinkedHashSet.class}.  Ideally the implementation would also
     * indicate the parameterised type, but that is not representable with a
     * plain {@code Class} token.</p>
     */
    @Test
    void issue06b_getTypeReturnsRawSetClass() {
        // FIX candidate: return LinkedHashSet.class to reflect the actual
        //                runtime type used in addToken / removeToken.
        assertEquals(java.util.Set.class, field.getType(),
                "getType() must return Set.class (raw) — documents current behaviour");
    }

    // -----------------------------------------------------------------------
    // Issue 07 — Dead code and style issues
    // -----------------------------------------------------------------------

    /**
     * ISSUE-07a — Documentation test (passes).
     *
     * <p>In {@code TokenField.java:432}, the variable that holds the updated
     * token set is declared as {@code HashSet<Object>} but is actually
     * instantiated as a {@code new LinkedHashSet<Object>(set)}.  The declared
     * type is incorrect.</p>
     *
     * <p>Despite the wrong declared type, insertion order IS preserved at
     * runtime because the concrete object is a {@code LinkedHashSet}.  This
     * test documents that observable fact while calling out the root cause:
     * the declared type should be {@code LinkedHashSet<Object>} to accurately
     * reflect the runtime type and make the ordering guarantee explicit.</p>
     *
     * <p>Code location: {@code TokenField.java:432}
     * {@code HashSet<Object> newSet = new LinkedHashSet<Object>(set);}</p>
     */
    @Test
    void issue07a_addTokenPreservesInsertionOrderDespiteHashSetDeclaration() {
        // Add tokens in a deliberate order.
        field.addToken("first");
        field.addToken("second");
        field.addToken("third");

        // getValue() returns the LinkedHashSet that was stored by addToken.
        @SuppressWarnings("unchecked")
        java.util.Set<Object> stored = (java.util.Set<Object>) field.getValue();

        assertNotNull(stored, "getValue() must return the stored token set");
        List<Object> ordered = new ArrayList<Object>(stored);

        // Insertion order must be preserved — this works ONLY because the
        // runtime type is LinkedHashSet, not plain HashSet.
        // The bug is that the local variable in addToken is declared as
        // HashSet<Object>, hiding the ordering guarantee from the reader.
        assertEquals(Arrays.asList("first", "second", "third"), ordered,
                "Insertion order must be preserved (runtime type is LinkedHashSet, "
                        + "even though the variable is declared as HashSet)");
    }
}
