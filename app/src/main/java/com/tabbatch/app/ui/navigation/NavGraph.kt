package com.tabbatch.app.ui.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Collection : Screen("collection")
    data object Group : Screen("group/{domain}") {
        fun route(domain: String) = "group/${java.net.URLEncoder.encode(domain, "UTF-8")}"
    }
    data object Export : Screen("export")
}
