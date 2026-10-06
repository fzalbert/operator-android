package com.rabbitmes.mobile

import com.rabbitmes.mobile.core.problemCode
import com.rabbitmes.mobile.core.toUserMessage
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class MobileMesErrorMessageTest {
    @Test
    fun `uses backend detail instead of status code`() {
        val error = httpError(
            code = 400,
            body = """{"title":"Некорректный запрос","detail":"У задачи должен быть указан ангар","status":400}""",
        )

        assertEquals("У задачи должен быть указан ангар", error.toUserMessage("Не удалось начать задачу"))
    }

    @Test
    fun `uses backend conflict message`() {
        val error = httpError(409, """{"message":"Сначала завершите предыдущую задачу"}""")

        assertEquals("Сначала завершите предыдущую задачу", error.toUserMessage("Не удалось начать задачу"))
    }

    @Test
    fun `uses friendly conflict fallback when response is empty`() {
        val error = httpError(409, "")

        assertEquals(
            "Не удалось начать задачу. Действие уже выполнено или задача находится в другом состоянии",
            error.toUserMessage("Не удалось начать задачу"),
        )
    }

    @Test
    fun `hides technical server details`() {
        val error = httpError(500, """{"detail":"Unable to resolve service for type System.InternalService"}""")

        assertEquals("Сервис временно недоступен. Попробуйте позже", error.toUserMessage("Не удалось сохранить результат"))
    }

    @Test
    fun `explains connection errors`() {
        assertEquals(
            "Не удалось загрузить задачи: нет соединения с сервером",
            IOException().toUserMessage("Не удалось загрузить задачи"),
        )
    }

    @Test
    fun `localizes nest alignment field names from backend`() {
        val error = httpError(
            400,
            """{"detail":"Для выравнивания гнезд нужно указать либо \"added\", либо \"removed\" — одно из двух."}""",
        )

        assertEquals(
            "Для выравнивания гнезд нужно указать либо «положили», либо «забрали» — одно из двух.",
            error.toUserMessage("Не удалось сохранить результат"),
        )
    }

    @Test
    fun `reads problem code from conflict body`() {
        val error = httpError(
            409,
            """{"title":"Конфликт","detail":"В ангаре уже открыта смена","status":409,"code":"hangar_occupied"}""",
        )

        assertEquals("hangar_occupied", error.problemCode())
        assertEquals(null, httpError(409, "").problemCode())
    }

    private fun httpError(code: Int, body: String): HttpException = HttpException(
        Response.error<Any>(
            code,
            body.toResponseBody("application/problem+json".toMediaType()),
        ),
    )
}
