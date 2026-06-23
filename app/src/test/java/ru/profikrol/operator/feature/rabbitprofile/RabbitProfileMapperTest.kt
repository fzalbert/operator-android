package ru.profikrol.operator.feature.rabbitprofile

import org.junit.Assert.assertEquals
import org.junit.Test

class RabbitProfileMapperTest {

    @Test
    fun `backend map is converted to ordered profile sections`() {
        val backendResponse = mapOf<String, Any?>(
            "rabbit_code" to "RF-00247",
            "reproductive_status" to "Беременна",
            "classification" to "Самка",
            "sex" to "Самка",
            "age" to "8 мес",
            "breed" to "Калифорнийская",
            "live_weight" to "3.2 кг",
            "registration_date" to "10.08.2025",
            "kindling_date" to "15.02.2026",
            "department" to "Отделение 2",
            "responsible_employee" to "Иванов И.И.",
            "transfer_date" to "20.03.2026",
            "hangar_number" to "Ангар 2",
            "cage" to "A-12",
            "diagnosis" to "Здорова",
            "feed_type" to "Комбикорм №3",
            "ready_for_insemination" to "Да (28.03.2026)",
            "planned_slaughter_date" to "15.05.2026",
            "favorite_treat" to "Морковь",
        )

        val uiModel = backendResponse.toRabbitProfile().toUiModel()

        assertEquals("RF-00247", uiModel.code)
        assertEquals("Беременна", uiModel.status)
        assertEquals("Самка", uiModel.classification)
        assertEquals(listOf("Общие данные", "Планирование"), uiModel.sections.map { it.title })
        assertEquals(
            listOf(
                "Пол",
                "Возраст",
                "Порода",
                "Вес (живая масса)",
                "Дата регистрации",
                "Дата щенения (окрола)",
                "Отделение",
                "Ответственный сотрудник",
                "Дата перевода",
                "Номер ангара",
                "Клетка",
                "Диагноз",
                "Вид корма",
                "Favorite treat",
            ),
            uiModel.sections.first().fields.keys.toList(),
        )
        assertEquals("A-12", uiModel.sections.first().fields["Клетка"])
        assertEquals(
            "Да (28.03.2026)",
            uiModel.sections[1].fields["Готовность к осеменению"],
        )
        assertEquals("Морковь", uiModel.sections.first().fields["Favorite treat"])
    }

    @Test
    fun `missing fields and empty sections are not displayed`() {
        val uiModel = mapOf<String, Any?>("code" to "RF-1")
            .toRabbitProfile()
            .toUiModel()

        assertEquals("—", uiModel.classification)
        assertEquals(emptyList<RabbitProfileSection>(), uiModel.sections)
    }
}
