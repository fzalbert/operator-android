package ru.profikrol.operator.core.role

import ru.profikrol.operator.feature.home.HomeActionId
import ru.profikrol.operator.feature.rfidscanresult.RabbitActionId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Набор операций, доступных оператору фермы.
 *
 * Оператор отвечает за физические работы с гнёздами и кроликами:
 * подготовка, выравнивание, маркировка, выбраковка.
 * Осеменение — компетенция технолога, оператору не доступно.
 */
@Singleton
class OperatorFeatureFactory @Inject constructor() : RoleFeatureFactory {

    override fun homeActions(): List<HomeActionId> = listOf(
        HomeActionId.NestPreparation,
        HomeActionId.NestAlignment,
        HomeActionId.RfidInstallation,
        HomeActionId.RabbitCulling,
    )

    override fun rabbitScanActions(): List<RabbitActionId> = listOf(
        RabbitActionId.Weighing,
        RabbitActionId.Palpation,
        RabbitActionId.Moving,
        RabbitActionId.ViewCard,
        RabbitActionId.Culling,
    )
}
