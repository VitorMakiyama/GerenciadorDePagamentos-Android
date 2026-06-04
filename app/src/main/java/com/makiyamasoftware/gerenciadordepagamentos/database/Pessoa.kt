package com.makiyamasoftware.gerenciadordepagamentos.database

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
@Entity(tableName = "pessoas_table")
data class Pessoa(
	@PrimaryKey(autoGenerate = true)
	val id: Long = 0L,

	@ColumnInfo(name = "nome")
	val nome: String,

	@ColumnInfo(name = "ordem")
	val ordem: Int,

	@ColumnInfo(name = "pagamento_id")
	val pagamentoId: Long
) : Parcelable