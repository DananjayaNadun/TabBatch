package com.tabbatch.app.platform.browser

import com.tabbatch.app.domain.model.BrowserIntegrationError
import com.tabbatch.app.domain.model.TabRecord

/**
 * Abstraction for a *live* browser tab source — i.e. reading a browser's currently-open tabs
 * directly, without the user exporting/pasting/sharing anything.
 *
 * IMPORTANT: as of this writing there is no officially supported API for a third-party Android
 * app to enumerate Chrome for Android's live tab list. Chrome for Android does not support
 * desktop-style browser extensions, and there is no public `chrome.tabs`-equivalent surface
 * exposed to ordinary apps. See docs/ARCHITECTURE_DECISIONS.md (ADR-0001) and
 * docs/PLATFORM_LIMITATIONS.md for the full rationale.
 *
 * This interface exists so that IF such a capability ever becomes available (or for a browser
 * that does expose one), it can be added as a new adapter without changing the domain model,
 * import pipeline, or UI beyond swapping which [BrowserTabSource] is wired in.
 */
interface BrowserTabSource {
    /** Human-readable name of the browser/mechanism this source targets, e.g. "Chrome (Android)". */
    val displayName: String

    /** Whether this source is currently usable on this device. Adapters must NOT do expensive or
     * privacy-sensitive work here — just a cheap capability check. */
    fun isAvailable(): Boolean

    /** Attempts to read the browser's currently open tabs. Must return a typed failure
     * ([BrowserIntegrationError]) rather than throwing or fabricating data when unsupported. */
    suspend fun getOpenTabs(): Result<List<TabRecord>>
}

/**
 * The only [BrowserTabSource] implementation shipped today. It always reports itself as
 * unavailable and always fails with [BrowserIntegrationError.UnsupportedBrowserIntegration] —
 * this is intentional, not a stub bug. It exists so the UI has something concrete to bind to
 * and can render an honest "not supported" state instead of hiding the feature entirely or
 * faking a button that does nothing (see project brief section 1 and 17).
 */
class UnsupportedChromeAndroidTabSource : BrowserTabSource {
    override val displayName: String = "Chrome (Android)"

    override fun isAvailable(): Boolean = false

    override suspend fun getOpenTabs(): Result<List<TabRecord>> =
        Result.failure(UnsupportedIntegrationException(BrowserIntegrationError.UnsupportedBrowserIntegration))
}

class UnsupportedIntegrationException(val error: BrowserIntegrationError) : Exception(error.message)
