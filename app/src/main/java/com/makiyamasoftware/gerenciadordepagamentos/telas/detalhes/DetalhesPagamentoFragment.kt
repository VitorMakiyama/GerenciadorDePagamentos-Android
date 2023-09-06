package com.makiyamasoftware.gerenciadordepagamentos.telas.detalhes

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.makiyamasoftware.gerenciadordepagamentos.R
import com.makiyamasoftware.gerenciadordepagamentos.convertStringToCalendar
import com.makiyamasoftware.gerenciadordepagamentos.database.PagamentosDatabase
import com.makiyamasoftware.gerenciadordepagamentos.databinding.FragmentDetalhesPagamentoBinding
import com.makiyamasoftware.gerenciadordepagamentos.precisaDeNovoHistorico
import java.util.Calendar

private const val TAG: String = "DetalhesPagamentoFrag"
/**
 * O fragment de detalhes, mostrará todas as informações do pagamento selecionado (clicado), através dos
 *  botões no menu é possível alterar os seus detalhes e salvar as alterações
 */
class DetalhesPagamentoFragment: Fragment() {
    lateinit var viewModel: DetalhesPagamentoViewModel

    var pagamentoOutdated = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding : FragmentDetalhesPagamentoBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_detalhes_pagamento, container, false)

        // Puxar do Bundle o Parcel e transformar ele de volta em um Pagamento
        // Usamos o !! (null-asserted) pq, caso nao haja um PagamentoSelecionado, algo esta muito errado e queremos ver um erro, embora em producao seja ideal tratar esse erro
        val pagamentoSelecionado = DetalhesPagamentoFragmentArgs.fromBundle(requireArguments()).pagamentoEscolhido

        // After the deprecation of toolbar and actionBar
        val actionBar = (requireActivity() as AppCompatActivity).supportActionBar
        actionBar?.setDisplayShowTitleEnabled(true)
        actionBar?.title = pagamentoSelecionado.nome
        Log.i(TAG, "Titulo da actionbar depois: ${activity?.actionBar?.title}")

        // instanciar uma application p/ usar no ViewModelFactory
        val application = requireNotNull(this.activity).application
        // instancia o DAO do database, para podermos acessa-lo e alterar os dados
        val dataSource = PagamentosDatabase.getInstance(application).pagamentosDatabaseDao
        // instancia uma viewModelFactory
        val viewModelFactory = DetalhesPagamentoViewModelFactory(dataSource, application, pagamentoSelecionado)
        viewModel = ViewModelProvider(this, viewModelFactory).get(DetalhesPagamentoViewModel::class.java)

        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        viewModel.historicoDePagamento.observe(viewLifecycleOwner) {
            if (it.isNotEmpty()) {
                viewModel.atualizarPreco()
                viewModel.bindHistRecente(binding)
                verifyPagamentosUpdates()
            }
            Log.i(TAG, "LiveData mudou historico de paggId:${viewModel.pagamentoSelecionado.value!!.pagamentoID} e o historico e \n${viewModel.historicoDePagamento.value!!.size}")
        }

        viewModel.pessoas.observe(viewLifecycleOwner) {
            if (!it.isNullOrEmpty()) {
                viewModel.atualizarHistorico()
            }
        }

        // Observer do click listener do status do Pagamento, habilitando o evento para atualiza-lo
        viewModel.onMudarStatus.observe(viewLifecycleOwner) {
            if (it) {
                // Criar um AlertDialog, definindo os botões, clickLiseteners e os textos
                val builder = AlertDialog.Builder(context)
                builder.setTitle(getString(R.string.detalhesPagamentoFragment_status_alertTitle))
                builder.setMessage(getString(R.string.detalhesPagamentoFragment_status_alertMessage) +
                        when (viewModel.histRecente.estaPago) {
                            true -> getString(R.string.blocoEstaPago_status_naoPago)
                            else -> getString(R.string.blocoEstaPago_status_pago)
                })
                builder.setPositiveButton(getText(R.string.generic_Ok)) { _, _ -> viewModel.onMudarStatus()}
                builder.setNegativeButton(getText(R.string.generic_Nao)) { _, _ -> }
                builder.show()
                viewModel.onClickStatusHistoricoDone()
            }
        }

        // Observer para navegar para a pagina com todos os HistoricosDePagamento do Pagamento
        viewModel.verTodoOHistorico.observe(viewLifecycleOwner) {
            if (it) {
                Toast.makeText(context, "Clicou em ver todo o Historico!", Toast.LENGTH_LONG).show()
                findNavController().navigate(DetalhesPagamentoFragmentDirections.actionDetalhesPagamentoFragmentToHistoricosPagamentoFragment(viewModel.pagamentoSelecionado.value!!))
                viewModel.onVerTodoOHistoricoDone()
            }
        }

        return binding.root
    }

    /**
     * Verifica se o Pagamento esta com os Historicos desatualizados. Se estiver,
     *  gera um AlertDialogue perguntando para o usuario se ele quer atualiza-lo,
     *  gerando novos Historicos
     */
    fun verifyPagamentosUpdates() {
        // Verifica se e necessario atualizar e criar novos historicos de pagamento
        if (precisaDeNovoHistorico(
                viewModel.pagamentoSelecionado.value!!.freqDoPag,
                convertStringToCalendar(viewModel.getHistoricoMaisRecente().data),
                Calendar.getInstance(),
                requireActivity().resources.getStringArray(R.array.frequencias_pagamentos))
         and !pagamentoOutdated) {
            // Criar um AlertDialog, definindo os botões, clickLiseteners e os textos
            val builder = AlertDialog.Builder(context)
            builder.setTitle(getString(R.string.detalhesPagamentoFragment_update_historicos_alertTitle))
            builder.setMessage(getString(R.string.detalhesPagamentoFragment_update_historicos_alertMessage))
            builder.setPositiveButton(getText(R.string.generic_Update)) { _, _ -> viewModel.onUpdateHistoricosDoPagamento() }
            builder.setNegativeButton(getText(R.string.generic_Nao)) { _, _ -> }
            builder.show()

            pagamentoOutdated = true
        }
    }
}