package dev.passwrd.android.platform

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private const val ANDROID_KEYSTORE = "AndroidKeyStore"
private const val PIN_KEY_ALIAS = "passwrd.pin.kek"
private const val BIO_KEY_ALIAS = "passwrd.bio.kek"
private const val GCM_TAG_LENGTH_BITS = 128

/**
 * Claves AES-256-GCM no exportables del Android Keystore — ver docs/CRYPTO_SPEC.md
 * "Desbloqueo por PIN" y "Desbloqueo biométrico". Con StrongBox si el dispositivo lo
 * soporta, con fallback silencioso al TEE normal si no.
 */
internal object KeystoreKeys {

    fun pinKey(): SecretKey = getOrCreateKey(PIN_KEY_ALIAS, requireUserAuth = false)

    /** setUserAuthenticationRequired + invalidada al enrolar una huella nueva. */
    fun biometricKey(): SecretKey = getOrCreateKey(BIO_KEY_ALIAS, requireUserAuth = true)

    fun deletePinKey() = deleteKey(PIN_KEY_ALIAS)
    fun deleteBiometricKey() = deleteKey(BIO_KEY_ALIAS)

    fun encryptCipher(key: SecretKey): Cipher =
        Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key) }

    fun decryptCipher(key: SecretKey, iv: ByteArray): Cipher =
        Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        }

    private fun getOrCreateKey(alias: String, requireUserAuth: Boolean): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(alias, null) as? SecretKey)?.let { return it }

        fun spec(useStrongBox: Boolean) =
            KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .apply {
                    if (requireUserAuth) {
                        setUserAuthenticationRequired(true)
                        setInvalidatedByBiometricEnrollment(true)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
                        } else {
                            @Suppress("DEPRECATION")
                            setUserAuthenticationValidityDurationSeconds(-1)
                        }
                    }
                    if (useStrongBox && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        setIsStrongBoxBacked(true)
                    }
                }
                .build()

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        return try {
            keyGenerator.init(spec(useStrongBox = true))
            keyGenerator.generateKey()
        } catch (e: Exception) {
            // Sin StrongBox en este dispositivo (o fallo al inicializarlo): TEE normal.
            keyGenerator.init(spec(useStrongBox = false))
            keyGenerator.generateKey()
        }
    }

    private fun deleteKey(alias: String) {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (keyStore.containsAlias(alias)) keyStore.deleteEntry(alias)
    }
}
