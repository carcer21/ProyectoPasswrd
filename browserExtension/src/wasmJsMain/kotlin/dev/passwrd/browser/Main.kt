package dev.passwrd.browser

import dev.passwrd.core.model.ItemPayload
import dev.passwrd.core.model.ItemType
import dev.passwrd.core.model.VaultItem
import dev.passwrd.core.vault.VaultRepository
import dev.passwrd.core.vault.VaultSession
import dev.passwrd.core.vault.currentTimeMillis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private val scope = CoroutineScope(Dispatchers.Default)
private val store = ChromeStorageVaultStore()
private val session = VaultSession(store)
private val repository = VaultRepository(store, session)

private val screens = listOf("screen-onboarding", "screen-unlock", "screen-list")
private var collectingItems = false
private var editingItem: VaultItem? = null

fun main() {
    onClick("onboarding-submit") { scope.launch { handleCreateVault() } }
    onClick("unlock-submit") { scope.launch { handleUnlock() } }
    onClick("list-lock") {
        session.lock()
        collectingItems = false
        showScreen("screen-unlock")
    }
    onClick("new-submit") { scope.launch { handleSubmitItem() } }
    onClick("new-cancel") { cancelEdit() }

    scope.launch {
        store.load()
        showScreen(if (session.isInitialized()) "screen-unlock" else "screen-onboarding")
    }
}

private fun showScreen(id: String) {
    screens.forEach { hide(it) }
    show(id)
    if (id == "screen-list") observeItems()
}

private suspend fun handleCreateVault() {
    val password = value("onboarding-password")
    val confirm = value("onboarding-confirm")
    if (password.length < 8 || password != confirm) {
        setText("onboarding-error", "Contraseña inválida o no coincide (mínimo 8 caracteres)")
        show("onboarding-error")
        return
    }
    hide("onboarding-error")
    try {
        session.createVault(password.encodeToByteArray())
        showScreen("screen-list")
    } catch (e: Throwable) {
        val cause = e.cause
        val text = "Error: ${e::class.simpleName}: ${e.message}" +
            (if (cause != null) " | cause: ${cause::class.simpleName}: ${cause.message}" else "")
        setText("onboarding-error", text)
        show("onboarding-error")
    }
}

private suspend fun handleUnlock() {
    val password = value("unlock-password")
    val ok = session.unlock(password.encodeToByteArray())
    if (ok) {
        hide("unlock-error")
        showScreen("screen-list")
    } else {
        show("unlock-error")
    }
}

private fun observeItems() {
    if (collectingItems) return
    collectingItems = true
    scope.launch {
        repository.observeActive().collectLatest { items -> renderItems(items) }
    }
}

private const val MASKED_PASSWORD = "••••••••"

private fun renderItems(items: List<VaultItem>) {
    if (items.isEmpty()) {
        setHtml("item-list", "<li>Vault vacío</li>")
        return
    }
    setHtml("item-list", items.joinToString("") { itemHtml(it) })
    items.forEach { item -> bindItemControls(item) }
}

private fun itemHtml(item: VaultItem): String {
    val (name, subtitle) = when (val payload = item.payload) {
        is ItemPayload.Login -> payload.name to payload.username
        is ItemPayload.SecureNote -> payload.name to "Nota segura"
        is ItemPayload.Card -> payload.name to "Tarjeta"
        is ItemPayload.Identity -> payload.name to "Identidad"
        is ItemPayload.Passkey -> payload.name to "Passkey"
    }
    val passwordRow = if (item.payload is ItemPayload.Login) {
        "<div class=\"pw-row\">" +
            "<span id=\"pwtext-${item.id}\">$MASKED_PASSWORD</span>" +
            "<button id=\"reveal-${item.id}\" type=\"button\">Ver</button>" +
            "<button id=\"copy-${item.id}\" type=\"button\">Copiar</button>" +
            "</div>"
    } else {
        ""
    }
    val editButton = if (item.payload is ItemPayload.Login) {
        "<button id=\"edit-${item.id}\" type=\"button\">Editar</button>"
    } else {
        ""
    }
    val actionsRow = "<div class=\"item-actions\">$editButton<button id=\"delete-${item.id}\" type=\"button\">Borrar</button></div>"
    return "<li><div class=\"name\">${escapeHtml(name)}</div><div class=\"user\">${escapeHtml(subtitle)}</div>$passwordRow$actionsRow</li>"
}

private fun bindItemControls(item: VaultItem) {
    val payload = item.payload
    if (payload is ItemPayload.Login) {
        var revealed = false
        onClick("reveal-${item.id}") {
            revealed = !revealed
            setText("pwtext-${item.id}", if (revealed) payload.password else MASKED_PASSWORD)
            setText("reveal-${item.id}", if (revealed) "Ocultar" else "Ver")
        }
        onClick("copy-${item.id}") { copyToClipboard(payload.password) }
        onClick("edit-${item.id}") { startEdit(item, payload) }
    }
    onClick("delete-${item.id}") {
        if (confirmAction("¿Borrar \"${itemName(item)}\"? No se puede deshacer.")) {
            scope.launch { repository.delete(item.id) }
        }
    }
}

private fun itemName(item: VaultItem): String = when (val payload = item.payload) {
    is ItemPayload.Login -> payload.name
    is ItemPayload.SecureNote -> payload.name
    is ItemPayload.Card -> payload.name
    is ItemPayload.Identity -> payload.name
    is ItemPayload.Passkey -> payload.name
}

private fun startEdit(item: VaultItem, payload: ItemPayload.Login) {
    editingItem = item
    setValue("new-name", payload.name)
    setValue("new-username", payload.username)
    setValue("new-password", payload.password)
    setText("new-submit", "Guardar cambios")
    show("new-cancel")
}

private fun cancelEdit() {
    editingItem = null
    setValue("new-name", "")
    setValue("new-username", "")
    setValue("new-password", "")
    setText("new-submit", "Añadir login")
    hide("new-cancel")
}

@OptIn(ExperimentalUuidApi::class)
private suspend fun handleSubmitItem() {
    val name = value("new-name")
    if (name.isBlank()) return
    val now = currentTimeMillis()
    val editing = editingItem
    val item = if (editing != null) {
        editing.copy(
            payload = ItemPayload.Login(name = name, username = value("new-username"), password = value("new-password")),
            updatedAt = now,
            revision = editing.revision + 1,
        )
    } else {
        VaultItem(
            id = Uuid.random().toString(),
            type = ItemType.LOGIN,
            payload = ItemPayload.Login(name = name, username = value("new-username"), password = value("new-password")),
            createdAt = now,
            updatedAt = now,
            revision = 1,
        )
    }
    repository.upsert(item)
    cancelEdit()
}
