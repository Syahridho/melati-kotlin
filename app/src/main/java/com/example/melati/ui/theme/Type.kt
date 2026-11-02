package com.example.melati.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/*
 * Tipografi Melati.
 *
 * Satu keluarga huruf saja: sans-serif sistem (Roboto). Tidak ada serif dan
 * tidak ada font unduhan. Hierarki dibentuk oleh UKURAN dan KETEBALAN, bukan
 * oleh ganti jenis huruf — sama seperti shadcn/ui di web:
 *
 *   judul layar   20sp semibold
 *   judul kartu   18sp semibold
 *   label seksi   14sp semibold
 *   isi           14sp regular
 *   keterangan    13sp regular / 12sp untuk label kecil
 */

private val Sans = FontFamily.SansSerif

private fun sans(
    size: Int,
    lineHeight: Int,
    weight: FontWeight = FontWeight.Normal,
    letterSpacing: Float = 0f,
) = TextStyle(
    fontFamily = Sans,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    fontWeight = weight,
    letterSpacing = letterSpacing.sp,
)

val MelatiTypography = Typography(
    displayLarge = sans(40, 48, FontWeight.SemiBold, -0.4f),
    displayMedium = sans(34, 42, FontWeight.SemiBold, -0.4f),
    displaySmall = sans(28, 36, FontWeight.SemiBold, -0.2f),

    // Judul layar.
    headlineLarge = sans(24, 32, FontWeight.SemiBold, -0.2f),
    headlineMedium = sans(20, 28, FontWeight.SemiBold, -0.1f),
    // Judul kartu / judul percakapan.
    headlineSmall = sans(18, 26, FontWeight.SemiBold),

    // Judul kartu ringkasan & nomor tiket.
    titleLarge = sans(18, 26, FontWeight.SemiBold),
    // Label seksi di atas input.
    titleMedium = sans(14, 20, FontWeight.SemiBold),
    titleSmall = sans(14, 20, FontWeight.Medium),

    bodyLarge = sans(16, 24),
    bodyMedium = sans(14, 20),
    bodySmall = sans(13, 18),

    labelLarge = sans(14, 20, FontWeight.Medium),
    labelMedium = sans(13, 18, FontWeight.Medium, 0.1f),
    labelSmall = sans(12, 16, FontWeight.Medium, 0.1f),
)
