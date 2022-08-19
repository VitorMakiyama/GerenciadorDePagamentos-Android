package com.makiyamasoftware.gerenciadordepagamentos.telas.inicio

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.core.view.size
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import androidx.navigation.fragment.findNavController
import com.makiyamasoftware.gerenciadordepagamentos.R
import com.makiyamasoftware.gerenciadordepagamentos.database.HistoricoDePagamento
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
                adapter.addESubmit(it)
            }
            for (i in it) {
                if (it.indexOf(i) >= viewModel.sizeHistorico) {
                    viewModel.getHistoricoNaoPagoAntigoOuUltimo(i.pagamentoID)
                    Log.i(TAG, "${it.indexOf(i)}>=${viewModel.sizeHistorico} Pediu historico ${it.indexOf(i)}\n")
                }
            }
            Log.i("Test 5","pagamento vazio? ${it.isNullOrEmpty()}")
            if (it.isNullOrEmpty()) {
                viewModel.textoVazioVisivel()
            } else viewModel.textoVazioInvisivel()
        })
        // Observer do historico (para que os historicos sejam atualizados quando forem obtidos)
        viewModel.historicoDosPagamentosState.observe(viewLifecycleOwner) { state ->
            if (state) {
                adapter.addESubmit(viewModel.pagamentos.value)
                for (i in viewModel.historicoDosPagamentos) {
                    i.observe(viewLifecycleOwner) {
                        adapter.addESubmit(viewModel.pagamentos.value)
                        if (viewModel.historicoDosPagamentos.indexOf(i) >= viewModel.sizePessoas) {
                            it.pagadorID.let { it1 -> viewModel.getPessoaDoHistorico(it1) }
                        }
                        Log.i(TAG, "historico ${viewModel.historicoDosPagamentos.indexOf(i)} ou ${viewModel.sizeHistorico - 1} == ${i} e size == ${viewModel.sizeHistorico}\n")
                        Log.i(TAG, "historicos ${viewModel.historicoDosPagamentos}")
                    }
                }
                viewModel.historicoStateDone()
            }
        }
        //
        viewModel.pessoasRecentesState.observe(viewLifecycleOwner) {
            if (it) {
                Log.i(TAG, "pessoas.size == ${viewModel.pessoasRecentes} e historicos.size == ${viewModel.historicoDosPagamentos.size}\n")
                for (i in viewModel.pessoasRecentes) {
                    i.observe(viewLifecycleOwner) {
                        adapter.addESubmit(viewModel.pagamentos.value)
                    }
                }
                Log.i(TAG, "entrou - pessoas.size != historicos.size")
                viewModel.pessoasStateDone()
            }
        }

        // setando a navegacao do FAB
        viewModel.navigateToCriarPagamento.observe(viewLifecycleOwner, Observer { //trocou this por ViewLifecycleOwner
            if (it) {
                this.findNavController().navigate(PagamentosMainFragmentDirections.actionPagamentosMainFragmentToCriarPagamentoFragment())
                //Toast.makeText(context, "FAB clicado", Toast.LENGTH_SHORT).show()
                viewModel.doneNavigating()
            }
        })
        // Setando a navegaçao do card do pagamento
        viewModel.clickNoPagamento.observe(viewLifecycleOwner) {
            if (it) {
                Toast.makeText(context, "${viewModel.getPagamentoCerto(viewModel.selectedPag)}\n\n${viewModel.getHistoricoCerto(viewModel.selectedPag)}", Toast.LENGTH_SHORT).show()
                findNavController().navigate(PagamentosMainFragmentDirections.actionPagamentosMainFragmentToDetalhesPagamentoFragment(viewModel.getPagamentoCerto(viewModel.selectedPag)!!))
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
        Log.i(TAG, "HISTORICOS ${viewModel.historicoDosPagamentos}\n" +
                "${viewModel.historicoDosPagamentos.first()?.value}\n${viewModel.historicoDosPagamentos.last()?.value}")
    }

    override fun onResume() {
        super.onResume()
        viewModel.reloadHistoricos()
    }
}