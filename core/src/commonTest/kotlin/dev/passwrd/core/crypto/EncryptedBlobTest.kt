package dev.passwrd.core.crypto

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class EncryptedBlobTest {

    @Test
    fun roundTrip() = runTest {
        val key = secureRandomBytes(32)
        val plaintext = """{"user":"a@b.com","password":"hunter2"}""".encodeToByteArray()

        val blob = EncryptedBlob.encrypt(key, plaintext)
        val decrypted = EncryptedBlob.decrypt(key, blob)

        assertEquals(plaintext.toHex(), decrypted.toHex())
    }

    @Test
    fun formatStartsWithVersionByte() = runTest {
        val blob = EncryptedBlob.encrypt(secureRandomBytes(32), "x".encodeToByteArray())
        assertEquals(EncryptedBlob.VERSION, blob[0])
    }

    @Test
    fun eachCallUsesFreshNonce() = runTest {
        val key = secureRandomBytes(32)
        val a = EncryptedBlob.encrypt(key, "same plaintext".encodeToByteArray())
        val b = EncryptedBlob.encrypt(key, "same plaintext".encodeToByteArray())

        assertTrue(a.toHex() != b.toHex(), "dos cifrados del mismo texto no deben coincidir (nonce distinto)")
    }

    @Test
    fun wrongKeyFailsToDecrypt() = runTest {
        val blob = EncryptedBlob.encrypt(secureRandomBytes(32), "secreto".encodeToByteArray())

        assertFailsWith<AuthenticationFailedException> {
            EncryptedBlob.decrypt(secureRandomBytes(32), blob)
        }
    }

    @Test
    fun aadMismatchFailsToDecrypt() = runTest {
        val key = secureRandomBytes(32)
        val blob = EncryptedBlob.encrypt(key, "secreto".encodeToByteArray(), aad = "item-1".encodeToByteArray())

        assertFailsWith<AuthenticationFailedException> {
            EncryptedBlob.decrypt(key, blob, aad = "item-2".encodeToByteArray())
        }
    }
}
