package dev.passwrd.desktop.ui.generator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.passwrd.core.generator.PasswordGenerator
import dev.passwrd.core.generator.PasswordPolicy
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

@Composable
fun GeneratorScreen(onBack: () -> Unit) {
    var length by remember { mutableStateOf(20f) }
    var useUpper by remember { mutableStateOf(true) }
    var useDigits by remember { mutableStateOf(true) }
    var useSymbols by remember { mutableStateOf(true) }
    var avoidAmbiguous by remember { mutableStateOf(true) }

    fun policy() = PasswordPolicy(
        length = length.toInt(),
        useLower = true,
        useUpper = useUpper,
        useDigits = useDigits,
        useSymbols = useSymbols,
        avoidAmbiguous = avoidAmbiguous,
    )

    var password by remember { mutableStateOf(PasswordGenerator.generate(policy())) }

    fun regenerate() {
        password = PasswordGenerator.generate(policy())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Generador") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    password,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(16.dp),
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) {
                OutlinedButton(onClick = { regenerate() }, modifier = Modifier.weight(1f)) {
                    Text("Regenerar")
                }
                Button(
                    onClick = {
                        // Sin flag de "sensible" en el portapapeles del sistema: eso es propiedad de Android 13+.
                        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(password), null)
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Copiar")
                }
            }

            Text("Longitud: ${length.toInt()}", modifier = Modifier.padding(top = 24.dp))
            Slider(
                value = length,
                onValueChange = { length = it; regenerate() },
                valueRange = 8f..64f,
                steps = 55,
            )

            ToggleRow("Mayúsculas", useUpper) { useUpper = it; regenerate() }
            ToggleRow("Números", useDigits) { useDigits = it; regenerate() }
            ToggleRow("Símbolos", useSymbols) { useSymbols = it; regenerate() }
            ToggleRow("Evitar caracteres ambiguos (0/O, 1/l/I)", avoidAmbiguous) { avoidAmbiguous = it; regenerate() }
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
