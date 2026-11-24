@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.melati.ui.screens.laporan

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import android.content.ClipData
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.melati.R
import com.example.melati.domain.model.Attachment
import com.example.melati.domain.model.LayananType
import com.example.melati.domain.model.TicketType
import com.example.melati.ui.components.CopyIcon
import com.example.melati.ui.components.DashedPicker
import com.example.melati.ui.components.SectionLabel
import com.example.melati.ui.components.ShadcnButton
import com.example.melati.ui.components.ShadcnCard
import com.example.melati.ui.components.ShadcnCardBody
import com.example.melati.ui.components.ShadcnCardHeader
import com.example.melati.ui.components.ShadcnDialog
import com.example.melati.ui.components.ShadcnFileChip
import com.example.melati.ui.components.ShadcnOutlinedButton
import com.example.melati.ui.components.ShadcnRadioCard
import com.example.melati.ui.components.ShadcnRadioGroup
import com.example.melati.ui.components.ShadcnTextField
import com.example.melati.ui.components.ShadcnValueBox
import com.example.melati.ui.theme.*
import kotlinx.coroutines.launch

private val ScreenPadding = 20.dp

/** Tampilan tanggal kejadian mengikuti web: dd/mm/yyyy. */
private val TanggalKejadianFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

@Composable
fun LaporanScreen(
    state: LaporanUiState,
    onEvent: (LaporanEvent) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    // Picker lampiran: hanya meneruskan URI ke ViewModel. Metadata (nama,
    // ukuran, tipe) dan izin baca resolve-nya urusan ViewModel, bukan UI.
    val pickLampiran =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            if (uris.isNotEmpty()) {
                onEvent(LaporanEvent.AttachmentsPicked(uris))
            }
        }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ScreenPadding)
            .padding(top = 16.dp, bottom = 24.dp),
    ) {
        // Seluruh formulir berada dalam satu kartu putih: pita judul biru muda
        // di atas, lalu isian yang dipisahkan jarak 20dp.
        ShadcnCard {
            ShadcnCardHeader(
                title = stringResource(R.string.laporan_title),
                subtitle = stringResource(R.string.laporan_subtitle),
            )

            ShadcnCardBody(spacing = 20.dp) {
                // ===== Klasifikasi =====
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionLabel(
                        text = stringResource(R.string.label_klasifikasi),
                        error = state.serverFieldErrors[LaporanField.TYPE]
                            ?: errorTextFor(state.fieldErrors[LaporanField.TYPE]),
                    )
                    ShadcnRadioGroup {
                        TicketType.entries.forEach { tipe ->
                            ShadcnRadioCard(
                                text = tipe.label,
                                selected = state.type == tipe,
                                onClick = { onEvent(LaporanEvent.TypeSelected(tipe)) },
                            )
                        }
                    }
                }

                // Sub-layanan tidak lagi dipilih pengguna: klasifikasi Lapor
                // selalu memakai PST (ditetapkan di ViewModel).

                // ===== Data pelapor (semua opsional) =====
                ShadcnTextField(
                    value = state.name,
                    onValueChange = { onEvent(LaporanEvent.NameChanged(it)) },
                    label = stringResource(R.string.label_nama),
                    hint = stringResource(R.string.hint_opsional),
                    placeholder = stringResource(R.string.placeholder_nama),
                    error = state.serverFieldErrors[LaporanField.NAME]
                        ?: errorTextFor(state.fieldErrors[LaporanField.NAME]),
                    keyboardType = KeyboardType.Text,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next,
                    ),
                )
                ShadcnTextField(
                    value = state.email,
                    onValueChange = { onEvent(LaporanEvent.EmailChanged(it)) },
                    label = stringResource(R.string.label_email),
                    hint = stringResource(R.string.hint_opsional),
                    placeholder = stringResource(R.string.placeholder_email),
                    error = state.serverFieldErrors[LaporanField.EMAIL]
                        ?: errorTextFor(state.fieldErrors[LaporanField.EMAIL]),
                    keyboardType = KeyboardType.Email,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                    ),
                )
                ShadcnTextField(
                    value = state.phone,
                    onValueChange = { onEvent(LaporanEvent.PhoneChanged(it)) },
                    label = stringResource(R.string.label_telepon),
                    hint = stringResource(R.string.hint_opsional),
                    placeholder = stringResource(R.string.placeholder_telepon),
                    error = state.serverFieldErrors[LaporanField.PHONE]
                        ?: errorTextFor(state.fieldErrors[LaporanField.PHONE]),
                    keyboardType = KeyboardType.Phone,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Next,
                    ),
                )

                // ===== Tanggal kejadian (hanya Pengaduan) =====
                if (state.type == TicketType.LAPOR) {
                    var showDatePicker by remember { mutableStateOf(false) }

                    if (showDatePicker) {
                        val datePickerState = rememberDatePickerState(
                            initialSelectedDateMillis = state.tanggalKejadian
                                ?.atStartOfDay(ZoneOffset.UTC)
                                ?.toInstant()
                                ?.toEpochMilli(),
                            selectableDates = object : SelectableDates {
                                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                                    !Instant.ofEpochMilli(utcTimeMillis)
                                        .atZone(ZoneOffset.UTC)
                                        .toLocalDate()
                                        .isAfter(LocalDate.now())

                                override fun isSelectableYear(year: Int): Boolean =
                                    year <= LocalDate.now().year
                            },
                        )
                        DatePickerDialog(
                            onDismissRequest = { showDatePicker = false },
                            confirmButton = {
                                TextButton(onClick = {
                                    datePickerState.selectedDateMillis?.let { millis ->
                                        onEvent(
                                            LaporanEvent.TanggalKejadianChanged(
                                                Instant.ofEpochMilli(millis)
                                                    .atZone(ZoneOffset.UTC)
                                                    .toLocalDate()
                                            )
                                        )
                                    }
                                    showDatePicker = false
                                }) {
                                    Text(stringResource(R.string.action_pilih))
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDatePicker = false }) {
                                    Text(stringResource(R.string.action_tutup))
                                }
                            },
                        ) {
                            DatePicker(state = datePickerState)
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                ShadcnTextField(
                                    value = state.tanggalKejadian
                                        ?.format(TanggalKejadianFormat)
                                        .orEmpty(),
                                    onValueChange = {},
                                    label = stringResource(R.string.label_tanggal_kejadian),
                                    placeholder = stringResource(R.string.placeholder_tanggal_kejadian),
                                    readOnly = true,
                                    trailingIcon = {
                                        Icon(
                                            imageVector = Icons.Filled.DateRange,
                                            contentDescription = null,
                                            tint = MelatiMutedForeground,
                                        )
                                    },
                                    error = state.serverFieldErrors[LaporanField.TANGGAL_KEJADIAN]
                                        ?: errorTextFor(state.fieldErrors[LaporanField.TANGGAL_KEJADIAN]),
                                )
                                // Lapisan transparan: klik di mana pun pada field
                                // (termasuk label) membuka kalender, tanpa keyboard.
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                            onClick = { showDatePicker = true },
                                        ),
                                )
                            }
                            if (state.tanggalKejadian != null) {
                                IconButton(onClick = {
                                    onEvent(LaporanEvent.TanggalKejadianChanged(null))
                                }) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = stringResource(R.string.cd_hapus_tanggal),
                                        tint = MelatiMutedForeground,
                                    )
                                }
                            }
                        }
                        Text(
                            text = stringResource(R.string.hint_tanggal_kejadian),
                            style = MaterialTheme.typography.labelSmall,
                            color = MelatiMutedForeground,
                        )
                    }
                }

                // ===== Judul =====
                val judulLabel = when (state.type) {
                    TicketType.LAPOR -> stringResource(R.string.label_judul_pengaduan)
                    TicketType.ASPIRASI -> stringResource(R.string.label_judul_aspirasi)
                    TicketType.PERMINTAAN_INFORMASI -> stringResource(R.string.label_judul_permintaan_informasi)
                    else -> stringResource(R.string.label_judul)
                }
                val judulPlaceholder = when (state.type) {
                    TicketType.LAPOR -> stringResource(R.string.placeholder_judul_pengaduan)
                    TicketType.ASPIRASI -> stringResource(R.string.placeholder_judul_aspirasi)
                    TicketType.PERMINTAAN_INFORMASI -> stringResource(R.string.placeholder_judul_permintaan_informasi)
                    else -> ""
                }
                val isiPlaceholder = when (state.type) {
                    TicketType.LAPOR -> stringResource(R.string.placeholder_isi_pengaduan)
                    TicketType.ASPIRASI -> stringResource(R.string.placeholder_isi_aspirasi)
                    TicketType.PERMINTAAN_INFORMASI -> stringResource(R.string.placeholder_isi_permintaan_informasi)
                    else -> ""
                }
                ShadcnTextField(
                    value = state.subject,
                    onValueChange = { onEvent(LaporanEvent.SubjectChanged(it)) },
                    label = judulLabel,
                    placeholder = judulPlaceholder,
                    hint = stringResource(R.string.hint_wajib),
                    error = state.serverFieldErrors[LaporanField.SUBJECT]
                        ?: errorTextFor(state.fieldErrors[LaporanField.SUBJECT]),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next,
                    ),
                )

                // ===== Isi laporan =====
                val isiLabel = when (state.type) {
                        TicketType.LAPOR -> stringResource(R.string.label_isi_pengaduan)
                        TicketType.ASPIRASI -> stringResource(R.string.label_isi_aspirasi)
                        TicketType.PERMINTAAN_INFORMASI -> stringResource(R.string.label_isi_permintaan_informasi)
                        else -> stringResource(R.string.label_isi)
                    }
                    ShadcnTextField(
                        value = state.body,
                        onValueChange = { onEvent(LaporanEvent.BodyChanged(it)) },
                        label = isiLabel,
                        placeholder = isiPlaceholder,
                        error = state.serverFieldErrors[LaporanField.BODY]
                            ?: errorTextFor(state.fieldErrors[LaporanField.BODY]),
                        singleLine = false,
                        minLines = 5,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Default,
                        ),
                    )

                // ===== Lampiran =====
                LampiranSection(
                    state = state,
                    onRemove = { onEvent(LaporanEvent.AttachmentRemoved(it)) },
                    onPick = { pickLampiran.launch(arrayOf("image/jpeg", "image/png", "application/pdf")) },
                )

                // ===== Banner error umum =====
                state.generalError?.let { pesan ->
                    ErrorBanner(
                        message = pesan,
                        onDismiss = { onEvent(LaporanEvent.GeneralErrorShown) },
                    )
                }

                // ===== Kirim =====
                HorizontalDivider(thickness = 1.dp, color = MelatiBorder)
                ShadcnButton(
                    text = if (state.isSubmitting) {
                        stringResource(R.string.state_mengirim)
                    } else {
                        stringResource(R.string.action_kirim)
                    },
                    onClick = { onEvent(LaporanEvent.Submit) },
                    enabled = state.canSubmit,
                    loading = state.isSubmitting,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    CreatedTicketDialog(state, onEvent, snackbarHostState)
}

@Composable
private fun LampiranSection(
    state: LaporanUiState,
    onRemove: (Attachment) -> Unit,
    onPick: () -> Unit,
) {
    val penuh = state.attachments.size >= Attachment.MAX_FILES
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel(text = stringResource(R.string.label_lampiran))
        state.attachments.forEach { lampiran ->
            ShadcnFileChip(
                name = lampiran.fileName,
                meta = formatUkuran(lampiran.sizeBytes),
                onRemove = { onRemove(lampiran) },
            )
        }
        DashedPicker(
            text = stringResource(R.string.action_tambah_lampiran),
            onClick = onPick,
            enabled = !penuh,
        )
        Text(
            text = stringResource(R.string.lampiran_hint),
            style = MaterialTheme.typography.labelSmall,
            color = MelatiMutedForeground,
        )
        state.attachmentError?.let { galat ->
            val pesan = when (galat) {
                FormError.FILE_TYPE -> stringResource(R.string.error_file_type)
                FormError.FILE_SIZE -> stringResource(R.string.error_file_size)
                FormError.FILE_COUNT -> stringResource(R.string.error_file_count)
                else -> null
            }
            pesan?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MelatiDestructive,
                )
            }
        }
    }
}

/** Banner galat umum: latar merah muda lembut, garis tipis, satu tombol tutup. */
@Composable
private fun ErrorBanner(message: String, onDismiss: () -> Unit) {
    Surface(
        color = MelatiDestructiveTint,
        contentColor = MelatiForeground,
        shape = MaterialTheme.shapes.small,
        border = androidx.compose.foundation.BorderStroke(1.dp, MelatiDestructive.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, top = 2.dp, bottom = 2.dp, end = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = null,
                tint = MelatiDestructive,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.size(10.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MelatiForeground,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.action_tutup),
                    tint = MelatiMutedForeground,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

/**
 * Dialog nomor tiket: lingkaran centang hijau, judul + keterangan terpusat,
 * kotak nomor tiket abu, tombol salin bergaris, dan tombol tutup biru penuh.
 */
@Composable
private fun CreatedTicketDialog(
    state: LaporanUiState,
    onEvent: (LaporanEvent) -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    val tiketBaru = state.createdTicket ?: return
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val appName = stringResource(R.string.app_name)
    val copiedMessage = stringResource(R.string.ticket_number_copied)

    ShadcnDialog(onDismissRequest = { onEvent(LaporanEvent.CreatedTicketDismissed) }) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Tombol tutup (X) di sudut kanan atas kartu.
            IconButton(
                onClick = { onEvent(LaporanEvent.CreatedTicketDismissed) },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 12.dp, y = (-12).dp)
                    .size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.action_tutup),
                    tint = MelatiMutedForeground,
                    modifier = Modifier.size(20.dp),
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MelatiSuccessBg),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = MelatiSuccess,
                        modifier = Modifier.size(32.dp),
                    )
                }
                Text(
                    text = stringResource(R.string.ticket_created_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MelatiForeground,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.ticket_created_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MelatiMutedForeground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                ShadcnValueBox(
                    label = stringResource(R.string.label_nomor_tiket),
                    value = tiketBaru.number,
                    modifier = Modifier.padding(top = 4.dp),
                )
                ShadcnOutlinedButton(
                    text = stringResource(R.string.action_salin_nomor),
                    onClick = {
                        scope.launch {
                            clipboard.setClipEntry(
                                ClipEntry(
                                    ClipData.newPlainText(
                                        appName,
                                        tiketBaru.number,
                                    ),
                                ),
                            )
                            snackbarHostState.currentSnackbarData?.dismiss()
                            snackbarHostState.showSnackbar(
                                copiedMessage,
                            )
                        }
                    },
                    leadingIcon = CopyIcon,
                    modifier = Modifier.fillMaxWidth(),
                )
                ShadcnButton(
                    text = stringResource(R.string.action_tutup),
                    onClick = { onEvent(LaporanEvent.CreatedTicketDismissed) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** Peta FormError → teks; galat lampiran ditangani terpisah di LampiranSection. */
@Composable
private fun errorTextFor(error: FormError?): String? = when (error) {
    FormError.REQUIRED -> stringResource(R.string.error_required)
    FormError.INVALID_EMAIL -> stringResource(R.string.error_email_invalid)
    FormError.INVALID_PHONE -> stringResource(R.string.error_phone_invalid)
    FormError.INVALID_DATE -> stringResource(R.string.error_date_future)
    FormError.TOO_LONG -> stringResource(R.string.error_too_long)
    else -> null
}

private fun formatUkuran(bytes: Long): String {
    val lokalis = java.util.Locale("id", "ID")
    return when {
        bytes >= 1_000_000 -> String.format(lokalis, "%.1f MB", bytes / 1_000_000.0)
        bytes >= 1_000 -> String.format(lokalis, "%.1f KB", bytes / 1_000.0)
        else -> String.format(lokalis, "%d B", bytes)
    }
}

// ===== Pratinjau =====

@Preview(name = "Laporan terisi", showBackground = true, heightDp = 1400)
@Composable
private fun PreviewLaporanTerisi() {
    MelatiTheme {
        LaporanScreen(
            state = LaporanUiState(
                type = TicketType.LAPOR,
                layanan = LayananType.PST,
                name = "Rina Putri",
                email = "rina@mail.com",
                phone = "0812 3456 7890",
                subject = "Pertanyaan data PDRB",
                body = "Mohon informasi perkembangan data PDRB Provinsi Riau tahun terbaru.",
                attachments = listOf(
                    Attachment(
                        uri = Uri.parse("content://dokumen/lampiran-1.pdf"),
                        fileName = "dokumen-pendukung.pdf",
                        mimeType = "application/pdf",
                        sizeBytes = 220_000L,
                    ),
                ),
            ),
            onEvent = {},
            snackbarHostState = androidx.compose.material3.SnackbarHostState(),
        )
    }
}
