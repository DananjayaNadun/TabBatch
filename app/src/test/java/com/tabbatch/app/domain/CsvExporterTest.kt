package com.tabbatch.app.domain

import com.tabbatch.app.domain.export.CsvExporter
import com.tabbatch.app.domain.export.ExportOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvExporterTest {

    @Test
    fun `empty collection produces header only`() {
        val csv = CsvExporter.export(emptyList(), ExportOptions())
        val lines = csv.trim().lines()
        assertEquals(1, lines.size)
        assertTrue(lines[0].startsWith("order,id,title,url"))
    }

    @Test
    fun `single record round trips fields`() {
        val records = TestFixtures.of(1)
        val csv = CsvExporter.export(records, ExportOptions())
        val lines = csv.trim().lines()
        assertEquals(2, lines.size)
        assertTrue(lines[1].contains("https://example0.com/page/0"))
    }

    @Test
    fun `sixty records export all rows`() {
        val records = TestFixtures.demo60()
        val csv = CsvExporter.export(records, ExportOptions())
        assertEquals(records.size + 1, csv.trim().lines().size)
    }

    @Test
    fun `one thousand records export all rows`() {
        val records = TestFixtures.of(1000)
        val csv = CsvExporter.export(records, ExportOptions())
        assertEquals(1001, csv.trim().lines().size)
    }

    @Test
    fun `excluding duplicates drops later occurrences`() {
        val records = TestFixtures.demo60()
        val withDupes = CsvExporter.export(records, ExportOptions(includeDuplicates = true))
        val withoutDupes = CsvExporter.export(records, ExportOptions(includeDuplicates = false))
        assertTrue(withDupes.trim().lines().size > withoutDupes.trim().lines().size)
    }

    @Test
    fun `unicode titles are preserved`() {
        val csv = CsvExporter.export(TestFixtures.withUnicodeTitles(), ExportOptions())
        assertTrue(csv.contains("日本語のタイトル"))
        assertTrue(csv.contains("🎉"))
    }

    @Test
    fun `commas in title are quoted`() {
        val records = TestFixtures.of(1).map { it.copy(title = "A, B, C") }
        val csv = CsvExporter.export(records, ExportOptions())
        assertTrue(csv.contains("\"A, B, C\""))
    }

    @Test
    fun `long url is not truncated`() {
        val csv = CsvExporter.export(TestFixtures.withLongUrl(), ExportOptions())
        assertTrue(csv.contains("segment".repeat(80)))
    }
}
