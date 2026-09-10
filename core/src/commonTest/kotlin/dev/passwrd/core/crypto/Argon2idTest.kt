package dev.passwrd.core.crypto

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Vector oficial RFC 9106 sección 5.3 "Argon2id Test Vectors" (version 19, m=32KiB, t=3,
 * p=4 lanes). Verificado byte a byte contra el texto crudo del RFC antes de incluirlo aquí.
 * Nota: usa `secret` y `associatedData`, que el vault en producción no usa (ver
 * docs/CRYPTO_SPEC.md) — sólo existen en la API para poder ejecutar este vector.
 */
class Argon2idTest {

    // El vector oficial RFC 9106 (con secret/associatedData) vive en jvmTest: hash-wasm
    // (actual de wasmJs) no soporta esos dos parámetros — ver docs/adr/0007.

    @Test
    fun defaultParamsProduceDeterministicOutput() = runTest {
        val password = "correct horse battery staple".encodeToByteArray()
        val salt = secureRandomBytes(16)
        // Parámetros reducidos: el objetivo del test es determinismo, no el coste real.
        val params = Argon2Params(memoryKib = 8 * 1024, iterations = 2, parallelism = 1)

        val a = Argon2id.derive(password, salt, params)
        val b = Argon2id.derive(password, salt, params)

        assertEquals(a.toHex(), b.toHex())
    }

    @Test
    fun differentPasswordsProduceDifferentKeys() = runTest {
        val salt = secureRandomBytes(16)
        val params = Argon2Params(memoryKib = 8 * 1024, iterations = 2, parallelism = 1)

        val a = Argon2id.derive("password-a".encodeToByteArray(), salt, params)
        val b = Argon2id.derive("password-b".encodeToByteArray(), salt, params)

        kotlin.test.assertNotEquals(a.toHex(), b.toHex())
    }
}
