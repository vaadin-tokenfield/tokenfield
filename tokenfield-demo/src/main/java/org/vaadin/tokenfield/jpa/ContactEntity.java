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

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

/**
 * The persistent address-book contact behind the demo's JPAContainer panel.
 * <p>
 * Deliberately the smallest entity that still exercises the crash in
 * <a href="https://github.com/vaadin-tokenfield/tokenfield/issues/15">issue
 * #15</a>: it needs a generated identifier (so container item ids are entity
 * ids rather than the beans themselves) and a {@code String} property to hand
 * to {@link org.vaadin.tokenfield.TokenField#setTokenCaptionPropertyId(Object)},
 * which is what the ComboBox then builds its {@code SimpleStringFilter} on
 * while the user types.
 * </p>
 */
@Entity
public class ContactEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String name;

    private String email;

    public ContactEntity() {
        // JPA
    }

    public ContactEntity(String name, String email) {
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
}
