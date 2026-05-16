package com.makiyamasoftware.gerenciadordepagamentos.ui.components

import android.util.Log
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.makiyamasoftware.gerenciadordepagamentos.R
import com.makiyamasoftware.gerenciadordepagamentos.eventsanalyser.home.EventsHomeScreen
import com.makiyamasoftware.gerenciadordepagamentos.eventsanalyser.home.EventsHomeViewModel
import com.makiyamasoftware.gerenciadordepagamentos.eventsanalyser.network.EventAnalyserApi
import com.makiyamasoftware.gerenciadordepagamentos.payments.inicio.PagamentosMainViewModel
import com.makiyamasoftware.gerenciadordepagamentos.settings.SettingsRepository
import com.makiyamasoftware.gerenciadordepagamentos.settings.SettingsScreen
import com.makiyamasoftware.gerenciadordepagamentos.settings.SettingsViewModel
import com.makiyamasoftware.gerenciadordepagamentos.settings.dataStore
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

private const val TAG = "NavigationBottomBar"

@Serializable
open class BottomBarDestination(
    val label: String,
    val icon: Int,
    val contentDescription: String
)

// Route for nested graph for Payments Manager
@Serializable
class Payments(
    val route: String = "Payments",
) : BottomBarDestination(
    label = "Payments Manager",
    icon = R.drawable.credit_card_gear_24dp,
    contentDescription = "Payments Manager"
)

// Route for Events Manager
@Serializable
class Events(
    val route: String = "Events",
) : BottomBarDestination(
    label = "Events Manager",
    icon = R.drawable.event_24dp,
    contentDescription = "Events Manager"
)

// Route for app settings
@Serializable
private class Settings() : BottomBarDestination(
    label = "Settings",
    icon = R.drawable.settings_24dp,
    contentDescription = "Settings"
)

@Composable
fun AppNavHost(
    viewModel: PagamentosMainViewModel,
    navController: NavHostController,
    startBottomBarDestination: BottomBarDestination,
    modifier: Modifier = Modifier,
    onShowSnackbar: (message: String, actionLabel: String?, duration: SnackbarDuration, onActionPerformed: () -> Unit, onDismissed: () -> Unit) -> Unit
) {
    val repository = SettingsRepository(LocalContext.current.dataStore)

    NavHost(
        navController,
        startDestination = startBottomBarDestination
    ) {
        // Nested Navigation Graph for Payments Manager

        // Route to PagamentosMain
        mainPagamentosDestination(viewModel, modifier)


        // Events Manager
        eventsAnalyserDestination(
            modifier = modifier,
            onShowSnackbar = onShowSnackbar,
            repository = repository
        )

        composable<Settings> {
            val viewModel: SettingsViewModel =
                viewModel(factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return SettingsViewModel(repository) as T
                    }
                })
            SettingsScreen(viewModel)
            Log.d(TAG, "Navigated to Settings")
        }
    }
}

@Composable
fun NavigationBottomBar(
    paymentsViewModel: PagamentosMainViewModel,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()

    val paymentsBottomDestination = Payments()
    val eventsBottomDestination = Events()
    val settingsBottomDestination = Settings()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    // necessary setup to show a snackbar, if I want to show an action button, I should change the onShowSnackbar parameters to include a onClick
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val onShowSnackbar: (message: String, actionLabel: String?, duration: SnackbarDuration, onActionPerformed: () -> Unit, onDismissed: () -> Unit) -> Unit =
        { message: String, actionLabel: String?, duration: SnackbarDuration, onActionPerformed: () -> Unit, onDismissed: () -> Unit ->
            scope.launch {
                snackbarHostState.currentSnackbarData?.dismiss() // Dismisses the current snackbar if it exists
                val result = snackbarHostState.showSnackbar(
                    message = message,
                    actionLabel = actionLabel,
                    duration = duration,
                    withDismissAction = true
                )
                when (result) {
                    SnackbarResult.ActionPerformed -> onActionPerformed()
                    SnackbarResult.Dismissed -> onDismissed()
                }
            }
        }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        bottomBar = {
            NavigationBar(windowInsets = NavigationBarDefaults.windowInsets) {
                NavigationBarItem(
                    selected = currentDestination?.hasRoute(Payments::class) ?: false,
                    onClick = {
                        navController.navigate(route = paymentsBottomDestination)
                    },
                    icon = {
                        Icon(
                            painter = painterResource(paymentsBottomDestination.icon),
                            contentDescription = paymentsBottomDestination.contentDescription
                        )
                    },
                    label = { Text(paymentsBottomDestination.label) }
                )
                NavigationBarItem(
                    selected = currentDestination?.hasRoute(Events::class) ?: false,
                    onClick = {
                        navController.navigate(route = eventsBottomDestination)
                    },
                    icon = {
                        Icon(
                            painter = painterResource(eventsBottomDestination.icon),
                            contentDescription = eventsBottomDestination.contentDescription
                        )
                    },
                    label = { Text(eventsBottomDestination.label) }
                )
                NavigationBarItem(
                    selected = currentDestination?.hasRoute(Settings::class) ?: false,
                    onClick = {
                        navController.navigate(route = settingsBottomDestination)
                    },
                    icon = {
                        Icon(
                            painter = painterResource(settingsBottomDestination.icon),
                            contentDescription = settingsBottomDestination.contentDescription
                        )
                    },
                    label = { Text(settingsBottomDestination.label) }
                )
            }
        }
    ) { contentPadding ->
        AppNavHost(
            paymentsViewModel,
            navController,
            paymentsBottomDestination,
            modifier = Modifier.padding(contentPadding),
            onShowSnackbar
        )
    }
}
