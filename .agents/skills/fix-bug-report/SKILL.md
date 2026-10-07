---
name: fix-bug-report
description: Use when working on a bug report, GitHub issue, or proposed fix for TokenField - reproduce it, classify it against Vaadin 7 reference behavior, and judge the reporter's suggested fix before implementing.
---

# Bug reports and fixes

Treat an issue's diagnosis and proposed fix as a hypothesis, not a spec. Before proposing a fix:

1. **Reproduce** with a failing unit test through `TestTokenField` (or a `.feature` scenario if
   only a browser shows it). A report that cannot be reproduced is reported back as such.
2. **Classify**: genuine bug, or invalid usage of the component? Compare against how the
   standard Vaadin 7 components behave in the same situation (`ComboBox`, `AbstractSelect`,
   `AbstractField`, `CustomField`): the add-on mirrors their contracts, so their behavior is
   the reference. Read the actual framework source/bytecode of the pinned `vaadin.version`
   (sources jar in `~/.m2`, or `javap` against `vaadin-server`), and the Vaadin 7 docs via the
   `vaadin` MCP server; the Javadoc of `TokenField` records where it deliberately deviates.
3. **Judge the proposed solution** against Vaadin practice: does the framework already offer a
   hook or pattern for this (`markAsDirty`, `setInternalValue`, RPC/state, `ItemCaptionMode`
   handling, ...)? Prefer the framework's way over an add-on-local workaround, and say so when
   the issue's suggestion differs from what you implement.

The fix is done when the reproduction test passes, the reference-behavior comparison is
written down (test name or a 1–3 line comment), and any deviation from the reporter's
suggestion is explained in the PR/commit.
