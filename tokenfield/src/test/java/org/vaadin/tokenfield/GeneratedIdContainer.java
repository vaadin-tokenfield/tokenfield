package org.vaadin.tokenfield;

import java.util.concurrent.atomic.AtomicLong;

import com.vaadin.data.Item;
import com.vaadin.data.util.IndexedContainer;

/**
 * Refuses an explicit item id and assigns {@code Long} ids of its own, the way
 * a {@code JPAContainer} does. Carries a {@code name} property to hold the
 * typed text.
 */
class GeneratedIdContainer extends IndexedContainer {
    private static final long serialVersionUID = 1L;

    static final String NAME = "name";

    private final AtomicLong nextId = new AtomicLong();

    GeneratedIdContainer() {
        addContainerProperty(NAME, String.class, null);
    }

    @Override
    public Item addItem(Object itemId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object addItem() {
        Long itemId = nextId.incrementAndGet();
        return super.addItem(itemId) == null ? null : itemId;
    }
}
