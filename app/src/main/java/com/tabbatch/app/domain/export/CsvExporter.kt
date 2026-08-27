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

    private fun csvEscape(value: String): String {
        val needsQuoting = value.contains(',') || value.contains('"') || value.contains('\n') || value.contains('\r')
        val escaped = value.replace("\"", "\"\"")
        return if (needsQuoting) "\"$escaped\"" else escaped
    }
}
