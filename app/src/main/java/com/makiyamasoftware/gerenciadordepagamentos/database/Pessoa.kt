package com.makiyamasoftware.gerenciadordepagamentos.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pessoas_table")
data class Pessoa(
        @PrimaryKey(autoGenerate = true)
        val pessoaId: Long = 0L,

        @ColumnInfo(name = "nome")
        val nome: String,

        @ColumnInfo(name = "ordem")
        val ordem: Int,

        @ColumnInfo(name = "pagamento_id")
        val pagamentoId: Long
)