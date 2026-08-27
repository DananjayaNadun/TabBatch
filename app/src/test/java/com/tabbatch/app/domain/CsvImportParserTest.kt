package com.tabbatch.app.domain

import com.tabbatch.app.domain.parser.CsvImportParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvImportParserTest {

    @Test
    fun `detects url column by common header name`() {
        val csv = "url,other\nhttps://example.com/a,x\nhttps://example.com/b,y"
        val result = CsvImportParser.parse(csv)
        assertEquals(2, result.accepted.size)
    }

    @Test
    fun `detects alternate header aliases`() {
        for (header in listOf("URL", "link", "href", "address")) {
            val csv = "$header\nhttps://example.com/a"
            val result = CsvImportParser.parse(csv)
            assertEquals("header=$header", 1, result.accepted.size)
        }
    }

    @Test
    fun `retains title when title column present`() {
        val csv = "title,url\nHello World,https://example.com/a"
        val result = CsvImportParser.parse(csv)
        assertEquals("Hello World", result.accepted.first().title)
    }

    @Test
    fun `handles quoted fields containing commas`() {
        val csv = "title,url\n\"Hello, World\",https://example.com/a"
        val result = CsvImportParser.parse(csv)
        assertEquals("Hello, World", result.accepted.first().title)
    }

    @Test
    fun `missing url column is malformed csv`() {
        val csv = "title,description\nfoo,bar"
        val result = CsvImportParser.parse(csv)
        assertTrue(result.accepted.isEmpty())
        assertTrue(result.rejected.isNotEmpty())
    }

    @Test
    fun `empty csv is empty import`() {
        val result = CsvImportParser.parse("")
        assertTrue(result.accepted.isEmpty())
        assertTrue(result.rejected.isNotEmpty())
    }

    @Test
    fun `malformed row url is rejected individually`() {
        val csv = "url\nhttps://example.com/a\nnot a url\nhttps://example.com/b"
        val result = CsvImportParser.parse(csv)
        assertEquals(2, result.accepted.size)
        assertEquals(1, result.rejected.size)
    }
}
