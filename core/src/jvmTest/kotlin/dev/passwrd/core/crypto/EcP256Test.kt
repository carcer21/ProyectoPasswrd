package dev.passwrd.core.crypto

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * ECDSA plana no es determinista (nonce aleatorio por firma), así que aquí no hay vector
 * oficial que verificar — la prueba correcta es round-trip: firmar y verificar con JCA.
 */
class EcP256Test {

    @Test
    fun keysHaveExpectedFieldSize() = runTest {
        val keyPair = EcP256.generateKeyPair()
        assertEquals(32, keyPair.publicKeyX.size)
        assertEquals(32, keyPair.publicKeyY.size)
        assertTrue(keyPair.privateKeyPkcs8.isNotEmpty())
    }

    @Test
    fun signatureVerifiesWithMatchingPublicKey() = runTest {
        val keyPair = EcP256.generateKeyPair()
        val data = "authenticatorData || clientDataHash".encodeToByteArray()

        val signature = EcP256.sign(keyPair.privateKeyPkcs8, data)

        assertTrue(EcP256.verify(keyPair.publicKeyX, keyPair.publicKeyY, data, signature))
    }

    @Test
    fun signatureFailsWithWrongPublicKey() = runTest {
        val keyPair = EcP256.generateKeyPair()
        val otherKeyPair = EcP256.generateKeyPair()
        val data = "some assertion data".encodeToByteArray()

        val signature = EcP256.sign(keyPair.privateKeyPkcs8, data)

        assertFalse(EcP256.verify(otherKeyPair.publicKeyX, otherKeyPair.publicKeyY, data, signature))
    }

    @Test
    fun signatureFailsWithTamperedData() = runTest {
        val keyPair = EcP256.generateKeyPair()
        val data = "original data".encodeToByteArray()
        val signature = EcP256.sign(keyPair.privateKeyPkcs8, data)

        assertFalse(EcP256.verify(keyPair.publicKeyX, keyPair.publicKeyY, "tampered data".encodeToByteArray(), signature))
    }

    @Test
    fun differentKeyPairsProduceDifferentKeys() = runTest {
        val a = EcP256.generateKeyPair()
        val b = EcP256.generateKeyPair()
        assertFalse(a.publicKeyX.contentEquals(b.publicKeyX) && a.publicKeyY.contentEquals(b.publicKeyY))
    }
}
