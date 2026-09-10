package dev.passwrd.desktop.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.ln
import kotlin.math.min

/**
 * Estimación de fuerza deliberadamente simple (diversidad de clases de carácter x longitud,
 * NO zxcvbn): es una guía visual, no una decisión de seguridad — la resistencia real la da
 * Argon2id (ver docs/CRYPTO_SPEC.md), no esta heurística.
 */
private fun estimateBits(password: String): Double {
    if (password.isEmpty()) return 0.0
    var charsetSize = 0
    if (password.any { it.isLowerCase() }) charsetSize += 26
    if (password.any { it.isUpperCase() }) charsetSize += 26
    if (password.any { it.isDigit() }) charsetSize += 10
    if (password.any { !it.isLetterOrDigit() }) charsetSize += 32
    if (charsetSize == 0) return 0.0
    return password.length * (ln(charsetSize.toDouble()) / ln(2.0))
}

private const val MIN_LENGTH = 8

@Composable
fun CreateVaultScreen(onCreate: (ByteArray) -> Unit) {
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    val bits = estimateBits(password)
    val progress = min(1f, (bits / 80.0).toFloat())
    val strengthLabel = when {
        password.isEmpty() -> ""
        bits < 40 -> "Débil"
        bits < 60 -> "Aceptable"
        bits < 80 -> "Fuerte"
        else -> "Muy fuerte"
    }
    val passwordsMatch = password.isNotEmpty() && password == confirmPassword
    val canCreate = password.length >= MIN_LENGTH && passwordsMatch

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Crea tu contraseña maestra", style = MaterialTheme.typography.headlineSmall)
        Text(
            "No se guarda en ningún sitio y no hay forma de recuperarla si la olvidas. " +
                "Es la única llave de todo tu vault.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña maestra") },
            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
        if (strengthLabel.isNotEmpty()) {
            Text(strengthLabel, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 4.dp))
        }

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Repite la contraseña") },
            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        )
        if (confirmPassword.isNotEmpty() && !passwordsMatch) {
            Text(
                "Las contraseñas no coinciden",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        Button(
            onClick = { onCreate(password.encodeToByteArray()) },
            enabled = canCreate,
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp).align(Alignment.CenterHorizontally),
        ) {
            Text("Crear vault")
        }
    }
}
