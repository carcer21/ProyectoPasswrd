package dev.passwrd.core.crypto

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Vector oficial NIST CAVP (gcmEncryptExtIV256.rsp, Keylen=256/IVlen=96/PTlen=128/AADlen=0,
 * Count=0). Descargado y verificado byte a byte del fichero .rsp original antes de
 * incluirlo aquí.
 */
class Aes256GcmTest {

    @Test
    fun nistCavpVector() = runTest {
        val key = hex("31bdadd96698c204aa9ce1448ea94ae1fb4a9a0b3c9d773b51bb1822666b8f22")
        val iv = hex("0d18e06c7c725ac9e362e1ce")
        val plaintext = hex("2db5168e932556f8089a0622981d017d")
        val expectedCiphertext = hex("fa4362189661d163fcd6a56d8bf0405a")
        val expectedTag = hex("d636ac1bbedd5cc3ee727dc2ab4a9489")

        val sealed = Aes256Gcm.seal(key, iv, plaintext)
        assertEquals((expectedCiphertext + expectedTag).toHex(), sealed.toHex())

        val opened = Aes256Gcm.open(key, iv, sealed)
        assertEquals(plaintext.toHex(), opened.toHex())
    }

    @Test
    fun tamperedCiphertextFailsAuthentication() = runTest {
        val key = secureRandomBytes(32)
        val nonce = secureRandomBytes(12)
        val sealed = Aes256Gcm.seal(key, nonce, "secreto".encodeToByteArray()).copyOf()
        sealed[0] = sealed[0].xor(0x01)

        assertFailsWith<AuthenticationFailedException> {
            Aes256Gcm.open(key, nonce, sealed)
        }
    }

    @Test
    fun wrongKeyFailsAuthentication() = runTest {
        val nonce = secureRandomBytes(12)
        val sealed = Aes256Gcm.seal(secureRandomBytes(32), nonce, "secreto".encodeToByteArray())

        assertFailsWith<AuthenticationFailedException> {
            Aes256Gcm.open(secureRandomBytes(32), nonce, sealed)
        }
    }

    private fun Byte.xor(other: Int): Byte = (this.toInt() xor other).toByte()
}
