package com.example.melati.data.remote

import com.example.melati.domain.model.MessageAuthor
import com.example.melati.domain.model.TicketStatus
import com.example.melati.domain.model.TicketType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class TicketParserTest {

    @Test
    fun `nomor tiket di dalam pembungkus data terbaca`() {
        val body = """{"success":true,"data":{"ticket_number":"L-2026-0001"}}"""

        val created = TicketParser.parseCreated(body)

        assertEquals("L-2026-0001", created.number)
        assertEquals(TicketType.LAPOR, created.type)
    }

    @Test
    fun `nomor tiket dengan nama field indonesia terbaca`() {
        val created = TicketParser.parseCreated("""{"nomor_tiket":"A-2026-0042"}""")

        assertEquals("A-2026-0042", created.number)
        assertEquals(TicketType.ASPIRASI, created.type)
    }

    @Test
    fun `respons tanpa nomor tiket dianggap gagal`() {
        assertThrows(ApiException::class.java) {
            TicketParser.parseCreated("""{"success":true}""")
        }
    }

    @Test
    fun `detail tiket memetakan status dan isi`() {
        val body = """
            {
              "data": {
                "ticket_number": "I-2026-0007",
                "status": "Diproses",
                "subject": "Permintaan data",
                "message": "Mohon data produksi padi 2025.",
                "created_at": "2026-10-01 09:15",
                "service": "PST"
              }
            }
        """.trimIndent()

        val ticket = TicketParser.parseTicket(body)

        assertEquals("I-2026-0007", ticket.number)
        assertEquals(TicketType.PERMINTAAN_INFORMASI, ticket.type)
        assertEquals(TicketStatus.IN_PROGRESS, ticket.status)
        assertEquals("Permintaan data", ticket.subject)
        assertEquals("Mohon data produksi padi 2025.", ticket.body)
        assertEquals("PST", ticket.layanan?.label)
    }

    @Test
    fun `thread balasan admin terpetakan sebagai pesan admin`() {
        val body = """
            {
              "ticket_number": "L-2026-0010",
              "status": "dijawab",
              "replies": [
                {"id": 1, "message": "Terima kasih atas laporannya.", "created_at": "2026-10-02"},
                {"id": 2, "message": "Sedang kami tindak lanjuti.", "created_at": "2026-10-03"}
              ]
            }
        """.trimIndent()

        val ticket = TicketParser.parseTicket(body)

        assertEquals(2, ticket.messages.size)
        assertTrue(ticket.messages.all { it.author == MessageAuthor.ADMIN })
        assertEquals("Terima kasih atas laporannya.", ticket.messages.first().body)
    }

    @Test
    fun `thread umum membedakan pelapor dan admin`() {
        val body = """
            {
              "ticket_number": "L-2026-0011",
              "messages": [
                {"message": "Saya melaporkan gangguan.", "role": "user"},
                {"message": "Kami sedang memeriksa.", "role": "admin"}
              ]
            }
        """.trimIndent()

        val ticket = TicketParser.parseTicket(body)

        assertEquals(MessageAuthor.REPORTER, ticket.messages[0].author)
        assertEquals(MessageAuthor.ADMIN, ticket.messages[1].author)
    }

    @Test
    fun `balasan pelapor dari server asli tidak berlabel petugas`() {
        // Bentuk respons nyata `GET /api/check` (28 Okt 2026): thread ada di
        // `responses`, pengirim ditandai `is_reporter` dan `type`.
        val body = """
            {
              "success": true,
              "data": {
                "ticket_number": "L-1400/102026/RM06",
                "classification": "pengaduan",
                "status": "baru",
                "content": "Ini adalah uji end to end.",
                "responses": [
                  {
                    "id": 4,
                    "type": "balasan_pelapor",
                    "message": "Terima kasih, tiket sudah saya terima",
                    "user_name": "Budi Santoso (Pelapor)",
                    "is_reporter": true,
                    "created_at": "2026-10-28T10:14:37+07:00"
                  }
                ]
              }
            }
        """.trimIndent()

        val ticket = TicketParser.parseTicket(body)

        assertEquals(1, ticket.messages.size)
        assertEquals(MessageAuthor.REPORTER, ticket.messages.single().author)
        assertEquals("Terima kasih, tiket sudah saya terima", ticket.messages.single().body)
    }

    @Test
    fun `respons teks tunggal dianggap balasan admin`() {
        val ticket = TicketParser.parseTicket(
            """{"ticket_number":"A-2026-0002","response":"Saran Anda sudah kami terima."}"""
        )

        assertEquals(1, ticket.messages.size)
        assertEquals(MessageAuthor.ADMIN, ticket.messages.single().author)
    }

    @Test
    fun `status yang tidak dikenal tidak dianggap final`() {
        assertEquals(TicketStatus.UNKNOWN, TicketStatus.fromRaw("eskalasi lanjutan"))
        assertEquals(TicketStatus.NEW, TicketStatus.fromRaw("menunggu verifikasi"))
        assertFalse(TicketStatus.UNKNOWN.isFinal)
        assertTrue(TicketStatus.RESOLVED.isFinal)
        assertTrue(TicketStatus.CLOSED.isFinal)
    }

    @Test
    fun `nomor tiket tanpa prefix tetap bisa diparse`() {
        val ticket = TicketParser.parseTicket("""{"ticket_number":"12345"}""", fallbackNumber = "12345")

        assertEquals("12345", ticket.number)
    }

    @Test
    fun `body bukan json menandakan response tidak valid`() {
        val error = assertThrows(ApiException::class.java) {
            TicketParser.parseTicket("<html>Server Error</html>")
        }

        assertEquals(ApiException.Kind.INVALID_RESPONSE, error.kind)
    }
}
