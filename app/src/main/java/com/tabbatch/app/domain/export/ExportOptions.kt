package com.tabbatch.app.domain.export

/** User-configurable export options, shared by every exporter (see project brief section 10,
 * "Export screen"). */
data class ExportOptions(
    val includeTitles: Boolean = true,
    val includeUrls: Boolean = true,
    val includeDuplicates: Boolean = true,
    val includeOriginalUrls: Boolean = false,
)

enum class ExportFormat { Pdf, Csv, Json, Text }
