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

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import com.vaadin.addon.jpacontainer.JPAContainer;
import com.vaadin.addon.jpacontainer.JPAContainerFactory;

/**
 * The in-memory address book behind the demo's JPAContainer panel.
 * <p>
 * The whole database lives in H2's memory and is created on first use, so the
 * demo WAR still deploys with nothing to install; see
 * {@code META-INF/persistence.xml}. The contact list is a fixed literal rather
 * than generated, so the browser suite can assert on specific suggestions.
 * </p>
 */
public final class JpaContacts {

    /** Must match the unit name in {@code META-INF/persistence.xml}. */
    private static final String PERSISTENCE_UNIT = "tokenfield-demo";

    /** Seed data, as {@code name|email} pairs. */
    private static final String[] CONTACTS = {
            "Linus Torvalds|linus.torvalds@example.com",
            "Nathan Einstein|nathan.einstein@example.com",
            "Sarah Fielding|sarah.fielding@example.com",
            "Peter Sheridan|peter.sheridan@example.com",
            "Jane Richards|jane.richards@example.com",
            "Marc Thompson|marc.thompson@example.com",
            "Nicole Beck|nicole.beck@example.com",
            "Robert Jones|robert.jones@example.com",
            "Laura Halas|laura.halas@example.com",
            "Joe Black|joe.black@example.com" };

    private static volatile boolean seeded;

    private JpaContacts() {
        // static utility
    }

    /**
     * Builds a JPAContainer over the seeded contact table, seeding it first if
     * this is the first call.
     *
     * @return a fresh, read-write container of {@link ContactEntity}
     */
    public static JPAContainer<ContactEntity> container() {
        seed();
        return JPAContainerFactory.make(ContactEntity.class, PERSISTENCE_UNIT);
    }

    /**
     * Creates the schema and inserts {@link #CONTACTS} exactly once per JVM.
     * The demo has no lifecycle hook of its own to hang this on, and every
     * browser tab creates a new UI, so the guard is what keeps repeated visits
     * from stacking up duplicate rows.
     */
    private static synchronized void seed() {
        if (seeded) {
            return;
        }
        EntityManagerFactory emf = Persistence
                .createEntityManagerFactory(PERSISTENCE_UNIT);
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            for (String contact : CONTACTS) {
                String[] parts = contact.split("\\|");
                em.persist(new ContactEntity(parts[0], parts[1]));
            }
            em.getTransaction().commit();
        } finally {
            em.close();
        }
        seeded = true;
    }
}
