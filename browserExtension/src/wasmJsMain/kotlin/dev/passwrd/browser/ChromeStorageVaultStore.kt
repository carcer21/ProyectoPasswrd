package dev.passwrd.browser

import dev.passwrd.core.storage.VaultHeaderData
import dev.passwrd.core.storage.VaultItemData
import dev.passwrd.core.storage.VaultStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

private const val STORAGE_KEY = "passwrd_vault"

@Serializable
private data class StoredHeader(
    val kdfSalt: String,
    val kdfMemoryKib: Int,
    val kdfIterations: Int,
    val kdfParallelism: Int,
    val wrappedVaultKey: String,
    val createdAt: Long,
)

@Serializable
private data class StoredItem(
    val id: String,
    val type: Int,
    val wrappedItemKey: String,
    val encPayload: String,
    val createdAt: Long,
    val updatedAt: Long,
    val revision: Long,
    val deletedAt: Long?,
)

@Serializable
private data class StoredVault(
    val header: StoredHeader? = null,
    val items: List<StoredItem> = emptyList(),
)

@OptIn(ExperimentalEncodingApi::class)
private fun VaultHeaderData.toStored() = StoredHeader(
    kdfSalt = Base64.encode(kdfSalt),
    kdfMemoryKib = kdfMemoryKib,
    kdfIterations = kdfIterations,
    kdfParallelism = kdfParallelism,
    wrappedVaultKey = Base64.encode(wrappedVaultKey),
    createdAt = createdAt,
)

@OptIn(ExperimentalEncodingApi::class)
private fun StoredHeader.toData() = VaultHeaderData(
    kdfSalt = Base64.decode(kdfSalt),
    kdfMemoryKib = kdfMemoryKib,
    kdfIterations = kdfIterations,
    kdfParallelism = kdfParallelism,
    wrappedVaultKey = Base64.decode(wrappedVaultKey),
    createdAt = createdAt,
)

@OptIn(ExperimentalEncodingApi::class)
private fun VaultItemData.toStored() = StoredItem(
    id = id,
    type = type,
    wrappedItemKey = Base64.encode(wrappedItemKey),
    encPayload = Base64.encode(encPayload),
    createdAt = createdAt,
    updatedAt = updatedAt,
    revision = revision,
    deletedAt = deletedAt,
)

@OptIn(ExperimentalEncodingApi::class)
private fun StoredItem.toData() = VaultItemData(
    id = id,
    type = type,
    wrappedItemKey = Base64.decode(wrappedItemKey),
    encPayload = Base64.decode(encPayload),
    createdAt = createdAt,
    updatedAt = updatedAt,
    revision = revision,
    deletedAt = deletedAt,
)

/**
 * [VaultStore] sobre `chrome.storage.local` — ver ADR 0006. El popup se recrea en cada
 * apertura, así que `load()` hay que llamarlo una vez al arrancar; a partir de ahí todo vive
 * en memoria y se escribe a través (`persist()`) en cada cambio.
 */
class ChromeStorageVaultStore : VaultStore {
    private var header: VaultHeaderData? = null
    private val items = LinkedHashMap<String, VaultItemData>()
    private val itemsFlow = MutableStateFlow<List<VaultItemData>>(emptyList())

    suspend fun load() {
        val raw = chromeStorageGet(STORAGE_KEY)
        val stored = if (raw != null) Json.decodeFromString(StoredVault.serializer(), raw) else StoredVault()
        header = stored.header?.toData()
        items.clear()
        stored.items.forEach { items[it.id] = it.toData() }
        publishItems()
    }

    override suspend fun getHeader(): VaultHeaderData? = header

    override suspend fun upsertHeader(header: VaultHeaderData) {
        this.header = header
        persist()
    }

    override fun observeActiveItems(): Flow<List<VaultItemData>> = itemsFlow

    override suspend fun findItemById(id: String): VaultItemData? = items[id]

    override suspend fun upsertItem(item: VaultItemData) {
        items[item.id] = item
        publishItems()
        persist()
    }

    override suspend fun markItemDeleted(id: String, deletedAt: Long) {
        val current = items[id] ?: return
        items[id] = current.copy(deletedAt = deletedAt, updatedAt = deletedAt, revision = current.revision + 1)
        publishItems()
        persist()
    }

    override suspend fun purgeItem(id: String) {
        items.remove(id)
        publishItems()
        persist()
    }

    private fun publishItems() {
        itemsFlow.value = items.values.filter { it.deletedAt == null }.sortedByDescending { it.updatedAt }
    }

    private suspend fun persist() {
        val stored = StoredVault(header?.toStored(), items.values.map { it.toStored() })
        chromeStorageSet(STORAGE_KEY, Json.encodeToString(StoredVault.serializer(), stored))
    }
}
