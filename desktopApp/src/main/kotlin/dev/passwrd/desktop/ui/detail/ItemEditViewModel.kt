package dev.passwrd.desktop.ui.detail

import dev.passwrd.core.model.ItemPayload
import dev.passwrd.core.model.ItemType
import dev.passwrd.core.model.VaultItem
import dev.passwrd.core.vault.VaultRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

sealed interface ItemEditUiState {
    data object Loading : ItemEditUiState
    data class Ready(val item: VaultItem?) : ItemEditUiState
}

class ItemEditViewModel(
    private val repository: VaultRepository,
    private val itemId: String?,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow<ItemEditUiState>(ItemEditUiState.Loading)
    val state: StateFlow<ItemEditUiState> = _state.asStateFlow()

    init {
        scope.launch {
            val existing = itemId?.let { repository.findById(it) }
            _state.value = ItemEditUiState.Ready(existing)
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    fun save(type: ItemType, payload: ItemPayload, onDone: () -> Unit) {
        scope.launch {
            val current = (state.value as? ItemEditUiState.Ready)?.item
            val now = System.currentTimeMillis()
            val item = VaultItem(
                id = current?.id ?: itemId ?: Uuid.random().toString(),
                type = type,
                payload = payload,
                createdAt = current?.createdAt ?: now,
                updatedAt = now,
                revision = (current?.revision ?: 0) + 1,
            )
            repository.upsert(item)
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        val id = itemId ?: return
        scope.launch {
            repository.delete(id)
            onDone()
        }
    }
}
