package com.makiyamasoftware.gerenciadordepagamentos.telas.historicospagamento

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.makiyamasoftware.gerenciadordepagamentos.database.HistoricoDePagamento
import com.makiyamasoftware.gerenciadordepagamentos.database.Pagamento
import com.makiyamasoftware.gerenciadordepagamentos.database.PagamentosDatabaseDao
import com.makiyamasoftware.gerenciadordepagamentos.database.Pessoa
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val TAG = "HistoricosPagamentoViewModel"

class HistoricosPagamentoViewModel(private val dataSource: PagamentosDatabaseDao,
                                   val app: Application,
                                   val pagamentoSelecionado : Pagamento): AndroidViewModel(app) {
    lateinit var historicos: LiveData<List<HistoricoDePagamento>>
    lateinit var pessoas: LiveData<List<Pessoa>>
    var historicoClicado: Int = -1

    init {
        viewModelScope.launch {
            historicos = dataSource.getHistoricosDePagamento(pagamentoSelecionado.pagamentoID)
            pessoas = dataSource.getPessoasDoPagamento(pagamentoSelecionado.pagamentoID)
        }
    }

    fun getHistoricoAt(index : Int) : HistoricoDePagamento {
        Log.d(TAG, "HISTS SIZE: ${historicos.value?.size} and index $index")
        return historicos.value?.get(index) ?: throw ArrayIndexOutOfBoundsException("$index do vetor (${historicos.value?.size}) de historicos out of bounds")
    }

    private val _eventUpdateStatus = MutableLiveData<StatusChangeType>()
    val eventUpdateStatus : LiveData<StatusChangeType>
        get() = _eventUpdateStatus

    /** Função que gerencia o click e atualização do status de um historico,
     *      caso o historico clicado esteja NAO PAGO, verifica se existem
     *      historicos NAO PAGOS antes e pergunta se deseja marcar todos
     *      os historicos anteriores como pagos atraves de um AlertDialog
     * */
    fun onClickStatus(position: Int) {
        historicoClicado = position
        if (historicosAnterioresAtualizaveis() && !getHistoricoClicado().estaPago) {
            _eventUpdateStatus.value = StatusChangeType.MULTIPLE
        } else {
            _eventUpdateStatus.value = StatusChangeType.SINGULAR
        }
    }

    /** Verifica se existem historicos NAO PAGOS antes do historico que foi clicado,
     *      retorna true em caso positivo.
     * */
    private fun historicosAnterioresAtualizaveis(): Boolean {
        for (index in historicoClicado + 1..<historicos.value!!.size) {
            // Se achar um não pago, retorna true
            if (!historicos.value!![index].estaPago) {
                Log.d(TAG, "Historicos= ${historicos.value!![index]}")
                return true
            }
        }
        return false
    }

    fun onAtualizarStatus() {
        historicos.value!![historicoClicado].toogleStatus()
        salvarNoBD(historicos.value!![historicoClicado])
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
    fun onAtualizarStatusMultiplos() {
        // começa a partir do item após o historico clicado
        for (i in historicoClicado + 1..<historicos.value!!.size) {
            if (!historicos.value!![i].estaPago) {
                historicos.value!![i].toogleStatus()
                salvarNoBD(historicos.value!![i])
            }
        }
    }



    fun getHistoricoClicado(): HistoricoDePagamento {
        if (historicoClicado < 0) {
            Log.e(TAG, "Erro, acessando historico clicado sem seta-lo!")
            throw UninitializedPropertyAccessException("Acessando historico clicado sem seta-lo!")
        } else {
            return getHistoricoAt(historicoClicado)
        }
    }
}

enum class StatusChangeType { SINGULAR, MULTIPLE}