package dev.passwrd.desktop.ui.unlock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

/**
 * Sin PIN ni biometría: son atajos envueltos en Android Keystore (ver docs/CRYPTO_SPEC.md),
 * y desktop no tiene ese hardware. v1 de escritorio sólo desbloquea con contraseña maestra.
 */
@Composable
fun UnlockScreen(
    hasError: Boolean,
    onUnlock: (ByteArray) -> Unit,
    onErrorShown: () -> Unit,
) {
    var password by remember { mutableStateOf("") }

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
    }
}
