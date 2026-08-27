# Architecture

TabBatch is a single-module Android app (`app/`) organized into four layers. The dependency
direction is one-way: UI depends on domain, platform adapters implement domain-facing
interfaces, and the domain layer depends on nothing Android-specific.

```text
ui/               Jetpack Compose screens + navigation + AppViewModel (state holder)
   |
   v
domain/           Pure Kotlin: models, normalizer, grouping, dedup, parsers, exporters
   ^
   |
data/             PdfExporter (needs android.graphics.pdf), CollectionRepository (persistence)
platform/         Android-specific adapters: sharing, clipboard, SAF file I/O, browser source
```

## Layers

### `domain/`

The core, framework-free transformation pipeline. Every class here is unit-testable on the
plain JVM (see `app/src/test/java/com/tabbatch/app/domain/`).

- `model/` — `TabRecord`, `TabCollection`, `TabSource`, `DomainGroup`, `DuplicateSummary`, and
  typed error sealed classes (`ImportError`, `BrowserIntegrationError`) in `Errors.kt`.
- `normalizer/UrlNormalizer` — validates and normalizes one raw URL string at a time. Trims
  whitespace, strips one layer of wrapping quotes, validates scheme (`http`/`https` only),
  lower-cases scheme/host, and converts internationalized hostnames to punycode for grouping.
  Never rewrites query strings or fragments.
- `grouping/DomainGrouper` — resolves a best-effort registrable domain ("eTLD+1", e.g.
  `example.com` for host `docs.example.com`) using a small dependency-free heuristic. See
  ADR-0002 in `ARCHITECTURE_DECISIONS.md` for why this isn't a full Public Suffix List
  implementation.
- `dedup/DuplicateDetector` — single-pass O(n) exact-duplicate detection over normalized URLs;
  never deletes records itself, only reports which ids are duplicates.
- `parser/` — `TextImportParser` (multiline URL list), `CsvImportParser` (header-aware, URL +
  optional title column), `JsonImportParser` (TabBatch's own schema), and `RecordFactory`, which
  turns a raw URL + optional title into a `TabRecord` (running it through `UrlNormalizer` and
  `DomainGrouper`) or a `RejectedInput`.
- `export/` — `CsvExporter`, `JsonExporter`, `TextExporter`, plus `ExportOptions` /
  `ExportFormat` shared by all exporters (including the PDF one in `data/`). See
  `docs/EXPORT_FORMATS.md`.

### `data/`

Implementations that need real Android APIs but aren't "platform adapters" in the
sharing/file-picker sense:

- `export/PdfExporter` — builds a PDF using the platform's built-in `android.graphics.pdf.PdfDocument`
  API (no third-party PDF library). Runs on `Dispatchers.Default`, off the caller's thread.
- `repository/CollectionRepository` — local storage for saved collections.

### `platform/`

Thin adapters around Android-specific I/O, kept separate so the domain layer never has to know
they exist:

- `sharing/ShareIntentParser` — extracts URLs/text from an incoming `ACTION_SEND` /
  `ACTION_SEND_MULTIPLE` intent.
- `sharing/ClipboardHelper` — user-initiated clipboard read (never polls or monitors in the
  background) and a URL-likelihood check.
- `sharing/FileShareHelper` — hands a generated export file to the Android share sheet.
- `filepicker/SafFileIO` — Storage Access Framework read/write for file import/export.
- `browser/BrowserTabSource` — the live-tab-access abstraction. The only implementation shipped,
  `UnsupportedChromeAndroidTabSource`, always reports itself unavailable. See
  `docs/PLATFORM_LIMITATIONS.md` and ADR-0001.

### `ui/`

Jetpack Compose + Material 3. `AppViewModel` holds UI state and calls into the domain/data
layers; `navigation/` wires up Home → Collection → Group → Export. Screens: `home/HomeScreen`,
`collection/CollectionScreen`, `group/GroupScreen`, `export/ExportScreen`.

## Data flow: import

```text
raw input (pasted text / shared text / .txt / .csv / .json)
   -> TextImportParser | CsvImportParser | JsonImportParser
   -> RecordFactory.tryCreate(rawUrl, title)
        -> UrlNormalizer.normalize(rawUrl)   (reject on failure -> RejectedInput)
        -> DomainGrouper.registrableDomainOf(host)
   -> ImportResult(records: List<TabRecord>, rejected: List<RejectedInput>)
   -> AppViewModel updates UI state (grouped counts, duplicate summary via DuplicateDetector)
```

## Data flow: export

```text
selected TabRecords + ExportOptions
   -> CsvExporter.export(...) | JsonExporter.export(...) | TextExporter.export(...) | PdfExporter.export(...)
   -> bytes/text written via SafFileIO to a user-chosen location
   -> FileShareHelper hands the resulting content URI to the Android share sheet (optional)
```

Every exporter independently applies `DuplicateSummary`/`DuplicateDetector` to honor
`ExportOptions.includeDuplicates`, and independently respects `includeTitles` / `includeUrls` /
`includeOriginalUrls` — there is no shared mutable export pipeline object, which keeps each
exporter simple to test in isolation.
