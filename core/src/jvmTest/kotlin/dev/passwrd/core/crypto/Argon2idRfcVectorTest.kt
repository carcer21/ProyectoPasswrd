package dev.passwrd.core.crypto

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Vector oficial RFC 9106 sección 5.3 "Argon2id Test Vectors" (version 19, m=32KiB, t=3,
 * p=4 lanes). Verificado byte a byte contra el texto crudo del RFC antes de incluirlo aquí.
 * Sólo en jvmTest: usa `secret`/`associatedData`, que hash-wasm (actual de wasmJs) no
 * soporta — ver docs/adr/0007. El uso en producción (ver docs/CRYPTO_SPEC.md) no los usa.
 */
class Argon2idRfcVectorTest {

    @Test
    fun rfc9106TestVector() = runTest {
        val password = ByteArray(32) { 0x01 }
        val salt = ByteArray(16) { 0x02 }
        val secret = ByteArray(8) { 0x03 }
        val associatedData = ByteArray(12) { 0x04 }
        val params = Argon2Params(memoryKib = 32, iterations = 3, parallelism = 4, outputLength = 32)

        val expectedTag = hex("0d640df58d78766c08c037a34a8b53c9d01ef0452d75b65eb52520e96b01e659")

        val tag = Argon2id.derive(password, salt, params, secret, associatedData)

        assertEquals(expectedTag.toHex(), tag.toHex())
    }
}
