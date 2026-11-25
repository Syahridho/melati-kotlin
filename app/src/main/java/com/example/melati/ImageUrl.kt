package com.example.melati

import com.example.melati.BuildConfig

/**
 * Bentuk URL gambar final dari path (relatif/absolut) dan BASE_URL dari env.
 * - path null/kosong -> null (tampilkan placeholder).
 * - path absolut (http/https) -> ambil hanya path-nya, gabungkan dengan BASE_URL.
 * - path relatif -> gabungkan dengan BASE_URL tanpa dobel slash.
 * - BASE_URL dengan/tanpa trailing slash didukung.
 */
fun resolveImageUrl(path: String?): String? = resolveImageUrl(path, BuildConfig.BASE_URL)

fun resolveImageUrl(path: String?, baseUrl: String): String? {
    val p = path?.trim().orEmpty()
    if (p.isEmpty()) return null

    val base = baseUrl.trim().let { if (it.endsWith("/")) it else "$it/" }

    val relative = if (p.startsWith("http://", ignoreCase = true) || p.startsWith("https://", ignoreCase = true)) {
        val uri = runCatching { java.net.URI(p) }.getOrNull()
        (uri?.rawPath ?: p.substringAfter("://").substringAfter("/", "")).removePrefix("/")
    } else {
        p.removePrefix("/")
    }

    if (relative.isEmpty()) return base
    return base + relative
}

object ImageUrlResolver {
    fun resolve(path: String?): String? = resolveImageUrl(path)
    fun resolve(path: String?, baseUrl: String): String? = resolveImageUrl(path, baseUrl)
    fun resolveImageUrl(path: String?): String? = com.example.melati.resolveImageUrl(path)
    fun resolveImageUrl(path: String?, baseUrl: String): String? = com.example.melati.resolveImageUrl(path, baseUrl)
}

object ImageUrls {
    fun resolveImageUrl(path: String?): String? = com.example.melati.resolveImageUrl(path)
    fun resolveImageUrl(path: String?, baseUrl: String): String? = com.example.melati.resolveImageUrl(path, baseUrl)
}

object ImageUtils {
    fun resolveImageUrl(path: String?): String? = com.example.melati.resolveImageUrl(path)
    fun resolveImageUrl(path: String?, baseUrl: String): String? = com.example.melati.resolveImageUrl(path, baseUrl)
}
