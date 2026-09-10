package dev.passwrd.core.vault

import dev.passwrd.core.crypto.Argon2Params
import dev.passwrd.core.crypto.Argon2id
import dev.passwrd.core.crypto.AuthenticationFailedException
import dev.passwrd.core.crypto.EncryptedBlob
import dev.passwrd.core.crypto.Hkdf
import dev.passwrd.core.crypto.KeyRing
import dev.passwrd.core.crypto.secureRandomBytes
import dev.passwrd.core.crypto.zeroize
import dev.passwrd.core.storage.VaultHeaderData
import dev.passwrd.core.storage.VaultStore

private const val KEK_INFO = "passwrd.v1.kek"
private const val SALT_SIZE = 16
private const val VAULT_KEY_SIZE = 32

/**
 * Orquesta la jerarquía de claves de docs/CRYPTO_SPEC.md: MK → KEK → VK. Es la ÚNICA clase
 * que ve la contraseña maestra. Todo lo demás ([VaultRepository]) sólo pide la Vault Key ya
 * desenvuelta.
 */
class VaultSession(private val store: VaultStore) {
    private var keyRing: KeyRing = KeyRing.locked()

    val isUnlocked: Boolean get() = keyRing.isUnlocked

    suspend fun isInitialized(): Boolean = store.getHeader() != null

    /** Crea el vault por primera vez y lo deja desbloqueado. */
    suspend fun createVault(masterPassword: ByteArray, kdfParams: Argon2Params = Argon2Params.default()) {
        var stage = "getHeader"
        try {
            check(store.getHeader() == null) { "el vault ya existe" }

            stage = "secureRandomBytes(salt)"
            val salt = secureRandomBytes(SALT_SIZE)
            stage = "secureRandomBytes(vaultKey)"
            val vaultKey = secureRandomBytes(VAULT_KEY_SIZE)
            stage = "deriveKek"
            val kek = deriveKek(masterPassword, salt, kdfParams)
            stage = "EncryptedBlob.encrypt"
            val wrappedVaultKey = EncryptedBlob.encrypt(kek, vaultKey)
            kek.zeroize()

            stage = "upsertHeader"
            store.upsertHeader(
                VaultHeaderData(
                    kdfSalt = salt,
                    kdfMemoryKib = kdfParams.memoryKib,
                    kdfIterations = kdfParams.iterations,
                    kdfParallelism = kdfParams.parallelism,
                    wrappedVaultKey = wrappedVaultKey,
                    createdAt = currentTimeMillis(),
                ),
            )
            keyRing = KeyRing.unlocked(vaultKey)
        } catch (e: Throwable) {
            throw RuntimeException("createVault fallo en stage [$stage]: ${e::class.simpleName}: ${e.message}", e)
        }
    }

    /**
     * @return `true` si la contraseña era correcta y el vault quedó desbloqueado.
     * No distingue "contraseña incorrecta" de "dato manipulado" — ver docs/THREAT_MODEL.md.
     */
    suspend fun unlock(masterPassword: ByteArray): Boolean {
        val header = store.getHeader() ?: error("no hay vault creado todavía")
        val params = Argon2Params(header.kdfMemoryKib, header.kdfIterations, header.kdfParallelism)
        val kek = deriveKek(masterPassword, header.kdfSalt, params)

        return try {
            val vaultKey = EncryptedBlob.decrypt(kek, header.wrappedVaultKey)
            keyRing = KeyRing.unlocked(vaultKey)
            true
        } catch (e: AuthenticationFailedException) {
            false
        } finally {
            kek.zeroize()
        }
    }

    /**
     * Usado por atajos de desbloqueo (PIN, biometría) que ya han recuperado la Vault Key por
     * su cuenta (envuelta en Android Keystore) — ver docs/CRYPTO_SPEC.md. `core` no sabe nada
     * de PIN ni de biometría; sólo expone el punto de entrada para restaurar la sesión.
     */
    fun unlockWithVaultKey(vaultKey: ByteArray) {
        keyRing = KeyRing.unlocked(vaultKey)
    }

    fun lock() {
        keyRing.lock()
    }

    /** Coste O(1): sólo re-envuelve la Vault Key, nunca reescribe los items — ver CRYPTO_SPEC.md "Rotación". */
    suspend fun changeMasterPassword(newPassword: ByteArray, kdfParams: Argon2Params = Argon2Params.default()) {
        val vaultKey = keyRing.requireVaultKey()
        val salt = secureRandomBytes(SALT_SIZE)
        val kek = deriveKek(newPassword, salt, kdfParams)
        val wrappedVaultKey = EncryptedBlob.encrypt(kek, vaultKey)
        kek.zeroize()

        val header = checkNotNull(store.getHeader())
        store.upsertHeader(
            header.copy(
                kdfSalt = salt,
                kdfMemoryKib = kdfParams.memoryKib,
                kdfIterations = kdfParams.iterations,
                kdfParallelism = kdfParams.parallelism,
                wrappedVaultKey = wrappedVaultKey,
            ),
        )
    }

    fun requireVaultKey(): ByteArray = keyRing.requireVaultKey()

    private suspend fun deriveKek(masterPassword: ByteArray, salt: ByteArray, params: Argon2Params): ByteArray {
        val masterKey = try {
            Argon2id.derive(masterPassword, salt, params)
        } catch (e: Throwable) {
            throw RuntimeException("deriveKek fallo en Argon2id.derive: ${e::class.simpleName}: ${e.message}", e)
        }
        val kek = try {
            Hkdf.deriveKey(masterKey, salt, KEK_INFO.encodeToByteArray(), VAULT_KEY_SIZE)
        } catch (e: Throwable) {
            throw RuntimeException("deriveKek fallo en Hkdf.deriveKey: ${e::class.simpleName}: ${e.message}", e)
        }
        masterKey.zeroize()
        return kek
    }
}
