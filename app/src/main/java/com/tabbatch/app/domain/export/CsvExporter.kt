package com.tabbatch.app.domain.export

import com.tabbatch.app.domain.model.DuplicateSummary
import com.tabbatch.app.domain.model.TabRecord

/** Generates CSV text with the stable schema documented in docs/EXPORT_FORMATS.md:
 * `order,id,title,url,original_url,host,registrable_domain,source,created_at`. */
object CsvExporter {

    private val HEADER = listOf(
        "order", "id", "title", "url", "original_url", "host", "registrable_domain", "source", "created_at",
    )

    fun export(records: List<TabRecord>, options: ExportOptions): String {
        val duplicateIds = DuplicateSummary.of(records).duplicateRecordIds
        val filtered = if (options.includeDuplicates) records else records.filterNot { it.id in duplicateIds }

        val sb = StringBuilder()
        sb.append(HEADER.joinToString(",")).append("\r\n")
        for (r in filtered) {
            val cells = listOf(
                r.order.toString(),
                r.id,
                if (options.includeTitles) r.title.orEmpty() else "",
                if (options.includeUrls) r.url else "",
                if (options.includeOriginalUrls) r.originalUrl else "",
                r.host,
                r.registrableDomain.orEmpty(),
                r.source.name,
                r.createdAt.toString(),
            )
            sb.append(cells.joinToString(",") { csvEscape(it) }).append("\r\n")
        }
        return sb.toString()
    }

    /** Leading characters that spreadsheet apps (Excel, Google Sheets, LibreOffice) interpret
     * as the start of a formula. A field starting with one of these is prefixed with a single
     * apostrophe to defuse CSV formula injection, per OWASP's recommended mitigation. */
    private val FORMULA_TRIGGER_CHARS = charArrayOf('=', '+', '-', '@')

    private fun csvEscape(value: String): String {
        val sanitized = if (value.isNotEmpty() && value[0] in FORMULA_TRIGGER_CHARS) "'$value" else value
        val needsQuoting =
            sanitized.contains(',') || sanitized.contains('"') || sanitized.contains('\n') || sanitized.contains('\r')
        val escaped = sanitized.replace("\"", "\"\"")
        return if (needsQuoting) "\"$escaped\"" else escaped
    }
}
