package com.tabbatch.app.platform.sharing

import android.content.Intent

/** Extracts shared text from an incoming ACTION_SEND / ACTION_SEND_MULTIPLE intent
 * (Android Sharesheet — project brief section 15). */
object ShareIntentParser {

    fun extractSharedText(intent: Intent): String? {
        return when (intent.action) {
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)
            Intent.ACTION_SEND_MULTIPLE -> {
                val texts = intent.getStringArrayListExtra(Intent.EXTRA_TEXT)
                texts?.joinToString("\n")
            }
            else -> null
        }
    }
}
