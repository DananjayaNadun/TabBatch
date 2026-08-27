package com.tabbatch.app.domain.parser

import com.tabbatch.app.domain.grouping.DomainGrouper
import com.tabbatch.app.domain.model.RejectedInput
import com.tabbatch.app.domain.model.TabRecord
import com.tabbatch.app.domain.model.TabSource
import com.tabbatch.app.domain.normalizer.NormalizeResult
import com.tabbatch.app.domain.normalizer.UrlNormalizer
import java.util.UUID

/** Shared helper used by every import parser to turn a raw (url, title) pair into either an
 * accepted [TabRecord] or a [RejectedInput], with a stable incrementing [order]. */
internal class RecordFactory(
    private val source: TabSource,
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val idGenerator: () -> String = { UUID.randomUUID().toString() },
) {
    private var nextOrder = 0

    fun tryCreate(rawUrl: String, title: String?): Result<TabRecord> {
        return when (val result = UrlNormalizer.normalize(rawUrl)) {
            is NormalizeResult.Success -> {
                val registrableDomain = DomainGrouper.registrableDomainOf(result.host)
                val record = TabRecord(
                    id = idGenerator(),
                    url = result.normalizedUrl,
                    originalUrl = result.originalUrl,
                    title = title?.trim()?.takeIf { it.isNotEmpty() },
                    host = result.host,
                    registrableDomain = registrableDomain,
                    createdAt = clock(),
                    source = source,
                    order = nextOrder++,
                )
                Result.success(record)
            }
            is NormalizeResult.Failure -> Result.failure(ImportRejection(result.originalUrl, result.error))
        }
    }
}

internal class ImportRejection(
    val rawText: String,
    val error: com.tabbatch.app.domain.model.ImportError,
) : Exception(error.message)

internal fun RecordFactory.buildResult(
    outcomes: List<Result<TabRecord>>,
): ImportResult {
    val accepted = mutableListOf<TabRecord>()
    val rejected = mutableListOf<RejectedInput>()
    for (outcome in outcomes) {
        outcome.fold(
            onSuccess = { accepted.add(it) },
            onFailure = { ex ->
                if (ex is ImportRejection) {
                    rejected.add(RejectedInput(ex.rawText, ex.error))
                }
            },
        )
    }
    return ImportResult(accepted, rejected)
}
