package com.rabbitmes.mobile.domain.operations

import com.rabbitmes.mobile.domain.ChecklistItem
import com.rabbitmes.mobile.domain.OperationType
import com.rabbitmes.mobile.domain.PROBLEM_REASON_KEY
import com.rabbitmes.mobile.domain.TargetType
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TargetResultsTest {
    private val cage = ChecklistItem(id = "t1", label = "Клетка 12", targetType = TargetType.CAGE, targetId = "12")

    private fun ready(type: OperationType, values: Map<String, String>, item: ChecklistItem = cage) =
        TargetResults.build(type, item, values) as TargetResult.Ready

    @Test
    fun `insemination and palpation require rfid`() {
        assertEquals(TargetResult.Invalid("RFID обязателен"), TargetResults.build(OperationType.INSEMINATION, cage, emptyMap()))
        assertEquals(TargetResult.Invalid("RFID обязателен"), TargetResults.build(OperationType.PALPATION, cage, mapOf("rfid" to " ")))
    }

    @Test
    fun `palpation sends pregnancy result`() {
        val result = ready(OperationType.PALPATION, mapOf("rfid" to "E1", "pregnant" to "true"))
        assertEquals("pregnant", result.json["result"]!!.jsonPrimitive.content)
        assertEquals("E1", result.rfid)
    }

    @Test
    fun `female arrival requires positive age and sends cell, rfid and age`() {
        assertEquals(
            TargetResult.Invalid("Укажите возраст кролика в днях"),
            TargetResults.build(OperationType.FEMALE_ARRIVAL, cage, mapOf("rfid" to "E1", "age" to "0")),
        )
        val json = ready(OperationType.FEMALE_ARRIVAL, mapOf("rfid" to "E1", "age" to "90", "note" to "x")).json
        assertEquals(12, json["cellId"]!!.jsonPrimitive.int)
        assertEquals("E1", json["femaleRfid"]!!.jsonPrimitive.content)
        assertEquals(90, json["age"]!!.jsonPrimitive.int)
        assertEquals("x", json["note"]!!.jsonPrimitive.content)
        assertFalse(json.containsKey("rfid"))
    }

    @Test
    fun `weighing rabbits sends all weights of the cage`() {
        val json = ready(OperationType.WEIGHING_RABBIT, mapOf("weightsGrams" to "1800, 0, 2100")).json
        assertEquals("[1800,2100]", json["weightsGrams"].toString())
    }

    @Test
    fun `nest equalization sends only one of removed and added`() {
        val json = ready(OperationType.NEST_EQUALIZATION, mapOf("alive" to "8", "added" to "2")).json
        assertTrue(json.containsKey("added"))
        assertFalse(json.containsKey("removed"))
    }

    @Test
    fun `animal settlement resolves selected cell`() {
        val result = TargetResults.build(
            OperationType.ANIMAL_SETTLEMENT,
            cage,
            mapOf("cellId" to "Ряд 1 · Клетка 5"),
            TargetResultContext { selected -> if (selected == "Ряд 1 · Клетка 5") 55L else null },
        ) as TargetResult.Ready
        assertEquals(55, result.json["cellId"]!!.jsonPrimitive.int)
    }

    @Test
    fun `generic operation sends values without rfid and problem keys`() {
        val json = ready(OperationType.GENERAL, mapOf("rfid" to "E1", PROBLEM_REASON_KEY to "x", "done" to "true")).json
        assertEquals(setOf("done"), json.keys)
    }
}
