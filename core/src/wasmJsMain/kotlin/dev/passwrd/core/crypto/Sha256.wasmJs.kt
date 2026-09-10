package dev.passwrd.core.crypto

actual suspend fun sha256(data: ByteArray): ByteArray = webCryptoSha256(data)
