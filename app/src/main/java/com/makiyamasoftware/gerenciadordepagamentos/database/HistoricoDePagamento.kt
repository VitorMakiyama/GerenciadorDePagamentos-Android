package com.makiyamasoftware.gerenciadordepagamentos.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

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
        val estaPago: Boolean = false
)