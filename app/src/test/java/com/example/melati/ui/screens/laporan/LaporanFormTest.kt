package com.example.melati.ui.screens.laporan

import com.example.melati.data.repository.buildCreateParts
import com.example.melati.domain.model.LayananType
import com.example.melati.domain.model.TicketDraft
import com.example.melati.domain.model.TicketType
import okhttp3.MultipartBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Unit test logika form Laporan. Validasi dan pemetaan payload dipisah dari
 * ViewModel/Repository agar bisa dijalankan di JVM tanpa Context atau emulator.
 */
class LaporanFormTest {

    private val today = LocalDate.of(2026, 11, 23)

    private fun validState() = LaporanUiState(
        type = TicketType.LAPOR,
        layanan = LayananType.PST,
        subject = "Jalan rusak",
        body = "Ada jalan rusak di depan kantor.",
    )

    private fun validate(state: LaporanUiState) = LaporanValidator.validate(state, today)

    @Test
    fun `nama kosong tetap valid`() {
        val errors = validate(validState())

        assertTrue(errors.isEmpty())
        assertNull(errors[LaporanField.NAME])
    }

    @Test
    fun `email kosong tetap valid`() {
        val errors = validate(validState().copy(email = ""))

        assertTrue(errors.isEmpty())
        assertNull(errors[LaporanField.EMAIL])
    }

    @Test
    fun `email salah format ditolak bila diisi`() {
        val errors = validate(validState().copy(email = "bukan-email"))

        assertEquals(FormError.INVALID_EMAIL, errors[LaporanField.EMAIL])
    }

    @Test
    fun `telepon salah format ditolak bila diisi`() {
        val errors = validate(validState().copy(phone = "12345"))

        assertEquals(FormError.INVALID_PHONE, errors[LaporanField.PHONE])
    }

    @Test
    fun `telepon 08 dan plus 62 diterima`() {
        assertTrue(validate(validState().copy(phone = "0812 3456 7890")).isEmpty())
        assertTrue(validate(validState().copy(phone = "+62 812-3456-7890")).isEmpty())
    }

    @Test
    fun `tanggal masa depan ditolak`() {
        val errors = validate(validState().copy(tanggalKejadian = today.plusDays(1)))

        assertEquals(FormError.INVALID_DATE, errors[LaporanField.TANGGAL_KEJADIAN])
    }

    @Test
    fun `pindah ke aspirasi mereset tanggal dan layanan`() {
        val state = validState().copy(tanggalKejadian = today)

        val aspirasi = state.withType(TicketType.ASPIRASI)

        assertEquals(TicketType.ASPIRASI, aspirasi.type)
        assertNull(aspirasi.tanggalKejadian)
        assertNull(aspirasi.layanan)
    }

    @Test
    fun `payload pengaduan memuat tanggal sedangkan aspirasi tidak`() {
        val pengaduan = TicketDraft(
            type = TicketType.LAPOR,
            layanan = LayananType.PST,
            reporterName = "",
            reporterEmail = null,
            reporterPhone = null,
            tanggalKejadian = LocalDate.of(2026, 11, 20),
            subject = "Jalan rusak",
            body = "Isi laporan",
        )
        val aspirasi = pengaduan.copy(type = TicketType.ASPIRASI, layanan = null)

        assertEquals("2026-11-20", fieldValue(buildCreateParts(pengaduan), "tanggal_kejadian"))
        assertNull(fieldValue(buildCreateParts(aspirasi), "tanggal_kejadian"))
    }

    @Test
    fun `payload pengaduan tanpa tanggal tidak memuat field tanggal`() {
        val draft = TicketDraft(
            type = TicketType.LAPOR,
            layanan = LayananType.PST,
            reporterName = "",
            reporterEmail = null,
            reporterPhone = null,
            tanggalKejadian = null,
            subject = "Jalan rusak",
            body = "Isi laporan",
        )

        assertNull(fieldValue(buildCreateParts(draft), "tanggal_kejadian"))
        // Nama kosong tidak dikirim sama sekali (dianggap null oleh server).
        assertNull(fieldValue(buildCreateParts(draft), "reporter_name"))
    }

    private fun fieldValue(parts: List<MultipartBody.Part>, name: String): String? {
        val part = parts.firstOrNull {
            it.headers?.get("Content-Disposition")?.contains("name=\"$name\"") == true
        } ?: return null
        val buffer = Buffer()
        part.body.writeTo(buffer)
        return buffer.readUtf8()
    }
}
