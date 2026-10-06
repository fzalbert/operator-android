package com.rabbitmes.mobile.shift.model

import java.time.LocalDate

/** Цех. В каждом цехе обычно два ангара. */
data class Workshop(
    val id: String,
    val name: String,
    val hangars: List<Hangar>,
)

data class Hangar(
    val id: String,
    val name: String,
)

/** Задача, назначенная оператору в выбранном ангаре. */
data class AssignedTask(
    val id: String,
    val title: String,
    val hangarName: String,
    /** Срок «до ЧЧ:ММ», если сервер его отдаёт. */
    val dueTime: String? = null,
    /** Плановая дата задачи с прошлых дней; null — задача на сегодня. */
    val overdueDate: LocalDate? = null,
)
