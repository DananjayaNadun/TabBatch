package com.tabbatch.app.platform.sharing

import android.content.ClipboardManager
import android.content.Context

/**
 * User-initiated clipboard read only (project brief section 16): TabBatch never polls or
 * monitors the clipboard in the background, and never stores clipboard history — it reads the
 * current clip contents exactly once, at the moment the user taps "Paste from Clipboard".
 */
object ClipboardHelper {
    /** Returns the current clipboard text, or null if empty/unavailable/not text. */
    fun readClipboardText(context: Context): String? {
        val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return null
        if (!manager.hasPrimaryClip()) return null
        val clip = manager.primaryClip ?: return null
        if (clip.itemCount == 0) return null
        return clip.getItemAt(0).coerceToText(context)?.toString()?.takeIf { it.isNotBlank() }
    }

    /** Cheap heuristic used purely for UI hinting ("Clipboard looks like it contains a URL") —
     * does not affect what gets imported; the real validation happens in UrlNormalizer. */
    fun looksLikeUrls(text: String): Boolean =
        text.lineSequence().any { it.trim().let { t -> t.startsWith("http://") || t.startsWith("https://") } }
}
