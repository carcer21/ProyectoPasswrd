package dev.passwrd.core.storage

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultItemDao {
    @Query("SELECT * FROM vault_items WHERE deletedAt IS NULL ORDER BY updatedAt DESC")
    fun observeActive(): Flow<List<VaultItemEntity>>

    @Query("SELECT * FROM vault_items WHERE id = :id")
    suspend fun findById(id: String): VaultItemEntity?

    @Upsert
    suspend fun upsert(item: VaultItemEntity)

    /** Tombstone (ver docs/CRYPTO_SPEC.md, sync futuro) — no borra la fila. */
    @Query("UPDATE vault_items SET deletedAt = :deletedAt, updatedAt = :deletedAt, revision = revision + 1 WHERE id = :id")
    suspend fun markDeleted(id: String, deletedAt: Long)

    @Query("DELETE FROM vault_items WHERE id = :id")
    suspend fun purge(id: String)
}
