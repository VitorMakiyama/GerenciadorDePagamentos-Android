package com.makiyamasoftware.gerenciadordepagamentos.telas.historicospagamento

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.makiyamasoftware.gerenciadordepagamentos.database.Pagamento
import com.makiyamasoftware.gerenciadordepagamentos.database.PagamentosDatabaseDao


/**
 * Cria o PagamentosDatabaseDao e o context para o o ViewModel
 * **/
class HistoricosPagamentoViewModelFactory(private val dataSource : PagamentosDatabaseDao,
                                          private val application: Application,
                                          private val pagamento: Pagamento) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HistoricosPagamentoViewModel::class.java)) {
            return HistoricosPagamentoViewModel(dataSource, application, pagamento) as T
        }
        throw IllegalArgumentException("ViewModel class desconhecida")
    }
}