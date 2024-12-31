package com.makiyamasoftware.gerenciadordepagamentos.database

import android.app.Application
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.makiyamasoftware.gerenciadordepagamentos.R
import com.makiyamasoftware.gerenciadordepagamentos.convertStringDateToStringDay
import com.makiyamasoftware.gerenciadordepagamentos.convertStringDateToStringMonth

@Entity(tableName = "historico_de_pagamento_table")
data class HistoricoDePagamento(
    @PrimaryKey(autoGenerate = true)
        val historicoID: Long = 0L,

    val data: String,

    var preco: Double,

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
    fun getDataString(freqPossiveis: Array<String>, freqDoPagamento: String): String {
            // freqDoPagamento == "Escolha a frequencia"
        return when (freqDoPagamento) {
            freqPossiveis[0] -> {
                throw Exception("Erro HistoricoDePagamento: frequencia invalida")
            }

            freqPossiveis[1] -> {
                convertStringDateToStringDay(data)
            }

            else -> {
                convertStringDateToStringMonth(data)
            }
        }
    }
    fun getEstaPagoString(application: Application): String {
        return if (estaPago) {
            application.getString(R.string.blocoEstaPago_status_pago)
        } else application.getString(R.string.blocoEstaPago_status_naoPago)
    }
    fun getBackgroundColorInt(application: Application): Int {
        return if (estaPago) {
            application.resources.getColor(R.color.colorPagoBackground, null)
        } else application.resources.getColor(R.color.colorNaoPagoBackground, null)
    }
}
