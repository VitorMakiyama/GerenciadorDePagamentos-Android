package com.makiyamasoftware.gerenciadordepagamentos.telas.detalhes

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.makiyamasoftware.gerenciadordepagamentos.database.Pagamento
import com.makiyamasoftware.gerenciadordepagamentos.database.PagamentosDatabaseDao


/**
 * Provém o PagamentosDatabaseDao e o context para o o ViewModel
 * **/
class DetalhesPagamentoViewModelFactory (private val dataSource: PagamentosDatabaseDao,
                                      private val application: Application,
                                         private val pagamentoSelecionado: Pagamento
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DetalhesPagamentoViewModel::class.java)) {
            return DetalhesPagamentoViewModel(dataSource, application, pagamentoSelecionado) as T
        }
        throw IllegalArgumentException("ViewModel class desconhecida")
    }
}