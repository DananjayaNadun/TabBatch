package com.tabbatch.app.domain.parser

import com.tabbatch.app.domain.export.TabBatchJsonSchema
import com.tabbatch.app.domain.model.ImportError
import com.tabbatch.app.domain.model.RejectedInput
import com.tabbatch.app.domain.model.TabSource
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/** Parses TabBatch's own versioned JSON export schema (see docs/EXPORT_FORMATS.md and
 * [TabBatchJsonSchema]) back into an [ImportResult]. Any JSON that doesn't match the schema, or
 * whose schemaVersion is unrecognized, is rejected as [ImportError.MalformedJson]. */
object JsonImportParser {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun parse(jsonText: String, source: TabSource = TabSource.Json): ImportResult {
        if (jsonText.isBlank()) {
            return ImportResult(emptyList(), listOf(RejectedInput("", ImportError.EmptyImport)))
        }

        val document = try {
            json.decodeFromString(TabBatchJsonSchema.Document.serializer(), jsonText)
        } catch (e: SerializationException) {
            return ImportResult(emptyList(), listOf(RejectedInput(jsonText.take(200), ImportError.MalformedJson)))
        } catch (e: IllegalArgumentException) {
            return ImportResult(emptyList(), listOf(RejectedInput(jsonText.take(200), ImportError.MalformedJson)))
        }

        if (document.schemaVersion != TabBatchJsonSchema.CURRENT_VERSION) {
            return ImportResult(emptyList(), listOf(RejectedInput(jsonText.take(200), ImportError.MalformedJson)))
        }

        if (document.tabs.isEmpty()) {
            return ImportResult(emptyList(), listOf(RejectedInput("", ImportError.EmptyImport)))
        }

        val factory = RecordFactory(source)
        val outcomes = document.tabs.map { factory.tryCreate(it.url, it.title) }
        return factory.buildResult(outcomes)
    }
}
