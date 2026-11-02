package com.example.melati.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.melati.R
import com.example.melati.domain.model.TicketStatus
import com.example.melati.domain.model.TicketType
import com.example.melati.ui.theme.MelatiDestructive
import com.example.melati.ui.theme.MelatiForeground
import com.example.melati.ui.theme.MelatiMutedForeground
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Format tanggal ISO yang toleran: Instant, OffsetDateTime, LocalDateTime,
 * atau LocalDate. Mengembalikan teks berbahasa Indonesia ("27 Okt 2026"),
 * atau string mentah bila tidak dapat diurai.
 */
internal fun formatTanggalIndo(iso: String?): String? {
    if (iso.isNullOrBlank()) return null
    val locale = Locale.forLanguageTag("id-ID")
    val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
        .withLocale(locale)
        .withZone(ZoneId.systemDefault())
    return try {
        tryInstant(iso, formatter)
            ?: tryOffset(iso, formatter)
            ?: tryLocal(iso, locale)
    } catch (_: Exception) {
        iso
    }
}

private fun tryInstant(iso: String, formatter: DateTimeFormatter): String? = runCatching {
    formatter.format(Instant.parse(iso))
}.getOrNull()

private fun tryOffset(iso: String, formatter: DateTimeFormatter): String? = runCatching {
    formatter.format(OffsetDateTime.parse(iso).toInstant())
}.getOrNull()

private fun tryLocal(iso: String, locale: Locale): String? {
    val localDate = runCatching { LocalDate.parse(iso) }.getOrNull()
    if (localDate != null) {
        val dateOnly = DateTimeFormatter.ofPattern("d MMMM yyyy", locale)
        return localDate.format(dateOnly)
    }
    val localDateTime = runCatching { LocalDateTime.parse(iso) }.getOrNull() ?: return null
    val local = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(locale)
    return local.format(localDateTime)
}

/**
 * Kartu ringkasan tiket untuk Riwayat (PRD §5.3).
 *
 * Isi kartu dipisah dari tombol hapus agar klik tidak bersarang: area teks
 * membuka detail tiket, ikon hapus memicu konfirmasi penghapusan.
 */
@Composable
fun TicketCard(
    number: String,
    type: TicketType,
    status: TicketStatus,
    summary: String,
    createdAt: String?,
    onClick: () -> Unit,
    onDelete: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    ShadcnCard(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onClick)
                    .padding(start = 16.dp, top = 14.dp, bottom = 14.dp, end = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = number,
                        style = MaterialTheme.typography.titleMedium,
                        color = MelatiForeground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Row(modifier = Modifier.padding(start = 8.dp)) {
                        StatusBadge(status = status)
                    }
                }
                Text(
                    text = type.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MelatiMutedForeground,
                )
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MelatiForeground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
                formatTanggalIndo(createdAt)?.let { tanggal ->
                    Text(
                        text = tanggal,
                        style = MaterialTheme.typography.labelSmall,
                        color = MelatiMutedForeground,
                    )
                }
            }
            if (onDelete != null) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .padding(top = 6.dp, end = 6.dp)
                        .size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.action_hapus),
                        tint = MelatiDestructive,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}
