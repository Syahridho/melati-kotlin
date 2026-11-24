package com.example.melati.data.local

import com.example.melati.domain.model.TicketStatus
import com.example.melati.domain.model.TicketSummary
import com.example.melati.domain.model.TicketType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TicketSuccessfulActionTest {

    private fun existing(
        status: TicketStatus = TicketStatus.ANSWERED,
        savedAt: Long = 500L,
    ) = TicketSummary(
        number = "L-2026-0001",
        type = TicketType.LAPOR,
        summary = "Jalan rusak",
        createdAt = "2026-11-01",
        status = status,
        savedAtEpochMillis = savedAt,
    )

    @Test
    fun `complete menyimpan status RESOLVED`() {
        val merged = existing().afterSuccessfulAction(
            number = "L-2026-0001",
            status = TicketStatus.RESOLVED,
            nowMillis = 9_000L,
        )

        assertEquals(TicketStatus.RESOLVED, merged?.status)
        assertEquals(500L, merged?.savedAtEpochMillis)
    }

    @Test
    fun `complete tanpa status tidak mengubah entri`() {
        // applySuccessfulAction returns early when status null; afterSuccessfulAction
        // with null status keeps existing status.
        val merged = existing().afterSuccessfulAction(
            number = "L-2026-0001",
            status = null,
            nowMillis = 9_000L,
        )

        assertEquals(TicketStatus.ANSWERED, merged?.status)
    }

    @Test
    fun `complete membuat entri baru bila belum ada`() {
        val created = (null as TicketSummary?).afterSuccessfulAction(
            number = "A-2026-0042",
            status = TicketStatus.RESOLVED,
            nowMillis = 9_000L,
        )

        assertEquals("A-2026-0042", created?.number)
        assertEquals(TicketType.ASPIRASI, created?.type)
        assertEquals(TicketStatus.RESOLVED, created?.status)
        assertEquals(9_000L, created?.savedAtEpochMillis)
        assertTrue(created?.summary?.contains("A-2026-0042") == true)
    }

    @Test
    fun `tanpa status dan tanpa entri lama tidak membuat entri`() {
        val created = (null as TicketSummary?).afterSuccessfulAction(
            number = "L-2026-0001",
            status = null,
            nowMillis = 9_000L,
        )

        assertNull(created)
    }
}
