package com.makiyamasoftware.gerenciadordepagamentos.telas.inicio

import android.app.Application
import android.util.Log
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.makiyamasoftware.gerenciadordepagamentos.database.HistoricoDePagamento
import com.makiyamasoftware.gerenciadordepagamentos.database.Pagamento
import com.makiyamasoftware.gerenciadordepagamentos.database.PagamentosDatabaseDao
import com.makiyamasoftware.gerenciadordepagamentos.database.Pessoa
import kotlinx.coroutines.*

class PagamentosMainViewModel(val database: PagamentosDatabaseDao, val application: Application) : ViewModel() {
    /**
    * Seta o evento para navegar para o fragmento de criar um novo
    *  pagamento
    * */
    private var _navigateToCriarPagamento = MutableLiveData<Boolean>()
    val navigateToCriarPagamento: LiveData<Boolean>
        get() = _navigateToCriarPagamento

    /** Um Job pode ser cancelado, e todas as couroutines associadas a ele sao canceladas tambem,
     * assim, fica facil d ecancelar todas as coroutines de uma vez, se esse ViewModel for destruido
     * isso ocorre no onCleared(), por isso cancelamos o Job nessa funcao
     * **/
    private var viewModelJob = Job()
    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }

    /** Precisamos de um scope da Main Thread (ou tb UI Thread), que lancara todas as coroutines,
     * ja que muitos trabalhos que são efetuado por ela, eventualmente serão mostrados na UI
     *
     * Dispatchers.Main nos retorna a UI thread e ao passarmos o Job junto, estamos associando os dois
     * para criar o Scope
     * */
    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    // Lista de Pagamentos, deve ser atualizada conforme o DB
    var pagamentos: LiveData<List<Pagamento>> = database.getAllPagamentos()

    // LiveData List dos ultimos históricos dos pagamentos (historicos atuais)
    var historicoDosPagamentos : LiveData<List<HistoricoDePagamento>> = database.getListaInicialHistoricoDePagamento()
    var sizeHistorico: Int = 0


    // MutableList das pessoas relativas aos ultimos historicos
    var pessoasRecentes = MutableLiveData<List<Pessoa>>()
    private val _pessoasRecentesState = MutableLiveData<Boolean>(false)
    val pessoasRecentesState: LiveData<Boolean>
        get() =  _pessoasRecentesState
    var sizePessoas: Int = 0
    fun pessoasStateDone() {
        _pessoasRecentesState.value = false
    }

    init {
        uiScope.launch {
            pessoasRecentes.value = getAllPessoas()!!
        }
    }
    // funcao para pegar todas as pessoas
    suspend fun getAllPessoas(): List<Pessoa> {
        return withContext(Dispatchers.IO) {
            database.getAllPessoas()
        }
    }

    /**
     * Atributo para controlar o texto para lista vazia
     */
    private var _visibilityTextoVazio = MutableLiveData<Int>()
    val visibilityTextoVazio: LiveData<Int>
        get() = _visibilityTextoVazio
    fun textoVazioVisivel() {
        _visibilityTextoVazio.value = View.VISIBLE
    }
    fun textoVazioInvisivel() {
        _visibilityTextoVazio.value = View.GONE
    }

    init {
        _navigateToCriarPagamento.value = false

    }

    // Funções para navegação do FAB, para criar novo pagamento
    fun onCriarNovoPagamento() {
        _navigateToCriarPagamento.value = true
    }
    fun doneNavigating() {
        _navigateToCriarPagamento.value = false
        Log.i("Test","${pagamentos.value.isNullOrEmpty()}")
    }

    /** Provisoria: para zerar o DB
     * **/
    fun onClearAll() {
        uiScope.launch {
            zerarODB()
        }
    }
    private suspend fun zerarODB() {
        withContext(Dispatchers.IO) {
            database.clearHistoricoDePagamentos()
            database.clearPagamentos()
            database.clearPessoas()
        }
    }

    /**
     * Funcao retorna o Historico correspondente ao id pagId de pagamento passado, dentro
     * dos historicos armazenados
     */
    fun getHistoricoCerto(pagId: Long): HistoricoDePagamento? {
        Log.i(TAG, "entrou no getHistorico HISTORICO${historicoDosPagamentos.value}")
        if (!historicoDosPagamentos.value.isNullOrEmpty()) {
            Log.i(TAG, "getHistorico historicos nao null")
            for (i in historicoDosPagamentos.value!!) if (pagId == i.pagamentoId) return i
        }
        return null
    }
    /**
     * Funcao retorna o Pagamento correspondente ao id de pagamentro passado, dentro
     * dos pagamentos armazenados
     */
    fun getPagamentoCerto(pagId: Long): Pagamento? {
        if (pagamentos.value != null) {
            for (i in pagamentos.value!!) {
                if (pagId == i.id) return i
            }
        }
        return null
    }
    /**
     * Funcao retorna a Pessoa correspondente ao id de pessoa passado,
     *  dentro das pessoas armazenadas
     */
    fun getPessoaCerta(pessoaId: Long): Pessoa? {
        if (!pessoasRecentes.value.isNullOrEmpty()) {
            for (i in pessoasRecentes.value!!) {
                if (pessoaId == i.id) return i
            }
        }
        return null
    }

    /**
     * Funcao que retorna a pessoa respectiva do historico, identificado
     *  pelo pessoaId (parametro)
     */
    /*fun getPessoaDoHistoricoFromDB(pessoaId: Long, pagId: Long) {
        uiScope.launch {
            if (sizePessoas < sizeHistorico) {
                // Se ainda nao houver uma pessoa desse pagamento, adiciona-la no vetor
                pessoasRecentes.add(_getPessoaDoHistorico(pessoaId))
                sizePessoas++
            }
            _pessoasRecentesState.value = true
        }
    }*/
    private suspend fun _getPessoaDoHistorico(pessoaId: Long) : LiveData<Pessoa> {
        return withContext(Dispatchers.IO) {
            database.getPessoa(pessoaId)
        }
    }
    /**
     * Click listener è evento para o click nos cards, ele aciona a navegaçao atraves de um observer no Fragment
     */
    private val _clickNoPagamento = MutableLiveData<Boolean>()
    val clickNoPagamento : LiveData<Boolean>
        get() = _clickNoPagamento
    var selectedPag: Long = 0L
    fun onClickPagamento(pagamentoId: Long) {
        selectedPag = pagamentoId
        _clickNoPagamento.value = true
    }
    fun onClickPagamentoDone() {
        _clickNoPagamento.value = false
    }
}