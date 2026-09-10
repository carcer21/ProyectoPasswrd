package dev.passwrd.android.ui.settings

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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import dev.passwrd.android.platform.BiometricUnlockManager
import dev.passwrd.android.platform.PinUnlockManager
import dev.passwrd.android.transfer.TransferViewModel
import dev.passwrd.core.crypto.zeroize
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    activity: FragmentActivity,
    pinUnlockManager: PinUnlockManager,
    biometricUnlockManager: BiometricUnlockManager,
    transferViewModel: TransferViewModel,
    onLaunchingFilePicker: () -> Unit,
    currentVaultKey: () -> ByteArray,
    onBack: () -> Unit,
    onLock: () -> Unit,
    onChangeMasterPassword: (ByteArray, () -> Unit) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showPinSetupDialog by remember { mutableStateOf(false) }
    var pinEnabled by remember { mutableStateOf(pinUnlockManager.isEnabled) }
    var biometricEnabled by remember { mutableStateOf(biometricUnlockManager.isEnabled) }
    val biometricAvailable = biometricUnlockManager.isAvailable(activity)

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
                headlineContent = { Text("Desbloqueo por PIN") },
                supportingContent = { Text("Atajo rápido; la contraseña maestra sigue siendo necesaria tras 5 fallos") },
                trailingContent = {
                    Switch(
                        checked = pinEnabled,
                        onCheckedChange = { checked ->
                            if (checked) {
                                showPinSetupDialog = true
                            } else {
                                pinUnlockManager.disable()
                                pinEnabled = false
                            }
                        },
                    )
                },
            )

            ListItem(
                headlineContent = { Text("Desbloqueo biométrico") },
                supportingContent = {
                    Text(if (biometricAvailable) "Huella o cara del dispositivo" else "No disponible en este dispositivo")
                },
                trailingContent = {
                    Switch(
                        checked = biometricEnabled,
                        enabled = biometricAvailable,
                        onCheckedChange = { checked ->
                            if (checked) {
                                biometricUnlockManager.setup(activity, currentVaultKey()) { success ->
                                    biometricEnabled = success
                                }
                            } else {
                                biometricUnlockManager.disable()
                                biometricEnabled = false
                            }
                        },
                    )
                },
            )

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

            ExportImportSection(activity, transferViewModel, onLaunchingFilePicker)
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

    if (showPinSetupDialog) {
        PinSetupDialog(
            onDismiss = { showPinSetupDialog = false },
            onConfirm = { pin ->
                scope.launch {
                    pinUnlockManager.setup(pin, currentVaultKey())
                    pin.zeroize()
                    pinEnabled = true
                    showPinSetupDialog = false
                }
            },
        )
    }
}

@Composable
private fun PinSetupDialog(onDismiss: () -> Unit, onConfirm: (ByteArray) -> Unit) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    val canConfirm = pin.length in 4..8 && pin == confirmPin

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configurar PIN") },
        text = {
            Column {
                OutlinedTextField(
                    pin,
                    { pin = it.filter(Char::isDigit).take(8) },
                    label = { Text("PIN (4-8 dígitos)") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                )
                OutlinedTextField(
                    confirmPin,
                    { confirmPin = it.filter(Char::isDigit).take(8) },
                    label = { Text("Repite el PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(pin.encodeToByteArray()) }, enabled = canConfirm) {
                Text("Activar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
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
