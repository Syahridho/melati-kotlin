package com.example.melati.ui.screens.laporan

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.melati.R
import com.example.melati.data.remote.ApiException
import com.example.melati.domain.model.Attachment
import com.example.melati.domain.model.LayananType
import com.example.melati.domain.model.TicketDraft
import com.example.melati.domain.model.TicketType
import com.example.melati.domain.repository.TicketRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class LaporanViewModel(
    private val repository: TicketRepository,
    private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(LaporanUiState())
    val state: StateFlow<LaporanUiState> = _state.asStateFlow()

    fun onEvent(event: LaporanEvent) {
        when (event) {
            is LaporanEvent.TypeSelected -> _state.update {
                it.copy(
                    type = event.type,
                    // Lapor hanya punya satu sub-layanan (PST), jadi langsung
                    // dipilihkan agar pengguna tidak perlu memilih lagi.
                    layanan = if (event.type == TicketType.LAPOR) LayananType.PST else null,
                    fieldErrors = it.fieldErrors - LaporanField.TYPE - LaporanField.LAYANAN,
                    serverFieldErrors = it.serverFieldErrors - LaporanField.TYPE - LaporanField.LAYANAN
                )
            }

            is LaporanEvent.NameChanged -> updateField(LaporanField.NAME) { it.copy(name = event.value) }
            is LaporanEvent.EmailChanged -> updateField(LaporanField.EMAIL) { it.copy(email = event.value) }
            is LaporanEvent.PhoneChanged -> updateField(LaporanField.PHONE) { it.copy(phone = event.value) }
            is LaporanEvent.SubjectChanged -> updateField(LaporanField.SUBJECT) { it.copy(subject = event.value) }
            is LaporanEvent.BodyChanged -> updateField(LaporanField.BODY) { it.copy(body = event.value) }

            is LaporanEvent.AttachmentsPicked -> addAttachments(event.uris)
            is LaporanEvent.AttachmentRemoved -> removeAttachment(event.attachment)

            LaporanEvent.Submit -> submit()
            LaporanEvent.AttachmentErrorShown -> _state.update { it.copy(attachmentError = null) }
            LaporanEvent.GeneralErrorShown -> _state.update { it.copy(generalError = null) }
            LaporanEvent.CreatedTicketDismissed -> resetForm()
        }
    }

    private fun updateField(field: LaporanField, transform: (LaporanUiState) -> LaporanUiState) {
        _state.update { current ->
            transform(current).copy(
                fieldErrors = current.fieldErrors - field,
                serverFieldErrors = current.serverFieldErrors - field
            )
        }
    }

    // ---------------------------------------------------------------------
    // Lampiran
    // ---------------------------------------------------------------------

    private fun addAttachments(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val current = _state.value.attachments
            val remaining = (Attachment.MAX_FILES - current.size).coerceAtLeast(0)
            if (remaining == 0) {
                _state.update { it.copy(attachmentError = FormError.FILE_COUNT) }
                return@launch
            }

            val accepted = mutableListOf<Attachment>()
            var error: FormError? = null
            val toProcess = uris.take(remaining)
            if (uris.size > remaining) error = FormError.FILE_COUNT

            for (uri in toProcess) {
                when (val result = withContext(Dispatchers.IO) { resolveAttachment(uri) }) {
                    is AttachmentResult.Ok -> accepted += result.attachment
                    is AttachmentResult.Rejected -> error = error ?: result.reason
                }
            }

            if (accepted.isNotEmpty() || error != null) {
                _state.update {
                    it.copy(
                        attachments = it.attachments + accepted,
                        attachmentError = error
                    )
                }
            }
        }
    }

    private fun removeAttachment(attachment: Attachment) {
        attachment.uri.path?.let { runCatching { File(it).delete() } }
        _state.update { it.copy(attachments = it.attachments - attachment) }
    }

    private sealed interface AttachmentResult {
        data class Ok(val attachment: Attachment) : AttachmentResult
        data class Rejected(val reason: FormError) : AttachmentResult
    }

    private fun resolveAttachment(uri: Uri): AttachmentResult {
        val resolver = context.contentResolver
        var displayName = "lampiran"
        var declaredSize = -1L

        runCatching {
            resolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIndex >= 0) displayName = cursor.getString(nameIndex) ?: displayName
                    if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) declaredSize = cursor.getLong(sizeIndex)
                }
            }
        }

        if (declaredSize > Attachment.MAX_BYTES_PER_FILE) {
            return AttachmentResult.Rejected(FormError.FILE_SIZE)
        }

        val mimeType = normalizeMime(resolver.getType(uri) ?: guessMime(displayName))
            ?: return AttachmentResult.Rejected(FormError.FILE_TYPE)

        if (mimeType !in Attachment.ALLOWED_MIME_TYPES) {
            return AttachmentResult.Rejected(FormError.FILE_TYPE)
        }

        val directory = File(context.cacheDir, "attachments").apply { mkdirs() }
        val safeName = displayName.replace(Regex("[^A-Za-z0-9._-]"), "_").take(80)
        val target = File(directory, "${UUID.randomUUID()}_$safeName")

        val copied = runCatching {
            resolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            } ?: return AttachmentResult.Rejected(FormError.FILE_TYPE)
        }.isSuccess

        if (!copied) {
            target.delete()
            return AttachmentResult.Rejected(FormError.FILE_TYPE)
        }

        val size = target.length()
        if (size <= 0L || size > Attachment.MAX_BYTES_PER_FILE) {
            target.delete()
            return AttachmentResult.Rejected(FormError.FILE_SIZE)
        }

        return AttachmentResult.Ok(
            Attachment(
                uri = Uri.fromFile(target),
                fileName = displayName,
                mimeType = mimeType,
                sizeBytes = size
            )
        )
    }

    private fun normalizeMime(raw: String?): String? = when (raw?.lowercase()) {
        "image/jpg", "image/pjpeg" -> "image/jpeg"
        "application/x-pdf" -> "application/pdf"
        else -> raw
    }

    private fun guessMime(fileName: String): String? = when (fileName.substringAfterLast('.', "").lowercase()) {
        "jpg", "jpeg" -> "image/jpeg"
        "pdf" -> "application/pdf"
        else -> null
    }

    // ---------------------------------------------------------------------
    // Submit
    // ---------------------------------------------------------------------

    private fun submit() {
        val current = _state.value
        val errors = validate(current)
        if (errors.isNotEmpty()) {
            _state.update { it.copy(fieldErrors = errors) }
            return
        }

        val draft = TicketDraft(
            type = current.type ?: return,
            layanan = current.layanan,
            reporterName = current.name.trim(),
            reporterEmail = current.email.trim().takeIf { it.isNotEmpty() },
            reporterPhone = current.phone.trim().takeIf { it.isNotEmpty() },
            subject = current.subject.trim().takeIf { it.isNotEmpty() },
            body = current.body.trim(),
            attachments = current.attachments
        )

        viewModelScope.launch {
            _state.update {
                it.copy(
                    isSubmitting = true,
                    generalError = null,
                    fieldErrors = emptyMap(),
                    serverFieldErrors = emptyMap()
                )
            }
            repository.createTicket(draft).fold(
                onSuccess = { created ->
                    draft.attachments.forEach { attachment ->
                        attachment.uri.path?.let { runCatching { File(it).delete() } }
                    }
                    _state.update {
                        it.copy(
                            isSubmitting = false,
                            createdTicket = created,
                            attachments = emptyList()
                        )
                    }
                },
                onFailure = { throwable ->
                    val api = throwable as? ApiException
                    _state.update {
                        it.copy(
                            isSubmitting = false,
                            generalError = api?.let(::errorMessage)
                                ?: context.getString(R.string.error_generic),
                            serverFieldErrors = mapServerErrors(api)
                        )
                    }
                }
            )
        }
    }

    private fun validate(state: LaporanUiState): Map<LaporanField, FormError> {
        val errors = mutableMapOf<LaporanField, FormError>()

        if (state.type == null) errors[LaporanField.TYPE] = FormError.REQUIRED
        if (state.type == TicketType.LAPOR && state.layanan == null) {
            errors[LaporanField.LAYANAN] = FormError.REQUIRED
        }
        if (state.name.isBlank()) errors[LaporanField.NAME] = FormError.REQUIRED
        // Server menolak tiket tanpa `title` (422 "Kolom title wajib diisi"),
        // meski PRD menyebutnya opsional — validasi klien mengikuti server.
        if (state.subject.isBlank()) errors[LaporanField.SUBJECT] = FormError.REQUIRED
        if (state.body.isBlank()) errors[LaporanField.BODY] = FormError.REQUIRED

        val email = state.email.trim()
        if (email.isNotEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            errors[LaporanField.EMAIL] = FormError.INVALID_EMAIL
        }

        val phone = state.phone.trim()
        if (phone.isNotEmpty() && !PHONE_PATTERN.matches(phone)) {
            errors[LaporanField.PHONE] = FormError.INVALID_PHONE
        }

        return errors
    }

    private fun errorMessage(api: ApiException): String {
        val serverMessage = api.serverMessage
        return when {
            api.isConnectivityIssue -> context.getString(R.string.error_network)
            api.isUnauthorized -> context.getString(R.string.error_unauthorized)
            !serverMessage.isNullOrBlank() -> serverMessage
            else -> context.getString(R.string.error_generic)
        }
    }

    /**
     * Memetakan error validasi Laravel ke field form.
     *
     * Kunci yang dipakai server adalah nama field multipart sebenarnya
     * (`reporter_name`, `reporter_wa`, `title`, `content`, …), bukan nama
     * Indonesia — lihat docs/API-ASSUMPTIONS.md. Karena itu pencocokan
     * dilakukan setelah membuang `_` dan `.` (mis. `attachments.0`).
     */
    private fun mapServerErrors(api: ApiException?): Map<LaporanField, String> {
        val errors = api?.fieldErrors ?: return emptyMap()
        return errors.mapNotNull { (key, message) ->
            val field = when (key.lowercase().replace("_", "").replace(".", "")) {
                "name", "nama", "reportername" -> LaporanField.NAME
                "email", "reporteremail" -> LaporanField.EMAIL
                "phone", "telepon", "nohp", "notelp", "wa", "whatsapp", "reporterwa", "reporterphone" -> LaporanField.PHONE
                "subject", "judul", "title" -> LaporanField.SUBJECT
                "message", "body", "isi", "laporan", "content" -> LaporanField.BODY
                "type", "klasifikasi", "classification" -> LaporanField.TYPE
                "service", "layanan", "servicetype" -> LaporanField.LAYANAN
                else -> null
            } ?: return@mapNotNull null
            field to message
        }.toMap()
    }

    private fun resetForm() {
        _state.value = LaporanUiState()
    }

    private companion object {
        val PHONE_PATTERN = Regex("^\\+?[0-9\\s\\-]{8,20}$")
    }
}
