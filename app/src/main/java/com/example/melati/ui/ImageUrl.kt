package com.example.melati.ui

fun resolveImageUrl(path: String?): String? = com.example.melati.resolveImageUrl(path)

fun resolveImageUrl(path: String?, baseUrl: String): String? = com.example.melati.resolveImageUrl(path, baseUrl)

object ImageUrlResolver {
    fun resolve(path: String?): String? = com.example.melati.resolveImageUrl(path)
    fun resolve(path: String?, baseUrl: String): String? = com.example.melati.resolveImageUrl(path, baseUrl)
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
