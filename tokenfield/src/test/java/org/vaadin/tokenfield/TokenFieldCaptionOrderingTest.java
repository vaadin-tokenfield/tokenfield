package org.vaadin.tokenfield;

import com.vaadin.data.util.IndexedContainer;
import com.vaadin.data.util.ObjectProperty;
import com.vaadin.server.Resource;
import com.vaadin.server.ThemeResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;

import static com.google.common.truth.Truth.assertWithMessage;

/**
 * Reproduces the order-dependent token captions: a token button renders the raw
 * tokenId whenever the data it derives its caption from arrives after the
 * button was created.
 *
 * <p>Vaadin's own selects re-derive caption and icon from the current state on
 * every paint ({@code AbstractSelect#paintItem}), so the order in which the
 * container, the caption mode and the value are configured cannot be observed.
 * TokenField renders tokens as real {@code Button} components configured once,
 * at creation time, which makes that order observable.</p>
 */
class TokenFieldCaptionOrderingTest {

    private TestTokenField field;

    @BeforeEach
    void setup() {
        field = new TestTokenField();
    }

    private String buttonCaption(Object tokenId) {
        return field.getTokenButtons().get(tokenId).getCaption();
    }

    /**
     * The exact sequence from the bug report: captions first, then the value,
     * then the container.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    void explicitCaptionsSetBeforeTheValueAreRendered() {
        field.setTokenCaption("id-a", "Alpha");
        field.setTokenCaption("id-b", "Beta");

        LinkedHashSet<Object> value = new LinkedHashSet<Object>();
        value.add("id-a");
        value.add("id-b");
        field.setPropertyDataSource(new ObjectProperty(value));

        IndexedContainer container = new IndexedContainer();
        container.addItem("id-a");
        container.addItem("id-b");
        field.setContainerDataSource(container);

        assertWithMessage("Explicit caption must win over the raw tokenId")
                .that(buttonCaption("id-a")).contains("Alpha");
        assertWithMessage("Explicit caption must win over the raw tokenId")
                .that(buttonCaption("id-b")).contains("Beta");
    }

    /** An explicit caption set after the token button already exists. */
    @Test
    void explicitCaptionSetAfterTheTokenIsRendered() {
        field.addToken("id-a");
        field.setTokenCaption("id-a", "Alpha");

        assertWithMessage("Setting a caption must update the existing token button")
                .that(buttonCaption("id-a")).contains("Alpha");
    }

    /** Captions read from a container property, with the container arriving last. */
    @Test
    void containerBackedCaptionsArrivingAfterTheTokenAreRendered() {
        field.setTokenCaptionPropertyId("name");
        field.addToken("id-a");

        IndexedContainer container = new IndexedContainer();
        container.addContainerProperty("name", String.class, null);
        container.addItem("id-a").getItemProperty("name").setValue("Alpha");
        field.setContainerDataSource(container);

        assertWithMessage("Container-backed caption must be picked up once the container is set")
                .that(buttonCaption("id-a")).contains("Alpha");
    }

    /** Same for icons, which are derived from the very same sources. */
    @Test
    void explicitIconSetAfterTheTokenIsRendered() {
        Resource icon = new ThemeResource("icons/token.png");
        field.addToken("id-a");
        field.setTokenIcon("id-a", icon);

        assertWithMessage("Setting an icon must update the existing token button")
                .that(field.getTokenButtons().get("id-a").getIcon())
                .isSameInstanceAs(icon);
    }

    /** A caption property value that changes while the token is on screen. */
    @Test
    void containerCaptionPropertyChangeIsReflected() {
        IndexedContainer container = new IndexedContainer();
        container.addContainerProperty("name", String.class, null);
        container.addItem("id-a").getItemProperty("name").setValue("Alpha");
        field.setContainerDataSource(container);
        field.setTokenCaptionPropertyId("name");
        field.addToken("id-a");

        container.getContainerProperty("id-a", "name").setValue("Renamed");

        assertWithMessage("A caption property change must reach the token button")
                .that(buttonCaption("id-a")).contains("Renamed");
    }
}
