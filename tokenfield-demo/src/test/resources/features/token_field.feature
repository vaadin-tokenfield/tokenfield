Feature: TokenField everyday usage
  As a user filling in a multi-value field (tags, recipients, categories...)
  I want to add, review and remove tokens with the keyboard or the mouse
  So that I can build up a list of values without leaving the field

  # Each scenario below opens one of the demo's six example panels, chosen
  # by a business-language name rather than the demo's own on-screen Panel
  # caption (see DemoSteps#panelIndexFor):
  #   "Basic"                         -> "Basic"
  #   "Comma separated"               -> "Comma separated"
  #   "Address book"                  -> "Full featured example"
  #   "Data binding and buffering"    -> "Data binding and buffering"
  #   "Layout and insert position"    -> "Layout and InsertPosition"
  #   "JPA address book"              -> "Full featured example, JPAContainer"

  Scenario: The demo shows all six examples with their inputs ready
    Then the demo page shows all six example panels, each with its own input

  # ---------------------------------------------------------------------
  # Basic — plain TokenField, default settings.
  # ---------------------------------------------------------------------

  Scenario: Typing a value and pressing Enter creates a token
    Given the "Basic" example
    When I type "urgent" and press Enter
    Then a token chip labeled "urgent" appears in the field
    And the input is empty again, ready for the next value

  Scenario: Clicking a token chip removes it
    Given the "Basic" example
    And I have added the token "urgent"
    When I click the "urgent" token chip
    Then the "urgent" token is removed from the field

  Scenario: Backspace on an empty input removes the most recently added token
    Given the "Basic" example
    And I have added the tokens "first" and "last", in that order
    When the input is empty and I press Backspace
    Then the "last" token is removed from the field
    And the "first" token remains

  Scenario: A value typed once is offered again as a suggestion
    Given the "Basic" example
    And I have added the token "urgent"
    When I start typing "urg"
    Then "urgent" appears in the suggestion list

  Scenario: Adding the same value twice does not create a duplicate token
    Given the "Basic" example
    And I have added the token "urgent"
    When I type "urgent" and press Enter
    Then the field still contains exactly one "urgent" token

  # ---------------------------------------------------------------------
  # Comma separated — a custom onTokenInput/rememberToken splits typed or
  # pasted text on commas, one token per non-blank segment.
  # ---------------------------------------------------------------------

  Scenario: Pasting a comma-separated list creates one token per entry
    Given the "Comma separated" example
    When I type "a, b, c" and press Enter
    Then a token chip labeled "a" appears in the field
    And a token chip labeled "b" appears in the field
    And a token chip labeled "c" appears in the field

  Scenario: Blank entries between commas are silently dropped
    Given the "Comma separated" example
    When I type "x,,y ," and press Enter
    Then only the tokens "x" and "y" appear in the field

  Scenario: The field prompts with an example of the expected format
    Given the "Comma separated" example
    Then the input shows the placeholder "tag, another, yetanother"

  # ---------------------------------------------------------------------
  # Address book — TokenField backed by an address-book container, with two
  # contacts already added as tokens plus one off-container email. Tokens
  # display as "Name <email>"; typing an address that isn't in the address
  # book prompts to add it there.
  #
  # Every scenario in this section runs twice, against two panels that
  # differ only in their container:
  #
  #   "Address book"     -> a BeanItemContainer of Contact beans, seeded
  #                         deterministically (DemoRoot.generateTestContainer()
  #                         uses `new Random(5)`), item id = the bean.
  #   "JPA address book" -> a JPAContainer over an H2 in-memory database,
  #                         seeded from JpaContacts.CONTACTS to match,
  #                         item id = the generated entity id.
  #
  # Running one spec against both is the point: it is what says TokenField's
  # full feature set works over a lazy, database-backed container and not
  # only an in-memory one. Where the JPA panel needs application code the
  # BeanItemContainer one does not, JpaAddressBookPanel says why.
  # ---------------------------------------------------------------------

  Scenario Outline: The seeded contacts are shown with their name and email
    Given the "<example>" example
    Then a token chip labeled "Linus Adams <linus.adams@example.com>" appears in the field
    And a token chip labeled "Robert Jones <robert.jones@example.com>" appears in the field
    And a token chip labeled "thatnewguy@example.com <thatnewguy@example.com>" appears in the field

    Examples:
      | example          |
      | Address book     |
      | JPA address book |

  Scenario Outline: A token added off the address book is visually marked as such
    Given the "<example>" example
    Then the "thatnewguy@example.com" token is marked as not part of the address book
    And the "Linus Adams" token is not marked as off-book

    Examples:
      | example          |
      | Address book     |
      | JPA address book |

  Scenario Outline: Typing part of a name filters the suggestions to matches
    Given the "<example>" example
    When I start typing "e"
    Then all visible suggestions contain "e"

    Examples:
      | example          |
      | Address book     |
      | JPA address book |

  Scenario Outline: Selecting a suggested contact adds it as a token immediately
    Given the "<example>" example
    When I type "Einstein" and pick the matching suggestion
    Then a token chip labeled "Nathan Einstein" appears in the field
    And no confirmation window is shown

    Examples:
      | example          |
      | Address book     |
      | JPA address book |

  # The trailing input check is issue #14: these panels remember nothing
  # (setRememberNewTokens(false)), which used to leave the address the user
  # typed sitting in the input after it had been turned into a token.
  Scenario Outline: Entering an address not in the address book asks whether to keep it
    Given the "<example>" example
    When I type "new@example.com" and press Enter
    Then a "New Contact" window opens
    And the input no longer shows "new@example.com"

    Examples:
      | example          |
      | Address book     |
      | JPA address book |

  Scenario Outline: Declining to add a new contact still keeps it as a token for this field
    Given the "<example>" example
    When I type "new@example.com" and press Enter
    Then a "New Contact" window opens
    When I choose "Don't add" in the "New Contact" window
    Then the "New Contact" window closes
    And a token chip labeled "new@example.com" appears in the field
    And the "new@example.com" token is marked as not part of the address book

    Examples:
      | example          |
      | Address book     |
      | JPA address book |

  # For the JPA panel this is the one scenario that writes to the database:
  # "Add to contacts" persists the contact and adds the entity id it is
  # given back as the token. Every UI init reseeds the table, so the row it
  # leaves behind does not carry into the next scenario.
  Scenario Outline: Adding a new contact makes it a regular, unmarked token
    Given the "<example>" example
    When I type "another@example.com" and press Enter
    Then a "New Contact" window opens
    When I choose "Add to contacts" in the "New Contact" window
    Then the "New Contact" window closes
    And a token chip labeled "another@example.com" appears in the field
    And the "another@example.com" token is not marked as off-book

    Examples:
      | example          |
      | Address book     |
      | JPA address book |

  Scenario Outline: Cancelling removal of an existing contact's token keeps it
    Given the "<example>" example
    When I click the "Linus Adams" token chip
    Then a "Remove Linus Adams" window opens
    When I choose "Cancel" in the "Remove Linus Adams" window
    Then the "Remove Linus Adams" window closes
    And a token chip labeled "Linus Adams" appears in the field

    Examples:
      | example          |
      | Address book     |
      | JPA address book |

  Scenario Outline: Confirming removal of an existing contact's token deletes it
    Given the "<example>" example
    When I click the "Robert Jones" token chip
    Then a "Remove Robert Jones" window opens
    When I choose "Remove" in the "Remove Robert Jones" window
    Then the "Remove Robert Jones" window closes
    And the "Robert Jones" token is removed from the field

    Examples:
      | example          |
      | Address book     |
      | JPA address book |

  # The onTokenDelete override for these panels bypasses RemoveWindow
  # entirely for the Backspace path.
  Scenario Outline: Backspace removes the last contact token without asking for confirmation
    Given the "<example>" example
    When the input is empty and I press Backspace
    Then no confirmation window is shown
    And the "thatnewguy@example.com" token is removed from the field
    And a token chip labeled "Linus Adams" appears in the field
    And a token chip labeled "Robert Jones" appears in the field

    Examples:
      | example          |
      | Address book     |
      | JPA address book |

  # ---------------------------------------------------------------------
  # Data binding and buffering — TokenField shares its item container with
  # a ListSelect and is buffered: new tokens land in the shared container
  # right away, but the field's own value (and so the ListSelect's
  # selection) only updates once committed.
  # ---------------------------------------------------------------------

  Scenario: A newly added token is visible everywhere but not yet selected
    Given the "Data binding and buffering" example
    When I type "Six" and press Enter
    Then a token chip labeled "Six" appears in the field
    And "Six" appears as an available option in the linked selection
    And no option is yet marked as selected in the linked selection

  Scenario: Committing the field applies the pending selection
    Given the "Data binding and buffering" example
    And I have added the token "Six"
    When I click the commit button
    Then "Six" becomes the selected entry in the linked selection

  Scenario: Picking an existing entry as a token does not require a commit to appear as a token
    Given the "Data binding and buffering" example
    When I type "Three" and pick the matching suggestion
    Then a token chip labeled "Three" appears in the field
    And no option is yet marked as selected in the linked selection

  # ---------------------------------------------------------------------
  # Layout and insert position — same TokenField, configurable layout,
  # insert position and read-only state. Starts on CssLayout / BEFORE / not
  # read-only.
  #
  # Note: Backspace under InsertPosition.AFTER is deliberately not covered
  # here — the client-side "after" flag is never assigned from server
  # state, so Backspace always deletes regardless of insert position. That
  # is a known, out-of-scope production gap, not something this suite pins.
  # ---------------------------------------------------------------------

  Scenario Outline: Existing tokens survive a layout change
    Given the "Layout and insert position" example
    And I have added the tokens "a" and "b", in that order
    When I switch the field's layout to "<layout>"
    Then the field now uses a "<layout>" layout
    And both tokens "a" and "b" are still present

    Examples:
      | layout             |
      | Horizontal Layout   |
      | Grid Layout         |

  Scenario Outline: New tokens are inserted before or after the input, as configured
    Given the "Layout and insert position" example
    And the insert position is set to "<position>"
    When I type "x" and press Enter
    Then the token chip appears "<order>" the input field

    Examples:
      | position | order  |
      | BEFORE   | before |
      | AFTER    | after  |

  Scenario: Making the field read-only hides the input but keeps existing tokens
    Given the "Layout and insert position" example
    And I have added the token "kept"
    When I mark the field as read-only
    Then the text input is no longer shown
    And a token chip labeled "kept" appears in the field
    When I mark the field as editable again
    Then the text input reappears

  # ---------------------------------------------------------------------
  # JPA address book, on its own — the scenarios above already cover this
  # panel's behaviour against the BeanItemContainer one. What is left is
  # the one place the two deliberately differ.
  #
  # Typing a whole value that names someone already in the address book:
  # the BeanItemContainer panel treats typed text as an email address, so
  # a name does not match and it offers to add a new contact. The JPA
  # panel resolves the text against the container by name or email first
  # (JpaContacts.findId), because letting it through would write a
  # duplicate row to the database rather than just duplicating a bean.
  # ---------------------------------------------------------------------

  Scenario: Typing the name of an existing contact adds that contact
    Given the "JPA address book" example
    When I type "Nathan Einstein" and press Enter
    Then a token chip labeled "Nathan Einstein <nathan.einstein@example.com>" appears in the field
    And no confirmation window is shown
