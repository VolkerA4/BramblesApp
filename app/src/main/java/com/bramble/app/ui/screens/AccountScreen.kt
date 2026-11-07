package com.bramble.app.ui.screens

import android.app.Application
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bramble.app.R
import com.bramble.app.data.ProfileRepository

class AccountViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = ProfileRepository(app)

    var name by mutableStateOf(repository.name())
        private set
    var pronouns by mutableStateOf(repository.pronouns())
        private set

    fun update(newName: String, newPronouns: String) {
        name = newName.trim()
        pronouns = newPronouns.trim()
        repository.save(name, pronouns)
    }
}

@Composable
fun AccountScreen(viewModel: AccountViewModel = viewModel()) {
    var editing by rememberSaveable { mutableStateOf(false) }

    if (editing) {
        EditProfileDialog(
            initialName = viewModel.name,
            initialPronouns = viewModel.pronouns,
            onSave = { name, pronouns ->
                viewModel.update(name, pronouns)
                editing = false
            },
            onDismiss = { editing = false },
        )
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(56.dp)) // room for the menu button in the corner
        IdCard(
            name = viewModel.name,
            pronouns = viewModel.pronouns,
            onEdit = { editing = true },
        )
    }
}

@Composable
private fun IdCard(name: String, pronouns: String, onEdit: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.raccoon),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(96.dp),
                )
                // End padding keeps long names from running under the pencil button.
                Column(Modifier.weight(1f).padding(start = 16.dp, end = 40.dp)) {
                    Text(name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    if (pronouns.isNotEmpty()) {
                        Text(
                            pronouns,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            IconButton(onClick = onEdit, modifier = Modifier.align(Alignment.TopEnd)) {
                Icon(painterResource(R.drawable.ic_edit), contentDescription = stringResource(R.string.profile_edit))
            }
        }
    }
}

@Composable
private fun EditProfileDialog(
    initialName: String,
    initialPronouns: String,
    onSave: (name: String, pronouns: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    var pronouns by rememberSaveable { mutableStateOf(initialPronouns) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.profile_edit)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.profile_name_label)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = pronouns,
                    onValueChange = { pronouns = it },
                    label = { Text(stringResource(R.string.profile_pronouns_label)) },
                    singleLine = true,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            // A name is required; pronouns can be left empty.
            TextButton(onClick = { onSave(name, pronouns) }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.profile_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.profile_cancel)) }
        },
    )
}
