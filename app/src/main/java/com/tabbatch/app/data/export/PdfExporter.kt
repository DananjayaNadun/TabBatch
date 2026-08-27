package com.tabbatch.app.data.export

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.tabbatch.app.domain.export.ExportOptions
import com.tabbatch.app.domain.model.DomainGroup
import com.tabbatch.app.domain.model.DuplicateSummary
import com.tabbatch.app.domain.model.TabRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Generates a PDF export using the platform's built-in [PdfDocument] API — no third-party PDF
 * library is needed for TabBatch's requirements (branded header, grouped sections, wrapped text,
 * page numbers). Runs entirely off the caller's thread via [Dispatchers.Default] (project brief
 * section 12: "Generation must happen off the main/UI thread").
 */
object PdfExporter {

    private const val PAGE_WIDTH = 595 // A4 at 72dpi
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 40f
    private const val CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN

    suspend fun export(
        collectionName: String,
        records: List<TabRecord>,
        options: ExportOptions,
        exportedAt: Long = System.currentTimeMillis(),
    ): ByteArray = withContext(Dispatchers.Default) {
        val duplicateIds = DuplicateSummary.of(records).duplicateRecordIds
        val filtered = if (options.includeDuplicates) records else records.filterNot { it.id in duplicateIds }
        val groups = DomainGroup.groupRecords(filtered)
        val summary = DuplicateSummary.of(records)

        val document = PdfDocument()
        val titlePaint = Paint().apply { textSize = 20f; isFakeBoldText = true; isAntiAlias = true }
        val metaPaint = Paint().apply { textSize = 10f; color = 0xFF555555.toInt(); isAntiAlias = true }
        val groupPaint = Paint().apply { textSize = 13f; isFakeBoldText = true; isAntiAlias = true; color = 0xFF1A237E.toInt() }
        val titleTextPaint = Paint().apply { textSize = 11f; isAntiAlias = true; typeface = Typeface.DEFAULT_BOLD }
        val urlPaint = Paint().apply { textSize = 10f; isAntiAlias = true; color = 0xFF2962FF.toInt() }
        val footerPaint = Paint().apply { textSize = 9f; color = 0xFF888888.toInt(); isAntiAlias = true; textAlign = Paint.Align.CENTER }

        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
        var canvas = page.canvas
        var y = MARGIN + 10f

        fun finishPage() {
            canvas.drawText("Page $pageNumber", PAGE_WIDTH / 2f, PAGE_HEIGHT - 20f, footerPaint)
            document.finishPage(page)
        }

        fun newPage() {
            finishPage()
            pageNumber++
            page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
            canvas = page.canvas
            y = MARGIN
        }

        fun ensureSpace(needed: Float) {
            if (y + needed > PAGE_HEIGHT - MARGIN - 20f) newPage()
        }

        // Header
        canvas.drawText("TabBatch export", MARGIN, y, titlePaint)
        y += 24f
        canvas.drawText(collectionName, MARGIN, y, metaPaint)
        y += 14f
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(exportedAt))
        canvas.drawText(
            "Exported $dateStr  •  ${filtered.size} records  •  ${groups.size} groups  •  " +
                "${summary.duplicateCount} duplicates",
            MARGIN, y, metaPaint,
        )
        y += 24f

        for (group in groups) {
            ensureSpace(24f)
            canvas.drawText("${group.displayName}  (${group.count})", MARGIN, y, groupPaint)
            y += 18f

            group.records.forEachIndexed { index, record ->
                val n = index + 1
                if (options.includeTitles && !record.title.isNullOrBlank()) {
                    val titleLines = wrapText(record.title, titleTextPaint, CONTENT_WIDTH)
                    for ((li, line) in titleLines.withIndex()) {
                        ensureSpace(14f)
                        val prefix = if (li == 0) "$n. " else "    "
                        canvas.drawText(prefix + line, MARGIN, y, titleTextPaint)
                        y += 13f
                    }
                }
                if (options.includeUrls) {
                    val urlLines = wrapText(record.url, urlPaint, CONTENT_WIDTH - 10f)
                    for (line in urlLines) {
                        ensureSpace(13f)
                        canvas.drawText("   " + line, MARGIN, y, urlPaint)
                        y += 12f
                    }
                }
                if (record.id in duplicateIds) {
                    ensureSpace(12f)
                    canvas.drawText("   (duplicate)", MARGIN, y, metaPaint)
                    y += 12f
                }
                y += 4f
            }
            y += 10f
        }

        if (filtered.isEmpty()) {
            canvas.drawText("No records to export.", MARGIN, y, metaPaint)
        }

        finishPage()

        val out = ByteArrayOutputStream()
        document.writeTo(out)
        document.close()
        out.toByteArray()
    }

    /** Simple greedy word-wrap using [Paint.measureText]; sufficient for our monospaced-ish URL
     * and title rendering needs and avoids pulling in a text-layout library. */
    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        if (text.isEmpty()) return emptyList()
        val lines = mutableListOf<String>()
        var current = StringBuilder()
        for (word in text.split(Regex("(?<=\\s)|(?=\\s)")).filter { it.isNotEmpty() }) {
            val candidate = current.toString() + word
            if (paint.measureText(candidate) > maxWidth && current.isNotEmpty()) {
                lines.add(current.toString().trimEnd())
                current = StringBuilder(word.trimStart())
            } else {
                current.append(word)
            }
            // Break very long single "words" (e.g. long URLs with no spaces) by character.
            while (paint.measureText(current.toString()) > maxWidth) {
                var cut = current.length
                while (cut > 1 && paint.measureText(current.substring(0, cut)) > maxWidth) cut--
                lines.add(current.substring(0, cut))
                current = StringBuilder(current.substring(cut))
            }
        }
        if (current.isNotEmpty()) lines.add(current.toString().trimEnd())
        return lines
    }
}
