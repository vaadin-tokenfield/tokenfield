package org.vaadin.tokenfield;

import com.vaadin.ui.Button;
import com.vaadin.ui.themes.Reindeer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

/**
 * Tests for {@link TokenField#configureTokenButton} defaults and overrides,
 * and the click → {@link TokenField#onTokenClick} wiring.
 */
class TokenFieldButtonConfigTest {

    private TestTokenField field;

    @BeforeEach
    void setup() {
        field = new TestTokenField();
    }

    @Test
    void buttonCaptionContainsTokenId() {
        field.addToken("scala");
        String caption = field.getTokenButtons().get("scala").getCaption();
        assertWithMessage("Default button caption must include the token id")
                .that(caption).contains("scala");
    }

    @Test
    void buttonCaptionContainsRemoveMarker() {
        field.addToken("java");
        String caption = field.getTokenButtons().get("java").getCaption();
        // configureTokenButton appends " ×"
        assertWithMessage("Default button caption must contain the '×' remove marker")
                .that(caption).contains("×");
    }

    @Test
    void buttonDescriptionIsClickToRemove() {
        field.addToken("go");
        assertThat(field.getTokenButtons().get("go").getDescription()).isEqualTo("Click to remove");
    }

    @Test
    void buttonStyleIsLinkTheme() {
        field.addToken("rust");
        assertThat(field.getTokenButtons().get("rust").getStyleName()).isEqualTo(Reindeer.BUTTON_LINK);
    }

    @Test
    void buttonCaptionUsesContainerItemCaptionWhenPresent() {
        field.getComboBox().addItem("id1");
        field.getComboBox().setItemCaption("id1", "Custom Label");
        field.addToken("id1");
        String caption = field.getTokenButtons().get("id1").getCaption();
        assertWithMessage("Button caption must use the container's item caption when available")
                .that(caption).contains("Custom Label");
    }

    @Test
    void overridingConfigureTokenButtonIsHonored() {
        TestTokenField custom = new TestTokenField() {
            @Override
            protected void configureTokenButton(Object tokenId, Button button) {
                button.setCaption("CUSTOM");
            }
        };
        custom.addToken("x");
        assertWithMessage("Overriding configureTokenButton must replace default configuration")
                .that(custom.getTokenButtons().get("x").getCaption()).isEqualTo("CUSTOM");
    }

    @Test
    void clickingTokenButtonCallsOnTokenClick() {
        AtomicReference<Object> clicked = new AtomicReference<Object>();
        TestTokenField custom = new TestTokenField() {
            @Override
            protected void onTokenClick(Object tokenId) {
                clicked.set(tokenId);
            }
        };
        custom.addToken("py");
        custom.getTokenButtons().get("py").click();
        assertWithMessage("Clicking the token button must call onTokenClick with the token id")
                .that(clicked.get()).isEqualTo("py");
    }

    @Test
    void clickTokenButtonRemovesTokenByDefault() {
        field.addToken("del");
        field.getTokenButtons().get("del").click();
        assertWithMessage("Default click handler must remove the token")
                .that(field.getTokenButtons()).doesNotContainKey("del");
    }
}
