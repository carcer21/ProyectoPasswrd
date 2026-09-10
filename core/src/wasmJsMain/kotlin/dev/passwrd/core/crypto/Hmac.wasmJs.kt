package dev.passwrd.core.crypto

actual suspend fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray {
    // RFC 5869 permite salt/ikm vacío; WebCrypto exige una clave HMAC no vacía, igual que JCA.
    val effectiveKey = if (key.isEmpty()) ByteArray(32) else key
    return webCryptoHmacSha256(effectiveKey, data)
}
