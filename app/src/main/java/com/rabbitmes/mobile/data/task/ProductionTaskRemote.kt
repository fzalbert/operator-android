package com.rabbitmes.mobile.data.task

import android.content.Context
import android.provider.Settings
import com.rabbitmes.mobile.core.runCatchingCancellable
import com.rabbitmes.mobile.data.mapper.toMobileTask
import com.rabbitmes.mobile.domain.MobileTask
import com.rabbitmes.mobile.session.EmployeeSession
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.JsonObject
import retrofit2.HttpException
import ru.profikrol.operator.data.remote.production.AddProductionTargetRequest
import ru.profikrol.operator.data.remote.production.CompleteTargetRequest
import ru.profikrol.operator.data.remote.production.MortalityCountResult
import ru.profikrol.operator.data.remote.production.ProductionMortalityCountProblemRequest
import ru.profikrol.operator.data.remote.production.ProductionTargetCommentProblemRequest
import ru.profikrol.operator.data.remote.production.ProductionTaskApi
import ru.profikrol.operator.data.remote.production.ProductionTaskDetailsDto
import ru.profikrol.operator.data.remote.production.ProductionTaskDto
import ru.profikrol.operator.data.remote.production.SubmitProductionTaskResultRequest
import javax.inject.Inject
import javax.inject.Singleton

/** Production API (через Gateway) от лица текущего сотрудника. */
@Singleton
class ProductionTaskRemote @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: ProductionTaskApi,
    private val employeeSession: EmployeeSession,
) {
    private val employeeId: String get() = employeeSession.id
    private val deviceId: String by lazy {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            .orEmpty()
            .ifBlank { android.os.Build.MODEL }
    }

    suspend fun employeeTasks(): List<ProductionTaskDto> = call { it.getEmployeeTasks(employeeId, employeeId) }

    suspend fun details(taskId: String): ProductionTaskDetailsDto = call { it.getTask(employeeId, taskId) }

    suspend fun task(taskId: String): MobileTask = details(taskId).toMobileTask(employeeId)

    suspend fun taskOrNull(taskId: String): MobileTask? = runCatchingCancellable { task(taskId) }.getOrNull()

    /** Старт задачи. 409 значит, что задача уже открыта. */
    suspend fun start(taskId: String) {
        val response = call { it.startTask(employeeId, taskId) }
        if (!response.isSuccessful && response.code() != 409) throw HttpException(response)
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

    private suspend fun <T> call(action: suspend (ProductionTaskApi) -> T): T = action(api)
}
