package com.rabbitmes.mobile.data.task

import android.util.Log
import com.rabbitmes.mobile.core.UserMessages
import com.rabbitmes.mobile.core.runCatchingCancellable
import com.rabbitmes.mobile.core.toHttpDebugMessage
import com.rabbitmes.mobile.data.mapper.isOpen
import com.rabbitmes.mobile.data.reference.FarmReferenceRepository
import com.rabbitmes.mobile.domain.ChecklistItem
import com.rabbitmes.mobile.domain.ChecklistStatus
import com.rabbitmes.mobile.domain.MobileTask
import com.rabbitmes.mobile.domain.OperationType
import com.rabbitmes.mobile.domain.TargetType
import com.rabbitmes.mobile.domain.TaskStatus
import com.rabbitmes.mobile.domain.orderedOpenTasks
import com.rabbitmes.mobile.domain.withSingleInProgressTask
import com.rabbitmes.mobile.session.EmployeeSession
import com.rabbitmes.mobile.session.ShiftRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import ru.profikrol.operator.data.local.offline.OfflineActionType
import ru.profikrol.operator.data.local.offline.OfflineRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Единственный источник production-задач (задач цикла) текущей смены сотрудника.
 *
 * Держит список в памяти, кэширует его офлайн и периодически обновляет с сервера.
 * Локальные изменения, которые сервер ещё не вернул (выполненные цели,
 * события обхода), накладываются поверх серверных данных при каждой загрузке.
 */
@Singleton
class ProductionTaskRepository @Inject constructor(
    private val remote: ProductionTaskRemote,
    private val references: FarmReferenceRepository,
    private val offlineRepository: OfflineRepository,
    private val employeeSession: EmployeeSession,
    private val shiftRepository: ShiftRepository,
    private val messages: UserMessages,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _tasks = MutableStateFlow<List<MobileTask>>(emptyList())
    val tasks: StateFlow<List<MobileTask>> = _tasks.asStateFlow()
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var isRequestInProgress = false
    private var isReloadRequested = false
    private var autoRefreshJob: Job? = null
    private val targetOverrides = mutableMapOf<String, Map<String, ChecklistItem>>()
    private val mortalityRoundEvents = mutableMapOf<String, List<ChecklistItem>>()

    fun task(id: String): MobileTask? = _tasks.value.firstOrNull { it.id == id }

    fun nextTask(tasks: List<MobileTask>): MobileTask? = tasks.orderedOpenTasks().firstOrNull()

    /** Задача, с которой сотрудник должен работать сейчас: первая в очереди. */
    fun nextTask(): MobileTask? = nextTask(_tasks.value)

    fun canWorkOn(taskId: String): Boolean = nextTask()?.id == taskId

    suspend fun load(showLoading: Boolean = true) {
        // Задачи сервер отдаёт по активной смене; без неё запрашивать нечего.
        if (!shiftRepository.current.isOpen()) return
        if (isRequestInProgress) {
            isReloadRequested = true
            return
        }
        isRequestInProgress = true
        if (showLoading) _isLoading.value = true
        try {
            val employeeId = employeeSession.id
            Log.d(TAG, "Loading tasks. employeeId=$employeeId showLoading=$showLoading")
            val productionTasks = runCatchingCancellable { fetchShiftTasks() }
                .getOrElse { error ->
                    messages.handle(
                        error = error,
                        fallbackMessage = "Не удалось загрузить задачи",
                        logMessage = "Production tasks request failed: ${error.toHttpDebugMessage()}",
                        showToUser = showLoading,
                    )
                    return
                }
            val cells = loadReferences(productionTasks, showLoading)
            val withCells = productionTasks.map { task ->
                task.copy(
                    checklist = task.checklist.map { item ->
                        val cell = item.cageId?.toLongOrNull()?.let { cageId -> cells.firstOrNull { it.id == cageId } }
                        if (cell == null) item else item.copy(cageLabel = cell.displayName, rabbitCount = cell.meatRabbitsCount)
                    },
                )
            }
            // Старт, который ещё лежит в очереди, сервер не видит: такая задача остаётся в работе.
            // Отклонённый сервером старт из очереди уже убран, и задача возвращается в серверное состояние.
            val pendingStarts = offlineRepository.actions(employeeId)
                .filter { it.type == OfflineActionType.START_PRODUCTION_TASK.name }
                .mapTo(mutableSetOf()) { it.taskId }
            _tasks.value = withCells
                .map { it.withLocalChanges() }
                .distinctBy(MobileTask::id)
                .map { remote ->
                    if (remote.id in pendingStarts && remote.status == TaskStatus.NEW) {
                        remote.copy(status = TaskStatus.IN_PROGRESS)
                    } else {
                        remote
                    }
                }
                .withSingleInProgressTask()
            // Успешный ответ сервера надёжнее флага сети эмулятора, который на ферме бывает false.
            shiftRepository.setOnline(true)
            persist()
            Log.d(TAG, "Tasks visible=${_tasks.value.size}")
        } finally {
            isRequestInProgress = false
            if (showLoading) _isLoading.value = false
            if (isReloadRequested) {
                isReloadRequested = false
                load(showLoading = false)
            }
        }
    }

    /** Перечитывает одну задачу с сервера и заменяет её в списке. */
    suspend fun refresh(taskId: String): MobileTask? {
        val synced = remote.taskOrNull(taskId)?.withLocalChanges() ?: return null
        replace(synced)
        return synced
    }

    fun replace(task: MobileTask) {
        _tasks.value = _tasks.value.map { if (it.id == task.id) task else it }
        persist()
    }

    /** Локальное изменение задачи. В офлайне задача помечается как изменённая без сети. */
    fun update(taskId: String, transform: (MobileTask) -> MobileTask) {
        val isOnline = shiftRepository.isOnline
        _tasks.value = _tasks.value.map { task ->
            if (task.id == taskId) {
                val updated = transform(task)
                if (isOnline) updated.copy(offlineEvents = task.offlineEvents) else updated.markOffline()
            } else {
                task
            }
        }
        persist()
    }

    fun clearOfflineMarks() {
        _tasks.value = _tasks.value.map { it.copy(offlineEvents = 0) }
        persist()
    }

    /** Помнит выполненную цель, пока сервер не начнёт отдавать её выполненной. */
    fun rememberTargetResult(taskId: String, item: ChecklistItem) {
        if (item.serverType != SERVER_TYPE_TARGET) return
        targetOverrides[taskId] = targetOverrides[taskId].orEmpty() + (item.id to item)
    }

    fun rememberMortalityRoundEvent(taskId: String, event: ChecklistItem) {
        mortalityRoundEvents[taskId] = mortalityRoundEvents[taskId].orEmpty() + event
    }

    fun clearTasks() {
        _tasks.value = emptyList()
        persist()
    }

    suspend fun restoreCached() {
        val cached = offlineRepository.restoreTasks(employeeSession.id)
        if (cached.isNotEmpty()) {
            _tasks.value = cached.withSingleInProgressTask()
        }
    }

    fun persist() {
        val employeeId = employeeSession.id
        val snapshot = _tasks.value
        scope.launch {
            offlineRepository.saveTasks(employeeId, snapshot)
            shiftRepository.persist()
        }
    }

    fun startAutoRefresh() {
        if (autoRefreshJob?.isActive == true) return
        autoRefreshJob = scope.launch {
            while (isActive) {
                delay(REFRESH_INTERVAL_MS)
                runCatchingCancellable { load(showLoading = false) }
                    .onFailure { Log.e(TAG, "Tasks auto refresh failed", it) }
            }
        }
    }

    fun stopAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = null
    }

    fun reset() {
        stopAutoRefresh()
        _tasks.value = emptyList()
        targetOverrides.clear()
        mortalityRoundEvents.clear()
        references.clear()
    }

    private suspend fun fetchShiftTasks(): List<MobileTask> {
        val list = remote.shiftTasks()
        Log.d(TAG, "Production getShiftTasks returned ${list.size} items")
        return supervisorScope {
            list
                .filter { task -> task.taskType?.lowercase() !in setOf("automation", "scada") }
                .map { dto ->
                    async {
                        runCatchingCancellable { remote.task(dto.id) }
                            .onFailure { error ->
                                Log.e(TAG, "Production task details failed: ${error.toHttpDebugMessage()}. taskId=${dto.id}", error)
                            }
                            .getOrNull()
                    }
                }
                .awaitAll()
                .filterNotNull()
        }
    }

    private suspend fun loadReferences(tasks: List<MobileTask>, showErrors: Boolean): List<ru.profikrol.operator.data.remote.cell.CellDto> {
        val needsRabbits = tasks.any { task ->
            task.operationType in RABBIT_OPERATIONS || task.checklist.any { it.targetType == TargetType.RABBIT }
        }
        if (needsRabbits) {
            runCatchingCancellable { references.refreshRabbits() }
                .onFailure { error ->
                    messages.handle(error, "Не удалось загрузить список кроликов", "Rabbits request failed", showErrors)
                }
        }
        if (tasks.none { it.operationType in CELL_OPERATIONS }) return emptyList()
        val hangarIds = tasks.mapNotNull { it.hangarId.toLongOrNull() }.toSet()
        val cells = runCatchingCancellable { references.cellsFor(hangarIds) }
            .getOrElse { error ->
                messages.handle(error, "Не удалось загрузить список клеток", "Cells request failed", showErrors)
                emptyList()
            }
        if (hangarIds.isNotEmpty()) {
            runCatchingCancellable { references.refreshRows(hangarIds) }
                .onFailure { error -> Log.w(TAG, "Rows request failed: ${error.toHttpDebugMessage()}", error) }
        }
        return cells
    }

    private fun MobileTask.withLocalChanges(): MobileTask = withTargetOverrides().withMortalityRoundEvents()

    private fun MobileTask.withTargetOverrides(): MobileTask {
        val overrides = targetOverrides[id].orEmpty()
        if (overrides.isEmpty()) return this
        return copy(
            checklist = checklist.map { item ->
                val override = overrides[item.id] ?: return@map item
                if (
                    item.serverType == SERVER_TYPE_TARGET &&
                    item.status == ChecklistStatus.PENDING &&
                    override.status != ChecklistStatus.PENDING
                ) {
                    override
                } else {
                    item
                }
            },
        )
    }

    private fun MobileTask.withMortalityRoundEvents(): MobileTask {
        if (operationType != OperationType.MORTALITY_ROUND) return this
        val events = mortalityRoundEvents[id].orEmpty()
        if (events.isEmpty()) return this
        val visibleServerItems = checklist.filter { item ->
            item.serverType != SERVER_TYPE_TARGET || item.status == ChecklistStatus.PENDING
        }
        return copy(checklist = (visibleServerItems + events).distinctBy { it.id })
    }

    companion object {
        const val SERVER_TYPE_TARGET = "production-target"
        const val SERVER_TYPE_CHECKLIST = "production-checklist"
        private const val TAG = "RabbitApi"
        private const val REFRESH_INTERVAL_MS = 30_000L
        private val RABBIT_OPERATIONS = setOf(
            OperationType.INSEMINATION,
            OperationType.PALPATION,
            OperationType.MORTALITY_ROUND,
        )
        private val CELL_OPERATIONS = setOf(
            OperationType.MORTALITY_ROUND,
            OperationType.ANIMAL_SETTLEMENT,
            OperationType.WEIGHING_CAGE,
            OperationType.WEIGHING_RABBIT,
        )
    }
}
