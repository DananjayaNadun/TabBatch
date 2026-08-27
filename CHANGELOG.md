# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project intends to adhere to [Semantic Versioning](https://semver.org/) once a first
tagged release is cut.

## [Unreleased]

### Added

- Gradle Kotlin DSL project scaffold (`app` module, version catalog, JDK 17, compileSdk/targetSdk
  35, minSdk 26).
- Domain layer (pure Kotlin, no Android dependency):
  - `TabRecord`, `TabCollection`, `TabSource`, typed `ImportError`/`BrowserIntegrationError`
    models.
  - `UrlNormalizer` — conservative URL validation/normalization, including IDN/punycode host
    handling.
  - `DomainGrouper` — dependency-free registrable-domain ("eTLD+1") heuristic for grouping tabs
    by website.
  - `DuplicateDetector` / `DuplicateSummary` — exact-duplicate detection without silent deletion.
  - Import parsers for multiline text, CSV, and TabBatch's own JSON schema.
  - Exporters for CSV, JSON (versioned schema, `TabBatchJsonSchema`), and plain text.
- Platform adapters:
  - `BrowserTabSource` abstraction, with the only shipped implementation
    (`UnsupportedChromeAndroidTabSource`) honestly reporting itself as unavailable — there is no
    live Chrome Android tab integration (see `docs/ARCHITECTURE_DECISIONS.md`, ADR-0001).
  - `ShareIntentParser` for receiving `ACTION_SEND`/`ACTION_SEND_MULTIPLE` shared text.
  - `ClipboardHelper` for user-initiated paste-from-clipboard import.
  - `SafFileIO` for Storage Access Framework-based file import/export.
  - `FileShareHelper` for sharing exported files via the Android share sheet.
- Local PDF exporter (`data/export/PdfExporter.kt`) and an in-memory/local
  `CollectionRepository`.
- Jetpack Compose UI: Home, Collection, Group, and Export screens, Material 3 theme, and
  navigation graph (`ui/navigation`).
- Unit test suite (68 tests) covering the domain layer: URL normalization, domain grouping,
  duplicate detection, CSV/JSON import and export, and text export.
- Project documentation: `README.md`, `CONTRIBUTING.md`, `SECURITY.md`, `CODE_OF_CONDUCT.md`,
  `docs/ARCHITECTURE.md`, `docs/ARCHITECTURE_DECISIONS.md`, `docs/EXPORT_FORMATS.md`,
  `docs/PLATFORM_LIMITATIONS.md`, `docs/DEVELOPMENT.md`.
- GitHub Actions CI (`android.yml`): unit tests + debug APK build on push/PR. Issue and pull
  request templates.

### Fixed

- `UrlNormalizer` now correctly extracts the host from internationalized (non-ASCII) URLs before
  punycode conversion; previously `java.net.URI#getHost()` returning null for such hosts caused
  every IDN URL to be rejected as invalid.

### Known limitations

- No live Chrome for Android tab access — not possible with a public API today. See
  `docs/PLATFORM_LIMITATIONS.md`.
- PDF export, local persistence (`CollectionRepository`), and Compose UI screens do not yet have
  instrumentation/UI test coverage — only the domain layer is unit tested so far.
- No 1,000-record stress test has been run yet against the PDF exporter specifically.
