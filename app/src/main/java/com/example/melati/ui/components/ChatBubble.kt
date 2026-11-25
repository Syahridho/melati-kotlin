package com.example.melati.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.melati.R
import com.example.melati.domain.model.MessageAuthor
import com.example.melati.domain.model.TicketAttachment
import com.example.melati.domain.model.TicketMessage
import com.example.melati.ui.theme.MelatiBorder
import com.example.melati.ui.theme.MelatiForeground
import com.example.melati.ui.theme.MelatiMuted
import com.example.melati.ui.theme.MelatiMutedForeground
import com.example.melati.ui.theme.MelatiOnPrimaryMuted
import com.example.melati.ui.theme.MelatiPrimary
import com.example.melati.ui.theme.MelatiSurface
import com.example.melati.resolveImageUrl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.net.URL
import com.example.melati.ui.theme.MelatiOnPrimaryMuted
import com.example.melati.ui.theme.MelatiPrimary
import com.example.melati.ui.theme.MelatiSurface

/**
 * Gelembung percakapan buatan sendiri (tanpa komponen pustaka): pesan petugas
 * di KIRI (putih bergaris), pesan pelapor di KANAN (biru penuh). Sudut kecil
 * muncul di sisi penulis agar arah percakapan terbaca.
 */
@Composable
fun ChatBubble(message: TicketMessage, modifier: Modifier = Modifier) {
    val fromReporter = message.author == MessageAuthor.REPORTER
    var previewIndex by remember { mutableStateOf<Int?>(null) }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (fromReporter) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom,
    ) {
        Surface(
            color = if (fromReporter) MelatiPrimary else MelatiSurface,
            contentColor = if (fromReporter) Color.White else MelatiForeground,
            border = if (fromReporter) null else BorderStroke(1.dp, MelatiBorder),
            shape = if (fromReporter) {
                RoundedCornerShape(
                    topStart = 12.dp, topEnd = 12.dp, bottomStart = 12.dp, bottomEnd = 4.dp,
                )
            } else {
                RoundedCornerShape(
                    topStart = 12.dp, topEnd = 12.dp, bottomStart = 4.dp, bottomEnd = 12.dp,
                )
            },
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            modifier = Modifier.widthIn(max = 300.dp),
        ) {
            Column(
                modifier = Modifier
                    .background(Color.Transparent)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Text(
                    text = stringResource(
                        id = if (fromReporter) R.string.author_reporter else R.string.author_admin,
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (fromReporter) MelatiOnPrimaryMuted else MelatiMutedForeground,
                )
                if (message.body.isNotBlank()) {
                    Text(
                        text = message.body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (fromReporter) Color.White else MelatiForeground,
                    )
                }
                if (message.attachments.isNotEmpty()) {
                    Spacer(Modifier.size(6.dp))
                    MessageAttachments(
                        attachments = message.attachments,
                        fromReporter = fromReporter,
                        onImageClick = { index -> previewIndex = index },
                    )
                }
                formatTanggalIndo(message.createdAt)?.let { waktu ->
                    Text(
                        text = waktu,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (fromReporter) MelatiOnPrimaryMuted else MelatiMutedForeground,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
    previewIndex?.let { idx ->
        val images = message.attachments.filter { it.isImage() }
        if (images.isNotEmpty()) {
            ImagePreviewDialog(
                images = images,
                initialIndex = idx.coerceIn(0, images.lastIndex),
                onDismiss = { previewIndex = null },
            )
        }
    }
}

fun TicketAttachment.isImage(): Boolean {
    val mime = mimeType?.lowercase().orEmpty()
    if (mime.startsWith("image/")) return true
    val ext = name.substringAfterLast('.', "").lowercase()
    return ext in setOf("png", "jpg", "jpeg", "gif", "webp", "bmp")
}

private fun formatSize(bytes: Long?): String {
    if (bytes == null || bytes <= 0) return ""
    return if (bytes >= 1_000_000) String.format(java.util.Locale.getDefault(), "%.1f MB", bytes / 1_000_000.0)
    else if (bytes >= 1_000) String.format(java.util.Locale.getDefault(), "%.1f KB", bytes / 1_000.0)
    else "$bytes B"
}

@Composable
private fun MessageAttachments(
    attachments: List<TicketAttachment>,
    fromReporter: Boolean,
    onImageClick: (Int) -> Unit,
) {
    val context = LocalContext.current
    val images = attachments.filter { it.isImage() }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        attachments.forEach { att ->
            val url = resolveImageUrl(att.rawSource)
            if (att.isImage()) {
                val imageIndex = images.indexOf(att)
                AsyncImage(
                    model = url,
                    contentDescription = att.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .widthIn(max = 200.dp)
                        .size(120.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onImageClick(imageIndex) },
                )
            } else {
                Surface(
                    color = if (fromReporter) MelatiOnPrimaryMuted else MelatiMuted,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.clickable {
                        url?.let { openAttachment(context, it, att.name) }
                    },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Filled.Info, contentDescription = null)
                        Column {
                            Text(
                                text = att.name,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            val size = formatSize(att.sizeBytes)
                            if (size.isNotBlank()) {
                                Text(text = size, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun openAttachment(context: Context, url: String, name: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    context.startActivity(intent)
}

@Composable
private fun ImagePreviewDialog(
    images: List<TicketAttachment>,
    initialIndex: Int,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val pagerState = rememberPagerState(initialPage = initialIndex, pageCount = { images.size })
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val scope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable { onDismiss() },
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                val url = resolveImageUrl(images[page].rawSource)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(page) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 5f)
                                offset += pan
                            }
                        }
                        .pointerInput(page) {
                            detectTapGestures(onDoubleTap = {
                                scale = if (scale > 1f) 1f else 2.5f
                                offset = Offset.Zero
                            })
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    AsyncImage(
                        model = url,
                        contentDescription = images[page].name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y,
                            ),
                    )
                }
            }
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val current = images.getOrElse(pagerState.currentPage) { images.first() }
                Text(
                    text = "${current.name} ${formatSize(current.sizeBytes)}".trim(),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Tutup", tint = Color.White)
                }
                IconButton(onClick = {
                    val url = resolveImageUrl(current.rawSource)
                    if (url != null) {
                        scope.launch { downloadImage(context, url, current.name) }
                    }
                }) {
                    Icon(Icons.Filled.Info, contentDescription = "Unduh", tint = Color.White)
                }
            }
        }
    }
}

private suspend fun downloadImage(context: Context, url: String, name: String) {
    withContext(Dispatchers.IO) {
        try {
            val bytes = URL(url).openStream().readBytes()
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val values = android.content.ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, name)
                    put(MediaStore.Downloads.MIME_TYPE, "image/*")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                uri?.let { context.contentResolver.openOutputStream(it)?.use { os -> os.write(bytes) } }
            } else {
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                dir.mkdirs()
                java.io.File(dir, name).writeBytes(bytes)
            }
        } catch (_: Exception) {
        }
    }
}
