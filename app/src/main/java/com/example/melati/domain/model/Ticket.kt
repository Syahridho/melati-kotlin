package com.example.melati.domain.model

import android.net.Uri

/**
 * Klasifikasi tiket. Prefix nomor tiket mengikuti PRD §5.1.
 */
enum class TicketType(val prefix: String, val label: String, val apiValue: String) {
    LAPOR("L-", "Lapor", "pengaduan"),
    ASPIRASI("A-", "Aspirasi", "aspirasi"),
    PERMINTAAN_INFORMASI("I-", "Permintaan Informasi", "permintaan_informasi");

    companion object {
        /** Tebakan prefix dari nomor tiket; null bila tidak dikenali. */
        fun fromNumber(number: String): TicketType? =
            entries.firstOrNull { number.trim().startsWith(it.prefix, ignoreCase = true) }

        /** Pemetaan toleran terhadap representasi string dari API. */
        fun fromRaw(raw: String?): TicketType? {
            val value = raw?.trim()?.lowercase().orEmpty()
            return when {
                value.isEmpty() -> null
                value.contains("aspirasi") || value.contains("aspiration") -> ASPIRASI
                value.contains("informasi") || value.contains("information") || value == "i" -> PERMINTAAN_INFORMASI
                value.contains("lapor") || value.contains("report") || value.contains("pengaduan") || value == "l" -> LAPOR
                else -> null
            }
        }
    }
}

/** Sub-layanan yang hanya berlaku untuk klasifikasi [TicketType.LAPOR]. */
enum class LayananType(val label: String, val apiValue: String) {
    PST("PST", "pst"),
    LAINNYA("Lainnya", "lainnya");

    companion object {
        fun fromRaw(raw: String?): LayananType? {
            val value = raw?.trim()?.lowercase().orEmpty()
            return when {
                value.isEmpty() -> null
                value.contains("pst") -> PST
                value.contains("lain") || value.contains("other") -> LAINNYA
                else -> null
            }
        }
    }
}

/**
 * Status tiket. API belum mendefinisikan daftar status (PRD §13.4), sehingga
 * pemetaan dibuat toleran dan selalu punya fallback [UNKNOWN].
 */
enum class TicketStatus(val label: String) {
    NEW("Baru"),
    IN_PROGRESS("Diproses"),
    ANSWERED("Dijawab"),
    RESOLVED("Selesai"),
    CLOSED("Ditutup"),
    UNKNOWN("Tidak diketahui");

    /** Status final: balasan dan aksi selesai dinonaktifkan (PRD §5.2). */
    val isFinal: Boolean get() = this == RESOLVED || this == CLOSED

    companion object {
        fun fromRaw(raw: String?): TicketStatus {
            val value = raw?.trim()?.lowercase().orEmpty()
            return when {
                value.isEmpty() -> UNKNOWN
                value.contains("selesai") || value.contains("resolved") || value.contains("done") ||
                    value.contains("complete") -> RESOLVED
                value.contains("tutup") || value.contains("closed") -> CLOSED
                value.contains("dijawab") || value.contains("answered") || value.contains("respons") -> ANSWERED
                value.contains("proses") || value.contains("process") || value.contains("progress") ||
                    value.contains("tindak") -> IN_PROGRESS
                value.contains("baru") || value == "new" || value.contains("pending") ||
                    value.contains("menunggu") -> NEW
                else -> UNKNOWN
            }
        }
    }
}

/** Penulis pesan pada thread percakapan. */
enum class MessageAuthor { REPORTER, ADMIN }

/**
 * Lampiran yang dipilih pengguna. [uri] berasal dari Photo Picker / SAF, sehingga
 * akses baca harus dibuka (persistable permission) sebelum upload.
 */
data class Attachment(
    val uri: Uri,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long
) {
    companion object {
        const val MAX_FILES = 3
        const val MAX_BYTES_PER_FILE = 2L * 1024L * 1024L // 2 MB
        val ALLOWED_MIME_TYPES = setOf("image/jpeg", "application/pdf")
    }
}

/** Draf tiket dari form Laporan; belum dikirim ke server. */
data class TicketDraft(
    val type: TicketType,
    val layanan: LayananType?,
    val reporterName: String,
    val reporterEmail: String?,
    val reporterPhone: String?,
    val subject: String?,
    val body: String,
    val attachments: List<Attachment> = emptyList()
)

/** Hasil pembuatan tiket: nomor tiket yang harus disimpan pengguna. */
data class TicketCreated(
    val number: String,
    val type: TicketType
)

/** Satu pesan dalam thread tiket. */
data class TicketMessage(
    val id: String?,
    val body: String,
    val author: MessageAuthor,
    val createdAt: String? = null,
    val attachmentNames: List<String> = emptyList()
)

/** Detail tiket lengkap beserta percakapan. */
data class Ticket(
    val number: String,
    val type: TicketType,
    val layanan: LayananType? = null,
    val status: TicketStatus,
    val subject: String? = null,
    val body: String = "",
    val createdAt: String? = null,
    val messages: List<TicketMessage> = emptyList()
)

/** Ringkasan tiket yang disimpan di Riwayat lokal (PRD §5.3). */
data class TicketSummary(
    val number: String,
    val type: TicketType,
    val summary: String,
    val createdAt: String?,
    val status: TicketStatus,
    val savedAtEpochMillis: Long
)
