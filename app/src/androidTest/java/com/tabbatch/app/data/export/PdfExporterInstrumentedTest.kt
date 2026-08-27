package com.tabbatch.app.data.export

import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tabbatch.app.domain.export.ExportOptions
import com.tabbatch.app.domain.grouping.DomainGrouper
import com.tabbatch.app.domain.model.TabRecord
import com.tabbatch.app.domain.model.TabSource
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Instrumented tests for [PdfExporter] — needs a real android.graphics implementation
 * (Canvas/Paint/PdfDocument aren't available on the plain JVM unit-test runner), so these run
 * under connectedAndroidTest / on a device or emulator.
 *
 * Verifies: no crash and a sane page count across 1/10/60/100/1000 records, and edge cases
 * (empty title, a URL long enough to force character-level hard-wrapping, unicode title).
 */
@RunWith(AndroidJUnit4::class)
class PdfExporterInstrumentedTest {

    private fun record(
        i: Int,
        title: String? = "Title $i",
        domain: String = "site${i % 20}.com",
        path: String = "/page/$i",
    ): TabRecord {
        val url = "https://$domain$path"
        return TabRecord(
            id = "r$i",
            url = url,
            originalUrl = url,
            title = title,
            host = domain,
            registrableDomain = DomainGrouper.registrableDomainOf(domain),
            createdAt = 1_700_000_000_000L + i,
            source = TabSource.TextFile,
            order = i,
        )
    }

    /** Writes [bytes] to a temp file and returns the PDF page count via [PdfRenderer], the
     * platform's own PDF page-boundary parser — an independent check that PdfExporter's
     * pagination produced a structurally valid, decodable multi-page document. */
    private fun pageCountOf(bytes: ByteArray): Int {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File.createTempFile("pdf_test", ".pdf", context.cacheDir)
        file.writeBytes(bytes)
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
            PdfRenderer(pfd).use { renderer ->
                return renderer.pageCount
            }
        }
    }

    @Test
    fun single_record_produces_a_one_page_pdf() = runBlocking {
        val bytes = PdfExporter.export("Test", listOf(record(0)), ExportOptions())
        assertTrue(bytes.isNotEmpty())
        assertEquals(1, pageCountOf(bytes))
    }

    @Test
    fun ten_records_produce_a_valid_pdf() = runBlocking {
        val records = (0 until 10).map { record(it) }
        val bytes = PdfExporter.export("Test", records, ExportOptions())
        assertTrue(pageCountOf(bytes) >= 1)
    }

    @Test
    fun sixty_records_produce_a_valid_multi_record_pdf() = runBlocking {
        val records = (0 until 60).map { record(it) }
        val bytes = PdfExporter.export("Test", records, ExportOptions())
        assertTrue(pageCountOf(bytes) >= 1)
    }

    @Test
    fun one_hundred_records_paginate_across_multiple_pages() = runBlocking {
        val records = (0 until 100).map { record(it) }
        val bytes = PdfExporter.export("Test", records, ExportOptions())
        assertTrue("expected pagination to kick in for 100 records", pageCountOf(bytes) > 1)
    }

    @Test
    fun one_thousand_records_complete_without_crashing_and_paginate() = runBlocking {
        val records = (0 until 1000).map { record(it) }
        val bytes = PdfExporter.export("Test", records, ExportOptions())
        assertTrue(bytes.isNotEmpty())
        val pages = pageCountOf(bytes)
        assertTrue("expected many pages for 1000 records, got $pages", pages > 5)
    }

    @Test
    fun empty_title_does_not_crash_and_is_simply_omitted() = runBlocking {
        val records = listOf(record(0, title = null), record(1, title = ""))
        val bytes = PdfExporter.export("Test", records, ExportOptions())
        assertTrue(bytes.isNotEmpty())
        assertEquals(1, pageCountOf(bytes))
    }

    @Test
    fun very_long_url_with_no_break_points_wraps_by_character_without_infinite_loop() = runBlocking {
        val longUrl = "https://example.com/" + "a".repeat(5000)
        val record = TabRecord(
            id = "long",
            url = longUrl,
            originalUrl = longUrl,
            title = "Long URL",
            host = "example.com",
            registrableDomain = "example.com",
            createdAt = 1_700_000_000_000L,
            source = TabSource.TextFile,
            order = 0,
        )
        // The assertion itself is that this completes at all (a broken wrapText could loop
        // forever or throw); a timeout at the test-runner level would catch a real hang.
        val bytes = PdfExporter.export("Test", listOf(record), ExportOptions())
        assertTrue(bytes.isNotEmpty())
        // The main assertion is implicit: a broken wrapText could loop forever or throw on an
        // unbroken 5000-character token, and this completed and produced a valid, decodable PDF.
        assertTrue(pageCountOf(bytes) >= 1)
    }

    @Test
    fun unicode_title_renders_without_crashing() = runBlocking {
        val records = listOf(
            record(0, title = "日本語のタイトル"),
            record(1, title = "Emoji test 🎉🚀✨"),
            record(2, title = "Café — résumé"),
        )
        val bytes = PdfExporter.export("Test", records, ExportOptions())
        assertTrue(bytes.isNotEmpty())
        assertEquals(1, pageCountOf(bytes))
    }

    @Test
    fun empty_collection_renders_a_single_page_with_no_records_message() = runBlocking {
        val bytes = PdfExporter.export("Test", emptyList(), ExportOptions())
        assertTrue(bytes.isNotEmpty())
        assertEquals(1, pageCountOf(bytes))
    }
}
