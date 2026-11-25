package com.example.melati.ui.screens.cek

import com.example.melati.domain.model.Ticket
import com.example.melati.domain.model.TicketStatus
import com.example.melati.domain.model.TicketType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CekStateTest {

    @Test
    fun `state idle tidak memiliki detail tiket`() {
        val state = CekUiState()
        assertNull(state.ticket)
    }

    @Test
    fun `state loaded menyimpan detail tiket`() {
        val ticket = Ticket(
            number = "L-2026-0001",
            type = TicketType.LAPOR,
            status = TicketStatus.IN_PROGRESS,
        )
        val state = CekUiState(phase = CekPhase.Loaded(ticket))
        assertEquals(ticket, state.ticket)
        assertEquals("L-2026-0001", state.ticket?.number)
    }
}
