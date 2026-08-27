package com.tabbatch.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tabbatch.app.ui.AppViewModel
import com.tabbatch.app.ui.collection.CollectionScreen
import com.tabbatch.app.ui.export.ExportScreen
import com.tabbatch.app.ui.group.GroupScreen
import com.tabbatch.app.ui.home.HomeScreen

@Composable
fun TabBatchNavHost(
    viewModel: AppViewModel = viewModel(),
    navController: NavHostController = rememberNavController(),
    initialSharedText: String? = null,
) {
    val collection by viewModel.collection.collectAsState()

    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(
                viewModel = viewModel,
                initialSharedText = initialSharedText,
                onCollectionReady = { navController.navigateSingleTopTo(Screen.Collection.route) },
            )
        }
        composable(Screen.Collection.route) {
            CollectionScreen(
                viewModel = viewModel,
                onGroupSelected = { domain -> navController.navigate(Screen.Group.route(domain)) },
                onExport = { navController.navigate(Screen.Export.route) },
                onBackToHome = { navController.popBackStack(Screen.Home.route, inclusive = false) },
            )
        }
        composable(Screen.Group.route) { backStackEntry ->
            val encodedDomain = backStackEntry.arguments?.getString("domain").orEmpty()
            val domain = java.net.URLDecoder.decode(encodedDomain, "UTF-8")
            GroupScreen(
                viewModel = viewModel,
                domain = domain,
                onBack = { navController.popBackStack() },
                onExport = { navController.navigate(Screen.Export.route) },
            )
        }
        composable(Screen.Export.route) {
            ExportScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
            )
        }
    }
}

private fun NavHostController.navigateSingleTopTo(route: String) = navigate(route) {
    launchSingleTop = true
}
