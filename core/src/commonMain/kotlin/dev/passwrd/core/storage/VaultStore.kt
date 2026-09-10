package dev.passwrd.core.storage

import kotlinx.coroutines.flow.Flow

/** Espejo sin anotaciones de Room de `vault_header` — ver [VaultStore]. */
data class VaultHeaderData(
    val kdfSalt: ByteArray,
    val kdfMemoryKib: Int,
    val kdfIterations: Int,
    val kdfParallelism: Int,
    val wrappedVaultKey: ByteArray,
    val createdAt: Long,
)

/** Espejo sin anotaciones de Room de `vault_items` — ver [VaultStore]. */
data class VaultItemData(
    val id: String,
    val type: Int,
    val wrappedItemKey: ByteArray,
    val encPayload: ByteArray,
    val createdAt: Long,
    val updatedAt: Long,
    val revision: Long,
    val deletedAt: Long?,
)

/**
 * Frontera de almacenamiento — `vault/` (Fase 2) depende de esto, no de Room directamente.
 * Room no publica variante para `wasmJs` todavía (ver ADR 0006), así que la implementación
 * real vive por plataforma: `RoomVaultStore` en Android/JVM (jvmCommonMain), y
 * `chrome.storage.local` en la extensión de navegador (Fase 9b).
 */
interface VaultStore {
    suspend fun getHeader(): VaultHeaderData?
    suspend fun upsertHeader(header: VaultHeaderData)

    fun observeActiveItems(): Flow<List<VaultItemData>>
    suspend fun findItemById(id: String): VaultItemData?
    suspend fun upsertItem(item: VaultItemData)

    /** Tombstone (ver docs/CRYPTO_SPEC.md, sync futuro) — no borra la fila. */
    suspend fun markItemDeleted(id: String, deletedAt: Long)
    suspend fun purgeItem(id: String)
}
