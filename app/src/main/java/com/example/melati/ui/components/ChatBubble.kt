package com.example.melati.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.melati.R
import com.example.melati.domain.model.MessageAuthor
import com.example.melati.domain.model.TicketMessage
import com.example.melati.ui.theme.MelatiBorder
import com.example.melati.ui.theme.MelatiForeground
import com.example.melati.ui.theme.MelatiMuted
import com.example.melati.ui.theme.MelatiMutedForeground
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
                if (message.attachmentNames.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.label_lampiran_pesan),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (fromReporter) MelatiOnPrimaryMuted else MelatiMutedForeground,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    message.attachmentNames.forEach { nama ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (fromReporter) Color.White else MelatiForeground,
                                modifier = Modifier.padding(end = 4.dp),
                            )
                            Text(
                                text = nama,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (fromReporter) Color.White else MelatiForeground,
                            )
                        }
                    }
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
}
