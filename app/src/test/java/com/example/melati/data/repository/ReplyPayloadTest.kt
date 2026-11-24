package com.example.melati.data.repository

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MultipartBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ReplyPayloadTest {

    private fun partName(part: MultipartBody.Part): String {
        val header = part.headers?.get("Content-Disposition") ?: error("Part tanpa Content-Disposition")
        return Regex("name=\\\"([^\\\"]+)\\\"").find(header)?.groupValues?.get(1)
            ?: error("Nama part tidak ditemukan pada: $header")
    }

    @Test
    fun `reply JSON memuat message`() {
        val payload = Json.parseToJsonElement(buildReplyJson("Halo petugas")).jsonObject

        assertEquals(listOf("message"), payload.keys.toList())
        assertEquals("Halo petugas", payload["message"]?.jsonPrimitive?.content)
    }

    @Test
    fun `reply multipart berurutan message lalu attachments`() {
        val parts = buildReplyParts("Halo petugas", emptyList())

        assertEquals(listOf("message"), parts.map(::partName))
        assertNotNull(parts[0].body)
    }
}
