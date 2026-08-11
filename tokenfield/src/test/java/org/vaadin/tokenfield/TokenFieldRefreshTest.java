package org.vaadin.tokenfield;

import com.vaadin.data.util.IndexedContainer;
import com.vaadin.server.Resource;
import com.vaadin.server.ThemeResource;
import com.vaadin.ui.AbstractSelect.ItemCaptionMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.themes.Reindeer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * A token button must show what the data source says now, not what it said when
 * the button happened to be created. Covers the caption/icon setters, container
 * swaps, and changes inside the container - the equivalents of what makes an
 * {@code AbstractSelect} repaint.
 */
class TokenFieldRefreshTest {

    private TestTokenField field;
    private IndexedContainer container;

    @BeforeEach
    void setup() {
        field = new TestTokenField();
        container = new IndexedContainer();
        container.addContainerProperty("name", String.class, null);
        container.addContainerProperty("icon", Resource.class, null);
    }

    private String captionOf(Object tokenId) {
        return field.getTokenButtons().get(tokenId).getCaption();
    }

    // -----------------------------------------------------------------------
    // The caption and icon setters
    // -----------------------------------------------------------------------

    @Test
    void setTokenCaptionUpdatesAnExistingButton() {
        field.addToken("id1");
        field.setTokenCaption("id1", "Late Caption");
        assertThat(captionOf("id1")).contains("Late Caption");
    }

    @Test
    void setTokenCaptionToNullRevertsToTheFallback() {
        field.addToken("id1");
        field.setTokenCaption("id1", "Late Caption");
        field.setTokenCaption("id1", null);
        assertThat(captionOf("id1")).contains("id1");
        assertThat(captionOf("id1")).doesNotContain("Late Caption");
    }

    @Test
    void setTokenIconUpdatesAnExistingButton() {
        field.addToken("id1");
        Resource icon = new ThemeResource("icons/token.png");
        field.setTokenIcon("id1", icon);
        assertThat(field.getTokenButtons().get("id1").getIcon()).isSameInstanceAs(icon);
    }

    @Test
    void setTokenCaptionModeUpdatesExistingButtons() {
        container.addItem("id1");
        field.setContainerDataSource(container);
        field.setTokenCaption("id1", "Explicit");
        field.addToken("id1");
        assertThat(captionOf("id1")).contains("Explicit");

        field.setTokenCaptionMode(ItemCaptionMode.ID);
        assertWithMessage("Switching the caption mode must re-caption the buttons")
                .that(captionOf("id1")).doesNotContain("Explicit");
    }

    @Test
    void setTokenIconPropertyIdUpdatesExistingButtons() {
        Resource icon = new ThemeResource("icons/token.png");
        container.addItem("id1");
        container.getContainerProperty("id1", "icon").setValue(icon);
        field.setContainerDataSource(container);
        field.addToken("id1");

        field.setTokenIconPropertyId("icon");
        assertThat(field.getTokenButtons().get("id1").getIcon()).isSameInstanceAs(icon);
    }

    // -----------------------------------------------------------------------
    // Changes to the container
    // -----------------------------------------------------------------------

    @Test
    void addingTheItemToTheContainerLaterUpdatesTheButton() {
        field.setContainerDataSource(container);
        field.setTokenCaptionPropertyId("name");
        field.addToken("id1");
        assertThat(captionOf("id1")).contains("id1");

        container.addItem("id1");
        container.getContainerProperty("id1", "name").setValue("Arrived Late");

        assertWithMessage("An item set change must reach the token buttons")
                .that(captionOf("id1")).contains("Arrived Late");
    }

    @Test
    void editingTheCaptionPropertyUpdatesTheButton() {
        container.addItem("id1");
        container.getContainerProperty("id1", "name").setValue("Before");
        field.setContainerDataSource(container);
        field.setTokenCaptionPropertyId("name");
        field.addToken("id1");
        assertThat(captionOf("id1")).contains("Before");

        container.getContainerProperty("id1", "name").setValue("After");

        assertWithMessage("Editing the caption property must reach the token button")
                .that(captionOf("id1")).contains("After");
    }

    @Test
    void editingTheIconPropertyUpdatesTheButton() {
        Resource icon = new ThemeResource("icons/token.png");
        container.addItem("id1");
        field.setContainerDataSource(container);
        field.setTokenIconPropertyId("icon");
        field.addToken("id1");
        assertThat(field.getTokenButtons().get("id1").getIcon()).isNull();

        container.getContainerProperty("id1", "icon").setValue(icon);

        assertThat(field.getTokenButtons().get("id1").getIcon()).isSameInstanceAs(icon);
    }

    @Test
    void editingAnItemUpdatesTheButtonInItemMode() {
        container.addItem("id1");
        container.getContainerProperty("id1", "name").setValue("Before");
        field.setContainerDataSource(container);
        field.setTokenCaptionMode(ItemCaptionMode.ITEM);
        field.addToken("id1");

        container.getContainerProperty("id1", "name").setValue("After");

        assertWithMessage("ITEM mode captions the whole item, so any property counts")
                .that(captionOf("id1")).contains("After");
    }

    @Test
    void addingAContainerPropertyUpdatesTheButtons() {
        IndexedContainer bare = new IndexedContainer();
        bare.addItem("id1");
        field.setContainerDataSource(bare);
        field.addToken("id1");
        field.setTokenCaptionMode(ItemCaptionMode.ITEM);

        bare.addContainerProperty("name", String.class, "From Property Set Change");

        assertWithMessage("A property set change must reach the token buttons")
                .that(captionOf("id1")).contains("From Property Set Change");
    }

    @Test
    void swappingTheContainerUpdatesTheButtons() {
        container.addItem("id1");
        container.getContainerProperty("id1", "name").setValue("First Book");
        field.setContainerDataSource(container);
        field.setTokenCaptionPropertyId("name");
        field.addToken("id1");
        assertThat(captionOf("id1")).contains("First Book");

        IndexedContainer other = new IndexedContainer();
        other.addContainerProperty("name", String.class, null);
        other.addItem("id1");
        other.getContainerProperty("id1", "name").setValue("Second Book");
        field.setContainerDataSource(other);

        assertThat(captionOf("id1")).contains("Second Book");
    }

    @Test
    void theOldContainerStopsDrivingTheButtons() {
        container.addItem("id1");
        container.getContainerProperty("id1", "name").setValue("First Book");
        field.setContainerDataSource(container);
        field.setTokenCaptionPropertyId("name");
        field.addToken("id1");

        IndexedContainer other = new IndexedContainer();
        other.addContainerProperty("name", String.class, null);
        other.addItem("id1");
        other.getContainerProperty("id1", "name").setValue("Second Book");
        field.setContainerDataSource(other);

        container.getContainerProperty("id1", "name").setValue("Edited After Swap");

        assertWithMessage("The replaced container must no longer drive the buttons")
                .that(captionOf("id1")).contains("Second Book");
    }

    // -----------------------------------------------------------------------
    // The configureTokenButton hook under repeated invocation
    // -----------------------------------------------------------------------

    @Test
    void overriddenConfigureTokenButtonIsUsedOnRefreshToo() {
        TestTokenField custom = new TestTokenField() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void configureTokenButton(Object tokenId, Button button) {
                button.setCaption("CUSTOM " + getTokenCaption(tokenId));
            }
        };
        custom.addToken("id1");
        custom.setTokenCaption("id1", "Late");

        assertThat(custom.getTokenButtons().get("id1").getCaption()).isEqualTo("CUSTOM Late");
    }

    @Test
    void repeatedConfigurationDoesNotAccumulateStyles() {
        field.addToken("id1");
        field.setTokenCaption("id1", "One");
        field.setTokenCaption("id1", "Two");
        field.setTokenCaption("id1", "Three");

        assertWithMessage("configureTokenButton must be idempotent")
                .that(field.getTokenButtons().get("id1").getStyleName())
                .isEqualTo(Reindeer.BUTTON_LINK);
        assertThat(captionOf("id1")).contains("Three");
    }

    @Test
    void configureTokenButtonTouchingTheContainerDoesNotRecurse() {
        final AtomicInteger calls = new AtomicInteger();
        final TestTokenField custom = new TestTokenField() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void configureTokenButton(Object tokenId, Button button) {
                super.configureTokenButton(tokenId, button);
                calls.incrementAndGet();
                // A hook that writes to the container would re-enter through the
                // item set change listener if the refresh were not guarded.
                getContainerDataSource().addItem("side-effect-" + calls.get());
            }
        };
        custom.setContainerDataSource(container);

        assertDoesNotThrow(() -> custom.addToken("id1"));
        assertWithMessage("A refresh must not re-enter itself").that(calls.get()).isLessThan(10);
    }
}
