package dev.passwrd.core.vault

import dev.passwrd.core.crypto.EncryptedBlob
import dev.passwrd.core.crypto.secureRandomBytes
import dev.passwrd.core.crypto.zeroize
import dev.passwrd.core.model.ItemType
import dev.passwrd.core.model.VaultItem
import dev.passwrd.core.storage.VaultItemData
import dev.passwrd.core.storage.VaultStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val ITEM_KEY_SIZE = 32

/**
 * Frontera de cifrado por item (ver arquitectura en el plan: "storage/ sólo ve blobs
 * opacos"). Cada upsert genera una Item Key nueva y re-cifra el payload — coste O(1) por
 * item, ver docs/CRYPTO_SPEC.md "Rotación". El `id` del item se pasa como AAD: liga cada
 * blob a su fila y evita que un ciphertext se pueda mover a otra fila sin que falle la
 * autenticación.
 */
class VaultRepository(
    private val store: VaultStore,
    private val session: VaultSession,
) {
    fun observeActive(): Flow<List<VaultItem>> =
        store.observeActiveItems().map { entities ->
            val decrypted = ArrayList<VaultItem>(entities.size)
            for (entity in entities) decrypted.add(decrypt(entity))
            decrypted
        }

    suspend fun findById(id: String): VaultItem? =
        store.findItemById(id)?.let { decrypt(it) }

    suspend fun upsert(item: VaultItem) {
        val vaultKey = session.requireVaultKey()
        val aad = item.id.encodeToByteArray()

        val itemKey = secureRandomBytes(ITEM_KEY_SIZE)
        val payloadBytes = ItemCodec.encode(item.payload)
        val encPayload = EncryptedBlob.encrypt(itemKey, payloadBytes, aad)
        val wrappedItemKey = EncryptedBlob.encrypt(vaultKey, itemKey, aad)
        itemKey.zeroize()

        store.upsertItem(
            VaultItemData(
                id = item.id,
                type = item.type.code,
                wrappedItemKey = wrappedItemKey,
                encPayload = encPayload,
                createdAt = item.createdAt,
                updatedAt = item.updatedAt,
                revision = item.revision,
                deletedAt = item.deletedAt,
            ),
        )
    }

    suspend fun delete(id: String) {
        store.markItemDeleted(id, currentTimeMillis())
    }

    suspend fun purge(id: String) {
        store.purgeItem(id)
    }

    private suspend fun decrypt(entity: VaultItemData): VaultItem {
        val vaultKey = session.requireVaultKey()
        val aad = entity.id.encodeToByteArray()

        val itemKey = EncryptedBlob.decrypt(vaultKey, entity.wrappedItemKey, aad)
        val payloadBytes = EncryptedBlob.decrypt(itemKey, entity.encPayload, aad)
        itemKey.zeroize()

        return VaultItem(
            id = entity.id,
            type = ItemType.fromCode(entity.type),
            payload = ItemCodec.decode(payloadBytes),
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            revision = entity.revision,
            deletedAt = entity.deletedAt,
        )
    }
}
