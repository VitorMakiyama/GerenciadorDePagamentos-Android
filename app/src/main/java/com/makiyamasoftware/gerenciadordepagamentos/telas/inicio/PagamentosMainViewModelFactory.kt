package com.makiyamasoftware.gerenciadordepagamentos.telas.inicio

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.makiyamasoftware.gerenciadordepagamentos.database.PagamentosDatabaseDao

/**
 * Provem o PagamentosDatabaseDao e o context para o o ViewModel
 * **/
class PagamentosMainViewModelFactory (private val dataSource: PagamentosDatabaseDao,
                                      private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PagamentosMainViewModel::class.java)) {
            return PagamentosMainViewModel(dataSource, application) as T
        }
        throw IllegalArgumentException("ViewModel class desconhecida")
    }
}