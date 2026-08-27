package com.tabbatch.app.platform.sharing

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/** Writes generated export content to the app's cache/exports directory and builds a
 * FileProvider content:// Intent.ACTION_SEND for the Android share sheet. Never writes to a
 * location outside the app's own sandbox — SAF is used separately for user-directed "Save to..."
 * exports (see [com.tabbatch.app.platform.filepicker]). */
object FileShareHelper {

    private const val EXPORTS_DIR = "exports"

    fun writeExportFile(context: Context, fileName: String, content: String): File {
        val dir = File(context.cacheDir, EXPORTS_DIR).apply { mkdirs() }
        val file = File(dir, fileName)
        file.writeText(content)
        return file
    }

    fun writeExportBytes(context: Context, fileName: String, bytes: ByteArray): File {
        val dir = File(context.cacheDir, EXPORTS_DIR).apply { mkdirs() }
        val file = File(dir, fileName)
        file.writeBytes(bytes)
        return file
    }

    fun shareIntentFor(context: Context, file: File, mimeType: String): Intent {
        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(intent, "Share export")
    }
}
