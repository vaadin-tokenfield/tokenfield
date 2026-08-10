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
package org.vaadin.tokenfield;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

/**
 * The example contact behind both address-book panels, mostly generated
 * setters/getters.
 * <p>
 * One class, two containers — which is the point of the two panels. The
 * "Full featured example" keeps these in a {@code BeanItemContainer}, where the
 * bean itself is the item id, so {@link #equals(Object)} and
 * {@link #hashCode()} (by email, the natural key) are what identify a contact.
 * "Full featured example, JPAContainer" stores the same class in H2 through
 * JPA, where the item id is {@link #getId()} instead. Sharing the class is what
 * lets one set of browser scenarios run against both panels and mean something.
 * </p>
 * <p>
 * JPA requires an entity to be a top-level class with a no-argument
 * constructor, which is why this is not a nested class of {@code DemoRoot} any
 * more. Nothing forces a bean in the {@code BeanItemContainer} panel to be
 * persisted: those instances simply never meet an EntityManager, and their
 * {@link #getId()} stays null.
 * </p>
 */
@Entity
public class Contact implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Null until persisted. This is the item id in the JPAContainer panel; the
     * BeanItemContainer panel never sets it and uses the bean itself instead.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String name;

    private String email;

    public Contact() {
        // JPA
    }

    public Contact(String name, String email) {
        this.name = name;
        this.email = email;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String toString() {
        return email;
    }

    /**
     * By email, the contact's natural key — not by {@link #getId()}, which is
     * null for every bean in the BeanItemContainer panel and so would make all
     * of them equal to each other.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Contact) {
            String other = ((Contact) obj).getEmail();
            return email == null ? other == null : email.equals(other);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return email == null ? 0 : email.hashCode();
    }
}
