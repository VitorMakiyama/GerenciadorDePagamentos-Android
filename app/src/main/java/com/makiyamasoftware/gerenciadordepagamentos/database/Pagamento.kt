package com.makiyamasoftware.gerenciadordepagamentos.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Pagamento guarda as informacoes do pagamento,
 * Tem um pagamento_ID unico
 */
@Entity(tableName = "pagamento_table")
data class Pagamento(
        @PrimaryKey(autoGenerate = true)
        val pagamentoID: Long = 0L,

        @ColumnInfo(name = "nome")
        val nome: String,

        @ColumnInfo(name = "data_de_inicio_em_string")
        val dataDeInicio: String,

        @ColumnInfo(name = "numero_de_pessoas")
        val numPessoas: Int,

        @ColumnInfo(name = "frequencia_do_pagamento")
        val freqDoPag: String
        )