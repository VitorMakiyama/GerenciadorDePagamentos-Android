package com.makiyamasoftware.gerenciadordepagamentos.database

import android.app.Application
import android.graphics.Color
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.makiyamasoftware.gerenciadordepagamentos.R
import com.makiyamasoftware.gerenciadordepagamentos.convertStringDateToStringDay
import com.makiyamasoftware.gerenciadordepagamentos.convertStringDateToStringMonth
import java.lang.Exception

@Entity(tableName = "historico_de_pagamento_table")
data class HistoricoDePagamento(
        @PrimaryKey(autoGenerate = true)
        val historicoID: Long = 0L,

        val data: String,

        val preco: Double,

        @ColumnInfo(name = "pagador_id")
        val pagadorID: Long,

        @ColumnInfo(name = "pagamento_id")
        val pagamentoID: Long,

        @ColumnInfo(name = "esta_pago")
        var estaPago: Boolean = false
) {
    fun toogleStatus() {
        estaPago = !estaPago
    }
    fun getDataString(application: Application, freqDoPagamento: String): String {
            val freqPossiveis: Array<String> = application.resources.getStringArray(R.array.frequencias_pagamentos)
            // freqDoPagamento == "Escolha a frequencia"
            when (freqDoPagamento) {
                freqPossiveis[0] -> {
                    throw Exception("Erro HistoricoDePagamento: frequencia invalida")
                }
                freqPossiveis[1] -> {
                    return convertStringDateToStringDay(data)
                }
                else -> {
                    return convertStringDateToStringMonth(data)
                }
            }
    }
    fun getEstaPagoString(application: Application): String {
            if (estaPago) {
                    return application.getString(R.string.blocoEstaPago_status_pago)
            } else return application.getString(R.string.blocoEstaPago_status_naoPago)
    }
    fun getBackgroundColorInt(application: Application): Int {
            if (estaPago) {
                    return application.resources.getColor(R.color.colorPagoBackground, null)
            } else return application.resources.getColor(R.color.colorNaoPagoBackground, null)
    }
}