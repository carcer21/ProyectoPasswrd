package dev.passwrd.core.crypto

/** HMAC-SHA256, primitiva base de [Hkdf]. */
expect suspend fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray
