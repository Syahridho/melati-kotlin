package com.example.melati.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

/**
 * Sudut membulat mengikuti shadcn/ui: input & tombol 8dp, kartu & dialog 12dp.
 * Tidak ada sudut besar khas Material (28dp) di aplikasi ini.
 */
val MelatiShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(12.dp),
)

/**
 * Tema Melati — hanya mode terang. Mode gelap di luar lingkup v1, sehingga
 * skema selalu [MelatiLightColors] dan tidak mengikuti pengaturan sistem.
 */
@Composable
fun MelatiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MelatiLightColors,
        typography = MelatiTypography,
        shapes = MelatiShapes,
        content = content,
    )
}
