package com.example.melati

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.melati.di.AppContainer
import com.example.melati.ui.navigation.MelatiBottomBar
import com.example.melati.ui.navigation.MelatiNavHost
import com.example.melati.ui.navigation.isTabSelected
import com.example.melati.ui.screens.splash.SplashScreen
import com.example.melati.ui.theme.MelatiTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as MelatiApplication).container

        setContent {
            MelatiTheme {
                var showSplash by rememberSaveable { mutableStateOf(true) }

                LaunchedEffect(Unit) {
                    delay(SplashState.VISIBLE_MILLIS)
                    showSplash = false
                }

                Crossfade(
                    targetState = showSplash,
                    animationSpec = tween(durationMillis = SplashState.FADE_MILLIS),
                    label = "splash",
                ) { splashVisible ->
                    if (splashVisible) {
                        SplashScreen()
                    } else {
                        MelatiApp(container)
                    }
                }
            }
        }
    }
}

/**
 * Durasi layar pembuka.
 *
 * Sengaja singkat: PRD §6 menuntut layar utama muncul < 2 detik, dan splash ini
 * menambah waktu ke frame pertama yang berisi konten. Nilai di bawah membuat
 * total cold start rilis (±0,8 dtk di perangkat uji) tetap di bawah 2 detik.
 */
private object SplashState {
    const val VISIBLE_MILLIS = 700L
    const val FADE_MILLIS = 200
}

@Composable
private fun MelatiApp(container: AppContainer) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            MelatiBottomBar(
                currentRoute = currentRoute,
                onSelect = { tab ->
                    if (!isTabSelected(currentRoute, tab)) {
                        navController.navigate(tab.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
            )
        }
    ) { innerPadding ->
        MelatiNavHost(
            container = container,
            navController = navController,
            snackbarHostState = snackbarHostState,
            modifier = Modifier.padding(innerPadding)
        )
    }
}
