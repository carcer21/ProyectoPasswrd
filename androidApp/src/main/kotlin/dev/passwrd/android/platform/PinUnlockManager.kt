package dev.passwrd.android.platform

import dev.passwrd.core.crypto.Argon2Params
import dev.passwrd.core.crypto.Argon2id
import dev.passwrd.core.crypto.AuthenticationFailedException
import dev.passwrd.core.crypto.EncryptedBlob
import dev.passwrd.core.crypto.secureRandomBytes
import dev.passwrd.core.crypto.zeroize

private const val MAX_PIN_ATTEMPTS = 5
private const val PIN_SALT_SIZE = 16

// El PIN nunca es la raíz criptográfica (ver docs/CRYPTO_SPEC.md) — la resistencia la da el
// Keystore, no Argon2id. Parámetros ligeros a propósito: no tiene sentido pagar el coste
// completo en cada desbloqueo rápido.
private val PIN_KDF_PARAMS = Argon2Params(memoryKib = 19 * 1024, iterations = 2, parallelism = 1)

/**
 * PIN → Argon2id → PDK → AES-GCM envuelve VK → pinBlob → cifrado OTRA VEZ con clave del
 * Android Keystore. Ver docs/CRYPTO_SPEC.md "Desbloqueo por PIN".
 */
class PinUnlockManager(private val store: QuickUnlockStore) {

    val isEnabled: Boolean get() = store.pinEnabled

    suspend fun setup(pin: ByteArray, vaultKey: ByteArray) {
        val salt = secureRandomBytes(PIN_SALT_SIZE)
        val pdk = Argon2id.derive(pin, salt, PIN_KDF_PARAMS)
        val pinBlob = EncryptedBlob.encrypt(pdk, vaultKey)
        pdk.zeroize()

        val cipher = KeystoreKeys.encryptCipher(KeystoreKeys.pinKey())
        val encryptedPinBlob = cipher.doFinal(pinBlob)
        store.savePinSetup(salt, PIN_KDF_PARAMS.memoryKib, PIN_KDF_PARAMS.iterations, cipher.iv, encryptedPinBlob)
    }

    /** @return la Vault Key si el PIN es correcto, o null (contando el intento) si no. */
    suspend fun tryUnlock(pin: ByteArray): ByteArray? {
        val setup = store.loadPinSetup() ?: return null
        if (store.pinAttempts >= MAX_PIN_ATTEMPTS) {
            disable()
            return null
        }

        val pinBlob = try {
            val cipher = KeystoreKeys.decryptCipher(KeystoreKeys.pinKey(), setup.keystoreIv)
            cipher.doFinal(setup.blob)
        } catch (e: Exception) {
            // Clave de Keystore invalidada tras una actualización de sistema -> atajo muerto.
            disable()
            return null
        }

        val params = Argon2Params(setup.memoryKib, setup.iterations, parallelism = 1)
        val pdk = Argon2id.derive(pin, setup.salt, params)
        return try {
            val vaultKey = EncryptedBlob.decrypt(pdk, pinBlob)
            store.pinAttempts = 0
            vaultKey
        } catch (e: AuthenticationFailedException) {
            val attempts = store.pinAttempts + 1
            store.pinAttempts = attempts
            if (attempts >= MAX_PIN_ATTEMPTS) disable()
            null
        } finally {
            pdk.zeroize()
        }
    }

    fun disable() {
        store.clearPin()
        KeystoreKeys.deletePinKey()
    }
}
