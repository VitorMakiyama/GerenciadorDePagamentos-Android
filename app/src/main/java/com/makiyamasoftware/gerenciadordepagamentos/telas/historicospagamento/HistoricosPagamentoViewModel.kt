package com.makiyamasoftware.gerenciadordepagamentos.telas.historicospagamento

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.makiyamasoftware.gerenciadordepagamentos.database.HistoricoDePagamento
import com.makiyamasoftware.gerenciadordepagamentos.database.Pagamento
import com.makiyamasoftware.gerenciadordepagamentos.database.PagamentosDatabaseDao
import kotlinx.coroutines.launch

const val TAG = "HistoricosPagamentoViewModel"

class HistoricosPagamentoViewModel(private val dataSource: PagamentosDatabaseDao,
                                   val app: Application,
                                   val pagamentoSelecionado : Pagamento): AndroidViewModel(app) {
    lateinit var historicos: LiveData<List<HistoricoDePagamento>>

    init {
        viewModelScope.launch {
            historicos = dataSource.getHistoricosDePagamento(pagamentoSelecionado.pagamentoID)
        }
    }

    fun getHistoricoAt(index : Int) : HistoricoDePagamento {
        Log.d(TAG, "HISTS SIZE: ${historicos.value?.size} and index $index")
        return historicos.value?.get(index) ?: throw ArrayIndexOutOfBoundsException("$index do vetor (${historicos.value?.size}) de historicos out of bounds")
    }
}