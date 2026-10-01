package com.rabbitmes.mobile.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RfidMatchingTest {
    private fun item(label: String = "", targetId: String = "t-1", scanIdentifier: String? = null) =
        ChecklistItem(id = "1", label = label, targetType = TargetType.RABBIT, targetId = targetId, scanIdentifier = scanIdentifier)

    @Test
    fun `matches by target id ignoring case and spaces`() {
        assertTrue(item(targetId = "ABC123").matchesRfid(" abc123 "))
    }

    @Test
    fun `matches by scan identifier and resolved rabbit id`() {
        assertTrue(item(scanIdentifier = "E200").matchesRfid("e200"))
        assertTrue(item(targetId = "55").matchesRfid("E200", resolvedRabbitId = "55"))
    }

    @Test
    fun `matches rfid as a whole word in label`() {
        assertTrue(item(label = "RFID: E2:80:11 · Возраст: 120").matchesRfid("E2:80:11"))
    }

    @Test
    fun `partial rfid does not match label`() {
        assertFalse(item(label = "RFID: E2801160 · Возраст: 120").matchesRfid("E280"))
        assertFalse(item(label = "RFID: E2801160").matchesRfid("1160"))
    }

    @Test
    fun `blank rfid never matches`() {
        assertFalse(item(label = "RFID: ").matchesRfid("  "))
    }
}
