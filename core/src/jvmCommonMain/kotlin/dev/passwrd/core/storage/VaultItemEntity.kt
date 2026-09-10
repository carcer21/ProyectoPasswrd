package dev.passwrd.core.storage

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Fila opaca: `storage/` nunca ve una clave ni un payload en claro — sólo blobs cifrados.
 * Ver docs/CRYPTO_SPEC.md "Modelo de datos preparado para sync".
 */
@Entity(tableName = "vault_items")
data class VaultItemEntity(
    @PrimaryKey val id: String,
    val type: Int,
    val wrappedItemKey: ByteArray,
    val encPayload: ByteArray,
    val createdAt: Long,
    val updatedAt: Long,
    val revision: Long,
    val deletedAt: Long?,
)
