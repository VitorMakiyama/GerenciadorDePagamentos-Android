package com.makiyamasoftware.gerenciadordepagamentos.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface PagamentosDatabaseDao {
    /** Pagamento **/
    @Insert
    fun inserirPagamento(pagamento: Pagamento)
    @Update
    fun updatePagamento(pagamento: Pagamento)
    @Query("SELECT * FROM pagamento_table WHERE id = :id")
    fun getPagamento(id: Long): LiveData<Pagamento>
    @Query("SELECT * FROM pagamento_table")
    fun getAllPagamentos(): LiveData<List<Pagamento>>
    @Query("SELECT id FROM pagamento_table ORDER BY id DESC LIMIT 1")
    fun getUltimoPagamentoID(): Long
    @Query("DELETE FROM pagamento_table WHERE id = :pagamentoID")
    fun deletePagamento(pagamentoID: Long)
    @Query("DELETE FROM pagamento_table")
    fun clearPagamentos()
    @Query("SELECT * FROM pagamento_table")
    fun getAllPagamentosBackground() : List<Pagamento>

    /** Pessoa **/
    @Insert
    fun inserirPessoa(pessoa: Pessoa)
    @Update
    fun updatePessoa(pessoa: Pessoa)
    @Query("SELECT * FROM pessoas_table WHERE id = :id")
    fun getPessoa(id: Long): LiveData<Pessoa>
    @Query("SELECT * FROM pessoas_table WHERE pagamento_id = :pagamentoID ORDER BY ordem DESC LIMIT 1")
    fun getUltimaPessoasDoPagamento(pagamentoID: Long): Pessoa
    @Query("SELECT * FROM pessoas_table WHERE pagamento_id = :pagamentoID ORDER BY ordem ASC")
    fun getPessoasDoPagamento(pagamentoID: Long): LiveData<List<Pessoa>>
    @Query("SELECT * FROM pessoas_table ORDER BY pagamento_id, ordem ASC")
    fun getAllPessoas(): List<Pessoa>
    @Query("DELETE FROM pessoas_table WHERE pagamento_id = :pagamentoID")
    fun deletePessoasFromPagamento(pagamentoID: Long)
    @Query("DELETE FROM pessoas_table")
    fun clearPessoas()
    @Query("SELECT * FROM pessoas_table WHERE pagamento_id = :pagamentoID ORDER BY ordem ASC")
    fun getPessoasDoPagamentoBackground(pagamentoID: Long) : List<Pessoa>

    /** HistoricoDePagamento **/
    @Insert
    fun inserirHistoricoDePagamento(vararg historicoDePagamento: HistoricoDePagamento)
    @Update
    fun updateHistoricoDePagamento(historicoDePagamento: HistoricoDePagamento)
    @Query("SELECT * FROM historico_de_pagamento_table WHERE pagamento_id = :pagamentoID ORDER BY id DESC")
    fun getHistoricosDePagamento(pagamentoID: Long): LiveData<List<HistoricoDePagamento>>
    @Query("SELECT * FROM historico_de_pagamento_table WHERE pagamento_id = :pagamentoID ORDER BY esta_pago,id DESC LIMIT 1")
    fun getUltimoHistoricoDePagamento(pagamentoID: Long): LiveData<HistoricoDePagamento>
    @Query("SELECT * FROM (SELECT * FROM historico_de_pagamento_table " +
            "ORDER BY esta_pago, CASE WHEN esta_pago = 1 THEN id END DESC, " +
            "CASE WHEN esta_pago = 0 THEN id END ASC) GROUP BY pagamento_id "
    )
    fun getListaInicialHistoricoDePagamento(): LiveData<List<HistoricoDePagamento>>
    @Query("DELETE FROM historico_de_pagamento_table WHERE pagamento_id = :pagamentoID")
    fun deleteHistoricoDePagamentosFromPagamento(pagamentoID: Long)
    @Query("DELETE FROM historico_de_pagamento_table")
    fun clearHistoricoDePagamentos()
    @Query("SELECT * FROM historico_de_pagamento_table WHERE pagamento_id = :pagamentoID ORDER BY id DESC LIMIT 1")
    fun getHistoricoDePagamentoBackground(pagamentoID: Long) : HistoricoDePagamento
}