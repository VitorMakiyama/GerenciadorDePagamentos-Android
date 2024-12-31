package com.makiyamasoftware.gerenciadordepagamentos.database

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class FakePagamentosDao: PagamentosDatabaseDao {
    val pagamentosData: LinkedHashMap<Long, Pagamento> = LinkedHashMap()
    val pessoasData: LinkedHashMap<Long, Pessoa> = LinkedHashMap()
    val historicosData: LinkedHashMap<Long, HistoricoDePagamento> = LinkedHashMap()
    private var shouldReturnError = false
    fun setReturnError(value: Boolean) {
        shouldReturnError = value
    }

    override fun inserirPagamento(pagamento: Pagamento) {
        pagamentosData[pagamento.pagamentoID] = pagamento
    }

    override fun updatePagamento(pagamento: Pagamento) {
        TODO("Not yet implemented")
    }

    override fun getPagamento(id: Long): LiveData<Pagamento> {
        TODO("Not yet implemented")
    }

    override fun getAllPagamentos(): LiveData<List<Pagamento>> {
        TODO("Not yet implemented")
    }

    override fun getUltimoPagamentoID(): Long {
        TODO("Not yet implemented")
    }

    override fun clearPagamentos() {
        TODO("Not yet implemented")
    }

    override fun getAllPagamentosBackground(): List<Pagamento> {
        TODO("Not yet implemented")
    }

    override fun inserirPessoa(pessoa: Pessoa) {
        pessoasData[pessoa.pessoaID] = pessoa
    }

    override fun updatePessoa(pessoa: Pessoa) {
        TODO("Not yet implemented")
    }

    override fun getPessoa(id: Long): LiveData<Pessoa> {
        TODO("Not yet implemented")
    }

    override fun getUltimaPessoasDoPagamento(pagamentoID: Long): Pessoa {
        TODO("Not yet implemented")
    }

    override fun getPessoasDoPagamento(pagamentoID: Long): LiveData<List<Pessoa>> {
        val _data = MutableLiveData(pessoasData.values.toList())
        val data: LiveData<List<Pessoa>> = _data
        return data
    }

    override fun getAllPessoas(): List<Pessoa> {
        TODO("Not yet implemented")
    }

    override fun clearPessoas() {
        TODO("Not yet implemented")
    }

    override fun getPessoasDoPagamentoBackground(pagamentoID: Long): List<Pessoa> {
        TODO("Not yet implemented")
    }

    override fun inserirHistoricoDePagamento(vararg historicoDePagamento: HistoricoDePagamento) {
        for (historico in historicoDePagamento) {
            historicosData[historico.historicoID] = historico
        }
    }

    override fun updateHistoricoDePagamento(historicoDePagamento: HistoricoDePagamento) {
        historicosData[historicoDePagamento.historicoID] = historicoDePagamento
    }

    override fun getHistoricosDePagamento(pagamentoID: Long): LiveData<List<HistoricoDePagamento>> {
        val _data = MutableLiveData(historicosData.values.toList().reversed())
        val data: LiveData<List<HistoricoDePagamento>> = _data
        return data
    }

    override fun getUltimoHistoricoDePagamento(pagamentoID: Long): LiveData<HistoricoDePagamento> {
        TODO("Not yet implemented")
    }

    override fun getListaInicialHistoricoDePagamento(): LiveData<List<HistoricoDePagamento>> {
        TODO("Not yet implemented")
    }

    override fun clearHistoricoDePagamentos() {
        TODO("Not yet implemented")
    }

    override fun getHistoricoDePagamentoBackground(pagamentoID: Long): HistoricoDePagamento {
        TODO("Not yet implemented")
    }
}