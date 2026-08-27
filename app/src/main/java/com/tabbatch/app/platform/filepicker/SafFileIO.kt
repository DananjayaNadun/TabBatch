package com.tabbatch.app.platform.filepicker

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

/** Thin wrapper around the Storage Access Framework content resolver for reading a
 * user-picked import file and writing a user-picked export destination. No filesystem paths are
 * assumed — everything goes through content:// [Uri]s returned by
 * ACTION_OPEN_DOCUMENT / ACTION_CREATE_DOCUMENT, which the UI layer launches via
 * `rememberLauncherForActivityResult`. */
object SafFileIO {

    suspend fun readText(context: Context, uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                BufferedReader(InputStreamReader(input)).readText()
            } ?: throw IllegalStateException("Could not open input stream for $uri")
        }
    }

    suspend fun writeText(context: Context, uri: Uri, content: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openOutputStream(uri)?.use { output ->
                output.write(content.toByteArray(Charsets.UTF_8))
            } ?: throw IllegalStateException("Could not open output stream for $uri")
        }
    }

    suspend fun writeBytes(context: Context, uri: Uri, bytes: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openOutputStream(uri)?.use { output ->
                output.write(bytes)
            } ?: throw IllegalStateException("Could not open output stream for $uri")
        }
    }
}
