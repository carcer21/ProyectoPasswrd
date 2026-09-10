package dev.passwrd.android.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.passwrd.core.crypto.zeroize
import dev.passwrd.core.vault.VaultSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface VaultUiState {
    data object Loading : VaultUiState
    data object NeedsOnboarding : VaultUiState
    data object Locked : VaultUiState
    data object Unlocked : VaultUiState
}

/** Sólo orquesta el ciclo de vida del vault (ver docs/CRYPTO_SPEC.md) — nunca ve items. */
class VaultViewModel(private val session: VaultSession) : ViewModel() {
    private val _state = MutableStateFlow<VaultUiState>(VaultUiState.Loading)
    val state: StateFlow<VaultUiState> = _state.asStateFlow()

    private val _unlockError = MutableStateFlow(false)
    val unlockError: StateFlow<Boolean> = _unlockError.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = if (session.isInitialized()) VaultUiState.Locked else VaultUiState.NeedsOnboarding
        }
    }

    fun createVault(password: ByteArray) {
        viewModelScope.launch {
            session.createVault(password)
            password.zeroize()
            _state.value = VaultUiState.Unlocked
        }
    }

    fun unlock(password: ByteArray) {
        viewModelScope.launch {
            val ok = session.unlock(password)
            password.zeroize()
            _unlockError.value = !ok
            if (ok) _state.value = VaultUiState.Unlocked
        }
    }

    fun clearUnlockError() {
        _unlockError.value = false
    }

    /** PIN o biometría ya recuperaron la Vault Key por su cuenta — ver [PinUnlockManager]/[BiometricUnlockManager]. */
    fun unlockWithRecoveredVaultKey(vaultKey: ByteArray) {
        session.unlockWithVaultKey(vaultKey)
        _state.value = VaultUiState.Unlocked
    }

    fun lock() {
        session.lock()
        _state.value = VaultUiState.Locked
    }

    fun changeMasterPassword(newPassword: ByteArray, onDone: () -> Unit) {
        viewModelScope.launch {
            session.changeMasterPassword(newPassword)
            newPassword.zeroize()
            onDone()
        }
    }

    companion object {
        fun factory(session: VaultSession) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = VaultViewModel(session) as T
        }
    }
}
