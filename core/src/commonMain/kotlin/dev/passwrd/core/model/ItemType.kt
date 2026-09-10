package dev.passwrd.core.model

/**
 * `code` se guarda en claro en [dev.passwrd.core.storage.VaultItemEntity.type] — necesario
 * para filtrar/ordenar sin descifrar todo el vault. Ver docs/THREAT_MODEL.md.
 */
enum class ItemType(val code: Int) {
    LOGIN(0),
    SECURE_NOTE(1),
    CARD(2),
    IDENTITY(3),
    PASSKEY(4);

    companion object {
        fun fromCode(code: Int): ItemType =
            entries.firstOrNull { it.code == code }
                ?: throw IllegalArgumentException("ItemType desconocido: $code")
    }
}
