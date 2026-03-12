package com.makiyamasoftware.gerenciadordepagamentos.telas.inicio

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.makiyamasoftware.gerenciadordepagamentos.database.HistoricoDePagamento
import com.makiyamasoftware.gerenciadordepagamentos.database.Pagamento
import com.makiyamasoftware.gerenciadordepagamentos.database.PagamentosDatabaseDao
import com.makiyamasoftware.gerenciadordepagamentos.database.Pessoa
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class MainPaymentsUIState(
    val paymentsList: List<Pagamento> = emptyList(),
    val paymentsHistories: List<HistoricoDePagamento> = emptyList(),
    val paymentsPeople: List<Pessoa> = emptyList(),
    val showDeleteDialog: Boolean = false
)

open class PagamentosMainViewModel(val database: PagamentosDatabaseDao, val application: Application) : ViewModel() {
    // UIState for Compose
    private val _uiState = MutableStateFlow(MainPaymentsUIState())
    val uiState: StateFlow<MainPaymentsUIState> = _uiState.asStateFlow()

    fun updateMainPaymentsState() {
        _uiState.update { currentState ->
            currentState.copy(
                paymentsList = pagamentos.value,
                paymentsHistories = latestHistories.value,
                paymentsPeople = latestPeople.value
            )
        }
    }

    fun toggleDialog() {
        _uiState.update { currentState ->
            currentState.copy(
                showDeleteDialog = !currentState.showDeleteDialog
            )
        }
    }

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
    private val _payments = MutableStateFlow<List<Pagamento>>(listOf())
    val pagamentos: StateFlow<List<Pagamento>> = _payments

    // StateFlow that encapsulates the List of latest histories of the payments (historicos atuais)
    private val _latestHistories = MutableStateFlow<List<HistoricoDePagamento>>(listOf())
    val latestHistories: StateFlow<List<HistoricoDePagamento>> = _latestHistories

    // StateFlow that encapsulates the List of People from histories
    private val _latestPeople = MutableStateFlow<List<Pessoa>>(listOf())
    val latestPeople: StateFlow<List<Pessoa>> = _latestPeople

    init {
        updateDataFromDB()
    }

    fun updateDataFromDB() {
        uiScope.launch(Dispatchers.IO) {
            _payments.update{ database.getAllPagamentos() }
            _latestHistories.update { database.getListaInicialHistoricoDePagamento() }
            _latestPeople.update { database.getAllPessoas() }
        }
    }

    // Suspend function to get all people from database
    suspend fun getAllPessoas(): List<Pessoa> {
        return withContext(Dispatchers.IO) {
            database.getAllPessoas()
        }
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
        Log.i(TAG, "entrou no getHistorico HISTORICO${latestHistories.value}")
        if (!latestHistories.value.isNullOrEmpty()) {
            Log.i(TAG, "getHistorico historicos nao null")
            for (i in latestHistories.value!!) if (pagId == i.pagamentoId) return i
        }
        return null
    }

    /**
     * Funcao retorna a Pessoa correspondente ao id de pessoa passado,
     *  dentro das pessoas armazenadas
     */
    fun getPessoaCerta(pessoaId: Long): Pessoa? {
        if (latestPeople.value.isNotEmpty()) {
            for (i in latestPeople.value) {
                if (pessoaId == i.id) return i
            }
        }
        return null
    }

    //TODO: DEPRECATE THIS
    private val _navigateToCriarPagamento = MutableLiveData(false)
    val navigateToCriarPagamento: LiveData<Boolean> = _navigateToCriarPagamento

    // Funções para navegação do FAB, para criar novo pagamento
    fun onCriarNovoPagamento() {
        _navigateToCriarPagamento.value = true
    }
    fun doneNavigating() {
        _navigateToCriarPagamento.value = false
        Log.i("Test","${pagamentos.value.isNullOrEmpty()}")
    }

}