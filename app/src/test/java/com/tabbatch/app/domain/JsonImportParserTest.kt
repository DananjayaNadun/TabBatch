package com.tabbatch.app.domain

import com.tabbatch.app.domain.parser.JsonImportParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JsonImportParserTest {

    @Test
    fun `parses valid tabbatch schema`() {
        val jsonText = """
            {
              "schemaVersion": 1,
              "collection": { "name": "Research", "createdAt": 1000 },
              "tabs": [
                { "order": 0, "id": "a", "url": "https://example.com/1", "host": "example.com", "source": "Json", "createdAt": 1000 },
                { "order": 1, "id": "b", "url": "https://example.com/2", "host": "example.com", "source": "Json", "createdAt": 1000, "title": "Two" }
              ]
            }
        """.trimIndent()
        val result = JsonImportParser.parse(jsonText)
        assertEquals(2, result.accepted.size)
        assertEquals("Two", result.accepted[1].title)
    }

    @Test
    fun `empty text yields empty import error`() {
        val result = JsonImportParser.parse("")
        assertTrue(result.accepted.isEmpty())
        assertTrue(result.rejected.isNotEmpty())
    }

    @Test
    fun `malformed json is rejected`() {
        val result = JsonImportParser.parse("{ this is not json ")
        assertTrue(result.accepted.isEmpty())
        assertTrue(result.rejected.isNotEmpty())
    }

    @Test
    fun `unknown schema version is rejected`() {
        val jsonText = """{"schemaVersion": 999, "collection": {"name": "x", "createdAt": 0}, "tabs": []}"""
        val result = JsonImportParser.parse(jsonText)
        assertTrue(result.accepted.isEmpty())
        assertTrue(result.rejected.isNotEmpty())
    }

    @Test
    fun `empty tabs array yields empty import error`() {
        val jsonText = """{"schemaVersion": 1, "collection": {"name": "x", "createdAt": 0}, "tabs": []}"""
        val result = JsonImportParser.parse(jsonText)
        assertTrue(result.accepted.isEmpty())
    }

    @Test
    fun `invalid url inside valid schema is rejected individually`() {
        val jsonText = """
            {
              "schemaVersion": 1,
              "collection": { "name": "x", "createdAt": 0 },
              "tabs": [
                { "order": 0, "id": "a", "url": "not a url", "host": "x", "source": "Json", "createdAt": 0 }
              ]
            }
        """.trimIndent()
        val result = JsonImportParser.parse(jsonText)
        assertTrue(result.accepted.isEmpty())
        assertEquals(1, result.rejected.size)
    }
}
