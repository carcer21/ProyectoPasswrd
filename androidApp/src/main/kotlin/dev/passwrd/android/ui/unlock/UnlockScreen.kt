package dev.passwrd.android.ui.unlock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import kotlinx.coroutines.launch

@Composable
fun UnlockScreen(
    activity: FragmentActivity,
    pinUnlockManager: PinUnlockManager,
    biometricUnlockManager: BiometricUnlockManager,
    hasError: Boolean,
    onUnlock: (ByteArray) -> Unit,
    onErrorShown: () -> Unit,
    onVaultKeyRecovered: (ByteArray) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var password by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }
    val biometricAvailable = biometricUnlockManager.isEnabled && biometricUnlockManager.isAvailable(activity)

    // Ofrecer biometría automáticamente al entrar a la pantalla — atajo esperado por el usuario.
    LaunchedEffect(biometricAvailable) {
        if (biometricAvailable) {
            biometricUnlockManager.unlock(activity) { vaultKey ->
                if (vaultKey != null) onVaultKeyRecovered(vaultKey)
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Passwrd bloqueado", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Introduce tu contraseña maestra",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
        )

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                if (hasError) onErrorShown()
            },
            label = { Text("Contraseña maestra") },
            visualTransformation = PasswordVisualTransformation(),
            isError = hasError,
            supportingText = if (hasError) {
                { Text("Contraseña incorrecta") }
            } else {
                null
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Button(
            onClick = { onUnlock(password.encodeToByteArray()) },
            enabled = password.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        ) {
            Text("Desbloquear")
        }

        if (pinUnlockManager.isEnabled) {
            OutlinedTextField(
                value = pin,
                onValueChange = {
                    pin = it
                    pinError = false
                },
                label = { Text("PIN") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                isError = pinError,
                supportingText = if (pinError) {
                    { Text("PIN incorrecto") }
                } else {
                    null
                },
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            )
            OutlinedButton(
                onClick = {
                    scope.launch {
                        val vaultKey = pinUnlockManager.tryUnlock(pin.encodeToByteArray())
                        if (vaultKey != null) {
                            onVaultKeyRecovered(vaultKey)
                        } else {
                            pinError = true
                        }
                    }
                },
                enabled = pin.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) {
                Text("Desbloquear con PIN")
            }
        }

        if (biometricAvailable) {
            OutlinedButton(
                onClick = {
                    biometricUnlockManager.unlock(activity) { vaultKey ->
                        if (vaultKey != null) onVaultKeyRecovered(vaultKey)
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) {
                Text("Usar biometría")
            }
        }
    }
}
