package dev.passwrd.desktop

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import dev.passwrd.core.crypto.zeroize
import dev.passwrd.desktop.di.DesktopContainer
import dev.passwrd.desktop.transfer.TransferViewModel
import dev.passwrd.desktop.ui.detail.ItemEditScreen
import dev.passwrd.desktop.ui.detail.ItemEditViewModel
import dev.passwrd.desktop.ui.generator.GeneratorScreen
import dev.passwrd.desktop.ui.list.ItemListScreen
import dev.passwrd.desktop.ui.list.ItemListViewModel
import dev.passwrd.desktop.ui.onboarding.CreateVaultScreen
import dev.passwrd.desktop.ui.settings.SettingsScreen
import dev.passwrd.desktop.ui.unlock.UnlockScreen
import kotlinx.coroutines.launch

private sealed interface VaultUiState {
    data object Loading : VaultUiState
    data object NeedsOnboarding : VaultUiState
    data object Locked : VaultUiState
    data object Unlocked : VaultUiState
}

/** Navegación manual por estado — mismo criterio que MainActivity de androidApp: pocas pantallas. */
private sealed interface Screen {
    data object List : Screen
    data class Edit(val itemId: String?) : Screen
    data object Generator : Screen
    data object Settings : Screen
}

fun main() = application {
    val container = remember { DesktopContainer() }
    Window(onCloseRequest = ::exitApplication, title = "Passwrd") {
        MaterialTheme {
            Surface {
                App(container)
            }
        }
    }
}

@Composable
private fun App(container: DesktopContainer) {
    val scope = rememberCoroutineScope()
    var vaultState by remember { mutableStateOf<VaultUiState>(VaultUiState.Loading) }
    var unlockError by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        vaultState = if (container.vaultSession.isInitialized()) VaultUiState.Locked else VaultUiState.NeedsOnboarding
    }

    when (vaultState) {
        VaultUiState.Loading -> Unit

        VaultUiState.NeedsOnboarding -> CreateVaultScreen(
            onCreate = { password ->
                scope.launch {
                    container.vaultSession.createVault(password)
                    password.zeroize()
                    vaultState = VaultUiState.Unlocked
                }
            },
        )

        VaultUiState.Locked -> UnlockScreen(
            hasError = unlockError,
            onUnlock = { password ->
                scope.launch {
                    val ok = container.vaultSession.unlock(password)
                    password.zeroize()
                    unlockError = !ok
                    if (ok) vaultState = VaultUiState.Unlocked
                }
            },
            onErrorShown = { unlockError = false },
        )

        VaultUiState.Unlocked -> {
            var screen by remember { mutableStateOf<Screen>(Screen.List) }
            when (val current = screen) {
                Screen.List -> {
                    val listViewModel = remember { ItemListViewModel(container.vaultRepository, scope) }
                    ItemListScreen(
                        viewModel = listViewModel,
                        onAddItem = { screen = Screen.Edit(null) },
                        onOpenItem = { id -> screen = Screen.Edit(id) },
                        onOpenGenerator = { screen = Screen.Generator },
                        onOpenSettings = { screen = Screen.Settings },
                    )
                }
                is Screen.Edit -> {
                    val editViewModel = remember(current.itemId) {
                        ItemEditViewModel(container.vaultRepository, current.itemId, scope)
                    }
                    ItemEditScreen(
                        viewModel = editViewModel,
                        isNewItem = current.itemId == null,
                        onDone = { screen = Screen.List },
                    )
                }
                Screen.Generator -> GeneratorScreen(onBack = { screen = Screen.List })
                Screen.Settings -> {
                    val transferViewModel = remember { TransferViewModel(container.vaultRepository) }
                    SettingsScreen(
                        transferViewModel = transferViewModel,
                        onBack = { screen = Screen.List },
                        onLock = {
                            container.vaultSession.lock()
                            vaultState = VaultUiState.Locked
                            screen = Screen.List
                        },
                        onChangeMasterPassword = { newPassword, onDone ->
                            scope.launch {
                                container.vaultSession.changeMasterPassword(newPassword)
                                newPassword.zeroize()
                                onDone()
                            }
                        },
                    )
                }
            }
        }
    }
}
