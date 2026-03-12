package com.makiyamasoftware.gerenciadordepagamentos.telas.inicio

import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.makiyamasoftware.gerenciadordepagamentos.R
import com.makiyamasoftware.gerenciadordepagamentos.database.HistoricoDePagamento
import com.makiyamasoftware.gerenciadordepagamentos.database.Pagamento
import com.makiyamasoftware.gerenciadordepagamentos.database.Pessoa
import com.makiyamasoftware.gerenciadordepagamentos.pagamentosMainViewModelFake
import com.makiyamasoftware.gerenciadordepagamentos.ui.components.AlertDialogComponent
import com.makiyamasoftware.gerenciadordepagamentos.ui.theme.GerenciadorDePagamentosTheme

private const val DEBUG_TAG = "PagamentosMainScreen"

@Composable
fun PagamentosMainScreen(
    viewModel: PagamentosMainViewModel = viewModel(),
    setTopAppBarActions: (actions: @Composable (RowScope.() -> Unit)) -> Unit,
    onNavigateToDetails: (payment: Pagamento, history: HistoricoDePagamento, person: Pessoa) -> Unit,
    onNavigateToCreateNewPayment: () -> Unit,
    modifier: Modifier,
) {
    val mainPaymentsUIState by viewModel.uiState.collectAsState()
    Log.d(DEBUG_TAG, "mainPaymentsUIState: ${mainPaymentsUIState.paymentsList}")

    LaunchedEffect(
        viewModel.payments,
        viewModel.latestHistories,
        viewModel.latestPeople
    ) {
        viewModel.updateMainPaymentsUIState()
    }

    LaunchedEffect(viewModel) {
        setTopAppBarActions {
            IconButton(onClick = {
                viewModel.toggleDialog()
                Log.d(DEBUG_TAG, "mainPaymentsUIState: ${mainPaymentsUIState.paymentsList}")
            }) {
                Icon(
                    painter = painterResource(R.drawable.delete_forever_24dp),
                    contentDescription = stringResource(R.string.topAppBar_DeleteForever_description)
                )
            }
        }
    }

    SideEffect {
        viewModel.updateDataFromDB()
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = viewModel::onCriarNovoPagamento, //TODO: when CriarPagamento is fully migrated, change to onNavigateToCreateNewPayment
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_round_add_24dp),
                    stringResource(R.string.Label_CriarPagamentoFragment_fragment_name)
                )
            }
        },
        floatingActionButtonPosition = FabPosition.End,
    ) { padding ->
        if (mainPaymentsUIState.showDeleteDialog) {
            AlertDialogComponent(
                title = stringResource(R.string.clear_aviso_titulo),
                message = stringResource(R.string.clear_aviso_mensagem),
                onAffirmativeRequest = {
                    viewModel.onClearAll()
                    viewModel.toggleDialog()
                },
                onDismissRequest = viewModel::toggleDialog,
            )
        }
        PagamentosMainContent(
            mainPaymentsUIState.paymentsList,
            viewModel,
            onNavigateToDetails,
            modifier.padding(padding)
        )
    }
}

@Composable
fun PagamentosMainContent(
    paymentsList: List<Pagamento>,
    viewModel: PagamentosMainViewModel,
    onNavigateToDetails: (Pagamento, HistoricoDePagamento, Pessoa) -> Unit,
    modifier: Modifier = Modifier
) {
    if (paymentsList.isEmpty()) {
        // Centered Text com o Texto provisorio quando nao tem nenhum Pagamento
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center, // Centers children vertically
            horizontalAlignment = Alignment.CenterHorizontally // Centers children horizontally
        ) {
            Text(text = stringResource(R.string.fragment_pagamentos_main_provisorio))
        }
    } else {
        LazyColumn(
            modifier
                .fillMaxSize()
                .padding(horizontal = dimensionResource(R.dimen.margin_normal)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.margin_normal))
        ) {
            items(items = paymentsList) { payment ->
                val history = viewModel.getHistoricoCerto(payment.id)
                val person = viewModel.getPessoaCerta(history?.pagadorId ?: 0L)
                PaymentCard(
                    payment = payment,
                    history = history ?: HistoricoDePagamento(
                        data = "2026-03-07",
                        preco = -1.0,
                        pagadorId = 0L,
                        pagamentoId = payment.id
                    ),
                    onClick = onNavigateToDetails,
                    person = person ?: Pessoa(
                        nome = "NULL",
                        ordem = 0,
                        pagamentoId = payment.id
                    ),
                )
            }
        }
    }
}

@Composable
fun PaymentCard(
    payment: Pagamento,
    history: HistoricoDePagamento,
    onClick: (Pagamento, HistoricoDePagamento, Pessoa) -> Unit,
    person: Pessoa,
    modifier: Modifier = Modifier
) {
    Card(
        modifier
            .fillMaxWidth()
            //.padding(dimensionResource(R.dimen.margin_small))
            .clickable {
                onClick(
                    payment,
                    history,
                    person
                ) // Select this payment, should navigate to PaymentDetails using this Payment, History and Person
            }
    ) {
        PaymentCardContent(
            history = history,
            paymentTitle = payment.titulo,
            personName = person.nome,
            frequency = payment.frequencia
        )
    }
}

@Composable
fun PaymentCardContent(
    history: HistoricoDePagamento,
    paymentTitle: String,
    personName: String,
    frequency: String
) {
    val animatedColor by animateColorAsState(
        if (history.estaPago) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
        label = "statusColor",
        animationSpec = tween(
            durationMillis = 600
        )
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp), horizontalArrangement = Arrangement.Center
    ) {
        Surface(
            color = animatedColor,//if (isPaid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimary,
            //shape = CardDefaults.shape,
            modifier = Modifier
                .weight(0.5f)
                .fillMaxSize()
        ) {
            Column(
                Modifier
                    .padding(dimensionResource(R.dimen.margin_normal))
            ) {
                Text(
                    text = history.getDataString(
                        stringArrayResource(R.array.frequencias_pagamentos),
                        frequency
                    ),
                    Modifier
                        .padding(bottom = dimensionResource(R.dimen.margin_normal))
                        .weight(0.5f)
                )
                Text(
                    text = stringResource(history.getEstaPagoStringID())
                )
            }
        }
        Column(
            modifier = Modifier
                .padding(16.dp)
                .weight(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = paymentTitle,
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(bottom = dimensionResource(R.dimen.margin_small))
            )
            Text(
                text = personName,
                style = MaterialTheme.typography.bodyLarge,
//                modifier = Modifier.padding(bottom = dimensionResource(R.dimen.margin_small))
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
fun PaymentCardPreview() {
    val payment = Pagamento(
        id = 1L,
        titulo = "Spotify",
        dataDeInicio = "2024-12-01",
        numeroDePessoas = 1,
        frequencia = "Diário",
        autoUpdateHistorico = true,
        podeEnviarPush = true
    )
    val history = HistoricoDePagamento(
        id = 1L,
        data = "2024-11-24",
        preco = 10.0,
        pagadorId = 1L,
        pagamentoId = 1L,
        estaPago = false
    )
    val person = Pessoa(
        id = 1L,
        nome = "Eu",
        ordem = 1,
        pagamentoId = 1L
    )

    GerenciadorDePagamentosTheme {
        PaymentCard(
            payment,
            history,
            { payment: Pagamento, history: HistoricoDePagamento, person: Pessoa -> println("clicou") },
            person
        )
    }
}

@Preview(showBackground = false, backgroundColor = 0x000000)
@Composable
fun PagamentosMainPreview() {
    println("Teste: ")
    val navController = rememberNavController()
    GerenciadorDePagamentosTheme {
        PagamentosMainScreen(
            pagamentosMainViewModelFake,
            {},
            { payment: Pagamento, history: HistoricoDePagamento, person: Pessoa ->
                Log.d(
                    "PagamentosMainPreview",
                    "Navigated to Payment Details"
                )
            },
            { Log.d("PagamentosMainPreview", "Navigated to Create New Payment") },
            Modifier
        )
    }
}
