package com.tabbatch.app.domain

import com.tabbatch.app.domain.dedup.DuplicateDetector
import com.tabbatch.app.domain.export.CsvExporter
import com.tabbatch.app.domain.export.ExportOptions
import com.tabbatch.app.domain.export.JsonExporter
import com.tabbatch.app.domain.export.TextExporter
import com.tabbatch.app.domain.model.DomainGroup
import com.tabbatch.app.domain.model.TabSource
import com.tabbatch.app.domain.parser.TextImportParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * End-to-end pipeline test (parse -> normalize -> group -> dedup -> export) against a
 * deterministic 1000-URL fixture spanning 20+ domains, with injected exact duplicates and
 * long titles/URLs. Exists to prove the pipeline completes with correct counts and doesn't
 * hide an O(n^2) blow-up as the dataset grows (see docs/PLATFORM_LIMITATIONS.md /
 * CHANGELOG.md "Known limitations" — this was previously untested).
 */
class LargeDatasetPipelineTest {

    // Distinct registrable domains (not subdomains of a shared parent) so grouping actually
    // produces 25 separate domain groups rather than collapsing under DomainGrouper's
    // last-two-labels heuristic.
    private val domains = (1..25).map { "site$it.com" }

    /** Builds raw multiline input text: 1000 unique URLs distributed across 25 domains, plus
     * 40 exact-duplicate lines (repeats of earlier lines) and a handful of very long titles
     * baked into the URL path (titles aren't carried by plain-text import, so long *URLs* are
     * used to stress word-wrap/measurement paths downstream in export). */
    private fun buildRawText(uniqueCount: Int, duplicateCount: Int): String {
        val lines = mutableListOf<String>()
        for (i in 0 until uniqueCount) {
            val domain = domains[i % domains.size]
            val path = if (i % 97 == 0) "/very/long/path/" + "segment".repeat(40) else "/page/$i"
            lines.add("https://$domain$path")
        }
        // Duplicate a spread of earlier lines exactly, so DuplicateDetector has real work to do.
        repeat(duplicateCount) { i ->
            lines.add(lines[(i * 13) % lines.size])
        }
        return lines.joinToString("\n")
    }

    @Test
    fun `full pipeline handles 1000 URLs across 25 domains with duplicates`() {
        val uniqueCount = 1000
        val duplicateCount = 40
        val text = buildRawText(uniqueCount, duplicateCount)

        // 1. Parse + normalize.
        val result = TextImportParser.parse(text, TabSource.TextFile)
        assertTrue("no lines should be rejected in this well-formed fixture", result.rejected.isEmpty())
        assertEquals(uniqueCount + duplicateCount, result.accepted.size)

        val records = result.accepted.mapIndexed { index, r -> r.copy(order = index) }

        // 2. Group by domain.
        val groups = DomainGroup.groupRecords(records)
        assertEquals(domains.size, groups.size)
        assertEquals(records.size, groups.sumOf { it.count })

        // 3. Dedup.
        val duplicateIds = DuplicateDetector.duplicateIds(records)
        assertEquals(duplicateCount, duplicateIds.size)
        val deduped = DuplicateDetector.deduplicate(records)
        assertEquals(uniqueCount, deduped.size)

        // 4. Export to every format and assert basic correctness/completion.
        val options = ExportOptions()

        val csv = CsvExporter.export(records, options)
        assertEquals(records.size + 1, csv.trim().lines().size) // header + one row per record

        val csvNoDupes = CsvExporter.export(records, options.copy(includeDuplicates = false))
        assertEquals(uniqueCount + 1, csvNoDupes.trim().lines().size)

        val json = JsonExporter.export(
            collectionName = "Stress test",
            collectionCreatedAt = 1_700_000_000_000L,
            records = records,
            options = options,
            exportedAt = 1_700_000_100_000L,
        )
        assertTrue(json.contains("\"totalCount\": ${records.size}") || json.contains("\"totalCount\":${records.size}"))
        assertTrue(json.contains("\"duplicateCount\": $duplicateCount") || json.contains("\"duplicateCount\":$duplicateCount"))

        val text2 = TextExporter.export("Stress test", records, options)
        assertTrue(text2.isNotBlank())
        // One header line per domain group present in the grouped text output.
        for (group in groups) {
            assertTrue("expected group '${group.displayName}' in text export", text2.contains(group.displayName))
        }
    }

    @Test
    fun `full pipeline completes quickly (no quadratic blow-up)`() {
        val text = buildRawText(5000, 200)
        val start = System.nanoTime()

        val result = TextImportParser.parse(text, TabSource.TextFile)
        val records = result.accepted
        DomainGroup.groupRecords(records)
        DuplicateDetector.deduplicate(records)
        CsvExporter.export(records, ExportOptions())

        val elapsedMs = (System.nanoTime() - start) / 1_000_000
        assertEquals(5200, records.size)
        // Generous ceiling: this is about correctness-of-scaling, not tight performance
        // benchmarking on shared CI hardware. An O(n^2) implementation of any pipeline stage
        // would blow well past this for 5000+ records.
        assertTrue("pipeline took ${elapsedMs}ms, expected it to complete quickly", elapsedMs < 10_000)
    }
}
