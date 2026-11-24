package com.example.melati

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
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

    val density = LocalDensity.current
    val imeVisible = WindowInsets.ime.getBottom(density) > 0

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.systemBars,
        bottomBar = {
            if (!imeVisible) {
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
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
                .fillMaxSize(),
        ) {
            MelatiNavHost(
                container = container,
                navController = navController,
                snackbarHostState = snackbarHostState,
                modifier = Modifier.fillMaxSize(),
            )
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) { data ->
                Snackbar(
                    snackbarData = data,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                )
            }
        }
    }
}
