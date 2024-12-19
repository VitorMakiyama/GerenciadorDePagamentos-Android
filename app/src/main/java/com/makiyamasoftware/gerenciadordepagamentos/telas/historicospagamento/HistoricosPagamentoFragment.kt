package com.makiyamasoftware.gerenciadordepagamentos.telas.historicospagamento

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.makiyamasoftware.gerenciadordepagamentos.R
import com.makiyamasoftware.gerenciadordepagamentos.database.PagamentosDatabase
import com.makiyamasoftware.gerenciadordepagamentos.databinding.FragmentHistoricosPagamentoBinding

class HistoricosPagamentoFragment : Fragment() {
    lateinit var viewModel: HistoricosPagamentoViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding : FragmentHistoricosPagamentoBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_historicos_pagamento ,container, false)

        // Puxar do Bundle o Parcel e transformar ele de volta em um Pagamento
        // Usamos o !! (null-asserted) pq, caso nao haja um PagamentoSelecionado, algo esta muito errado e queremos ver um erro, embora em producao seja ideal tratar esse erro
        val pagamentoSelecionado = HistoricosPagamentoFragmentArgs.fromBundle(requireArguments()).pagamentoEscolhido

        // After the deprecation of toolbar and actionBar
        val actionBar = (requireActivity() as AppCompatActivity).supportActionBar
        actionBar?.setDisplayShowTitleEnabled(true)
        actionBar?.title = pagamentoSelecionado.nome

        // instanciar uma application p/ usar no ViewModelFactory
        val application = requireNotNull(this.activity).application
        // instancia o DAO do database, para podermos acessa-lo e alterar os dados
        val dataSource = PagamentosDatabase.getInstance(application).pagamentosDatabaseDao
        // instancia uma viewModelFactory
        val viewModelFactory = HistoricosPagamentoViewModelFactory(dataSource, application, pagamentoSelecionado)
        viewModel = ViewModelProvider(this, viewModelFactory).get(HistoricosPagamentoViewModel::class.java)

        val adapter = HistoricosPagamentoAdapter(viewModel)

        binding.historicosList.adapter = adapter

        viewModel.historicos.observe(viewLifecycleOwner) {
            adapter.submitList(it)
            // Se o evento de Status for MULTIPLE, avisamos o range de mudança do item clicado até o fim do vetor
            //      se não, notifica a mudancxa apenas do item clicado
            if (viewModel.eventUpdateStatus.value == StatusChangeType.MULTIPLE) {
                adapter.notifyItemRangeChanged(
                    viewModel.historicoClicado, // index do item clicado
                    (viewModel.historicos.value?.size?.minus(1)) ?: 0 // o size do vetor, ou 0, caso ainda n esteja incializado
                )
            } else {
                adapter.notifyItemChanged(viewModel.historicoClicado)
            }
        }

        viewModel.pessoas.observe(viewLifecycleOwner) {
            adapter.submitList(viewModel.historicos.value)
        }

        viewModel.eventUpdateStatus.observe(viewLifecycleOwner) {
            onShowDialog(it)
        }

        return binding.root
    }

    private fun onShowDialog(type: StatusChangeType) {
        val mensagem: String = if (type == StatusChangeType.SINGULAR) {
            getString(R.string.historicosPagamentoFragment_status_alertMessage_unico) +
                    when (viewModel.getHistoricoClicado().estaPago) {
                        true -> getString(R.string.blocoEstaPago_status_naoPago)
                        else -> getString(R.string.blocoEstaPago_status_pago)
                    }
        } else {
            getString(R.string.historicosPagamentoFragment_status_alertMessage_multiplos)
        }

        // Criar um AlertDialog, definindo os botões, clickListeners e os textos
        val builder = AlertDialog.Builder(context)
        builder.setTitle(R.string.historicosPagamentoFragment_status_alertTitle)
        builder.setMessage(mensagem)
        builder.setPositiveButton(R.string.historicosPagamentoFragment_status_alert_button_opcaoUnico) { _, _ -> viewModel.onAtualizarStatus() }
        if (type == StatusChangeType.MULTIPLE) { builder.setNeutralButton(R.string.generic_Sim) { _, _ -> viewModel.onAtualizarStatus(); viewModel.onAtualizarMultiplosStatus()} }
        builder.setNegativeButton(R.string.generic_Nao) { _, _ ->}
        builder.show()

        Log.i(TAG, "HISTORICOS ${viewModel.historicos}\nType: ${type}\nClicado: ${viewModel.getHistoricoClicado()}")
    }
}