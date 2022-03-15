package com.makiyamasoftware.gerenciadordepagamentos.telas.inicio

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.core.view.size
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.makiyamasoftware.gerenciadordepagamentos.R
import com.makiyamasoftware.gerenciadordepagamentos.database.PagamentosDatabase
import com.makiyamasoftware.gerenciadordepagamentos.databinding.FragmentPagamentosMainBinding

/**
 * O fragment inical, mostrará todos os pagamentos, na forma de CardViews, o
 * floating button serve para adicionar novos pagamentos
 */
class PagamentosMainFragment : Fragment() {
    lateinit var viewModel: PagamentosMainViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                                savedInstanceState: Bundle?): View? {
        val binding = FragmentPagamentosMainBinding.inflate(inflater, container, false)

        // instanciar uma application p/ usar no ViewModelFactory
        val application = requireNotNull(this.activity).application
        // instancia o DAO do database, para podermos acessa-lo e alterar os dados
        val dataSource = PagamentosDatabase.getInstance(application).pagamentosDatabaseDao
        // instancia uma viewModelFactory
        val viewModelFactory = PagamentosMainViewModelFactory(dataSource, application)
        // instancia uma ViewModel usando a Provider e a Factory
        viewModel = ViewModelProvider(this, viewModelFactory).get(PagamentosMainViewModel::class.java)

        // setando o DataBinding
        binding.lifecycleOwner = this
        binding.pagamentosMainViewModel = viewModel

        // criando e setando o adapter
        val adapter = PagamentosMainAdapter(viewModel, this.viewLifecycleOwner)
        binding.listaPagamentos.adapter = adapter

        // Observer da lista de pagamentos
        viewModel.pagamentos.observe(viewLifecycleOwner, Observer {
            it?.let {
                adapter.addESubmit(it, historico = viewModel.historicoDosPagamentos.value)
            }
            for (i in it) {
                viewModel.getHistoricoNaoPagoAntigoOuUltimo(i.pagamentoID)
            }
            Log.i("Test 5","pagamet vazio? ${it.isNullOrEmpty()}")
            if (it.isNullOrEmpty()) {
                viewModel.textoVazioVisivel()
            } else viewModel.textoVazioInvisivel()
        })
        // Observer

        //setando a navegacao do FAB
        viewModel.navigateToCriarPagamento.observe(viewLifecycleOwner, Observer { //trocou this por ViewLifecycleOwner
            if (it) {
                this.findNavController().navigate(PagamentosMainFragmentDirections.actionPagamentosMainFragmentToCriarPagamentoFragment())
                //Toast.makeText(context, "FAB clicado", Toast.LENGTH_SHORT).show()
                viewModel.doneNavigating()
            }
        })
        // setando a navegacçao do card do pagamento
        //TODO fazer a navegacao
        viewModel.clickNoPagamento.observe(viewLifecycleOwner) {
            if (it) {
                Toast.makeText(context, "${viewModel.getPagamentoCerto(viewModel.selectedPag)}\n\n${viewModel.getHistoricoCerto(viewModel.selectedPag)}", Toast.LENGTH_SHORT).show()
                viewModel.onClickPagamentoDone()
            }
        }
        setHasOptionsMenu(true)
        return binding.root
    }
    // Override para o funcionamento do Options Menu
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.fragment_pagamentos_main_menu, menu)
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.clearButton -> {onClearButton(); true}
            else -> false
        }
    }
    private fun onClearButton() {
        // Criar um AlertDialog, definindo os botões, clickLiseteners e os textos
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Apagar o banco de dados")
        builder.setMessage("Você tem certeza que deseja apagar TODOS os dados do banco de dados? (Não pode ser desfeito)")
        builder.setPositiveButton("Sim") {_, wich -> viewModel.onClearAll()}
        builder.setNegativeButton("Não") {_, wich ->}
        builder.show()
    }
}