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
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Состояние смены. Сюда же входят признак сети и число неотправленных
 * действий: так исторически устроен [ShiftState], и он же кэшируется офлайн.
 */
@Singleton
class ShiftRepository @Inject constructor(
    private val profileApi: ProfileApi,
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

    suspend fun open(): ShiftState = apply(profileApi.openShift().toShiftState(employeeSession.id, _shift.value))

    suspend fun close(): ShiftState = apply(profileApi.closeShift().toShiftState(employeeSession.id, _shift.value))

    /** Обновляет id сотрудника и смену из профиля. */
    suspend fun refreshFromProfile(): ProfileDto {
        val profile = profileApi.getMyProfile()
        employeeSession.updateId(profile.employeeId)
        apply(profile.shift.toShiftState(employeeSession.id, _shift.value))
        return profile
    }

    private fun apply(state: ShiftState): ShiftState {
        _shift.value = state
        return state
    }
}
