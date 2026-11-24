package com.example.melati.ui.screens.cek

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.melati.R
import com.example.melati.data.remote.ApiException
import com.example.melati.domain.model.Attachment
import com.example.melati.domain.model.Ticket
import com.example.melati.domain.repository.TicketRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class CekViewModel(
    private val repository: TicketRepository,
    private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(CekUiState())
    val state: StateFlow<CekUiState> = _state.asStateFlow()

    private var lastLoadedNumber: String? = null

    /** Dipanggil dari navigasi saat pengguna membuka tiket dari Riwayat. */
    fun loadNumber(number: String) {
        val clean = number.trim()
        if (clean.isEmpty() || clean == lastLoadedNumber) return
        _state.update { it.copy(query = clean) }
        check(clean)
    }

    fun onEvent(event: CekEvent) {
        when (event) {
            is CekEvent.QueryChanged -> _state.update {
                it.copy(query = event.value, isQueryError = false)
            }

            CekEvent.Check -> check()
            CekEvent.Retry -> check()
            CekEvent.Refresh -> refresh()

            is CekEvent.ReplyChanged -> _state.update { it.copy(replyText = event.value) }
            is CekEvent.ReplyAttachmentsPicked -> addReplyAttachments(event.uris)
            is CekEvent.ReplyAttachmentRemoved -> removeReplyAttachment(event.attachment)
            CekEvent.SendReply -> sendReply()

            CekEvent.RequestComplete -> _state.update { it.copy(showCompleteConfirm = true) }
            CekEvent.DismissCompleteConfirm -> _state.update { it.copy(showCompleteConfirm = false) }
            CekEvent.ConfirmComplete -> {
                _state.update { it.copy(showCompleteConfirm = false) }
                complete()
            }

            CekEvent.MessageShown -> _state.update { it.copy(message = null) }
        }
    }

    private fun check(number: String? = null) {
        val query = (number ?: _state.value.query).trim()
        if (query.isEmpty()) {
            _state.update { it.copy(isQueryError = true) }
            return
        }

        viewModelScope.launch {
            _state.update {
                it.copy(
                    phase = CekPhase.Loading,
                    isQueryError = false,
                    message = null,
                )
            }
            repository.checkTicket(query).fold(
                onSuccess = { ticket ->
                    lastLoadedNumber = ticket.number
                    _state.update {
                        it.copy(query = ticket.number, phase = CekPhase.Loaded(ticket))
                    }
                },
                onFailure = { throwable -> handleCheckFailure(query, throwable) }
            )
        }
    }

    private fun refresh() {
        val ticket = _state.value.ticket ?: return
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }
            repository.checkTicket(ticket.number).fold(
                onSuccess = { updated ->
                    _state.update { it.copy(phase = CekPhase.Loaded(updated), isRefreshing = false) }
                },
                onFailure = {
                    _state.update {
                        it.copy(
                            isRefreshing = false,
                            message = context.getString(R.string.refresh_gagal)
                        )
                    }
                }
            )
        }
    }

    private fun sendReply() {
        val current = _state.value
        val ticket = current.ticket ?: return
        val text = current.replyText.trim()
        if (text.length < 5 || ticket.status.isFinal || current.isSendingReply ||
            current.isResolvingReplyAttachments
        ) return

        // Kunci sinkron sebelum launch agar tap ganda tidak memicu beberapa
        // request/dialog (server juga membatasi 10 balasan/menit).
        _state.update { it.copy(isSendingReply = true) }
        viewModelScope.launch {
            performReply(ticket, text)
        }
    }

    private suspend fun performReply(ticket: Ticket, text: String) {
        _state.update { it.copy(isSendingReply = true) }
        val attachments = _state.value.replyAttachments
        repository.replyToTicket(ticket.number, text, attachments).fold(
            onSuccess = {
                // Balasan sudah terkirim; file cache tidak diperlukan lagi.
                attachments.forEach { attachment ->
                    attachment.uri.path?.let { runCatching { File(it).delete() } }
                }
                _state.update {
                    it.copy(
                        isSendingReply = false,
                        replyText = "",
                        replyAttachments = emptyList(),
                        replyAttachmentError = null,
                        message = context.getString(R.string.reply_sent)
                    )
                }
                reloadSilently(ticket.number)
            },
            onFailure = { throwable -> handleReplyFailure(throwable) }
        )
    }

    private fun handleReplyFailure(throwable: Throwable) {
        val error = messageFor(throwable, R.string.reply_gagal)
        _state.update {
            it.copy(isSendingReply = false, message = error)
        }
    }

    private fun addReplyAttachments(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val current = _state.value
        if (current.isResolvingReplyAttachments || current.isSendingReply) return

        _state.update {
            it.copy(isResolvingReplyAttachments = true, replyAttachmentError = null)
        }
        viewModelScope.launch {
            val remaining = (Attachment.MAX_FILES - _state.value.replyAttachments.size)
                .coerceAtLeast(0)
            if (remaining == 0) {
                _state.update {
                    it.copy(
                        isResolvingReplyAttachments = false,
                        replyAttachmentError = ReplyAttachmentError.FILE_COUNT
                    )
                }
                return@launch
            }

            val accepted = mutableListOf<Attachment>()
            var error: ReplyAttachmentError? = null
            val toProcess = uris.take(remaining)
            if (uris.size > remaining) error = ReplyAttachmentError.FILE_COUNT

            for (uri in toProcess) {
                when (val result = withContext(Dispatchers.IO) { resolveReplyAttachment(uri) }) {
                    is ReplyAttachmentResult.Ok -> accepted += result.attachment
                    is ReplyAttachmentResult.Rejected -> error = error ?: result.reason
                }
            }

            _state.update {
                it.copy(
                    replyAttachments = it.replyAttachments + accepted,
                    replyAttachmentError = error,
                    isResolvingReplyAttachments = false
                )
            }
        }
    }

    private fun removeReplyAttachment(attachment: Attachment) {
        if (_state.value.isSendingReply) return
        attachment.uri.path?.let { path -> runCatching { File(path).delete() } }
        _state.update {
            it.copy(
                replyAttachments = it.replyAttachments - attachment,
                replyAttachmentError = null
            )
        }
    }

    private sealed interface ReplyAttachmentResult {
        data class Ok(val attachment: Attachment) : ReplyAttachmentResult
        data class Rejected(val reason: ReplyAttachmentError) : ReplyAttachmentResult
    }

    /** Validasi dan salin URI ke cache, sama seperti lampiran pada form Laporan. */
    private fun resolveReplyAttachment(uri: Uri): ReplyAttachmentResult {
        val resolver = context.contentResolver
        var displayName = "lampiran"
        var declaredSize = -1L

        runCatching {
            resolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIndex >= 0) displayName = cursor.getString(nameIndex) ?: displayName
                    if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                        declaredSize = cursor.getLong(sizeIndex)
                    }
                }
            }
        }

        if (declaredSize > Attachment.MAX_BYTES_PER_FILE) {
            return ReplyAttachmentResult.Rejected(ReplyAttachmentError.FILE_SIZE)
        }

        val mimeType = normalizeReplyMime(resolver.getType(uri) ?: guessReplyMime(displayName))
            ?: return ReplyAttachmentResult.Rejected(ReplyAttachmentError.FILE_TYPE)
        if (mimeType !in Attachment.ALLOWED_MIME_TYPES) {
            return ReplyAttachmentResult.Rejected(ReplyAttachmentError.FILE_TYPE)
        }

        val directory = File(context.cacheDir, "attachments").apply { mkdirs() }
        val safeName = displayName.replace(Regex("[^A-Za-z0-9._-]"), "_").take(80)
        val target = File(directory, "${UUID.randomUUID()}_$safeName")

        val copied = runCatching {
            resolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            } ?: return ReplyAttachmentResult.Rejected(ReplyAttachmentError.FILE_TYPE)
        }.isSuccess

        if (!copied) {
            target.delete()
            return ReplyAttachmentResult.Rejected(ReplyAttachmentError.FILE_TYPE)
        }

        val size = target.length()
        if (size <= 0L || size > Attachment.MAX_BYTES_PER_FILE) {
            target.delete()
            return ReplyAttachmentResult.Rejected(ReplyAttachmentError.FILE_SIZE)
        }

        return ReplyAttachmentResult.Ok(
            Attachment(
                uri = Uri.fromFile(target),
                fileName = displayName,
                mimeType = mimeType,
                sizeBytes = size
            )
        )
    }

    private fun normalizeReplyMime(raw: String?): String? = when (raw?.lowercase()) {
        "image/jpg", "image/pjpeg" -> "image/jpeg"
        "image/png" -> "image/png"
        "application/x-pdf" -> "application/pdf"
        else -> raw
    }

    private fun guessReplyMime(fileName: String): String? =
        when (fileName.substringAfterLast('.', "").lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "pdf" -> "application/pdf"
            else -> null
        }

    private fun complete() {
        val ticket = _state.value.ticket ?: return
        if (_state.value.isCompleting) return

        viewModelScope.launch {
            _state.update { it.copy(isCompleting = true) }
            repository.completeTicket(ticket.number).fold(
                onSuccess = {
                    _state.update {
                        it.copy(isCompleting = false, message = context.getString(R.string.ticket_completed))
                    }
                    reloadSilently(ticket.number)
                },
                onFailure = { throwable ->
                    _state.update {
                        it.copy(
                            isCompleting = false,
                            message = messageFor(throwable, R.string.complete_gagal)
                        )
                    }
                }
            )
        }
    }

    private suspend fun reloadSilently(number: String) {
        repository.checkTicket(number).onSuccess { updated ->
            lastLoadedNumber = updated.number
            _state.update { it.copy(phase = CekPhase.Loaded(updated)) }
        }
    }

    private fun handleCheckFailure(query: String, throwable: Throwable) {
        val api = throwable as? ApiException
        val phase = when {
            api?.isNotFound == true -> CekPhase.NotFound(query)
            else -> CekPhase.Failed(messageFor(throwable, R.string.error_generic))
        }
        _state.update { it.copy(phase = phase) }
    }

    private fun messageFor(throwable: Throwable, fallback: Int): String {
        val api = throwable as? ApiException ?: return context.getString(fallback)
        val serverMessage = api.serverMessage
        val detail = buildString {
            append(serverMessage.orEmpty())
            append(' ')
            append(api.fieldErrors.values.joinToString(" "))
        }.lowercase()
        val ticketFinished = listOf("selesai", "closed", "complete").any(detail::contains) ||
            (api.httpCode == 400 && serverMessage.isNullOrBlank())
        return when {
            api.isConnectivityIssue -> context.getString(R.string.error_network)
            api.isUnauthorized -> context.getString(R.string.error_unauthorized)
            ticketFinished -> context.getString(R.string.error_ticket_finished)
            api.httpCode == 404 -> context.getString(R.string.cek_not_found_title)
            api.httpCode == 422 -> {
                if (!serverMessage.isNullOrBlank()) serverMessage
                else context.getString(fallback)
            }
            api.httpCode == 429 -> context.getString(R.string.error_too_many_attempts)
            !serverMessage.isNullOrBlank() -> serverMessage
            else -> context.getString(fallback)
        }
    }
}
