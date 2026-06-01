package com.makiyamasoftware.gerenciadordepagamentos.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.makiyamasoftware.gerenciadordepagamentos.R
import com.makiyamasoftware.gerenciadordepagamentos.database.HistoricoDePagamento
import com.makiyamasoftware.gerenciadordepagamentos.formatReadablePrice
import com.makiyamasoftware.gerenciadordepagamentos.ui.theme.GerenciadorDePagamentosTheme

@Composable
fun PaymentHistoryCard(
    history: HistoricoDePagamento,
    isPaid: Boolean,
    price: Double,
    onUpdatePaymentStatus: () -> Unit,
    personName: String,
    frequency: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier
            .fillMaxWidth()
            .padding(dimensionResource(R.dimen.margin_small))
    ) {
        PaymentHistoryCardContent(
            history = history,
            isPaid = isPaid,
            price = price,
            onUpdatePaymentStatus = onUpdatePaymentStatus,
            personName = personName,
            frequency = frequency
        )
    }
}

@Composable
fun PaymentHistoryCardContent(
    history: HistoricoDePagamento,
    isPaid: Boolean,
    price: Double,
    onUpdatePaymentStatus: () -> Unit,
    frequency: String,
    personName: String,
) {
    val animatedBackgroundColor by animateColorAsState(
        if (isPaid) colorScheme.tertiaryContainer else colorScheme.error,
        label = "statusColor",
        animationSpec = tween(
            durationMillis = 600
        )
    )
    val animatedTextColor by animateColorAsState(
        targetValue = if (history.estaPago) colorScheme.onTertiaryContainer else colorScheme.onError,
        label = "statusTextColor",
        animationSpec = tween(durationMillis = 600)
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp), horizontalArrangement = Arrangement.Center
    ) {
        Surface(
            color = animatedBackgroundColor,//if (isPaid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimary,
            //shape = CardDefaults.shape,
            modifier = Modifier
                .weight(0.5f)
                .fillMaxSize()
                .clickable {
                    onUpdatePaymentStatus()
                }
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
                    Modifier.padding(bottom = dimensionResource(R.dimen.margin_normal)),
                    color = animatedTextColor
                )
                Text(
                    text = if (isPaid) stringResource(R.string.blocoEstaPago_status_pago) else stringResource(
                        R.string.blocoEstaPago_status_naoPago
                    ),
                    color = animatedTextColor
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
                text = personName,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = dimensionResource(R.dimen.margin_small))
            )
            Text(
                stringResource(R.string.simbolo_BRL) + if (isNoFrequencyPayment(frequency)) formatReadablePrice(
                    price
                ) else formatReadablePrice(
                    history.preco
                )
            )
        }
    }
}

@Composable
fun isNoFrequencyPayment(frequency: String): Boolean {
    return stringArrayResource(R.array.frequencias_pagamentos).last() == frequency
}

@Preview
@Composable
private fun PaymentHistoryCardPreview() {
    val history by remember {
        mutableStateOf(
            HistoricoDePagamento(
                id = 1L,
                data = "2024-11-24",
                preco = 10.0,
                pagadorId = 1L,
                pagamentoId = 1L,
                estaPago = false
            )
        )
    }
    var isPaid by remember { mutableStateOf(false) }
    val price = history.preco
    GerenciadorDePagamentosTheme {
        PaymentHistoryCard(
            history,
            isPaid,
            price,
            { isPaid = !isPaid },
            "Eu",
            "Diário"
        )
    }
}
