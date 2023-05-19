package com.makiyamasoftware.gerenciadordepagamentos.telas.detalhes

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.makiyamasoftware.gerenciadordepagamentos.R
import com.makiyamasoftware.gerenciadordepagamentos.atualizarNovosHistoricosDePagamento
import com.makiyamasoftware.gerenciadordepagamentos.database.HistoricoDePagamento
import com.makiyamasoftware.gerenciadordepagamentos.database.Pagamento
import com.makiyamasoftware.gerenciadordepagamentos.database.PagamentosDatabaseDao
import com.makiyamasoftware.gerenciadordepagamentos.database.Pessoa
import com.makiyamasoftware.gerenciadordepagamentos.databinding.FragmentDetalhesPagamentoBinding
import com.makiyamasoftware.gerenciadordepagamentos.pessoaCerta
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

private const val TAG = "DetalhesPagamentoViewModel"

class DetalhesPagamentoViewModel(private val dataSource: PagamentosDatabaseDao,
                                 private val app: Application) : AndroidViewModel(app) {
    private val viewModelJob = Job()
    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }
    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    private var _pagamentoSelecionado  = MutableLiveData<Pagamento>()
    val pagamentoSelecionado: LiveData<Pagamento>
        get() = _pagamentoSelecionado
    var historicoDePagamento = MutableLiveData<List<HistoricoDePagamento>>()
    val histRecente: HistoricoDePagamento
        get() = getHistoricoRecente()

    var pessoas = MutableLiveData<List<Pessoa>>()
    private var editavel: Boolean = false

    init {
        historicoDePagamento.value = listOf<HistoricoDePagamento>()
        getHistoricoEPessoas()
    }

    // Preco atual do pagamento (atualizado se o usuario editar o preco e escolher a opção de "Salvar alterações"
    private val _preco = MutableLiveData<String>()
    val preco: LiveData<String>
        get() = _preco
    // Do cardView do ultimo historico
    private val _ultHistNomePessoa = MutableLiveData<String>()
    val ultHistNomePessoa: LiveData<String>
        get() = _ultHistNomePessoa
    private val _ultHistPrecoPessoa = MutableLiveData<String>()
    val ultHistPrecoPessoa: LiveData<String>
        get() = _ultHistPrecoPessoa


    val nomePagamentoEditado = MutableLiveData<String>()
    val precoEditado = MutableLiveData<String>()

    private fun getHistoricoEPessoas() {
        uiScope.launch {
            Log.i(TAG, "Entrou na coroutine de buscar o historico")
            val historico = getAllHistorico()
            val pessoasTemp = getAllPessoas()
            historicoDePagamento.value = historico
            pessoas.value = pessoasTemp
            Log.i(TAG, "Atribuiu o historico")
        }
    }
    // Comeca do historico mais antigo (ultimo do vetor) buscando o historico NAO PAGO mais recente
    private fun getHistoricoRecente(): HistoricoDePagamento {
        for (i in (historicoDePagamento.value!!.size - 1) downTo 1) {
            val historico = historicoDePagamento.value!![i]
            if (!(historico.estaPago)) return historico
        }
        return historicoDePagamento.value!!.first()
    }
    private suspend fun getAllHistorico(): List<HistoricoDePagamento> {
        return withContext(Dispatchers.IO) {
            val historico = dataSource.getHistoricosDePagamento(pagamentoSelecionado.value!!.pagamentoID)
            //historico.value?: listOf()
            historico
        }
    }
    private suspend fun getAllPessoas(): List<Pessoa> {
        return withContext(Dispatchers.IO) {
            val pessoas = dataSource.getPessoasDoPagamento(pagamentoSelecionado.value!!.pagamentoID)
            pessoas
        }
    }
    fun atualizarPreco() {
        if (pagamentoSelecionado.value!!.freqDoPag != app.resources.getStringArray(R.array.frequencias_pagamentos)
                .last()) {
            _preco.value = historicoDePagamento.value!!.first().preco.toString()
        }
        Log.i(TAG, "Click Data: paggId:${pagamentoSelecionado.value!!.pagamentoID} e o historico e \n${historicoDePagamento.value!!.size}")
    }
    fun atualizarHistorico() {
        if (pessoas.value != null) {
            _ultHistNomePessoa.value = pessoaCerta(pessoas.value!!, histRecente.pagadorID).nome
        }
        _ultHistPrecoPessoa.value = app.getString(R.string.simbolo_BRL) + " " + histRecente.preco.toString()
    }

    fun bindHistRecente(binding: FragmentDetalhesPagamentoBinding) {
        binding.textDataHist.text = histRecente.getDataString(getApplication(), pagamentoSelecionado.value!!.freqDoPag)
        binding.textStatusHist.text = histRecente.getEstaPagoString(getApplication())
        binding.backgroungHist.setBackgroundColor(histRecente.getBackgroundColorInt(getApplication()))
    }
    // Atributos e funcoes auxiliares para controlar o click do status do Historico
    private val _onMudarStatus = MutableLiveData<Boolean>()
    val onMudarStatus: LiveData<Boolean>
        get() = _onMudarStatus
    fun onClickStatusHistorico() {
        _onMudarStatus.value = true
    }
    fun onClickStatusHistoricoDone() {
        _onMudarStatus.value = false
    }
    fun onMudarStatus() {
        val historico = histRecente
        historico.toogleStatus()
        uiScope.launch {
            saveNovoHistoricoOnDB(historico)
            historicoDePagamento.value = getAllHistorico()!!
        }
    }
    private suspend fun saveNovoHistoricoOnDB(historico: HistoricoDePagamento) {
        withContext(Dispatchers.IO) {
            dataSource.updateHistoricoDePagamento(historico)
            Log.i(TAG, "Terminou de salvar o status do historico")
        }
    }

    fun setPagamento(pagamento: Pagamento) {
        _pagamentoSelecionado.value = pagamento
    }

    //TODO refazer escolherDataInicial
    fun onEscolherDataInicial() {
        if (editavel) {
            Toast.makeText(app, "Clicou no escolherDataInicial", Toast.LENGTH_LONG).show()
        }
        Log.i(TAG, "Pegou o historico de paggId:${pagamentoSelecionado.value!!.pagamentoID} e o historico e \n${historicoDePagamento.value}")
    }
    //TODO refazer
    fun onSelectSpinnerItem() {

    }
    //TODO: fazer a navegação para a página de históricos
    private var _verTodoOHistorico = MutableLiveData<Boolean>()
    val verTodoOHistorico: LiveData<Boolean>
        get() = _verTodoOHistorico
//     Funcoes auxiliares para monitorar a LiveData do botao de ver todos os Historicos
    fun onVerTodoOHistorico() {
        _verTodoOHistorico.value = true
    }
    fun onVerTodoOHistoricoDone() {
        _verTodoOHistorico.value = false
    }

    //
    fun getHistoricoMaisRecente(): HistoricoDePagamento{
        return historicoDePagamento.value!!.first()
    }
    /**
     *  Funcao chamada quando, ao iniciar esse fragment, e' detectada a necessidade de
     *      atualizar os Historicos do Pagamento, pois o ultimo esta com uma data menor que
     *      hoje - periodo
     */
    fun onUpdateHistoricosDoPagamento() {
        val novosHistoricos = atualizarNovosHistoricosDePagamento(getHistoricoRecente(),
            Calendar.getInstance(),
            pagamentoSelecionado.value!!,
            app.resources.getStringArray(R.array.frequencias_pagamentos),
            pessoas.value!!)
        Log.i(TAG, "$novosHistoricos")

        uiScope.launch {
            salvarAtualizacoesHistorico(novosHistoricos)
        }
    }
    // Funcao para salvar os Historicos atualizados no DataBase, no background sem interromper a Main thread (UI)
    private suspend fun salvarAtualizacoesHistorico(novosHistoricos: List<HistoricoDePagamento>) {
        withContext(Dispatchers.IO) {
            for (historico in novosHistoricos) dataSource.inserirHistoricoDePagamento(historico)
        }
    }
}