package com.rabbitmes.mobile.session

import com.rabbitmes.mobile.data.MockRepository
import com.rabbitmes.mobile.domain.Employee
import com.rabbitmes.mobile.domain.RoleId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import ru.profikrol.operator.data.local.SessionStore
import ru.profikrol.operator.domain.model.UserRole
import javax.inject.Inject
import javax.inject.Singleton

/** Текущий сотрудник. Его id уходит в заголовок X-Employee-Id всех production-запросов. */
@Singleton
class EmployeeSession @Inject constructor(
    private val sessionStore: SessionStore,
) {
    private val _employee = MutableStateFlow(MockRepository.employees.first())
    val employee: StateFlow<Employee> = _employee.asStateFlow()
    val current: Employee get() = _employee.value
    val id: String get() = _employee.value.id

    /**
     * Собирает сотрудника из сессии. Роль, цеха и подсказки пока берутся из
     * шаблонного сотрудника [MockRepository] с той же ролью.
     */
    fun startFromSession(): Employee {
        val sessionUser = sessionStore.currentUser
        val template = when (sessionUser?.role) {
            UserRole.Technologist,
            UserRole.SuperAdmin -> MockRepository.employees.first { it.role == RoleId.CHIEF_TECHNOLOGIST }
            UserRole.Operator, null -> MockRepository.employees.first { it.role == RoleId.OPERATOR }
        }
        val displayName = sessionUser?.displayName.orEmpty().ifBlank { template.fullName }
        val initials = displayName
            .split(" ")
            .filter(String::isNotBlank)
            .take(2)
            .mapNotNull { it.firstOrNull()?.uppercase() }
            .joinToString("")
            .ifBlank { template.initials }
        val employee = template.copy(
            id = sessionUser?.id ?: template.id,
            fullName = displayName,
            initials = initials,
        )
        _employee.value = employee
        return employee
    }

    fun updateId(employeeId: String) {
        _employee.update { it.copy(id = employeeId) }
    }
}
