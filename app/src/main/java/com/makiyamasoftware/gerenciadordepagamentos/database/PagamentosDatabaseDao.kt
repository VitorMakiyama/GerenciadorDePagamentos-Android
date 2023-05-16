package com.makiyamasoftware.gerenciadordepagamentos.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.Deferred

@Dao
interface PagamentosDatabaseDao {
    @Insert
    fun inserirPagamento(pagamento: Pagamento)
    @Update
    fun updatePagamento(pagamento: Pagamento)
    @Query("SELECT * FROM pagamento_table WHERE pagamentoID = :id")
    fun getPagamento(id: Long): Pagamento
    @Query("SELECT * FROM pagamento_table")
    fun getAllPagamentos(): LiveData<List<Pagamento>>
    @Query("SELECT pagamentoID FROM pagamento_table ORDER BY pagamentoID DESC LIMIT 1")
    fun getUltimoPagamentoID(): Long
    @Query("DELETE FROM pagamento_table")
    fun clearPagamentos()

    @Insert
    fun inserirPessoa(pessoa: Pessoa)
    @Update
    fun updatePessoa(pessoa: Pessoa)
    @Query("SELECT * FROM pessoas_table WHERE pessoaID = :id")
    fun getPessoa(id: Long): LiveData<Pessoa>
    @Query("SELECT * FROM pessoas_table WHERE pagamento_id = :pagamentoID ORDER BY ordem ASC")
    fun getPessoasDoPagamento(pagamentoID: Long): List<Pessoa>
    @Query("DELETE FROM pessoas_table")
    fun clearPessoas()

    @Insert
    fun inserirHistoricoDePagamento(historicoDePagamento: HistoricoDePagamento)
    @Update
    fun updateHistoricoDePagamento(historicoDePagamento: HistoricoDePagamento)
    @Query("SELECT * FROM historico_de_pagamento_table WHERE pagamento_id = :pagamentoID ORDER BY historicoID DESC")
    fun getHistoricosDePagamento(pagamentoID: Long): List<HistoricoDePagamento>
    @Query("SELECT * FROM historico_de_pagamento_table WHERE pagamento_id = :pagamentoID ORDER BY esta_pago,historicoID DESC LIMIT 1")
    fun getUltimoHistoricoDePagamento(pagamentoID: Long): LiveData<HistoricoDePagamento>
    @Query("SELECT * FROM (SELECT * FROM historico_de_pagamento_table " +
            "ORDER BY esta_pago, CASE WHEN esta_pago = 1 THEN historicoID END DESC, " +
            "CASE WHEN esta_pago = 0 THEN historicoID END ASC) GROUP BY pagamento_id ")
    fun getListaInicialHistoricoDePagamento(): LiveData<List<HistoricoDePagamento>>
    @Query("DELETE FROM historico_de_pagamento_table")
    fun clearHistoricoDePagamentos()
}