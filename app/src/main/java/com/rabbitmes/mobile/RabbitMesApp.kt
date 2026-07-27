package com.rabbitmes.mobile

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
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
    val currentScreen = vm.screen
    BackHandler(enabled = currentScreen != AppScreen.Login) {
        when (currentScreen) {
            AppScreen.Login -> Unit
            AppScreen.Shift -> Unit
            AppScreen.Tasks,
            AppScreen.Map,
            AppScreen.Sync,
            AppScreen.Profile,
            AppScreen.Notifications -> vm.navigate(AppScreen.Shift)
            AppScreen.AcceptanceQueue -> vm.navigate(AppScreen.Tasks)
            is AppScreen.TaskExecution -> vm.navigate(AppScreen.Tasks)
            is AppScreen.RfidScan -> vm.navigate(AppScreen.TaskExecution(currentScreen.taskId))
            is AppScreen.Acceptance -> vm.navigate(AppScreen.AcceptanceQueue)
            is AppScreen.AnimalHistory -> vm.navigate(AppScreen.Tasks)
            is AppScreen.RabbitProfile -> vm.navigate(AppScreen.TaskExecution(currentScreen.taskId))
        }
    }

    fun bottom(current: String): @Composable () -> Unit = { BottomNav(current, vm.shift.pendingSyncEvents) { key ->
        when(key) { "shift" -> vm.navigate(AppScreen.Shift); "tasks" -> vm.navigate(AppScreen.Tasks); "accept" -> vm.navigate(AppScreen.AcceptanceQueue); "sync" -> vm.navigate(AppScreen.Sync); "profile" -> vm.navigate(AppScreen.Profile) }
    } }
    when(val screen = vm.screen) {
        AppScreen.Login -> AuthScreen(onLoggedIn = vm::onLoggedInFromSession)
        AppScreen.Shift -> ShiftScreen(vm.currentEmployee, vm.shift, vm.tasksForCurrentEmployee(), vm.nextTask(), vm.lastMessage, vm.notifications.count { it.isUnread }, vm::startShift, vm::finishShift, { vm.navigate(AppScreen.TaskExecution(it)) }, { vm.navigate(AppScreen.Notifications) }, vm::logout, bottom("shift"))
        AppScreen.Tasks -> TaskListScreen(vm.tasksForCurrentEmployee(), vm.nextTask(), vm.lastMessage, vm.shift.startedAt != null, { vm.navigate(AppScreen.TaskExecution(it)) }, { vm.navigate(AppScreen.Shift) }, bottom("tasks"))
        AppScreen.Map -> HangarMapScreen(vm.workshop, vm.tasksForCurrentEmployee(), { vm.navigate(AppScreen.TaskExecution(it)) }, { vm.navigate(AppScreen.Tasks) }, bottom("map"))
        AppScreen.Sync -> SyncQueueScreen(vm.shift, vm.tasks, vm::syncNow, { vm.navigate(AppScreen.Tasks) }, bottom("sync"))
        AppScreen.Profile -> ProfileScreen(vm.currentEmployee, vm.tasksForCurrentEmployee(), vm.operations, vm::logout, bottom("profile"))
        AppScreen.Notifications -> NotificationsScreen(vm.notifications, { vm.navigate(AppScreen.Shift) }, vm::markNotificationAsRead, vm::markAllNotificationsAsRead)
        AppScreen.AcceptanceQueue -> AcceptanceQueueScreen(vm.tasksForAcceptance(), { vm.navigate(AppScreen.Acceptance(it)) }, { vm.navigate(AppScreen.Tasks) }, bottom("accept"))
        is AppScreen.TaskExecution -> {
            val task = vm.task(screen.taskId)
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
            val task = vm.task(screen.taskId)
            AcceptanceScreen(task, vm.remarks, { vm.navigate(AppScreen.AcceptanceQueue) }, { vm.acceptTask(task.id, it); vm.navigate(AppScreen.AcceptanceQueue) }, { vm.rejectTask(task.id, it); vm.navigate(AppScreen.AcceptanceQueue) }, { itemId, reason, comment, attachments -> vm.addRemark(task.id, itemId, reason, comment, attachments) })
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
