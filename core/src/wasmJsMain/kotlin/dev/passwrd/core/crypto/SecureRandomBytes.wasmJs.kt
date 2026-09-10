package dev.passwrd.core.crypto

actual fun secureRandomBytes(size: Int): ByteArray = webCryptoRandomBytes(size)
