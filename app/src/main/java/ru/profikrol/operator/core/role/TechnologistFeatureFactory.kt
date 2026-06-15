package ru.profikrol.operator.core.role

import ru.profikrol.operator.feature.home.HomeActionId
import ru.profikrol.operator.feature.rfidscanresult.RabbitActionId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Набор операций, доступных технологу.
 *
 * Технолог не выполняет физических работ — у него на главном
 * только сканирование RFID (оно вне фабрики, доступно всем).
 * Из действий над кроликом доступно только осеменение.
 */
@Singleton
class TechnologistFeatureFactory @Inject constructor() : RoleFeatureFactory {

    override fun homeActions(): List<HomeActionId> = emptyList()

    override fun rabbitScanActions(): List<RabbitActionId> = listOf(
        RabbitActionId.Insemination,
    )
}
