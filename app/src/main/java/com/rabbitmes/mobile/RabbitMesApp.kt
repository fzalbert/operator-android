package com.rabbitmes.mobile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.rabbitmes.mobile.data.MockRepository
import com.rabbitmes.mobile.domain.ChecklistStatus
import com.rabbitmes.mobile.ui.components.BottomNav
import com.rabbitmes.mobile.ui.operations.OperationScreenFactory
import com.rabbitmes.mobile.ui.screens.*
import ru.profikrol.operator.feature.auth.AuthScreen
import ru.profikrol.operator.feature.rfidscan.RfidScanScreen
import ru.profikrol.operator.feature.rabbitprofile.RabbitProfileScreen

@Composable
fun RabbitMesApp(vm: MobileMesViewModel) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }
    val appError = vm.appError

    LaunchedEffect(appError?.id) {
        val error = appError ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(error.message)
        vm.consumeAppError(error.id)
    }

    DisposableEffect(lifecycleOwner, vm) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> vm.startTasksAutoRefresh()
                Lifecycle.Event.ON_STOP -> vm.stopTasksAutoRefresh()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            vm.startTasksAutoRefresh()
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            vm.stopTasksAutoRefresh()
        }
    }

    fun bottom(current: String): @Composable () -> Unit = { BottomNav(current, vm.shift.pendingSyncEvents) { key ->
        when(key) { "shift" -> vm.navigate(AppScreen.Shift); "tasks" -> vm.navigate(AppScreen.Tasks); "accept" -> vm.navigate(AppScreen.AcceptanceQueue); "sync" -> vm.navigate(AppScreen.Sync); "profile" -> vm.navigate(AppScreen.Profile) }
    } }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { _ ->
        when(val screen = vm.screen) {
            AppScreen.Login -> AuthScreen(onLoggedIn = vm::onLoggedInFromSession)
            AppScreen.Shift -> ShiftScreen(vm.currentEmployee, vm.shift, vm.tasksForCurrentEmployee(), vm.nextTask(), vm.lastMessage, vm.notifications.count { it.isUnread }, vm.isShiftActionInProgress, vm.isTasksLoading, vm::startShift, vm::finishShift, { vm.navigate(AppScreen.TaskExecution(it)) }, { vm.navigate(AppScreen.Notifications) }, vm::logout, bottom("shift"))
            AppScreen.Tasks -> TaskListScreen(vm.tasksForCurrentEmployee(), vm.nextTask(), vm.lastMessage, vm.shift.startedAt != null, { vm.navigate(AppScreen.TaskExecution(it)) }, { vm.navigate(AppScreen.Shift) }, bottom("tasks"))
            AppScreen.Map -> HangarMapScreen(vm.workshop, vm.tasksForCurrentEmployee(), { vm.navigate(AppScreen.TaskExecution(it)) }, { vm.navigate(AppScreen.Tasks) }, bottom("map"))
            AppScreen.Sync -> SyncQueueScreen(vm.shift, vm.tasks, vm::syncNow, { vm.navigate(AppScreen.Tasks) }, bottom("sync"))
            AppScreen.Profile -> ProfileScreen(vm.currentEmployee, vm.tasksForCurrentEmployee(), vm.operations, vm::logout, bottom("profile"))
            AppScreen.Notifications -> NotificationsScreen(vm.notifications, { vm.navigate(AppScreen.Shift) }, vm::markNotificationAsRead, vm::markAllNotificationsAsRead)
            AppScreen.AcceptanceQueue -> AcceptanceQueueScreen(vm.tasksForAcceptance(), { vm.navigate(AppScreen.Acceptance(it)) }, { vm.navigate(AppScreen.Tasks) }, bottom("accept"))
            is AppScreen.TaskExecution -> {
                val task = vm.taskOrNull(screen.taskId)
                if (task == null) {
                    LaunchedEffect(screen.taskId) { vm.navigate(AppScreen.Tasks) }
                    TaskListScreen(vm.tasksForCurrentEmployee(), vm.nextTask(), vm.lastMessage, vm.shift.startedAt != null, { vm.navigate(AppScreen.TaskExecution(it)) }, { vm.navigate(AppScreen.Shift) }, bottom("tasks"))
                } else {
                    val canEdit = vm.nextTask()?.id == task.id
                    OperationScreenFactory(
                    task = task,
                    definition = vm.definition(task.operationType),
                    scannedRfid = vm.scannedRfidForTask(task.id),
                    onBack = { vm.navigate(AppScreen.Tasks) },
                    onBegin = { vm.beginTask(task.id) },
                    onScan = { rfid, values -> vm.scanRfidAndCompleteItem(task.id, rfid, values) },
                    onOpenRfidScanner = { values ->
                        values.forEach { (key, value) -> vm.updateTaskValue(task.id, key, value) }
                        vm.navigate(AppScreen.RfidScan(task.id, values))
                    },
                    onValue = { key, value -> vm.updateTaskValue(task.id, key, value) },
                    onPhoto = { name, uri -> vm.addPhoto(task.id, name, uri) },
                    onVideo = { name, uri -> vm.addVideo(task.id, name, uri) },
                    onFile = { name, uri -> vm.addFile(task.id, name, uri) },
                    onComment = { vm.addComment(task.id, it) },
                    onChecklistDone = { itemId -> vm.markChecklistItem(task.id, itemId, ChecklistStatus.DONE, "", "Выполнено вручную") },
                    onChecklistDoneWithValues = { itemId, values -> vm.completeChecklistItem(task.id, itemId, values) },
                    onChecklistProblem = { itemId, reason, comment -> vm.markChecklistItem(task.id, itemId, ChecklistStatus.PROBLEM, reason, comment) },
                    onChecklistSkip = { itemId, reason -> vm.markChecklistItem(task.id, itemId, ChecklistStatus.SKIPPED, reason, "Пропущено") },
                    onComplete = { vm.completeTask(task.id); vm.navigate(AppScreen.Tasks) },
                    onSkip = { vm.skipTask(task.id, it); vm.navigate(AppScreen.Tasks) },
                    onOpenAnimal = { rfid -> vm.navigate(AppScreen.RabbitProfile(rfid, task.id)) },
                        canEdit = canEdit,
                    )
                }
            }
            is AppScreen.RfidScan -> {
                RfidScanScreen(
                    onBack = {
                        vm.navigate(AppScreen.TaskExecution(screen.taskId))
                    },
                    onScanned = { code ->
                        vm.rememberScannedRfid(screen.taskId, code)
                        vm.navigate(AppScreen.TaskExecution(screen.taskId))
                    },
                    demoRfidCode = vm.nextPendingRfid(screen.taskId),
                )
            }
            is AppScreen.Acceptance -> {
                val task = vm.taskOrNull(screen.taskId)
                if (task == null) {
                    LaunchedEffect(screen.taskId) { vm.navigate(AppScreen.AcceptanceQueue) }
                    AcceptanceQueueScreen(vm.tasksForAcceptance(), { vm.navigate(AppScreen.Acceptance(it)) }, { vm.navigate(AppScreen.Tasks) }, bottom("accept"))
                } else {
                    AcceptanceScreen(task, vm.remarks, { vm.navigate(AppScreen.AcceptanceQueue) }, { vm.acceptTask(task.id, it); vm.navigate(AppScreen.AcceptanceQueue) }, { vm.rejectTask(task.id, it); vm.navigate(AppScreen.AcceptanceQueue) }, { itemId, reason, comment, attachments -> vm.addRemark(task.id, itemId, reason, comment, attachments) })
                }
            }
            is AppScreen.AnimalHistory -> {
                val rabbit = vm.rabbits.firstOrNull { it.id == screen.rabbitId } ?: vm.rabbits.first()
                AnimalHistoryScreen(rabbit, MockRepository.cage(rabbit.cageId), { vm.navigate(AppScreen.Tasks) })
            }
            is AppScreen.RabbitProfile -> RabbitProfileScreen(
                rfidCode = screen.rfidCode,
                onBack = { vm.navigate(AppScreen.TaskExecution(screen.taskId)) },
                onWeighing = { vm.navigate(AppScreen.TaskExecution(screen.taskId)) },
                onMoving = { vm.navigate(AppScreen.TaskExecution(screen.taskId)) },
                onCulling = { vm.navigate(AppScreen.TaskExecution(screen.taskId)) },
            )
        }
    }
}
