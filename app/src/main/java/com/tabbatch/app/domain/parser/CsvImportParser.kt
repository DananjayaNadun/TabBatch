package com.tabbatch.app.domain.parser

import com.tabbatch.app.domain.model.ImportError
import com.tabbatch.app.domain.model.RejectedInput
import com.tabbatch.app.domain.model.TabSource

/**
 * Minimal, dependency-free RFC-4180-ish CSV parser tailored to TabBatch's needs: detect a URL
 * column by header name, optionally a title column, and tolerate quoted fields containing commas.
 *
 * A full CSV grammar (embedded newlines inside quoted fields, custom delimiters, BOM handling)
 * is intentionally out of scope for an MVP import tool; this covers the realistic case of a
 * browser-exported or spreadsheet-exported "one row per tab" CSV.
 */
object CsvImportParser {

    private val URL_HEADER_ALIASES = setOf("url", "link", "href", "address", "uri")
    private val TITLE_HEADER_ALIASES = setOf("title", "name", "label", "page title")

    fun parse(csvText: String, source: TabSource = TabSource.Csv): ImportResult {
        val rawLines = csvText.lines().filter { it.isNotBlank() }
        if (rawLines.isEmpty()) {
            return ImportResult(emptyList(), listOf(RejectedInput("", ImportError.EmptyImport)))
        }

        val headerCells = splitCsvLine(rawLines.first()).map { it.trim().lowercase() }
        val urlColumnIndex = headerCells.indexOfFirst { it in URL_HEADER_ALIASES }
        if (urlColumnIndex < 0) {
            return ImportResult(emptyList(), listOf(RejectedInput(rawLines.first(), ImportError.MalformedCsv)))
        }
        val titleColumnIndex = headerCells.indexOfFirst { it in TITLE_HEADER_ALIASES }

        val factory = RecordFactory(source)
        val outcomes = mutableListOf<Result<com.tabbatch.app.domain.model.TabRecord>>()
        for (line in rawLines.drop(1)) {
            val cells = splitCsvLine(line)
            val rawUrl = cells.getOrNull(urlColumnIndex)?.trim().orEmpty()
            if (rawUrl.isEmpty()) continue
            val title = if (titleColumnIndex >= 0) cells.getOrNull(titleColumnIndex)?.trim() else null
            outcomes.add(factory.tryCreate(rawUrl, title))
        }

        if (outcomes.isEmpty()) {
            return ImportResult(emptyList(), listOf(RejectedInput(csvText, ImportError.EmptyImport)))
        }
        return factory.buildResult(outcomes)
    }

    /** Splits one CSV line into cells, honoring double-quoted fields (with "" as an escaped quote)
     * containing commas. Does not support quoted fields spanning multiple lines. */
    private fun splitCsvLine(line: String): List<String> {
        val cells = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                inQuotes && c == '"' && i + 1 < line.length && line[i + 1] == '"' -> {
                    current.append('"')
                    i++
                }
                c == '"' -> inQuotes = !inQuotes
                c == ',' && !inQuotes -> {
                    cells.add(current.toString())
                    current.clear()
                }
                else -> current.append(c)
            }
            i++
        }
        cells.add(current.toString())
        return cells
    }
}
