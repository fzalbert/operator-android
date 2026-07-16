package com.rabbitmes.mobile.ui.components

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.rabbitmes.mobile.domain.AttachmentType
import ru.profikrol.operator.R
import java.io.File

private data class PendingCapture(val file: File, val uri: Uri, val type: AttachmentType)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachmentPickerButtons(
    onAttachment: (type: AttachmentType, name: String, uri: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var showMediaOptions by remember { mutableStateOf(false) }
    var pendingCapture by remember { mutableStateOf<PendingCapture?>(null) }

    fun Uri.displayName(): String {
        val fromProvider = context.contentResolver.query(
            this,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
        return fromProvider ?: lastPathSegment ?: "attachment"
    }

    val mediaPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val type = if (context.contentResolver.getType(uri)?.startsWith("video/") == true) {
                AttachmentType.VIDEO
            } else {
                AttachmentType.PHOTO
            }
            onAttachment(type, uri.displayName(), uri.toString())
        }
    }
    val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        pendingCapture?.let { capture ->
            if (success) {
                onAttachment(capture.type, capture.file.name, capture.uri.toString())
            } else {
                capture.file.delete()
            }
        }
        pendingCapture = null
    }
    val captureVideo = rememberLauncherForActivityResult(ActivityResultContracts.CaptureVideo()) { success ->
        pendingCapture?.let { capture ->
            if (success) {
                onAttachment(capture.type, capture.file.name, capture.uri.toString())
            } else {
                capture.file.delete()
            }
        }
        pendingCapture = null
    }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            onAttachment(AttachmentType.FILE, uri.displayName(), uri.toString())
        }
    }

    fun createCapture(type: AttachmentType): PendingCapture {
        val directory = File(context.cacheDir, "attachments").apply { mkdirs() }
        val file = File.createTempFile(
            if (type == AttachmentType.PHOTO) "photo-" else "video-",
            if (type == AttachmentType.PHOTO) ".jpg" else ".mp4",
            directory,
        )
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return PendingCapture(file, uri, type)
    }

    if (showMediaOptions) {
        ModalBottomSheet(onDismissRequest = { showMediaOptions = false }) {
            Text("Добавить фото или видео", modifier = Modifier.padding(horizontal = MesSpacing.headerHorizontal, vertical = MesSpacing.smallGap))
            Column(Modifier.padding(bottom = MesSpacing.screenBottom)) {
                ListItem(
                    headlineContent = { Text("Сделать фото") },
                    leadingContent = { Icon(painterResource(R.drawable.ic_camera), contentDescription = null) },
                    modifier = Modifier.clickable {
                        showMediaOptions = false
                        createCapture(AttachmentType.PHOTO).also {
                            pendingCapture = it
                            takePhoto.launch(it.uri)
                        }
                    },
                )
                ListItem(
                    headlineContent = { Text("Снять видео") },
                    leadingContent = { Icon(Icons.Default.Videocam, contentDescription = null) },
                    modifier = Modifier.clickable {
                        showMediaOptions = false
                        createCapture(AttachmentType.VIDEO).also {
                            pendingCapture = it
                            captureVideo.launch(it.uri)
                        }
                    },
                )
                ListItem(
                    headlineContent = { Text("Выбрать из галереи") },
                    leadingContent = { Icon(Icons.Default.PhotoLibrary, contentDescription = null) },
                    modifier = Modifier.clickable {
                        showMediaOptions = false
                        mediaPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                        )
                    },
                )
            }
        }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MesSpacing.smallGap),
    ) {
        OutlinedButton(
            onClick = { showMediaOptions = true },
            modifier = Modifier.weight(1f).height(56.dp),
            contentPadding = PaddingValues(horizontal = MesSpacing.smallGap),
        ) {
            Icon(painterResource(R.drawable.ic_camera), contentDescription = null, Modifier.size(20.dp))
            Spacer(Modifier.width(MesSpacing.tinyGap))
            Text("Фото/видео", maxLines = 1, softWrap = false)
        }
        OutlinedButton(
            onClick = { filePicker.launch(arrayOf("*/*")) },
            modifier = Modifier.weight(1f).height(56.dp),
            contentPadding = PaddingValues(horizontal = MesSpacing.smallGap),
        ) {
            Icon(painterResource(R.drawable.ic_upload), contentDescription = null, Modifier.size(20.dp))
            Spacer(Modifier.width(MesSpacing.tinyGap))
            Text("Файл", maxLines = 1, softWrap = false)
        }
    }
}
