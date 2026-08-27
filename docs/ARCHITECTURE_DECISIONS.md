# Architecture Decision Records

Lightweight ADRs for the decisions in this project that aren't obvious from the code alone.

---

## ADR-0001: No live Chrome-Android tab integration

**Status:** Accepted.

**Context**

The original product idea was to let a user organize the tabs currently open in their Chrome
for Android browser. Before building anything, this needed a feasibility check: does an
ordinary third-party Android app have any officially supported way to enumerate, read, or
control another app's (Chrome's) live tab collection?

**Finding**

No. As of this writing:

- Chrome for Android does not support desktop-style browser extensions, so there is no
  `chrome.tabs`-style extension API surface available on Android at all.
- There is no separate public Android API (Intent, ContentProvider, Binder service, etc.) that
  Google documents for a third-party app to read Chrome's live open-tab list.
- Reverse-engineering Chrome's private tab-state storage, or requiring root, would be an
  undocumented, fragile, policy-risky approach — explicitly disallowed for the stable product by
  the project brief.

An experimental approach using Android's `AccessibilityService` to *read Chrome's on-screen UI*
(not a real tab API — UI automation) was considered. It was evaluated as a research idea only,
and explicitly **not implemented**: it would require the user to grant a broad, sensitive
accessibility permission for a single narrow feature, would be fragile to Chrome UI/version
changes, may violate Play Store policy around accessibility API misuse, and cannot reliably scale
to "read every open tab" without effectively scraping the tab-switcher UI. Per the project brief
(section 18 / "Optional Accessibility Research"), an approach this fragile should not ship, and
it has not shipped — see `docs/PLATFORM_LIMITATIONS.md` for the disclosure this decision requires.

**Decision**

TabBatch does not implement, simulate, or fake any live Chrome-Android tab connection.

- `platform/browser/BrowserTabSource` defines the abstraction a real adapter would implement
  *if* a supported mechanism ever exists.
- The only shipped implementation, `UnsupportedChromeAndroidTabSource`, always reports
  `isAvailable() == false` and always fails `getOpenTabs()` with a typed
  `BrowserIntegrationError.UnsupportedBrowserIntegration` — never a thrown exception, never a
  fabricated empty/success result.
- The supported input paths are, and remain: Android share intent (`ACTION_SEND`), manual
  paste, and file import (`.txt` / `.csv` / TabBatch `.json`).
- The UI must render this as an honest, explained "not currently available" state, never a
  disabled-looking button with no explanation and never a button that silently does nothing.

**Consequences**

- The product's core value has to come from the organize/export pipeline (grouping, dedup,
  export), not from magic tab-collection convenience — which matches the project's actual
  motivation (see `TABBATCH_PROJECT_DESCRIPTION.md`, "Why this project exists").
- If Chrome/Android ever ships an official mechanism, a new `BrowserTabSource` implementation
  can be added without touching the domain model, import pipeline, or export pipeline — that's
  the entire reason the interface exists today with only an "unsupported" implementation behind
  it.

---

## ADR-0002: Dependency-free heuristic for registrable-domain grouping

**Status:** Accepted.

**Context**

Grouping tabs "by website" needs a registrable domain ("eTLD+1"), e.g. `docs.example.com` and
`shop.example.com` should both group under `example.com`. Getting this fully correct requires
Mozilla's Public Suffix List (PSL) — thousands of rules, including exceptions and wildcards,
that change over time.

Two realistic options:

1. A maintained PSL-backed library, e.g. Guava's `InternetDomainName`.
2. A small hand-written heuristic covering the common cases, documented and tested.

**Decision**

Ship option 2: `domain/grouping/DomainGrouper`. It special-cases IP addresses/`localhost`
(no registrable domain), checks a curated list of common two-label public suffixes (`co.uk`,
`com.au`, `github.io`, `vercel.app`, etc.), and otherwise takes the last two labels of the host.

**Tradeoff, explicitly accepted**

- Guava's `InternetDomainName` would be correct for the full PSL, but pulls in the entire Guava
  library (~3 MB) for one feature, in an app whose explicit goal is a minimal, auditable
  dependency footprint (project brief: "Do not add a dependency just because an AI coding tool
  suggested it").
- The heuristic will misclassify registrable domains for public suffixes *not* in the curated
  list (e.g. some `*.compute.amazonaws.com`-style multi-part suffixes), and does not implement
  PSL wildcard/exception rules.
- This is acceptable because TabBatch's grouping is **descriptive** (organizing a tab list for a
  human to read), not **security-sensitive** (e.g. cookie/session scoping), where an incorrect
  eTLD+1 could matter a great deal more.
- The heuristic, its known limitations, and the specific curated suffix list are documented in
  the `DomainGrouper` KDoc itself, not just here, so a future contributor sees the tradeoff at
  the point of use.

**Revisit if:** grouping accuracy becomes a recurring user complaint, or the dependency budget
changes (e.g. moving to a multi-module build where a heavier dependency is more acceptable).

---

## ADR-0003: JSON export schema versioning

**Status:** Accepted.

**Context**

TabBatch's JSON export/import format is both a user-facing "machine-readable export" and its own
import format (round-tripping a previously exported collection). The shape of that document will
need to change over time as features are added (e.g. new metadata fields).

**Decision**

Every exported JSON document carries an explicit integer `schemaVersion` field
(`TabBatchJsonSchema.CURRENT_VERSION`, currently `1`), separate from the app's own version
number. `JsonImportParser` checks this field and rejects any document whose version it does not
recognize, rather than guessing field meanings or silently dropping unknown/missing fields.

**Rationale**

- An integer version is simpler to reason about and compare than embedding the app version
  string, and decouples "the export format changed" from "the app version changed" — many app
  releases won't touch the export schema at all.
- Rejecting unknown versions outright (rather than best-effort parsing) avoids silently
  misinterpreting a future schema's fields under old assumptions, which could corrupt data the
  user re-imports.
- Fields added in a backward-compatible way (e.g. new optional metadata) can bump semantics
  without bumping `schemaVersion`, since `CollectionMeta`/`TabEntry` fields are already mostly
  optional (`= null` defaults) — reserve a version bump for changes that would break an older
  parser's assumptions about a field's meaning or required presence.

**Consequences**

- A future schema change (v2) requires: bump `CURRENT_VERSION`, update `JsonImportParser` to
  handle both v1 (for backward-compatible import of old exports, if desired) and v2, and update
  `docs/EXPORT_FORMATS.md`.
