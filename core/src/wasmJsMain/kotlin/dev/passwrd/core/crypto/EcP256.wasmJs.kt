package dev.passwrd.core.crypto

actual object EcP256 {
    actual suspend fun generateKeyPair(): EcKeyPair {
        val pair = webCryptoGenerateEcKeyPair()
        // raw = 0x04 || X(32) || Y(32) — ver RFC 5480 §2.2 (punto sin comprimir).
        val raw = pair.publicKeyRaw
        return EcKeyPair(
            privateKeyPkcs8 = pair.privateKeyPkcs8,
            publicKeyX = raw.copyOfRange(1, 33),
            publicKeyY = raw.copyOfRange(33, 65),
        )
    }

    actual suspend fun sign(privateKeyPkcs8: ByteArray, data: ByteArray): ByteArray {
        val raw = webCryptoEcSignRaw(privateKeyPkcs8, data)
        return rawSignatureToDer(raw)
    }

    actual suspend fun verify(publicKeyX: ByteArray, publicKeyY: ByteArray, data: ByteArray, signature: ByteArray): Boolean {
        val rawKey = byteArrayOf(0x04) + publicKeyX + publicKeyY
        val rawSignature = try {
            derSignatureToRaw(signature)
        } catch (e: Exception) {
            return false
        }
        return webCryptoEcVerifyRaw(rawKey, data, rawSignature)
    }
}

/** WebCrypto firma en raw IEEE P1363 (r||s); WebAuthn/JCA piden DER (SEQUENCE de dos INTEGER). */
private fun rawSignatureToDer(raw: ByteArray): ByteArray {
    require(raw.size == 64) { "firma raw ECDSA debe ser 64 bytes (r||s), es ${raw.size}" }
    val r = derInteger(raw.copyOfRange(0, 32))
    val s = derInteger(raw.copyOfRange(32, 64))
    val content = r + s
    return byteArrayOf(0x30, content.size.toByte()) + content
}

private fun derInteger(fixed32: ByteArray): ByteArray {
    var bytes = fixed32
    var start = 0
    while (start < bytes.size - 1 && bytes[start] == 0.toByte()) start++
    bytes = bytes.copyOfRange(start, bytes.size)
    if (bytes[0].toInt() and 0x80 != 0) bytes = byteArrayOf(0) + bytes
    return byteArrayOf(0x02, bytes.size.toByte()) + bytes
}

private fun derSignatureToRaw(der: ByteArray): ByteArray {
    require(der.size >= 8 && der[0] == 0x30.toByte()) { "firma no es DER SEQUENCE" }
    val (r, nextOffset) = readDerInteger(der, 2)
    val (s, _) = readDerInteger(der, nextOffset)
    return r.toFixed32() + s.toFixed32()
}

private fun readDerInteger(der: ByteArray, offset: Int): Pair<ByteArray, Int> {
    require(der[offset] == 0x02.toByte()) { "esperaba INTEGER en firma DER" }
    val len = der[offset + 1].toInt() and 0xFF
    val start = offset + 2
    return der.copyOfRange(start, start + len) to (start + len)
}

private fun ByteArray.toFixed32(): ByteArray {
    val stripped = dropWhile { it == 0.toByte() }.toByteArray()
    return when {
        stripped.size == 32 -> stripped
        stripped.size < 32 -> ByteArray(32 - stripped.size) + stripped
        else -> stripped.copyOfRange(stripped.size - 32, stripped.size)
    }
}
