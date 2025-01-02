package com.makiyamasoftware.gerenciadordepagamentos.database

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize


/**
 * Pagamento guarda as informacoes do pagamento,
 * Tem um id de pagamento unico
 */
@Parcelize
@Entity(tableName = "pagamento_table")
data class Pagamento(
	@PrimaryKey(autoGenerate = true)
	val id: Long = 0L,

	var titulo: String,

	@ColumnInfo(name = "data_de_inicio")
	val dataDeInicio: String,

	@ColumnInfo(name = "numero_de_pessoas")
	val numeroDePessoas: Int,

	val frequencia: String,

	@ColumnInfo(name = "auto_update_historico")
	var autoUpdateHistorico: Boolean = false,

	@ColumnInfo(name = "pode_enviar_push")
	var podeEnviarPush: Boolean = false,
) : Parcelable
