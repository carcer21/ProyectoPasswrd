package dev.passwrd.core.storage

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface VaultHeaderDao {
    @Query("SELECT * FROM vault_header WHERE id = 0")
    suspend fun get(): VaultHeaderEntity?

    @Upsert
    suspend fun upsert(header: VaultHeaderEntity)
}
