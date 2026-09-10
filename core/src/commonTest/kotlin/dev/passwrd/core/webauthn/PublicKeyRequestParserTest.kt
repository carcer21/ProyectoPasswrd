package dev.passwrd.core.webauthn

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PublicKeyRequestParserTest {

    @Test
    fun parsesCreationOptions() {
        val json = """
            {
              "rp": {"name": "Example", "id": "example.com"},
              "user": {"id": "dXNlci0xMjM", "name": "user@example.com", "displayName": "Test User"},
              "challenge": "Y2hhbGxlbmdl",
              "pubKeyCredParams": [{"type": "public-key", "alg": -7}],
              "timeout": 60000,
              "attestation": "none"
            }
        """.trimIndent()

        val options = PublicKeyRequestParser.parseCreationOptions(json)

        assertEquals("example.com", options.rpId)
        assertEquals("dXNlci0xMjM", options.userIdBase64)
        assertEquals("user@example.com", options.userName)
        assertEquals("Test User", options.userDisplayName)
        assertEquals("Y2hhbGxlbmdl", options.challengeBase64)
    }

    @Test
    fun fallsBackToRpNameWhenIdMissing() {
        val json = """{"rp": {"name": "example.com"}, "user": {"id": "aWQ", "name": "u"}, "challenge": "Yw"}"""
        assertEquals("example.com", PublicKeyRequestParser.parseCreationOptions(json).rpId)
    }

    @Test
    fun parsesRequestOptionsWithAllowCredentials() {
        val json = """
            {
              "challenge": "Y2hhbGxlbmdl",
              "rpId": "example.com",
              "allowCredentials": [{"type": "public-key", "id": "Y3JlZC0x"}, {"type": "public-key", "id": "Y3JlZC0y"}]
            }
        """.trimIndent()

        val options = PublicKeyRequestParser.parseRequestOptions(json)

        assertEquals("example.com", options.rpId)
        assertEquals("Y2hhbGxlbmdl", options.challengeBase64)
        assertEquals(listOf("Y3JlZC0x", "Y3JlZC0y"), options.allowedCredentialIds)
    }

    @Test
    fun parsesRequestOptionsWithoutAllowCredentials() {
        val json = """{"challenge": "Yw", "rpId": "example.com"}"""
        val options = PublicKeyRequestParser.parseRequestOptions(json)
        assertTrue(options.allowedCredentialIds.isEmpty())
    }
}
