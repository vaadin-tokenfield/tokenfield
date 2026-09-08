# Research: adding a user-typed item to a container that generates its own ids

Context: issue #39. `TokenField.rememberToken(String)` must add a typed token to
the ComboBox's container. Containers that generate their own ids (JPAContainer,
SQLContainer) throw `UnsupportedOperationException` from `Container.addItem(Object)`
and only support `Container.addItem()`. Versions checked: vaadin-server 7.7.17,
JPAContainer 3.2.0 and 4.0.0, vaadin-compatibility-server 8.14.3, vaadin-server
8.14.3. All line numbers refer to the `-sources.jar` files listed at the end.

## Question and short answer

How does Vaadin 7 itself, and other components, add a new item typed by the user to
a container that only supports `addItem()`? Do they catch the exception, re-point
the selection at the generated id, and handle entity constraints?

Short answer: nothing in Vaadin 7 does. `AbstractSelect.DefaultNewItemHandler` calls
`addItem(newItemCaption)` unguarded, so on JPAContainer and SQLContainer it throws
and the ComboBox's "new items allowed" mode is unusable without a custom
`NewItemHandler`. The official docs say exactly this: adding new items "is not
possible if the selection component is ... bound to a Container that does not allow
adding new items", and show a custom handler that builds the domain object itself,
adds it through the container's typed API (`addBean`, by analogy `addEntity`) and
then selects the new object. Nobody catches `UnsupportedOperationException` to fall
back to `addItem()`. Nobody re-points a selection because the handler only ever
learns the typed string, never a container id. JPAContainer's own editors
(`MasterDetailEditor`, `OneToOneForm`) create the entity with `newInstance()`, set
required properties on it first, and only then call `addEntity`. Constraint handling
therefore happens by ordering, not by exception handling. The #39 fallback is a
reasonable extension of the framework's behaviour, but it has no precedent in
primary sources and it flushes a blank entity in write-through mode.

## Per-component findings

| Component / container | `addItem(Object)` | `addItem()` | How a typed new item reaches the container | Generated id re-pointed into the selection? | Constraint handling |
|---|---|---|---|---|---|
| `Container` contract (7.7.17) | Optional; "Returns null if the operation fails or the Container already contains a Item with the given ID"; throws `UnsupportedOperationException` "if adding an item with an explicit item ID is not supported" (`Container.java:170-190`) | Optional; returns the new id or null; throws `UnsupportedOperationException` "if adding an item without an explicit item ID is not supported" (`Container.java:192-211`) | n/a | n/a | n/a. No capability query exists; only the exception. |
| `AbstractSelect` / `DefaultNewItemHandler` | Delegates to `items.addItem(itemId)`; exception propagates. Its javadoc still says "If the function is unsupported, it always returns null" (`AbstractSelect.java:893-914`), which the code does not honour. | Delegates to `items.addItem()`; exception propagates (`AbstractSelect.java:881-889`) | `changeVariables` reads the `newitem` variable and calls `getNewItemHandler().addNewItem(newitem)` (`AbstractSelect.java:455-463`). `DefaultNewItemHandler.addNewItem` does `addItem(newItemCaption)`, then if a caption property id is set writes the caption into `getContainerProperty(newItemCaption, captionPropertyId)`, then `setValue(newItemCaption)` or adds it to the multi-select set (`AbstractSelect.java:591-616`). | No. The typed string is the id: "The text entered by the user is used as id. Note that data-source must allow adding new items." (`AbstractSelect.java:1148-1156`) | None. No try/catch anywhere on the path. |
| `ComboBox` (7.7.17) | inherited | inherited | Same as above but in its own `changeVariables`; after `addNewItem` it resets `filterstring`/`prevfilterstring` so the option list is rebuilt (`ComboBox.java:757-765`). `setNewItemsAllowed(false)` in the constructor (`ComboBox.java:158`). | No | None |
| `AbstractSelect.readItem` (declarative) | | | `addItem(itemId)` + `setItemCaption`, or `addItem(itemId = caption)` when no `item-id` attribute (`AbstractSelect.java:2253-2259`). Same "caption is the id" convention. | n/a | n/a |
| `IndexedContainer` | Adds under the given id (`IndexedContainer.java:244-259`) | Generates an `Integer` id from a counter starting at 1, then calls `addItem(id)` (`IndexedContainer.java:225-240`, `378-384`) | Both paths work, so `DefaultNewItemHandler` works. | n/a | n/a |
| `AbstractInMemoryContainer` (base of the bean containers) | Throws `UnsupportedOperationException("Adding items not supported. Override the relevant addItem*() methods ...")` (`AbstractInMemoryContainer.java:405-408`) | Same (`:411-414`) | | | |
| `BeanItemContainer` | Overridden: the argument **is the bean**; `addItem(Object itemId)` is `super.addBean((BEANTYPE) itemId)` (`BeanItemContainer.java:220-230`). A typed `String` fails `validateBean` (`getBeanType().isAssignableFrom(...)`, `AbstractBeanContainer.java:440-458`) and `addItem` returns **null**, so `DefaultNewItemHandler` silently does nothing. | Not overridden: throws (inherited from `AbstractInMemoryContainer`) | Docs prescribe a custom `NewItemHandler` that constructs the bean, calls `container.addBean(...)` and `select.select(newPlanet)` (docs, "Allowing Adding New Items"). | Yes, in the custom handler, because the handler holds the bean, which *is* the id. | The handler constructs the bean with whatever constructor it likes. |
| `BeanContainer` / `AbstractBeanContainer` | `addItem(IDTYPE, BEANTYPE)` two-arg form is the public API (`BeanContainer.java:83-91`); the one-arg `Container.addItem(Object)` is the inherited throwing one | throws (inherited) | Custom handler only | Handler chooses the id | Handler's job |
| `SQLContainer` | Unconditional `throw new UnsupportedOperationException()` (`SQLContainer.java:1386-1388`) | Creates a `TemporaryRowId` with an empty key array and a `RowItem` with null column properties. If `autoCommit` is on, inserts **immediately** via `TableQuery.storeRowImmediately` and returns the **final** `RowId` from `getGeneratedKeys()`; otherwise appends to `addedItems` and returns the temporary id (`SQLContainer.java:144-208`; `TableQuery.java:289-330`). | No selection component in the framework does it; the docs say "Adding items to an SQLContainer object can only be done via the addItem() method." | Buffered mode: temporary id stays in the item; the real key is announced through `QueryDelegate.RowIdChangeListener` after `commit()` (`QueryDelegate.java:172-198`; `SQLContainer.java:895-949`). Docs: "these events are not fired if auto commit mode is enabled." | In auto-commit mode the blank row is inserted at once: a NOT NULL column fails the insert, `addItem()` logs a warning, rolls back and **returns null** (`SQLContainer.java:174-201`). In buffered mode constraints are only hit at `commit()`. |
| `JPAContainer` 3.2.0 | Unconditional `throw new UnsupportedOperationException()`, javadoc "This functionality is not supported by this implementation." (`JPAContainer.java:669-677`) | `getEntityClass().newInstance()` then `addEntity(newInstance)`; only `InstantiationException` / `IllegalAccessException` are converted to `UnsupportedOperationException`, everything else (e.g. a `PersistenceException`) propagates. Javadoc: "not fully supported ... tries to call empty parameter constructor and add entity as such to database. If identifiers are not autogenerated or empty parameter constructor does not exist, the operation will fail" (`JPAContainer.java:679-697`) | The container's own API is `addEntity(T)` (docs: "You can add new entities to a JPAContainer with the addEntity() method. It returns the item ID of the new entity."). JPAContainer's field factory editors call `newInstance()`, set the back-reference property on the bean, then `addEntity` (`MasterDetailEditor.java:184-198`; `OneToOneForm.java:102-111`). | The returned id is the entity's `@Id` value in write-through mode, read back from the merged copy (`JPAContainer.java:1106-1116`), or a random `UUID` in buffered mode (`BufferedContainerDelegate.java:260-267`). No component re-points anything; the caller gets the id from `addEntity`. | Write-through: `MutableLocalEntityProvider.addEntity` does `em.merge(entity); em.flush()` inside a transaction (`MutableLocalEntityProvider.java:108-147`). Constraints are hit immediately. Buffered: the entity only reaches the provider in `BufferedContainerDelegate.commit()` (`:210-243`). |
| `JPAContainer` 4.0.0 (Vaadin 8 compat) | identical | identical | identical | identical | identical. The diff against 3.2.0 is only `com.vaadin.data` to `com.vaadin.v7.data` imports. |
| `com.vaadin.v7.ui.AbstractSelect` (compat 8.14.3) | same as 7.7.17 | same | `DefaultNewItemHandler` unchanged apart from `newitem.length() > 0` becoming `!newitem.isEmpty()` (`v7/ui/AbstractSelect.java:598-616`) | No | None |
| `com.vaadin.ui.ComboBox` (Vaadin 8 core, 8.14.3) | n/a (no Container) | n/a | `NewItemHandler` is a deprecated `Consumer<String>` (`ComboBox.java:130-133`); since 8.4 `NewItemProvider<T>` is a `Function<String, Optional<T>>` whose javadoc says it "adds a new item based on user input" and returns the created item for "automatic selection handling" (`ComboBox.java:136-147`). | Yes: the provider returns the new `T`, which the ComboBox selects. The application owns creation and persistence. | Application's job. |

## Established patterns

1. **Typed text is the item id.** Vaadin's default handler adds the item under the
   caption and writes the caption property under that same key
   (`AbstractSelect.java:600-607`). Docs: "The default implementation ... adds the
   item using the entered caption as the item ID, and if the selection component
   gets the captions from a property, copies the caption to that property."

2. **Containers that name their own items need a custom `NewItemHandler`.** Docs:
   "Adding new items is not possible if the selection component is read-only or is
   bound to a Container that does not allow adding new items." The only example
   given builds the object in the handler, adds it with the container's typed API
   and selects it:

   ```java
   select.setNewItemHandler(new NewItemHandler() {
       @Override
       public void addNewItem(String newItemCaption) {
           Planet newPlanet = new Planet(0, newItemCaption);
           container.addBean(newPlanet);
           select.select(newPlanet);
       }
   });
   ```

   Vaadin 8 turned this into the API itself: `NewItemProvider<T>` returns the
   created object and the ComboBox selects it (`ComboBox.java:136-147`, 8.14.3).

3. **No framework code catches `UnsupportedOperationException` from `addItem`.**
   Greps over `AbstractSelect`, `ComboBox`, `Table`, the `data.util` containers and
   both JPAContainer releases show the exception only being thrown or declared,
   never caught around an add. The one place SQLContainer catches it is when
   probing `addContainerProperty` on a delegate (`SQLContainer.java:1047-1055`),
   which is unrelated. The `Container` interface offers no capability query for
   the optional add operations.

4. **Generated-id containers hand the id back from their own add call.**
   JPAContainer: `Object itemId = countries.addEntity(france);` (docs, "Creating and
   Accessing Entities"). SQLContainer: `addItem()` returns the final `RowId` only in
   auto-commit mode; in buffered mode it returns a `TemporaryRowId` and the real key
   arrives via `RowIdChangeListener` after `commit()` (`SQLContainer.java:144-149`,
   `TableQuery.java:289-330`). No selection component consumes either mechanism.

5. **Required properties are set before the entity is added.** JPAContainer's own
   editors wrap the fresh instance in a `BeanItem`, set the back-reference, then
   call `addEntity` (`MasterDetailEditor.java:184-198`). `OneToOneForm` goes
   further: when the form is unbuffered it switches the container to
   `setWriteThrough(false)` before `addEntity` precisely so the blank entity is
   *not* inserted yet (`OneToOneForm.java:102-111`).

6. **JPAContainer write-through depends on the provider, not only on a flag.**
   `isWriteThrough()` is `!(provider instanceof BatchableEntityProvider) || writeThrough`
   with `writeThrough = false` as the field default (`JPAContainer.java:146`,
   `1310-1313`). `JPAContainerFactory.make()` uses `CachingMutableLocalEntityProvider`,
   which is not batchable, so those containers are always write-through
   (`JPAContainerFactory.java:62-66`). `makeBatchable()` uses
   `CachingBatchableLocalEntityProvider`, so those containers start **buffered**
   (`JPAContainerFactory.java:182-186`). `setBuffered(b)` is just
   `setWriteThrough(!b)` (`JPAContainer.java:1734-1738`). The demo uses `make()`
   (`tokenfield-demo/.../jpa/JpaContacts.java:75`), so it is write-through.

## Assessment for TokenField (#39)

**The first half of #39 matches precedent exactly.** Adding under the typed token id
and writing the caption property under that same id is what
`DefaultNewItemHandler` does (`AbstractSelect.java:600-607`); the current
`rememberToken` diverges only by resolving `getTokenCaption(tokenId)` first
(`TokenField.java:303-311`).

**The fallback (catch `UnsupportedOperationException`, call `addItem()`, write the
caption property) has no precedent in Vaadin or JPAContainer.** The framework's
answer to a container that refuses explicit ids is "install a custom handler that
creates the object", and Vaadin 8 baked that into `NewItemProvider`. That said,
the fallback is consistent with the `Container` javadoc: the exception is the
documented and only signal that explicit ids are unsupported, and `addItem()` is
the documented alternative. It is an extension, not a contradiction. Two things the
primary sources make clear about it:

- **`AbstractSelect.addItem(Object)` returns null and throws for different
  reasons.** Null means "failed or already present" (`Container.java:175-177`);
  `BeanItemContainer` returns null for a `String` because it is not the bean type
  (`AbstractBeanContainer.java:453-458`). The exception means "explicit ids are not
  supported". Both need handling; only the exception justifies the `addItem()`
  fallback. Falling back on null would add a second item to an `IndexedContainer`
  that already holds the token.

- **The token id and the container id will differ**, and nothing in the framework
  reconciles them. `addNewItem` receives only the string
  (`AbstractSelect.java:577-579`), and `TokenField` has already called
  `onTokenInput(tokenId)` with the string before `rememberToken` runs
  (`TokenField.java:286-293`). The precedent for re-pointing (docs example,
  Vaadin 8 `NewItemProvider`) is the handler selecting the object it created, so
  any re-pointing in TokenField would have to be a deliberate design choice, not a
  port of existing behaviour.

**`newInstance()` and NOT NULL constraints, verified from source.**

- `JPAContainer.addItem()` creates the entity with the no-arg constructor and calls
  `addEntity` *before* returning, so the caption property can only be set
  afterwards (`JPAContainer.java:688-692`).
- In write-through mode `addEntity` calls `MutableEntityProvider.addEntity`, which
  runs `em.merge(entity); em.flush()` inside a transaction
  (`JPAContainer.java:1110-1116`; `MutableLocalEntityProvider.java:134-147`,
  `108-130`). The flush is immediate, so a `@Column(nullable = false)` column, a
  `@NotNull` validator or a non-generated `@Id` fails right there. Only
  `InstantiationException` and `IllegalAccessException` become
  `UnsupportedOperationException` (`JPAContainer.java:693-696`); the persistence
  exception propagates out of `rememberToken` untouched.
- Writing the caption property afterwards in write-through mode is a **second**
  database round trip: `JPAContainerItem.setValue` calls
  `container.containerItemPropertyModified`, which calls
  `updateEntityProperty` on the provider (`JPAContainerItem.java:287-293`;
  `JPAContainer.java:1211-1229`).
- In buffered mode (`isWriteThrough()` false, only possible with a batchable
  provider) `addEntity` stores the instance under a random `UUID` and nothing is
  flushed (`BufferedContainerDelegate.java:260-267`). Setting the caption property
  then mutates that same cached instance (`updateEntity` is a no-op for added ids,
  `BufferedContainerDelegate.java:319-330`), and the constraint is checked at
  `commit()` when `batchUpdate` replays the deltas (`:210-243`). The item id stays
  the `UUID`; `getItem(uuid)` resolves it only while the buffer is uncommitted
  (`JPAContainer.java:785-797`), so after `commit()` the token's container id is
  dead unless the application re-resolves it.
- SQLContainer behaves the same way with reversed defaults: buffered by default,
  and in auto-commit mode a failed insert returns **null** from `addItem()`
  rather than throwing (`SQLContainer.java:174-201`).

**Conclusion.** The #39 approach is defensible as a best-effort extension for
containers whose remaining columns are nullable or defaulted, or that are in
buffered mode, and it should be documented as such. For a `JPAContainerFactory.make()`
container with any other NOT NULL column it will throw from `rememberToken`
exactly as the issue predicts, and the primary-source pattern for that case is
the one the demo already uses: `setRememberNewTokens(false)` plus an
application-owned creation path (dialog or an override of `rememberToken` that
builds the entity, sets its required fields, calls `addEntity`, and uses the
returned id). If the fallback is kept, the javadoc should state the two limits
that follow directly from source: the token id is not re-pointed at the generated
id, and in write-through mode the blank entity is flushed before the caption is
written.

## Sources

Primary, local source jars (paths inside the jar; line numbers from the extracted file):

- `~/.m2/repository/com/vaadin/vaadin-server/7.7.17/vaadin-server-7.7.17-sources.jar`
  - `com/vaadin/data/Container.java` lines 170-211 (`addItem(Object)`, `addItem()` javadocs)
  - `com/vaadin/ui/AbstractSelect.java` lines 455-463 (`changeVariables`), 561-579 (`setNewItemHandler`, `getNewItemHandler`, `NewItemHandler`), 591-616 (`DefaultNewItemHandler`), 881-914 (`addItem()`, `addItem(Object)`), 1148-1156 (`isNewItemsAllowed` javadoc), 2253-2259 (`readItem`)
  - `com/vaadin/ui/ComboBox.java` lines 158, 757-765
  - `com/vaadin/data/util/IndexedContainer.java` lines 225-259, 378-384
  - `com/vaadin/data/util/AbstractInMemoryContainer.java` lines 405-414
  - `com/vaadin/data/util/BeanItemContainer.java` lines 220-241
  - `com/vaadin/data/util/AbstractBeanContainer.java` lines 440-458, 512-523
  - `com/vaadin/data/util/BeanContainer.java` lines 83-91
  - `com/vaadin/data/util/sqlcontainer/SQLContainer.java` lines 144-208, 895-949, 1004-1007, 1047-1055, 1386-1388
  - `com/vaadin/data/util/sqlcontainer/query/TableQuery.java` lines 251-274, 289-330
  - `com/vaadin/data/util/sqlcontainer/query/QueryDelegate.java` lines 172-198
- `~/.m2/repository/com/vaadin/addon/jpacontainer/3.2.0/jpacontainer-3.2.0-sources.jar`
  - `com/vaadin/addon/jpacontainer/JPAContainer.java` lines 146, 669-697, 699-705, 740-753, 785-815, 1106-1128, 1211-1229, 1270-1280, 1310-1313, 1331-1348, 1734-1742
  - `com/vaadin/addon/jpacontainer/BufferedContainerDelegate.java` lines 210-243, 260-267, 319-330
  - `com/vaadin/addon/jpacontainer/provider/MutableLocalEntityProvider.java` lines 108-147
  - `com/vaadin/addon/jpacontainer/JPAContainerItem.java` lines 261-299, 557-559
  - `com/vaadin/addon/jpacontainer/JPAContainerFactory.java` lines 62-66, 182-186, 226-230
  - `com/vaadin/addon/jpacontainer/fieldfactory/MasterDetailEditor.java` lines 184-198
  - `com/vaadin/addon/jpacontainer/fieldfactory/OneToOneForm.java` lines 102-111
- `~/.m2/repository/com/vaadin/addon/jpacontainer/4.0.0/jpacontainer-4.0.0-sources.jar` (diffed against 3.2.0: import package renames only; pom depends on `vaadin-compatibility-server` 8.0.0)
- `~/.m2/repository/com/vaadin/vaadin-compatibility-server/8.14.3/vaadin-compatibility-server-8.14.3-sources.jar`
  - `com/vaadin/v7/ui/AbstractSelect.java` lines 598-616 (`DefaultNewItemHandler`)
- `~/.m2/repository/com/vaadin/vaadin-server/8.14.3/vaadin-server-8.14.3-sources.jar`
  - `com/vaadin/ui/ComboBox.java` lines 130-147 (`NewItemHandler`, `NewItemProvider`), 776-800
- This repository: `tokenfield/src/main/java/org/vaadin/tokenfield/TokenField.java` lines 262, 279-296, 303-311; `tokenfield-demo/src/main/java/org/vaadin/tokenfield/jpa/JpaContacts.java` line 75; `tokenfield-demo/src/main/java/org/vaadin/tokenfield/jpa/JpaAddressBookPanel.java` line 115

Primary, official Vaadin 7 documentation (fetched 2026-09-08):

- https://vaadin.com/docs/v7/framework/components/components-selection.html, section "Allowing Adding New Items" (anchor `components.selection.newitems`)
- https://vaadin.com/docs/v7/framework/components/components-combobox.html (only cross-links to the section above)
- https://vaadin.com/docs/v7/framework/jpacontainer/jpacontainer-usage.html, section "Creating and Accessing Entities"
- https://vaadin.com/docs/v7/framework/jpacontainer/jpacontainer-entityprovider.html (batchable providers; no buffered-mode text)
- https://vaadin.com/docs/v7/framework/jpacontainer/jpacontainer-fieldfactory.html (only `setBuffered(true)` in an example)
- https://vaadin.com/docs/v7/framework/sqlcontainer/sqlcontainer-editing.html, sections "Adding items", "Fetching generated row keys", "Auto-commit mode"

Secondary:

- GitHub search of `vaadin/framework` issues for `NewItemHandler` (15 hits, e.g. #8538, #9640, #9774, PR #10606 "Implement NewItemProvider to replace NewItemHandler") found no issue about `NewItemHandler` with JPAContainer or SQLContainer. Not cited for any claim above.
- Viritin, branch `vaadin7`, `src/main/java/org/vaadin/viritin/fields/LazyComboBox.java` and `TypedSelect.java`: no new-item handling at all (`TypedSelect` stores options in a `ListContainer`). Checked as a possible precedent; none found.

Not found: no Vaadin 7 component, add-on or doc example that catches
`UnsupportedOperationException` from `addItem(Object)` and falls back to `addItem()`.
