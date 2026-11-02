package com.example.melati.ui.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.melati.R
import com.example.melati.ui.theme.MelatiTheme

private const val SplashFadeMillis = 300

/**
 * Splash singkat (~0,9 detik): logo BPS–melati di atas, wordmark "Melati"
 * di bawah. Satu gerak halus saja — fade-in latar/isi 300 ms (dapat
 * terpotong tanpa merusak pengalaman karena tidak ada teks atau spinner).
 */
@Composable
fun SplashScreen(modifier: Modifier = Modifier) {
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        alpha.animateTo(1f, tween(durationMillis = SplashFadeMillis))
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .alpha(alpha.value)
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        // Batas lebar terdefinisi agar fillMaxWidth(f) pada Image valid
        // (bukan ukuran intrinsik px drawable) dan tetap terpusat vertikal
        // oleh Box di atas.
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Logo: porsi sedikit lebih besar sebagai elemen utama; tinggi
            // mengikuti rasio asli 546×457 via ContentScale.Fit.
            Image(
                painter = painterResource(R.drawable.melati_big),
                contentDescription = null, // dekoratif
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth(0.62f)
                    .widthIn(max = 280.dp)
                    .aspectRatio(546f / 457f),
            )
            Spacer(Modifier.height(28.dp))
            // Wordmark + tagline dalam satu gambar; rasio asli 669×373.
            Image(
                painter = painterResource(R.drawable.desc_melati2),
                contentDescription = stringResource(R.string.cd_logo_melati),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth(0.58f)
                    .widthIn(max = 300.dp)
                    .aspectRatio(669f / 373f),
            )
        }
    }
}

@Preview(name = "Splash", showBackground = true, widthDp = 393, heightDp = 800)
@Composable
private fun PreviewSplash() {
    MelatiTheme {
        SplashScreen(modifier = Modifier.fillMaxSize())
    }
}
