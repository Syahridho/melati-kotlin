package com.example.melati.data.remote

import com.example.melati.domain.model.LayananType
import com.example.melati.domain.model.MessageAuthor
import com.example.melati.domain.model.Ticket
import com.example.melati.domain.model.TicketCreated
import com.example.melati.domain.model.TicketMessage
import com.example.melati.domain.model.TicketStatus
import com.example.melati.domain.model.TicketType
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

/**
 * Parser yang sengaja toleran: nama field API belum final (PRD §13), jadi setiap
 * nilai dicari lewat beberapa kemungkinan nama. Bila kontrak API dipastikan,
 * parser ini bisa dipersempit tanpa mengubah lapisan lain.
 */
internal object TicketParser {

    private val NUMBER_KEYS = arrayOf(
        "ticket_number", "nomor_tiket", "no_tiket", "number", "ticket_no", "kode_tiket", "code"
    )
    private val TYPE_KEYS = arrayOf("type", "classification", "klasifikasi", "category", "jenis", "ticket_type")
    private val SERVICE_KEYS = arrayOf("service", "layanan", "sub_category", "subkategori", "unit")
    private val STATUS_KEYS = arrayOf("status", "ticket_status", "state", "status_tiket")
    private val SUBJECT_KEYS = arrayOf("subject", "title", "judul", "perihal")
    private val BODY_KEYS = arrayOf("message", "body", "description", "content", "isi", "laporan", "keluhan", "pesan")
    private val CREATED_KEYS = arrayOf("created_at", "createdAt", "date", "tanggal", "created", "waktu")
    private val AUTHOR_KEYS = arrayOf("author", "sender", "role", "from", "by", "sender_type", "pengirim", "user_type", "type")
    private val MESSAGE_KEYS = arrayOf("message", "body", "content", "text", "isi", "pesan", "balasan", "response")

    fun parseCreated(body: String?): TicketCreated {
        val root = parseObject(body)
        val payload = root.unwrapPayload()
        val candidates = listOf(payload, root)

        val number = candidates.firstNotNullOfOrNull { it.pickString(*NUMBER_KEYS) }
            ?: root.allStringsRecursive().firstNotNullOfOrNull { (key, value) ->
                val normalized = key.lowercase().replace("_", "")
                if (NUMBER_KEYS.any { it.lowercase().replace("_", "") == normalized }) value else null
            }
            ?: throw ApiException(
                ApiException.Kind.INVALID_RESPONSE,
                serverMessage = root.pickString("message", "pesan")
            )

        val type = TicketType.fromNumber(number)
            ?: TicketType.fromRaw(candidates.firstNotNullOfOrNull { it.pickString(*TYPE_KEYS) })
            ?: TicketType.LAPOR

        return TicketCreated(number = number.trim(), type = type)
    }

    fun parseTicket(body: String?, fallbackNumber: String? = null): Ticket {
        val root = parseObject(body)
        val payload = root.unwrapPayload()
        val candidates = listOf(payload, root)

        val number = candidates.firstNotNullOfOrNull { it.pickString(*NUMBER_KEYS) }
            ?: fallbackNumber
            ?: throw ApiException(ApiException.Kind.INVALID_RESPONSE)

        val type = TicketType.fromRaw(candidates.firstNotNullOfOrNull { it.pickString(*TYPE_KEYS) })
            ?: TicketType.fromNumber(number)
            ?: TicketType.LAPOR

        val status = TicketStatus.fromRaw(candidates.firstNotNullOfOrNull { it.pickString(*STATUS_KEYS) })

        return Ticket(
            number = number.trim(),
            type = type,
            layanan = LayananType.fromRaw(candidates.firstNotNullOfOrNull { it.pickString(*SERVICE_KEYS) }),
            status = status,
            subject = candidates.firstNotNullOfOrNull { it.pickString(*SUBJECT_KEYS) },
            body = candidates.firstNotNullOfOrNull { it.pickString(*BODY_KEYS) }.orEmpty(),
            createdAt = candidates.firstNotNullOfOrNull { it.pickString(*CREATED_KEYS) },
            messages = parseMessages(payload)
        )
    }

    private fun parseMessages(payload: JsonObject): List<TicketMessage> {
        // Daftar balasan petugas biasanya terpisah dari thread umum.
        val adminOnly = payload.pickArray("replies", "responses", "tanggapan", "balasan")
        val general = payload.pickArray("messages", "thread", "chat", "conversations", "percakapan")

        val (array, defaultAuthor) = when {
            adminOnly != null -> adminOnly to MessageAuthor.ADMIN
            general != null -> general to MessageAuthor.REPORTER
            else -> return parseSingleResponse(payload)
        }
        return array.mapNotNull { element ->
            val obj = element.asObjectOrNull() ?: return@mapNotNull element
                .asStringOrNull()
                ?.takeIf { it.isNotBlank() }
                ?.let { TicketMessage(id = null, body = it, author = defaultAuthor) }
            val body = obj.pickString(*MESSAGE_KEYS)?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            TicketMessage(
                id = obj.pickString("id", "message_id"),
                body = body,
                author = resolveAuthor(obj, defaultAuthor),
                createdAt = obj.pickString(*CREATED_KEYS),
                attachmentNames = obj.pickArray("attachments", "lampiran")
                    ?.mapNotNull { it.asObjectOrNull()?.pickString("file_name", "filename", "name", "nama") }
                    .orEmpty()
            )
        }
    }

    /** Beberapa API hanya menyimpan satu field `response` berupa teks. */
    private fun parseSingleResponse(payload: JsonObject): List<TicketMessage> {
        val response = payload.pickString("response", "jawaban", "tanggapan", "feedback")
            ?.takeIf { it.isNotBlank() }
            ?: return emptyList()
        return listOf(TicketMessage(id = null, body = response, author = MessageAuthor.ADMIN))
    }

    private fun resolveAuthor(obj: JsonObject, default: MessageAuthor): MessageAuthor {
        // Server Melati (diverifikasi 28 Okt 2026) menandai pengirim dengan
        // `is_reporter` (boolean) dan `type` = "balasan_pelapor" /
        // "balasan_petugas". Tanpa ini, balasan pelapor ikut berlabel "Petugas".
        val isReporter = (obj.pick("is_reporter", "isReporter") as? JsonPrimitive)?.booleanOrNull
        if (isReporter != null) return if (isReporter) MessageAuthor.REPORTER else MessageAuthor.ADMIN

        val raw = obj.pickString(*AUTHOR_KEYS)?.lowercase()
        if (raw != null) {
            return when {
                raw.contains("admin") || raw.contains("petugas") || raw.contains("operator") ||
                    raw.contains("officer") || raw.contains("staff") -> MessageAuthor.ADMIN
                raw.contains("user") || raw.contains("pelapor") || raw.contains("reporter") ||
                    raw.contains("masyarakat") || raw.contains("public") -> MessageAuthor.REPORTER
                else -> default
            }
        }
        val isAdmin = (obj.pick("is_admin", "isAdmin") as? JsonPrimitive)?.booleanOrNull
        return when (isAdmin) {
            true -> MessageAuthor.ADMIN
            false -> MessageAuthor.REPORTER
            null -> default
        }
    }

    private fun parseObject(body: String?): JsonObject {
        val text = body?.trim().orEmpty()
        if (text.isEmpty()) throw ApiException(ApiException.Kind.INVALID_RESPONSE)
        val element = runCatching { MelatiJson.parseToJsonElement(text) }.getOrNull()
            ?: throw ApiException(ApiException.Kind.INVALID_RESPONSE)
        return element.asObjectOrNull() ?: throw ApiException(ApiException.Kind.INVALID_RESPONSE)
    }
}

internal fun parseApiError(code: Int, body: String?): ApiException {
    val obj = runCatching { MelatiJson.parseToJsonElement(body.orEmpty()) }.getOrNull().asObjectOrNull()
    val fieldErrors = obj?.pickObject("errors", "validation")?.let { errors ->
        errors.entries.mapNotNull { (key, value) ->
            val message = value.asStringOrNull()
                ?: (value as? JsonArray)?.firstOrNull()?.asStringOrNull()
            if (message.isNullOrBlank()) null else key to message
        }.toMap()
    }.orEmpty()

    val message = obj?.pickString("message", "error", "pesan", "detail", "exception")
    return ApiException(ApiException.Kind.HTTP, serverMessage = message, fieldErrors = fieldErrors, httpCode = code)
}
