package com.makiyamasoftware.gerenciadordepagamentos.telas.detalhes

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.makiyamasoftware.gerenciadordepagamentos.R
import com.makiyamasoftware.gerenciadordepagamentos.atualizarNovosHistoricosDePagamento
import com.makiyamasoftware.gerenciadordepagamentos.database.HistoricoDePagamento
import com.makiyamasoftware.gerenciadordepagamentos.database.Pagamento
import com.makiyamasoftware.gerenciadordepagamentos.database.PagamentosDatabaseDao
import com.makiyamasoftware.gerenciadordepagamentos.database.Pessoa
import com.makiyamasoftware.gerenciadordepagamentos.databinding.FragmentDetalhesPagamentoBinding
import com.makiyamasoftware.gerenciadordepagamentos.getPessoaCerta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

private const val TAG = "DetalhesPagamentoViewModel"

class DetalhesPagamentoViewModel(private val dataSource: PagamentosDatabaseDao,
                                 private val app: Application,
                                 private val pagamentoSelecionado: Pagamento) : AndroidViewModel(app) {
    private val viewModelJob = Job()
    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }

    lateinit var historicoDePagamento: LiveData<List<HistoricoDePagamento>>
    val histRecente: HistoricoDePagamento
        get() = getHistoricoRecente()

    lateinit var pagamentoLiveData: LiveData<Pagamento>
    lateinit var pessoas: LiveData<List<Pessoa>>

    // Variavel que guarda as edicoes do Switch de auto update
    var pagamentoSelecionadoAutoUpdate = (pagamentoSelecionado.autoUpdateHistorico)

    init {
        viewModelScope.launch {
            pagamentoLiveData = dataSource.getPagamento(pagamentoSelecionado.pagamentoID)
            Log.i(TAG, "Entrou na coroutine de buscar o historico")
            historicoDePagamento = dataSource.getHistoricosDePagamento(pagamentoSelecionado.pagamentoID)
            pessoas = dataSource.getPessoasDoPagamento(pagamentoSelecionado.pagamentoID)
            Log.i(TAG, "Requisitou o historico e as pessoas do DB")
        }
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


    // Comeca do historico mais antigo (ultimo do vetor) buscando o historico NAO PAGO mais recente
    private fun getHistoricoRecente(): HistoricoDePagamento {
        for (i in (historicoDePagamento.value!!.size - 1) downTo 1) {
            val historico = historicoDePagamento.value!![i]
            if (!(historico.estaPago)) return historico
        }
        return historicoDePagamento.value!!.first()
    }

    fun atualizarPreco() {
        if (pagamentoSelecionado.freqDoPag == app.resources.getStringArray(R.array.frequencias_pagamentos).last()) {
            var p: Double = 0.0
            for (hist in historicoDePagamento.value!!) {
                p += hist.preco
            }
            _preco.value = p.toString()
        } else {
            _preco.value = histRecente.preco.toString()
        }
        Log.i(TAG, "Click Data: paggId:${pagamentoSelecionado.pagamentoID} e o historico e \n${historicoDePagamento.value!!.size}")
    }

    // Essa funcao atualiza as váriaveis Livedata que estão ligadas ao layout xml através de DataBinding,
    //  é necessario verificar se ambos os LiveData<List> de Pessoa e HistoricoDePagamento já foram retornados pelo DB DAO.
    //  Assim essa funcao tbm deve ser chamada por ambos os observers, assim o ultimo a receber os dados os atualiza.
    fun atualizarHistoricoRecente() {
        if (pessoas.value != null && historicoDePagamento.value != null) {
            _ultHistNomePessoa.value = getPessoaCerta(pessoas.value!!, histRecente.pagadorID).nome
            _ultHistPrecoPessoa.value = app.getString(R.string.simbolo_BRL) + " " + histRecente.preco.toString()
        }
    }

    // Faz o bind da data, do texto e da cor do status do historico mais recente (cardview)
    fun bindHistRecente(binding: FragmentDetalhesPagamentoBinding) {
        binding.textDataHist.text = histRecente.getDataString(getApplication(), pagamentoSelecionado.freqDoPag)
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
        viewModelScope.launch {
            saveNovoHistoricoOnDB(historico)
        }
    }
    private suspend fun saveNovoHistoricoOnDB(historico: HistoricoDePagamento) {
        withContext(Dispatchers.IO) {
            dataSource.updateHistoricoDePagamento(historico)
            Log.i(TAG, "Terminou de salvar o status do historico")
        }
    }

    //TODO refazer escolherDataInicial
    fun onEscolherDataInicial() {
        Toast.makeText(app, "Clicou no escolherDataInicial", Toast.LENGTH_LONG).show()
        Log.i(TAG, "Pegou o historico de paggId:${pagamentoSelecionado.pagamentoID} e o historico e \n${historicoDePagamento.value}")
    }
    //TODO refazer
    fun onSelectSpinnerItem() {

    }


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
            pagamentoSelecionado,
            app.resources.getStringArray(R.array.frequencias_pagamentos),
            pessoas.value!!)
        Log.i(TAG, "$novosHistoricos")

        // Parte de interação com o DataBase
        viewModelScope.launch {
            // Salva os novos historicos no DB
            salvarAtualizacoesHistorico(novosHistoricos)
            // Chama a função de puxar os historicos do DB, para atualizar os dados e a UI
        }
    }
    // Funcao para salvar os Historicos atualizados no DataBase, no background sem interromper a Main thread (UI)
    private suspend fun salvarAtualizacoesHistorico(novosHistoricos: List<HistoricoDePagamento>) {
        withContext(Dispatchers.IO) {
            for (historico in novosHistoricos) dataSource.inserirHistoricoDePagamento(historico)
        }
    }

    // Logica para salvar as edicoes
    private val _onSalvarEdicoes = MutableLiveData<Boolean>()
    val onSalvarEdicoes: LiveData<Boolean>
        get() = _onSalvarEdicoes

    // Funcao chamada ao clicar no salvarFAB
    fun onClickSalvarEdicoes() {
        _onSalvarEdicoes.value = true
        viewModelScope.launch {
            salvarEdicoesPagamentoNoDB()
        }
    }

    // Funcao que salva as edicoes no Pagamento no DB
    private suspend fun salvarEdicoesPagamentoNoDB() {
        withContext(Dispatchers.IO) {
            dataSource.updatePagamento(
                Pagamento(
                    pagamentoID = pagamentoSelecionado.pagamentoID,
                    nome = pagamentoSelecionado.nome,
                    dataDeInicio = pagamentoSelecionado.dataDeInicio,
                    freqDoPag = pagamentoSelecionado.freqDoPag,
                    numPessoas = pagamentoSelecionado.numPessoas,
                    autoUpdateHistorico = pagamentoSelecionadoAutoUpdate
                )
            )
        }
    }

    // Funcao chamada pelo observer ao terminar de alterar as views, para resetar a variavel de estado
    fun onSavedEdicoes() {
        _onSalvarEdicoes.value = false
        Log.d(TAG, "auto update ${pagamentoSelecionadoAutoUpdate}")
    }
}