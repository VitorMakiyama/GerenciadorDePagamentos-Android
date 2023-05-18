package com.makiyamasoftware.gerenciadordepagamentos

import android.annotation.SuppressLint
import com.makiyamasoftware.gerenciadordepagamentos.database.HistoricoDePagamento
import com.makiyamasoftware.gerenciadordepagamentos.database.Pagamento
import com.makiyamasoftware.gerenciadordepagamentos.database.Pessoa
import java.text.SimpleDateFormat
import java.util.*

@SuppressLint("SimpleDateFormat")
fun convertCalendarToString(calendar: Calendar): String {
    return SimpleDateFormat("yyyy.MM.dd").format(calendar.time).toString()
}

fun convertStringToCalendar(data: String): Calendar {
    val calendar = Calendar.getInstance()
    calendar.time = SimpleDateFormat("yyyy.MM.dd").parse(data)!!
    return calendar
}

fun convertStringDateToStringMonth(data: String): String {
    return SimpleDateFormat("MMM/yy").format(convertStringToCalendar(data).time).toString()
}

fun convertStringDateToStringDay(data: String): String {
    return SimpleDateFormat("dd/MMM/yy").format(convertStringToCalendar(data).time).toString()
}

/**
 * Retorna a pessoa identificada pelo iD dentre a lista de pessoas passada.
 *  Se não encontrar, retorna a primeira pessoa
 */
fun pessoaCerta(pessoas: List<Pessoa>, iD: Long): Pessoa {
    for (i in pessoas) {
        if (i.pessoaID == iD) {
            return i
        }
    }
    return pessoas.first()
}

/**
 * Função que retorna uma lista com os os novos históricos de pagamentos, à partir do
 *  último hisórico, da frequencia do pagamento e da data atual.
 */
fun atualizarNovosHistoricosDePagamento(ultimoHistorico: HistoricoDePagamento, hoje: Calendar, pag: Pagamento, freqs: Array<String>, pessoas: List<Pessoa>): List<HistoricoDePagamento> {
    val ultimaDataHist = convertStringToCalendar(ultimoHistorico.data)

    // Compara a diferenca entre os periodos do ultimoHIstorico e da data atual
    val anos: Int = hoje.get(Calendar.YEAR) - ultimaDataHist.get(Calendar.YEAR)
    var meses = hoje.get(Calendar.MONTH) - ultimaDataHist.get(Calendar.MONTH)
    var dias = hoje.get(Calendar.DAY_OF_YEAR) - ultimaDataHist.get(Calendar.DAY_OF_YEAR)

    // inicia a lista
    val novosHistoricos = mutableListOf<HistoricoDePagamento>()

    when (pag.freqDoPag) {
        // Diário
        freqs[1] -> {
            if (anos > 0) {
                // Logica para acrescentar um dia no caso do ano bissexto (ocorre quando o ano é divisivel por 4)
                for (i in 0..anos) {
                    if ((ultimaDataHist.get(Calendar.YEAR) + i)%4 == 0) {
                        dias += 1
                    }
                }
                // adiciona o numero de anos faltantes (em dias)
                dias += anos*365
            }
            // cria os historicos
            for (i in 0..dias) {
                ultimaDataHist.add(Calendar.DAY_OF_YEAR, 1)
                val h = HistoricoDePagamento(data = convertCalendarToString(ultimaDataHist),
                                                pagamentoID = pag.pagamentoID,
                                                pagadorID = getProximaPessoa(ultimoHistorico.pagadorID, pessoas).pessoaID, preco = ultimoHistorico.preco)
                novosHistoricos.add(h)
            }
            return novosHistoricos
        }
        // Mensal
        freqs[2] -> {
            if (anos > 0) {
                // adiciona o numero de anos faltantes (em meses)
                meses += anos*12
            }
            // cria os historicos
            for (n in 0..meses) {
                ultimaDataHist.add(Calendar.MONTH, 1)
                val h = HistoricoDePagamento(data = convertCalendarToString(ultimaDataHist), pagamentoID = pag.pagamentoID,
                                                pagadorID = getProximaPessoa(ultimoHistorico.pagadorID, pessoas).pessoaID,
                                                preco = ultimoHistorico.preco)
                novosHistoricos.add(h)
            }
            return novosHistoricos
        }
        // Semestral
        freqs[3] -> {
            // converte os meses em semestres
            meses /= 6
            if (anos > 0) {
                // adiciona o numero de anos faltantes (em semestres)
                meses += anos*2
            }
            // cria os historicos
            for (n in 0..meses) {
                ultimaDataHist.add(Calendar.MONTH, 6)
                val h = HistoricoDePagamento(data = convertCalendarToString(ultimaDataHist), pagamentoID = pag.pagamentoID,
                    pagadorID = getProximaPessoa(ultimoHistorico.pagadorID, pessoas).pessoaID,
                    preco = ultimoHistorico.preco)
                novosHistoricos.add(h)
            }
            return novosHistoricos
        }
        // Anual
        freqs[4] -> {
            // cria os historicos
            for (n in 0..anos) {
                ultimaDataHist.add(Calendar.YEAR, 1)
                val h = HistoricoDePagamento(
                    data = convertCalendarToString(ultimaDataHist), pagamentoID = pag.pagamentoID,
                    pagadorID = getProximaPessoa(ultimoHistorico.pagadorID, pessoas).pessoaID,
                    preco = ultimoHistorico.preco
                )
                novosHistoricos.add(h)
            }
            return novosHistoricos
        }
        // Escolha.. e Sem frequência
        else -> return emptyList()
    }
}

/**
 * Funcao que recebe o id da pessoa atual, a lista das pessoas participantes,
 *  e atraves do atributo 'ordem' encontra e retorna a proxima pesoa da ordem,
 *  se for a ultima da ordem, ela recomeça
 */
fun getProximaPessoa(idAtual: Long, pessoas: List<Pessoa>): Pessoa {
    val pAtual = pessoas.find { pessoa: Pessoa ->  pessoa.pessoaID == idAtual}

    // se é a pessoa atual é a ultima da ordem (a List<Pessoa> deve estar ordenada)
    if (pAtual!!.ordem == pessoas.size) {
        // retorna a primeira
        return pessoas.last()
    }
    for (p in pessoas) {
        // verifica se p é o próximo da ordem => n - (n-1) = 1
        if ((p.ordem - pAtual.ordem) == 1) {
            return p
        }
    }
    return pAtual
}

/**
 * Funcao que retorna True se um ja tiver passado um periodo (frequencia), em
 *  relação a data do ultimo HistoricoDePagamento e a data atual. Retorna False
 *  caso contrario
 */
fun precisaDeNovoHistorico(frequencia: String, dataUltimoHistorico: Calendar, hoje: Calendar, freqs: Array<String>): Boolean {
    // Compara a diferenca entre os periodos do ultimoHIstorico e da data atual
    val anos: Int = hoje.get(Calendar.YEAR) - dataUltimoHistorico.get(Calendar.YEAR)
    val meses = hoje.get(Calendar.MONTH) - dataUltimoHistorico.get(Calendar.MONTH)
    val dias = hoje.get(Calendar.DAY_OF_YEAR) - dataUltimoHistorico.get(Calendar.DAY_OF_YEAR)
    when (frequencia) {
        // Diário
        freqs[1] -> {
            if (anos > 0 || dias >0) return true
            return false
        }
        // Mensal
        freqs[2] -> {
            if (anos > 0 || meses > 0) return true
            return false
        }
        // Semestral
        freqs[3] -> {
            // converte os meses em semestres
            val semestres: Int = meses/6
            if (anos > 0 || semestres > 0) return true
            return false
        }
        // Anual
        freqs[4] -> {
            // cria os historicos
            if (anos > 0) return true
            return false
        }
        // Escolha.. e Sem frequência
        else -> return false
    }
}