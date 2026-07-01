package ru.profikrol.operator.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import ru.profikrol.operator.core.network.NetworkMonitor
import ru.profikrol.operator.domain.model.Rabbit
import ru.profikrol.operator.feature.auth.AuthScreen
import ru.profikrol.operator.feature.home.HomeActionId
import ru.profikrol.operator.feature.home.HomeScreen
import ru.profikrol.operator.feature.moving.MovingScreen
import ru.profikrol.operator.feature.nestalignment.NestAlignmentScreen
import ru.profikrol.operator.feature.nestpreparation.NestPreparationScreen
import ru.profikrol.operator.feature.notifications.NotificationsScreen
import ru.profikrol.operator.feature.rabbitculling.RabbitCullingScreen
import ru.profikrol.operator.feature.rabbitprofile.RabbitProfileScreen
import ru.profikrol.operator.feature.rfidinstallation.RfidInstallationScreen
import ru.profikrol.operator.feature.rfidscan.RfidScanScreen
import ru.profikrol.operator.feature.rfidscanresult.RfidScanResultScreen
import ru.profikrol.operator.feature.settings.SettingsScreen
import ru.profikrol.operator.feature.weighing.WeighingScreen
import androidx.compose.runtime.CompositionLocalProvider
import ru.profikrol.operator.core.network.LocalNetworkStatusProvider

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val context = androidx.compose.ui.platform.LocalContext.current
    val networkMonitor = remember { NetworkMonitor(context) }
    CompositionLocalProvider(
        LocalNetworkStatusProvider provides networkMonitor
    ) {
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
                    onLoggedOut = {
                        navController.navigate(Route.Auth) {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
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
                        navController.navigate(Route.RfidScan)
                    },
                    onWeighing = { code -> navController.navigate(Route.Weighing(code)) },
                    onMoving = { code -> navController.navigate(Route.Moving(code)) },
                    onViewCard = { code -> navController.navigate(Route.RabbitProfile(code)) },
                    onCullingClick = { rabbit ->
                        navController.currentBackStackEntry
                            ?.savedStateHandle
                            ?.set("rfidCode", rabbit.rfidCode)

                        navController.currentBackStackEntry
                            ?.savedStateHandle
                            ?.set("status", rabbit.status)

                        navController.currentBackStackEntry
                            ?.savedStateHandle
                            ?.set("age", rabbit.age)

                        navController.currentBackStackEntry
                            ?.savedStateHandle
                            ?.set("cage", rabbit.cage)

                        navController.currentBackStackEntry
                            ?.savedStateHandle
                            ?.set("weight", rabbit.weight)

                        navController.currentBackStackEntry
                            ?.savedStateHandle
                            ?.set("diagnosis", rabbit.diagnosis)

                        navController.navigate(Route.RabbitCulling)
                    },
                )
            }
            composable<Route.RabbitProfile> { backStackEntry ->
                val route = backStackEntry.toRoute<Route.RabbitProfile>()
                RabbitProfileScreen(
                    rfidCode = route.code,
                    onBack = { navController.popBackStack() },
                    onWeighing = { code -> navController.navigate(Route.Weighing(code)) },
                    onMoving = { code -> navController.navigate(Route.Moving(code)) },
                    onCulling = { profile ->
                        navController.currentBackStackEntry
                            ?.savedStateHandle
                            ?.set("rfidCode", profile.code)
                        navController.currentBackStackEntry
                            ?.savedStateHandle
                            ?.set("status", profile.status.orEmpty())
                        navController.currentBackStackEntry
                            ?.savedStateHandle
                            ?.set("age", profile.fieldValue("Возраст"))
                        navController.currentBackStackEntry
                            ?.savedStateHandle
                            ?.set("cage", profile.fieldValue("Клетка"))
                        navController.currentBackStackEntry
                            ?.savedStateHandle
                            ?.set("weight", profile.fieldValue("Вес (живая масса)"))
                        navController.currentBackStackEntry
                            ?.savedStateHandle
                            ?.set("diagnosis", profile.fieldValue("Диагноз"))

                        navController.navigate(Route.RabbitCulling)
                    },
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

                val savedStateHandle =
                    navController.previousBackStackEntry?.savedStateHandle

                val rabbit = Rabbit(
                    rfidCode = savedStateHandle?.get<String>("rfidCode").orEmpty(),
                    status = savedStateHandle?.get<String>("status").orEmpty(),
                    age = savedStateHandle?.get<String>("age").orEmpty(),
                    cage = savedStateHandle?.get<String>("cage").orEmpty(),
                    weight = savedStateHandle?.get<String>("weight").orEmpty(),
                    diagnosis = savedStateHandle?.get<String>("diagnosis").orEmpty(),
                )

                RabbitCullingScreen(
                    rabbit = rabbit,
                    onBack = { navController.popBackStack() },
                    onScanAgain = {
                        navController.navigate(Route.RfidScan)
                    }
                )
            }
        }
    }
}

private fun ru.profikrol.operator.feature.rabbitprofile.RabbitProfileUiModel.fieldValue(
    label: String,
): String = sections
    .firstNotNullOfOrNull { section -> section.fields[label] }
    .orEmpty()
