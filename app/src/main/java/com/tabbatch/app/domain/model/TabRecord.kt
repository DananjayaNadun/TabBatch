package com.tabbatch.app.domain.model

/**
 * A single normalized browser tab / URL record.
 *
 * This is the central domain type of TabBatch. It intentionally contains no
 * Android framework types so that the whole domain package can be unit
 * tested on a plain JVM with no Android runtime.
 *
 * @property id Stable identifier, unique within a [TabCollection]. Not derived from the URL,
 *   so that two records with the same URL (exact duplicates) can both exist in memory.
 * @property url The normalized URL. This is what should be used for grouping, deduplication,
 *   and by default for export.
 * @property originalUrl The exact URL text as it was found in the source input, before any
 *   normalization (trimming, quote stripping, etc). Preserved for auditability.
 * @property title An optional page/tab title, if known from the source (e.g. a CSV title
 *   column, or a shared link's subject).
 * @property host The full host component of [url], e.g. "docs.example.com". Always lower-case.
 * @property registrableDomain The best-effort registrable ("eTLD+1") domain, e.g. "example.com"
 *   for host "docs.example.com". Null when no reasonable registrable domain could be derived
 *   (raw IP addresses, "localhost", single-label hosts, etc.) — see [DomainGrouper].
 * @property createdAt Epoch-millisecond timestamp of when this record was created/imported.
 * @property source Where this record came from.
 * @property order Stable 0-based import order, used to keep list/export ordering deterministic
 *   regardless of later sorting/grouping/search operations.
 */
data class TabRecord(
    val id: String,
    val url: String,
    val originalUrl: String,
    val title: String?,
    val host: String,
    val registrableDomain: String?,
    val createdAt: Long,
    val source: TabSource,
    val order: Int,
)
