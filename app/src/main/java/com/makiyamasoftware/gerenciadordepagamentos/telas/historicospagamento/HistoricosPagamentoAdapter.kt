package com.makiyamasoftware.gerenciadordepagamentos.telas.historicospagamento

import android.app.Application
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.makiyamasoftware.gerenciadordepagamentos.database.HistoricoDePagamento
import com.makiyamasoftware.gerenciadordepagamentos.databinding.HistoricosItemListaBinding
import com.makiyamasoftware.gerenciadordepagamentos.getPessoaCerta

class HistoricosPagamentoAdapter(private val viewModel: HistoricosPagamentoViewModel): ListAdapter<HistoricoDePagamento, RecyclerView.ViewHolder>(HistoricosDiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return HistoricoViewHolder.from(parent)
    }

    override fun getItemCount(): Int {
        return viewModel.historicos.value?.size ?: 0
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is HistoricoViewHolder -> holder.bind(viewModel.getHistoricoAt(position), viewModel.pagamentoSelecionado.freqDoPag, viewModel)
        }
    }

    class HistoricoViewHolder(val binding: HistoricosItemListaBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(historico: HistoricoDePagamento, freq: String, viewModel: HistoricosPagamentoViewModel) {
            binding.textNomeHist.text = getPessoaCerta(viewModel.pessoas.value, historico.pagadorID).nome
            binding.textPrecoHist.text = historico.preco.toString()
            binding.textDataHist.text = historico.getDataString(viewModel.app, freq)
            binding.textStatusHist.text = historico.getEstaPagoString(viewModel.app)
            binding.backgroungHist.setBackgroundColor(historico.getBackgroundColorInt(viewModel.app))
        }

        companion object {
            fun from(parent: ViewGroup): HistoricoViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val historicoBinding = HistoricosItemListaBinding.inflate(layoutInflater, parent, false)
                return HistoricoViewHolder(historicoBinding)
            }
        }
    }
}

/**
 * DiffCallback para calcular a != entre dois !null itens na lista.
 *
 * Usado pelo ListAdapter para calcular o minimo numero de mudancas entre a lista antiga e a
 * nova, passada pelo submitList
 */
private class HistoricosDiffCallback : DiffUtil.ItemCallback<HistoricoDePagamento>() {
    override fun areItemsTheSame(
        oldItem: HistoricoDePagamento,
        newItem: HistoricoDePagamento
    ): Boolean {
        return oldItem.historicoID == newItem.historicoID
    }

    override fun areContentsTheSame(
        oldItem: HistoricoDePagamento,
        newItem: HistoricoDePagamento
    ): Boolean {
        return oldItem.estaPago == newItem.estaPago && oldItem.preco == newItem.preco
    }

}