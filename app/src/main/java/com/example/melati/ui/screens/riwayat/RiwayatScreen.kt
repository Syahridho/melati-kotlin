package com.example.melati.ui.screens.riwayat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.melati.R
import com.example.melati.domain.model.TicketStatus
import com.example.melati.domain.model.TicketSummary
import com.example.melati.domain.model.TicketType
import com.example.melati.ui.components.ShadcnAlertDialog
import com.example.melati.ui.components.ShadcnScreenHeader
import com.example.melati.ui.components.ShadcnStateCard
import com.example.melati.ui.components.ShadcnTextButton
import com.example.melati.ui.components.TicketCard
import com.example.melati.ui.theme.MelatiDestructive
import com.example.melati.ui.theme.MelatiForeground
import com.example.melati.ui.theme.MelatiMutedForeground
import com.example.melati.ui.theme.MelatiPrimary
import com.example.melati.ui.theme.MelatiTheme

private val ScreenPadding = 20.dp

@Composable
fun RiwayatScreen(
    state: RiwayatUiState,
    onEvent: (RiwayatEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = ScreenPadding)
            .padding(top = 16.dp, bottom = 16.dp),
    ) {
        // ===== Header + aksi hapus semua =====
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            ShadcnScreenHeader(
                title = stringResource(R.string.riwayat_title),
                subtitle = stringResource(R.string.riwayat_notice),
                modifier = Modifier.weight(1f),
            )
            if (state.items.isNotEmpty()) {
                ShadcnTextButton(
                    text = stringResource(R.string.action_hapus_semua),
                    onClick = { onEvent(RiwayatEvent.RequestClearAll) },
                    color = MelatiDestructive,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }

        Spacer(Modifier.size(16.dp))

        // ===== Konten =====
        if (state.isLoading) {
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
        } else if (state.items.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                ShadcnStateCard(
                    title = stringResource(R.string.riwayat_empty_title),
                    message = stringResource(R.string.riwayat_empty_message),
                    icon = Icons.AutoMirrored.Filled.List,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp, top = 4.dp),
            ) {
                items(state.items, key = { it.number }) { item ->
                    TicketCard(
                        number = item.number,
                        type = item.type,
                        status = item.status,
                        summary = item.summary,
                        createdAt = item.createdAt,
                        onClick = { onEvent(RiwayatEvent.Open(item.number)) },
                        onDelete = { onEvent(RiwayatEvent.RequestDelete(item.number)) },
                    )
                }
            }
        }
    }

    // ===== Dialog konfirmasi hapus satu tiket =====
    state.pendingDeleteNumber?.let { nomor ->
        ShadcnAlertDialog(
            title = stringResource(R.string.hapus_confirm_title),
            message = stringResource(R.string.hapus_confirm_message),
            extraContent = {
                Text(
                    text = nomor,
                    style = MaterialTheme.typography.titleMedium,
                    color = MelatiForeground,
                )
            },
            confirmLabel = stringResource(R.string.action_hapus),
            onConfirm = { onEvent(RiwayatEvent.ConfirmDelete) },
            dismissLabel = stringResource(R.string.action_batal),
            onDismiss = { onEvent(RiwayatEvent.DismissDelete) },
            destructive = true,
        )
    }

    // ===== Dialog konfirmasi hapus semua =====
    if (state.showClearAllConfirm) {
        ShadcnAlertDialog(
            title = stringResource(R.string.hapus_semua_confirm_title),
            message = stringResource(R.string.hapus_semua_confirm_message),
            confirmLabel = stringResource(R.string.action_hapus_semua),
            onConfirm = { onEvent(RiwayatEvent.ConfirmClearAll) },
            dismissLabel = stringResource(R.string.action_batal),
            onDismiss = { onEvent(RiwayatEvent.DismissClearAll) },
            destructive = true,
        )
    }
}

// ===== Pratinjau =====

@Preview(name = "Riwayat berisi", showBackground = true, heightDp = 800)
@Composable
private fun PreviewRiwayatBerisi() {
    MelatiTheme {
        RiwayatScreen(
            state = RiwayatUiState(
                isLoading = false,
                items = listOf(
                    TicketSummary(
                        number = "L-2026-0001",
                        type = TicketType.LAPOR,
                        summary = "Layanan PST belum merespons permintaan data ketersediaan pangan.",
                        createdAt = "2026-10-20T09:30:00Z",
                        status = TicketStatus.IN_PROGRESS,
                        savedAtEpochMillis = 1_768_000_000_000,
                    ),
                    TicketSummary(
                        number = "A-2026-0102",
                        type = TicketType.ASPIRASI,
                        summary = "Usulan pemasangan informasi statistik di pusat data kabupaten.",
                        createdAt = "2026-10-24T14:00:00Z",
                        status = TicketStatus.NEW,
                        savedAtEpochMillis = 1_768_300_000_000,
                    ),
                    TicketSummary(
                        number = "I-2026-0207",
                        type = TicketType.PERMINTAAN_INFORMASI,
                        summary = "Mohon data pertumbuhan ekonomi kabupaten tahun lalu.",
                        createdAt = "2026-10-12T10:20:00Z",
                        status = TicketStatus.ANSWERED,
                        savedAtEpochMillis = 1_768_400_000_000,
                    ),
                ),
            ),
            onEvent = {},
        )
    }
}

@Preview(name = "Riwayat kosong", showBackground = true)
@Composable
private fun PreviewRiwayatKosong() {
    MelatiTheme {
        RiwayatScreen(
            state = RiwayatUiState(isLoading = false, items = emptyList()),
            onEvent = {},
        )
    }
}
