package com.tabbatch.app.domain

import com.tabbatch.app.domain.dedup.DuplicateDetector
import com.tabbatch.app.domain.model.TabRecord
import com.tabbatch.app.domain.model.TabSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DuplicateDetectorTest {

    private fun record(id: String, url: String, order: Int) = TabRecord(
        id = id, url = url, originalUrl = url, title = null, host = "example.com",
        registrableDomain = "example.com", createdAt = 0L, source = TabSource.Clipboard, order = order,
    )

    @Test
    fun `exact duplicate is detected`() {
        val records = listOf(
            record("1", "https://example.com/a", 0),
            record("2", "https://example.com/a", 1),
        )
        val duplicates = DuplicateDetector.duplicateIds(records)
        assertEquals(setOf("2"), duplicates)
    }

    @Test
    fun `same domain different page is not a duplicate`() {
        val records = listOf(
            record("1", "https://example.com/a", 0),
            record("2", "https://example.com/b", 1),
        )
        assertTrue(DuplicateDetector.duplicateIds(records).isEmpty())
    }

    @Test
    fun `same page different query parameter is not a duplicate`() {
        val records = listOf(
            record("1", "https://example.com/a?x=1", 0),
            record("2", "https://example.com/a?x=2", 1),
        )
        assertTrue(DuplicateDetector.duplicateIds(records).isEmpty())
    }

    @Test
    fun `deduplicate preserves first occurrence and stable ordering`() {
        val records = listOf(
            record("1", "https://example.com/a", 0),
            record("2", "https://example.com/b", 1),
            record("3", "https://example.com/a", 2),
        )
        val deduped = DuplicateDetector.deduplicate(records)
        assertEquals(listOf("1", "2"), deduped.map { it.id })
    }

    @Test
    fun `empty list has no duplicates`() {
        assertTrue(DuplicateDetector.duplicateIds(emptyList()).isEmpty())
    }

    @Test
    fun `three way duplicate counts only the later two`() {
        val records = listOf(
            record("1", "https://example.com/a", 0),
            record("2", "https://example.com/a", 1),
            record("3", "https://example.com/a", 2),
        )
        assertEquals(setOf("2", "3"), DuplicateDetector.duplicateIds(records))
    }
}
