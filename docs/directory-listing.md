# Vaadin Directory listing copy

Ready-to-paste content for creating the **new** Directory entry (this fork does not own the
original "TokenField" listing and cannot edit it). Keep this file in sync with whatever is
actually entered on vaadin.com/directory — the listing name in particular is immutable once
uploaded.

## Name

`TokenField Reloaded`

(Must exactly match `Implementation-Title` in `tokenfield/pom.xml`'s `addon.name` property —
immutable after the first upload.)

## Summary (one-liner)

A Vaadin component for selecting multiple "tokens" via a ComboBox-style input — for tagging,
email address selection, and similar multi-select use cases.

## Description

TokenField Reloaded is an updated fork of the original, abandoned TokenField add-on for
**Vaadin 7**. It provides a field for selecting multiple "tokens" (e.g., tags, email addresses)
using a ComboBox-style input with suggestions from a backing `Container`.

This fork is not affiliated with the original TokenField Directory listing or its author; see the
project's `NOTICE` file for attribution. It exists to keep the add-on usable with a proper test
suite (JUnit 5 + Playwright) and a path toward a Vaadin 8 port.

Features:

- Tokens can be inserted before/after the input field (layout-dependent)
- Configurable layout
- Suggestions sourced from a `Container`
- Optional auto-add of new tokens to the container
- Optional restriction to only allow tokens already in the container
- Customizable add/remove behavior and token button rendering
- Built-in styles for both a "fake textfield" look and a plain ComboBox look

## Categories

UI Components, Data Binding, Forms

## Maturity level

Beta — functionally complete, covered by an automated test suite, but not yet exercised in
production by this fork's maintainers.

## Compatibility

Vaadin 7.7+. (Do **not** carry over the original listing's "Vaadin 6.0+" claim — this build has
not been tested against Vaadin 6.)

## Links

- Source repository: https://github.com/vaadin-tokenfield/tokenfield
- Issue tracker: https://github.com/vaadin-tokenfield/tokenfield/issues
- License: Apache License 2.0

## Icon

`assets/icon.png` — placeholder monogram, 256×256, <1 KB. Replace with real branding before or
shortly after the first listing goes live.

## Release notes (per version)

Copy the relevant section from `CHANGELOG.md`.
