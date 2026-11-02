package com.example.melati.data.remote

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Kontrak HTTP ke Laravel API Melati.
 *
 * Path dan metode di bawah sudah **diverifikasi** terhadap route table server test
 * (`test.syahridho.my.id`) dan cocok dengan pemakaian web Melati:
 * - `GET  /api/check?ticket_number=...`
 * - `POST /api/check/{ticketNumber}/reply`
 * - `POST /api/check/{ticketNumber}/complete`
 * - `POST /api/tickets`
 *
 * Catatan: server mewajibkan API key (respons 401 "Invalid or missing API key").
 * Lihat [NetworkModule] — key dibaca dari `BuildConfig.API_KEY`, bukan hardcoded.
 */
interface MelatiApi {

    /** PRD §5.1 — multipart, wajib menyertakan `channel_id` (BuildConfig.CHANNEL_ID). */
    @Multipart
    @POST("api/tickets")
    suspend fun createTicket(@Part parts: List<MultipartBody.Part>): Response<ResponseBody>

    /** PRD §5.2 — cek tiket berdasarkan nomor (GET, bukan POST). */
    @GET("api/check")
    suspend fun checkTicket(@Query("ticket_number") ticketNumber: String): Response<ResponseBody>

    /** PRD §5.2 — balasan pelapor; selalu multipart karena mendukung lampiran. */
    @Multipart
    @POST("api/check/{ticketNumber}/reply")
    suspend fun replyTicket(
        @Path("ticketNumber") ticketNumber: String,
        @Part parts: List<MultipartBody.Part>
    ): Response<ResponseBody>

    /** PRD §5.2 — tandai tiket selesai. */
    @POST("api/check/{ticketNumber}/complete")
    suspend fun completeTicket(@Path("ticketNumber") ticketNumber: String): Response<ResponseBody>
}
