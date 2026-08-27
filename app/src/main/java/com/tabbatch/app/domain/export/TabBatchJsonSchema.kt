package com.tabbatch.app.domain.export

import kotlinx.serialization.Serializable

/**
 * TabBatch's own versioned JSON export/import schema. See docs/EXPORT_FORMATS.md for the
 * human-readable spec. [CURRENT_VERSION] must be bumped whenever a breaking change is made to
 * the shape below; [JsonImportParser] rejects any other version rather than guessing.
 */
object TabBatchJsonSchema {
    const val CURRENT_VERSION = 1

    @Serializable
    data class Document(
        val schemaVersion: Int,
        val collection: CollectionMeta,
        val tabs: List<TabEntry>,
    )

    @Serializable
    data class CollectionMeta(
        val name: String,
        val createdAt: Long,
        val exportedAt: Long? = null,
        val totalCount: Int? = null,
        val uniqueCount: Int? = null,
        val duplicateCount: Int? = null,
    )

    @Serializable
    data class TabEntry(
        val order: Int,
        val id: String,
        val title: String? = null,
        val url: String,
        val originalUrl: String? = null,
        val host: String,
        val registrableDomain: String? = null,
        val source: String,
        val createdAt: Long,
        val isDuplicate: Boolean = false,
    )
}
