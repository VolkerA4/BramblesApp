package com.bramble.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bramble.app.R
import com.bramble.app.ui.theme.BrambleDialog
import com.bramble.app.data.Backup
import com.bramble.app.data.BackupData
import com.bramble.app.data.ThemeSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    homeViewModel: TasksViewModel = viewModel(),
    accountViewModel: AccountViewModel = viewModel(),
) {
    // The system back button/gesture closes settings instead of leaving the app.
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    @StringRes var statusRes by rememberSaveable { mutableStateOf<Int?>(null) }
    var pendingRestore by remember { mutableStateOf<BackupData?>(null) }

    // The system file picker does the saving/opening, so the app needs no storage permission.
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val json = Backup.toJson(
            homeViewModel.items.toList(),
            accountViewModel.name,
            accountViewModel.pronouns,
            homeViewModel.spoonData,
        )
        scope.launch {
            val saved = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(json.toByteArray()) }
                }.isSuccess
            }
            statusRes = if (saved) R.string.backup_saved else R.string.backup_save_failed
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val data = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() }
                }.getOrNull()?.let(Backup::parse)
            }
            if (data == null) {
                statusRes = R.string.backup_read_failed
            } else {
                statusRes = null
                pendingRestore = data // wait for the user to confirm before overwriting anything
            }
        }
    }

    pendingRestore?.let { data ->
        BrambleDialog(
            onDismissRequest = { pendingRestore = null },
            title = { Text(stringResource(R.string.restore_title)) },
            text = { Text(stringResource(R.string.restore_message)) },
            confirmButton = {
                TextButton(onClick = {
                    homeViewModel.replaceAll(data.items, data.spoons)
                    accountViewModel.update(
                        data.name ?: accountViewModel.name,
                        data.pronouns ?: accountViewModel.pronouns,
                    )
                    statusRes = R.string.backup_restored
                    pendingRestore = null
                }) { Text(stringResource(R.string.restore_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestore = null }) { Text(stringResource(R.string.restore_cancel)) }
            },
        )
    }

    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp)) {
            IconButton(onClick = onBack) {
                Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.settings_back))
            }
            Text(stringResource(R.string.settings), style = MaterialTheme.typography.titleLarge)
        }

        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.appearance_title), style = MaterialTheme.typography.titleMedium)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 24.dp),
            ) {
                Text(
                    stringResource(R.string.dark_mode),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = ThemeSettings.darkMode ?: false,
                    onCheckedChange = { ThemeSettings.setDarkMode(context, it) },
                )
            }

            Text(stringResource(R.string.backup_title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.backup_description),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
            )
            Button(
                onClick = { exportLauncher.launch("bramble-backup.json") },
                shape = RoundedCornerShape(12.dp), // matches the task cards
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.backup_export)) }
            OutlinedButton(
                onClick = { importLauncher.launch(arrayOf("*/*")) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) { Text(stringResource(R.string.backup_import)) }

            statusRes?.let {
                Text(
                    stringResource(it),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
        }
    }
}
