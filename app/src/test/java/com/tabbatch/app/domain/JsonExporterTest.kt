package com.tabbatch.app.domain

import com.tabbatch.app.domain.export.ExportOptions
import com.tabbatch.app.domain.export.JsonExporter
import com.tabbatch.app.domain.export.TabBatchJsonSchema
import com.tabbatch.app.domain.parser.JsonImportParser
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JsonExporterTest {

    @Test
    fun `empty collection has schema version and empty tabs`() {
        val text = JsonExporter.export("Empty", 0L, emptyList(), ExportOptions(), 0L)
        val doc = Json.decodeFromString(TabBatchJsonSchema.Document.serializer(), text)
        assertEquals(TabBatchJsonSchema.CURRENT_VERSION, doc.schemaVersion)
        assertTrue(doc.tabs.isEmpty())
    }

    @Test
    fun `sixty records round trip through import parser`() {
        val records = TestFixtures.demo60()
        val text = JsonExporter.export("Demo", 0L, records, ExportOptions(), 0L)
        val reimported = JsonImportParser.parse(text)
        assertEquals(records.size, reimported.accepted.size)
    }

    @Test
    fun `one thousand records export succeeds`() {
        val records = TestFixtures.of(1000)
        val text = JsonExporter.export("Stress", 0L, records, ExportOptions(), 0L)
        val doc = Json.decodeFromString(TabBatchJsonSchema.Document.serializer(), text)
        assertEquals(1000, doc.tabs.size)
    }

    @Test
    fun `unicode titles survive round trip`() {
        val records = TestFixtures.withUnicodeTitles()
        val text = JsonExporter.export("Unicode", 0L, records, ExportOptions(), 0L)
        val doc = Json.decodeFromString(TabBatchJsonSchema.Document.serializer(), text)
        assertEquals("日本語のタイトル", doc.tabs[0].title)
    }

    @Test
    fun `duplicate flag is set correctly`() {
        val records = TestFixtures.demo60()
        val text = JsonExporter.export("Demo", 0L, records, ExportOptions(), 0L)
        val doc = Json.decodeFromString(TabBatchJsonSchema.Document.serializer(), text)
        val duplicateCount = doc.tabs.count { it.isDuplicate }
        assertEquals(3, duplicateCount)
    }
}
