package com.rabbitmes.mobile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.rabbitmes.mobile.domain.ChecklistStatus
import com.rabbitmes.mobile.ui.components.BottomNav
import com.rabbitmes.mobile.ui.operations.OperationScreenFactory
import com.rabbitmes.mobile.ui.screens.AcceptanceQueueScreen
import com.rabbitmes.mobile.ui.screens.NotificationsScreen
import com.rabbitmes.mobile.ui.screens.ProfileScreen
import com.rabbitmes.mobile.shift.ui.PickHangarScreen
import com.rabbitmes.mobile.shift.ui.ShiftHomeScreen
import com.rabbitmes.mobile.shift.ui.ShiftHomeViewModel
import com.rabbitmes.mobile.shift.ui.ShiftUiState
import com.rabbitmes.mobile.ui.screens.SyncQueueScreen
import com.rabbitmes.mobile.ui.screens.TaskListScreen
import com.rabbitmes.mobile.ui.viewmodel.AppViewModel
import com.rabbitmes.mobile.ui.viewmodel.NotificationsViewModel
import com.rabbitmes.mobile.ui.viewmodel.ProfileViewModel
import com.rabbitmes.mobile.ui.viewmodel.SyncViewModel
import com.rabbitmes.mobile.ui.viewmodel.TaskExecutionViewModel
import com.rabbitmes.mobile.ui.viewmodel.TaskListViewModel
import ru.profikrol.operator.feature.auth.AuthScreen
import ru.profikrol.operator.feature.rabbitprofile.RabbitProfileScreen

@Composable
fun RabbitMesApp(appViewModel: AppViewModel) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }
    val isLoggedIn by appViewModel.isLoggedIn.collectAsStateWithLifecycle()
    val appError by appViewModel.error.collectAsStateWithLifecycle()
    val isBusy by appViewModel.busy.collectAsStateWithLifecycle()

    LaunchedEffect(appError?.id) {
        val error = appError ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(error.message)
        appViewModel.consumeError(error.id)
    }

    DisposableEffect(lifecycleOwner, appViewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> appViewModel.onAppStarted()
                Lifecycle.Event.ON_STOP -> appViewModel.onAppStopped()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { _ ->
        Box(Modifier.fillMaxSize()) {
            if (isLoggedIn) {
                MainScreen(appViewModel)
            } else {
                AuthScreen(onLoggedIn = appViewModel::onLoggedIn)
            }
            if (isBusy) BusyOverlay()
        }
    }
}

/**
 * Главный экран после входа: нижняя навигация в одном [Scaffold] и все маршруты внутри.
 * Панель показывается только на вкладках, на экранах задачи и профиля кролика её нет.
 */
@Composable
private fun MainScreen(appViewModel: AppViewModel) {
    val navController = rememberNavController()
    val pendingSyncEvents by appViewModel.pendingSyncEvents.collectAsStateWithLifecycle()
    val currentEntry by navController.currentBackStackEntryAsState()
    val currentTab = currentEntry?.destination?.let { destination -> TABS.firstOrNull { destination.hasRoute(it.route::class) } }

    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collect { appViewModel.onDestinationChanged() }
    }
    LaunchedEffect(navController) {
        appViewModel.openNotificationsRequests.collect {
            navController.navigate(AppRoute.Notifications) { launchSingleTop = true }
        }
    }
    val openTask: (String) -> Unit = { taskId -> navController.navigate(AppRoute.TaskExecution(taskId)) }

    Scaffold(
        // Отступы от системных панелей экраны по-прежнему делают сами.
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (currentTab != null) {
                BottomNav(currentTab.key, pendingSyncEvents) { key ->
                    TABS.firstOrNull { it.key == key }?.let { tab -> navController.openTab(tab.route) }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController,
            startDestination = AppRoute.Shift,
            modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding),
        ) {
            composable<AppRoute.Shift> {
                val vm: ShiftHomeViewModel = hiltViewModel()
                ShiftHomeScreen(
                    state = vm.state.collectAsStateWithLifecycle().value,
                    header = vm.header.collectAsStateWithLifecycle().value,
                    onNotificationsClick = { navController.navigate(AppRoute.Notifications) },
                    onStartShift = {
                        vm.startShift()
                        navController.navigate(AppRoute.PickHangar)
                    },
                    onCloseShift = vm::closeShift,
                    onStartTask = { task -> openTask(task.id) },
                )
            }
            composable<AppRoute.PickHangar> {
                // Общая ViewModel с вкладкой «Смена»: выбор ангара — часть её состояния.
                val shiftEntry = remember(it) { navController.getBackStackEntry(AppRoute.Shift) }
                val vm: ShiftHomeViewModel = hiltViewModel(shiftEntry)
                val state = vm.state.collectAsStateWithLifecycle().value
                val header = vm.header.collectAsStateWithLifecycle().value
                BackHandler(onBack = vm::cancelPick)
                // После подтверждения или «назад» состояние уходит из PickHangar — закрываем экран.
                // Пока экран уезжает анимацией, показываем последний выбор, чтобы не мигал пустотой.
                var lastPick by remember { mutableStateOf<ShiftUiState.PickHangar?>(null) }
                if (state is ShiftUiState.PickHangar) lastPick = state
                LaunchedEffect(state) {
                    if (state !is ShiftUiState.PickHangar && lastPick != null) navController.popBackStack()
                }
                lastPick?.let { pick ->
                    PickHangarScreen(
                        state = pick,
                        unreadNotifications = header.unreadNotifications,
                        onBack = vm::cancelPick,
                        onNotificationsClick = { navController.navigate(AppRoute.Notifications) },
                        onSelect = vm::selectHangar,
                        onConfirm = vm::confirmHangar,
                    )
                }
            }
            composable<AppRoute.Tasks> {
                val vm: TaskListViewModel = hiltViewModel()
                TaskListScreen(
                    tasks = vm.tasks.collectAsStateWithLifecycle().value,
                    nextTask = vm.nextTask.collectAsStateWithLifecycle().value,
                    message = vm.message.collectAsStateWithLifecycle().value,
                    shiftStarted = vm.shiftStarted.collectAsStateWithLifecycle().value,
                    onOpen = openTask,
                    onBack = { navController.openTab(AppRoute.Shift) },
                )
            }
            composable<AppRoute.Sync> {
                val vm: SyncViewModel = hiltViewModel()
                SyncQueueScreen(
                    shift = vm.shift.collectAsStateWithLifecycle().value,
                    tasks = vm.tasks.collectAsStateWithLifecycle().value,
                    onSync = vm::syncNow,
                    onBack = { navController.openTab(AppRoute.Tasks) },
                )
            }
            composable<AppRoute.Profile> {
                val vm: ProfileViewModel = hiltViewModel()
                ProfileScreen(
                    employee = vm.employee.collectAsStateWithLifecycle().value,
                    tasks = vm.tasks.collectAsStateWithLifecycle().value,
                    operations = vm.operations,
                    onLogout = appViewModel::logout,
                )
            }
            composable<AppRoute.Notifications> {
                val vm: NotificationsViewModel = hiltViewModel()
                NotificationsScreen(
                    notifications = vm.notifications.collectAsStateWithLifecycle().value,
                    onBack = { navController.popBackStack() },
                    onRead = vm::markAsRead,
                    onReadAll = vm::markAllAsRead,
                )
            }
            composable<AppRoute.AcceptanceQueue> {
                AcceptanceQueueScreen(onBack = { navController.openTab(AppRoute.Tasks) })
            }
            composable<AppRoute.TaskExecution> {
                TaskExecutionDestination(
                    onClose = { navController.openTab(AppRoute.Tasks) },
                    onOpenAnimal = { rfid, taskId -> navController.navigate(AppRoute.RabbitProfile(rfid, taskId)) },
                )
            }
            composable<AppRoute.RabbitProfile> { entry ->
                val route = entry.toRoute<AppRoute.RabbitProfile>()
                // Взвешивание, перемещение и выбраковка из профиля пока не подключены: возвращаемся к задаче.
                RabbitProfileScreen(
                    rfidCode = route.rfidCode,
                    onBack = { navController.popBackStack() },
                    onWeighing = { navController.popBackStack() },
                    onMoving = { navController.popBackStack() },
                    onCulling = { navController.popBackStack() },
                )
            }
        }
    }
}

private data class Tab(val key: String, val route: AppRoute)

private val TABS = listOf(
    Tab("shift", AppRoute.Shift),
    Tab("tasks", AppRoute.Tasks),
    Tab("accept", AppRoute.AcceptanceQueue),
    Tab("sync", AppRoute.Sync),
    Tab("profile", AppRoute.Profile),
)

@Composable
private fun TaskExecutionDestination(
    onClose: () -> Unit,
    onOpenAnimal: (rfid: String, taskId: String) -> Unit,
) {
    val vm: TaskExecutionViewModel = hiltViewModel()
    val task = vm.task.collectAsStateWithLifecycle().value
    val scannedRfid by vm.scannedRfid.collectAsStateWithLifecycle()
    val scannedValues by vm.scannedValues.collectAsStateWithLifecycle()
    val hasPendingSync by vm.hasPendingSync.collectAsStateWithLifecycle()

    DisposableEffect(vm) { onDispose { vm.stopRfidScan() } }
    if (task == null) {
        LaunchedEffect(vm.taskId) { onClose() }
        return
    }
    OperationScreenFactory(
        task = task,
        definition = vm.definition(task.operationType),
        scannedRfid = scannedRfid,
        scannedValues = scannedValues,
        onBack = onClose,
        onBegin = vm::begin,
        onScan = vm::submitScan,
        onOpenRfidScanner = vm::startRfidScan,
        onValue = vm::updateValue,
        onPhoto = vm::addPhoto,
        onVideo = vm::addVideo,
        onFile = vm::addFile,
        onComment = vm::addComment,
        onChecklistDone = { itemId -> vm.markChecklistItem(itemId, ChecklistStatus.DONE, "", "Выполнено вручную") },
        onChecklistDoneWithValues = vm::completeChecklistItem,
        onChecklistProblem = { itemId, reason, comment -> vm.markChecklistItem(itemId, ChecklistStatus.PROBLEM, reason, comment) },
        onChecklistSkip = { itemId, reason -> vm.markChecklistItem(itemId, ChecklistStatus.SKIPPED, reason, "Пропущено") },
        onMortalityRoundProblem = vm::addMortalityRoundProblem,
        onComplete = {
            vm.complete()
            onClose()
        },
        onSkip = { reason ->
            vm.reject(reason)
            onClose()
        },
        onGeneralComplete = { comment ->
            vm.complete(comment)
            onClose()
        },
        onGeneralReject = { reason, comment ->
            vm.reject(reason, comment)
            onClose()
        },
        onOpenAnimal = { rfid -> onOpenAnimal(rfid, task.id) },
        resolveRabbitId = vm::rabbitIdForRfid,
        canEdit = vm.canEdit(task),
        hasPendingSync = hasPendingSync,
    )
}

@Composable
private fun BusyOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.18f))
            .clickable(onClick = {}),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(44.dp),
            color = Color(0xFF16794B),
            strokeWidth = 4.dp,
        )
    }
}

/** Вкладки нижнего меню: одна копия экрана, «Смена» всегда в основании стека. */
private fun NavHostController.openTab(route: AppRoute) {
    navigate(route) {
        popUpTo<AppRoute.Shift>()
        launchSingleTop = true
    }
}
