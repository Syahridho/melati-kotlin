package com.example.melati.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.melati.domain.model.TicketStatus
import com.example.melati.domain.model.TicketSummary
import com.example.melati.domain.model.TicketType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.IOException

private val Context.historyDataStore: DataStore<Preferences> by preferencesDataStore(name = "melati_history")

/**
 * Riwayat tiket di perangkat (PRD §5.3, §8). Disimpan sebagai JSON di DataStore
 * agar tanpa login pun nomor tiket tetap tercatat. Data hilang bila app di-uninstall.
 */
class TicketHistoryStore(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; explicitNulls = false }

    val history: Flow<List<TicketSummary>> = context.historyDataStore.data
        .catch { cause ->
            if (cause is IOException) emit(emptyPreferences()) else throw cause
        }
        .map { preferences -> decode(preferences[HISTORY_KEY]) }
        .distinctUntilChanged()

    suspend fun save(summary: TicketSummary) {
        context.historyDataStore.edit { preferences ->
            val current = decode(preferences[HISTORY_KEY])
            val updated = (listOf(summary) + current.filterNot { it.number == summary.number })
                .sortedByDescending { it.savedAtEpochMillis }
            preferences[HISTORY_KEY] = encode(updated)
        }
    }

    /**
     * Memperbarui status tiket di Riwayat setelah aksi sukses (mis. RESOLVED
     * setelah complete). Entri baru dibuat hanya bila [status] diberikan dan
     * entri belum ada.
     */
    suspend fun applySuccessfulAction(
        number: String,
        status: TicketStatus? = null
    ) {
        if (status == null) return
        val cleanNumber = number.trim()
        if (cleanNumber.isEmpty()) return

        context.historyDataStore.edit { preferences ->
            val current = decode(preferences[HISTORY_KEY])
            val existing = current.firstOrNull { it.number == cleanNumber }
            val updatedEntry = existing.afterSuccessfulAction(
                number = cleanNumber,
                status = status,
                nowMillis = System.currentTimeMillis()
            ) ?: return@edit
            val updated = (listOf(updatedEntry) + current.filterNot { it.number == cleanNumber })
                .sortedByDescending { it.savedAtEpochMillis }
            preferences[HISTORY_KEY] = encode(updated)
        }
    }

    suspend fun remove(number: String) {
        context.historyDataStore.edit { preferences ->
            val current = decode(preferences[HISTORY_KEY])
            preferences[HISTORY_KEY] = encode(current.filterNot { it.number == number })
        }
    }

    suspend fun clear() {
        context.historyDataStore.edit { preferences -> preferences.remove(HISTORY_KEY) }
    }

    private fun decode(raw: String?): List<TicketSummary> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            json.decodeFromString(ListSerializer(HistoryEntry.serializer()), raw).mapNotNull { it.toDomain() }
        }.getOrDefault(emptyList())
    }

    private fun encode(items: List<TicketSummary>): String =
        json.encodeToString(ListSerializer(HistoryEntry.serializer()), items.map(HistoryEntry::from))

    @Serializable
    private data class HistoryEntry(
        val number: String,
        val type: String,
        val summary: String,
        val createdAt: String? = null,
        val status: String,
        val savedAt: Long
    ) {
        fun toDomain(): TicketSummary? = runCatching {
            TicketSummary(
                number = number,
                type = TicketType.valueOf(type),
                summary = summary,
                createdAt = createdAt,
                status = TicketStatus.valueOf(status),
                savedAtEpochMillis = savedAt
            )
        }.getOrNull()

        companion object {
            fun from(summary: TicketSummary) = HistoryEntry(
                number = summary.number,
                type = summary.type.name,
                summary = summary.summary,
                createdAt = summary.createdAt,
                status = summary.status.name,
                savedAt = summary.savedAtEpochMillis
            )
        }
    }

    private companion object {
        val HISTORY_KEY = stringPreferencesKey("history_v1")
    }
}

/**
 * Hasil penggabungan setelah aksi sukses, tanpa DataStore agar teruji.
 * - Entri ada: status baru hanya menimpa bila diberikan.
 * - Entri belum ada: dibuat hanya bila [status] diberikan.
 */
internal fun TicketSummary?.afterSuccessfulAction(
    number: String,
    status: TicketStatus?,
    nowMillis: Long
): TicketSummary? {
    if (this != null) {
        return copy(status = status ?: this.status)
    }
    if (status == null) return null

    val cleanNumber = number.trim()
    return TicketSummary(
        number = cleanNumber,
        type = TicketType.fromNumber(cleanNumber) ?: TicketType.LAPOR,
        summary = "Tiket $cleanNumber",
        createdAt = null,
        status = status,
        savedAtEpochMillis = nowMillis
    )
}
