package com.example.melati.ui.screens.laporan

fun resolveImageUrl(path: String?): String? = com.example.melati.resolveImageUrl(path)

fun resolveImageUrl(path: String?, baseUrl: String): String? = com.example.melati.resolveImageUrl(path, baseUrl)

object ImageUrlResolver {
    fun resolve(path: String?): String? = com.example.melati.resolveImageUrl(path)
    fun resolve(path: String?, baseUrl: String): String? = com.example.melati.resolveImageUrl(path, baseUrl)
    fun resolveImageUrl(path: String?): String? = com.example.melati.resolveImageUrl(path)
    fun resolveImageUrl(path: String?, baseUrl: String): String? = com.example.melati.resolveImageUrl(path, baseUrl)
}
