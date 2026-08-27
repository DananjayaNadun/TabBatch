package com.tabbatch.app.domain

import com.tabbatch.app.domain.grouping.DomainGrouper
import com.tabbatch.app.domain.model.TabRecord
import com.tabbatch.app.domain.model.TabSource

/** Deterministic synthetic fixture generators for tests — mirrors the 10/60/1000 sample
 * datasets shipped under sampledata/ for manual/demo use. No real user data is ever used. */
object TestFixtures {

    private val DOMAIN_WEIGHTS = listOf(
        "youtube.com" to 37, "github.com" to 9, "google.com" to 7, "reddit.com" to 4, "example.org" to 3,
    )

    /** The canonical 60-tab demo dataset from the project brief: 37 YouTube, 9 GitHub,
     * 7 Google, 4 Reddit, 3 Other — with 3 exact duplicates injected. */
    fun demo60(): List<TabRecord> {
        val records = mutableListOf<TabRecord>()
        var order = 0
        for ((domain, count) in DOMAIN_WEIGHTS) {
            repeat(count) { i ->
                records.add(recordFor(domain, "/watch?v=$i", order++))
            }
        }
        // Inject 3 exact duplicates of existing URLs.
        val dup1 = records[0].copy(id = "dup-1", order = order++)
        val dup2 = records[1].copy(id = "dup-2", order = order++)
        val dup3 = records[10].copy(id = "dup-3", order = order++)
        records.add(dup1)
        records.add(dup2)
        records.add(dup3)
        return records
    }

    fun of(count: Int): List<TabRecord> = (0 until count).map { i ->
        recordFor("example$${i % 25}.com".replace("$", ""), "/page/$i", i)
    }

    fun withUnicodeTitles(): List<TabRecord> = listOf(
        recordFor("example.com", "/a", 0, title = "日本語のタイトル"),
        recordFor("example.com", "/b", 1, title = "Emoji test 🎉🚀✨"),
        recordFor("example.com", "/c", 2, title = "Café — résumé"),
    )

    fun withLongUrl(): List<TabRecord> {
        val longPath = "/" + "segment".repeat(80)
        return listOf(recordFor("example.com", longPath, 0))
    }

    private fun recordFor(domain: String, path: String, order: Int, title: String? = "Title $order"): TabRecord {
        val url = "https://$domain$path"
        return TabRecord(
            id = "r$order-${domain}-$path".hashCode().toString(),
            url = url,
            originalUrl = url,
            title = title,
            host = domain,
            registrableDomain = DomainGrouper.registrableDomainOf(domain),
            createdAt = 1_700_000_000_000L + order,
            source = TabSource.TextFile,
            order = order,
        )
    }
}
