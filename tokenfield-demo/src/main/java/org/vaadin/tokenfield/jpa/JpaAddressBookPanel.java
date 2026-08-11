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

import java.util.Arrays;
import java.util.Set;

import org.vaadin.tokenfield.Contact;
import org.vaadin.tokenfield.DemoRoot;
import org.vaadin.tokenfield.TokenField;

import com.vaadin.addon.jpacontainer.JPAContainer;
import com.vaadin.data.Item;
import com.vaadin.data.util.BeanItem;
import com.vaadin.shared.ui.combobox.FilteringMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Form;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

/**
 * The demo's "Full featured example" over a {@code JPAContainer} instead of a
 * {@code BeanItemContainer}.
 * <p>
 * Same address book, same three pre-added tokens, same "Name &lt;email&gt;"
 * captions, same add/remove confirmation windows — so the browser suite runs
 * one set of address-book scenarios against both panels and the container
 * implementation is the only thing that differs. That is the point of the
 * panel: to say that {@code TokenField}'s feature set works over a lazy,
 * database-backed container, not only the in-memory ones.
 * </p>
 * <h3>What a database-backed container changes</h3>
 * <p>
 * A {@code BeanItemContainer} is keyed by the bean, so a token id doubles as
 * the contact. A {@code JPAContainer} is keyed by the entity id — here a
 * {@code Long} — so this panel deals in two kinds of token id:
 * </p>
 * <ul>
 * <li>a {@code Long}, for a contact that is in the address book, and</li>
 * <li>the raw address {@code String} the user typed, for one that is not.</li>
 * </ul>
 * <p>
 * The overrides below all exist to handle the second kind. A
 * {@code JPAContainer} asked whether it contains {@code "new@example.com"} does
 * not answer false — it fails trying to convert that {@code String} to a
 * {@code Long}:
 * </p>
 *
 * <pre>
 * javax.persistence.PersistenceException: ... The object [new@example.com], of
 * class [class java.lang.String] ... could not be converted to [class
 * java.lang.Long]
 * </pre>
 * <p>
 * That is <a href=
 * "https://github.com/vaadin-tokenfield/tokenfield/issues/24">#24</a>. The
 * add-on reads such a refusal as "no such item", so an off-book token resolves
 * to its own string representation instead of propagating the exception. The
 * {@link #getTokenCaption(Object)} override below now arrives at the same
 * answer as the add-on would and is kept only to spell the rule out in one
 * place; an application written against this version does not need it.
 * </p>
 */
public class JpaAddressBookPanel extends Panel {

    private static final long serialVersionUID = 1L;

    /** Pre-added as a token, but deliberately never added to the address book. */
    private static final String OFF_BOOK_ADDRESS = "thatnewguy@example.com";

    public JpaAddressBookPanel() {
        super("Full featured example, JPAContainer");

        VerticalLayout l = new VerticalLayout();
        l.setMargin(true);
        l.setStyleName("black");
        setContent(l);

        final JPAContainer<Contact> contacts = JpaContacts.container();

        // we want this to be vertical
        VerticalLayout lo = new VerticalLayout();
        lo.setSpacing(true);

        final TokenField f = new JpaAddressBookField(lo, contacts);
        l.addComponent(f);
        // This would turn on the "fake tekstfield" look:
        f.setStyleName(TokenField.STYLE_TOKENFIELD);
        f.setWidth("100%");
        f.setInputWidth("100%");
        f.setContainerDataSource(contacts); // 'address book'
        f.setFilteringMode(FilteringMode.CONTAINS); // suggest
        // The caption property id also puts the ComboBox into
        // ITEM_CAPTION_MODE_PROPERTY, which is what makes it filter through the
        // container - a JPA query - rather than in memory.
        f.setTokenCaptionPropertyId("name"); // use name in input
        f.setInputPrompt("Enter contact name or new email address");
        f.setRememberNewTokens(false); // we'll do this via the dialog

        // Pre-add the same three the BeanItemContainer panel does: two from the
        // address book, one that is not in it.
        f.addToken(JpaContacts.findId("linus.adams@example.com"));
        f.addToken(JpaContacts.findId("robert.jones@example.com"));
        f.addToken(OFF_BOOK_ADDRESS);
    }

    /**
     * The field itself, with the same four overrides the BeanItemContainer
     * panel uses plus {@link #getTokenCaption(Object)}.
     */
    private static class JpaAddressBookField extends TokenField {

        private static final long serialVersionUID = 1L;

        private final JPAContainer<Contact> contacts;

        JpaAddressBookField(VerticalLayout lo,
                JPAContainer<Contact> contacts) {
            super(lo);
            this.contacts = contacts;
        }

        /**
         * True for a token id that names a row in the address book.
         * <p>
         * The type test is the whole point: it answers the question
         * {@code container.containsId(tokenId)} would answer for an in-memory
         * container, without handing the container an id it cannot hold.
         * </p>
         */
        private boolean isContact(Object tokenId) {
            return tokenId instanceof Long;
        }

        /** dialog if not in 'address book', otherwise just add */
        @Override
        protected void onTokenInput(Object tokenId) {
            // Picked from the suggestions: the ComboBox hands over a real id.
            if (isContact(tokenId)) {
                addAvoidingDuplicate(tokenId);
                return;
            }

            // Typed: the raw text, which may still name someone in the book.
            String typed = String.valueOf(tokenId);
            Long known = JpaContacts.findId(typed);
            if (known != null) {
                addAvoidingDuplicate(known);
                return;
            }

            Set<Object> set = (Set<Object>) getValue();
            if (set != null && set.contains(typed)) {
                Notification.show(typed + " is already added");
                return;
            }
            // don't add directly,
            // show custom "add to address book" dialog
            getUI().addWindow(new NewContactWindow(typed, this, contacts));
        }

        private void addAvoidingDuplicate(Object tokenId) {
            Set<Object> set = (Set<Object>) getValue();
            if (set != null && set.contains(tokenId)) {
                Notification.show(getTokenCaption(tokenId)
                        + " is already added");
                return;
            }
            addToken(tokenId);
        }

        /** show confirm dialog */
        @Override
        protected void onTokenClick(Object tokenId) {
            getUI().addWindow(new DemoRoot.RemoveWindow(tokenId,
                    getTokenCaption(tokenId), this));
        }

        /** just delete, no confirm */
        @Override
        protected void onTokenDelete(Object tokenId) {
            this.removeToken(tokenId);
        }

        /**
         * Resolves the caption without asking the container about an id it
         * cannot hold — see the class comment and #24. A contact's caption
         * comes from the container as usual; an off-book token is its own
         * address.
         */
        @Override
        public String getTokenCaption(Object tokenId) {
            if (isContact(tokenId)) {
                return super.getTokenCaption(tokenId);
            }
            return String.valueOf(tokenId);
        }

        /** custom caption + style if not in 'address book' */
        @Override
        protected void configureTokenButton(Object tokenId, Button button) {
            super.configureTokenButton(tokenId, button);
            // custom caption
            button.setCaption(getTokenCaption(tokenId) + " <"
                    + emailOf(tokenId) + ">");
            // width
            button.setWidth("100%");

            if (!isContact(tokenId)) {
                // it's not in the address book; style
                button.addStyleName(TokenField.STYLE_BUTTON_EMPHAZISED);
            }
        }

        /**
         * The address to show beside the name. A {@code BeanItemContainer}
         * token id prints as the contact's email all by itself
         * ({@code Contact.toString()}); an entity id prints as a number, so the
         * email has to be read off the item.
         */
        private String emailOf(Object tokenId) {
            if (!isContact(tokenId)) {
                return String.valueOf(tokenId);
            }
            Item item = contacts.getItem(tokenId);
            if (item == null) {
                return String.valueOf(tokenId);
            }
            return String.valueOf(item.getItemProperty("email").getValue());
        }
    }

    /**
     * The window used to add new contacts to the 'address book', the JPA
     * counterpart of {@code DemoRoot.EditContactWindow}.
     * <p>
     * "Add to contacts" persists the contact and adds the entity id it was
     * given as the token; "Don't add" adds the typed address itself, which
     * stays a token of this field only.
     * </p>
     */
    public static class NewContactWindow extends Window {

        private static final long serialVersionUID = 1L;

        NewContactWindow(final String typed, final TokenField f,
                final JPAContainer<Contact> contacts) {
            super("New Contact");
            VerticalLayout l = new VerticalLayout();
            setContent(l);
            final Contact contact = typed.contains("@")
                    ? new Contact("", typed)
                    : new Contact(typed, "");
            setModal(true);
            center();
            setWidth("250px");
            setStyleName("black");
            setResizable(false);

            // Just bind a Form to the entity via BeanItem. The generated id is
            // not the user's business, so it is left out of the form.
            Form form = new Form();
            form.setItemDataSource(new BeanItem<Contact>(contact),
                    Arrays.asList("name", "email"));
            form.setImmediate(true);
            l.addComponent(form);

            // layout buttons horizontally
            HorizontalLayout hz = new HorizontalLayout();
            l.addComponent(hz);
            hz.setSpacing(true);
            hz.setWidth("100%");

            Button dont = new Button("Don't add", new Button.ClickListener() {

                private static final long serialVersionUID = 1L;

                @Override
                public void buttonClick(ClickEvent event) {
                    // Not persisted, so it has no entity id: the token id is
                    // the address itself.
                    f.addToken(addressOf(contact));
                    f.getUI().removeWindow(NewContactWindow.this);
                }
            });
            hz.addComponent(dont);
            hz.setComponentAlignment(dont, Alignment.MIDDLE_LEFT);

            Button add = new Button("Add to contacts",
                    new Button.ClickListener() {

                        private static final long serialVersionUID = 1L;

                        @Override
                        public void buttonClick(ClickEvent event) {
                            if (isBlank(contact.getEmail())) {
                                contact.setEmail(contact.getName());
                            }
                            // addEntity returns the generated id, which is
                            // exactly the token id this field wants.
                            f.addToken(contacts.addEntity(contact));
                            f.getUI().removeWindow(NewContactWindow.this);
                        }
                    });
            hz.addComponent(add);
            hz.setComponentAlignment(add, Alignment.MIDDLE_RIGHT);
        }

        private static String addressOf(Contact contact) {
            return isBlank(contact.getEmail()) ? contact.getName()
                    : contact.getEmail();
        }

        private static boolean isBlank(String s) {
            return s == null || s.isEmpty();
        }
    }
}
