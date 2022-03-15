package com.makiyamasoftware.gerenciadordepagamentos

import android.annotation.SuppressLint
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