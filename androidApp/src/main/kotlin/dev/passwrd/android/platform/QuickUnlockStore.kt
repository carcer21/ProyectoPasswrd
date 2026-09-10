package dev.passwrd.android.platform

import android.content.Context
import android.util.Base64

private const val PREFS_NAME = "passwrd.quick_unlock"

private const val KEY_PIN_ENABLED = "pin_enabled"
private const val KEY_PIN_SALT = "pin_salt"
private const val KEY_PIN_KDF_MEM = "pin_kdf_mem"
private const val KEY_PIN_KDF_ITER = "pin_kdf_iter"
private const val KEY_PIN_IV = "pin_iv"
private const val KEY_PIN_BLOB = "pin_blob"
private const val KEY_PIN_ATTEMPTS = "pin_attempts"

private const val KEY_BIO_ENABLED = "bio_enabled"
private const val KEY_BIO_IV = "bio_iv"
private const val KEY_BIO_BLOB = "bio_blob"

/**
 * SharedPreferences en claro a propósito: lo que aquí se guarda ya está cifrado con una
 * clave del Android Keystore no exportable (ver [KeystoreKeys]). Una capa
 * EncryptedSharedPreferences encima cifraría dos veces lo mismo sin ganar nada — ver
 * docs/CRYPTO_SPEC.md.
 */
class QuickUnlockStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var pinEnabled: Boolean
        get() = prefs.getBoolean(KEY_PIN_ENABLED, false)
        private set(value) = prefs.edit().putBoolean(KEY_PIN_ENABLED, value).apply()

    var pinAttempts: Int
        get() = prefs.getInt(KEY_PIN_ATTEMPTS, 0)
        set(value) = prefs.edit().putInt(KEY_PIN_ATTEMPTS, value).apply()

    fun savePinSetup(salt: ByteArray, memoryKib: Int, iterations: Int, keystoreIv: ByteArray, blob: ByteArray) {
        prefs.edit()
            .putString(KEY_PIN_SALT, encode(salt))
            .putInt(KEY_PIN_KDF_MEM, memoryKib)
            .putInt(KEY_PIN_KDF_ITER, iterations)
            .putString(KEY_PIN_IV, encode(keystoreIv))
            .putString(KEY_PIN_BLOB, encode(blob))
            .putInt(KEY_PIN_ATTEMPTS, 0)
            .putBoolean(KEY_PIN_ENABLED, true)
            .apply()
    }

    fun loadPinSetup(): PinSetup? {
        val salt = prefs.getString(KEY_PIN_SALT, null)?.let(::decode) ?: return null
        val iv = prefs.getString(KEY_PIN_IV, null)?.let(::decode) ?: return null
        val blob = prefs.getString(KEY_PIN_BLOB, null)?.let(::decode) ?: return null
        val memoryKib = prefs.getInt(KEY_PIN_KDF_MEM, -1).takeIf { it > 0 } ?: return null
        val iterations = prefs.getInt(KEY_PIN_KDF_ITER, -1).takeIf { it > 0 } ?: return null
        return PinSetup(salt, memoryKib, iterations, iv, blob)
    }

    fun clearPin() {
        prefs.edit()
            .remove(KEY_PIN_SALT).remove(KEY_PIN_KDF_MEM).remove(KEY_PIN_KDF_ITER)
            .remove(KEY_PIN_IV).remove(KEY_PIN_BLOB).remove(KEY_PIN_ATTEMPTS)
            .putBoolean(KEY_PIN_ENABLED, false)
            .apply()
    }

    var biometricEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIO_ENABLED, false)
        private set(value) = prefs.edit().putBoolean(KEY_BIO_ENABLED, value).apply()

    fun saveBiometricSetup(keystoreIv: ByteArray, blob: ByteArray) {
        prefs.edit()
            .putString(KEY_BIO_IV, encode(keystoreIv))
            .putString(KEY_BIO_BLOB, encode(blob))
            .putBoolean(KEY_BIO_ENABLED, true)
            .apply()
    }

    fun loadBiometricSetup(): BiometricSetup? {
        val iv = prefs.getString(KEY_BIO_IV, null)?.let(::decode) ?: return null
        val blob = prefs.getString(KEY_BIO_BLOB, null)?.let(::decode) ?: return null
        return BiometricSetup(iv, blob)
    }

    fun clearBiometric() {
        prefs.edit().remove(KEY_BIO_IV).remove(KEY_BIO_BLOB).putBoolean(KEY_BIO_ENABLED, false).apply()
    }

    private fun encode(bytes: ByteArray) = Base64.encodeToString(bytes, Base64.NO_WRAP)
    private fun decode(s: String): ByteArray = Base64.decode(s, Base64.NO_WRAP)
}

data class PinSetup(
    val salt: ByteArray,
    val memoryKib: Int,
    val iterations: Int,
    val keystoreIv: ByteArray,
    val blob: ByteArray,
)

data class BiometricSetup(val keystoreIv: ByteArray, val blob: ByteArray)
