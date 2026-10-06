package com.rabbitmes.mobile.data.task

import com.rabbitmes.mobile.core.DeviceInfo
import com.rabbitmes.mobile.core.runCatchingCancellable
import com.rabbitmes.mobile.data.mapper.toMobileTask
import com.rabbitmes.mobile.domain.MobileTask
import com.rabbitmes.mobile.domain.TaskStatus
import com.rabbitmes.mobile.session.EmployeeSession
import kotlinx.serialization.json.JsonObject
import retrofit2.HttpException
import ru.profikrol.operator.data.remote.production.AddProductionTargetRequest
import ru.profikrol.operator.data.remote.production.CompleteTargetRequest
import ru.profikrol.operator.data.remote.production.MortalityCountResult
import ru.profikrol.operator.data.remote.production.ProductionMortalityCountProblemRequest
import ru.profikrol.operator.data.remote.production.ProductionTargetCommentProblemRequest
import ru.profikrol.operator.data.remote.production.ProductionTaskProblemRequest
import ru.profikrol.operator.data.remote.production.ProductionTaskApi
import ru.profikrol.operator.data.remote.production.ProductionTaskDetailsDto
import ru.profikrol.operator.data.remote.production.ProductionTaskDto
import ru.profikrol.operator.data.remote.production.SubmitProductionTaskResultRequest
import javax.inject.Inject
import javax.inject.Singleton

/** Production API (через Gateway) от лица текущего сотрудника. */
@Singleton
class ProductionTaskRemote @Inject constructor(
    private val api: ProductionTaskApi,
    private val employeeSession: EmployeeSession,
    private val deviceInfo: DeviceInfo,
) {
    private val employeeId: String get() = employeeSession.id
    private val deviceId: String get() = deviceInfo.deviceId

    /** Задачи активной смены. 404 — открытой смены нет, значит и задач нет. */
    suspend fun shiftTasks(): List<ProductionTaskDto> {
        val response = call { it.getShiftTasks(employeeId) }
        if (response.code() == 404) return emptyList()
        if (!response.isSuccessful) throw HttpException(response)
        return response.body().orEmpty()
    }

    suspend fun details(taskId: String): ProductionTaskDetailsDto = call { it.getTask(employeeId, taskId) }

    suspend fun task(taskId: String): MobileTask = details(taskId).toMobileTask(employeeId)

    suspend fun taskOrNull(taskId: String): MobileTask? = runCatchingCancellable { task(taskId) }.getOrNull()

    /**
     * Старт задачи: сотрудник становится её исполнителем. 409 приходит и когда задача
     * уже в работе, и когда её нельзя начать (например, у задачи нет ангара), поэтому
     * успехом он считается, только если сервер показывает задачу начатой.
     */
    suspend fun start(taskId: String) {
        val response = call { it.startTask(employeeId, taskId) }
        if (response.isSuccessful) return
        val error = HttpException(response)
        if (response.code() == 409 && task(taskId).status in STARTED_STATUSES) return
        throw error
    }

    suspend fun completeChecklistItem(taskId: String, itemId: String) =
        call { it.completeChecklistItem(employeeId, taskId, itemId) }

    suspend fun completeTarget(taskId: String, targetId: String, result: JsonObject, rfid: String? = null) =
        call {
            it.completeTarget(
                employeeId,
                taskId,
                targetId,
                CompleteTargetRequest(result = result, rfid = rfid, deviceId = deviceId),
            )
        }

    suspend fun reportTargetProblem(taskId: String, targetId: String, comment: String) =
        call { it.reportTargetCommentProblem(employeeId, taskId, targetId, ProductionTargetCommentProblemRequest(comment)) }

    suspend fun reportMortalityCount(taskId: String, targetId: String, count: Int) =
        call {
            it.reportMortalityCountProblem(
                employeeId,
                taskId,
                targetId,
                ProductionMortalityCountProblemRequest(result = MortalityCountResult(count)),
            )
        }

    /** Добавляет цель в задачу и возвращает её id из ответа сервера. */
    suspend fun addTarget(taskId: String, request: AddProductionTargetRequest): String =
        call { it.addTarget(employeeId, taskId, request) }.string().trim().trim('"')

    suspend fun submitResult(taskId: String, resultJson: String) =
        call { it.submitTaskResult(employeeId, taskId, SubmitProductionTaskResultRequest(resultJson)) }

    suspend fun complete(taskId: String) = call { it.completeTask(employeeId, taskId) }

    suspend fun reportTaskProblem(taskId: String, comment: String) =
        call { it.reportTaskProblem(employeeId, taskId, ProductionTaskProblemRequest(comment)) }

    private suspend fun <T> call(action: suspend (ProductionTaskApi) -> T): T = action(api)

    private companion object {
        val STARTED_STATUSES = setOf(TaskStatus.IN_PROGRESS, TaskStatus.DONE)
    }
}
