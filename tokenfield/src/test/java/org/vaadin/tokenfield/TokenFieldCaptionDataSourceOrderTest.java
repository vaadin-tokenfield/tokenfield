package org.vaadin.tokenfield;

import com.vaadin.data.util.IndexedContainer;
import com.vaadin.data.util.ObjectProperty;
import com.vaadin.ui.AbstractSelect.ItemCaptionMode;
import com.vaadin.ui.Button;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

/**
 * Regression tests for issue #8: token buttons showed the raw token id instead
 * of the configured caption when the data sources were set in the order
 * {@code setTokenCaption} &rarr; {@code setPropertyDataSource} &rarr;
 * {@code setContainerDataSource}.
 *
 * <p>Token buttons are captioned while the value is being bound
 * ({@code setPropertyDataSource} &rarr; {@code setInternalValue} &rarr;
 * {@code addTokenButton} &rarr; {@code configureTokenButton} &rarr;
 * {@link TokenField#getTokenCaption(Object)}), which is before the container
 * data source arrives. The caption must not depend on the token already being
 * a member of that container.</p>
 */
class TokenFieldCaptionDataSourceOrderTest {

    /** Strips the trailing " &times;" the token button appends to a caption. */
    private static String captionOf(TestTokenField f, Object tokenId) {
        Button b = f.getTokenButtons().get(tokenId);
        assertWithMessage("No token button was created for " + tokenId).that(b).isNotNull();
        return b.getCaption().replace(" ×", "");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static ObjectProperty propertyWith(Object... tokenIds) {
        LinkedHashSet<Object> value = new LinkedHashSet<Object>();
        for (Object tokenId : tokenIds) {
            value.add(tokenId);
        }
        return new ObjectProperty(value);
    }

    /**
     * The exact sequence from issue #8: captions registered first, value bound
     * second, container set last.
     */
    @Test
    void explicitCaptionIsUsedWhenContainerIsSetAfterPropertyDataSource() {
        TestTokenField f = new TestTokenField();
        f.setTokenCaption("id-123", "Pretty Name");

        f.setPropertyDataSource(propertyWith("id-123"));

        IndexedContainer c = new IndexedContainer();
        c.addItem("id-123");
        f.setContainerDataSource(c);

        assertWithMessage("getTokenCaption must return the registered caption even though"
                + " the container was still empty when the button was built")
                .that(f.getTokenCaption("id-123")).isEqualTo("Pretty Name");
        assertThat(captionOf(f, "id-123")).isEqualTo("Pretty Name");
    }

    /**
     * A caption registered before the value is bound must reach the button even
     * if no container data source is ever set.
     */
    @Test
    void explicitCaptionIsUsedWithoutAnyContainerDataSource() {
        TestTokenField f = new TestTokenField();
        f.setTokenCaption("a", "Alpha");

        f.setPropertyDataSource(propertyWith("a"));

        assertThat(captionOf(f, "a")).isEqualTo("Alpha");
    }

    /**
     * The documented workaround order (container first) must keep working.
     */
    @Test
    void explicitCaptionIsUsedWhenContainerIsSetFirst() {
        TestTokenField f = new TestTokenField();
        IndexedContainer c = new IndexedContainer();
        c.addItem("id-123");
        f.setContainerDataSource(c);
        f.setTokenCaption("id-123", "Pretty Name");

        f.setPropertyDataSource(propertyWith("id-123"));

        assertThat(captionOf(f, "id-123")).isEqualTo("Pretty Name");
    }

    /**
     * Tokens with no caption of any kind keep falling back to the id string,
     * which is what the removed {@code containsId} guard used to provide.
     */
    @Test
    void unknownTokenWithoutCaptionStillFallsBackToIdString() {
        TestTokenField f = new TestTokenField();
        f.setPropertyDataSource(propertyWith("plain-id"));

        assertThat(f.getTokenCaption("plain-id")).isEqualTo("plain-id");
        assertThat(captionOf(f, "plain-id")).isEqualTo("plain-id");
    }

    /**
     * In {@link ItemCaptionMode#EXPLICIT} an id with no explicit caption has no
     * caption to offer, so the id string must be used rather than the empty
     * string {@code AbstractSelect.getItemCaption} returns.
     */
    @Test
    void explicitModeWithoutRegisteredCaptionFallsBackToIdString() {
        TestTokenField f = new TestTokenField();
        f.setTokenCaptionMode(ItemCaptionMode.EXPLICIT);
        IndexedContainer c = new IndexedContainer();
        c.addItem("uncaptioned");
        f.setContainerDataSource(c);

        assertThat(f.getTokenCaption("uncaptioned")).isEqualTo("uncaptioned");
    }

    /**
     * Captions that live in the container (
     * {@link ItemCaptionMode#PROPERTY}) are only readable once the container is
     * there. Buttons built before that must be re-captioned when the container
     * data source arrives.
     */
    @Test
    void propertyModeCaptionIsAppliedWhenContainerArrivesAfterTheButtons() {
        TestTokenField f = new TestTokenField();
        f.setTokenCaptionMode(ItemCaptionMode.PROPERTY);
        f.setTokenCaptionPropertyId("name");

        f.setPropertyDataSource(propertyWith("id-123"));
        assertWithMessage("Without a container there is no caption property to read yet")
                .that(captionOf(f, "id-123")).isEqualTo("id-123");

        IndexedContainer c = new IndexedContainer();
        c.addContainerProperty("name", String.class, null);
        c.addItem("id-123").getItemProperty("name").setValue("Pretty Name");
        f.setContainerDataSource(c);

        assertThat(f.getTokenCaption("id-123")).isEqualTo("Pretty Name");
        assertThat(captionOf(f, "id-123")).isEqualTo("Pretty Name");
    }

    /**
     * Re-captioning on {@code setContainerDataSource} must not clobber tokens
     * the new container knows nothing about.
     */
    @Test
    void tokensMissingFromANewContainerKeepFallingBackToTheirIdString() {
        TestTokenField f = new TestTokenField();
        f.setTokenCaptionMode(ItemCaptionMode.PROPERTY);
        f.setTokenCaptionPropertyId("name");
        f.setPropertyDataSource(propertyWith("known", "unknown"));

        IndexedContainer c = new IndexedContainer();
        c.addContainerProperty("name", String.class, null);
        c.addItem("known").getItemProperty("name").setValue("Known One");
        f.setContainerDataSource(c);

        assertThat(captionOf(f, "known")).isEqualTo("Known One");
        assertThat(captionOf(f, "unknown")).isEqualTo("unknown");
    }
}
