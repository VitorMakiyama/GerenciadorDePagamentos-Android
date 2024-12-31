package com.makiyamasoftware.gerenciadordepagamentos.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.makiyamasoftware.gerenciadordepagamentos.R
import com.makiyamasoftware.gerenciadordepagamentos.database.HistoricoDePagamento
import com.makiyamasoftware.gerenciadordepagamentos.database.Pessoa
import com.makiyamasoftware.gerenciadordepagamentos.formatReadablePrice
import com.makiyamasoftware.gerenciadordepagamentos.getPessoaCerta
import com.makiyamasoftware.gerenciadordepagamentos.parseStringToDouble
import com.makiyamasoftware.gerenciadordepagamentos.ui.theme.GerenciadorDePagamentosTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertDialogComponent(
	title: String = "",
	message: String,
	affirmativeText: String = stringResource(R.string.generic_Sim),
	onAffirmativeRequest: () -> Unit,
	dismissText: String = stringResource(R.string.generic_Nao),
	thirdActionText: String = "",
	onThirdRequest: (() -> Unit)? = null,
	onDismissRequest: () -> Unit,
	modifier: Modifier = Modifier
) {
	val dialogModifier = Modifier.padding(dimensionResource(R.dimen.margin_normal))
	BasicAlertDialog(
		onDismissRequest = onDismissRequest,
	) {
		Card(
			modifier = Modifier.widthIn(min = 280.dp, max = 560.dp)
		) {
			Column(
				verticalArrangement = Arrangement.Center,
				//horizontalAlignment = Alignment.CenterHorizontally,
				modifier = dialogModifier
			) {
				if (title.isNotEmpty()) Text(
					title,
					modifier = Modifier.padding(bottom = dimensionResource(R.dimen.margin_normal)),
					style = MaterialTheme.typography.headlineMedium
				)
				Text(text = message)
				Row {
					if (onThirdRequest != null) {
						TextButton(
							onClick = onThirdRequest,
						) {
							Text(thirdActionText)
						}
					}
					Row(
						horizontalArrangement = Arrangement.End,
						modifier = Modifier.fillMaxWidth()
					) {
						// Third Option
						// Dismiss Button
						TextButton(
							onClick = onDismissRequest,
							modifier = modifier.padding(horizontal = dimensionResource(R.dimen.margin_normal))
						) {
							Text(dismissText)
						}
						// Affirmative Button
						TextButton(
							onClick = onAffirmativeRequest,
						) {
							Text(affirmativeText)
						}
					}
				}
			}
		}
	}
}

/**
 * Alert Dialog específico para alterar o preco de um `Pagamento` sem frequencia
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoFrequencyPriceChangeAlertDialog(
	title: String = stringResource(R.string.detalhesPagamentoFragment_on_change_price_noFrequency_alertTitle),
	histories: List<HistoricoDePagamento>,
	people: List<Pessoa>,
	affirmativeText: String = stringResource(R.string.generic_Pronto),
	onAffirmativeRequest: (List<HistoricoDePagamento>) -> Unit,
	dismissText: String = stringResource(R.string.generic_Cancelar),
	onDismissRequest: () -> Unit,
	modifier: Modifier = Modifier
) {
	val dialogModifier = Modifier.padding(dimensionResource(R.dimen.margin_normal))
	val modifiedHistories = mutableMapOf<Long, HistoricoDePagamento>()

	BasicAlertDialog(
		onDismissRequest = onDismissRequest,
	) {
		Card(
			modifier = Modifier.widthIn(min = 280.dp, max = 560.dp)
		) {
			Column(
				verticalArrangement = Arrangement.Center,
				//horizontalAlignment = Alignment.CenterHorizontally,
				modifier = dialogModifier
			) {
				if (title.isNotEmpty()) Text(
					title,
					modifier = Modifier.padding(bottom = dimensionResource(R.dimen.margin_normal)),
					style = MaterialTheme.typography.headlineMedium
				)
				// Descricao explicativa
				Text(stringResource(R.string.detalhesPagamentoFragment_on_change_price_noFrequency_alertMessage))
				// Os preços do pagamento SEM FREQUENCIA a serem modificados
				LazyColumn(modifier = Modifier.padding(vertical = dimensionResource(R.dimen.margin_small))) {
					items(
						items = histories
					) { history ->
						var price by rememberSaveable { mutableDoubleStateOf(history.preco) }
						OutlinedTextField(
							value = formatReadablePrice(price),
							label = { Text(getPessoaCerta(people, history.pagadorID).nome) },
							onValueChange = {
								parseStringToDouble(it)?.let { convertedPrice ->
									price = convertedPrice
									history.preco = convertedPrice
									modifiedHistories[history.historicoID] = history
								}
							},
							modifier = modifier.padding(vertical = dimensionResource(R.dimen.margin_small)),
							keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
						)
					}
				}

				Row(
					horizontalArrangement = Arrangement.End,
					modifier = Modifier.fillMaxWidth()
				) {
					// Dismiss Button
					TextButton(
						onClick = onDismissRequest,
						modifier = modifier.padding(horizontal = dimensionResource(R.dimen.margin_normal))
					) {
						Text(dismissText)
					}
					// Affirmative Button
					TextButton(
						// Envia apenas os historicos modificadosa para serem salvos no DB
						onClick = { onAffirmativeRequest(modifiedHistories.values.toList()) },
					) {
						Text(affirmativeText)
					}
				}

			}
		}
	}
}

@Preview
@Composable
private fun AlertDialogComponentPreview() {
	GerenciadorDePagamentosTheme {
		AlertDialogComponent(
			title = "Titulo",
			message = stringResource(R.string.detalhesPagamentoFragment_status_alertMessage),
			affirmativeText = pluralStringResource(R.plurals.generic_Update, 11),
			onAffirmativeRequest = {},
			dismissText = stringResource(R.string.generic_Cancelar),
			onDismissRequest = {},
			thirdActionText = stringResource(R.string.generic_button_OnlyThis),
			onThirdRequest = {}
		)
	}
}

@Preview
@Composable
private fun NoFrequencyAlertDialogPreview() {
	val histories = listOf(
		HistoricoDePagamento(
			data = "2024-12-28",
			preco = 10.9,
			pagadorID = 1,
			pagamentoID = 1
		), HistoricoDePagamento(
			data = "2024-12-29",
			preco = 59.9,
			pagadorID = 2,
			pagamentoID = 1
		)
	)
	val people = listOf(
		Pessoa(
			pessoaID = 1,
			nome = "Pessoa 1",
			ordem = 1,
			pagamentoID = 1
		), Pessoa(
			pessoaID = 2,
			nome = "Pessoa 2",
			ordem = 2,
			pagamentoID = 1
		)
	)
	GerenciadorDePagamentosTheme {
		NoFrequencyPriceChangeAlertDialog(
			title = "Titulo",
			histories = histories,
			people = people,
			affirmativeText = "Pronto",
			onAffirmativeRequest = {},
			dismissText = "Cancelar",
			onDismissRequest = {},
		)
	}
}
