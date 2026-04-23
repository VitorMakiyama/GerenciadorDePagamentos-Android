package com.makiyamasoftware.gerenciadordepagamentos.settings

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.makiyamasoftware.gerenciadordepagamentos.R
import com.makiyamasoftware.gerenciadordepagamentos.ui.components.EditTextDialog
import com.makiyamasoftware.gerenciadordepagamentos.ui.components.SettingsItem
import com.makiyamasoftware.gerenciadordepagamentos.ui.theme.GerenciadorDePagamentosTheme

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = viewModel()) {
    val uiState = viewModel.uiState

    SettingsScreen(
        baseURL = viewModel.uiState.baseURL,
        showEditBaseURLDialog = viewModel::showEditBaseURLDialog,
        editBaseURL = uiState.editBaseURL,
        setNewBaseURL = viewModel::setNewBaseURL,
        dismissEditBaseURLDialog = viewModel::dismissEditBaseURLDialog
    )
}

@Composable
fun SettingsScreen(
    baseURL: String,
    showEditBaseURLDialog: () -> Unit,
    editBaseURL: Boolean,
    setNewBaseURL: (String) -> Unit,
    dismissEditBaseURLDialog: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier.padding(dimensionResource(R.dimen.margin_normal))
    ) {
        Text(
            text = stringResource(R.string.SettingScreen_Settings_Headline),
            style = typography.headlineMedium
        )
        SettingsItem(
            stringResource(R.string.SettingScreen_baseURL_title),
            baseURL,
            onClickItem = showEditBaseURLDialog
        )
        AppVersionText()

        if (editBaseURL) {
            EditTextDialog(
                modifier = Modifier,
                title = stringResource(R.string.SettingScreen_baseURL_title),
                value = baseURL,
                onAffirmativeRequest = setNewBaseURL,
                onDismissRequest = dismissEditBaseURLDialog
            )
        }
    }
}

@Composable
fun AppVersionText() {
    val context = LocalContext.current
    val pm = context.packageManager
    val pInfo = pm.getPackageInfo(context.packageName, 0)

    SettingsItem(
        title = "App Version",
        body = "Version: ${pInfo.versionName}\nCode: ${if (Build.VERSION.SDK_INT >= 28) pInfo.longVersionCode else pInfo.versionCode.toLong()}"
    )
}

@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun SettingsScreenPreview() {
    GerenciadorDePagamentosTheme {
        SettingsScreen(
            baseURL = "teste.com.br",
            showEditBaseURLDialog = {},
            editBaseURL = false,
            setNewBaseURL = { _ -> },
            dismissEditBaseURLDialog = {},
        )
    }
}
