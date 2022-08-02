package com.makiyamasoftware.gerenciadordepagamentos

import android.annotation.SuppressLint
import com.makiyamasoftware.gerenciadordepagamentos.database.Pessoa
import java.text.SimpleDateFormat
import java.util.*

@SuppressLint("SimpleDateFormat")
fun convertCalendarToString(calendar: Calendar): String {
    return SimpleDateFormat("yyyy.MM.dd").format(calendar.time).toString()
}

fun convertStringToCalendar(data: String): Calendar {
    val calendar = Calendar.getInstance()
    calendar.setTime(SimpleDateFormat("yyyy.MM.dd").parse(data)!!)
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