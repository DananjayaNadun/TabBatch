package com.tabbatch.app.domain.export

import com.tabbatch.app.domain.model.DomainGroup
import com.tabbatch.app.domain.model.DuplicateSummary
import com.tabbatch.app.domain.model.TabRecord

/** Generates a human-readable, grouped plain-text export (see project brief section 13). */
object TextExporter {

    fun export(collectionName: String, records: List<TabRecord>, options: ExportOptions): String {
        val duplicateIds = DuplicateSummary.of(records).duplicateRecordIds
        val filtered = if (options.includeDuplicates) records else records.filterNot { it.id in duplicateIds }
        val groups = DomainGroup.groupRecords(filtered)

        val sb = StringBuilder()
        sb.append(collectionName).append(" — ").append(filtered.size).append(" tabs\n\n")

        for (group in groups) {
            sb.append(group.displayName).append('\n')
            group.records.forEachIndexed { index, r ->
                val n = index + 1
                if (options.includeTitles && !r.title.isNullOrBlank()) {
                    sb.append(n).append(". ").append(r.title).append('\n')
                    if (options.includeUrls) sb.append("   ").append(r.url).append('\n')
                } else if (options.includeUrls) {
                    sb.append(n).append(". ").append(r.url).append('\n')
                }
                if (options.includeOriginalUrls && r.originalUrl != r.url) {
                    sb.append("   (original: ").append(r.originalUrl).append(")\n")
                }
            }
            sb.append('\n')
        }
        return sb.toString().trimEnd('\n') + "\n"
    }
}
