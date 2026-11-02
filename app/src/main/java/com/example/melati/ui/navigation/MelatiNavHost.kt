package com.example.melati.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.melati.R
import com.example.melati.di.AppContainer
import com.example.melati.ui.screens.cek.CekScreen
import com.example.melati.ui.screens.cek.CekViewModel
import com.example.melati.ui.screens.laporan.LaporanScreen
import com.example.melati.ui.screens.laporan.LaporanViewModel
import com.example.melati.ui.screens.riwayat.RiwayatEvent
import com.example.melati.ui.screens.riwayat.RiwayatScreen
import com.example.melati.ui.screens.riwayat.RiwayatViewModel
import com.example.melati.ui.theme.MelatiBorder
import com.example.melati.ui.theme.MelatiMutedForeground
import com.example.melati.ui.theme.MelatiPrimary
import com.example.melati.ui.theme.MelatiSurface
import androidx.compose.material3.SnackbarHostState

/** Tiga tab utama aplikasi; urutan menentukan urutan di bilah bawah. */
enum class MelatiTab(
    val prefix: String,
    val route: String,
    @param:StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    LAPORAN("laporan", Routes.LAPORAN, R.string.tab_laporan, Icons.Filled.Create),
    CEK("cek", Routes.CEK_BASE, R.string.tab_cek, Icons.Filled.Search),
    RIWAYAT("riwayat", Routes.RIWAYAT, R.string.tab_riwayat, Icons.AutoMirrored.Filled.List),
}

/** Tab dianggap aktif bila rute saat ini diawali prefix tab (tanpa argumen). */
fun isTabSelected(currentRoute: String?, tab: MelatiTab): Boolean =
    currentRoute?.substringBefore("?")?.startsWith(tab.prefix) == true

/**
 * Bilah navigasi bawah bergaya shadcn: permukaan putih, garis 1dp di atas,
 * ikon+label biru untuk tab aktif dan abu untuk yang lain. Tanpa pil indikator
 * Material dan tanpa bayangan.
 */
@Composable
fun MelatiBottomBar(
    currentRoute: String?,
    onSelect: (MelatiTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MelatiSurface),
    ) {
        HorizontalDivider(thickness = 1.dp, color = MelatiBorder)
        NavigationBar(
            containerColor = MelatiSurface,
            tonalElevation = 0.dp,
        ) {
            MelatiTab.entries.forEach { tab ->
                val selected = isTabSelected(currentRoute, tab)
                NavigationBarItem(
                    selected = selected,
                    onClick = { onSelect(tab) },
                    icon = {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = null,
                        )
                    },
                    label = {
                        Text(
                            text = stringResource(tab.labelRes),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MelatiPrimary,
                        selectedTextColor = MelatiPrimary,
                        indicatorColor = Color.Transparent,
                        unselectedIconColor = MelatiMutedForeground,
                        unselectedTextColor = MelatiMutedForeground,
                    ),
                )
            }
        }
    }
}

@Composable
fun MelatiNavHost(
    container: AppContainer,
    navController: NavHostController,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    NavHost(
        navController = navController,
        startDestination = Routes.LAPORAN,
        modifier = modifier
    ) {
        composable(Routes.LAPORAN) {
            val viewModel: LaporanViewModel = viewModel(
                factory = viewModelFactory {
                    initializer { LaporanViewModel(container.ticketRepository, context.applicationContext) }
                }
            )
            val state by viewModel.state.collectAsStateWithLifecycle()
            LaporanScreen(
                state = state,
                onEvent = viewModel::onEvent,
                snackbarHostState = snackbarHostState
            )
        }

        composable(
            route = Routes.CEK,
            arguments = listOf(
                navArgument(Routes.ARG_NOMOR) {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { entry ->
            val nomor = entry.arguments?.getString(Routes.ARG_NOMOR).orEmpty()
            val viewModel: CekViewModel = viewModel(
                factory = viewModelFactory {
                    initializer { CekViewModel(container.ticketRepository, context.applicationContext) }
                }
            )
            val state by viewModel.state.collectAsStateWithLifecycle()

            LaunchedEffect(nomor) { viewModel.loadNumber(nomor) }

            CekScreen(
                state = state,
                onEvent = viewModel::onEvent,
                snackbarHostState = snackbarHostState
            )
        }

        composable(Routes.RIWAYAT) {
            val viewModel: RiwayatViewModel = viewModel(
                factory = viewModelFactory {
                    initializer { RiwayatViewModel(container.ticketRepository) }
                }
            )
            val state by viewModel.state.collectAsStateWithLifecycle()

            RiwayatScreen(
                state = state,
                onEvent = { event ->
                    if (event is RiwayatEvent.Open) {
                        navController.navigate(Routes.cek(event.number)) { launchSingleTop = true }
                    } else {
                        viewModel.onEvent(event)
                    }
                }
            )
        }
    }
}
