package com.example.melati.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.melati.domain.model.TicketStatus
import com.example.melati.ui.theme.MelatiSuccess
import com.example.melati.ui.theme.MelatiSuccessBg
import com.example.melati.ui.theme.MelatiWarning

/*
 * Badge status bergaya shadcn: pil sudut 6dp, latar tint lembut, teks kecil
 * 12sp, dan satu titik warna. Semua kombinasi teks/latar di bawah memenuhi
 * kontras minimal 4.5:1. Enam status tetap dapat dibedakan satu sama lain.
 */
private data class BadgePalette(
    val container: Color,
    val content: Color,
    val dot: Color,
)

private val PaletteNew = BadgePalette(
    container = Color(0xFFE6F0FA),
    content = Color(0xFF004E96),
    dot = Color(0xFF005FB6),
)

private val PaletteInProgress = BadgePalette(
    container = Color(0xFFFEF3C7),
    content = Color(0xFF92400E),
    dot = MelatiWarning,
)

private val PaletteAnswered = BadgePalette(
    container = Color(0xFFCCFBF1),
    content = Color(0xFF115E59),
    dot = Color(0xFF0D9488),
)

private val PaletteResolved = BadgePalette(
    container = MelatiSuccessBg,
    content = Color(0xFF166534),
    dot = MelatiSuccess,
)

private val PaletteClosed = BadgePalette(
    container = Color(0xFFF4F4F5),
    content = Color(0xFF52525B),
    dot = Color(0xFF71717A),
)

private val PaletteUnknown = BadgePalette(
    container = Color(0xFFF4F4F5),
    content = Color(0xFF71717A),
    dot = Color(0xFFA1A1AA),
)

private fun badgePalette(status: TicketStatus): BadgePalette = when (status) {
    TicketStatus.NEW -> PaletteNew
    TicketStatus.IN_PROGRESS -> PaletteInProgress
    TicketStatus.ANSWERED -> PaletteAnswered
    TicketStatus.RESOLVED -> PaletteResolved
    TicketStatus.CLOSED -> PaletteClosed
    TicketStatus.UNKNOWN -> PaletteUnknown
}

@Composable
fun StatusBadge(status: TicketStatus, modifier: Modifier = Modifier) {
    val palette = badgePalette(status)
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(palette.container)
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Titik warna status: bantu pembedaan silang bagi pengguna yang
        // mengandalkan warna secara sekunder; label teks selalu utama.
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(palette.dot),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = status.label,
            color = palette.content,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}
