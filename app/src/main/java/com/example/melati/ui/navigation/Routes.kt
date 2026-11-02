package com.example.melati.ui.navigation

import android.net.Uri

object Routes {
    const val LAPORAN = "laporan"
    const val CEK_BASE = "cek"
    const val CEK = "cek?nomor={nomor}"
    const val RIWAYAT = "riwayat"
    const val ARG_NOMOR = "nomor"

    fun cek(nomor: String = ""): String =
        if (nomor.isBlank()) CEK_BASE else "$CEK_BASE?$ARG_NOMOR=${Uri.encode(nomor)}"
}
