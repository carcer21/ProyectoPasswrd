package dev.passwrd.core.crypto

actual object Aes256Gcm {
    // WebCrypto anexa el tag GCM (16 bytes) al final del ciphertext en encrypt() y lo separa
    // en decrypt() — mismo formato que JCA, así que no hace falta tocar EncryptedBlob.kt.
    actual suspend fun seal(key: ByteArray, nonce: ByteArray, plaintext: ByteArray, aad: ByteArray): ByteArray =
        webCryptoAesGcmSeal(key, nonce, plaintext, aad)

    actual suspend fun open(key: ByteArray, nonce: ByteArray, ciphertextAndTag: ByteArray, aad: ByteArray): ByteArray =
        webCryptoAesGcmOpen(key, nonce, ciphertextAndTag, aad)
}
