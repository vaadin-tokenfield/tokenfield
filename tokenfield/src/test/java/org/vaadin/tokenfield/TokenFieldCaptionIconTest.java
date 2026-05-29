package org.vaadin.tokenfield;

import com.vaadin.server.Resource;
import com.vaadin.server.ThemeResource;
import com.vaadin.ui.AbstractSelect.ItemCaptionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;

/**
 * Round-trips the token caption/icon getter-setter pairs that delegate to the
 * embedded ComboBox, and verifies {@link TokenField#configureTokenButton}
 * picks up a token's icon.
 */
class TokenFieldCaptionIconTest {

    private TestTokenField field;

    @BeforeEach
    void setup() {
        field = new TestTokenField();
    }

    @Test
    void setTokenCaptionIsReflectedByGetTokenCaption() {
        field.getContainerDataSource().addItem("id1");
        field.setTokenCaption("id1", "Custom Label");
        assertThat(field.getTokenCaption("id1")).isEqualTo("Custom Label");
    }

    @Test
    void getTokenCaptionFallsBackToToStringForUnknownId() {
        assertThat(field.getTokenCaption("unknown-id")).isEqualTo("unknown-id");
    }

    @Test
    void setTokenCaptionModeRoundTrips() {
        field.setTokenCaptionMode(ItemCaptionMode.ID);
        assertThat(field.getTokenCaptionMode()).isEqualTo(ItemCaptionMode.ID);
    }

    @Test
    void setTokenCaptionPropertyIdRoundTrips() {
        field.setTokenCaptionPropertyId("name");
        assertThat(field.getTokenCaptionPropertyId()).isEqualTo("name");
    }

    @Test
    void setTokenIconRoundTrips() {
        Resource icon = new ThemeResource("icons/token.png");
        field.setTokenIcon("id1", icon);
        assertThat(field.getTokenIcon("id1")).isSameInstanceAs(icon);
    }

    @Test
    void setTokenIconPropertyIdRoundTrips() {
        // Unlike setItemCaptionPropertyId, AbstractSelect#setItemIconPropertyId
        // validates that the property exists in the container.
        field.getContainerDataSource().addContainerProperty("iconProp", Resource.class, null);
        field.setTokenIconPropertyId("iconProp");
        assertThat(field.getTokenIconPropertyId()).isEqualTo("iconProp");
    }

    @Test
    void configureTokenButtonUsesTheConfiguredIcon() {
        Resource icon = new ThemeResource("icons/token.png");
        field.setTokenIcon("id1", icon);
        field.addToken("id1");
        assertThat(field.getTokenButtons().get("id1").getIcon()).isSameInstanceAs(icon);
    }
}
