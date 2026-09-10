package dev.passwrd.desktop.ui.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.passwrd.core.model.ItemPayload
import dev.passwrd.core.model.VaultItem

@Composable
fun ItemListScreen(
    viewModel: ItemListViewModel,
    onAddItem: () -> Unit,
    onOpenItem: (String) -> Unit,
    onOpenGenerator: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val items by viewModel.items.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Passwrd") },
                actions = {
                    IconButton(onClick = onOpenGenerator) {
                        Icon(Icons.Default.Refresh, contentDescription = "Generador de contraseñas")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Ajustes")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddItem) {
                Icon(Icons.Default.Add, contentDescription = "Añadir item")
            }
        },
    ) { padding ->
        if (items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Vault vacío", style = MaterialTheme.typography.titleMedium)
                    Text("Toca + para añadir tu primera credencial", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(items, key = { it.id }) { item ->
                    VaultItemRow(item, onClick = { onOpenItem(item.id) })
                }
            }
        }
    }
}

@Composable
private fun VaultItemRow(item: VaultItem, onClick: () -> Unit) {
    val (title, subtitle) = when (val payload = item.payload) {
        is ItemPayload.Login -> payload.name to payload.username
        is ItemPayload.SecureNote -> payload.name to "Nota segura"
        is ItemPayload.Card -> payload.name to "Tarjeta"
        is ItemPayload.Identity -> payload.name to "Identidad"
        is ItemPayload.Passkey -> payload.name to "Passkey · ${payload.userName}"
    }
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        modifier = Modifier.clickable(onClick = onClick),
    )
}
