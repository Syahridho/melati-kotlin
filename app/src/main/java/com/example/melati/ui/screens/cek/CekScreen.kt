@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.melati.ui.screens.cek

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.melati.domain.model.LayananType
import com.example.melati.domain.model.MessageAuthor
import com.example.melati.domain.model.Ticket
import com.example.melati.domain.model.TicketMessage
import com.example.melati.domain.model.TicketStatus
import com.example.melati.domain.model.TicketType
import com.example.melati.ui.components.ChatBubble
import com.example.melati.ui.components.ShadcnAlertDialog
import com.example.melati.ui.components.ShadcnButton
import com.example.melati.ui.components.ShadcnCard
import com.example.melati.ui.components.ShadcnCardBody
import com.example.melati.ui.components.ShadcnOutlinedButton
import com.example.melati.ui.components.ShadcnScreenHeader
import com.example.melati.ui.components.ShadcnStateCard
import com.example.melati.ui.components.ShadcnTextField
import com.example.melati.ui.components.StatusBadge
import com.example.melati.ui.components.formatTanggalIndo
import com.example.melati.ui.theme.MelatiBorder
import com.example.melati.ui.theme.MelatiForeground
import com.example.melati.ui.theme.MelatiMuted
import com.example.melati.ui.theme.MelatiMutedForeground
import com.example.melati.ui.theme.MelatiPlaceholder
import com.example.melati.ui.theme.MelatiPrimary
import com.example.melati.ui.theme.MelatiTheme

private val ScreenPadding = 20.dp

@Composable
fun CekScreen(
    state: CekUiState,
    onEvent: (CekEvent) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(state.message) {
        val pesan = state.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(pesan)
        onEvent(CekEvent.MessageShown)
    }

    // Keyboard berbasis fase: sembunyikan begitu hasil (Failed/NotFound/Loaded)
    // tampil, tidak hanya saat event Check dikirim dari tombol / IME.
    LaunchedEffect(state.phase) {
        if (state.phase !is CekPhase.Idle && state.phase !is CekPhase.Loading) {
            keyboardController?.hide()
            focusManager.clearFocus()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .padding(horizontal = ScreenPadding)
            .padding(top = 16.dp),
    ) {
        // ===== Header =====
        ShadcnScreenHeader(
            title = stringResource(R.string.cek_title),
            subtitle = stringResource(R.string.cek_subtitle),
            modifier = Modifier.padding(bottom = 16.dp),
        )

        // ===== Form pencarian =====
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
        Spacer(Modifier.size(12.dp))
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

        Spacer(Modifier.size(16.dp))

        // ===== Konten per fase =====
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (val phase = state.phase) {
                is CekPhase.Idle -> {
                    // Form tetap tampil, tanpa konten tambahan.
                }

                is CekPhase.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            color = MelatiPrimary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }

                is CekPhase.NotFound -> {
                    StatePanel(
                        title = stringResource(R.string.cek_not_found_title),
                        message = stringResource(R.string.cek_not_found_message),
                        onRetry = { onEvent(CekEvent.Retry) },
                    )
                }

                is CekPhase.Failed -> {
                    StatePanel(
                        title = stringResource(R.string.cek_failed_title),
                        message = phase.message,
                        onRetry = { onEvent(CekEvent.Retry) },
                    )
                }

                is CekPhase.Loaded -> {
                    val tiket = phase.ticket
                    PullToRefreshBox(
                        isRefreshing = state.isRefreshing,
                        onRefresh = { onEvent(CekEvent.Refresh) },
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        TicketDetail(
                            ticket = tiket,
                            isCompleting = state.isCompleting,
                            onEvent = onEvent,
                        )
                    }
                }
            }
        }

        // ===== Composer balasan (hanya saat tiket termuat) =====
        state.ticket?.let { tiket ->
            HorizontalDivider(thickness = 1.dp, color = MelatiBorder)
            Spacer(Modifier.size(12.dp))
            ReplyComposer(
                state = state,
                isFinal = tiket.status.isFinal,
                onEvent = onEvent,
            )
            Spacer(Modifier.size(12.dp))
        }
    }

    // ===== Konfirmasi tandai selesai =====
    if (state.showCompleteConfirm) {
        ShadcnAlertDialog(
            title = stringResource(R.string.selesai_confirm_title),
            message = stringResource(R.string.selesai_confirm_message),
            confirmLabel = stringResource(R.string.action_ya),
            onConfirm = { onEvent(CekEvent.ConfirmComplete) },
            dismissLabel = stringResource(R.string.action_batal),
            onDismiss = { onEvent(CekEvent.DismissCompleteConfirm) },
        )
    }
}

/**
 * Detail tiket di dalam PullToRefreshBox: kartu ringkasan, isi laporan,
 * lalu judul percakapan dan thread pesan. Auto-scroll ke pesan terbaru.
 */
@Composable
private fun TicketDetail(
    ticket: Ticket,
    isCompleting: Boolean,
    onEvent: (CekEvent) -> Unit,
) {
    val listState = rememberLazyListState()
    val jumlahPesan = ticket.messages.size

    // Auto-scroll ke pesan terbaru ketika jumlah pesan bertambah.
    LaunchedEffect(jumlahPesan) {
        if (jumlahPesan > 0) {
            listState.animateScrollToItem(1 + jumlahPesan)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 12.dp, top = 4.dp),
    ) {
        item {
            TicketSummaryCard(
                ticket = ticket,
                isCompleting = isCompleting,
                onComplete = { onEvent(CekEvent.RequestComplete) },
            )
        }

        item {
            Text(
                text = stringResource(R.string.conversation_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MelatiForeground,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        if (ticket.messages.isEmpty()) {
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
            itemsIndexed(ticket.messages) { _, pesan ->
                ChatBubble(message = pesan)
            }
        }
    }
}

/** Kartu ringkasan tiket: nomor, badge, metadata, isi, dan aksi selesai. */
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

                MetaRow(
                    label = stringResource(R.string.detail_klasifikasi),
                    value = ticket.type.label,
                )
                formatTanggalIndo(ticket.createdAt)?.let { tanggal ->
                    MetaRow(
                        label = stringResource(R.string.detail_dibuat),
                        value = tanggal,
                    )
                }

                // Aksi tandai selesai tidak tersedia untuk status final.
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

        // Isi laporan.
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

/**
 * Panel ramah untuk NotFound / Failed, dengan tombol coba lagi.
 *
 * Tahan terhadap tinggi terbatas (mis.) keyboard aktif: isi kartu bisa
 * digulir, pesan panjang wrap penuh, dan tidak ada tinggi tetap yang
 * memotong teks dua baris atau lebih.
 */
@Composable
private fun StatePanel(title: String, message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            ShadcnStateCard(
                title = title,
                message = message,
                actionLabel = stringResource(R.string.action_coba_lagi),
                onAction = onRetry,
            )
        }
    }
}

/** Komposer balasan di bawah thread; nonaktif untuk status final / saat mengirim. */
@Composable
private fun ReplyComposer(
    state: CekUiState,
    isFinal: Boolean,
    onEvent: (CekEvent) -> Unit,
) {
    val tidakAktif = isFinal || state.isSendingReply
    val bisaKirim = !tidakAktif && state.replyText.isNotBlank()
    val labelBalas = stringResource(R.string.placeholder_balas)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ShadcnTextField(
                value = state.replyText,
                onValueChange = { onEvent(CekEvent.ReplyChanged(it)) },
                placeholder = labelBalas,
                enabled = !tidakAktif,
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = labelBalas },
            )
            Box(
                modifier = Modifier
                    .size(48.dp)
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
        if (isFinal) {
            Text(
                text = stringResource(R.string.reply_disabled_closed),
                style = MaterialTheme.typography.labelSmall,
                color = MelatiMutedForeground,
            )
        }
    }
}

// ===== Pratinjau =====

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
                                attachmentNames = listOf("penjelasan-data.pdf"),
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
