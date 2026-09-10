package dev.passwrd.core.model

/** Modelo de dominio ya descifrado. Nunca cruza la frontera de `storage/` — ver arquitectura en el plan. */
data class VaultItem(
    val id: String,
    val type: ItemType,
    val payload: ItemPayload,
    val createdAt: Long,
    val updatedAt: Long,
    val revision: Long,
    val deletedAt: Long? = null,
)
