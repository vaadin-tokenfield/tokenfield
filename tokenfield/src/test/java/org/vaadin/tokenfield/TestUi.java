package org.vaadin.tokenfield;

import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinSession;
import com.vaadin.ui.UI;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A minimal attached {@link UI}, for tests that need a real
 * {@link com.vaadin.ui.ConnectorTracker}.
 *
 * <p>{@code markAsDirty()} is a no-op until a component has a UI, so any test
 * about dirty-marking has to attach the component to one. A UI in turn needs a
 * {@link VaadinSession} that reports itself as locked, because the connector
 * API asserts the session lock is held.</p>
 */
class TestUi extends UI {

    private static final long serialVersionUID = 1L;

    TestUi() {
        setSession(new LockedSession());
    }

    @Override
    protected void init(VaadinRequest request) {
        // Nothing to initialise; tests set the content themselves.
    }

    /** Puts the tracker into the state it has while a response is written. */
    void startWritingResponse() {
        getConnectorTracker().setWritingResponse(true);
    }

    /** Undoes {@link #startWritingResponse()}. */
    void finishWritingResponse() {
        getConnectorTracker().setWritingResponse(false);
    }

    /** A session that is always locked by the calling thread. */
    private static final class LockedSession extends VaadinSession {

        private static final long serialVersionUID = 1L;

        private final Lock lock = new ReentrantLock();

        LockedSession() {
            super(null);
        }

        @Override
        public Lock getLockInstance() {
            return lock;
        }

        @Override
        public boolean hasLock() {
            return true;
        }
    }
}
