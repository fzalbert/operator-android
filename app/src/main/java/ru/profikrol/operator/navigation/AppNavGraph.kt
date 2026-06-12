package ru.profikrol.operator.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import ru.profikrol.operator.feature.auth.AuthScreen
import ru.profikrol.operator.feature.home.HomeScreen
import ru.profikrol.operator.feature.nestalignment.NestAlignmentScreen
import ru.profikrol.operator.feature.nestpreparation.NestPreparationScreen
import ru.profikrol.operator.feature.notifications.NotificationsScreen
import ru.profikrol.operator.feature.rabbitculling.RabbitCullingScreen
import ru.profikrol.operator.feature.rfidinstallation.RfidInstallationScreen
import ru.profikrol.operator.feature.rfidscan.RfidScanScreen
import ru.profikrol.operator.feature.rfidscanresult.RfidScanResultScreen
import ru.profikrol.operator.feature.settings.SettingsScreen

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Route.Auth,
    ) {
        composable<Route.Auth> {
            AuthScreen(
                onLoggedIn = {
                    navController.navigate(Route.Home) {
                        popUpTo(Route.Auth) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }

        composable<Route.Home> {
            HomeScreen(
                onOpenNotifications = { navController.navigate(Route.Notifications) },
                onOpenSettings = { navController.navigate(Route.Settings) },
                onScanRfid = { navController.navigate(Route.RfidScan) },
                onOpenNestPreparation = { navController.navigate(Route.NestPreparation) },
                onOpenNestAlignment = { navController.navigate(Route.NestAlignment) },
                onOpenRfidInstallation = { navController.navigate(Route.RfidInstallation) },
                onOpenRabbitCulling = { navController.navigate(Route.RabbitCulling) },
            )
        }
        composable<Route.Notifications> {
            NotificationsScreen(
                onBack = { navController.popBackStack() },
            )
        }
        composable<Route.Settings> {
            SettingsScreen(
                onBack = { navController.popBackStack() },
            )
        }
        composable<Route.RfidScan> {
            RfidScanScreen(
                onBack = { navController.popBackStack() },
                onScanned = { code ->
                    navController.navigate(Route.RfidScanResult(code))
                },
            )
        }
        composable<Route.RfidScanResult> { backStackEntry ->
            val route = backStackEntry.toRoute<Route.RfidScanResult>()
            RfidScanResultScreen(
                rfidCode = route.code,
                onBack = { navController.popBackStack() },
                onScanAgain = {
                    navController.popBackStack(
                        route = Route.RfidScan,
                        inclusive = true,
                    )
                    navController.navigate(Route.RfidScan) {
                        launchSingleTop = true
                    }
                },
            )
        }
        composable<Route.NestPreparation> {
            NestPreparationScreen(onBack = { navController.popBackStack() })
        }
        composable<Route.NestAlignment> {
            NestAlignmentScreen(onBack = { navController.popBackStack() })
        }
        composable<Route.RfidInstallation> {
            RfidInstallationScreen(onBack = { navController.popBackStack() })
        }
        composable<Route.RabbitCulling> {
            RabbitCullingScreen(onBack = { navController.popBackStack() })
        }
    }
}
