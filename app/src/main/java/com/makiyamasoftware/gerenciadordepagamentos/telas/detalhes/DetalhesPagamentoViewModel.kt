package com.makiyamasoftware.gerenciadordepagamentos.telas.detalhes

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.makiyamasoftware.gerenciadordepagamentos.R
import com.makiyamasoftware.gerenciadordepagamentos.atualizarNovosHistoricosDePagamento
import com.makiyamasoftware.gerenciadordepagamentos.database.HistoricoDePagamento
import com.makiyamasoftware.gerenciadordepagamentos.database.Pagamento
import com.makiyamasoftware.gerenciadordepagamentos.database.PagamentosDatabaseDao
import com.makiyamasoftware.gerenciadordepagamentos.database.Pessoa
import com.makiyamasoftware.gerenciadordepagamentos.getPessoaCerta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

private const val TAG = "DetalhesPagamentoViewModel"

class DetalhesPagamentoViewModel(
	private val dataSource: PagamentosDatabaseDao,
	private val app: Application,
	private val pagamentoSelecionado: Pagamento
) : AndroidViewModel(app) {
	private val viewModelJob = Job()
	override fun onCleared() {
		super.onCleared()
		viewModelJob.cancel()
	}

	lateinit var historicosDoPagamento: LiveData<List<HistoricoDePagamento>>
	private val _historicoRecente = MutableLiveData<HistoricoDePagamento>(null)
	val historicoRecente: LiveData<HistoricoDePagamento>
		get() = _historicoRecente
	private val _estaPagoHistoricoRecente = MutableLiveData<Boolean>(false)
	val estaPagoHistoricoRecente: LiveData<Boolean>
		get() = _estaPagoHistoricoRecente

	lateinit var pagamento: LiveData<Pagamento>
	lateinit var pessoas: LiveData<List<Pessoa>>

	init {
		viewModelScope.launch {
			pagamento = dataSource.getPagamento(pagamentoSelecionado.id)
			Log.i(TAG, "Entrou na coroutine de buscar o historico")
			historicosDoPagamento =
				dataSource.getHistoricosDePagamento(pagamentoSelecionado.id)
			pessoas = dataSource.getPessoasDoPagamento(pagamentoSelecionado.id)
			Log.i(TAG, "Requisitou o historico e as pessoas do DB")
		}
	}

	// Preco atual do pagamento (do historicoRecente) (pode ser atualizado pelo usuario, o usuario tambem pode atualizar os próximos historicos)
	private val _precoRecente = MutableLiveData<Double>()
	val precoRecente: LiveData<Double>
		get() = _precoRecente

	// Preco de um pagamento sem frequencia (soma de todos os historicos de pagamento)
	private val _noFrequencyPrice = MutableLiveData<Double>(0.0)
	val noFrequencyPrice: LiveData<Double>
		get() = _noFrequencyPrice

	// A pessoa do historicoRecente
	private val _ultHistNomePessoa = MutableLiveData<String>()
	val ultHistNomePessoa: LiveData<String>
		get() = _ultHistNomePessoa


	// Comeca do historico mais antigo (ultimo do vetor) buscando o historico NAO PAGO mais recente
	private fun getHistoricoRecenteNaoPago(): HistoricoDePagamento? {
		historicosDoPagamento.value?.let { value ->
			for (i in (value.size - 1) downTo 1) {
				val historico = value[i]
				if (!(historico.estaPago)) return historico.copy()
			}
			return value.first().copy()
		}
		return null
	}

	/**
	 * Atualiza os LiveData dos precos (noFrequencyPrice e precoRecente).
	 * Recebe como parametro os historicos modificados, caso seja um pagamento sem frequencia
	 */
	fun atualizarPreco(historicosModificados: List<HistoricoDePagamento?> = emptyList()) {
		if (pagamentoSelecionado.frequencia == app.resources.getStringArray(R.array.frequencias_pagamentos).last()
		) {
			// Caso seja NO FREQUENCY
			var p: Double = 0.0
			for (hist in historicosDoPagamento.value!!) {
				if (historicosModificados.contains(hist)) {
					// Caso precisemos atualizar o preco de historicosModificados para mostra-lo na UI, mas o usuario ainda não salvou as alterações
					val modifiedH =
						historicosModificados.find { h -> h?.id == hist.id }
					p += modifiedH!!.preco
				} else {
					p += hist.preco
				}
			}
			_noFrequencyPrice.value = p
		}
		_precoRecente.value = historicoRecente.value?.preco
		Log.i(
			TAG,
			"Click Data: paggId:${pagamentoSelecionado.id} e o historico e \n${historicosDoPagamento.value!!.size}"
		)
	}

	// Essa funcao atualiza as váriaveis Livedata que estão ligadas ao layout xml através de DataBinding,
	//  é necessario verificar se ambos os LiveData<List> de Pessoa e HistoricoDePagamento já foram retornados pelo DB DAO.
	//  Assim essa funcao tbm deve ser chamada por ambos os observers, assim o ultimo a receber os dados os atualiza.
	fun atualizarHistoricoRecente() {
		if (pessoas.value != null && historicosDoPagamento.value != null) {
			_historicoRecente.value = getHistoricoRecenteNaoPago()!!
			_estaPagoHistoricoRecente.value = _historicoRecente.value?.estaPago
			_ultHistNomePessoa.value =
				getPessoaCerta(pessoas.value!!, historicoRecente.value!!.pagadorId).nome
		}
	}

	fun onCickChangeStatus() {
		_historicoRecente.value?.toogleStatus()
		viewModelScope.launch {
			saveNovoHistoricoOnDB(_historicoRecente.value!!)
		}
	}

	private suspend fun saveNovoHistoricoOnDB(historico: HistoricoDePagamento) {
		withContext(Dispatchers.IO) {
			dataSource.updateHistoricoDePagamento(historico)
			Log.i(TAG, "Terminou de salvar o status do historico")
		}
	}

	private var _verTodoOHistorico = MutableLiveData<Boolean>()
	val verTodoOHistorico: LiveData<Boolean>
		get() = _verTodoOHistorico

	//     Funcoes auxiliares para monitorar a LiveData do botao de ver todos os Historicos
	fun onVerTodoOHistorico() {
		_verTodoOHistorico.value = true
	}

	fun onVerTodoOHistoricoDone() {
		_verTodoOHistorico.value = false
	}

	//
	fun getHistoricoMaisRecente(): HistoricoDePagamento {
		return historicosDoPagamento.value!!.first()
	}

	/**
	 *  Funcao chamada quando, ao iniciar esse fragment, e' detectada a necessidade de
	 *      atualizar os Historicos do Pagamento, pois o ultimo esta com uma data menor que
	 *      hoje - periodo
	 */
	fun onUpdateHistoricosDoPagamento() {
		val novosHistoricos = atualizarNovosHistoricosDePagamento(
			getHistoricoRecenteNaoPago()!!,
			Calendar.getInstance(),
			pagamentoSelecionado,
			app.resources.getStringArray(R.array.frequencias_pagamentos),
			pessoas.value!!
		)
		Log.i(TAG, "$novosHistoricos")

		// Parte de interação com o DataBase
		viewModelScope.launch {
			// Salva os novos historicos no DB
			salvarAtualizacoesHistorico(novosHistoricos)
			// Chama a função de puxar os historicos do DB, para atualizar os dados e a UI
		}
	}

	// Funcao para salvar os Historicos atualizados no DataBase, no background sem interromper a Main thread (UI)
	private suspend fun salvarAtualizacoesHistorico(novosHistoricos: List<HistoricoDePagamento>) {
		withContext(Dispatchers.IO) {
			for (historico in novosHistoricos) dataSource.inserirHistoricoDePagamento(historico)
		}
	}

	// Logica de evento para começar a editar
	private val _isEditable = MutableLiveData<Boolean>(false)
	val isEditable: LiveData<Boolean>
		get() = _isEditable


	// Funcao chamada ao clicar no salvarFAB
	fun onClickSalvarEdicoes(
		pagamentoModificado: Pagamento?,
		historico: HistoricoDePagamento?,
		historicos: List<HistoricoDePagamento?>,
		updateLaterHistories: Boolean = false
	) {
		_isEditable.value = false
		viewModelScope.launch {
			if (updateLaterHistories) {
				val histories = historicosDoPagamento.value
				histories?.let {
					val latestHistoryIndex = histories.indexOf(histories.find { h -> h.id == historico?.id })
					if (latestHistoryIndex != -1) {
						// se o index foi encontrado
						for (i in latestHistoryIndex - 1 downTo 0) {
							// Iterar do index do historicoRecente - 1 ao 0 para atualizar os historicos
							salvarEdicoesPagamentoNoDB(
								pagamento = null,
								historico = it[i].copy(preco = historico!!.preco),
								historicos = emptyList()
							)
						}
					}
				}
			}
			salvarEdicoesPagamentoNoDB(pagamentoModificado, historico, historicos)
			atualizarPreco()
		}
	}

	// Funcao que salva as edicoes no Pagamento no DB
	private suspend fun salvarEdicoesPagamentoNoDB(
		pagamento: Pagamento?,
		historico: HistoricoDePagamento?,
		historicos: List<HistoricoDePagamento?>
	) {
		Log.d(TAG, "salvarEdicoesPagamentoNoDB: pag=$pagamento e hist=$historico")
		withContext(Dispatchers.IO) {
			if (pagamento != null) {
				dataSource.updatePagamento(pagamento)
			}
			if (isNoFrequencyPayment(pagamentoSelecionado)) {
				for (history in historicos) {
					history?.let {
						dataSource.updateHistoricoDePagamento(it)
					}
				}
			} else {
				if (historico != null) {
					dataSource.updateHistoricoDePagamento(historico)
				}
			}
		}
	}

	fun onEditingPayment() {
		_isEditable.value = true
	}

	// Logic for Compose AlertDialog
	enum class AlertType {
		CHANGE_STATUS, DELETE_PAYMENT, UPDATE_PAYMENT, MODIFY_NO_FREQUENCY_PRICE, MODIFY_OLD_HISTORY_PRICE
	}

	private val _shouldShowAlertDialog = MutableLiveData<Boolean>(false)
	val shouldShowAlertDialog: LiveData<Boolean>
		get() = _shouldShowAlertDialog

	private val _alertType = MutableLiveData<AlertType>(AlertType.CHANGE_STATUS)
	val alertType: LiveData<AlertType>
		get() = _alertType

	fun onShowAlertDialog(alertType: AlertType) {
		_alertType.value = alertType
		_shouldShowAlertDialog.value = true
	}

	fun onDoneShowAlertDialog() {
		_shouldShowAlertDialog.value = false
	}


	private val _hasDeletedPayment = MutableLiveData(false)
	val hasDeletedPayment: LiveData<Boolean>
		get() = _hasDeletedPayment
	/**
	 * Calls functions, on Dispatchers.IO, for deleting all information regarding this payment from the database
	 */
	fun onClickDeletePayment() {
		viewModelScope.launch {
			onDeleteThisPayment()
			_hasDeletedPayment.value = true
		}
	}
	/**
	 * Deletes all information of this payment from the database
	 */
	private suspend fun onDeleteThisPayment() {
		withContext(Dispatchers.IO) {
			dataSource.deletePessoasFromPagamento(pagamentoSelecionado.id)
			dataSource.deleteHistoricoDePagamentosFromPagamento(pagamentoSelecionado.id)
			dataSource.deletePagamento(pagamentoSelecionado.id)
		}
	}

	private fun isNoFrequencyPayment(payment: Pagamento): Boolean {
		return payment.frequencia == app.resources.getStringArray(R.array.frequencias_pagamentos)
			.last()
	}

	fun isLatestHistory(): Boolean {
		return historicoRecente.value?.id == historicosDoPagamento.value?.first()?.id
	}
}
