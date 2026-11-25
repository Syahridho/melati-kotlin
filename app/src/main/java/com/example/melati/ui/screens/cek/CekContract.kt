package com.example.melati.ui.screens.cek

import android.net.Uri
import com.example.melati.domain.model.Attachment
import com.example.melati.domain.model.Ticket

/** Fase layar Cek; menentukan konten utama yang ditampilkan. */
sealed interface CekPhase {
    data object Idle : CekPhase

    data object Loading : CekPhase

    data class NotFound(val query: String) : CekPhase

    data class Loaded(val ticket: Ticket) : CekPhase

    data class Failed(val message: String) : CekPhase
}

/** Kesalahan lampiran balasan; pemetaan ke teks dilakukan di layar. */
enum class ReplyAttachmentError { FILE_TYPE, FILE_SIZE, FILE_COUNT }

data class CekUiState(
    val query: String = "",
    val phase: CekPhase = CekPhase.Idle,
    val isQueryError: Boolean = false,
    val replyText: String = "",
    val replyAttachments: List<Attachment> = emptyList(),
    val replyAttachmentError: ReplyAttachmentError? = null,
    val isResolvingReplyAttachments: Boolean = false,
    val isSendingReply: Boolean = false,
    val isRefreshing: Boolean = false,
    val isCompleting: Boolean = false,
    val showCompleteConfirm: Boolean = false,
    val message: String? = null
) {
    val ticket: Ticket? get() = (phase as? CekPhase.Loaded)?.ticket
}

sealed interface CekEvent {
    data class QueryChanged(val value: String) : CekEvent
    data object Check : CekEvent
    data object Retry : CekEvent
    data object Refresh : CekEvent
    data class ReplyChanged(val value: String) : CekEvent
    data class ReplyAttachmentsPicked(val uris: List<Uri>) : CekEvent
    data class ReplyAttachmentRemoved(val attachment: Attachment) : CekEvent
    data object SendReply : CekEvent
    data object RequestComplete : CekEvent
    data object ConfirmComplete : CekEvent
    data object DismissCompleteConfirm : CekEvent
    data object MessageShown : CekEvent
    data object Reset : CekEvent
}
