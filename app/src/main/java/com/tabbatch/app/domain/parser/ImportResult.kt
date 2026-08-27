package com.tabbatch.app.domain.parser

import com.tabbatch.app.domain.model.RejectedInput
import com.tabbatch.app.domain.model.TabRecord

/** Outcome of running any of the import parsers. Never throws for malformed *content* —
 * malformed lines/rows are collected into [rejected] instead of aborting the whole import. */
data class ImportResult(
    val accepted: List<TabRecord>,
    val rejected: List<RejectedInput>,
) {
    val isEmpty: Boolean get() = accepted.isEmpty()

    companion object {
        fun empty() = ImportResult(emptyList(), emptyList())
    }
}
