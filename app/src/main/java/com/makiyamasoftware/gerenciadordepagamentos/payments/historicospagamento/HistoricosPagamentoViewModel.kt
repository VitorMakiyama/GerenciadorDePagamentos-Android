package com.makiyamasoftware.gerenciadordepagamentos.payments.historicospagamento

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.makiyamasoftware.gerenciadordepagamentos.database.HistoricoDePagamento
import com.makiyamasoftware.gerenciadordepagamentos.database.Pagamento
import com.makiyamasoftware.gerenciadordepagamentos.database.PagamentosDatabaseDao
import com.makiyamasoftware.gerenciadordepagamentos.database.Pessoa
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "HistoricosPagamentoViewModel"

data class PaymentHistoriesUIState(
    val histories: List<HistoricoDePagamento>,
    val people: List<Pessoa>,
    val statusChangeType: StatusChangeType? = null,
    val updateData: Boolean = false,
)

class HistoricosPagamentoViewModel(
    private val dataSource: PagamentosDatabaseDao,
    val pagamentoSelecionado: Pagamento
) : ViewModel() {
    private val _uiState = MutableStateFlow(PaymentHistoriesUIState(listOf(),listOf()))
    val uiState: StateFlow<PaymentHistoriesUIState> = _uiState.asStateFlow()

    var clickedHistoryIndex: Int = -1

    init {
        updateDataFromDB()
    }

    fun updateDataFromDB() {
        viewModelScope.launch(Dispatchers.IO) {
            val histories = dataSource.getHistoricosDePagamento(pagamentoSelecionado.id)
            val people = dataSource.getPessoasDoPagamento(pagamentoSelecionado.id)
            _uiState.update { currentState ->
                currentState.copy(
                    histories = histories,
                    people = people,
                    updateData = false
                )
            }
        }
    }

    /** Função que gerencia o click e atualização do status de um historico,
     *      caso o historico clicado esteja NAO PAGO, verifica se existem
     *      historicos NAO PAGOS antes e pergunta se deseja marcar todos
     *      os historicos anteriores como pagos atraves de um AlertDialog
     * */
    fun onClickStatus(history: HistoricoDePagamento) {
        clickedHistoryIndex = uiState.value.histories.indexOf(history)
        if (historicosAnterioresAtualizaveis() && !getHistoricoClicado().estaPago) {
            // opção de mostrar dialog para mudar multiplos Historicos
            _uiState.update {
                it.copy(
                    statusChangeType = StatusChangeType.MULTIPLE
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    statusChangeType = StatusChangeType.SINGULAR
                )
            }
        }
    }

    /** Verifica se existem historicos NAO PAGOS antes do historico que foi clicado,
     *      retorna true em caso positivo.
     * */
    private fun historicosAnterioresAtualizaveis(): Boolean {
        for (index in clickedHistoryIndex + 1..< uiState.value.histories.size) {
            // Se achar um não pago, retorna true
            if (!uiState.value.histories[index].estaPago) {
                Log.d(TAG, "Previous unpaid history found= ${uiState.value.histories[index]}")
                return true
            }
        }
        return false
    }

    fun onUpdateSingleStatus() {
        val histories = _uiState.value.histories
        histories[clickedHistoryIndex].toogleStatus()
        _uiState.update {
            it.copy(
                histories = histories,
                updateData = true
            )
        }
        salvarNoBD(histories[clickedHistoryIndex])
    }

    private fun salvarNoBD(historico: HistoricoDePagamento) {
        viewModelScope.launch {
            salvarHistoricoNoBD(historico)
        }
    }

    private suspend fun salvarHistoricoNoBD(historico: HistoricoDePagamento) {
        withContext(Dispatchers.IO) {
            dataSource.updateHistoricoDePagamento(historico)
        }
    }

    fun onUpdateMultipleStatus() {
        val histories = uiState.value.histories
        // começa a partir do item após o historico clicado
        for (i in clickedHistoryIndex + 1..< histories.size) {
            if (!histories[i].estaPago) {
                histories[i].toogleStatus()
                salvarNoBD(histories[i])
            }
        }
        _uiState.update {
            it.copy(
                histories = histories
            )
        }
    }

    fun getHistoricoClicado(): HistoricoDePagamento {
        return uiState.value.histories[clickedHistoryIndex]
    }

    fun onDismissChangeStatusAlertDialog() {
        _uiState.update {
            it.copy(
                statusChangeType = null
            )
        }
    }
}

enum class StatusChangeType { SINGULAR, MULTIPLE }