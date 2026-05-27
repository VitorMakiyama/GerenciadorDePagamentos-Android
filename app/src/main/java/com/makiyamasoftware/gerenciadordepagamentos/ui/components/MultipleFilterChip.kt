package com.makiyamasoftware.gerenciadordepagamentos.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.makiyamasoftware.gerenciadordepagamentos.R
import com.makiyamasoftware.gerenciadordepagamentos.beautifyText

@Composable
fun MultipleFilterChip(
    chipLabels: List<String>,
    onClickChip: (String) -> Unit,
    selected: String
) {
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    ) {
        chipLabels.forEach { chipLabel ->
            FilterChip(
                onClick = {
                    onClickChip(chipLabel)
                },
                label = { Text(text = beautifyText(chipLabel)) },
                selected = selected == chipLabel,
                trailingIcon = if (selected == chipLabel) {
                    {
                        Icon(
                            painter = painterResource(R.drawable.check_24dp),
                            contentDescription = "Selected Chip",
                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                        )
                    }
                } else {
                    null
                },
                modifier = Modifier.padding(horizontal = 2.dp)
            )
        }
    }
}
