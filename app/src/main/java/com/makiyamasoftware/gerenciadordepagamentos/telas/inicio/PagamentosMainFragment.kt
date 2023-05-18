package com.makiyamasoftware.gerenciadordepagamentos.telas.inicio

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.makiyamasoftware.gerenciadordepagamentos.R
import com.makiyamasoftware.gerenciadordepagamentos.database.PagamentosDatabase
import com.makiyamasoftware.gerenciadordepagamentos.databinding.FragmentPagamentosMainBinding

const val TAG = "PagamentosMainFragment"
/**
 * O fragment inical, mostrará todos os pagamentos, na forma de CardViews, o
 * floating button serve para adicionar novos pagamentos
 */
class PagamentosMainFragment : Fragment() {
    lateinit var viewModel: PagamentosMainViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                                savedInstanceState: Bundle?): View {
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
        val adapter = PagamentosMainAdapter(viewModel)
        binding.listaPagamentos.adapter = adapter

        // Observer da lista de pagamentos
        viewModel.pagamentos.observe(viewLifecycleOwner) {
            // Se houver uma lista (not null) dentro do livedata, faça:
            it?.let {
                adapter.addESubmit(it)
            }
            // Se a lista for null, mostre o texto de nenhum pagamento
            if (it.isNullOrEmpty()) {
                viewModel.textoVazioVisivel()
            } else viewModel.textoVazioInvisivel()
        }
        // Observer do historico (para que os historicos sejam atualizados quando forem obtidos)
        viewModel.historicoDosPagamentos.observe(viewLifecycleOwner, Observer {
            // Tudo com o historico so sera realizado caso a lista NAO SEJA NULL
            it?.let {
                viewModel.sizeHistorico = it.size

                // atualiza os dados no adapter, reenviando a lista de pagamentos
                adapter.addESubmit(viewModel.pagamentos.value)
                for (historico in it) {
                    historico.pagadorID.let { it1 -> viewModel.getPessoaDoHistoricoFromDB(it1) }
                    Log.i(TAG, "historico ${it.indexOf(historico)} de ${it.size} == ${historico} e size == ${viewModel.sizeHistorico}\n")
                }
                Log.i(TAG, "historicos ${it}")
            }
        })

        //
        viewModel.pessoasRecentesState.observe(viewLifecycleOwner, Observer {
            if (it) {
                Log.i(TAG, "pessoas.size == ${viewModel.pessoasRecentes} e historicos.size == ${viewModel.historicoDosPagamentos.value?.size}\n")
                for (i in viewModel.pessoasRecentes) {
                    i.observe(viewLifecycleOwner, Observer {
                        adapter.addESubmit(viewModel.pagamentos.value)
                    })
                }
                Log.i(TAG, "entrou - pessoas.size != historicos.size")
                viewModel.pessoasStateDone()
            }
        })

        // setando a navegacao do FAB
        viewModel.navigateToCriarPagamento.observe(viewLifecycleOwner, Observer { //trocou this por ViewLifecycleOwner
            if (it) {
                this.findNavController().navigate(PagamentosMainFragmentDirections.actionPagamentosMainFragmentToCriarPagamentoFragment())
                //Toast.makeText(context, "FAB clicado", Toast.LENGTH_SHORT).show()
                viewModel.doneNavigating()
            }
        })
        // Setando a navegaçao do card do pagamento
        viewModel.clickNoPagamento.observe(viewLifecycleOwner, Observer {
            if (it) {
                Toast.makeText(context, "${viewModel.getPagamentoCerto(viewModel.selectedPag)?.nome}  ${viewModel.getHistoricoCerto(viewModel.selectedPag)}", Toast.LENGTH_SHORT).show()
                findNavController().navigate(PagamentosMainFragmentDirections.actionPagamentosMainFragmentToDetalhesPagamentoFragment(viewModel.getPagamentoCerto(viewModel.selectedPag)!!))
                viewModel.onClickPagamentoDone()
            }
        })
        setHasOptionsMenu(true)
        return binding.root
    }
    // Override para o funcionamento do Options Menu
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.fragment_pagamentos_main_menu, menu)
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Log.i(TAG, "pessoas = ${viewModel.pessoasRecentes.size}")
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
        Log.i(TAG, "HISTORICOS ${viewModel.historicoDosPagamentos}\n" +
                "${viewModel.historicoDosPagamentos.value?.first()}\n${viewModel.historicoDosPagamentos.value?.last()}")
    }
}