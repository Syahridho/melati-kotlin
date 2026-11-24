package com.example.melati.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiExceptionTest {

    @Test
    fun `403 tanpa pesan kode akses bukan kode tidak valid`() {
        val error = ApiException(
            kind = ApiException.Kind.HTTP,
            serverMessage = "Forbidden",
            httpCode = 403
        )

        assertFalse(error.isUnauthorized)
    }

    @Test
    fun `401 dianggap unauthorized`() {
        val error = ApiException(kind = ApiException.Kind.HTTP, httpCode = 401)

        assertTrue(error.isUnauthorized)
    }

    @Test
    fun `parseApiError membaca 422 complete tanpa field success`() {
        val error = parseApiError(
            code = 422,
            body = """{"message":"Kolom access code wajib diisi.","errors":{"access_code":["Kolom access code wajib diisi."]}}"""
        )

        assertEquals(ApiException.Kind.HTTP, error.kind)
        assertEquals(422, error.httpCode)
        assertEquals("Kolom access code wajib diisi.", error.serverMessage)
        assertEquals("Kolom access code wajib diisi.", error.fieldErrors["access_code"])
    }
}
