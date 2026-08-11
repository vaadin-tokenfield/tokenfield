package org.vaadin.tokenfield;

import com.vaadin.data.util.IndexedContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

/**
 * Tests for container-datasource delegation: {@link TokenField#setContainerDataSource},
 * {@link TokenField#getTokenIds()}, and caption/icon fall-through logic.
 */
class TokenFieldContainerTest {

    private TestTokenField field;

    @BeforeEach
    void setup() {
        field = new TestTokenField();
    }

    @Test
    void setContainerDataSourceReplacesContainer() {
        IndexedContainer c = new IndexedContainer();
        c.addItem("item1");
        c.addItem("item2");
        field.setContainerDataSource(c);
        assertThat(field.getContainerDataSource()).isSameInstanceAs(c);
    }

    @Test
    void getTokenIdsDelegatesToContainer() {
        field.getContainerDataSource().addItem("alpha");
        field.getContainerDataSource().addItem("beta");
        Collection<?> ids = field.getTokenIds();
        assertThat(ids).contains("alpha");
        assertThat(ids).contains("beta");
    }

    @Test
    void getTokenCaptionFallsBackToStringWhenIdAbsentFromContainer() {
        // "ghost" was never added to the container; in the default
        // EXPLICIT_DEFAULTS_ID mode an id without an explicit caption is its own
        // caption, container membership notwithstanding
        String caption = field.getTokenCaption("ghost");
        assertWithMessage("Caption must fall back to tokenId.toString() when id not in container")
                .that(caption).isEqualTo("ghost");
    }

    @Test
    void getTokenCaptionUsesContainerItemCaption() {
        field.getContainerDataSource().addItem("id1");
        field.setTokenCaption("id1", "Pretty Label");
        assertThat(field.getTokenCaption("id1")).isEqualTo("Pretty Label");
    }

    @Test
    void addTokenAcceptsIdNotInSuggestionContainer() {
        // Tokens can be added even if absent from the suggestion container
        field.addToken("offlist");
        assertWithMessage("addToken must work even if id is not in the ComboBox container")
                .that(field.getTokenButtons()).containsKey("offlist");
    }

    @Test
    void tokenButtonCaptionUsesFallbackForUnknownId() {
        field.addToken("rawid");
        String btnCaption = field.getTokenButtons().get("rawid").getCaption();
        assertWithMessage("Button caption should contain the fallback (string repr of tokenId)")
                .that(btnCaption).contains("rawid");
    }

    @Test
    void tokenCaptionModeGetterDelegatesToComboBox() {
        // Default caption mode must be consistent between field and cb
        assertThat(field.getComboBox().getItemCaptionMode()).isEqualTo(field.getTokenCaptionMode());
    }
}
