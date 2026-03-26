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
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.makiyamasoftware.gerenciadordepagamentos.R
import com.makiyamasoftware.gerenciadordepagamentos.eventsanalyser.home.EventsHomeScreen
import com.makiyamasoftware.gerenciadordepagamentos.telas.inicio.PagamentosMainViewModel
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
open class BottomBarDestination

// Route for nested graph for Payments Manager
@Serializable
class Payments(
    val route: String = "Payments",
    val label: String = "Payments Manager",
    public val icon: Int = R.drawable.credit_card_gear_24dp,
    val contentDescription: String = "Payments Manager"
) : BottomBarDestination()

// Route for Events Manager
@Serializable
class Events(
    val route: String = "Events",
    val label: String = "Events Manager",
    val icon: Int = R.drawable.event_24dp,
    val contentDescription: String = "Events Manager"
) : BottomBarDestination()

@Composable
fun AppNavHost(
    viewModel: PagamentosMainViewModel,
    navController: NavHostController,
    startBottomBarDestination: BottomBarDestination,
    modifier: Modifier = Modifier,
    onShowSnackbar: (message: String, actionLabel: String?, duration: SnackbarDuration, onActionPerformed: () -> Unit, onDismissed: () -> Unit) -> Unit
) {
    NavHost(
        navController,
        startDestination = startBottomBarDestination
    ) {
        // Nested Navigation Graph for Payments Manager

        // Route to PagamentosMain
        mainPagamentosDestination(viewModel, modifier)


        // Events Manager
        composable<Events> {
            EventsHomeScreen(onShowSnackbar = onShowSnackbar)
            Log.d("NavigationBottomBar", "Navigated to Events Manager")
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
//                BottomBarDestination.entries.forEachIndexed { index, destination ->
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
//                }
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
