package com.makiyamasoftware.gerenciadordepagamentos.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.toRoute
import com.makiyamasoftware.gerenciadordepagamentos.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicTopAppBar(
    navController: NavController,
    viewModel: DynamicTopAppBarViewModel,
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val actions = viewModel.actions.collectAsState()

    // Observe the current back stack entry (the current page)
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),

        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
//                       containerColor = MaterialTheme.colorScheme.primaryContainer,
//                        titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = {
                    Text(
                        if (currentDestination?.hasRoute(PaymentsListDestination::class) == true) {
                            stringResource(R.string.topAppBar_Main_title)
                        } else if (currentDestination?.hasRoute(PaymentDetailsDestination::class) == true) {
                            backStackEntry?.toRoute<PaymentDetailsDestination>()?.payment?.titulo ?: stringResource(R.string.generic_caps_null)
                        } else { "" },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    if (currentDestination?.hasRoute(PaymentsListDestination::class) == false)
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(
                                painter = painterResource(R.drawable.arrow_back_24dp),
                                contentDescription = stringResource(R.string.topAppBar_Back_description)
                            )
                        }
                },
                actions = actions.value,
                scrollBehavior = scrollBehavior,
                modifier = modifier
            )
        },
    ) { innerPadding ->
        content(innerPadding)
    }
}

class DynamicTopAppBarViewModel : ViewModel() {
    fun setActions(actions: @Composable (RowScope.() -> Unit)) {
        _actions.value = actions
    }

    private val _actions = MutableStateFlow<@Composable (RowScope.() -> Unit)>({})
    val actions: StateFlow<@Composable (RowScope.() -> Unit)> = _actions
}
