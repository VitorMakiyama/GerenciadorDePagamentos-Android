package com.makiyamasoftware.gerenciadordepagamentos.telas.detalhes

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.makiyamasoftware.gerenciadordepagamentos.R
import com.makiyamasoftware.gerenciadordepagamentos.convertStringToCalendar
import com.makiyamasoftware.gerenciadordepagamentos.database.PagamentosDatabase
import com.makiyamasoftware.gerenciadordepagamentos.databinding.FragmentDetalhesPagamentoBinding
import com.makiyamasoftware.gerenciadordepagamentos.precisaDeNovoHistorico
import com.makiyamasoftware.gerenciadordepagamentos.ui.theme.GerenciadorDePagamentosTheme
import java.util.Calendar

private const val TAG: String = "DetalhesPagamentoFrag"

/**
 * O fragment de detalhes, mostrará todas as informações do pagamento selecionado (clicado), através dos
 *  botões no menu é possível alterar os seus detalhes e salvar as alterações
 */
class DetalhesPagamentoFragment: Fragment() {
    private lateinit var detalhesPagamentoViewModel: DetalhesPagamentoViewModel
    lateinit var binding: FragmentDetalhesPagamentoBinding

    private var pagamentoOutdated = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_detalhes_pagamento, container, false)

        // Puxar do Bundle o Parcel e transformar ele de volta em um Pagamento
        // Usamos o !! (null-asserted) pq, caso nao haja um PagamentoSelecionado, algo esta muito errado e queremos ver um erro, embora em producao seja ideal tratar esse erro
        val pagamentoSelecionado = DetalhesPagamentoFragmentArgs.fromBundle(requireArguments()).pagamentoEscolhido

        // After the deprecation of toolbar and actionBar
        val actionBar = (requireActivity() as AppCompatActivity).supportActionBar
        actionBar?.setDisplayShowTitleEnabled(true)
        actionBar?.title = pagamentoSelecionado.titulo
        Log.i(TAG, "Titulo da actionbar depois: ${activity?.actionBar?.title}")

        // instanciar uma application p/ usar no ViewModelFactory
        val application = requireNotNull(this.activity).application
        // instancia o DAO do database, para podermos acessa-lo e alterar os dados
        val dataSource = PagamentosDatabase.getInstance(application).pagamentosDatabaseDao
        // instancia uma viewModelFactory
        val viewModelFactory = DetalhesPagamentoViewModelFactory(dataSource, application, pagamentoSelecionado)
        detalhesPagamentoViewModel = ViewModelProvider(this, viewModelFactory).get(DetalhesPagamentoViewModel::class.java)

        binding.viewModel = detalhesPagamentoViewModel
        binding.lifecycleOwner = viewLifecycleOwner

        detalhesPagamentoViewModel.historicosDoPagamento.observe(viewLifecycleOwner) {
            if (it.isNotEmpty()) {
                detalhesPagamentoViewModel.atualizarHistoricoRecente()
                verifyPagamentosUpdates()
            }
            Log.d(TAG, "LiveData historico mudou, size=${detalhesPagamentoViewModel.historicosDoPagamento.value!!.size}\n${detalhesPagamentoViewModel.historicosDoPagamento.value}")
        }
        detalhesPagamentoViewModel.historicoRecente.observe(viewLifecycleOwner) {
            if (it != null) detalhesPagamentoViewModel.atualizarPreco()
            Log.d(TAG, "historicoRecente.estaPago=${it?.estaPago}")
        }

        detalhesPagamentoViewModel.pessoas.observe(viewLifecycleOwner) {
            if (!it.isNullOrEmpty()) {
                detalhesPagamentoViewModel.atualizarHistoricoRecente()
            }
        }

        detalhesPagamentoViewModel.pagamento.observe(viewLifecycleOwner) {
            if (it != null) {
                verifyPagamentosUpdates()
            }
        }

        // Observer para navegar para a pagina com todos os HistoricosDePagamento do Pagamento
        detalhesPagamentoViewModel.verTodoOHistorico.observe(viewLifecycleOwner) {
            if (it) {
                findNavController().navigate(DetalhesPagamentoFragmentDirections.actionDetalhesPagamentoFragmentToHistoricosPagamentoFragment(detalhesPagamentoViewModel.pagamento.value!!))
                detalhesPagamentoViewModel.onVerTodoOHistoricoDone()
            }
        }

        detalhesPagamentoViewModel.hasDeletedPayment.observe(viewLifecycleOwner) {
            if (it) {
                Toast.makeText(context, application.resources.getString(R.string.detalhesPagamento_ToastText_deletePayment, detalhesPagamentoViewModel.pagamento.value?.titulo), Toast.LENGTH_LONG).show()
                findNavController().popBackStack()
            }
        }

        setHasOptionsMenu(true)
        binding.apply {
            composeView.apply {
				setViewCompositionStrategy(
					ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
				)
                setContent {
                    // Inside Compose world!
                    GerenciadorDePagamentosTheme {
                        DetalhesPagamentoScreen(detalhesPagamentoViewModel)
                    }
                }
            }
        }
        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.fragment_detalhes_pagamento_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.apagarButton -> {onClickApagarPagamento(); true}
            R.id.editarButton -> {onClickEditarPagamento(); true}
            else -> false
        }
    }

    private fun onClickApagarPagamento() {
        detalhesPagamentoViewModel.onShowAlertDialog(DetalhesPagamentoViewModel.AlertType.DELETE_PAYMENT)
    }

    private fun onClickEditarPagamento() {
        detalhesPagamentoViewModel.onEditingPayment()

    }

    /**
     * Verifica se o Pagamento esta com os Historicos desatualizados. Se estiver,
     *  gera um AlertDialogue perguntando para o usuario se ele quer atualiza-lo,
     *  gerando novos Historicos
     */
    private fun verifyPagamentosUpdates() {
        if (detalhesPagamentoViewModel.pagamento.isInitialized && detalhesPagamentoViewModel.historicosDoPagamento.isInitialized) {
            // Verifica se e necessario atualizar e criar novos historicos de pagamento
            if (precisaDeNovoHistorico(
                    detalhesPagamentoViewModel.pagamento.value!!.frequencia,
                    convertStringToCalendar(detalhesPagamentoViewModel.getHistoricoMaisRecente().data),
                    Calendar.getInstance(),
                    requireActivity().resources.getStringArray(R.array.frequencias_pagamentos)
                )
                and !pagamentoOutdated
            ) {
                detalhesPagamentoViewModel.onShowAlertDialog(DetalhesPagamentoViewModel.AlertType.UPDATE_PAYMENT)
                pagamentoOutdated = true
            }
        }
    }
}