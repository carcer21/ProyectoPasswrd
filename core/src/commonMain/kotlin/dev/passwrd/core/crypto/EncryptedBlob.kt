package dev.passwrd.core.crypto

/**
 * Formato de blob cifrado persistido — un único [ByteArray], idéntico en todas las
 * plataformas (ver docs/CRYPTO_SPEC.md "Formato de blob cifrado"):
 *
 * ```
 * byte 0        : versión de formato (0x01)
 * bytes 1..12   : nonce (96 bits, CSPRNG)
 * bytes 13..N-17: ciphertext
 * bytes N-16..N : tag GCM (128 bits)
 * ```
 */
object EncryptedBlob {
    const val VERSION: Byte = 1
    private const val NONCE_SIZE = 12

    suspend fun encrypt(key: ByteArray, plaintext: ByteArray, aad: ByteArray = ByteArray(0)): ByteArray {
        val nonce = secureRandomBytes(NONCE_SIZE)
        val ciphertextAndTag = Aes256Gcm.seal(key, nonce, plaintext, aad)
        return byteArrayOf(VERSION) + nonce + ciphertextAndTag
    }

    suspend fun decrypt(key: ByteArray, blob: ByteArray, aad: ByteArray = ByteArray(0)): ByteArray {
        require(blob.size > 1 + NONCE_SIZE) { "blob demasiado corto" }
        require(blob[0] == VERSION) { "versión de blob no soportada: ${blob[0]}" }

        val nonce = blob.copyOfRange(1, 1 + NONCE_SIZE)
        val ciphertextAndTag = blob.copyOfRange(1 + NONCE_SIZE, blob.size)
        return Aes256Gcm.open(key, nonce, ciphertextAndTag, aad)
    }
}
