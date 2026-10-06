package com.rabbitmes.mobile.session

import com.rabbitmes.mobile.data.mapper.toShiftState
import com.rabbitmes.mobile.domain.ShiftState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import ru.profikrol.operator.data.local.offline.OfflineRepository
import ru.profikrol.operator.data.remote.profile.ProfileApi
import ru.profikrol.operator.data.remote.profile.ProfileDto
import ru.profikrol.operator.data.remote.profile.ProfileManufactureDto
import ru.profikrol.operator.data.remote.profile.ShiftApi
import ru.profikrol.operator.data.remote.profile.StartShiftRequest
import retrofit2.HttpException
import com.rabbitmes.mobile.core.DeviceInfo
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Состояние смены. Сюда же входят признак сети и число неотправленных
 * действий: так исторически устроен [ShiftState], и он же кэшируется офлайн.
 */
@Singleton
class ShiftRepository @Inject constructor(
    private val profileApi: ProfileApi,
    private val shiftApi: ShiftApi,
    private val deviceInfo: DeviceInfo,
    private val offlineRepository: OfflineRepository,
    private val employeeSession: EmployeeSession,
) {
    private val _shift = MutableStateFlow(ShiftState(employeeSession.id))
    val shift: StateFlow<ShiftState> = _shift.asStateFlow()
    val current: ShiftState get() = _shift.value
    val isOnline: Boolean get() = _shift.value.isOnline

    fun reset() {
        _shift.value = ShiftState(employeeSession.id)
    }

    suspend fun restoreCached() {
        offlineRepository.restoreShift(employeeSession.id)?.let { cached ->
            _shift.value = cached.copy(isOnline = _shift.value.isOnline)
        }
    }

    fun setOnline(isOnline: Boolean) {
        _shift.update { it.copy(isOnline = isOnline) }
    }

    fun setPendingSyncEvents(count: Int) {
        _shift.update { it.copy(pendingSyncEvents = count) }
    }

    suspend fun persist() {
        offlineRepository.saveShift(employeeSession.id, _shift.value)
    }

    /** Начинает смену в ангаре (ProductionProgram) и перечитывает её из профиля. */
    suspend fun open(hangarId: Long): ShiftState {
        shiftApi.startShift(StartShiftRequest(hangarId, deviceInfo.deviceId))
        refreshFromProfile()
        return _shift.value
    }

    /** Закрывает активную смену. 404 и 409 — открытой смены уже нет. */
    suspend fun close(): ShiftState {
        val response = shiftApi.closeShift()
        if (!response.isSuccessful && response.code() != 404 && response.code() != 409) throw HttpException(response)
        refreshFromProfile()
        return _shift.value
    }

    private val _manufactures = MutableStateFlow<List<ProfileManufactureDto>>(emptyList())

    /** Цеха сотрудника с ангарами из последнего профиля. */
    val manufactures: StateFlow<List<ProfileManufactureDto>> = _manufactures.asStateFlow()

    /** Обновляет id сотрудника, цеха и смену из профиля. */
    suspend fun refreshFromProfile(): ProfileDto {
        val profile = profileApi.getMyProfile()
        employeeSession.updateId(profile.employeeId)
        _manufactures.value = profile.manufactures
        apply(profile.shift.toShiftState(employeeSession.id, _shift.value))
        return profile
    }

    private fun apply(state: ShiftState): ShiftState {
        _shift.value = state
        return state
    }
}
