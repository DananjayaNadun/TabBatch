package com.tabbatch.app.domain

import com.tabbatch.app.domain.model.TabSource
import com.tabbatch.app.domain.parser.TextImportParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextImportParserTest {

    @Test
    fun `parses multiline urls`() {
        val text = """
            https://example.com/page1
            https://example.com/page2
            https://github.com/user/repo
        """.trimIndent()
        val result = TextImportParser.parse(text, TabSource.Clipboard)
        assertEquals(3, result.accepted.size)
        assertTrue(result.rejected.isEmpty())
    }

    @Test
    fun `blank lines are ignored not rejected`() {
        val text = "https://example.com/a\n\n\nhttps://example.com/b\n"
        val result = TextImportParser.parse(text, TabSource.Clipboard)
        assertEquals(2, result.accepted.size)
        assertTrue(result.rejected.isEmpty())
    }

    @Test
    fun `invalid lines are rejected but valid ones still accepted`() {
        val text = "https://example.com/a\nnot a url\nhttps://example.com/b"
        val result = TextImportParser.parse(text, TabSource.Clipboard)
        assertEquals(2, result.accepted.size)
        assertEquals(1, result.rejected.size)
    }

    @Test
    fun `empty input yields empty import error`() {
        val result = TextImportParser.parse("", TabSource.Clipboard)
        assertTrue(result.accepted.isEmpty())
        assertTrue(result.rejected.isNotEmpty())
    }

    @Test
    fun `order is stable and zero-based`() {
        val text = "https://a.com\nhttps://b.com\nhttps://c.com"
        val result = TextImportParser.parse(text, TabSource.Clipboard)
        assertEquals(listOf(0, 1, 2), result.accepted.map { it.order })
    }
}
