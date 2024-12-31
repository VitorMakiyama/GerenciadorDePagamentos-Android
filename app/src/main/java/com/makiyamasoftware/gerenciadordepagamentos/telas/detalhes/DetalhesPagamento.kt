package com.makiyamasoftware.gerenciadordepagamentos.telas.detalhes

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.makiyamasoftware.gerenciadordepagamentos.R
import com.makiyamasoftware.gerenciadordepagamentos.database.HistoricoDePagamento
import com.makiyamasoftware.gerenciadordepagamentos.database.Pagamento
import com.makiyamasoftware.gerenciadordepagamentos.formatReadablePrice
import com.makiyamasoftware.gerenciadordepagamentos.getFormattedStringDate
import com.makiyamasoftware.gerenciadordepagamentos.parseStringToDouble
import com.makiyamasoftware.gerenciadordepagamentos.ui.components.AlertDialogComponent
import com.makiyamasoftware.gerenciadordepagamentos.ui.components.NoFrequencyPriceChangeAlertDialog
import com.makiyamasoftware.gerenciadordepagamentos.ui.theme.GerenciadorDePagamentosTheme

private const val DEBUG_TAG = "Compose-DetalhesPagamento" 

@Composable
fun DetalhesPagamentoScreen(detalhesPagamentoViewModel: DetalhesPagamentoViewModel) {
	val pagamento by detalhesPagamentoViewModel.pagamento.observeAsState()
	val ultimoHistorico by detalhesPagamentoViewModel.historicoRecente.observeAsState()
	val estaPagoultimoHistorico by detalhesPagamentoViewModel.estaPagoHistoricoRecente.observeAsState()
	val precoUltimoHistorico by detalhesPagamentoViewModel.precoRecente.observeAsState()
	val precoNoFrequencyHistory by detalhesPagamentoViewModel.noFrequencyPrice.observeAsState()
	val nomePessoa by detalhesPagamentoViewModel.ultHistNomePessoa.observeAsState()

	var modifiablePayment by remember { mutableStateOf<Pagamento?>(null) }
	var modifiableHistory by remember { mutableStateOf<HistoricoDePagamento?>(null) }
	var modifiedHistories by remember { mutableStateOf<List<HistoricoDePagamento>>(emptyList()) }

	val isEditable by detalhesPagamentoViewModel.isEditable.observeAsState()

	val shouldShowAlert by detalhesPagamentoViewModel.shouldShowAlertDialog.observeAsState()
	val detalhesPagamentoAlertType by detalhesPagamentoViewModel.alertType.observeAsState()

	isEditable?.let { editable ->
		Scaffold(
			floatingActionButton = {
				if (editable) {
					FloatingActionButton(
						onClick = {
							pagamento?.let {
								if (modifiableHistory != null && !detalhesPagamentoViewModel.isLatestHistory()) {
									// Se o user tiver modificado o preco (modifiableHistory not null - só deveria acontecer quando o pagamento tiver alguma frequencia) e o historico não for o mais recente do pagamento
									detalhesPagamentoViewModel.onShowAlertDialog(DetalhesPagamentoViewModel.AlertType.MODIFY_OLD_HISTORY_PRICE)
								} else {
									detalhesPagamentoViewModel.onClickSalvarEdicoes(
										modifiablePayment,
										modifiableHistory,
										modifiedHistories
									)
								}
							}
						},
					) {
						Icon(
							Icons.Filled.Done,
							stringResource(R.string.detalhesPagamento_contentDescription_salvarAlteracoes)
						)
					}
				}
			},
			floatingActionButtonPosition = FabPosition.End,
		) { padding ->
			val onModifyPayment = { p: Pagamento?, h: HistoricoDePagamento? ->
				if (p != null) {
					modifiablePayment = p
				}
				if (h != null) { // Para pagamentos com frequencia
					modifiableHistory = h
				}
			}
			DetalhesPagamentoScreenContent(
				pagamento = pagamento,
				ultimoHistorico = ultimoHistorico,
				estaPago = estaPagoultimoHistorico!!,
				precoUltimoHistorico = precoUltimoHistorico,
				precoNoFrequencyHistory = precoNoFrequencyHistory,
				editable = editable,
				nomePessoa = nomePessoa,
				onModifyPayment = onModifyPayment,
				onClickNoFrequencyPrice = {
					detalhesPagamentoViewModel.onShowAlertDialog(
						DetalhesPagamentoViewModel.AlertType.MODIFY_NO_FREQUENCY_PRICE
					)
				},
				onChangeStatus = {
					detalhesPagamentoViewModel.onShowAlertDialog(
						DetalhesPagamentoViewModel.AlertType.CHANGE_STATUS
					)
				},
				onClickAllHistories = { detalhesPagamentoViewModel.onVerTodoOHistorico() },
				padding = padding,
				modifier = Modifier.padding(all = dimensionResource(R.dimen.margin_small))
			)
			if (shouldShowAlert == true) {
				when (detalhesPagamentoAlertType) {
					DetalhesPagamentoViewModel.AlertType.CHANGE_STATUS -> {
						AlertDialogComponent(
							title = stringResource(R.string.detalhesPagamentoFragment_status_alertTitle),
							message = stringResource(R.string.detalhesPagamentoFragment_status_alertMessage) +
									when (estaPagoultimoHistorico) {
										true -> stringResource(R.string.blocoEstaPago_status_naoPago)
										else -> stringResource(R.string.blocoEstaPago_status_pago)
									},
							onAffirmativeRequest = {
								detalhesPagamentoViewModel.onCickChangeStatus()
								detalhesPagamentoViewModel.onDoneShowAlertDialog()
							},
							onDismissRequest = { detalhesPagamentoViewModel.onDoneShowAlertDialog() },
						)
					}

					DetalhesPagamentoViewModel.AlertType.DELETE_PAYMENT -> {
						AlertDialogComponent(
							title = stringResource(R.string.detalhesPagamentoFragment_on_delete_payment_alertTitle),
							message = stringResource(R.string.detalhesPagamentoFragment_on_delete_payment_alertMessage),
							onAffirmativeRequest = {
								detalhesPagamentoViewModel.onClickDeletePayment()
								detalhesPagamentoViewModel.onDoneShowAlertDialog()
							},
							onDismissRequest = { detalhesPagamentoViewModel.onDoneShowAlertDialog() },
						)
					}

					DetalhesPagamentoViewModel.AlertType.UPDATE_PAYMENT -> {
						AlertDialogComponent(
							title = stringResource(R.string.detalhesPagamentoFragment_update_historicos_alertTitle),
							message = stringResource(R.string.detalhesPagamentoFragment_update_historicos_alertMessage),
							affirmativeText = pluralStringResource(R.plurals.generic_Update, 1),
							onAffirmativeRequest = {
								detalhesPagamentoViewModel.onUpdateHistoricosDoPagamento()
								detalhesPagamentoViewModel.onDoneShowAlertDialog()
							},
							onDismissRequest = { detalhesPagamentoViewModel.onDoneShowAlertDialog() },
						)
					}

					DetalhesPagamentoViewModel.AlertType.MODIFY_NO_FREQUENCY_PRICE -> {
						NoFrequencyPriceChangeAlertDialog(
							histories = detalhesPagamentoViewModel.historicosDoPagamento.value!!,
							people = detalhesPagamentoViewModel.pessoas.value!!,
							onAffirmativeRequest = { histories ->
								modifiedHistories = histories
								detalhesPagamentoViewModel.atualizarPreco(modifiedHistories)
								detalhesPagamentoViewModel.onDoneShowAlertDialog()
							},
							onDismissRequest = { detalhesPagamentoViewModel.onDoneShowAlertDialog() },
						)
					}

					DetalhesPagamentoViewModel.AlertType.MODIFY_OLD_HISTORY_PRICE -> {
						AlertDialogComponent(
							title = stringResource(R.string.detalhesPagamentoFragment_on_change_price_withFrequency_oldHistory_alertTitle),
							message = stringResource(R.string.detalhesPagamentoFragment_on_change_price_withFrequency_oldHistory_alertMessage, getFormattedStringDate(ultimoHistorico!!.data)),
							affirmativeText = pluralStringResource(R.plurals.generic_Update, detalhesPagamentoViewModel.historicosDoPagamento.value!!.size),
							onAffirmativeRequest = {
								detalhesPagamentoViewModel.onClickSalvarEdicoes(
									modifiablePayment,
									modifiableHistory,
									modifiedHistories,
									updateLaterHistories = true
								)
								detalhesPagamentoViewModel.onDoneShowAlertDialog()
							},
							dismissText = stringResource(R.string.generic_Cancelar),
							onDismissRequest = { detalhesPagamentoViewModel.onDoneShowAlertDialog() },
							thirdActionText = stringResource(R.string.generic_button_OnlyThis),
							onThirdRequest = {
								detalhesPagamentoViewModel.onClickSalvarEdicoes(
									modifiablePayment,
									modifiableHistory,
									modifiedHistories
								)
								detalhesPagamentoViewModel.onDoneShowAlertDialog()
							}
						)
					}

					else -> {
						detalhesPagamentoViewModel.onDoneShowAlertDialog()
					}
				}
			}
		}
	}
}

@Composable
fun DetalhesPagamentoScreenContent(
	pagamento: Pagamento?,
	ultimoHistorico: HistoricoDePagamento?,
	estaPago: Boolean,
	precoUltimoHistorico: Double?,
	precoNoFrequencyHistory: Double?,
	editable: Boolean,
	nomePessoa: String?,
	onModifyPayment: (Pagamento?, HistoricoDePagamento?) -> Unit,
	onClickNoFrequencyPrice: () -> Unit,
	onChangeStatus: () -> Unit,
	onClickAllHistories: () -> Unit,
	padding: PaddingValues,
	modifier: Modifier = Modifier,
) {
	Column(
		horizontalAlignment = Alignment.CenterHorizontally,
		modifier = Modifier
			.verticalScroll(rememberScrollState()) // Allows scroll, recommended for few items! For more items use LazyColumn
			.padding(padding)
	) {
		pagamento?.let { p ->
			ultimoHistorico?.let { h ->
				precoUltimoHistorico?.let {
					precoNoFrequencyHistory?.let {
						EditablePaymentFields(
							payment = p,
							history = h,
							price = if (isNoFrequencyPayment(frequency = pagamento.freqDoPag)) precoNoFrequencyHistory else precoUltimoHistorico,
							editable = editable,
							onModifyPayment = onModifyPayment,
							onClickNoFrequencyPrice = onClickNoFrequencyPrice,
							modifier = modifier
								.fillMaxWidth(),
						)
					}
					nomePessoa?.let {
						PaymentHistoryCard(
							history = h,
							isPaid = estaPago,
							price = precoUltimoHistorico,
							onUpdatePaymentStatus = onChangeStatus,
							frequency = p.freqDoPag,
							personName = it,
							modifier = modifier
						)
					}
				}
			}
			Button(
				onClick = onClickAllHistories,
				modifier = Modifier.padding(vertical = dimensionResource(R.dimen.margin_small)),
			) {
				Text(stringResource(R.string.detalhesPagamentoFragment_ver_todo_o_historicos))
			}
		}
	}
}

@Composable
fun EditablePaymentFields(
	payment: Pagamento,
	history: HistoricoDePagamento,
	price: Double,
	editable: Boolean,
	onModifyPayment: (Pagamento?, HistoricoDePagamento?) -> Unit,
	onClickNoFrequencyPrice: () -> Unit,
	modifier: Modifier = Modifier
) {
	var paymentName by remember { mutableStateOf(payment.nome) }
	var editablePrice by remember { mutableDoubleStateOf(price) }
	var autoUpdatePayment by remember { mutableStateOf(payment.autoUpdateHistorico) }
	var shouldSendPush by remember { mutableStateOf(payment.podeEnviarPush) }

	Column(modifier = modifier) {
		OutlinedTextField(
			value = paymentName,
			label = { Text(stringResource(R.string.detalhesPagamentoFragment_descricao_nome)) },
			onValueChange = {
				paymentName = it
				payment.nome = it
				onModifyPayment(payment, null)
			},
			readOnly = !editable,
			modifier = modifier,
		)
		OutlinedTextField(
			value = getFormattedStringDate(payment.dataDeInicio),
			label = { Text(stringResource(R.string.detalhesPagamentoFragment_descricao_data)) },
			onValueChange = { },
			readOnly = true,
			modifier = modifier
		)
		OutlinedTextField(
			value = payment.freqDoPag,
			label = { Text(stringResource(R.string.detalhesPagamentoFragment_descricao_frequencia)) },
			onValueChange = { },
			readOnly = true,
			modifier = modifier,
		)
		OutlinedTextField(
			value = if (isNoFrequencyPayment(payment.freqDoPag)) formatReadablePrice(price) else formatReadablePrice(editablePrice),
			label = { Text(stringResource(R.string.detalhesPagamentoFragment_descricao_preco)) },
			onValueChange = {
				parseStringToDouble(it)?.let { convertedPrice ->
					editablePrice = convertedPrice
					history.preco = convertedPrice
					onModifyPayment(null, history)
				}
			},
			enabled = !isNoFrequencyPayment(payment.freqDoPag), // disabled quando for pagamento sem frequencia
			readOnly = isNoFrequencyPayment(payment.freqDoPag) || !editable, // se for pagamento sem frequencia OU não for editavel
			trailingIcon = {
				if (isNoFrequencyPayment(payment.freqDoPag) && editable) Icon(
					Icons.Filled.Edit,
					contentDescription = stringResource(R.string.menu_detalhes_fragment_editar)
				)
			},
			keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
			modifier = if (isNoFrequencyPayment(payment.freqDoPag) && editable) modifier.clickable {
				onClickNoFrequencyPrice()
			} else modifier,
		)
		LabeledSwitchField(
			title = stringResource(R.string.detalhesPagamento_descricao_manter_atualizado),
			checked = autoUpdatePayment,
			onCheckChange = { checked ->
				autoUpdatePayment = checked
				payment.autoUpdateHistorico = checked
				onModifyPayment(payment, null)
			},
			editable = editable,
			modifier = modifier,
		)
		LabeledSwitchField(
			title = stringResource(R.string.detalhesPagamento_descricao_pode_enviar_push),
			checked = shouldSendPush,
			onCheckChange = { checked ->
				shouldSendPush = checked
				payment.podeEnviarPush = checked
				onModifyPayment(payment, null)
			},
			editable = editable,
			modifier = modifier,
		)
	}
}

@Composable
fun LabeledSwitchField(
	title: String,
	checked: Boolean,
	onCheckChange: (Boolean) -> Unit,
	editable: Boolean,
	modifier: Modifier = Modifier
) {
	Row(
		modifier,
		verticalAlignment = Alignment.CenterVertically
	) {
		OutlinedTextField(
			value = title,
			onValueChange = { },
			readOnly = true,
		)
		Switch(
			checked = checked,
			onCheckedChange = { onCheckChange(it) },
			modifier = modifier,
			thumbContent = {},
			enabled = editable,
		)
	}
}

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
	val animatedColor by animateColorAsState(
		if (isPaid) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
		label = "statusColor",
		animationSpec = tween(
			durationMillis = 600
		)
	)
	Row(
		modifier = Modifier
			.fillMaxWidth()
			.height(100.dp), horizontalArrangement = Arrangement.Center
	) {
		Surface(
			color = animatedColor,//if (isPaid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimary,
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
					Modifier.padding(bottom = dimensionResource(R.dimen.margin_normal))
				)
				Text(
					text = if (isPaid) stringResource(R.string.blocoEstaPago_status_pago) else stringResource(
						R.string.blocoEstaPago_status_naoPago
					)
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
private fun isNoFrequencyPayment(frequency: String): Boolean {
	return stringArrayResource(R.array.frequencias_pagamentos).last() == frequency
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun EditablePaymentFieldsPreview() {
	val pag by remember {
		mutableStateOf(
			Pagamento(
				pagamentoID = 1L,
				nome = "Spotify",
				dataDeInicio = "2024-12-01",
				numPessoas = 1,
				freqDoPag = "DAILY",
				autoUpdateHistorico = true,
				podeEnviarPush = true
			)
		)
	}
	val history = HistoricoDePagamento(
		historicoID = 1L,
		data = "2024-11-24",
		preco = 10.0,
		pagadorID = 1L,
		pagamentoID = 1L,
		estaPago = false
	)
	var editable by remember { mutableStateOf(false) }
	var price by remember { mutableDoubleStateOf(history.preco) }
	GerenciadorDePagamentosTheme {
		Column {
			Button(onClick = { editable = !editable }) { Text("Toggle Edit") }
			EditablePaymentFields(
				pag,
				history,
				price = price,
				editable,
				onModifyPayment = { p, h ->

				},
				onClickNoFrequencyPrice = {},
				Modifier
					.fillMaxWidth()
					.padding(vertical = dimensionResource(R.dimen.margin_small))
			)
		}
	}
}

/**
 * Compose PREVIEWS
 */

@Preview
@Composable
private fun PaymentHistoryCardPreview() {
	val history by remember {
		mutableStateOf(
			HistoricoDePagamento(
				historicoID = 1L,
				data = "2024-11-24",
				preco = 10.0,
				pagadorID = 1L,
				pagamentoID = 1L,
				estaPago = false
			)
		)
	}
	var isPaid by remember { mutableStateOf(false) }
	var price = history.preco
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

/* TODO: use in CriarNovoPagamento
		    ExposedDropdownMenuBox(
			expanded = isExpanded,
			onExpandedChange = { isExpanded = it },
			modifier = modifier,
		) {
			TextField(
				value = payment.freqDoPag,
				label = { Text(stringResource(R.string.detalhesPagamentoFragment_descricao_frequencia)) },
				onValueChange = {},
				readOnly = true,
				trailingIcon = {
					if (editable) ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded)
				},
				modifier = modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, editable)
			)
			ExposedDropdownMenu(
				expanded = isExpanded,
				onDismissRequest = {isExpanded = false}
			) {
				DropdownMenuItem(
					text = { Text("teste") },
					onClick = {
						//payment.freqDoPag = "teste"
						isExpanded = false
					}
				)
			}
		} */
