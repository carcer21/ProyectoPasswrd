package dev.passwrd.android.platform

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import javax.crypto.Cipher

/**
 * VK envuelta directamente por una clave del Keystore con `setUserAuthenticationRequired` +
 * `AUTH_BIOMETRIC_STRONG` — ver docs/CRYPTO_SPEC.md "Desbloqueo biométrico". La clave sólo
 * se usa a través de [BiometricPrompt.CryptoObject]: sin eso, la autenticación biométrica es
 * puro teatro (saltable con Frida). Si el `Cipher` no descifra, no hay clave.
 */
class BiometricUnlockManager(private val store: QuickUnlockStore) {

    val isEnabled: Boolean get() = store.biometricEnabled

    fun isAvailable(activity: FragmentActivity): Boolean =
        BiometricManager.from(activity)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS

    fun setup(activity: FragmentActivity, vaultKey: ByteArray, onResult: (Boolean) -> Unit) {
        val cipher = try {
            KeystoreKeys.encryptCipher(KeystoreKeys.biometricKey())
        } catch (e: Exception) {
            onResult(false)
            return
        }

        promptFor(activity, cipher, title = "Activar desbloqueo biométrico") { resultCipher ->
            if (resultCipher == null) {
                onResult(false)
                return@promptFor
            }
            val encryptedVaultKey = resultCipher.doFinal(vaultKey)
            store.saveBiometricSetup(resultCipher.iv, encryptedVaultKey)
            onResult(true)
        }
    }

    fun unlock(activity: FragmentActivity, onResult: (ByteArray?) -> Unit) {
        val setup = store.loadBiometricSetup()
        if (setup == null) {
            onResult(null)
            return
        }

        val cipher = try {
            KeystoreKeys.decryptCipher(KeystoreKeys.biometricKey(), setup.keystoreIv)
        } catch (e: Exception) {
            // KeyPermanentlyInvalidatedException (huella nueva enrolada) u otra invalidación.
            disable()
            onResult(null)
            return
        }

        promptFor(activity, cipher, title = "Desbloquear Passwrd") { resultCipher ->
            if (resultCipher == null) {
                onResult(null)
                return@promptFor
            }
            onResult(try {
                resultCipher.doFinal(setup.blob)
            } catch (e: Exception) {
                null
            })
        }
    }

    fun disable() {
        store.clearBiometric()
        KeystoreKeys.deleteBiometricKey()
    }

    private fun promptFor(activity: FragmentActivity, cipher: Cipher, title: String, onResult: (Cipher?) -> Unit) {
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onResult(result.cryptoObject?.cipher)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                onResult(null)
            }
            // onAuthenticationFailed (huella no reconocida) no cierra el prompt a propósito:
            // BiometricPrompt sigue esperando otro intento.
        }

        val prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), callback)
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setNegativeButtonText("Usar contraseña maestra")
            .build()
        prompt.authenticate(info, BiometricPrompt.CryptoObject(cipher))
    }
}
