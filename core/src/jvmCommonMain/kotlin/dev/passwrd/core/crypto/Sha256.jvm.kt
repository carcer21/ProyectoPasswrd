package dev.passwrd.core.crypto

import java.security.MessageDigest

actual suspend fun sha256(data: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").digest(data)
