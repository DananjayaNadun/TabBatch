# Platform Limitations

This document distinguishes, explicitly, what TabBatch's Android app can do, what standard
Chrome for Android does not currently allow any third-party app to do, and what remains
experimental/unimplemented. See also ADR-0001 in `ARCHITECTURE_DECISIONS.md`.

## No live Chrome tab access

**TabBatch cannot read, enumerate, group, or control the tabs currently open in Chrome for
Android.** This is not a bug or a missing feature to be added later with more engineering effort
— it is a platform limitation:

- Chrome for Android does not support desktop-style browser extensions, so there is no
  `chrome.tabs`-equivalent extension API surface on Android at all.
- There is no separate, officially documented Android API (Intent, ContentProvider, bound
  service, etc.) for an ordinary third-party app to read another app's live in-memory tab list.
- Any implementation that appeared to do this would necessarily rely on either an undocumented
  private mechanism, root access, or invasive UI scraping — all explicitly out of scope for
  TabBatch's stable product (see the project brief's hard constraint).

`platform/browser/BrowserTabSource` exists as a typed abstraction for *if* this ever changes.
Today its only implementation, `UnsupportedChromeAndroidTabSource`, always reports itself
unavailable and returns a typed `BrowserIntegrationError.UnsupportedBrowserIntegration` failure
— never a fake success, never a silent no-op button.

**What to use instead**, all of which are genuinely supported by Android today:

1. **Share intent** — select tabs in Chrome, tap Share, choose TabBatch. TabBatch receives the
   shared text/URL(s) via `ACTION_SEND` / `ACTION_SEND_MULTIPLE`
   (`platform/sharing/ShareIntentParser`).
2. **Manual paste** — copy a multiline list of URLs and paste it into TabBatch's import screen,
   or use the explicit "paste from clipboard" action (`platform/sharing/ClipboardHelper`), which
   only reads the clipboard when the user taps that action — never in the background, and never
   retained as history.
3. **File import** — import a `.txt` (one URL per line), `.csv` (URL + optional title column),
   or a `.json` file previously exported by TabBatch, via the Storage Access Framework
   (`platform/filepicker/SafFileIO`).

## Storage Access Framework (SAF) quirks

- SAF grants access to a specific document/tree the user picked, via a `content://` URI — not an
  arbitrary filesystem path. TabBatch never assumes it can construct or guess a file path; every
  read/write goes through the URI the picker returned.
- Persisted access to a previously chosen location is not guaranteed across app reinstalls or
  some Android version upgrades; if a previously used URI becomes inaccessible, TabBatch must
  surface a clear "location no longer available, please re-select" error rather than crashing or
  writing to an unexpected location.
- Exported files are written through SAF or handed to the share sheet — TabBatch does not write
  into app-private storage and then claim that as a completed "export," since the user couldn't
  actually retrieve such a file from outside the app.

## PDF generation constraints

- Built on `android.graphics.pdf.PdfDocument`, a raster/vector drawing API, not a text-layout
  engine — TabBatch implements its own greedy word-wrap (`PdfExporter.wrapText`) using
  `Paint.measureText`, rather than relying on a full typesetting library. This is sufficient for
  single-style paragraphs (titles, URLs) but does not support rich text, mixed-direction
  (bidi) text shaping, or complex script layout beyond what Android's `Canvas.drawText` handles
  natively.
- Runs on `Dispatchers.Default`, off the caller's thread, so it doesn't block UI — but very large
  collections (e.g. 1,000+ records with long titles) will still take a perceptible amount of
  wall-clock time; the export screen must show a loading state, not a frozen button.
- No PDF bookmarks/table-of-contents yet (tracked as a Phase 2 idea in the project description,
  not MVP-blocking).

## Experimental accessibility automation — explicitly not built

The project brief allows for a research spike into whether Android's `AccessibilityService`
could assist with a narrowly scoped browser-automation workflow (e.g. reading on-screen tab
titles from Chrome's tab switcher UI). This is **not** a real tab API — it would be UI
automation/scraping, layered on top of screen-reading accessibility permissions.

**Decision: not implemented**, and not planned as anything other than clearly-labeled future
research, for these reasons:

- It requires the user to grant a broad, sensitive accessibility permission for one narrow
  feature — a poor privacy/permission tradeoff for what it would deliver.
- It would be fragile: any Chrome UI or version change could silently break it, with no
  API contract to rely on.
- It carries real Google Play policy risk around accessibility API usage for purposes other than
  actual accessibility.
- It cannot reliably approximate "read every open tab" without effectively scraping the
  tab-switcher UI, which does not reliably expose all tabs' full URLs (titles and thumbnails are
  not the same as URLs).

If this is ever prototyped, it must live in an entirely separate module, require explicit
opt-in, clearly disclose what accessibility access means, never harvest unrelated screen
content, avoid retaining screenshots/screen text beyond what's strictly needed, handle
device/Chrome-version incompatibility gracefully, and never claim Google officially supports it.
None of that exists yet in this repository.

## Summary table

| Capability | Status |
|---|---|
| Import via Android share sheet (`ACTION_SEND`) | Supported |
| Import via manual paste | Supported |
| Import via `.txt` / `.csv` / TabBatch `.json` file | Supported |
| Export to PDF / CSV / JSON / text | Supported |
| Share exported file via Android share sheet | Supported |
| Live read of Chrome for Android's open tabs | **Not supported — no public API exists** |
| Accessibility-service tab automation | **Not implemented — future research only** |
