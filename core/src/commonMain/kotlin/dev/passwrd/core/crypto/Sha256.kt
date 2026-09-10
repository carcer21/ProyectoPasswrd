package dev.passwrd.core.crypto

expect suspend fun sha256(data: ByteArray): ByteArray
