/*
 * Copyright 2010-2013 Marc Englund
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.vaadin.tokenfield.jpa;

import java.util.List;
import java.util.Locale;

import javax.persistence.EntityManager;

import org.vaadin.tokenfield.Contact;

import com.vaadin.addon.jpacontainer.JPAContainer;
import com.vaadin.addon.jpacontainer.JPAContainerFactory;

/**
 * The in-memory address book behind the demo's JPA address-book panel.
 * <p>
 * The whole database lives in H2's memory and is created on first use, so the
 * demo WAR still deploys with nothing to install; see
 * {@code META-INF/persistence.xml}. The contact list is a fixed literal rather
 * than generated, so the browser suite can assert on specific suggestions —
 * and it mirrors what {@code DemoRoot.generateTestContainer()} seeds the
 * BeanItemContainer address book with, so the same scenarios can be run
 * against either container.
 * </p>
 */
public final class JpaContacts {

    /** Must match the unit name in {@code META-INF/persistence.xml}. */
    private static final String PERSISTENCE_UNIT = "tokenfield-demo";

    /**
     * Seed data, as {@code name|email} pairs.
     * <p>
     * The first two are what the JPA address-book panel pre-adds as tokens, so
     * they are the two the browser scenarios name.
     * </p>
     */
    private static final String[] CONTACTS = {
            "Linus Adams|linus.adams@example.com",
            "Robert Jones|robert.jones@example.com",
            "Nathan Einstein|nathan.einstein@example.com",
            "Sarah Fielding|sarah.fielding@example.com",
            "Peter Sheridan|peter.sheridan@example.com",
            "Jane Richards|jane.richards@example.com",
            "Marc Thompson|marc.thompson@example.com",
            "Nicole Beck|nicole.beck@example.com",
            "Laura Halas|laura.halas@example.com",
            "Joe Black|joe.black@example.com" };

    private JpaContacts() {
        // static utility
    }

    /**
     * Builds a JPAContainer over the contact table.
     *
     * @return a fresh, read-write, write-through container of
     *         {@link Contact}
     */
    public static JPAContainer<Contact> container() {
        JPAContainer<Contact> container = JPAContainerFactory.make(
                Contact.class, PERSISTENCE_UNIT);
        // The default for JPAContainerFactory.make, spelled out: an added
        // contact must be queryable straight away, since the panel turns round
        // and adds it as a token.
        container.setAutoCommit(true);
        return container;
    }

    /**
     * Puts the contact table back to exactly {@link #CONTACTS}.
     * <p>
     * Called once per UI init, which is what keeps the JPA panel behaving like
     * the in-memory ones beside it: every browser tab, and so every browser
     * test scenario, starts from the same address book however the previous one
     * left it. The cost is that opening a second tab resets the first one's —
     * fine for a demo, and the alternative is scenarios that only pass in the
     * order they happen to run in.
     * </p>
     */
    public static synchronized void resetToSeedData() {
        EntityManager em = entityManager();
        try {
            em.getTransaction().begin();
            em.createQuery("DELETE FROM Contact c").executeUpdate();
            for (String contact : CONTACTS) {
                String[] parts = contact.split("\\|");
                em.persist(new Contact(parts[0], parts[1]));
            }
            em.getTransaction().commit();
        } finally {
            em.close();
        }
    }

    /**
     * Looks a contact up the way a user would name one — by either the whole
     * displayed name or the whole email address, ignoring case.
     * <p>
     * This is what lets the JPA address-book panel tell "the user picked
     * someone already in the book" from "the user typed a new address": a
     * {@code JPAContainer} is keyed by entity id, so the container cannot
     * answer that question about raw text, and adding the contact a second
     * time would write a duplicate row rather than merely duplicating a bean.
     * </p>
     *
     * @param nameOrEmail
     *            what the user typed
     * @return the matching contact's entity id, or null if the address book has
     *         no such contact
     */
    public static Long findId(String nameOrEmail) {
        if (nameOrEmail == null || nameOrEmail.trim().isEmpty()) {
            return null;
        }
        EntityManager em = entityManager();
        try {
            List<Long> ids = em
                    .createQuery("SELECT c.id FROM Contact c"
                            + " WHERE LOWER(c.name) = :v"
                            + " OR LOWER(c.email) = :v", Long.class)
                    .setParameter("v",
                            nameOrEmail.trim().toLowerCase(Locale.ROOT))
                    .getResultList();
            return ids.isEmpty() ? null : ids.get(0);
        } finally {
            em.close();
        }
    }

    /**
     * A fresh EntityManager for the demo's own queries, to be closed by the
     * caller.
     * <p>
     * Obtained the same way JPAContainer does it:
     * {@code JPAContainerFactory.createEntityManagerForPersistenceUnit} caches
     * one EntityManagerFactory per unit name and is what
     * {@code JPAContainerFactory.make} itself calls, so seeding here and
     * reading through a container there go through the same factory rather than
     * two with separate views of the database.
     * </p>
     */
    private static EntityManager entityManager() {
        return JPAContainerFactory
                .createEntityManagerForPersistenceUnit(PERSISTENCE_UNIT);
    }
}
