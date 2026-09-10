package dev.passwrd.core.crypto

/**
 * AES-256-GCM. `seal`/`open` trabajan sobre nonce+clave ya dados (12 bytes de nonce, tag
 * de 128 bits anexado al final del resultado por la propia JCA) — la generación de nonce y
 * el formato de blob persistido viven en [EncryptedBlob].
 */
expect object Aes256Gcm {
    /** @return ciphertext || tag (16 bytes de tag al final). */
    suspend fun seal(key: ByteArray, nonce: ByteArray, plaintext: ByteArray, aad: ByteArray = ByteArray(0)): ByteArray

    /** @throws AuthenticationFailedException si el tag no valida (contraseña incorrecta o dato manipulado). */
    suspend fun open(key: ByteArray, nonce: ByteArray, ciphertextAndTag: ByteArray, aad: ByteArray = ByteArray(0)): ByteArray
}
