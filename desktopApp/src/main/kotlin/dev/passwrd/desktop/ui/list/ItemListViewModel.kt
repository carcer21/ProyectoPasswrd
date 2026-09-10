package dev.passwrd.desktop.ui.list

import dev.passwrd.core.model.VaultItem
import dev.passwrd.core.vault.VaultRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** Sin androidx.lifecycle.ViewModel: no hay Activity cuyos cambios de configuración sobrevivir en desktop. */
class ItemListViewModel(repository: VaultRepository, scope: CoroutineScope) {
    val items: StateFlow<List<VaultItem>> = repository.observeActive()
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
