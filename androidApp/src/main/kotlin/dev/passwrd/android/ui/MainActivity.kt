package dev.passwrd.android.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.passwrd.android.PasswrdApplication
import dev.passwrd.android.ui.detail.ItemEditScreen
import dev.passwrd.android.ui.detail.ItemEditViewModel
import dev.passwrd.android.ui.generator.GeneratorScreen
import dev.passwrd.android.ui.list.ItemListScreen
import dev.passwrd.android.ui.list.ItemListViewModel
import dev.passwrd.android.transfer.TransferViewModel
import dev.passwrd.android.ui.onboarding.CreateVaultScreen
import dev.passwrd.android.ui.settings.SettingsScreen
import dev.passwrd.android.ui.unlock.UnlockScreen

/** Navegación manual por estado — sin Navigation-Compose: la app tiene 5 pantallas, no hace falta. */
private sealed interface Screen {
    data object List : Screen
    data class Edit(val itemId: String?) : Screen
    data object Generator : Screen
    data object Settings : Screen
}

private const val AUTO_LOCK_TIMEOUT_MS = 2 * 60 * 1000L

// FragmentActivity (no ComponentActivity): BiometricPrompt aloja un Fragment interno para
// sobrevivir a cambios de configuración — ver docs/CRYPTO_SPEC.md "Desbloqueo biométrico".
class MainActivity : FragmentActivity() {
    private val container get() = (application as PasswrdApplication).container

    private val vaultViewModel: VaultViewModel by viewModels {
        VaultViewModel.factory(container.vaultSession)
    }

    private val autoLockHandler = Handler(Looper.getMainLooper())
    private val autoLockRunnable = Runnable { vaultViewModel.lock() }

    // Export/import lanza el selector de ficheros del sistema (SAF), que tapa la Activity y
    // dispara onStop() igual que si el usuario hubiera cambiado de app. Sin esta excepción el
    // vault se bloquearía a mitad del export, perdiendo la Vault Key antes de poder usarla al
    // volver — ver TransferSection.kt. Bitwarden y 1Password aplican la misma excepción: el
    // picker es un componente de confianza del sistema, no una app arbitraria, y la acción la
    // inició un usuario ya autenticado.
    private var suppressNextStopLock = false

    fun suppressNextAutoLock() {
        suppressNextStopLock = true
    }

    // Auto-lock al apagarse la pantalla — ver docs/CRYPTO_SPEC.md "Higiene de memoria".
    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            vaultViewModel.lock()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Sin capturas ni miniatura en recientes — ver docs/THREAT_MODEL.md.
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        setContent {
            MaterialTheme {
                Surface {
                    val vaultState by vaultViewModel.state.collectAsState()
                    val unlockError by vaultViewModel.unlockError.collectAsState()

                    when (vaultState) {
                        VaultUiState.Loading -> Unit
                        VaultUiState.NeedsOnboarding -> CreateVaultScreen(onCreate = vaultViewModel::createVault)
                        VaultUiState.Locked -> UnlockScreen(
                            activity = this@MainActivity,
                            pinUnlockManager = container.pinUnlockManager,
                            biometricUnlockManager = container.biometricUnlockManager,
                            hasError = unlockError,
                            onUnlock = vaultViewModel::unlock,
                            onErrorShown = vaultViewModel::clearUnlockError,
                            onVaultKeyRecovered = vaultViewModel::unlockWithRecoveredVaultKey,
                        )
                        VaultUiState.Unlocked -> {
                            var screen by remember { mutableStateOf<Screen>(Screen.List) }
                            when (val current = screen) {
                                is Screen.List -> {
                                    val listViewModel: ItemListViewModel = viewModel(
                                        factory = ItemListViewModel.factory(container.vaultRepository),
                                    )
                                    ItemListScreen(
                                        viewModel = listViewModel,
                                        onAddItem = { screen = Screen.Edit(null) },
                                        onOpenItem = { id -> screen = Screen.Edit(id) },
                                        onOpenGenerator = { screen = Screen.Generator },
                                        onOpenSettings = { screen = Screen.Settings },
                                    )
                                }
                                is Screen.Edit -> {
                                    val editViewModel: ItemEditViewModel = viewModel(
                                        factory = ItemEditViewModel.factory(container.vaultRepository, current.itemId),
                                    )
                                    ItemEditScreen(
                                        viewModel = editViewModel,
                                        isNewItem = current.itemId == null,
                                        onDone = { screen = Screen.List },
                                    )
                                }
                                is Screen.Generator -> GeneratorScreen(onBack = { screen = Screen.List })
                                is Screen.Settings -> {
                                    val transferViewModel: TransferViewModel = viewModel(
                                        factory = TransferViewModel.factory(container.vaultRepository),
                                    )
                                    SettingsScreen(
                                        activity = this@MainActivity,
                                        pinUnlockManager = container.pinUnlockManager,
                                        biometricUnlockManager = container.biometricUnlockManager,
                                        transferViewModel = transferViewModel,
                                        onLaunchingFilePicker = ::suppressNextAutoLock,
                                        currentVaultKey = { container.vaultSession.requireVaultKey() },
                                        onBack = { screen = Screen.List },
                                        onLock = {
                                            vaultViewModel.lock()
                                            screen = Screen.List
                                        },
                                        onChangeMasterPassword = vaultViewModel::changeMasterPassword,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        registerReceiver(screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))
        scheduleAutoLock()
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(screenOffReceiver)
        autoLockHandler.removeCallbacks(autoLockRunnable)
        if (suppressNextStopLock) {
            suppressNextStopLock = false
        } else {
            // Al pasar a segundo plano, bloqueo inmediato — ver docs/CRYPTO_SPEC.md.
            vaultViewModel.lock()
        }
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        scheduleAutoLock()
    }

    private fun scheduleAutoLock() {
        autoLockHandler.removeCallbacks(autoLockRunnable)
        autoLockHandler.postDelayed(autoLockRunnable, AUTO_LOCK_TIMEOUT_MS)
    }
}
