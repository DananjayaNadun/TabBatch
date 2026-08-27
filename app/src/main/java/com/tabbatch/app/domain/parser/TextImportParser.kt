package com.tabbatch.app.domain.parser

import com.tabbatch.app.domain.model.ImportError
import com.tabbatch.app.domain.model.TabSource

/** Parses one URL per line of free-form pasted/shared/file text. Blank lines are ignored
 * silently (not treated as rejections); everything else is validated by [com.tabbatch.app.domain.normalizer.UrlNormalizer]. */
object TextImportParser {

    fun parse(text: String, source: TabSource): ImportResult {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) {
            return ImportResult(
                accepted = emptyList(),
                rejected = listOf(com.tabbatch.app.domain.model.RejectedInput("", ImportError.EmptyImport)),
            )
        }
        val factory = RecordFactory(source)
        val outcomes = lines.map { factory.tryCreate(it, title = null) }
        return factory.buildResult(outcomes)
    }
}
