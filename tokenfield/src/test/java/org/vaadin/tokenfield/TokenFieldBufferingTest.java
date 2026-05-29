package org.vaadin.tokenfield;

import com.vaadin.data.util.ObjectProperty;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

/**
 * Tests for buffered mode: a field with {@code setBuffered(true)} must not
 * write through to the data-source until {@link TokenField#commit()} is called,
 * and {@link TokenField#discard()} must revert pending changes.
 *
 * <p>Uses Vaadin 7's {@link ObjectProperty} as a simple in-memory
 * {@link com.vaadin.data.Property} stand-in.</p>
 */
class TokenFieldBufferingTest {

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    void bufferedAddTokenNotWrittenThroughUntilCommit() {
        LinkedHashSet<Object> initial = new LinkedHashSet<Object>();
        ObjectProperty prop = new ObjectProperty(initial);

        TestTokenField f = new TestTokenField();
        f.setBuffered(true);
        f.setPropertyDataSource(prop);

        f.addToken("buffered");

        // Field's own view must contain the new token
        Set<Object> fieldVal = (Set<Object>) f.getValue();
        assertWithMessage("Field value must reflect the buffered token")
                .that(fieldVal).contains("buffered");

        // Property must NOT yet contain the token
        assertWithMessage("Property must not be updated before commit()")
                .that((Set<Object>) prop.getValue()).doesNotContain("buffered");

        f.commit();

        // After commit the property must be updated
        assertWithMessage("Property must contain the token after commit()")
                .that((Set<Object>) prop.getValue()).contains("buffered");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    void discardRevertsUncommittedChanges() {
        LinkedHashSet<Object> initial = new LinkedHashSet<Object>();
        initial.add("committed");
        ObjectProperty prop = new ObjectProperty(initial);

        TestTokenField f = new TestTokenField();
        f.setBuffered(true);
        f.setPropertyDataSource(prop);

        // The datasource value ("committed") should be visible in the field
        Set<Object> afterBind = (Set<Object>) f.getValue();
        assertWithMessage("Field must load the initial datasource value")
                .that(afterBind).contains("committed");
        assertThat(f.getTokenButtons()).hasSize(1);

        // Add a pending (uncommitted) token
        f.addToken("pending");
        Set<Object> withPending = (Set<Object>) f.getValue();
        assertThat(withPending).contains("pending");

        // Discard must revert to the datasource value
        f.discard();

        Set<Object> afterDiscard = (Set<Object>) f.getValue();
        assertWithMessage("Pending token must be gone after discard()")
                .that(afterDiscard).doesNotContain("pending");
        // The original token must still be present
        assertWithMessage("Original committed token must survive discard()")
                .that(afterDiscard).contains("committed");
        // Buttons reconciled: only one button for "committed"
        assertThat(f.getTokenButtons()).hasSize(1);
    }
}
