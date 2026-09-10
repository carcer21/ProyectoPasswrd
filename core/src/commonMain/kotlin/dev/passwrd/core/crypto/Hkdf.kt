package dev.passwrd.core.crypto

/**
 * HKDF-SHA256 (RFC 5869), extract-and-expand. Código común puro: construido sobre
 * [hmacSha256], sin más dependencia de plataforma que esa.
 */
object Hkdf {
    private const val HASH_LEN = 32

    suspend fun extract(salt: ByteArray, ikm: ByteArray): ByteArray = hmacSha256(salt, ikm)

    suspend fun expand(prk: ByteArray, info: ByteArray, length: Int): ByteArray {
        require(length in 1..(255 * HASH_LEN)) { "length fuera de rango RFC 5869" }

        val blocks = (length + HASH_LEN - 1) / HASH_LEN
        val output = ByteArray(length)
        var previous = ByteArray(0)
        var written = 0

        for (i in 1..blocks) {
            val input = previous + info + byteArrayOf(i.toByte())
            val block = hmacSha256(prk, input)
            val toCopy = minOf(HASH_LEN, length - written)
            block.copyInto(output, written, 0, toCopy)
            written += toCopy
            previous = block
        }
        return output
    }

    suspend fun deriveKey(ikm: ByteArray, salt: ByteArray, info: ByteArray, length: Int): ByteArray {
        val prk = extract(salt, ikm)
        return expand(prk, info, length)
    }
}
