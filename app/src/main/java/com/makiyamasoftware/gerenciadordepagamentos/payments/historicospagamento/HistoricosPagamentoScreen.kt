package com.makiyamasoftware.gerenciadordepagamentos.payments.historicospagamento

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.AndroidUiModes.UI_MODE_NIGHT_YES
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.makiyamasoftware.gerenciadordepagamentos.R
import com.makiyamasoftware.gerenciadordepagamentos.database.HistoricoDePagamento
import com.makiyamasoftware.gerenciadordepagamentos.database.Pagamento
import com.makiyamasoftware.gerenciadordepagamentos.database.PagamentosDatabaseDao
import com.makiyamasoftware.gerenciadordepagamentos.database.Pessoa
import com.makiyamasoftware.gerenciadordepagamentos.getPessoaCerta
import com.makiyamasoftware.gerenciadordepagamentos.ui.components.AlertDialog
import com.makiyamasoftware.gerenciadordepagamentos.ui.components.PaymentHistoryCard
import com.makiyamasoftware.gerenciadordepagamentos.ui.theme.GerenciadorDePagamentosTheme

private const val TAG = "HistoricosPagamentoScreen"

@Composable
fun HistoricosPagamentoScreen(
    dataSource: PagamentosDatabaseDao,
    payment: Pagamento,
    setTopAppBarActions: (actions: @Composable (RowScope.() -> Unit)) -> Unit,
    setTopAppBarPayment: (payment: Pagamento) -> Unit,
) {
    val factory = remember {
        HistoricosPagamentoViewModelFactory(
            dataSource = dataSource,
            pagamento = payment
        )
    }
    val viewModel: HistoricosPagamentoViewModel = viewModel(factory = factory)
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        /**
         * Runs a block of asynchronous code or suspend functions exactly once when a composable enters the composition
         */
        setTopAppBarPayment(payment)
        setTopAppBarActions {
            // If necessary, use this to populate the Top App Bar
        }
    }

    LaunchedEffect(uiState.updateData) {
        Log.d(TAG, "Getting histories and people from DB...")
        viewModel.updateDataFromDB()
    }

    if (uiState.statusChangeType != null) {
        lateinit var message: String
        lateinit var affirmativeText: String
        var thirdOption = ""

        when (uiState.statusChangeType) {
            StatusChangeType.SINGULAR -> {
                message =
                    stringResource(R.string.historicosPagamentoFragment_status_alertMessage_unico) + when (viewModel.getHistoricoClicado().estaPago) {
                        true -> stringResource(R.string.blocoEstaPago_status_naoPago)
                        false -> stringResource(R.string.blocoEstaPago_status_pago)
                    }
                affirmativeText = stringResource(R.string.generic_Sim)
            }

            StatusChangeType.MULTIPLE -> {
                message =
                    stringResource(R.string.historicosPagamentoFragment_status_alertMessage_multiplos)
                affirmativeText = stringResource(R.string.generic_button_OnlyThis)
                thirdOption = stringResource(R.string.generic_Sim)
            }

            else -> {}
        }
        AlertDialog(
            title = stringResource(R.string.historicosPagamentoFragment_status_alertTitle),
            message = message,
            affirmativeText = affirmativeText,
            onAffirmativeRequest = {
                viewModel.onUpdateSingleStatus()
                viewModel.onDismissChangeStatusAlertDialog()
                                   },
            thirdActionText = thirdOption,
            onThirdRequest = {
                viewModel.onUpdateSingleStatus()
                viewModel.onUpdateMultipleStatus()
                viewModel.onDismissChangeStatusAlertDialog()
            },
            onDismissRequest = viewModel::onDismissChangeStatusAlertDialog
        )
    }
    PaymentHistoriesContent(
        payment = payment,
        histories = uiState.histories,
        onClickStatus = viewModel::onClickStatus,
        people = uiState.people,
    )
}

@Composable
fun PaymentHistoriesContent(
    modifier: Modifier = Modifier,
    payment: Pagamento,
    histories: List<HistoricoDePagamento>,
    onClickStatus: (HistoricoDePagamento) -> Unit,
    people: List<Pessoa>
) {
    LazyColumn(
        modifier
            .fillMaxSize()
            .padding(horizontal = dimensionResource(R.dimen.margin_normal)),
        verticalArrangement = Arrangement.Top
    ) {
        items(histories) { history ->
            PaymentHistoryCard(
                history = history,
                isPaid = history.estaPago,
                price = history.preco,
                onUpdatePaymentStatus = { onClickStatus(history) },
                personName = getPessoaCerta(pessoas = people, pessoaID = history.pagadorId).nome,
                frequency = payment.frequencia,
                modifier = modifier,
            )
        }
    }
}

@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES, heightDp = 400)
@Composable
fun PaymentHistoriesContentPreview() {
    val payment = Pagamento(
        id = 1,
        titulo = "Payment 1",
        dataDeInicio = "2026-05-28",
        numeroDePessoas = 2,
        frequencia = stringArrayResource(R.array.frequencias_pagamentos)[2]
    )

    val histories = listOf(
        HistoricoDePagamento(
            data = "2026-01-01",
            preco = 10.0,
            pagadorId = 1,
            pagamentoId = 1
        ), HistoricoDePagamento(
            data = "2026-02-01",
            preco = 10.0,
            pagadorId = 2,
            pagamentoId = 1
        ), HistoricoDePagamento(
            data = "2026-03-01",
            preco = 50.0,
            pagadorId = 1,
            pagamentoId = 1
        ), HistoricoDePagamento(
            data = "2026-04-01",
            preco = 50.0,
            pagadorId = 2,
            pagamentoId = 1
        ), HistoricoDePagamento(
            data = "2026-05-01",
            preco = 50.0,
            pagadorId = 1,
            pagamentoId = 1
        )
    )

    val people = listOf(
        Pessoa(
            id = 1L,
            nome = "Person 1",
            ordem = 1,
            pagamentoId = 1L
        ),
        Pessoa(
            id = 2L,
            nome = "Person 2",
            ordem = 2,
            pagamentoId = 1L
        )
    )

    GerenciadorDePagamentosTheme {
        PaymentHistoriesContent(
            payment = payment,
            histories = histories,
            onClickStatus = {},
            people = people
        )
    }
}
