package dev.passwrd.core.storage

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Implementación de [VaultStore] con Room — Android y JVM (desktop). Ver ADR 0006. */
class RoomVaultStore(database: PasswrdDatabase) : VaultStore {
    private val headerDao = database.vaultHeaderDao()
    private val itemDao = database.vaultItemDao()

    override suspend fun getHeader(): VaultHeaderData? = headerDao.get()?.toData()

    override suspend fun upsertHeader(header: VaultHeaderData) {
        headerDao.upsert(
            VaultHeaderEntity(
                kdfSalt = header.kdfSalt,
                kdfMemoryKib = header.kdfMemoryKib,
                kdfIterations = header.kdfIterations,
                kdfParallelism = header.kdfParallelism,
                wrappedVaultKey = header.wrappedVaultKey,
                createdAt = header.createdAt,
            ),
        )
    }

    override fun observeActiveItems(): Flow<List<VaultItemData>> =
        itemDao.observeActive().map { entities -> entities.map { it.toData() } }

    override suspend fun findItemById(id: String): VaultItemData? = itemDao.findById(id)?.toData()

    override suspend fun upsertItem(item: VaultItemData) {
        itemDao.upsert(
            VaultItemEntity(
                id = item.id,
                type = item.type,
                wrappedItemKey = item.wrappedItemKey,
                encPayload = item.encPayload,
                createdAt = item.createdAt,
                updatedAt = item.updatedAt,
                revision = item.revision,
                deletedAt = item.deletedAt,
            ),
        )
    }

    override suspend fun markItemDeleted(id: String, deletedAt: Long) = itemDao.markDeleted(id, deletedAt)

    override suspend fun purgeItem(id: String) = itemDao.purge(id)

    private fun VaultHeaderEntity.toData() =
        VaultHeaderData(kdfSalt, kdfMemoryKib, kdfIterations, kdfParallelism, wrappedVaultKey, createdAt)

    private fun VaultItemEntity.toData() =
        VaultItemData(id, type, wrappedItemKey, encPayload, createdAt, updatedAt, revision, deletedAt)
}
