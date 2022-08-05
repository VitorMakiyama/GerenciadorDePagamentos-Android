package com.makiyamasoftware.gerenciadordepagamentos.telas.inicio

import android.app.Application
import android.util.Log
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.navigation.fragment.findNavController
import com.makiyamasoftware.gerenciadordepagamentos.database.HistoricoDePagamento
import com.makiyamasoftware.gerenciadordepagamentos.database.Pagamento
import com.makiyamasoftware.gerenciadordepagamentos.database.PagamentosDatabaseDao
import kotlinx.coroutines.*
import kotlinx.coroutines.android.awaitFrame

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
    val pagamentos: LiveData<List<Pagamento>> = database.getAllPagamentos()

    //
    var historicoDosPagamentos = MutableLiveData<MutableList<HistoricoDePagamento>>()
    init {
        historicoDosPagamentos.value = mutableListOf<HistoricoDePagamento>()
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
    suspend fun zerarODB() {
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
        if (historicoDosPagamentos.value != null) {
            for (i in historicoDosPagamentos.value!!)
                if (i == null)
                else if (pagId == i.pagamentoID) return i
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
                if (i == null)
                else if (pagId == i.pagamentoID) return i
            }
        }
        return null
    }

    /**
     * Funcao q retorna o historico nao pago mais antigo, e se nao tiver
     * o historico mais recente
     */
    fun getHistoricoNaoPagoAntigoOuUltimo(pagId: Long) {
        uiScope.launch {
            val historicoNovo: MutableList<HistoricoDePagamento> = historicoDosPagamentos.value!!
            historicoNovo.add(_getHistoricoNaoPagoAntigoOuUltimo(pagId))
            historicoDosPagamentos.value = historicoNovo
        }
       //Log.i("TestHistorico", "Historico: ${historico.value?.historicoID} e estaPago: ${historico.value?.estaPago}\n")
    }
    private suspend fun _getHistoricoNaoPagoAntigoOuUltimo(pagId: Long) : HistoricoDePagamento {
        return withContext(Dispatchers.IO) {
            var historico = database.getHistoricoDePagamentoNaoPago(pagId)
            if (historico == null) {
                historico = database.getUltimoHistoricoDePagamento(pagId)
            }
            historico
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