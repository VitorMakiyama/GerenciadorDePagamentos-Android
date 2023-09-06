package com.makiyamasoftware.gerenciadordepagamentos.telas.historicospagamento

import android.os.Bundle
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
        }

        viewModel.pessoas.observe(viewLifecycleOwner) {
            adapter.submitList(viewModel.historicos.value)
        }

        return binding.root
    }
}