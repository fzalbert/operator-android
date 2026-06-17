package ru.profikrol.operator.domain.nfc

interface NfcReader {
    val isAvailable: Boolean

    fun start(onScanned: (ScannedPayload) -> Unit)

    fun stop()
}

@JvmInline
value class ScannedPayload(val value: String)
