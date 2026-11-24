package com.example.melati.ui.screens.laporan

import com.example.melati.domain.model.TicketType
import java.time.LocalDate

/**
 * Aturan validasi form Laporan.
 *
 * Dipisah dari [LaporanViewModel] dan tidak memakai API Android (mis.
 * `android.util.Patterns`) supaya bisa diuji langsung di JVM tanpa Context
 * atau Robolectric.
 *
 * Nama, email, dan nomor telepon opsional: format hanya diperiksa bila diisi.
 * Klasifikasi, judul, dan isi laporan tetap wajib. Tanggal kejadian opsional,
 * tetapi tidak boleh melewati hari ini.
 */
object LaporanValidator {

    private val EMAIL_PATTERN =
        Regex("^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9-]+(?:\\.[A-Za-z0-9-]+)+$")

    /** Nomor Indonesia: 08…, 628…, atau +628…, 9–15 digit total. */
    private val PHONE_PATTERN = Regex("^(?:\\+62|62|0)8[0-9]{7,12}$")

    fun validate(
        state: LaporanUiState,
        today: LocalDate = LocalDate.now(),
    ): Map<LaporanField, FormError> {
        val errors = mutableMapOf<LaporanField, FormError>()

        if (state.type == null) errors[LaporanField.TYPE] = FormError.REQUIRED
        if (state.type == TicketType.LAPOR && state.layanan == null) {
            errors[LaporanField.LAYANAN] = FormError.REQUIRED
        }

        // Server menolak tiket tanpa `title` (422 "Kolom title wajib diisi"),
        // meski PRD menyebutnya opsional — validasi klien mengikuti server.
        if (state.subject.isBlank()) errors[LaporanField.SUBJECT] = FormError.REQUIRED
        if (state.body.isBlank()) errors[LaporanField.BODY] = FormError.REQUIRED

        val tanggal = state.tanggalKejadian
        if (tanggal != null && tanggal.isAfter(today)) {
            errors[LaporanField.TANGGAL_KEJADIAN] = FormError.INVALID_DATE
        }

        val email = state.email.trim()
        if (email.isNotEmpty() && !EMAIL_PATTERN.matches(email)) {
            errors[LaporanField.EMAIL] = FormError.INVALID_EMAIL
        }

        val phone = normalizePhone(state.phone)
        if (phone.isNotEmpty() && !PHONE_PATTERN.matches(phone)) {
            errors[LaporanField.PHONE] = FormError.INVALID_PHONE
        }

        return errors
    }

    /** Buang spasi dan pemisah umum agar "0812-3456 7890" tetap valid. */
    fun normalizePhone(raw: String): String =
        raw.filterNot { it.isWhitespace() || it in "-.()" }
}
