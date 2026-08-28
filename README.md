# TabBatch

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Android CI](https://github.com/DananjayaNadun/TabBatch/actions/workflows/android.yml/badge.svg)](.github/workflows/android.yml)

**Organize large collections of browser tab URLs and export them to PDF, CSV, JSON, or text — locally, on Android.**

## Problem

Opening dozens of pages from the same site on a phone is easy. Doing anything useful with
that collection afterward is not: there's no way to group those tabs by site, spot duplicates,
or get all the links out without switching between tabs and copy-pasting one at a time. A
phone clipboard is not a database for sixty URLs.

TabBatch treats a tab collection as structured data: import it, group it by domain, flag exact
duplicates without deleting them, search/select what you need, and export it to a portable file
you can open with anything.

## Screenshots

Captured on an Android emulator (API 34, 1080x2400) from a 60-URL synthetic demo collection
(5 domains, 5 exact duplicates):

| Home | Collection overview |
|---|---|
| ![Home screen](docs/screenshots/home.png) | ![Collection overview](docs/screenshots/collection.png) |

| Group detail | Export |
|---|---|
| ![Group detail](docs/screenshots/group.png) | ![Export screen](docs/screenshots/export.png) |

## Download

The first release (v0.1.0) will be published shortly on the
[Releases page](https://github.com/DananjayaNadun/TabBatch/releases/latest). Until then, build
from source (see below) — the app is not yet published to Google Play.

## Features

- **Import** — paste a multiline list of URLs, receive a share (`ACTION_SEND`) from another app,
  or import a `.txt`, `.csv`, or TabBatch-format `.json` file.
- **Normalize** — validates and cleans each URL (trims whitespace, strips accidental wrapping
  quotes, lower-cases scheme/host, converts internationalized hostnames to punycode) without
  rewriting query strings or fragments.
- **Group by domain** — deterministic registrable-domain grouping (e.g. `docs.example.com` and
  `example.com` both group under `example.com`).
- **Detect duplicates** — exact-URL duplicates are flagged, never silently removed; you choose
  whether an export keeps or drops them.
- **Search & select** — filter by title/URL/domain, select whole groups or individual records.
- **Export** — PDF, CSV, JSON (versioned schema), and plain text, all generated on-device.
- **Share** — hand the exported file to any app via the standard Android share sheet.

## A note on Chrome tab access

TabBatch does not read, list, or control the tabs currently open in Chrome for Android — Android
gives ordinary third-party apps no public API to do that, so instead of faking it, TabBatch is
built around three input paths Android genuinely supports: share intent, manual paste, and file
import. This is a deliberate platform-aware design choice, not a missing feature — see
[`docs/PLATFORM_LIMITATIONS.md`](docs/PLATFORM_LIMITATIONS.md) and
[`docs/ARCHITECTURE_DECISIONS.md`](docs/ARCHITECTURE_DECISIONS.md) (ADR-0001) for the full
rationale.

## Privacy

- No account, no backend, no analytics, no telemetry, no ads.
- No network calls required for core functionality — the app does not even request the
  `INTERNET` permission (see `app/src/main/AndroidManifest.xml`).
- URLs and titles are processed and stored on-device only.
- Exports only leave the device when you explicitly share them.

## Technical highlights

- Kotlin, Jetpack Compose, Material 3.
- compileSdk/targetSdk 35, minSdk 26 (Android 8.0+).
- Gradle Kotlin DSL, version catalogs (AGP 8.5.2, Kotlin 2.0.21, Compose BOM 2024.10.00).
- Domain layer (`domain/model`, `domain/normalizer`, `domain/grouping`, `domain/dedup`,
  `domain/parser`, `domain/export`) has zero Android framework dependency and is unit-tested on
  the plain JVM.
- GitHub Actions CI (`android.yml`) runs unit tests, lint, and a debug build on every push/PR.

## Testing

- 81 JUnit unit tests over the domain layer, run on the plain JVM
  (`gradlew.bat test` / `./gradlew test`): URL normalization, registrable-domain grouping,
  duplicate detection, CSV/JSON/text import and export, known grouping-heuristic limitations, and
  a synthetic 1,000+ record pipeline stress test.
- An instrumented test (`PdfExporterInstrumentedTest`, runs on-device/emulator) covers PDF export
  pagination for large collections.
- Compose UI screens and `CollectionRepository` do not yet have instrumentation/UI test coverage
  — tracked as future work.

## Architecture

```text
UI (Compose)  ->  ViewModel / use cases  ->  Domain (pure Kotlin, JVM-testable)
                                                  |
                                     +------------+------------+
                                     |                         |
                              Data / Persistence         Platform adapters
                                                    (share, clipboard, SAF file I/O)
```

The domain layer (`domain/model`, `domain/normalizer`, `domain/grouping`, `domain/dedup`,
`domain/parser`, `domain/export`) has no dependency on Android framework classes and is unit
tested on the plain JVM. See [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for details.

## Build & run

Requires JDK 17 and the Android SDK (compileSdk/targetSdk 35, minSdk 26).

```bash
# Windows
gradlew.bat assembleDebug

# macOS / Linux
./gradlew assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

> **Windows checkout path note:** if your checkout path contains non-ASCII characters (accented
> letters, em dashes, etc.), Gradle's test workers can fail with spurious `ClassNotFoundException`
> errors even though the code compiles fine. Building from an ASCII-only path avoids it. See
> [`docs/DEVELOPMENT.md`](docs/DEVELOPMENT.md#a-note-on-non-ascii-checkout-paths-on-windows) for
> the full explanation and a registry-based alternative fix.

## Testing

```bash
gradlew.bat test        # unit tests (domain layer)
gradlew.bat check       # unit tests + lint
```

See [`docs/DEVELOPMENT.md`](docs/DEVELOPMENT.md) for local setup details.

## Roadmap

**Implemented:** everything in Features/Testing above — share import, paste, file import
(txt/csv/json), normalization, domain grouping, duplicate detection, search/select, PDF/CSV/
JSON/text export, share-sheet export, domain-layer unit tests, CI.

**Future (not MVP-blocking, not implemented yet):**

- Reading-time / URL health checks, domain statistics.
- ZIP export bundling PDF + CSV + JSON.
- Additional browser-specific import adapters (session-file formats).
- Compose UI / instrumentation test coverage beyond PDF export.

**Experimental research only — not implemented, no timeline:** an accessibility-service
automation spike was considered and explicitly rejected for the current app (privacy tradeoff,
fragility, Play policy risk). See [`docs/PLATFORM_LIMITATIONS.md`](docs/PLATFORM_LIMITATIONS.md)
for why. It is not on the roadmap as a planned feature.

## Contributing

See [`CONTRIBUTING.md`](CONTRIBUTING.md).

## License

MIT — see [`LICENSE`](LICENSE).
