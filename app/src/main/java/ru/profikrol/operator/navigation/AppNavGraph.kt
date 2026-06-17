package ru.profikrol.operator.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import ru.profikrol.operator.feature.auth.AuthScreen
import ru.profikrol.operator.feature.home.HomeActionId
import ru.profikrol.operator.feature.home.HomeScreen
import ru.profikrol.operator.feature.nestalignment.NestAlignmentScreen
import ru.profikrol.operator.feature.nestpreparation.NestPreparationScreen
import ru.profikrol.operator.feature.notifications.NotificationsScreen
import ru.profikrol.operator.feature.moving.MovingScreen
import ru.profikrol.operator.feature.rabbitculling.RabbitCullingScreen
import ru.profikrol.operator.feature.rfidinstallation.RfidInstallationScreen
import ru.profikrol.operator.feature.rfidscan.RfidScanScreen
import ru.profikrol.operator.feature.rfidscanresult.RfidScanResultScreen
import ru.profikrol.operator.feature.settings.SettingsScreen
import ru.profikrol.operator.feature.weighing.WeighingScreen

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
                onActionClick = { id ->
                    // Маппинг id действия → роут. `when` без `else`:
                    // компилятор не даст забыть ветку при добавлении нового HomeActionId.
                    val route = when (id) {
                        HomeActionId.NestPreparation -> Route.NestPreparation
                        HomeActionId.NestAlignment -> Route.NestAlignment
                        HomeActionId.RfidInstallation -> Route.RfidInstallation
                        HomeActionId.RabbitCulling -> Route.RabbitCulling
                    }
                    navController.navigate(route)
                },
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
                onWeighing = { code -> navController.navigate(Route.Weighing(code)) },
                onMoving = { code -> navController.navigate(Route.Moving(code)) },
            )
        }
        composable<Route.Weighing> { backStackEntry ->
            val route = backStackEntry.toRoute<Route.Weighing>()
            WeighingScreen(
                rfidCode = route.code,
                onBack = { navController.popBackStack() },
                onScanRfid = {
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
        composable<Route.Moving> { backStackEntry ->
            val route = backStackEntry.toRoute<Route.Moving>()
            MovingScreen(
                rfidCode = route.code,
                onBack = { navController.popBackStack() },
                onScanRfid = {
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
