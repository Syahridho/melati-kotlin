package com.example.melati.ui.screens.laporan

import com.example.melati.domain.model.Attachment
import com.example.melati.domain.model.LayananType
import com.example.melati.domain.model.TicketCreated
import com.example.melati.domain.model.TicketType

/** Field form yang bisa menghasilkan error validasi. */
enum class LaporanField { TYPE, LAYANAN, NAME, EMAIL, PHONE, SUBJECT, BODY }

/** Jenis kesalahan form; pemetaan ke teks dilakukan di lapisan UI. */
enum class FormError { REQUIRED, INVALID_EMAIL, INVALID_PHONE, FILE_TYPE, FILE_SIZE, FILE_COUNT, TOO_LONG }

data class LaporanUiState(
    val type: TicketType? = null,
    val layanan: LayananType? = null,
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val subject: String = "",
    val body: String = "",
    val attachments: List<Attachment> = emptyList(),
    val isSubmitting: Boolean = false,
    val fieldErrors: Map<LaporanField, FormError> = emptyMap(),
    val attachmentError: FormError? = null,
    val generalError: String? = null,
    val serverFieldErrors: Map<LaporanField, String> = emptyMap(),
    val createdTicket: TicketCreated? = null
) {
    /** Tombol Kirim nonaktif saat upload berjalan (PRD §5.1). */
    val canSubmit: Boolean get() = !isSubmitting
}

sealed interface LaporanEvent {
    data class TypeSelected(val type: TicketType) : LaporanEvent
    data class NameChanged(val value: String) : LaporanEvent
    data class EmailChanged(val value: String) : LaporanEvent
    data class PhoneChanged(val value: String) : LaporanEvent
    data class SubjectChanged(val value: String) : LaporanEvent
    data class BodyChanged(val value: String) : LaporanEvent
    data class AttachmentsPicked(val uris: List<android.net.Uri>) : LaporanEvent
    data class AttachmentRemoved(val attachment: Attachment) : LaporanEvent
    data object Submit : LaporanEvent
    data object AttachmentErrorShown : LaporanEvent
    data object GeneralErrorShown : LaporanEvent
    data object CreatedTicketDismissed : LaporanEvent
}
