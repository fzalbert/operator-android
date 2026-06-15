package ru.profikrol.operator.core.role

import ru.profikrol.operator.feature.home.HomeActionId
import ru.profikrol.operator.feature.rfidscanresult.RabbitActionId

/**
 * Abstract Factory: возвращает набор операций, доступных текущей роли.
 * Чистый Kotlin — никаких Android-зависимостей, чтобы фабрика
 * легко тестировалась и переезжала в multiplatform при необходимости.
 *
 * Методы добавляются по мере появления экранов с ролевыми отличиями.
 * Возвращает только идентификаторы — UI знает, как их рендерить и куда ведут клики.
 */
interface RoleFeatureFactory {

    /** Действия на главном экране (Home). */
    fun homeActions(): List<HomeActionId>

    /** Действия над кроликом на экране результата сканирования RFID. */
    fun rabbitScanActions(): List<RabbitActionId>
}
