package dev.passwrd.core.crypto

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Vector oficial RFC 5869 Apéndice A.1 "Basic test case with SHA-256". Verificado byte a
 * byte contra el texto crudo del RFC (no vía resumen de otra herramienta) antes de
 * incluirlo aquí.
 */
class HkdfTest {

    @Test
    fun rfc5869TestCase1() = runTest {
        val ikm = hex("0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b")
        val salt = hex("000102030405060708090a0b0c")
        val info = hex("f0f1f2f3f4f5f6f7f8f9")
        val expectedPrk = hex("077709362c2e32df0ddc3f0dc47bba6390b6c73bb50f9c3122ec844ad7c2b3e5")
        val expectedOkm = hex(
            "3cb25f25faacd57a90434f64d0362f2a2d2d0a90cf1a5a4c5db02d56ecc4c5bf34007208d5b887185865"
        )

        val prk = Hkdf.extract(salt, ikm)
        assertEquals(expectedPrk.toHex(), prk.toHex())

        val okm = Hkdf.expand(prk, info, 42)
        assertEquals(expectedOkm.toHex(), okm.toHex())

        assertEquals(expectedOkm.toHex(), Hkdf.deriveKey(ikm, salt, info, 42).toHex())
    }

    @Test
    fun deriveKeyIsDeterministic() = runTest {
        val ikm = secureRandomBytes(32)
        val salt = secureRandomBytes(16)
        val info = "passwrd.v1.kek".encodeToByteArray()

        val a = Hkdf.deriveKey(ikm, salt, info, 32)
        val b = Hkdf.deriveKey(ikm, salt, info, 32)

        assertEquals(a.toHex(), b.toHex())
    }
}
