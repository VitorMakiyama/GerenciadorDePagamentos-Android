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
import com.makiyamasoftware.gerenciadordepagamentos.databinding.CriarParticipantesHeaderBinding

private const val TAG = "CriarPagamentoViewModel"

class CriarPagamentoViewModel(val database:PagamentosDatabaseDao, application: Application) : AndroidViewModel(application) {
    val nomePagamento = MutableLiveData<String>()

    val dataInicialString = MutableLiveData<String>()
    var dataDB = String()

    val preco = MutableLiveData<String>()
    lateinit var precoEnabled: CriarParticipantesHeaderBinding

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
                salvarNoDataBase(Pagamento(nome = nomePagamento.value!!, dataDeInicio = dataDB, numPessoas = participantes.value!!.size,
                                            freqDoPag = spinnerEscolhaFrequencia.selectedItem as String), participantes.value!!, precoDouble)
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
        var algumaParcelaDeParticipanteVazia = false
        for (pessoa in participantes.value!!) {
            if (pessoa.nome.value.isNullOrBlank() || pessoa.nome.value.isNullOrEmpty()) algumParticipanteVazio = true
            if (pessoa.parcelaAparece) if (pessoa.preco.value.isNullOrBlank() || pessoa.preco.value.isNullOrEmpty()) algumaParcelaDeParticipanteVazia = true
        }
        if (algumParticipanteVazio) naoPreenchidos += getApplication<Application?>().getString(R.string.criarPagamentosFragment_erro_campos_vazios_message_participante)
        if (algumaParcelaDeParticipanteVazia) naoPreenchidos += getApplication<Application?>().getString(R.string.criarPagamentosFragment_erro_campos_vazios_message_parcela_participante)
        return naoPreenchidos
    }
    // Variavel de evento para acionar o AlertDialog para preencher os campos
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
                    database.inserirPessoa(Pessoa(nome = it, ordem = ordem, pagamentoID = pagId))
                    val pessId = database.getPessoasDoPagamento(pagId).last().pessoaID
                    // Para registrar os historicos de pagamentos SEM periodicidade, nesse caso, cada pessoa gera um historico com o preco = parcela dele
                    if (pagamento.freqDoPag == getApplication<Application?>().resources.getStringArray(R.array.frequencias_pagamentos).last()) {
                        database.inserirHistoricoDePagamento(HistoricoDePagamento(data =pagamento.dataDeInicio, preco = participantes[i].preco.value!!.toDouble(), pagadorID = pessId,
                                                                                    pagamentoID = pagId))
                    } else if (ordem == 1) {
                    // Para registrar apenas o primeiro histórico de pagamentos com periodicidade, para iniciar
                        database.inserirHistoricoDePagamento(HistoricoDePagamento(data = pagamento.dataDeInicio, preco = preco, pagadorID = pessId, pagamentoID = pagId))
                    }
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
            it.add(ParticipanteAux().apply { if (participantes.value!!.first().parcelaAparece) this.parcelaAparece = true})
            participantes.value = it
        }
    }

    /**
     * selectListener do spinner - verifica o tipo de frequencia, e se for 'Sem frequência'
     *   ele irá atualizar o parcelaAparece de todos os participantes para 'true'
     *   se não, atualiza para 'false'
     */
    fun onSelectSpinnerItem () {
        val lista = mutableListOf<ParticipanteAux>()
        // Se o item selecionado for 'Sem frequência', verifica se o primeiro participante já não tem parcelaAparece == false,
        //  se nao, o atribui em todos (necessário copiar os participantes antes de altera-los)
        if (spinnerEscolhaFrequencia.selectedItem == getApplication<Application?>().resources.getStringArray(R.array.frequencias_pagamentos).last()) {
            if (!(participantes.value!!.first().parcelaAparece)) {
                ParticipanteAux().resetParticipanteId()
                for (i in participantes.value!!.indices) {
                    lista.add(participantes.value!![i].copy(parcelaAparece = true))
                }
                participantes.value = lista
                // desabilita o EditText do preco, para não ser editável
                precoEnabled.editTextPrecoPagamento.isEnabled = false
            }
        // Se o item selecionado for 'Escolha a frequência do pagamento', se sim, não altera nada (não há necessidade!)
        } else if (spinnerEscolhaFrequencia.selectedItem == getApplication<Application?>().resources.getStringArray(R.array.frequencias_pagamentos).first()) {
        } else {
            // Se qualquer outro item for selecionado, verificar se o primeiro participante já não tem parcelaAparece == true,
            //  se nao, o atribui em todos (necessário copiar os participantes antes de altera-los, se não o participante que está
            //  na lista do CriarPagamentoAdapter será alterado também)
            if (participantes.value!!.first().parcelaAparece) {
                ParticipanteAux().resetParticipanteId()
                for (i in participantes.value!!.indices) {
                    lista.add(participantes.value!![i].copy(parcelaAparece = false))
                }
                participantes.value = lista
                // Reabilita o EditText do preco, para ser editável e esvazia o preco
                preco.value = ""
                precoEnabled.editTextPrecoPagamento.isEnabled = true
                Log.i(TAG, "Pagamento preco: ${preco.value}")
            }
        }
        Log.i(TAG,"participantes: ${participantes.value}")
    }
}

data class ParticipanteAux(
    var dica: String = "",
    var dicaPreco: String = "",
    var parcelaAparece: Boolean = false,
    val nome: MutableLiveData<String> = MutableLiveData<String>(),
    val preco: MutableLiveData<String> = MutableLiveData<String>(),
    var id: Long = 0L
) {
    fun resetParticipanteId(){
        participanteId = 1L
    }
    companion object {
        private const val DICA_PARTICIPANTE = "º Participante"
        private const val DICA_PARTICIPANTE_PARCELA = "Parcela "
        private const val DICA_PARTICIPANTE_PRECO = "º Participante"
        var participanteId = 1L
    }
    init {
        id = participanteId
        dica = "$id" + DICA_PARTICIPANTE
        dicaPreco = DICA_PARTICIPANTE_PARCELA + "$id" + DICA_PARTICIPANTE_PRECO
        participanteId++
    }
}