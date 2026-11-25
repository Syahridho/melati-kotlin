package com.example.melati

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ImageUrlTest {

    private val base = "https://test.syahridho.my.id/"

    @Test
    fun `path relatif digabung dengan base url`() {
        assertEquals(
            "https://test.syahridho.my.id/storage/x.png",
            resolveImageUrl("storage/x.png", base),
        )
    }

    @Test
    fun `path absolut host salah diambil path-nya saja`() {
        assertEquals(
            "https://test.syahridho.my.id/storage/x.png",
            resolveImageUrl("https://other.host/storage/x.png", base),
        )
    }

    @Test
    fun `slash ganda tidak terbentuk`() {
        assertEquals(
            "https://test.syahridho.my.id/storage/x.png",
            resolveImageUrl("/storage/x.png", base),
        )
    }

    @Test
    fun `path kosong menghasilkan null`() {
        assertNull(resolveImageUrl("", base))
        assertNull(resolveImageUrl(null, base))
    }
}
