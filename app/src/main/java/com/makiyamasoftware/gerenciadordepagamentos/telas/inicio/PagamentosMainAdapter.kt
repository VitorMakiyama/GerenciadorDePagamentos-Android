package com.makiyamasoftware.gerenciadordepagamentos.telas.inicio

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.makiyamasoftware.gerenciadordepagamentos.R
import com.makiyamasoftware.gerenciadordepagamentos.database.HistoricoDePagamento
import com.makiyamasoftware.gerenciadordepagamentos.database.Pagamento
import com.makiyamasoftware.gerenciadordepagamentos.database.Pessoa
import com.makiyamasoftware.gerenciadordepagamentos.databinding.PagamentosListaBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TIPO_ITEM = 1

class PagamentosMainAdapter(private val viewModel: PagamentosMainViewModel): ListAdapter<DataItem, RecyclerView.ViewHolder>(PagamentoDiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TIPO_ITEM -> PagamentoViewHolder.from(parent)
            else -> throw ClassCastException("PagamentosMaindAdapter: viewType desconhecido $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is PagamentoViewHolder -> {
                val item = getItem(position) as DataItem.PagamentoItem
                holder.bind(item.pag, item.historico, item.getUltimoParticipante(), viewModel)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is DataItem.PagamentoItem -> TIPO_ITEM
        }
    }

    /**
     * Para criar o Header ou aumentar a dicasParticipantes
     */
    private val adapterScope = CoroutineScope(Dispatchers.Default)
    fun addESubmit(pagamentos: List<Pagamento>?) {
        adapterScope.launch {
            val itens = when (pagamentos) {
                null -> null
                else -> pagamentos.map { DataItem.PagamentoItem(it,
                    viewModel.getHistoricoCerto(it.pagamentoID),
                    viewModel.getPessoaCerta(it.pagamentoID)) }
            }
            withContext(Dispatchers.Main) {
                submitList(itens)
            }
        }
    }

    /**
    * ViewHolder utilizado
    * */
    class PagamentoViewHolder private constructor(private val editBinding: PagamentosListaBinding): RecyclerView.ViewHolder(editBinding.root){
        fun bind(pagamento: Pagamento, historico: HistoricoDePagamento?, ultimoParticipante: String, viewModel: PagamentosMainViewModel) {
            editBinding.pagamentos = pagamento
            editBinding.textUltimoParticipante.text = ultimoParticipante
            editBinding.executePendingBindings()
            Log.i("TestHistoricoBind", "Historico: $historico")
            if (historico == null) {
                editBinding.textMesPagamento.text = viewModel.application.getString(R.string.generic_caps_null)
                editBinding.textStatusPagamento.text = viewModel.application.getString(R.string.generic_caps_null)
            } else {
                editBinding.textMesPagamento.text = historico.getDataString(viewModel.application, pagamento.freqDoPag)
                editBinding.textStatusPagamento.text = historico.getEstaPagoString(viewModel.application)
                editBinding.backgroungPagamentoListas.setBackgroundColor(historico.getBackgroundColorInt(viewModel.application))
            }
            editBinding.pagamentosListaCardView.setOnClickListener { viewModel.onClickPagamento(pagamento.pagamentoID) }
        }
        companion object {
            fun from(parent: ViewGroup): PagamentoViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val editBinding = PagamentosListaBinding.inflate(layoutInflater, parent, false)
                return PagamentoViewHolder(editBinding)
            }
        }
    }
}

/**
 * DataItem é uma classe abstrata e sealed (só pode ter filhos definidos nesse arquivo) que contem
 *  Header e
 */
sealed class DataItem {
    abstract val id: Long
    abstract val pago: Boolean? // atributo do Historico para controlar se ele ja está disponivel
    abstract val nomePagador: String? // atributo do Historico para controlar se ele ja está disponivel
    data class PagamentoItem(val pag: Pagamento, val historico: HistoricoDePagamento?, val pessoa: Pessoa?): DataItem() {
        override val id: Long = pag.pagamentoID
        override val pago: Boolean?
            get() = historico?.estaPago
        override val nomePagador: String?
            get() = pessoa?.nome
        /**
         * Atributo para colocar o nome do ultimo participante que deve/fez o pgmt
         */
        //TODO ATUALIZAR PARA PEGAR  A PESSOA CERTA (USANDO O HISTORICO DE PGMT)
        fun getUltimoParticipante(): String {
            Log.i("PagamentosMainAdapter","Pessoa ${pessoa}")
            if (pessoa == null){
                return "NULL"
            }
            return pessoa.nome
        }

    }
}

/**
 * DiffCallback para calcular a != entre dois !null itens na lista.
 *
 * Usado pelo ListAdapter para calcular o minimo numero de mudancas entre a lista antiga e a
 * nova, passada pelo submitList
 */
class PagamentoDiffCallback: DiffUtil.ItemCallback<DataItem>() {
    override fun areItemsTheSame(oldItem: DataItem, newItem: DataItem): Boolean {
        Log.i(TAG,"OLD=${oldItem.pago} e NEW=${newItem.pago} return = ${oldItem.id == newItem.id && oldItem.pago == newItem.pago && oldItem.nomePagador == newItem.nomePagador}")
        return oldItem.id == newItem.id && oldItem.pago == newItem.pago && oldItem.nomePagador == newItem.nomePagador
    }

    override fun areContentsTheSame(oldItem: DataItem, newItem: DataItem): Boolean {
        return oldItem == newItem
    }

}