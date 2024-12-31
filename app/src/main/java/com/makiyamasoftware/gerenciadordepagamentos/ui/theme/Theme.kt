package com.makiyamasoftware.gerenciadordepagamentos.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.colorResource
import com.makiyamasoftware.gerenciadordepagamentos.R

@Composable
fun GerenciadorDePagamentosTheme(
	darkTheme: Boolean = isSystemInDarkTheme(),
	content: @Composable () -> Unit
) {
	val customDarkColorScheme = darkColorScheme(
		primaryContainer = colorResource(R.color.colorAccent),
		secondaryContainer = colorResource(R.color.colorNaoPagoBackground)
	)
	val customLightColorScheme = lightColorScheme(
		primaryContainer = colorResource(R.color.colorAccent),
		secondaryContainer = colorResource(R.color.colorNaoPagoBackground)
	)

	MaterialTheme(
		content = content,
		colorScheme = if (darkTheme) customDarkColorScheme else customLightColorScheme
	)
}