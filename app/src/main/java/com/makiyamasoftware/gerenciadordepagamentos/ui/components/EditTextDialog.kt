package com.makiyamasoftware.gerenciadordepagamentos.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.makiyamasoftware.gerenciadordepagamentos.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTextDialog(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    affirmativeText: String = stringResource(R.string.generic_Ok),
    onAffirmativeRequest: (String) -> Unit,
    dismissText: String = stringResource(R.string.generic_Cancelar),
    onDismissRequest: () -> Unit
) {
    val dialogModifier = Modifier.padding(dimensionResource(R.dimen.margin_normal))
    var editableText by remember { mutableStateOf<String>(value) }

    BasicAlertDialog(
        onDismissRequest = onDismissRequest,
    ) {
        Card(
            modifier = Modifier.widthIn(min = 280.dp, max = 560.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                //horizontalAlignment = Alignment.CenterHorizontally,
                modifier = dialogModifier
            ) {
                OutlinedTextField(
                    value = editableText,
                    label = { Text(text = title) },
                    onValueChange = { newText ->
                        editableText = newText
                    }
                )

                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Dismiss Button
                    TextButton(
                        onClick = onDismissRequest,
                        modifier = modifier.padding(horizontal = dimensionResource(R.dimen.margin_normal))
                    ) {
                        Text(dismissText)
                    }
                    // Affirmative Button
                    TextButton(
                        // Envia o texto editado para ser utilizado
                        onClick = { onAffirmativeRequest(editableText) },
                    ) {
                        Text(affirmativeText)
                    }
                }

            }
        }
    }
}