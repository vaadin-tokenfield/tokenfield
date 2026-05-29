# TokenField Reloaded

TokenField is a [Vaadin](http://vaadin.com/) component for selecting multiple 'tokens'
using a ComboBox (which by default actually looks like a TextField w/ suggestions) –
essentially a multi-select ComboBox.
Example use cases include tagging and email address selection.

**TokenField Reloaded** is an updated fork of the original, now-abandoned
[TokenField](https://vaadin.com/directory/component/tokenfield) add-on by Marc Englund (@emarc),
migrated from Google Code to GitHub. It is not affiliated with the original Directory listing —
see [NOTICE](NOTICE.txt) for full attribution.

Goals of the fork:

- [ ] add a proper testing harness (JUnit 5 + Playwright)
- [ ] fix inherited issues
- [ ] port to Vaadin 8

Non-Goal:

- port to Vaadin 9 or later;
  for most projects it will make most sense to upgrade directly
  to the latest Vaadin version, which contains a "Multi-Select Combo Box" out of the box,
  that should be a sufficient replacement for TokenField.

Discussion thread at the [Vaadin Forum](https://vaadin.com/forum/t/tokenfield-new-component/153084)
(original add-on thread).

## Compatibility

Vaadin 7.7.x, Java 8+. (The original add-on also listed Vaadin 6.x support; this fork is not
tested against Vaadin 6 and only claims Vaadin 7.)

## Installation

```xml
<dependency>
    <groupId>org.vaadin.addons.tokenfield</groupId>
    <artifactId>tokenfield</artifactId>
    <version>7.0.2</version>
</dependency>
```

```xml
<repository>
    <id>vaadin-addons</id>
    <url>https://maven.vaadin.com/vaadin-addons</url>
</repository>
```

After adding the dependency, recompile your project's widgetset (`mvn vaadin:update-widgetset
vaadin:compile`, or let `vaadin-maven-plugin`'s `update-widgetset`/`compile` goals do it as part of
your build) so the client-side code is picked up.

> **Note:** the Maven groupId is `org.vaadin.addons.tokenfield`, not `org.vaadin.addons` as used by
> the original 7.0.1 release — this is a deliberate, breaking change since this fork is published
> under a separate Directory listing. Update your dependency coordinates when switching from the
> original add-on. Beyond that change, this fork should be a drop-in replacement.

## Usage

```java
// Most basic use case: empty to start, user-entered tokens are
// automatically added to the (in-memory) container.
TokenField f = new TokenField("Add tags");
addComponent(f);
```

See the [demo module](tokenfield-demo) for a full-featured example, including custom token
rendering, a backing `Container` ('address book'), and confirm-on-remove dialogs.

The field is very configurable, as can be seen in the [demo](http://marc.virtuallypreinstalled.com/TokenField/).

Features include:

- tokens can be inserted before/after input (over/under/etc depending on layout)
- the layout can be changed
- suggestions from container
- auto add new to container
- disallow tokens not in container
- custom action on add (+ detect if token is in container)
- custom configuring of the token button (style, caption, etc)
- custom action on remove
- built-in style for either TextField or ComboBox look
- built-in styles for buttons, default and "emphasize"

## Building from source

```shell
./mvnw clean package              # build the add-on JAR and the Directory ZIP bundle
./mvnw -pl tokenfield-demo jetty:run   # run the demo at http://localhost:8080/
```

## Credits

Originally created by Marc Englund. See [NOTICE](NOTICE.txt) for the full attribution required by the
Apache License, and the git history for the complete list of contributors.

## License

Apache License 2.0 — see [LICENSE](LICENSE.txt).
