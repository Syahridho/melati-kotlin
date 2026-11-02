package com.example.melati.ui.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/*
 * Palet Melati — mengikuti token shadcn/ui yang dipakai web BPS.
 *
 * Aturannya sederhana dan disengaja:
 *  - Latar aplikasi abu sangat muda (#F8FAFC), kartu selalu putih.
 *  - Pemisah visual adalah GARIS 1dp (#E4E4E7), bukan bayangan/elevasi.
 *  - Satu warna aksen saja: biru BPS #005FB6.
 *  - Warna status dipakai hemat: hijau untuk sukses, merah untuk bahaya,
 *    kuning untuk proses berjalan.
 *
 * Mode gelap di luar lingkup v1, jadi hanya skema terang yang didefinisikan.
 */

// ===== Aksen =====
val MelatiPrimary = Color(0xFF005FB6)
val MelatiPrimaryPressed = Color(0xFF004E96)
val MelatiPrimaryTint = Color(0xFFE6F0FA) // primary/10 — latar lembut
val MelatiPrimaryDisabled = Color(0xFF93BEE6) // tombol utama nonaktif
val MelatiRing = Color(0xFF005FB6)

// ===== Netral =====
val MelatiBackground = Color(0xFFF8FAFC)
val MelatiSurface = Color(0xFFFFFFFF)
val MelatiMuted = Color(0xFFF4F4F5)
val MelatiMutedForeground = Color(0xFF71717A)
val MelatiForeground = Color(0xFF09090B)
val MelatiBorder = Color(0xFFE4E4E7)
val MelatiPlaceholder = Color(0xFFA1A1AA)

// ===== Status =====
val MelatiDestructive = Color(0xFFDC2626)
val MelatiDestructiveTint = Color(0xFFFEF2F2)
val MelatiSuccess = Color(0xFF16A34A)
val MelatiSuccessBg = Color(0xFFDCFCE7)
val MelatiWarning = Color(0xFFD97706)
val MelatiWarningBg = Color(0xFFFEF3C7)

// ===== Turunan khusus =====
val MelatiHeaderTint = Color(0xFFF0F7FF) // pita judul kartu form
val MelatiOnPrimaryMuted = Color(0xFFD9E7F4) // teks sekunder di atas biru

/*
 * Skema Material 3 yang dipetakan dari token di atas. Komponen M3 yang masih
 * dipakai (mis. OutlinedTextField, Snackbar) otomatis ikut warna baru.
 */
val MelatiLightColors = lightColorScheme(
    primary = MelatiPrimary,
    onPrimary = Color.White,
    primaryContainer = MelatiPrimaryTint,
    onPrimaryContainer = MelatiPrimaryPressed,

    secondary = MelatiMutedForeground,
    onSecondary = Color.White,
    secondaryContainer = MelatiMuted,
    onSecondaryContainer = MelatiForeground,

    tertiary = MelatiSuccess,
    onTertiary = Color.White,
    tertiaryContainer = MelatiSuccessBg,
    onTertiaryContainer = Color(0xFF052E16),

    error = MelatiDestructive,
    onError = Color.White,
    errorContainer = MelatiDestructiveTint,
    onErrorContainer = Color(0xFF7F1D1D),

    background = MelatiBackground,
    onBackground = MelatiForeground,
    surface = MelatiSurface,
    onSurface = MelatiForeground,
    surfaceVariant = MelatiMuted,
    onSurfaceVariant = MelatiMutedForeground,
    outline = MelatiBorder,
    outlineVariant = MelatiMuted,

    inverseSurface = Color(0xFF18181B),
    inverseOnSurface = Color(0xFFFAFAFA),
    inversePrimary = MelatiPrimaryDisabled,

    surfaceDim = MelatiMuted,
    surfaceBright = MelatiSurface,
    surfaceContainerLowest = MelatiSurface,
    surfaceContainerLow = MelatiSurface,
    surfaceContainer = MelatiMuted,
    surfaceContainerHigh = MelatiMuted,
    surfaceContainerHighest = Color(0xFFE4E4E7),
    scrim = Color(0xFF000000),
)
