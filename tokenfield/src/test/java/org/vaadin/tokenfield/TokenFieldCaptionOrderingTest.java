package org.vaadin.tokenfield;

import com.vaadin.data.util.IndexedContainer;
import com.vaadin.data.util.ObjectProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;

import static com.google.common.truth.Truth.assertWithMessage;

/**
 * Token captions must not depend on the order of the initialization calls.
 *
 * <p>A Vaadin select resolves its captions from live data every time it paints
 * ({@code AbstractSelect.paintItem}), so configuring a {@code ComboBox} before or
 * after its container makes no difference to what the user sees. TokenField
 * renders each token as a real {@link com.vaadin.ui.Button} instead, and must
 * reach the same result.</p>
 */
class TokenFieldCaptionOrderingTest {

    private TestTokenField field;

    @BeforeEach
    void setup() {
        field = new TestTokenField();
    }

    /** Two ids with captions, bound as a value, and only then given a container. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    void explicitCaptionsShowWhenTheContainerArrivesLast() {
        field.setTokenCaption("id1", "Alpha");
        field.setTokenCaption("id2", "Beta");

        LinkedHashSet<Object> value = new LinkedHashSet<Object>();
        value.add("id1");
        value.add("id2");
        field.setPropertyDataSource(new ObjectProperty(value));

        IndexedContainer c = new IndexedContainer();
        c.addItem("id1");
        c.addItem("id2");
        field.setContainerDataSource(c);

        assertWithMessage("Token button must show the caption set before the container arrived")
                .that(field.getTokenButtons().get("id1").getCaption()).contains("Alpha");
        assertWithMessage("Token button must show the caption set before the container arrived")
                .that(field.getTokenButtons().get("id2").getCaption()).contains("Beta");
    }

    /** The same, for captions that come out of the container itself. */
    @Test
    void containerCaptionsShowWhenTheContainerArrivesAfterTheToken() {
        field.addToken("id1");

        IndexedContainer c = new IndexedContainer();
        c.addContainerProperty("name", String.class, null);
        c.addItem("id1");
        c.getContainerProperty("id1", "name").setValue("Linus Adams");

        field.setContainerDataSource(c);
        field.setTokenCaptionPropertyId("name");

        assertWithMessage("Token button must pick up the caption property set after the token")
                .that(field.getTokenButtons().get("id1").getCaption()).contains("Linus Adams");
    }

    /**
     * The buttons of a value set all at once must follow the value's own order,
     * not the hash order of an intermediate set.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    void tokenButtonsFollowTheOrderOfTheBoundValue() {
        LinkedHashSet<Object> value = new LinkedHashSet<Object>();
        value.add("charlie");
        value.add("alpha");
        value.add("bravo");

        field.setPropertyDataSource(new ObjectProperty(value));

        assertWithMessage("Token buttons must appear in the order of the bound value")
                .that(field.getTokenButtons().keySet())
                .containsExactly("charlie", "alpha", "bravo").inOrder();
    }

    /**
     * Container membership must not be a precondition for having a caption:
     * {@code AbstractSelect.getItemCaption} never asks {@code containsId}, and
     * tokens outside the container are a supported case of this component.
     */
    @Test
    void explicitCaptionAppliesToTokenOutsideTheContainer() {
        field.setTokenCaption("ghost", "Ghost Token");

        assertWithMessage("An explicit caption must apply to an id the container does not hold")
                .that(field.getTokenCaption("ghost")).isEqualTo("Ghost Token");
    }
}
