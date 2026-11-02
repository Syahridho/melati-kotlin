package com.example.melati.domain.repository

import com.example.melati.domain.model.Attachment
import com.example.melati.domain.model.Ticket
import com.example.melati.domain.model.TicketCreated
import com.example.melati.domain.model.TicketDraft
import com.example.melati.domain.model.TicketSummary
import kotlinx.coroutines.flow.Flow

/**
 * Satu-satunya pintu bagi UI ke data tiket.
 *
 * Implementasi nyata ([com.example.melati.data.repository.TicketRepositoryImpl])
 * memanggil API Laravel. Seluruh ketidakpastian kontrak API (PRD §8, §13)
 * terkurung di lapisan data sehingga perubahan bentuk response tidak merambat ke UI.
 */
interface TicketRepository {

    /** Semua tiket tersimpan di Riwayat lokal, terbaru lebih dulu. */
    val history: Flow<List<TicketSummary>>

    /** PRD §5.1: POST /api/tickets dengan channel_id dari konfigurasi (BuildConfig). */
    suspend fun createTicket(draft: TicketDraft): Result<TicketCreated>

    /** PRD §5.2: POST /api/check berdasarkan nomor tiket. */
    suspend fun checkTicket(number: String): Result<Ticket>

    /** PRD §5.2: balasan pelapor pada thread tiket. */
    suspend fun replyToTicket(number: String, body: String, attachments: List<Attachment>): Result<Unit>

    /** PRD §5.2: tandai tiket selesai. */
    suspend fun completeTicket(number: String): Result<Unit>

    /** Simpan/perbarui ringkasan tiket di perangkat (idempoten per nomor). */
    suspend fun saveToHistory(summary: TicketSummary)

    suspend fun removeFromHistory(number: String)

    suspend fun clearHistory()
}
