# Export Formats

TabBatch supports four export formats, each configurable via `ExportOptions`
(`includeTitles`, `includeUrls`, `includeDuplicates`, `includeOriginalUrls`). Every exporter
independently applies these options and independently filters out duplicate records when
`includeDuplicates == false`, using `DuplicateSummary`/`DuplicateDetector`'s definition of an
exact duplicate (identical normalized URL — see `domain/dedup/DuplicateDetector.kt`).

## CSV — `domain/export/CsvExporter.kt`

Stable, documented header:

```text
order,id,title,url,original_url,host,registrable_domain,source,created_at
```

- One row per record, `\r\n` line endings.
- Fields are CSV-escaped (RFC-4180-style: quoted if they contain a comma, quote, or newline;
  embedded quotes doubled).
- `title` / `url` / `original_url` cells are emitted empty (not omitted) when their corresponding
  `ExportOptions` flag is off, so column alignment stays stable regardless of options.
- `source` is the `TabSource` enum name (e.g. `Clipboard`, `SharedText`, `TextFile`, `Csv`,
  `Json`).
- `created_at` is an epoch-millisecond timestamp.

## JSON — `domain/export/JsonExporter.kt` / `TabBatchJsonSchema.kt`

TabBatch's own versioned schema, also used as an import format (`JsonImportParser`). Current
version: **1** (`TabBatchJsonSchema.CURRENT_VERSION`). See ADR-0003 in
`ARCHITECTURE_DECISIONS.md` for the versioning policy.

```json
{
  "schemaVersion": 1,
  "collection": {
    "name": "Research",
    "createdAt": 1735000000000,
    "exportedAt": 1735003600000,
    "totalCount": 60,
    "uniqueCount": 57,
    "duplicateCount": 3
  },
  "tabs": [
    {
      "order": 0,
      "id": "b3f1...",
      "title": "Example Domain",
      "url": "https://example.com/",
      "originalUrl": "https://example.com/ ",
      "host": "example.com",
      "registrableDomain": "example.com",
      "source": "SharedText",
      "createdAt": 1735000000000,
      "isDuplicate": false
    }
  ]
}
```

Notes:

- `collection.exportedAt`, `totalCount`, `uniqueCount`, `duplicateCount` are informational
  metadata about the export, computed from the **full** (pre-filter) record set — they describe
  the collection, not just what's included in `tabs` when duplicates are excluded.
- `tabs[].isDuplicate` reflects whether that record is a duplicate in the full collection, even
  when `includeDuplicates == true` keeps it in the output.
- `title` / `originalUrl` are `null` when not known, or when their `ExportOptions` flag is off.
- `url` is emitted as an empty string (not omitted) when `includeUrls == false`, matching CSV's
  behavior of preserving field presence.
- Pretty-printed (`Json { prettyPrint = true }`) for human readability, since this is meant to be
  independently useful, not just a machine-only blob.

## Plain text — `domain/export/TextExporter.kt`

Human-readable, grouped by domain:

```text
Research — 57 tabs

YouTube
1. Video title
   https://youtube.com/watch?v=...

GitHub
1. Repository name
   https://github.com/user/repo
```

- Header line: `<collection name> — <N> tabs` (N = count after duplicate filtering).
- One blank-line-separated section per domain group, in `DomainGrouper`/`DomainGroup` order.
- Within a group: `N. <title>` followed by an indented URL line, or just the URL line if no
  title is known or `includeTitles` is off.
- If `includeOriginalUrls` is on and the original differs from the normalized URL, an additional
  `   (original: ...)` line follows.

## PDF — `data/export/PdfExporter.kt`

Generated locally using the platform's built-in `android.graphics.pdf.PdfDocument` API (no
third-party PDF dependency), on `Dispatchers.Default` so PDF rendering never blocks the UI
thread. A4-sized pages (595×842pt @ 72dpi), with:

- Header: "TabBatch export", collection name, export timestamp, record/group/duplicate counts.
- One section per domain group, bold group heading with count.
- Per record: numbered title (word-wrapped to the content width) and an indented, wrapped URL
  line; a `(duplicate)` marker line for records flagged by `DuplicateDetector`.
- Automatic pagination — a new page starts whenever the next block wouldn't fit, and every page
  gets a centered `Page N` footer.
- Long, unbroken tokens (e.g. a URL with no natural break point) are hard-wrapped by character
  width via `Paint.measureText`, so nothing gets silently clipped off the page edge.
- An empty collection renders a single page with "No records to export." rather than a
  zero-page or malformed document.

## CSV import column detection — `domain/parser/CsvImportParser.kt`

For completeness: on **import**, the URL column is detected (case-insensitively) from header
aliases `url`, `link`, `href`, `address`, `uri`; an optional title column from `title`, `name`,
`label`, `page title`. This is intentionally more permissive than the fixed export header above,
since imported CSVs may come from arbitrary sources (spreadsheets, other tools).
