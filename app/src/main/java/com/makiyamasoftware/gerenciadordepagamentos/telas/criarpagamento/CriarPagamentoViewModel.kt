package com.makiyamasoftware.gerenciadordepagamentos.telas.criarpagamento

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Application
import android.util.Log
import android.widget.Spinner
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.makiyamasoftware.gerenciadordepagamentos.R
import com.makiyamasoftware.gerenciadordepagamentos.convertCalendarToString
import com.makiyamasoftware.gerenciadordepagamentos.convertStringToCalendar
import com.makiyamasoftware.gerenciadordepagamentos.database.HistoricoDePagamento
import com.makiyamasoftware.gerenciadordepagamentos.database.Pagamento
import com.makiyamasoftware.gerenciadordepagamentos.database.PagamentosDatabaseDao
import com.makiyamasoftware.gerenciadordepagamentos.database.Pessoa
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "CriarPagamentoViewModel"

class CriarPagamentoViewModel(val database:PagamentosDatabaseDao, application: Application) : AndroidViewModel(application) {
    val nomePagamento = MutableLiveData<String>()

    val dataInicialString = MutableLiveData<String>()
    var dataDB = String()

    val preco = MutableLiveData<String>()

    lateinit var spinnerEscolhaFrequencia: Spinner

    /** Um Job pode ser cancelado, e todas as couroutines associadas a ele sao canceladas tambe,
     * assim, fica facil d ecancelar todas as coroutines de uma vez, se esse ViewModel for destruido
     * isso ocorre no onCleared(), por isso cancelamos o Job nessa funcao
     * **/
    private var viewModelJob = Job()
    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }

    /** Precisamos de um scope da Main Thread (ou tb UI Thread), que lancara todas as coroutines,
     * ja que muitos trabalhos que são efetuado por ela, eventualmente serão mosrados na UI
     * Dispatchers.Main nos retorna a UI thread e ao passarmos o Job junto, estamos associando os dois
     * para criar o Scope
     * */

    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    /**
     * Lista que contem um objeto com as informacoes provisorias do Participante, necessario para
     *  inflar a view e receber os valores
     */
    val participantes = MutableLiveData<MutableList<ParticipanteAux>>()

    init {
        dataInicialString.value = application.getString(R.string.criarParticipantesHeader_escolha_a_data)
        ParticipanteAux().resetParticipanteId()
        val list = mutableListOf<ParticipanteAux>(ParticipanteAux())
        list.add(ParticipanteAux())
        participantes.value = list

        Log.i(TAG,"${list.get(0).nome.value}")
    }

    /**
     * Evento para criar o Pagamento e salvar os dados
     */
    private val _criarNovoPagamentoEvent = MutableLiveData<Boolean>()
    val criarNovoPagamentoEvent: LiveData<Boolean>
        get() = _criarNovoPagamentoEvent
    fun onCriarNovoPagamento() {
        if (isAlgoNaoPreenchido() != "") {
            _camposVazios.value = true
        } else {
            _criarNovoPagamentoEvent.value = true
            var precoDouble: Double = preco.value?.toDouble() ?: 0.00
            uiScope.launch {
                salvarNoDataBase(Pagamento(nome = nomePagamento.value!!, dataDeInicio = dataDB,
                                            numPessoas = participantes.value!!.size, freqDoPag = spinnerEscolhaFrequencia.selectedItem as String
                ),
                                 participantes.value!!, precoDouble)
            }
        }
    }
    fun onCriarNovoPagamentoDone() {
        _criarNovoPagamentoEvent.value = false
    }
    // Funcao que retorna uma string com o nome dos campos que n foram preenchidos
    fun isAlgoNaoPreenchido(): String {
        var naoPreenchidos = ""
        if (nomePagamento.value.isNullOrBlank() || nomePagamento.value.isNullOrEmpty()) {
            naoPreenchidos += getApplication<Application?>().getString(R.string.criarPagamentosFragment_erro_campos_vazios_message_nomePagamento)
        }
        if (dataInicialString.value == getApplication<Application?>().getString(R.string.criarParticipantesHeader_escolha_a_data)) {
            naoPreenchidos += getApplication<Application?>().getString(R.string.criarPagamentosFragment_erro_campos_vazios_message_data)
        }
        if (spinnerEscolhaFrequencia.selectedItem == getApplication<Application?>().resources.getStringArray(R.array.frequencias_pagamentos)[0]) {
            naoPreenchidos += getApplication<Application?>().getString(R.string.criarPagamentosFragment_erro_campos_vazios_message_frequencia)
        }
        if (preco.value.isNullOrBlank() || preco.value.isNullOrEmpty()) {
            naoPreenchidos += getApplication<Application?>().getString(R.string.criarPagamentosFragment_erro_campos_vazios_message_preco)
        }

        var algumParticipanteVazio = false
        for (pessoa in participantes.value!!) {
            if (pessoa.nome.value == null) algumParticipanteVazio = true
        }
        if (algumParticipanteVazio) naoPreenchidos += getApplication<Application?>().getString(R.string.criarPagamentosFragment_erro_campos_vazios_message_participante)
        return naoPreenchidos
    }
    // variavel de evento para acionar o AlertDialog para preencher os campos
    private val _camposVazios = MutableLiveData<Boolean>(false)
    val camposVazios: LiveData<Boolean>
        get() = _camposVazios
    fun onCamposVaziosDone() {
        _camposVazios.value = false
    }

    /**
     * Funcao suspend (significa que queremos chamar essa funcao de uma coroutine, para nao bloquear a main thread)
     * para salvar os dados no DB, ele usa a thread IO que escolhe o melhor momento para fazer as operacoes
     * */
    suspend fun salvarNoDataBase(pagamento: Pagamento, participantes: List<ParticipanteAux>, preco: Double) {
        withContext(Dispatchers.IO) {
            database.inserirPagamento(pagamento)
            val pagId = database.getUltimoPagamentoID()
            Log.i(TAG,"ID pagamento: ${pagamento.pagamentoID} ou ${pagId}?")
            var ordem = 1
            for (i in participantes.indices) {
                participantes[i].nome.value?.let {
                    database.inserirPessoa(Pessoa(nome = it, ordem = ordem, pagamentoId = pagId))
                    val pessId = database.getPessoasDoPagamento(pagId).first().pessoaId
                    if (ordem == 1) database.inserirHistoricoDePagamento(HistoricoDePagamento(data = pagamento.dataDeInicio, preco = preco, pagadorID = pessId, pagamentoID = pagId))
                    ordem++
                }
            }
        }
    }

    /**
     * Evento para inicar o DatePickerFragment e Dialog,
     *   guardar o valor escolhido e atualizar a UI
     */
    private val _escolherDataInicalEvent = MutableLiveData<Boolean>()
    val escolherDataInicalEvent: LiveData<Boolean>
        get() = _escolherDataInicalEvent
    fun onEscolherDataInicial() {
        _escolherDataInicalEvent.value = true
    }
    fun onEscolherDataInicialDone() {
        _escolherDataInicalEvent.value = false
    }
    @SuppressLint("SimpleDateFormat")
    fun onDataInicialEscolhida(dataEscolhida: Calendar) {
        val data = SimpleDateFormat("EEE, dd' de 'MMM' de 'yyyy").format(dataEscolhida.time).toString()
        dataInicialString.value = data.capitalize()

        //Para salvar no DB de forma uniformizada, utilizei uma funcao para isso (implementada no Util.kt)
        dataDB = convertCalendarToString(dataEscolhida)
        Log.i(TAG,"DB data: ${convertCalendarToString(convertStringToCalendar(dataDB))}")
    }

    /**
     * clickListener para adicionar novo participante
     */
    fun onAddNovoParticipante() {
        val lista = participantes.value
        lista?.let {
            it.add( ParticipanteAux())
            participantes.value = it
        }
    }

}

data class ParticipanteAux(
    var dica: String = "",
    val nome: MutableLiveData<String> = MutableLiveData<String>(),
    var id: Long = 0L
) {
    fun resetParticipanteId(){
        participanteId = 1L
    }
    companion object {
        private const val DICA_PARTICIPANTE = "º Participante"
        var participanteId = 1L
    }
    init {
        id = participanteId
        dica = "$id" + DICA_PARTICIPANTE
        participanteId++
    }
}