package dev.passwrd.desktop.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import dev.passwrd.desktop.transfer.TransferViewModel

/** Sin PIN ni biometría (ver UnlockScreen.kt): sólo lock, cambio de contraseña y export/import. */
@Composable
fun SettingsScreen(
    transferViewModel: TransferViewModel,
    onBack: () -> Unit,
    onLock: () -> Unit,
    onChangeMasterPassword: (ByteArray, () -> Unit) -> Unit,
) {
    var showChangePasswordDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajustes") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            ListItem(
                headlineContent = { Text("Bloquear ahora") },
                supportingContent = { Text("Vuelve a pedir la contraseña maestra") },
            )
            Button(onClick = onLock, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Text("Bloquear ahora")
            }

            ListItem(
                headlineContent = { Text("Cambiar contraseña maestra") },
                supportingContent = { Text("No hay recuperación si la olvidas — apúntala en un sitio seguro") },
            )
            Button(
                onClick = { showChangePasswordDialog = true },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            ) {
                Text("Cambiar contraseña maestra")
            }

            ExportImportSection(transferViewModel)
        }
    }

    if (showChangePasswordDialog) {
        ChangeMasterPasswordDialog(
            onDismiss = { showChangePasswordDialog = false },
            onConfirm = { newPassword ->
                onChangeMasterPassword(newPassword) { showChangePasswordDialog = false }
            },
        )
    }
}

@Composable
private fun ChangeMasterPasswordDialog(onDismiss: () -> Unit, onConfirm: (ByteArray) -> Unit) {
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    val canConfirm = newPassword.length >= 8 && newPassword == confirmPassword

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva contraseña maestra") },
        text = {
            Column {
                OutlinedTextField(
                    newPassword,
                    { newPassword = it },
                    label = { Text("Nueva contraseña") },
                    visualTransformation = PasswordVisualTransformation(),
                )
                OutlinedTextField(
                    confirmPassword,
                    { confirmPassword = it },
                    label = { Text("Repite la contraseña") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(newPassword.encodeToByteArray()) }, enabled = canConfirm) {
                Text("Cambiar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}
