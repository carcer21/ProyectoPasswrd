package dev.passwrd.core.crypto

/**
 * Contenedor en memoria de la Vault Key mientras el vault está desbloqueado. `lock()` pone
 * la clave a cero antes de soltarla — ver docs/CRYPTO_SPEC.md "Higiene de memoria". La
 * jerarquía completa (MK → KEK → VK → IK) se orquesta en `core/vault` (Fase 2); esta clase
 * sólo resuelve el ciclo de vida de la única clave que hay que retener en RAM.
 */
class KeyRing internal constructor(private var vaultKey: ByteArray?) {

    val isUnlocked: Boolean get() = vaultKey != null

    fun requireVaultKey(): ByteArray =
        checkNotNull(vaultKey) { "KeyRing bloqueado: no hay Vault Key en memoria" }

    fun lock() {
        vaultKey?.zeroize()
        vaultKey = null
    }

    companion object {
        fun unlocked(vaultKey: ByteArray) = KeyRing(vaultKey)
        fun locked() = KeyRing(null)
    }
}
