@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.melati.ui.screens.cek

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.melati.R
import com.example.melati.domain.model.Attachment
import com.example.melati.domain.model.LayananType
import com.example.melati.domain.model.MessageAuthor
import com.example.melati.domain.model.Ticket
import com.example.melati.domain.model.TicketMessage
import com.example.melati.domain.model.TicketStatus
import com.example.melati.domain.model.TicketType
import com.example.melati.ui.components.ChatBubble
import com.example.melati.ui.components.ShadcnButton
import com.example.melati.ui.components.ShadcnCard
import com.example.melati.ui.components.ShadcnCardBody
import com.example.melati.ui.components.ShadcnFileChip
import com.example.melati.ui.components.ShadcnOutlinedButton
import com.example.melati.ui.components.ShadcnScreenHeader
import com.example.melati.ui.components.ShadcnStateCard
import com.example.melati.ui.components.ShadcnTextField
import com.example.melati.ui.components.StatusBadge
import com.example.melati.ui.components.formatTanggalIndo
import com.example.melati.ui.theme.MelatiBorder
import com.example.melati.ui.theme.MelatiDestructive
import com.example.melati.ui.theme.MelatiForeground
import com.example.melati.ui.theme.MelatiMuted
import com.example.melati.ui.theme.MelatiMutedForeground
import com.example.melati.ui.theme.MelatiPlaceholder
import com.example.melati.ui.theme.MelatiPrimary
import com.example.melati.ui.theme.MelatiSurface
import com.example.melati.ui.theme.MelatiTheme

private val ScreenPadding = 20.dp
private val ComposerControlHeight = 44.dp

@Composable
fun CekScreen(
    state: CekUiState,
    onEvent: (CekEvent) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()

    // Reset state saat layar Cek ditinggalkan (termasuk saat pindah tab).
    DisposableEffect(Unit) {
        onDispose { onEvent(CekEvent.Reset) }
    }
    val pickReplyAttachments = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) onEvent(CekEvent.ReplyAttachmentsPicked(uris))
    }

    LaunchedEffect(state.message) {
        val pesan = state.message ?: return@LaunchedEffect
        snackbarHostState.currentSnackbarData?.dismiss()
        snackbarHostState.showSnackbar(pesan)
        onEvent(CekEvent.MessageShown)
    }

    LaunchedEffect(state.phase) {
        if (state.phase !is CekPhase.Idle && state.phase !is CekPhase.Loading) {
            keyboardController?.hide()
            focusManager.clearFocus()
        }
    }

    // Auto-scroll ke pesan terbaru.
    // Pesan terbaru di atas: scroll ke item pertama percakapan.
    LaunchedEffect(state.ticket?.messages?.size, state.phase) {
        val jumlahPesan = state.ticket?.messages?.size ?: return@LaunchedEffect
        if (jumlahPesan > 0 && listState.layoutInfo.totalItemsCount > 0) {
            listState.animateScrollToItem(4.coerceAtMost(listState.layoutInfo.totalItemsCount - 1))
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = ScreenPadding)
            .padding(top = 16.dp),
    ) {
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { onEvent(CekEvent.Refresh) },
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 12.dp, top = 4.dp),
            ) {
                item {
                    ShadcnScreenHeader(
                        title = stringResource(R.string.cek_title),
                        subtitle = stringResource(R.string.cek_subtitle),
                        modifier = Modifier.padding(bottom = 16.dp),
                    )
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ShadcnTextField(
                            value = state.query,
                            onValueChange = { onEvent(CekEvent.QueryChanged(it)) },
                            label = stringResource(R.string.label_nomor_tiket),
                            placeholder = stringResource(R.string.placeholder_nomor_tiket),
                            error = if (state.isQueryError) stringResource(R.string.error_required) else null,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Search,
                            ),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                    onEvent(CekEvent.Check)
                                },
                            ),
                        )
                        val sedangMemuat = state.phase is CekPhase.Loading
                        ShadcnButton(
                            text = stringResource(R.string.action_cek),
                            onClick = {
                                keyboardController?.hide()
                                focusManager.clearFocus()
                                onEvent(CekEvent.Check)
                            },
                            enabled = !sedangMemuat && !state.isRefreshing,
                            leadingIcon = Icons.Filled.Search,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                when (val phase = state.phase) {
                    is CekPhase.Idle -> { }

                    is CekPhase.Loading -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(
                                    color = MelatiPrimary,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(28.dp),
                                )
                            }
                        }
                    }

                    is CekPhase.NotFound -> {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                ShadcnStateCard(
                                    title = stringResource(R.string.cek_not_found_title),
                                    message = stringResource(R.string.cek_not_found_message),
                                    actionLabel = stringResource(R.string.action_coba_lagi),
                                    onAction = { onEvent(CekEvent.Retry) },
                                )
                            }
                        }
                    }

                    is CekPhase.Failed -> {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                ShadcnStateCard(
                                    title = stringResource(R.string.cek_failed_title),
                                    message = phase.message,
                                    actionLabel = stringResource(R.string.action_coba_lagi),
                                    onAction = { onEvent(CekEvent.Retry) },
                                )
                            }
                        }
                    }

                    is CekPhase.Loaded -> {
                        val tiket = phase.ticket
                        item { TicketSummaryCard(ticket = tiket, isCompleting = state.isCompleting, onComplete = { onEvent(CekEvent.RequestComplete) }) }

                        item {
                            Text(
                                text = stringResource(R.string.conversation_title),
                                style = MaterialTheme.typography.headlineSmall,
                                color = MelatiForeground,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }

                        if (tiket.messages.isEmpty()) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(MaterialTheme.shapes.small)
                                        .background(MelatiMuted)
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Info,
                                        contentDescription = null,
                                        tint = MelatiMutedForeground,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        text = stringResource(R.string.conversation_empty),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MelatiMutedForeground,
                                    )
                                }
                            }
                        } else {
                            itemsIndexed(tiket.messages) { _, pesan ->
                                ChatBubble(message = pesan)
                            }
                        }
                    }
                }
            }
        }

        // ===== Composer balasan (hanya saat tiket termuat) =====
        state.ticket?.let { tiket ->
            HorizontalDivider(thickness = 1.dp, color = MelatiBorder)
            ReplyComposer(
                state = state,
                isFinal = tiket.status.isFinal,
                onEvent = onEvent,
                onPickAttachments = {
                    pickReplyAttachments.launch(arrayOf("image/jpeg", "image/png", "application/pdf"))
                },
            )
        }
    }

    // ===== Konfirmasi tandai selesai =====
    if (state.showCompleteConfirm) {
        com.example.melati.ui.components.ShadcnAlertDialog(
            title = stringResource(R.string.selesai_confirm_title),
            message = stringResource(R.string.selesai_confirm_message),
            confirmLabel = stringResource(R.string.action_ya),
            onConfirm = { onEvent(CekEvent.ConfirmComplete) },
            dismissLabel = stringResource(R.string.action_batal),
            onDismiss = { onEvent(CekEvent.DismissCompleteConfirm) },
        )
    }
}

/** Kartu ringkasan tiket */
@Composable
private fun TicketSummaryCard(
    ticket: Ticket,
    isCompleting: Boolean,
    onComplete: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ShadcnCard {
            ShadcnCardBody(spacing = 12.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.label_nomor_tiket),
                        style = MaterialTheme.typography.labelSmall,
                        color = MelatiMutedForeground,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = ticket.number,
                            style = MaterialTheme.typography.titleLarge,
                            color = MelatiForeground,
                            modifier = Modifier.weight(1f),
                        )
                        StatusBadge(status = ticket.status)
                    }
                }

                MetaRow(label = stringResource(R.string.detail_klasifikasi), value = ticket.type.label)
                formatTanggalIndo(ticket.createdAt)?.let { tanggal ->
                    MetaRow(label = stringResource(R.string.detail_dibuat), value = tanggal)
                }

                if (!ticket.status.isFinal) {
                    ShadcnOutlinedButton(
                        text = stringResource(R.string.action_selesai),
                        onClick = onComplete,
                        enabled = !isCompleting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                    )
                }
            }
        }

        ShadcnCard {
            ShadcnCardBody(spacing = 12.dp) {
                ticket.subject?.takeIf { it.isNotBlank() }?.let { judul ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = stringResource(R.string.detail_judul),
                            style = MaterialTheme.typography.labelSmall,
                            color = MelatiMutedForeground,
                        )
                        Text(
                            text = judul,
                            style = MaterialTheme.typography.titleMedium,
                            color = MelatiForeground,
                        )
                    }
                }
                if (ticket.body.isNotBlank()) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = stringResource(R.string.detail_isi),
                            style = MaterialTheme.typography.labelSmall,
                            color = MelatiMutedForeground,
                        )
                        Text(
                            text = ticket.body,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MelatiForeground,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetaRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MelatiMutedForeground,
            modifier = Modifier.defaultMinSize(minWidth = 110.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MelatiForeground,
        )
    }
}

@Composable
private fun ReplyComposer(
    state: CekUiState,
    isFinal: Boolean,
    onEvent: (CekEvent) -> Unit,
    onPickAttachments: () -> Unit,
) {
    val tidakAktif = isFinal || state.isSendingReply
    val trimmedText = state.replyText.trim()
    val isMinLengthError = trimmedText.isNotEmpty() && trimmedText.length < 5
    val bisaKirim = !tidakAktif && !state.isResolvingReplyAttachments &&
        trimmedText.length >= 5
    val bisaLampir = !tidakAktif && !state.isResolvingReplyAttachments &&
        state.replyAttachments.size < Attachment.MAX_FILES
    val labelBalas = stringResource(R.string.placeholder_balas)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        state.replyAttachments.forEach { lampiran ->
            ShadcnFileChip(
                name = lampiran.fileName,
                meta = formatLampiranSize(lampiran.sizeBytes),
                onRemove = { onEvent(CekEvent.ReplyAttachmentRemoved(lampiran)) },
            )
        }

        state.replyAttachmentError?.let { error ->
            val pesan = when (error) {
                ReplyAttachmentError.FILE_TYPE -> stringResource(R.string.error_file_type)
                ReplyAttachmentError.FILE_SIZE -> stringResource(R.string.error_file_size)
                ReplyAttachmentError.FILE_COUNT -> stringResource(R.string.error_file_count)
            }
            Text(
                text = pesan,
                style = MaterialTheme.typography.labelSmall,
                color = MelatiDestructive,
            )
        }

        if (state.replyAttachments.isEmpty()) {
            Text(
                text = stringResource(R.string.lampiran_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MelatiMutedForeground,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        if (isFinal) {
            Text(
                text = stringResource(R.string.reply_disabled_closed),
                style = MaterialTheme.typography.labelSmall,
                color = MelatiMutedForeground,
            )
        }

        if (isMinLengthError) {
            Text(
                text = stringResource(R.string.error_reply_min_length),
                style = MaterialTheme.typography.labelSmall,
                color = MelatiDestructive,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Box(
                modifier = Modifier
                    .size(ComposerControlHeight)
                    .clip(CircleShape)
                    .background(if (bisaLampir) MelatiSurface else MelatiMuted)
                    .border(
                        width = 1.dp,
                        color = if (bisaLampir) MelatiBorder else MelatiMuted,
                        shape = CircleShape,
                    )
                    .clickable(enabled = bisaLampir, onClick = onPickAttachments),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.action_tambah_lampiran),
                    tint = if (bisaLampir) MelatiPrimary else MelatiPlaceholder,
                    modifier = Modifier.size(20.dp),
                )
            }
            CompactReplyField(
                value = state.replyText,
                onValueChange = { onEvent(CekEvent.ReplyChanged(it)) },
                placeholder = labelBalas,
                enabled = !tidakAktif,
                isError = isMinLengthError,
                maxLines = 4,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (bisaKirim) onEvent(CekEvent.SendReply)
                    },
                ),
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = labelBalas },
            )

            Box(
                modifier = Modifier
                    .size(ComposerControlHeight)
                    .clip(CircleShape)
                    .background(if (bisaKirim) MelatiPrimary else MelatiMuted)
                    .clickable(enabled = bisaKirim) { onEvent(CekEvent.SendReply) },
                contentAlignment = Alignment.Center,
            ) {
                if (state.isSendingReply) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MelatiMutedForeground,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Send,
                        contentDescription = stringResource(R.string.action_kirim_balasan),
                        tint = if (bisaKirim) Color.White else MelatiPlaceholder,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactReplyField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    enabled: Boolean,
    isError: Boolean,
    maxLines: Int,
    keyboardOptions: KeyboardOptions,
    keyboardActions: KeyboardActions,
    modifier: Modifier = Modifier,
) {
    var isFocused by remember { mutableStateOf(false) }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        singleLine = false,
        maxLines = maxLines,
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = MelatiForeground),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        modifier = modifier
            .heightIn(min = ComposerControlHeight, max = ComposerControlHeight * 4)
            .onFocusChanged { isFocused = it.isFocused },
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = ComposerControlHeight, max = ComposerControlHeight * 4)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MelatiSurface)
                    .border(
                        width = 1.dp,
                        color = when {
                            isError -> MelatiDestructive
                            isFocused -> MelatiPrimary
                            else -> MelatiBorder
                        },
                        shape = RoundedCornerShape(8.dp),
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                }
                innerTextField()
            }
        },
    )
}

private fun formatLampiranSize(bytes: Long): String {
    val lokalis = java.util.Locale.forLanguageTag("id-ID")
    return when {
        bytes >= 1_000_000 -> String.format(lokalis, "%.1f MB", bytes / 1_000_000.0)
        bytes >= 1_000 -> String.format(lokalis, "%.1f KB", bytes / 1_000.0)
        else -> String.format(lokalis, "%d B", bytes)
    }
}

@Preview(name = "Cek dengan thread", showBackground = true, heightDp = 1150)
@Composable
private fun PreviewCekThread() {
    MelatiTheme {
        CekScreen(
            state = CekUiState(
                query = "L-2026-0001",
                phase = CekPhase.Loaded(
                    ticket = Ticket(
                        number = "L-2026-0001",
                        type = TicketType.LAPOR,
                        layanan = LayananType.PST,
                        status = TicketStatus.IN_PROGRESS,
                        subject = "Pertanyaan data statistik tahunan",
                        body = "Mohon bantuan terkait data statistik tahunan. Terima kasih.",
                        createdAt = "2026-10-20T09:30:00Z",
                        messages = listOf(
                            TicketMessage(
                                id = "1",
                                body = "Terima kasih, permintaan Anda sedang kami proses.",
                                author = MessageAuthor.ADMIN,
                                createdAt = "2026-10-21T08:00:00Z",
                                attachments = listOf(com.example.melati.domain.model.TicketAttachment(name = "penjelasan-data.pdf")),
                            ),
                            TicketMessage(
                                id = "2",
                                body = "Baik, saya menunggu kabar selanjutnya.",
                                author = MessageAuthor.REPORTER,
                                createdAt = "2026-10-21T09:15:00Z",
                            ),
                        ),
                    ),
                ),
                replyText = "Terima kasih petugas.",
            ),
            onEvent = {},
            snackbarHostState = androidx.compose.material3.SnackbarHostState(),
        )
    }
}
