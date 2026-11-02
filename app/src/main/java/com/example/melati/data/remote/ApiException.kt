package com.example.melati.data.remote

/**
 * Kegagalan yang sudah dinormalisasi, sehingga UI tidak perlu tahu soal HTTP.
 */
class ApiException(
    val kind: Kind,
    val serverMessage: String? = null,
    val fieldErrors: Map<String, String> = emptyMap(),
    val httpCode: Int? = null,
    cause: Throwable? = null
) : Exception(serverMessage, cause) {

    enum class Kind { NETWORK, TIMEOUT, HTTP, INVALID_RESPONSE, UNKNOWN }

    val isConnectivityIssue: Boolean get() = kind == Kind.NETWORK || kind == Kind.TIMEOUT

    val isNotFound: Boolean
        get() = httpCode == 404 || serverMessage
            ?.lowercase()
            ?.let { it.contains("tidak ditemukan") || it.contains("not found") || it.contains("tidak ada") }
            ?: false

    /** Server menolak identitas klien (API key salah / tidak ada). */
    val isUnauthorized: Boolean get() = httpCode == 401 || httpCode == 403
}
