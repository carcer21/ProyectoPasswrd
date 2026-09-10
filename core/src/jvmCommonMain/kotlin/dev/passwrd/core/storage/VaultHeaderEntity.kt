package dev.passwrd.core.storage

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Fila única (id fijo = 0): un dispositivo local tiene un solo vault en v1. Guarda los
 * parámetros Argon2id junto a la Vault Key envuelta — ver docs/CRYPTO_SPEC.md.
 */
@Entity(tableName = "vault_header")
data class VaultHeaderEntity(
    @PrimaryKey val id: Int = 0,
    val kdfSalt: ByteArray,
    val kdfMemoryKib: Int,
    val kdfIterations: Int,
    val kdfParallelism: Int,
    val wrappedVaultKey: ByteArray,
    val createdAt: Long,
)
