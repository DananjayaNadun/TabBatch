package com.tabbatch.app.domain.model

/** A single rejected input line/record, kept so the user can see *why* something was skipped
 * instead of silently losing input. */
data class RejectedInput(
    val rawText: String,
    val reason: ImportError,
)

/** Typed import/validation failures. Mirrors section 21 of the project brief: user-facing
 * errors should be translatable into a clear "what happened / what can I do" message. */
sealed class ImportError(val message: String) {
    data object EmptyImport : ImportError("No URLs were found in the input.")
    data class InvalidUrl(val detail: String) : ImportError("Not a valid http(s) URL: $detail")
    data class UnsupportedScheme(val scheme: String) : ImportError("Unsupported URL scheme: $scheme")
    data object MalformedCsv : ImportError("Could not find a URL column in this CSV file.")
    data object MalformedJson : ImportError("This file is not a valid TabBatch JSON export.")
    data class UnsupportedFormat(val detail: String) : ImportError("Unsupported input format: $detail")
}

/** Typed export failures. */
sealed class ExportError(val message: String) {
    data class ExportFailed(val detail: String) : ExportError("Export failed: $detail")
    data object StorageUnavailable : ExportError("No writable location was selected or storage is unavailable.")
    data object EmptySelection : ExportError("Nothing is selected to export.")
}

/** Result of a browser tab source query — see [com.tabbatch.app.platform.browser.BrowserTabSource]. */
sealed class BrowserIntegrationError(val message: String) {
    data object UnsupportedBrowserIntegration : BrowserIntegrationError(
        "Live tab access is not available through any officially supported Chrome Android API."
    )
    data object ExperimentalIntegrationUnavailable : BrowserIntegrationError(
        "The experimental accessibility-based integration is not enabled on this device."
    )
}
