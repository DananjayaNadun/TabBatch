# Development

## Prerequisites

- JDK 17.
- Android SDK, with `compileSdk`/`targetSdk` 35 and `minSdk` 26 platform components installed
  (Android Studio's SDK Manager will handle this, or use `sdkmanager` directly).
- A `local.properties` file at the repo root pointing at your SDK, e.g.:

  ```properties
  sdk.dir=C:\\Users\\you\\AppData\\Local\\Android\\Sdk
  ```

  (Android Studio generates this automatically on first open; it's git-ignored.)

## Project structure

```text
app/
  src/main/java/com/tabbatch/app/
    MainActivity.kt
    domain/           pure Kotlin — models, normalizer, grouping, dedup, parsers, exporters
    data/              PDF export (needs android.graphics), local repository
    platform/          Android adapters: sharing, clipboard, SAF file I/O, browser source
    ui/                Compose screens, navigation, AppViewModel, theme
  src/test/java/com/tabbatch/app/domain/   JUnit tests for the domain layer
docs/                ARCHITECTURE.md, ARCHITECTURE_DECISIONS.md, EXPORT_FORMATS.md,
                     PLATFORM_LIMITATIONS.md, DEVELOPMENT.md (this file)
```

See `docs/ARCHITECTURE.md` for what belongs in each layer and why.

## Building

```bash
# Windows
gradlew.bat assembleDebug

# macOS / Linux
./gradlew assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`.

## Running tests

```bash
# Windows
gradlew.bat test

# macOS / Linux
./gradlew test
```

This runs the JUnit unit tests under `app/src/test/`, which cover the domain layer (URL
normalization, domain grouping, duplicate detection, CSV/JSON import and export, text export).
Test reports: `app/build/reports/tests/testDebugUnitTest/index.html`.

For the full check (tests + lint):

```bash
gradlew.bat check
```

### A note on non-ASCII checkout paths on Windows

If your checkout path contains non-ASCII characters (accented letters, em dashes, etc.), two
separate issues can show up on Windows, independent of each other:

1. **AGP path-safety warning** — Android Gradle Plugin refuses to configure by default when the
   project path has non-ASCII characters. This repo's `gradle.properties` already sets
   `android.overridePathCheck=true` to opt back in.
2. **Test worker classpath corruption** — if the system's legacy ANSI codepage (Windows'
   "language for non-Unicode programs," a system-wide setting distinct from console codepage/
   `chcp`) cannot represent a character in the path, the Gradle test-worker JVM's classpath can
   get corrupted, causing every unit test class to fail with `ClassNotFoundException` even
   though compilation succeeds. If you hit this, either check out the repo to an ASCII-only path,
   or switch Windows' system locale for non-Unicode programs to UTF-8 (Windows Settings → Time &
   Language → Language & region → Administrative language settings → Change system locale →
   "Beta: Use Unicode UTF-8...").

Neither issue reflects a defect in the app or its tests — both are verified to build and pass
cleanly from an ASCII-only path.

## Running the app

Open the project in Android Studio and run the `app` configuration on a device/emulator with API
26+, or install the built debug APK directly:

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Code style

Kotlin, `kotlin.code.style=official` (set in `gradle.properties`). See `CONTRIBUTING.md` for
more.
