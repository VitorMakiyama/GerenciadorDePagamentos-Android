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
import androidx.compose.runtime.LaunchedEffect
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
import com.makiyamasoftware.gerenciadordepagamentos.R
import com.makiyamasoftware.gerenciadordepagamentos.database.Pagamento
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicTopAppBar(
    navController: NavController,
    viewModel: DynamicTopAppBarViewModel,
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val actions by viewModel.actions.collectAsState()
    val selectedPayment by viewModel.payment.collectAsState()


    // Observe the current back stack entry (the current page)
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    LaunchedEffect(currentDestination) {
        viewModel.setPayment() // Para redefinir o payment da appTopBar com o valor default
    }

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
                            selectedPayment.titulo
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
                actions = actions,
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

    private val defaultPayment = Pagamento(-1L, "", "", 0, "")
    private val _payment: MutableStateFlow<Pagamento> = MutableStateFlow(defaultPayment)
    val payment: StateFlow<Pagamento> = _payment

    fun setPayment(newPayment: Pagamento = defaultPayment) {
        _payment.update {
            newPayment.copy()
        }
    }

}
