package ru.profikrol.operator.data.nfc

import android.app.Activity
import android.app.Application
import android.content.Context
import android.nfc.NfcAdapter
import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.os.Bundle
import dagger.hilt.android.qualifiers.ApplicationContext
import ru.profikrol.operator.domain.nfc.NfcReader
import ru.profikrol.operator.domain.nfc.ScannedPayload
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidNfcReader @Inject constructor(
    @ApplicationContext private val context: Context,
) : NfcReader {

    private val nfcAdapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(context)
    private var currentActivity: Activity? = null
    private var onScanned: ((ScannedPayload) -> Unit)? = null

    override val isAvailable: Boolean
        get() = nfcAdapter?.isEnabled == true

    init {
        (context.applicationContext as? Application)?.registerActivityLifecycleCallbacks(
            object : Application.ActivityLifecycleCallbacks {
                override fun onActivityResumed(activity: Activity) {
                    currentActivity = activity
                    enableReaderMode()
                }

                override fun onActivityPaused(activity: Activity) {
                    if (currentActivity === activity) {
                        disableReaderMode(activity)
                        currentActivity = null
                    }
                }

                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
                override fun onActivityStarted(activity: Activity) = Unit
                override fun onActivityStopped(activity: Activity) = Unit
                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
                override fun onActivityDestroyed(activity: Activity) = Unit
            },
        )
    }

    override fun start(onScanned: (ScannedPayload) -> Unit) {
        this.onScanned = onScanned
        enableReaderMode()
    }

    override fun stop() {
        currentActivity?.let(::disableReaderMode)
        onScanned = null
    }

    private fun enableReaderMode() {
        val activity = currentActivity ?: return
        val adapter = nfcAdapter?.takeIf { it.isEnabled } ?: return
        if (onScanned == null) return

        adapter.enableReaderMode(
            activity,
            { tag ->
                onScanned?.invoke(ScannedPayload(tag.toScannedPayload()))
            },
            NfcAdapter.FLAG_READER_NFC_A or
                NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_NFC_F or
                NfcAdapter.FLAG_READER_NFC_V,
            null,
        )
    }

    private fun disableReaderMode(activity: Activity) {
        nfcAdapter?.disableReaderMode(activity)
    }
}

private fun Tag.toScannedPayload(): String =
    readNdefText().orEmpty().ifBlank { id.toHexString() }

private fun Tag.readNdefText(): String? {
    val ndef = Ndef.get(this) ?: return null

    return runCatching {
        ndef.connect()
        val message = ndef.ndefMessage ?: ndef.cachedNdefMessage
        message
            ?.records
            ?.mapNotNull(NdefRecord::toText)
            ?.joinToString(separator = "\n")
    }.also {
        runCatching { ndef.close() }
    }.getOrNull()
}

private fun NdefRecord.toText(): String? {
    return when {
        tnf == NdefRecord.TNF_WELL_KNOWN && type.contentEquals(NdefRecord.RTD_TEXT) -> {
            val status = payload.firstOrNull()?.toInt() ?: return null
            val languageCodeLength = status and 0x3F
            val textStart = 1 + languageCodeLength
            if (payload.size <= textStart) return null

            val charset = if (status and 0x80 == 0) Charsets.UTF_8 else Charsets.UTF_16
            String(payload, textStart, payload.size - textStart, charset)
        }

        tnf == NdefRecord.TNF_MIME_MEDIA && String(type) == "text/plain" -> {
            String(payload, Charsets.UTF_8)
        }

        else -> null
    }
}

private fun ByteArray.toHexString(): String =
    joinToString(separator = "") { byte ->
        "%02X".format(byte)
    }
