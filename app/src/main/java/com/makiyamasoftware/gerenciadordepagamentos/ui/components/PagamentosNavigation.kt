package com.makiyamasoftware.gerenciadordepagamentos.ui.components

import android.os.Bundle
import android.util.Log
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.makiyamasoftware.gerenciadordepagamentos.database.HistoricoDePagamento
import com.makiyamasoftware.gerenciadordepagamentos.database.Pagamento
import com.makiyamasoftware.gerenciadordepagamentos.database.Pessoa
import com.makiyamasoftware.gerenciadordepagamentos.telas.detalhes.DetalhesPagamentoScreen
import com.makiyamasoftware.gerenciadordepagamentos.telas.inicio.PagamentosMainScreen
import com.makiyamasoftware.gerenciadordepagamentos.telas.inicio.PagamentosMainViewModel
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
object PaymentsListDestination : PaymentsDestination

@Serializable
data class PaymentDetailsDestination(
    val payment: Pagamento,
    val history: HistoricoDePagamento,
    val person: Pessoa
) : PaymentsDestination

@Serializable
sealed interface PaymentsDestination

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
        composable<PaymentsListDestination> {
            PagamentosMainScreen(
                viewModel,
                topAppBarViewModel::setActions,
                { payment: Pagamento, history: HistoricoDePagamento, person: Pessoa ->
                    Log.d(
                        "PagamentosMainPreview",
                        "Navigated to Details of $payment, $history and $person"
                    )
                    navController.navigate(
                        route = PaymentDetailsDestination(
                            payment = payment,
                            history = history,
                            person = person
                        )
                    )
                },
                { Log.d("PagamentosMainPreview", "Navigated on Create New Payment") },
                modifier = Modifier
            )
        }
        composable<PaymentDetailsDestination>(
            typeMap = mapOf(
                typeOf<Pagamento>() to PaymentParameterType,
                typeOf<HistoricoDePagamento>() to HistoryParameterType,
                typeOf<Pessoa>() to PersonParameterType
            )
        ) { backStackEntry ->
            val details: PaymentDetailsDestination = backStackEntry.toRoute()

            DetalhesPagamentoScreen(
                dataSource = viewModel.database,
                selectedPayment = details.payment,
                latestPaymentHistory = details.history,
                latestPerson = details.person,
                setTopAppBarActions = topAppBarViewModel::setActions,
                onNavigateUp = navController::navigateUp
            )
        }
    }
}

@Composable
fun PagamentosNavigation(
    paymentsViewModel: PagamentosMainViewModel,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val startDestination = PaymentsListDestination
    val topAppBarViewModel: DynamicTopAppBarViewModel = viewModel()

    DynamicTopAppBar(
        navController = navController,
        viewModel = topAppBarViewModel
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
