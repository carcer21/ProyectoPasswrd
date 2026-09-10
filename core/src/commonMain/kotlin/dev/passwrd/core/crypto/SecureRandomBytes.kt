package dev.passwrd.core.crypto

/** CSPRNG del sistema. Nunca `kotlin.random.Random` (no es criptográficamente seguro). */
expect fun secureRandomBytes(size: Int): ByteArray
