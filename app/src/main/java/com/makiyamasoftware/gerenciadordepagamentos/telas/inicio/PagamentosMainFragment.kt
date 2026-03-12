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
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.makiyamasoftware.gerenciadordepagamentos.R
import com.makiyamasoftware.gerenciadordepagamentos.database.PagamentosDatabase
import com.makiyamasoftware.gerenciadordepagamentos.databinding.FragmentPagamentosMainBinding
import com.makiyamasoftware.gerenciadordepagamentos.ui.components.PagamentosNavigation
import com.makiyamasoftware.gerenciadordepagamentos.ui.theme.GerenciadorDePagamentosTheme

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
        viewModel = ViewModelProvider(this, viewModelFactory)[PagamentosMainViewModel::class.java]

        // setando o DataBinding
        binding.lifecycleOwner = this
        binding.pagamentosMainViewModel = viewModel

        // setando a navegacao do FAB TODO: DEPRECATE THIS
        viewModel.navigateToCriarPagamento.observe(viewLifecycleOwner,
            Observer { //trocou this por ViewLifecycleOwner
                if (it) {
                    this.findNavController()
                        .navigate(PagamentosMainFragmentDirections.actionPagamentosMainFragmentToCriarPagamentoFragment())
                    //Toast.makeText(context, "FAB clicado", Toast.LENGTH_SHORT).show()
                    viewModel.doneNavigating()
                }
            })

//        setHasOptionsMenu(true)
        binding.apply {
            composeView.apply {
                setViewCompositionStrategy(
                    ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
                )
                setContent {
                    // Inside Compose world!
                    GerenciadorDePagamentosTheme {
                        PagamentosNavigation(viewModel)
                    }
                }
            }
        }
        return binding.root
    }
    // Override para o funcionamento do Options Menu
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.fragment_pagamentos_main_menu, menu)
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Log.i(TAG, "pessoas = ${viewModel.latestPeople.value?.size}")
        return when (item.itemId) {
            R.id.clearButton -> {onClearButton(); true}
            else -> false
        }
    }
    private fun onClearButton() {
        // Criar um AlertDialog, definindo os botões, clickListeners e os textos
        val builder = AlertDialog.Builder(context)
        builder.setTitle(R.string.clear_aviso_titulo)
        builder.setMessage(R.string.clear_aviso_mensagem)
        builder.setPositiveButton(R.string.generic_Sim) { _, _ -> viewModel.onClearAll()}
        //builder.setNeutralButton() {_, _ -> _}
        builder.setNegativeButton(R.string.generic_Nao) { _, _ ->}
        builder.show()
        Log.i(TAG, "HISTORICOS ${viewModel.latestHistories}\n" +
                "${viewModel.latestHistories.value.first()}\n${viewModel.latestHistories.value.last()}")
    }
}