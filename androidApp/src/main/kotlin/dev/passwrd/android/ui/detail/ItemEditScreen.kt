package dev.passwrd.android.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.passwrd.core.generator.PasswordGenerator
import dev.passwrd.core.generator.PasswordPolicy
import dev.passwrd.core.model.ItemPayload
import dev.passwrd.core.model.ItemType

@Composable
fun ItemEditScreen(viewModel: ItemEditViewModel, isNewItem: Boolean, onDone: () -> Unit) {
    val uiState by viewModel.state.collectAsState()

    when (val state = uiState) {
        is ItemEditUiState.Loading -> Unit
        is ItemEditUiState.Ready -> {
            var selectedType by remember {
                mutableStateOf(state.item?.type ?: ItemType.LOGIN)
            }

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(if (isNewItem) "Nuevo item" else "Editar item") },
                        navigationIcon = {
                            IconButton(onClick = onDone) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                            }
                        },
                        actions = {
                            if (!isNewItem) {
                                IconButton(onClick = { viewModel.delete(onDone) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Eliminar")
                                }
                            }
                        },
                    )
                },
            ) { padding ->
                Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                    if (isNewItem) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = selectedType == ItemType.LOGIN,
                                onClick = { selectedType = ItemType.LOGIN },
                                label = { Text("Login") },
                            )
                            FilterChip(
                                selected = selectedType == ItemType.SECURE_NOTE,
                                onClick = { selectedType = ItemType.SECURE_NOTE },
                                label = { Text("Nota segura") },
                            )
                        }
                    }

                    when (selectedType) {
                        ItemType.LOGIN -> LoginForm(
                            initial = state.item?.payload as? ItemPayload.Login,
                            onSave = { payload -> viewModel.save(ItemType.LOGIN, payload, onDone) },
                        )
                        ItemType.SECURE_NOTE -> SecureNoteForm(
                            initial = state.item?.payload as? ItemPayload.SecureNote,
                            onSave = { payload -> viewModel.save(ItemType.SECURE_NOTE, payload, onDone) },
                        )
                        else -> Text("Este tipo de item todavía no tiene editor en la UI.")
                    }
                }
            }
        }
    }
}

@Composable
private fun LoginForm(initial: ItemPayload.Login?, onSave: (ItemPayload.Login) -> Unit) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var username by remember { mutableStateOf(initial?.username ?: "") }
    var password by remember { mutableStateOf(initial?.password ?: "") }
    var uri by remember { mutableStateOf(initial?.uris?.firstOrNull() ?: "") }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }

    Column(modifier = Modifier.padding(top = 16.dp)) {
        OutlinedTextField(name, { name = it }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(
            username, { username = it }, label = { Text("Usuario / email") },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
        Row(
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) {
            OutlinedTextField(
                password, { password = it }, label = { Text("Contraseña") },
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { password = PasswordGenerator.generate(PasswordPolicy()) }) {
                Icon(Icons.Default.Refresh, contentDescription = "Generar contraseña")
            }
        }
        OutlinedTextField(
            uri, { uri = it }, label = { Text("Sitio web (https://...)") },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
        OutlinedTextField(
            notes, { notes = it }, label = { Text("Notas") },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )

        Button(
            onClick = {
                onSave(
                    ItemPayload.Login(
                        name = name,
                        username = username,
                        password = password,
                        uris = if (uri.isBlank()) emptyList() else listOf(uri),
                        notes = notes,
                        totpSecret = initial?.totpSecret,
                    ),
                )
            },
            enabled = name.isNotBlank(),
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        ) {
            Text("Guardar")
        }
    }
}

@Composable
private fun SecureNoteForm(initial: ItemPayload.SecureNote?, onSave: (ItemPayload.SecureNote) -> Unit) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var content by remember { mutableStateOf(initial?.content ?: "") }

    Column(modifier = Modifier.padding(top = 16.dp)) {
        OutlinedTextField(name, { name = it }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(
            content, { content = it }, label = { Text("Contenido") },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )

        Button(
            onClick = { onSave(ItemPayload.SecureNote(name = name, content = content)) },
            enabled = name.isNotBlank(),
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        ) {
            Text("Guardar")
        }
    }
}
