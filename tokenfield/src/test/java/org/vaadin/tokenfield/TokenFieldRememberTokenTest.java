package org.vaadin.tokenfield;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;

/**
 * Tests {@link TokenField#rememberToken(String)} via the NewItemHandler path
 * ({@link TestTokenField#simulateNewItemInput(String)}), with and without a
 * {@code tokenCaptionPropertyId}.
 *
 * <p>The caption property used to be out of scope here, because
 * {@code rememberToken} added the new item under its <em>caption</em> and then
 * wrote the caption property under the original <em>id</em> — two different
 * items as soon as caption and id diverge. That is the second half of <a
 * href="https://github.com/vaadin-tokenfield/tokenfield/issues/24">issue
 * #24</a>; the item is now added under its id, and the caption is what gets
 * written.</p>
 */
class TokenFieldRememberTokenTest {

    private static final String CAPTION_PROPERTY = "label";

    private TestTokenField field;

    @BeforeEach
    void setup() {
        field = new TestTokenField();
    }

    @Test
    void newTokenIsAddedToContainer() {
        field.simulateNewItemInput("tag1");
        assertThat(field.getComboBox().containsId("tag1")).isTrue();
    }

    @Test
    void existingTokenIsNotDuplicated() {
        field.getContainerDataSource().addItem("dup");
        assertThat(field.getComboBox().getItemIds()).hasSize(1);
        field.simulateNewItemInput("dup");
        assertThat(field.getComboBox().getItemIds()).hasSize(1);
    }

    @Test
    void rememberNewTokensFalseSkipsContainer() {
        field.setRememberNewTokens(false);
        field.simulateNewItemInput("volatile");
        assertThat(field.getComboBox().containsId("volatile")).isFalse();
    }

    @Test
    void newTokenIsKeyedByItsIdNotItsCaption() {
        TestTokenField captioned = withCaptionProperty();

        captioned.simulateNewItemInput("id-123");

        assertThat(captioned.getComboBox().getItemIds())
                .containsExactly("id-123");
    }

    @Test
    void newTokenGetsItsCaptionWrittenToTheCaptionProperty() {
        TestTokenField captioned = withCaptionProperty();

        captioned.simulateNewItemInput("id-123");

        assertThat(captioned.getComboBox()
                .getContainerProperty("id-123", CAPTION_PROPERTY).getValue())
                        .isEqualTo("Pretty id-123");
    }

    /**
     * A field whose captions differ from its ids, which is the case the two
     * were confused in.
     * <p>
     * The caption comes from an override rather than from
     * {@code setTokenCaption}, because the caption map is only consulted for
     * an id the container already holds — a separate defect, tracked as <a
     * href="https://github.com/vaadin-tokenfield/tokenfield/issues/8">#8</a>,
     * and the wrong thing to lean a test for this one on. Overriding is what
     * the class documentation points at for captioning a new token anyway.
     * </p>
     */
    private static TestTokenField withCaptionProperty() {
        TestTokenField captioned = new TestTokenField() {

            private static final long serialVersionUID = 1L;

            @Override
            public String getTokenCaption(Object tokenId) {
                return "Pretty " + tokenId;
            }
        };
        captioned.getComboBox().addContainerProperty(CAPTION_PROPERTY,
                String.class, "");
        captioned.setTokenCaptionPropertyId(CAPTION_PROPERTY);
        return captioned;
    }
}
