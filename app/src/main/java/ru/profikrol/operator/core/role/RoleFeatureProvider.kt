package ru.profikrol.operator.core.role

import ru.profikrol.operator.data.local.SessionStore
import ru.profikrol.operator.domain.model.UserRole
import ru.profikrol.operator.feature.home.HomeActionId
import ru.profikrol.operator.feature.rfidscanresult.RabbitActionId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Диспетчер фабрик ролей. Выбирает реализацию [RoleFeatureFactory]
 * по текущей сессии и подсовывает её клиентам (ViewModel'ям).
 *
 * Hilt не умеет выбирать @Binds в рантайме — для этого и нужен этот класс.
 *
 * Добавление новой роли:
 *  1. Новый case в enum UserRole.
 *  2. Новая реализация XxxFeatureFactory.
 *  3. Новая ветка в when ниже.
 *  Всё. VM/Screen не трогаются.
 */
@Singleton
class RoleFeatureProvider @Inject constructor(
    private val sessionStore: SessionStore,
    private val operator: OperatorFeatureFactory,
    private val technologist: TechnologistFeatureFactory,
) {
    fun current(): RoleFeatureFactory = when (sessionStore.currentUser?.role) {
        UserRole.Operator -> operator
        UserRole.Technologist -> technologist
        null -> EmptyFeatureFactory
    }
}

/** Фолбэк для разлогиненного состояния. */
private object EmptyFeatureFactory : RoleFeatureFactory {
    override fun homeActions(): List<HomeActionId> = emptyList()
    override fun rabbitScanActions(): List<RabbitActionId> = emptyList()
}
