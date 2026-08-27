package com.tabbatch.app.domain.model

/** Where a [TabRecord] originated from. Purely descriptive/audit metadata. */
enum class TabSource {
    Clipboard,
    SharedText,
    TextFile,
    Csv,
    Json,
    /** Reserved for a future live-browser adapter. No implementation exists today — see
     * [com.tabbatch.app.platform.browser.BrowserTabSource] and docs/PLATFORM_LIMITATIONS.md. */
    FutureBrowserAdapter,
}
