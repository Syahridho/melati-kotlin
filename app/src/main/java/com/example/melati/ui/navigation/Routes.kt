package com.example.melati.ui.navigation

import android.net.Uri

object Routes {
    const val LAPORAN = "laporan"
    const val CEK = "cek"
    const val CEK_DETAIL = "cek/{ticketId}"
    const val RIWAYAT = "riwayat"
    const val ARG_TICKET_ID = "ticketId"

    /** Rute tab Cek (tanpa argumen) — dipakai bottom navigation. */
    const val CEK_BASE = CEK

    /** Rute detail tiket (destination tersendiri) agar back stack/tab konsisten. */
    fun cek(number: String = ""): String =
        if (number.isBlank()) CEK else "cek/${Uri.encode(number)}"

    fun cekDetail(ticketId: String): String = "cek/${Uri.encode(ticketId)}"
}
