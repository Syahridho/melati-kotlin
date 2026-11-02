package com.example.melati.data.repository

import com.example.melati.BuildConfig
import com.example.melati.data.local.TicketHistoryStore
import com.example.melati.data.remote.ApiException
import com.example.melati.data.remote.MelatiApi
import com.example.melati.data.remote.TicketParser
import com.example.melati.data.remote.parseApiError
import com.example.melati.domain.model.Attachment
import com.example.melati.domain.model.Ticket
import com.example.melati.domain.model.TicketCreated
import com.example.melati.domain.model.TicketDraft
import com.example.melati.domain.model.TicketStatus
import com.example.melati.domain.model.TicketSummary
import com.example.melati.domain.repository.TicketRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import java.io.File
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * Implementasi tunggal [TicketRepository].
 *
 * Nama field multipart di bawah diambil dari form web Melati yang berjalan
 * (`classification`, `channel_id`, `reporter_name`, `reporter_email`,
 * `reporter_wa`, `title`, `content`, `service_type`, `attachments[]`).
 */
class TicketRepositoryImpl(
    private val api: MelatiApi,
    private val historyStore: TicketHistoryStore
) : TicketRepository {

    override val history: Flow<List<TicketSummary>> = historyStore.history

    override suspend fun createTicket(draft: TicketDraft): Result<TicketCreated> = runCatchingApi {
        val created = TicketParser.parseCreated(api.createTicket(buildCreateParts(draft)).successBodyOrThrow())
        historyStore.save(created.toSummary(draft))
        created
    }

    override suspend fun checkTicket(number: String): Result<Ticket> = runCatchingApi {
        val ticket = TicketParser.parseTicket(
            body = api.checkTicket(number).successBodyOrThrow(),
            fallbackNumber = number
        )
        historyStore.save(ticket.toSummary())
        ticket
    }

    override suspend fun replyToTicket(
        number: String,
        body: String,
        attachments: List<Attachment>
    ): Result<Unit> = runCatchingApi {
        api.replyTicket(number, buildReplyParts(body, attachments)).ensureSuccess()
    }

    override suspend fun completeTicket(number: String): Result<Unit> = runCatchingApi {
        api.completeTicket(number).ensureSuccess()
        val current = historyStore.history.first().firstOrNull { it.number == number }
        if (current != null) historyStore.save(current.copy(status = TicketStatus.RESOLVED))
    }

    override suspend fun saveToHistory(summary: TicketSummary) = historyStore.save(summary)

    override suspend fun removeFromHistory(number: String) = historyStore.remove(number)

    override suspend fun clearHistory() = historyStore.clear()

    // ---------------------------------------------------------------------
    // Request building
    // ---------------------------------------------------------------------

    private fun buildCreateParts(draft: TicketDraft): List<MultipartBody.Part> {
        val parts = mutableListOf<MultipartBody.Part>()

        fun addField(name: String, value: String?) {
            if (!value.isNullOrBlank()) parts += MultipartBody.Part.createFormData(name, value)
        }

        addField("channel_id", CHANNEL_ID)
        addField("classification", draft.type.apiValue)
        addField("service_type", draft.layanan?.apiValue)
        addField("reporter_name", draft.reporterName)
        addField("reporter_email", draft.reporterEmail)
        addField("reporter_wa", draft.reporterPhone)
        addField("title", draft.subject)
        addField("content", draft.body)

        draft.attachments.forEach { parts += it.toPart() }
        return parts
    }

    private fun buildReplyParts(message: String, attachments: List<Attachment>): List<MultipartBody.Part> =
        buildList {
            add(MultipartBody.Part.createFormData("message", message))
            attachments.forEach { add(it.toPart()) }
        }

    private fun Attachment.toPart(): MultipartBody.Part {
        val path = uri.path ?: throw ApiException(ApiException.Kind.UNKNOWN)
        return MultipartBody.Part.createFormData(
            name = "attachments[]",
            filename = fileName,
            body = File(path).asRequestBody(mimeType.toMediaTypeOrNull())
        )
    }

    // ---------------------------------------------------------------------
    // Response handling
    // ---------------------------------------------------------------------

    private fun Response<ResponseBody>.ensureSuccess() {
        if (!isSuccessful) {
            throw parseApiError(code(), errorBody()?.string() ?: body()?.string())
        }
    }

    private fun Response<ResponseBody>.successBodyOrThrow(): String {
        ensureSuccess()
        return body()?.string().orEmpty()
    }

    private suspend fun <T> runCatchingApi(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: ApiException) {
        Result.failure(e)
    } catch (e: SocketTimeoutException) {
        Result.failure(ApiException(ApiException.Kind.TIMEOUT, cause = e))
    } catch (e: IOException) {
        Result.failure(ApiException(ApiException.Kind.NETWORK, cause = e))
    } catch (e: Exception) {
        Result.failure(ApiException(ApiException.Kind.UNKNOWN, cause = e))
    }

    // ---------------------------------------------------------------------
    // Mapping
    // ---------------------------------------------------------------------

    private fun TicketCreated.toSummary(draft: TicketDraft) = TicketSummary(
        number = number,
        type = type,
        summary = draft.subject?.takeIf { it.isNotBlank() } ?: draft.body.take(SUMMARY_LENGTH),
        createdAt = null,
        status = TicketStatus.NEW,
        savedAtEpochMillis = System.currentTimeMillis()
    )

    private fun Ticket.toSummary() = TicketSummary(
        number = number,
        type = type,
        summary = subject?.takeIf { it.isNotBlank() } ?: body.take(SUMMARY_LENGTH),
        createdAt = createdAt,
        status = status,
        savedAtEpochMillis = System.currentTimeMillis()
    )

    private companion object {
        /** Datang dari local.properties (`CHANNEL_ID`), default 1 sesuai PRD §5.1. */
        val CHANNEL_ID = BuildConfig.CHANNEL_ID
        const val SUMMARY_LENGTH = 120
    }
}
