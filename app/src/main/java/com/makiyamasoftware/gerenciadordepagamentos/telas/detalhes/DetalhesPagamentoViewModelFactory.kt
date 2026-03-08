package com.makiyamasoftware.gerenciadordepagamentos.telas.detalhes

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.makiyamasoftware.gerenciadordepagamentos.database.HistoricoDePagamento
import com.makiyamasoftware.gerenciadordepagamentos.database.Pagamento
import com.makiyamasoftware.gerenciadordepagamentos.database.PagamentosDatabaseDao
import com.makiyamasoftware.gerenciadordepagamentos.database.Pessoa


/**
 * Provém o PagamentosDatabaseDao e o context para o o ViewModel
 * **/
class DetalhesPagamentoViewModelFactory (private val dataSource: PagamentosDatabaseDao,
                                         private val selectedPayment: Pagamento,
                                         private val latestPaymentHistory: HistoricoDePagamento,
                                         private val latestPerson: Pessoa
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DetalhesPagamentoViewModel::class.java)) {
            return DetalhesPagamentoViewModel(dataSource, selectedPayment, latestPaymentHistory, latestPerson) as T
        }
        throw IllegalArgumentException("ViewModel class desconhecida")
    }
}