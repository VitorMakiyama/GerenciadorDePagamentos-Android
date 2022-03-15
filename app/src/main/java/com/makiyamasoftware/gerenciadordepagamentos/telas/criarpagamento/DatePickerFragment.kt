package com.makiyamasoftware.gerenciadordepagamentos.telas.criarpagamento

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.widget.DatePicker
import androidx.fragment.app.DialogFragment
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "DatePicker"

class DatePickerFragment(private val viewModel: CriarPagamentoViewModel) : DialogFragment(), DatePickerDialog.OnDateSetListener {

    override fun onCreateDialog(savedInstanceState: Bundle?): DatePickerDialog {
        Log.i(TAG, "oncreate chamado")

        // Usar a data atual como a default date
        val calendario = Calendar.getInstance()

        return DatePickerDialog(this.requireContext(), this, calendario.get(Calendar.YEAR), calendario.get(Calendar.MONTH), calendario.get(Calendar.DAY_OF_MONTH))
    }

    @SuppressLint("SimpleDateFormat")
    override fun onDateSet(p0: DatePicker?, p1: Int, p2: Int, p3: Int) {
        Log.i(TAG, "onDateSet chamado")
        val calendario = Calendar.getInstance()

        // Settar a data escolhida no calendario, formatar a data e passar para o viewModel
        calendario.set(p1, p2, p3)
        viewModel.onDataInicialEscolhida(calendario)
        Log.i(TAG, viewModel.dataInicialString.value!!)
    }
}