package com.makiyamasoftware.gerenciadordepagamentos.telas.detalhes

import android.content.res.Resources
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

private const val TAG = "DetalhesPagamentoViewModel"

data class PaymentDetailsUIState(
    val currentPayment: Pagamento,
    val latestPaymentHistory: HistoricoDePagamento,
    val latestPerson: Pessoa,
    val isEditMode: Boolean = false,
    val showDialog: Boolean = false
)

// Using the Custom Provider/Factory, more info here: https://medium.com/@chetanshingare2991/passing-parameters-to-viewmodel-in-jetpack-compose-the-right-way-with-hilt-custom-factory-d0ad52e9d7de
class DetalhesPagamentoViewModel(
//	private val savedStateHandle: SavedStateHandle,
    private val dataSource: PagamentosDatabaseDao,
//	private val app: Application,
    private val pagamentoSelecionado: Pagamento,
    private val latestPaymentHistory: HistoricoDePagamento,
    private val latestPerson: Pessoa
) : ViewModel() {
    private val viewModelJob = Job()
    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }

    // DetalhesPagamento UI State
    private val _uiState = MutableStateFlow(
        PaymentDetailsUIState(
            pagamentoSelecionado,
            latestPaymentHistory,
            latestPerson
        )
    )
    val uiState: StateFlow<PaymentDetailsUIState> = _uiState.asStateFlow()

    // Váriaveis que buscam, numa thread secundaria, os historicos e pessoas do DB
    private val _paymentHistories = MutableStateFlow<List<HistoricoDePagamento>>(emptyList())
    val paymentHistories: StateFlow<List<HistoricoDePagamento>> = _paymentHistories
    private val _paymentPeople = MutableStateFlow<List<Pessoa>>(emptyList())
    val paymentPeople: StateFlow<List<Pessoa>> = _paymentPeople

    init {
        viewModelScope.launch(Dispatchers.IO) {
            _paymentHistories.update {
                dataSource.getHistoricosDePagamento(pagamentoSelecionado.id)
            }
            _paymentPeople.update {
                dataSource.getPessoasDoPagamento(pagamentoSelecionado.id)
            }
            Log.i(TAG, "Requisitou os historicos e as pessoas do DB")
        }.invokeOnCompletion {
//            updatePaymentsDetailsState(pagamentoSelecionado, getHistoricoRecenteNaoPago())
        }
    }

    fun updatePaymentsDetailsState(
        latestPayment: Pagamento,
        latestPaymentHistory: HistoricoDePagamento
    ) {
        // Updates PaymentDetailsUIState with new data
        _uiState.update { currentState ->
            currentState.copy(
                currentPayment = latestPayment,
                latestPaymentHistory = latestPaymentHistory
            )
        }
    }

    fun toggleDialog() {
        _uiState.update { currentState ->
            currentState.copy(
                showDialog = !currentState.showDialog
            )
        }
    }

    fun toggleEditable() {
        _uiState.update { currentState ->
            currentState.copy(
                isEditMode = !currentState.isEditMode
            )
        }
    }

    // Comeca do historico mais antigo (ultimo do vetor) buscando o historico NAO PAGO mais recente
    private fun getHistoricoRecenteNaoPago(): HistoricoDePagamento {
        paymentHistories.value.let { value ->
            for (i in (value.size - 1) downTo 1) {
                val historico = value[i]
                if (!(historico.estaPago)) return historico.copy()
            }
            return value.first().copy()
        }
    }

    /**
     * Retorna o preco a ser mostrado na UI (noFrequencyPrice e precoRecente).
     * Recebe como parametro os historicos modificados, caso seja um pagamento sem frequencia
     */
    fun atualizarPreco(historicosModificados: List<HistoricoDePagamento> = emptyList()): Double {
        if (pagamentoSelecionado.frequencia == Resources.getSystem()
                .getStringArray(R.array.frequencias_pagamentos).last()
        ) {
            // Caso seja NO FREQUENCY
            var p: Double = 0.0
            for (hist in paymentHistories.value) {
                if (historicosModificados.contains(hist)) {
                    // Caso precisemos atualizar o preco de historicosModificados para mostra-lo na UI, mas o usuario ainda não salvou as alterações
                    val modifiedH =
                        historicosModificados.find { h -> h.id == hist.id }
                    p += modifiedH!!.preco
                } else {
                    p += hist.preco
                }
            }
            return p
        } else {
            Log.i(
                TAG,
                "Click Data: paggId:${pagamentoSelecionado.id} e o historico e \n${paymentHistories.value.size}"
            )
            return latestPaymentHistory.preco
        }
    }

    /**
     * Returns the price for the history, to be shown on UI.
     * There are 2 possibilities: frequency payment - shows the latest history value; or no frequency payment - sums the prices of all histories and returns them
     **/
    fun getPriceToShow(
        historicosModificados: List<HistoricoDePagamento?> = emptyList(),
        isNoFrequency: Boolean
    ): Double {
        if (isNoFrequency) {
            // Caso seja NO FREQUENCY
            var p: Double = 0.0
            for (hist in paymentHistories.value) {
                if (historicosModificados.contains(hist)) {
                    // Caso precisemos atualizar o preco de historicosModificados para mostra-lo na UI, mas o usuario ainda não salvou as alterações
                    val modifiedH =
                        historicosModificados.find { h -> h?.id == hist.id }
                    p += modifiedH!!.preco
                } else {
                    p += hist.preco
                }
            }
            return p
        } else {
            Log.i(
                TAG,
                "Click Data: paggId:${pagamentoSelecionado.id} e o historico e \n${paymentHistories.value.size}"
            )
            return latestPaymentHistory.preco
        }
    }

    /** Essa funcao atualiza as váriaveis da UIState, que estao sendo observadas como estados no layout Compose..
     * Assim essa funcao tbm deve ser chamada por ambos os observers, assim o ultimo a receber os dados os atualiza.
     **/
    fun updateLatestHistory() {
        if (paymentPeople.value.isNotEmpty() && paymentHistories.value.isNotEmpty()) {
            val newLatestHistory = getHistoricoRecenteNaoPago()
            val newLatestPerson = getPessoaCerta(paymentPeople.value, newLatestHistory.id)
            _uiState.update { currentState ->
                currentState.copy(
                    latestPaymentHistory = newLatestHistory,
                    latestPerson = newLatestPerson
                )
            }
        }
    }

    fun onClickChangeStatus() {
        _uiState.update { currentState ->
            val newLatestHistory = currentState.latestPaymentHistory.copy()
            newLatestHistory.toogleStatus()
            keepHistoriesUpToDate(newLatestHistory)
            currentState.copy(
                latestPaymentHistory = newLatestHistory
            )
        }
        viewModelScope.launch {
            saveNovoHistoricoOnDB(uiState.value.latestPaymentHistory)
        }
        updateLatestHistory()
    }

    private fun keepHistoriesUpToDate(updatedHistory: HistoricoDePagamento) {
        paymentHistories.value.indexOfFirst { h -> h.id == updatedHistory.id }.let { index ->
            Log.d(TAG, "keepHistoriesUpToDate: $index")
            if (index != -1) {
                _paymentHistories.update {
                    // preciso atualizar o historico na lista mutavel
                    it.toMutableList().apply {
                        this[index] = updatedHistory
                    }
                }
            }
        }
    }

    private suspend fun saveNovoHistoricoOnDB(historico: HistoricoDePagamento) {
        withContext(Dispatchers.IO) {
            dataSource.updateHistoricoDePagamento(historico)
            Log.i(TAG, "Terminou de salvar o status do historico")
        }
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
            Resources.getSystem().getStringArray(R.array.frequencias_pagamentos),
            paymentPeople.value!!
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
        pagamentoModificado: Pagamento,
        modifiedHistory: HistoricoDePagamento,
        updateLaterHistories: Boolean = false,
        isNoFrequencyPayment: Boolean
    ) {
        toggleEditable()
        viewModelScope.launch {
            if (isNoFrequencyPayment) {
                // nesse cenario de NoFrequencyPayment, nao preenchemos o modifiedHistory, pois todos os historicos podem ter sido alterados,
                // assim o ideal é atualizar todos os pagamentos do State (que se mantiveram atualizados gracas 'a funcao updateNoFrequencyPrices)
                paymentHistories.value.forEach {
                    salvarEdicoesPagamentoNoDB(pagamentoModificado, it)
                }
            } else {
                if (updateLaterHistories) {
                    val histories = paymentHistories.value
                    val updatedHistories = histories.toMutableList()
                    val latestHistoryIndex =
                        histories.indexOf(histories.find { h -> h.id == modifiedHistory.id })
                    if (latestHistoryIndex != -1) {
                        // se o index foi encontrado
                        for (i in latestHistoryIndex - 1 downTo 0) {
                            // Iterar do index do historicoRecente - 1 ao 0 para atualizar os historicos
                            val history = histories[i].copy(preco = modifiedHistory.preco)
                            salvarEdicoesPagamentoNoDB(
                                pagamento = null,
                                historico = history
                            )
                            updatedHistories[i] = history
                        }
                    }
                    _paymentHistories.update {
                        updatedHistories.toList()
                    }
                }
                // Atualiza o modifiedHistory no DB (caso so haja ele, nao entrara no if acima)
                salvarEdicoesPagamentoNoDB(pagamentoModificado, modifiedHistory)
            }
        }
    }

    // Funcao que salva as edicoes no Pagamento no DB
    private suspend fun salvarEdicoesPagamentoNoDB(
        pagamento: Pagamento?,
        historico: HistoricoDePagamento,
    ) {
        Log.d(TAG, "salvarEdicoesPagamentoNoDB: pag=$pagamento e hist=$historico")
        withContext(Dispatchers.IO) {
            if (pagamento != null) {
                dataSource.updatePagamento(pagamento)
            }
            dataSource.updateHistoricoDePagamento(historico)
        }
    }

    // Logic for Compose AlertDialog
    enum class AlertType {
        CHANGE_STATUS, DELETE_PAYMENT, UPDATE_PAYMENT, MODIFY_NO_FREQUENCY_PRICE, MODIFY_OLD_HISTORY_PRICE
    }

    private val _alertType = MutableLiveData<AlertType>(AlertType.CHANGE_STATUS)
    val alertType: LiveData<AlertType>
        get() = _alertType

    fun onShowAlertDialog(alertType: AlertType) {
        _alertType.value = alertType
    }

    /**
     * Calls functions, on Dispatchers.IO, for deleting all information regarding this payment from the database
     */
    fun onClickDeletePayment() {
        viewModelScope.launch {
            onDeleteThisPayment()
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

    fun isNoFrequencyPayment(frequenciesArray: Array<String>): Boolean {
        return uiState.value.currentPayment.frequencia == frequenciesArray.last()
    }

    /**
     * Funcao que retorna true caso o latestHistory do UIState seja o historico com data mais recente dentro da List<HistoricoDePagamento>
     *     Isso indica que esse historico foi o ultimo a ser criado, e que nao existem outros historicos depois dele
     */
    fun isMostRecentHistory(history: HistoricoDePagamento): Boolean {
        return history.id == paymentHistories.value.first().id
    }

    /**
     * This function receives only the modified payment histories from the NoFrequencyPriceChangeAlertDialog
     * It then searches in _paymentHistories StateFlow and updates this specific updated HistoricoDePagamento
     */
    fun updateNoFrequencyPrices(updatedHistories: List<HistoricoDePagamento>) {
        val histories = paymentHistories.value.toMutableList()

        updatedHistories.forEach { uh ->
            // Atualizar cada historico atualizado h na lista mutavel antes de atualizar o StateFlow
            histories.indexOfFirst { h -> h.id == uh.id }.let { index ->
                histories[index] = uh
            }
            if (isMostRecentHistory(uh)) {
                _uiState.update { currentState ->
                    currentState.copy(
                        latestPaymentHistory = uh
                    )
                }
            }
        }
        _paymentHistories.update {
            histories
        }
    }
}
