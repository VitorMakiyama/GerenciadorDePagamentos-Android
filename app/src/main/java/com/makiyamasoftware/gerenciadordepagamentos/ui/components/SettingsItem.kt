package com.makiyamasoftware.gerenciadordepagamentos.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsItem(
    title: String,
    body: String,
    onClickItem: () -> Unit = {}
) {
    Box(modifier = Modifier
        .fillMaxWidth()
        .padding(all = 10.dp)
        .clickable(onClick = onClickItem)) {
        Column() {
            Text(
                text = title,
                style = typography.titleSmall
            )
            Text(
                text = body,
                style = typography.bodySmall
            )
        }
    }
}
