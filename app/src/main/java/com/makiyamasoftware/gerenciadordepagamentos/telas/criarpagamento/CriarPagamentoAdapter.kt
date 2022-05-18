package com.makiyamasoftware.gerenciadordepagamentos.telas.criarpagamento

import android.app.Activity
import android.provider.ContactsContract
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Adapter
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.makiyamasoftware.gerenciadordepagamentos.R
import com.makiyamasoftware.gerenciadordepagamentos.databinding.CriarParticipantesEdittextBinding
import com.makiyamasoftware.gerenciadordepagamentos.databinding.CriarParticipantesHeaderBinding
import com.makiyamasoftware.gerenciadordepagamentos.databinding.CriarParticipantesTailBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TIPO_ITEM_HEADER = 0
private const val TIPO_ITEM_PARTICIPANTE = 1
private const val TIPO_ITEM_TAIL = 2

private const val TAG = "CriarPagamentosAdapter"

class CriarPagamentoAdapter(private val criarPagamentoViewModel: CriarPagamentoViewModel, private val lifeCycleOwner: LifecycleOwner) : ListAdapter<DataItem, RecyclerView.ViewHolder>(CriarPagamentoDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TIPO_ITEM_HEADER -> HeaderViewHolder.from(parent)
            TIPO_ITEM_PARTICIPANTE -> ParticipantesViewHolder.from(parent)
            TIPO_ITEM_TAIL -> TailViewHolder.from(parent)
            else -> throw ClassCastException("CriarPagamentoAdapter: viewType desconhecido: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ParticipantesViewHolder -> {
                val item = getItem(position) as DataItem.ParticipanteItem
                holder.bind(item.participante)
            }
            is HeaderViewHolder -> {
                holder.bind(criarPagamentoViewModel, lifeCycleOwner)
            }
            is TailViewHolder -> {
                holder.bind(criarPagamentoViewModel, lifeCycleOwner)
            }
        }

    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is DataItem.Header -> TIPO_ITEM_HEADER
            is DataItem.ParticipanteItem -> TIPO_ITEM_PARTICIPANTE
            is DataItem.Tail -> TIPO_ITEM_TAIL
        }
    }

    /**
     * Para criar o Header ou aumentar a dicasParticipantes
     */
    private val adapterScope = CoroutineScope(Dispatchers.Default)
    fun addHeaderESubmit(participantes: List<ParticipanteAux>?) {
        adapterScope.launch {
                val itens = when (participantes) {
                    null -> listOf(DataItem.Header) + listOf(DataItem.Tail)
                    else -> listOf(DataItem.Header) + participantes.map { DataItem.ParticipanteItem(it) } + listOf(DataItem.Tail)
                }
                withContext(Dispatchers.Main) {
                    submitList(itens)
                }
            }

        }

    /**
     * Os ViewHolders utilizados
     */
    class ParticipantesViewHolder private  constructor(private val editBinding: CriarParticipantesEdittextBinding): RecyclerView.ViewHolder(editBinding.root) {
        fun bind(participante: ParticipanteAux) {
            editBinding.editTextParticipante.hint = participante.dica
            editBinding.editTextParcelaParticipante.hint = participante.dicaPreco
            if (participante.parcelaAparece) {
                 editBinding.editTextParcelaParticipante.visibility = View.VISIBLE
            } else {
                editBinding.editTextParcelaParticipante.visibility = View.GONE
            }
            editBinding.participanteAux = participante
            editBinding.executePendingBindings()
        }
        
        companion object {
            fun from(parent: ViewGroup): ParticipantesViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val editBinding = CriarParticipantesEdittextBinding.inflate(layoutInflater, parent, false)
                return ParticipantesViewHolder(editBinding)
            }
        }
    }
    class HeaderViewHolder private constructor(private val headerBinding: CriarParticipantesHeaderBinding): RecyclerView.ViewHolder(headerBinding.root) {
        fun bind(criarPagamentoViewModel: CriarPagamentoViewModel, lifeCycleOwner: LifecycleOwner) {
            headerBinding.criarPagamentoViewModel = criarPagamentoViewModel
            ArrayAdapter.createFromResource(itemView.context, R.array.frequencias_pagamentos, android.R.layout.simple_spinner_item).also {
                adapter ->
                adapter.setDropDownViewResource((android.R.layout.simple_spinner_dropdown_item))
                headerBinding.spinnerEscolhaFrequencia.adapter = adapter
            }

            criarPagamentoViewModel.spinnerEscolhaFrequencia = headerBinding.spinnerEscolhaFrequencia

            headerBinding.lifecycleOwner = lifeCycleOwner
            criarPagamentoViewModel.precoEnabled = headerBinding
            headerBinding.executePendingBindings()
        }
        companion object {
            fun from(parent: ViewGroup): HeaderViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val headerBinding = CriarParticipantesHeaderBinding.inflate(layoutInflater, parent, false)
                return HeaderViewHolder(headerBinding)
            }
        }
    }
    class TailViewHolder private constructor(private val tailBinding: CriarParticipantesTailBinding): RecyclerView.ViewHolder(tailBinding.root) {
        fun bind(criarPagamentoViewModel: CriarPagamentoViewModel, lifeCycleOwner: LifecycleOwner) {
            tailBinding.criarPagamentoViewModel = criarPagamentoViewModel
            tailBinding.lifecycleOwner = lifeCycleOwner
            tailBinding.executePendingBindings()
        }
        companion object {
            fun from(parent: ViewGroup): TailViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val tailBinding = CriarParticipantesTailBinding.inflate(layoutInflater, parent, false)
                return TailViewHolder(tailBinding)
            }
        }
    }
}

class SpinnerActivity : Activity(), AdapterView.OnItemSelectedListener {
    override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
        parent.getString(p2)
    }

    override fun onNothingSelected(p0: AdapterView<*>?) {
        TODO("Not yet implemented")
    }

}
/**
 * DataItem é uma classe abstrata e sealed (só pode ter filhos definidos nesse arquivo) que contem
 *  Header, participantes e o tail (botao para adicionar)
 */
sealed class DataItem {
    data class ParticipanteItem(val participante: ParticipanteAux): DataItem() {
        override val id: Long = participante.id
    }

    object Header: DataItem() {
        override val id: Long = Long.MIN_VALUE
    }

    object  Tail: DataItem() {
        override val id: Long = Long.MIN_VALUE + 1
    }
    abstract val id: Long
}

/**
 * DiffCallback para calcular a != entre dois !null itens na lista.
 *
 * Usado pelo ListAdapter para calcular o minimo numero de mudancas entre a lista antiga e a
 * nova, passada pelo submitList
 */
class  CriarPagamentoDiffCallback : DiffUtil.ItemCallback<DataItem>() {
    override fun areItemsTheSame(oldItem: DataItem, newItem: DataItem): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: DataItem, newItem: DataItem): Boolean {
        var equal: Boolean = true
        if (oldItem is DataItem.ParticipanteItem && newItem is DataItem.ParticipanteItem) {
            equal = (oldItem.participante.parcelaAparece == newItem.participante.parcelaAparece)
            Log.i(TAG,"equal: ${equal}\nold ${oldItem.participante.id}: ${oldItem.participante.parcelaAparece}\n" +
                    "new ${newItem.participante.id}: ${oldItem.participante.parcelaAparece}")
        }
        return oldItem == newItem && equal
    }

}