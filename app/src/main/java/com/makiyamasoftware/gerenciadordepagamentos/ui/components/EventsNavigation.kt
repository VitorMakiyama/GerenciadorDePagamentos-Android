package com.makiyamasoftware.gerenciadordepagamentos.ui.components

import android.util.Log
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.makiyamasoftware.gerenciadordepagamentos.eventsanalyser.home.EventsHomeScreen
import com.makiyamasoftware.gerenciadordepagamentos.eventsanalyser.home.EventsHomeViewModel
import com.makiyamasoftware.gerenciadordepagamentos.eventsanalyser.network.EventAnalyserApi
import com.makiyamasoftware.gerenciadordepagamentos.eventsanalyser.reports.EventsReportsScreen
import com.makiyamasoftware.gerenciadordepagamentos.settings.SettingsRepository
import kotlinx.serialization.Serializable

private const val TAG = "EventsNavigation"

interface EventsDestination {
    val route: String
}

@Serializable
data class EventsHomeDestination(override val route: String = "Events Home") : EventsDestination

@Serializable
data class EventsReportsDestination(override val route: String = "Events Reports") :
    EventsDestination

fun NavGraphBuilder.eventsAnalyserDestination(
    modifier: Modifier,
    onShowSnackbar: (message: String, actionLabel: String?, duration: SnackbarDuration, onActionPerformed: () -> Unit, onDismissed: () -> Unit) -> Unit,
    repository: SettingsRepository
) {
    composable<Events> {
        EventsNavigation(
            modifier = modifier,
            onShowSnackbar = onShowSnackbar,
            repository = repository,
        )
    }
}

@Composable
fun EventsNavigation(
    modifier: Modifier = Modifier,
    onShowSnackbar: (message: String, actionLabel: String?, duration: SnackbarDuration, onActionPerformed: () -> Unit, onDismissed: () -> Unit) -> Unit,
    repository: SettingsRepository
) {
    val navController = rememberNavController()
    val startDestination = EventsHomeDestination()

    EventsNavHost(
        navController = navController,
        startDestination = startDestination,
        onShowSnackbar,
        repository = repository,
        modifier
    )
}

@Composable
fun EventsNavHost(
    navController: NavHostController,
    startDestination: EventsHomeDestination,
    onShowSnackbar: (message: String, actionLabel: String?, duration: SnackbarDuration, onActionPerformed: () -> Unit, onDismissed: () -> Unit) -> Unit,
    repository: SettingsRepository,
    modifier: Modifier
) {
    NavHost(
        navController,
        startDestination,
        modifier
    ) {
        // Events Home
        eventsHomeDestination(
            onShowSnackbar = onShowSnackbar,
            repository = repository,
            onNavigateToEventsReports = {
                navController.navigate(route = EventsReportsDestination())
            }
        )

        // Events Reports
        eventsReportsDestination()
    }
}

fun NavGraphBuilder.eventsHomeDestination(
    onShowSnackbar: (message: String, actionLabel: String?, duration: SnackbarDuration, onActionPerformed: () -> Unit, onDismissed: () -> Unit) -> Unit,
    onNavigateToEventsReports: () -> Unit,
    repository: SettingsRepository
) {
    composable<EventsHomeDestination> {
        val viewModel: EventsHomeViewModel =
            viewModel(factory = object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return EventsHomeViewModel(EventAnalyserApi.getService(repository)) as T
                }
            })
        EventsHomeScreen(
            viewModel = viewModel,
            onShowSnackbar = onShowSnackbar,
            onNavigateToEventsReports = onNavigateToEventsReports
        )
        Log.d(TAG, "Navigated to Events Manager")
    }
}

fun NavGraphBuilder.eventsReportsDestination() {
    composable<EventsReportsDestination> {
        EventsReportsScreen()
    }
    Log.d(TAG, "Navigated to Events Reports")
}
