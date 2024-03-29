package com.makiyamasoftware.gerenciadordepagamentos.telas.criarpagamento

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.makiyamasoftware.gerenciadordepagamentos.R
import com.makiyamasoftware.gerenciadordepagamentos.database.PagamentosDatabase
import com.makiyamasoftware.gerenciadordepagamentos.databinding.FragmentCriarPagamentoBinding

private const val TAG = "CriarPagamentoFragment"
/**
 *  Fragmento responsavel por criar um novo pagamento
 */

class CriarPagamentoFragment : Fragment() {

    lateinit var viewModel: CriarPagamentoViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        val binding: FragmentCriarPagamentoBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_criar_pagamento , container, false)

        // instanciar uma application p/ usar no ViewModelFactory
        val application = requireNotNull(this.activity).application
        // instancia o DAO do database, para podermos acessa-lo e alterar os dados
        val dataSource = PagamentosDatabase.getInstance(application).pagamentosDatabaseDao
        // instancia uma viewModelFactory
        val viewModelFactory = CriarPagamentoViewModelFactory(dataSource, application)
        // instancia uma ViewModel usando a Provider e a Factory
        viewModel = ViewModelProvider(this, viewModelFactory).get(CriarPagamentoViewModel::class.java)

        binding.criarPagamentoViewModel = viewModel

        /**
         * Criar se settar o adapter do RecyclerView
         */
        val adapter = CriarPagamentoAdapter(viewModel, viewLifecycleOwner)
        binding.criarList.adapter = adapter
        binding.lifecycleOwner = this

        viewModel.criarNovoPagamentoEvent.observe(viewLifecycleOwner, Observer {
            if (it == true) {
                Toast.makeText(context, criarTextoToast(viewModel),Toast.LENGTH_SHORT).show()
                this.findNavController().navigate(CriarPagamentoFragmentDirections.actionCriarPagamentoFragmentToPagamentosMainFragment())
                viewModel.onCriarNovoPagamentoDone()
            }
        })

        /**
         * Observer do DatePicker
         */
        viewModel.escolherDataInicalEvent.observe(viewLifecycleOwner, Observer {
            if (it == true) {
                showDatePickerDialog(viewModel)
                viewModel.onEscolherDataInicialDone()
            }
        })

        // Obersever da lista de participantes, quando muda informa a nova lista para o adapter, assim o RecyclerView pode atualiza-la
        viewModel.participantes.observe(viewLifecycleOwner, Observer {
            it?.let {
                adapter.addHeaderESubmit(it)
            }
            Log.i(TAG,"Enviou a lista")
            // setar o observer do novo participante (mesma lógica dos observer das parcelas)
            if (!(it.last().preco.hasObservers())) {
                Log.i(TAG,"Setou o observer do preco ${viewModel.participantes.value!!.last().id}")
                it.last().preco.observe(viewLifecycleOwner) {
                    Log.i(TAG,"Observer do preco ${viewModel.participantes.value!!.last().id}")
                    var soma = 0.0
                    if (viewModel.participantes.value!!.first().parcelaAparece) {
                        for (i in viewModel.participantes.value!!) {
                            if (i.preco.value?.toDoubleOrNull() != null) {
                                Log.i(TAG, "entrou aqui")
                                soma += i.preco.value!!.toDouble()
                                Log.i(TAG, "passou daqui")
                            }
                        }
                        viewModel.preco.value = soma.toString()
                    }
                }
            }
            binding.criarList.smoothScrollToPosition(adapter.itemCount)
        })

        /**
         * Observer do camposVazios
         */
        viewModel.camposVazios.observe(viewLifecycleOwner) {
            if (it == true) {
                // Criar um AlertDialog, definindo os botões, clickLiseteners e os textos
                val builder = AlertDialog.Builder(context)
                builder.setTitle(getString(R.string.criarPagamentosFragment_erro_campos_vazios_title))
                builder.setMessage(getString(R.string.criarPagamentosFragment_erro_campos_vazios_message_header) + viewModel.isAlgoNaoPreenchido())
                builder.setPositiveButton(getText(R.string.generic_Ok)) { _, wich -> }
                builder.show()
                viewModel.onCamposVaziosDone()
            }
        }

        /**
         * Observer das parcelas dos participantes iniciais (para alterar o preco do Pagamento de forma live)
         *  para isso, para cada detecção de alteraçao da LiveData fazemos a soma de todos as parcelas
         *  de participantes, que foram preenchidas.
         */
        for (i in viewModel.participantes.value!!) {
            i.preco.observe(viewLifecycleOwner) {
                var soma = 0.0
                if (viewModel.participantes.value!!.first().parcelaAparece) {
                    for (j in viewModel.participantes.value!!) {
                        if (j.preco.value?.toDoubleOrNull() != null) {
                            Log.i(TAG, "Preco parcial: ${j.preco.value}\n")
                            soma += j.preco.value!!.toDouble()
                        }
                    }
                    Log.i(TAG, "Soma Parcelas: ${soma}")
                    viewModel.preco.value = soma.toString()
                }
            }
        }

        Log.i("TestCriar","Datainicial: ${viewModel.dataInicialString.value}")
        return binding.root
    }
    // TODO "deletar essa funcao aux
    private fun criarTextoToast(viewModel: CriarPagamentoViewModel): String {
        val range = viewModel.participantes.value?.size ?: 0
        var string = ""
        for(i in 0 until range) {
            string += "${viewModel.participantes.value?.get(i)?.id ?:-1}º Participante: ${viewModel.participantes.value?.get(i)?.nome?.value}\n"
        }
        return "Pagamento : ${viewModel.nomePagamento.value}\n${viewModel.dataInicialString.value}\n${viewModel.spinnerEscolhaFrequencia.selectedItem}\n${viewModel.preco.value}\n" + string
    }

    fun showDatePickerDialog(viewModel: CriarPagamentoViewModel) {
        val newFragment = DatePickerFragment(viewModel)
        newFragment.show(parentFragmentManager, "datePicker")
    }
}
