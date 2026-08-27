package com.tabbatch.app.domain.export

import com.tabbatch.app.domain.model.DuplicateSummary
import com.tabbatch.app.domain.model.TabRecord
import kotlinx.serialization.json.Json

/** Generates TabBatch's versioned JSON export ([TabBatchJsonSchema]). */
object JsonExporter {

    private val json = Json { prettyPrint = true; encodeDefaults = true }

    fun export(
        collectionName: String,
        collectionCreatedAt: Long,
        records: List<TabRecord>,
        options: ExportOptions,
        exportedAt: Long,
    ): String {
        val duplicateIds = DuplicateSummary.of(records).duplicateRecordIds
        val filtered = if (options.includeDuplicates) records else records.filterNot { it.id in duplicateIds }
        val summary = DuplicateSummary.of(records)

        val document = TabBatchJsonSchema.Document(
            schemaVersion = TabBatchJsonSchema.CURRENT_VERSION,
            collection = TabBatchJsonSchema.CollectionMeta(
                name = collectionName,
                createdAt = collectionCreatedAt,
                exportedAt = exportedAt,
                totalCount = summary.totalCount,
                uniqueCount = summary.uniqueCount,
                duplicateCount = summary.duplicateCount,
            ),
            tabs = filtered.map { r ->
                TabBatchJsonSchema.TabEntry(
                    order = r.order,
                    id = r.id,
                    title = if (options.includeTitles) r.title else null,
                    url = if (options.includeUrls) r.url else "",
                    originalUrl = if (options.includeOriginalUrls) r.originalUrl else null,
                    host = r.host,
                    registrableDomain = r.registrableDomain,
                    source = r.source.name,
                    createdAt = r.createdAt,
                    isDuplicate = r.id in duplicateIds,
                )
            },
        )
        return json.encodeToString(TabBatchJsonSchema.Document.serializer(), document)
    }
}
