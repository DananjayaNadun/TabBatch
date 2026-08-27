# Contributing to TabBatch

Thanks for your interest in TabBatch. This is an open-source, portfolio-quality Android project;
contributions are welcome as long as they keep the same engineering bar.

## Ground rules

- **Honesty about platform capability.** Do not add code, UI copy, or docs that imply TabBatch
  can read Chrome for Android's live tab list, or that any private/undocumented API is used to
  do so. See `docs/PLATFORM_LIMITATIONS.md` and `docs/ARCHITECTURE_DECISIONS.md` (ADR-0001)
  before touching anything under `platform/browser/`.
- **Keep the domain layer pure.** Code under `domain/` must not import `android.*` classes and
  must be unit-testable on the plain JVM.
- **No new required network access.** TabBatch works fully offline; don't add a dependency that
  changes that without discussion first.
- **No analytics/ads/telemetry SDKs.**

## Getting started

1. Fork and clone the repository.
2. Open in Android Studio (or use the command line — see `docs/DEVELOPMENT.md`).
3. Requires JDK 17 and Android SDK compileSdk 35 / minSdk 26.
4. Run `gradlew.bat test` (Windows) or `./gradlew test` (macOS/Linux) before making changes, to
   confirm your environment is set up correctly.

## Code style

- Kotlin, following `kotlin.code.style=official` (already set in `gradle.properties`).
- Prefer immutable data classes and sealed types for domain modeling.
- Public classes/functions in `domain/` should have KDoc explaining intent, not just signature.
- Compose UI: keep composables small and stateless where practical; hoist state to
  `AppViewModel`.

## Tests

- Any change to `domain/` (normalizer, grouping, dedup, parsers, exporters) must come with unit
  test coverage for the new/changed behavior, including edge cases (empty input, malformed
  input, Unicode, very large collections where relevant).
- Run the full unit test suite before opening a PR: `gradlew.bat test`.
- UI changes that affect a full user flow (import → group → export) should ideally include or
  update an instrumentation test, but this is not a hard blocker for small UI tweaks.

## Pull request process

1. Open an issue first for anything non-trivial, so the approach can be discussed.
2. Keep PRs focused — one logical change per PR.
3. Include a clear description of what changed and why, and note any user-facing behavior
   change.
4. Make sure CI (`.github/workflows/android.yml`) passes: unit tests + debug APK build.
5. Be responsive to review feedback; small, incremental commits are easier to review than a
   single large rewrite.

## Reporting bugs / requesting features

Use the issue templates under `.github/ISSUE_TEMPLATE/`.

## Security issues

Do not open a public issue for a security vulnerability — see `SECURITY.md`.
