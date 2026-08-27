package com.tabbatch.app.domain

import com.tabbatch.app.domain.export.ExportOptions
import com.tabbatch.app.domain.export.TextExporter
import org.junit.Assert.assertTrue
import org.junit.Test

class TextExporterTest {

    @Test
    fun `empty collection produces just the header line`() {
        val text = TextExporter.export("Empty", emptyList(), ExportOptions())
        assertTrue(text.startsWith("Empty — 0 tabs"))
    }

    @Test
    fun `grouped output includes domain headings`() {
        val text = TextExporter.export("Demo", TestFixtures.demo60(), ExportOptions())
        assertTrue(text.contains("youtube.com"))
        assertTrue(text.contains("github.com"))
    }

    @Test
    fun `one thousand records does not throw`() {
        val text = TextExporter.export("Stress", TestFixtures.of(1000), ExportOptions())
        assertTrue(text.isNotEmpty())
    }

    @Test
    fun `unicode title is preserved`() {
        val text = TextExporter.export("U", TestFixtures.withUnicodeTitles(), ExportOptions())
        assertTrue(text.contains("Emoji test 🎉🚀✨"))
    }

    @Test
    fun `long url is preserved in full`() {
        val text = TextExporter.export("L", TestFixtures.withLongUrl(), ExportOptions())
        assertTrue(text.contains("segment".repeat(80)))
    }
}
