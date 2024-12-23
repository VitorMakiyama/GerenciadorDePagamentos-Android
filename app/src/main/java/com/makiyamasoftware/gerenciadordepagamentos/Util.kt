package com.makiyamasoftware.gerenciadordepagamentos

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.os.bundleOf
import androidx.navigation.NavDeepLinkBuilder
import com.makiyamasoftware.gerenciadordepagamentos.database.HistoricoDePagamento
import com.makiyamasoftware.gerenciadordepagamentos.database.Pagamento
import com.makiyamasoftware.gerenciadordepagamentos.database.Pessoa
import java.text.SimpleDateFormat
import java.util.Calendar

const val TAG = "UtilFunctions"

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
fun getPessoaCerta(pessoas: List<Pessoa>?, iD: Long): Pessoa {
    if (pessoas != null) {
        for (i in pessoas) {
            if (i.pessoaID == iD) {
                return i
            }
        }
        return pessoas.first()
    }
    return Pessoa(-1, "NULL", -1, -1)
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
    // variavel que guarda o ID do ultimo pagador
    var ultimoPagadorId: Long = ultimoHistorico.pagadorID

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
            for (i in 1..dias) {
                ultimaDataHist.add(Calendar.DAY_OF_YEAR, 1)
                val h = HistoricoDePagamento(data = convertCalendarToString(ultimaDataHist),
                                                pagamentoID = pag.pagamentoID,
                                                pagadorID = getProximaPessoa(ultimoPagadorId, pessoas).pessoaID, preco = ultimoHistorico.preco)
                novosHistoricos.add(h)
                ultimoPagadorId = h.pagadorID
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
            for (n in 1..meses) {
                ultimaDataHist.add(Calendar.MONTH, 1)
                val h = HistoricoDePagamento(data = convertCalendarToString(ultimaDataHist), pagamentoID = pag.pagamentoID,
                                                pagadorID = getProximaPessoa(ultimoPagadorId, pessoas).pessoaID,
                                                preco = ultimoHistorico.preco)
                novosHistoricos.add(h)
                ultimoPagadorId = h.pagadorID
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
            for (n in 1..meses) {
                ultimaDataHist.add(Calendar.MONTH, 6)
                val h = HistoricoDePagamento(data = convertCalendarToString(ultimaDataHist), pagamentoID = pag.pagamentoID,
                    pagadorID = getProximaPessoa(ultimoPagadorId, pessoas).pessoaID,
                    preco = ultimoHistorico.preco)
                novosHistoricos.add(h)
                ultimoPagadorId = h.pagadorID
            }
            return novosHistoricos
        }
        // Anual
        freqs[4] -> {
            // cria os historicos
            for (n in 1..anos) {
                ultimaDataHist.add(Calendar.YEAR, 1)
                val h = HistoricoDePagamento(
                    data = convertCalendarToString(ultimaDataHist), pagamentoID = pag.pagamentoID,
                    pagadorID = getProximaPessoa(ultimoPagadorId, pessoas).pessoaID,
                    preco = ultimoHistorico.preco
                )
                novosHistoricos.add(h)
                ultimoPagadorId = h.pagadorID
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
        // retorna a primeira pessoa para recomeçar
        return pessoas.first()
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

    Log.d(TAG, "meses = $meses, today=${hoje.time} data=${dataUltimoHistorico.time}")

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

const val CHANNEL_ID = "GerenciadorDePagamentosNotificationChannelId"
/**
 * Função que cria e entrega uma push notification
 */
fun createNewHistoryNotification(ctx: Context, textTitle: String, textContent: String, notificationId: Int, pagamento: Pagamento) {
    // Create an explicit intent for an Activity in your app.
    val intent = Intent(ctx, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    val pendingIntent: PendingIntent = NavDeepLinkBuilder(ctx)
        .setGraph(R.navigation.main_navigation)
        .setDestination(R.id.detalhesPagamentoFragment)
        .setArguments(bundleOf(Pair("pagamentoEscolhido", pagamento)))
        .createPendingIntent()// PendingIntent.getActivity(ctx, 0, intent, PendingIntent.FLAG_IMMUTABLE)


    val builder = NotificationCompat.Builder(ctx, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_logo_foreground)
        .setContentTitle(textTitle)
        .setContentText(textContent)
        .setStyle(NotificationCompat.BigTextStyle()
            .bigText(textContent))
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)

    with(NotificationManagerCompat.from(ctx)) {
        if (ActivityCompat.checkSelfPermission(
                ctx,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            // ActivityCompat.requestPermissions()
            // here to request the missing permissions, and then overriding
            // public fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>,
            //                                        grantResults: IntArray)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.

            return@with
        }
        // notificationId is a unique int for each notification that you must define.
        notify(notificationId, builder.build())
    }
}

/**
 * Funcao que utiliza um historico e uma pessoa para escrever o texto da notificacao
 *  de um pagamento num novo periodo
 */
fun getPaymentNotificationContent(historico: HistoricoDePagamento, pessoa: Pessoa): String {
    return "${pessoa.nome} precisa pagar R$ ${historico.preco}, referente à data: ${historico.data}"
}
