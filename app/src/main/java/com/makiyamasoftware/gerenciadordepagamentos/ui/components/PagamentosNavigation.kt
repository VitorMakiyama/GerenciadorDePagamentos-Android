package com.makiyamasoftware.gerenciadordepagamentos.ui.components

import android.os.Bundle
import android.util.Log
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.makiyamasoftware.gerenciadordepagamentos.database.HistoricoDePagamento
import com.makiyamasoftware.gerenciadordepagamentos.database.Pagamento
import com.makiyamasoftware.gerenciadordepagamentos.database.PagamentosDatabaseDao
import com.makiyamasoftware.gerenciadordepagamentos.database.Pessoa
import com.makiyamasoftware.gerenciadordepagamentos.payments.detalhes.DetalhesPagamentoScreen
import com.makiyamasoftware.gerenciadordepagamentos.payments.historicospagamento.HistoricosPagamentoScreen
import com.makiyamasoftware.gerenciadordepagamentos.payments.inicio.PagamentosMainScreen
import com.makiyamasoftware.gerenciadordepagamentos.payments.inicio.PagamentosMainViewModel
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.reflect.typeOf

internal val PaymentParameterType = object : NavType<Pagamento>(
    isNullableAllowed = false
) {
    override fun get(bundle: Bundle, key: String): Pagamento? =
        bundle.getString(key)?.let { parseValue(it) }

    override fun put(bundle: Bundle, key: String, value: Pagamento) {
        bundle.putString(key, serializeAsValue(value))
    }

    override fun parseValue(value: String): Pagamento = Json.decodeFromString(value)

    override fun serializeAsValue(value: Pagamento): String = Json.encodeToString(value)
}
internal val HistoryParameterType = object : NavType<HistoricoDePagamento>(
    isNullableAllowed = false
) {
    override fun get(bundle: Bundle, key: String): HistoricoDePagamento? =
        bundle.getString(key)?.let { parseValue(it) }

    override fun put(bundle: Bundle, key: String, value: HistoricoDePagamento) {
        bundle.putString(key, serializeAsValue(value))
    }

    override fun parseValue(value: String): HistoricoDePagamento = Json.decodeFromString(value)

    override fun serializeAsValue(value: HistoricoDePagamento): String = Json.encodeToString(value)
}
internal val PersonParameterType = object : NavType<Pessoa>(
    isNullableAllowed = false
) {
    override fun get(bundle: Bundle, key: String): Pessoa? =
        bundle.getString(key)?.let { parseValue(it) }

    override fun put(bundle: Bundle, key: String, value: Pessoa) {
        bundle.putString(key, serializeAsValue(value))
    }

    override fun parseValue(value: String): Pessoa = Json.decodeFromString(value)

    override fun serializeAsValue(value: Pessoa): String = Json.encodeToString(value)
}

@Serializable
object MainPaymentsList : PaymentsDestination

@Serializable
data class PaymentDetails(
    val payment: Pagamento,
    val history: HistoricoDePagamento,
    val person: Pessoa
) : PaymentsDestination

@Serializable
data class AllPaymentHistories(
    val payment: Pagamento
) : PaymentsDestination

@Serializable
sealed interface PaymentsDestination

fun NavGraphBuilder.paymentsListDestination(
    viewModel: PagamentosMainViewModel,
    topAppBarViewModel: DynamicTopAppBarViewModel,
    // Navigation events are exposed to the caller to be handled at a higher level
    onNavigateToPaymentDetails: (payment: Pagamento, history: HistoricoDePagamento, person: Pessoa) -> Unit,
    modifier: Modifier
) {
    composable<MainPaymentsList> {
        PagamentosMainScreen(
            viewModel,
            topAppBarViewModel::setActions,
            onNavigateToPaymentDetails,
            { Log.d("PagamentosMainPreview", "Navigated on Create New Payment") },
            modifier = modifier
        )
    }
}

fun NavController.navigateToPaymentDetails(
    payment: Pagamento,
    history: HistoricoDePagamento,
    person: Pessoa
) {
    Log.d(
        "PagamentosMainPreview",
        "Navigated to Details of $payment, $history and $person"
    )
    navigate(
        route = PaymentDetails(
            payment = payment,
            history = history,
            person = person
        )
    )
}

fun NavController.navigateToAllHistories(
    payment: Pagamento
) {
    Log.d(
        "PagamentosMainPreview",
        "Navigated to All Histories of $payment"
    )
    navigate(
        route = AllPaymentHistories(
            payment = payment,
        )
    )
}

fun NavGraphBuilder.detalhesPagamentoDestination(
    viewModel: PagamentosMainViewModel,
    topAppBarViewModel: DynamicTopAppBarViewModel,
    onNavigateUp: () -> Unit,
    onNavigateToAllHistories: (Pagamento) -> Unit
) {
    composable<PaymentDetails>(
        typeMap = mapOf(
            typeOf<Pagamento>() to PaymentParameterType,
            typeOf<HistoricoDePagamento>() to HistoryParameterType,
            typeOf<Pessoa>() to PersonParameterType
        )
    ) { backStackEntry ->
        val details: PaymentDetails = backStackEntry.toRoute()
        topAppBarViewModel.setPayment(details.payment)

        DetalhesPagamentoScreen(
            dataSource = viewModel.database,
            selectedPayment = details.payment,
            latestPaymentHistory = details.history,
            latestPerson = details.person,
            setTopAppBarActions = topAppBarViewModel::setActions,
            setTopAppBarPayment = topAppBarViewModel::setPayment,
            onNavigateUp = onNavigateUp,
            onNavigateToAllHistories = { onNavigateToAllHistories(details.payment) }
        )
    }
}

fun NavGraphBuilder.historicosPagamentoDestination(
    dataSource: PagamentosDatabaseDao,
) {
    composable<AllPaymentHistories>(
        typeMap = mapOf(
            typeOf<Pagamento>() to PaymentParameterType
        )
    ) { backStackEntry ->
        val allHistories: AllPaymentHistories = backStackEntry.toRoute()

        HistoricosPagamentoScreen(
            dataSource = dataSource,
            payment = allHistories.payment
        )
    }
}

@Composable
fun PaymentsNavHost(
    viewModel: PagamentosMainViewModel,
    topAppBarViewModel: DynamicTopAppBarViewModel,
    navController: NavHostController,
    startDestination: PaymentsDestination,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // Route to PagamentosMain
        paymentsListDestination(
            viewModel = viewModel,
            topAppBarViewModel = topAppBarViewModel,
            onNavigateToPaymentDetails = { payment, history, person ->
                navController.navigateToPaymentDetails(payment, history, person)
            },
            modifier
        )

        // Route to DetalhesPagamento
        detalhesPagamentoDestination(
            viewModel = viewModel,
            topAppBarViewModel = topAppBarViewModel,
            onNavigateUp = navController::navigateUp,
            onNavigateToAllHistories = { payment ->
                navController.navigateToAllHistories(payment)
            }
        )

        // Route to HistoricosPagamentoScreen
        historicosPagamentoDestination(
            dataSource = viewModel.database,
        )
    }
}

fun NavGraphBuilder.mainPagamentosDestination(
    paymentsViewModel: PagamentosMainViewModel,
    modifier: Modifier
) {
    composable<Payments> {
        PagamentosNavigation(paymentsViewModel = paymentsViewModel, modifier = modifier)
    }
}

@Composable
fun PagamentosNavigation(
    paymentsViewModel: PagamentosMainViewModel,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val startDestination = MainPaymentsList
    val topAppBarViewModel: DynamicTopAppBarViewModel = viewModel()

    DynamicTopAppBar(
        navController = navController,
        viewModel = topAppBarViewModel,
        modifier = modifier
    ) { contentPadding ->
        PaymentsNavHost(
            paymentsViewModel,
            topAppBarViewModel,
            navController,
            startDestination,
            modifier = Modifier.padding(contentPadding)
        )
    }
}
