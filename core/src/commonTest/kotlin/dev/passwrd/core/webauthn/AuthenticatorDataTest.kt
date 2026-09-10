package dev.passwrd.core.webauthn

import dev.passwrd.core.crypto.sha256
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AuthenticatorDataTest {

    @Test
    fun assertionHasNoAttestedCredentialDataFlag() = runTest {
        val data = AuthenticatorData.forAssertion("example.com", signCount = 5)

        assertEquals(37, data.size) // 32 rpIdHash + 1 flags + 4 signCount, sin datos de credencial
        assertEquals(sha256("example.com".encodeToByteArray()).toList(), data.copyOfRange(0, 32).toList())

        val flags = data[32].toInt() and 0xFF
        assertEquals(0x01 or 0x04, flags) // UP + UV, sin AT (0x40)

        val signCount = data.copyOfRange(33, 37)
        assertEquals(listOf(0, 0, 0, 5), signCount.map { it.toInt() })
    }

    @Test
    fun registrationHasAttestedCredentialDataFlagAndPayload() = runTest {
        val credentialId = ByteArray(16) { it.toByte() }
        val cosePublicKey = CoseKey.encodeP256PublicKey(ByteArray(32) { 1 }, ByteArray(32) { 2 })

        val data = AuthenticatorData.forRegistration("example.com", signCount = 0, credentialId, cosePublicKey)

        val flags = data[32].toInt() and 0xFF
        assertTrue(flags and 0x40 != 0) // AT presente

        val aaguid = data.copyOfRange(37, 53)
        assertTrue(aaguid.all { it == 0.toByte() })

        val credIdLen = ((data[53].toInt() and 0xFF) shl 8) or (data[54].toInt() and 0xFF)
        assertEquals(16, credIdLen)

        val extractedCredId = data.copyOfRange(55, 55 + credIdLen)
        assertEquals(credentialId.toList(), extractedCredId.toList())

        val extractedCoseKey = data.copyOfRange(55 + credIdLen, data.size)
        assertEquals(cosePublicKey.toList(), extractedCoseKey.toList())
    }
}
