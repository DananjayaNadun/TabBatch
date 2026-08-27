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

**Update (verified 2026-08-27):** on this checkout the folder name is
`TabBatch — Android Tab Organizer & URL Exporter`. Beyond the em dash, that name also contains a
literal `&`, which `cmd.exe` (what `gradlew.bat` runs under, including when invoked from
PowerShell) treats as a command separator — so `gradlew.bat` fails immediately with `'URL' is not
recognized as an internal or external command` before Gradle even starts, regardless of codepage
settings. This is a `cmd.exe` argument-parsing problem, not a Gradle or Windows-locale one, and
quoting the working directory does not fix it because `gradlew.bat` itself re-splits on `&`
internally. A second, separate issue was also observed: a `local.properties` written with a UTF-8
BOM (e.g. via some editors' "save as UTF-8" default) causes `sdk.dir` to silently fail to parse,
producing `SDK location not found` even though the file looks correct when read as text — write
`local.properties` as BOM-less UTF-8 (or plain ASCII) to avoid this.

Net effect: **on a path containing `&`, `gradlew.bat` cannot run at all**, independent of the
ASCII/codepage issue above. The only reliable workaround verified on this machine is building from
a checkout path with no `&`, no em dash, and no other `cmd.exe`-special characters — e.g. copy the
tree to `C:\Projects\TabBatch-build` and run Gradle there, then copy any source/doc changes back
into the real working tree via git. `gradlew` (the POSIX shell script, used from Git Bash) is not
affected by the `&` issue but Git Bash's `gradlew` still shells out to `java.exe` with a Windows
path, which has intermittently failed to resolve `JAVA_HOME`/`ANDROID_HOME` from a Git Bash
session on this machine — PowerShell from the ASCII-safe copy was the only path that built
reliably end-to-end here.

## Running the app

Open the project in Android Studio and run the `app` configuration on a device/emulator with API
26+, or install the built debug APK directly:

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Code style

Kotlin, `kotlin.code.style=official` (set in `gradle.properties`). See `CONTRIBUTING.md` for
more.
