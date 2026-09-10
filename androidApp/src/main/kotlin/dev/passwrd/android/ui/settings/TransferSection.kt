package dev.passwrd.android.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import dev.passwrd.android.transfer.ImportFormat
import dev.passwrd.android.transfer.TransferViewModel
import kotlinx.coroutines.launch

/**
 * Fase 7 del plan: export/import cifrado propio (Argon2id + AES-256-GCM con contraseña
 * de export independiente de la maestra) e importadores de Bitwarden, Proton Pass y CSV
 * genérico. Lectura/escritura de ficheros vía Storage Access Framework — el usuario elige
 * dónde, sin permisos de almacenamiento de por vida.
 */
@Composable
fun ExportImportSection(
    activity: FragmentActivity,
    viewModel: TransferViewModel,
    onLaunchingFilePicker: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var statusMessage by remember { mutableStateOf<String?>(null) }

    var showExportPasswordDialog by remember { mutableStateOf(false) }
    var pendingExportPassword by remember { mutableStateOf<ByteArray?>(null) }

    var showImportPasswordDialog by remember { mutableStateOf(false) }
    var pendingEncryptedImportText by remember { mutableStateOf<String?>(null) }

    var showFormatDialog by remember { mutableStateOf(false) }
    var pendingImportFormat by remember { mutableStateOf<ImportFormat?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val password = pendingExportPassword
        pendingExportPassword = null
        if (uri == null || password == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                val json = viewModel.exportEncrypted(password)
                activity.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                    ?: error("no se pudo abrir el fichero de destino")
            }.onSuccess {
                statusMessage = "Vault exportado correctamente."
            }.onFailure { e ->
                statusMessage = "No se pudo exportar: ${e.message}"
            }
        }
    }

    val importEncryptedPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val text = activity.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        if (text == null) {
            statusMessage = "No se pudo leer el fichero."
        } else {
            pendingEncryptedImportText = text
            showImportPasswordDialog = true
        }
    }

    val importExternalPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val format = pendingImportFormat
        pendingImportFormat = null
        if (uri == null || format == null) return@rememberLauncherForActivityResult
        val text = activity.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        if (text == null) {
            statusMessage = "No se pudo leer el fichero."
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            runCatching { viewModel.importExternal(format, text) }
                .onSuccess { count -> statusMessage = "Importados $count elementos." }
                .onFailure { e -> statusMessage = "No se pudo importar: ${e.message}" }
        }
    }

    Column {
        ListItem(
            headlineContent = { Text("Exportar bóveda") },
            supportingContent = { Text("Fichero .json cifrado con una contraseña de export, independiente de la maestra") },
        )
        Button(
            onClick = { showExportPasswordDialog = true },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        ) {
            Text("Exportar")
        }

        ListItem(
            headlineContent = { Text("Importar bóveda") },
            supportingContent = { Text("Desde un fichero .json exportado con Passwrd") },
        )
        Button(
            onClick = {
                onLaunchingFilePicker()
                importEncryptedPicker.launch(arrayOf("application/json", "*/*"))
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        ) {
            Text("Importar")
        }

        ListItem(
            headlineContent = { Text("Importar desde otro gestor") },
            supportingContent = { Text("Bitwarden, Proton Pass o CSV genérico, sin cifrar") },
        )
        Button(
            onClick = { showFormatDialog = true },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        ) {
            Text("Importar desde...")
        }

        statusMessage?.let { message ->
            Text(message, modifier = Modifier.padding(16.dp))
        }
    }

    if (showExportPasswordDialog) {
        ExportPasswordDialog(
            onDismiss = { showExportPasswordDialog = false },
            onConfirm = { password ->
                pendingExportPassword = password
                showExportPasswordDialog = false
                onLaunchingFilePicker()
                exportLauncher.launch("passwrd-export.json")
            },
        )
    }

    if (showImportPasswordDialog) {
        ImportPasswordDialog(
            onDismiss = {
                showImportPasswordDialog = false
                pendingEncryptedImportText = null
            },
            onConfirm = { password ->
                val text = pendingEncryptedImportText
                pendingEncryptedImportText = null
                showImportPasswordDialog = false
                if (text != null) {
                    scope.launch {
                        runCatching { viewModel.importEncrypted(text, password) }
                            .onSuccess { count -> statusMessage = "Importados $count elementos." }
                            .onFailure { e -> statusMessage = "Contraseña incorrecta o fichero dañado: ${e.message}" }
                    }
                }
            },
        )
    }

    if (showFormatDialog) {
        ImportFormatDialog(
            onDismiss = { showFormatDialog = false },
            onSelect = { format ->
                pendingImportFormat = format
                showFormatDialog = false
                onLaunchingFilePicker()
                importExternalPicker.launch(arrayOf("*/*"))
            },
        )
    }
}

@Composable
private fun ExportPasswordDialog(onDismiss: () -> Unit, onConfirm: (ByteArray) -> Unit) {
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    val canConfirm = password.length >= 8 && password == confirmPassword

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Contraseña de export") },
        text = {
            Column {
                Text("No tiene por qué ser la contraseña maestra. Sin ella, el fichero exportado no se puede abrir.")
                OutlinedTextField(
                    password,
                    { password = it },
                    label = { Text("Contraseña") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.padding(top = 8.dp),
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
            TextButton(onClick = { onConfirm(password.encodeToByteArray()) }, enabled = canConfirm) {
                Text("Exportar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}

@Composable
private fun ImportPasswordDialog(onDismiss: () -> Unit, onConfirm: (ByteArray) -> Unit) {
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Contraseña de export") },
        text = {
            OutlinedTextField(
                password,
                { password = it },
                label = { Text("Contraseña") },
                visualTransformation = PasswordVisualTransformation(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(password.encodeToByteArray()) }, enabled = password.isNotEmpty()) {
                Text("Importar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}

@Composable
private fun ImportFormatDialog(onDismiss: () -> Unit, onSelect: (ImportFormat) -> Unit) {
    val options = listOf(
        ImportFormat.BITWARDEN to "Bitwarden (export JSON sin cifrar)",
        ImportFormat.PROTON_PASS to "Proton Pass (data.json sin cifrar)",
        ImportFormat.CSV to "CSV genérico (name/url/username/password/notes)",
    )
    var selected by remember { mutableStateOf(options.first().first) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Formato de origen") },
        text = {
            Column {
                options.forEach { (format, label) ->
                    ListItem(
                        headlineContent = { Text(label) },
                        leadingContent = {
                            RadioButton(selected = selected == format, onClick = { selected = format })
                        },
                        modifier = Modifier.fillMaxWidth().clickable { selected = format },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSelect(selected) }) { Text("Elegir fichero") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}
