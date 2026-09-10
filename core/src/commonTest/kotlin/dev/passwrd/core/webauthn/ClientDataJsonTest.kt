package dev.passwrd.core.webauthn

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ClientDataJsonTest {

    @Test
    fun containsTypeChallengeAndOrigin() {
        val challenge = byteArrayOf(1, 2, 3, 4)
        val json = ClientDataJson.build("webauthn.create", challenge, "https://example.com").decodeToString()

        assertTrue(json.contains("\"type\":\"webauthn.create\""))
        assertTrue(json.contains("\"origin\":\"https://example.com\""))
        assertTrue(json.contains("\"challenge\":\"${Base64Url.encode(challenge)}\""))
    }

    @Test
    fun omitsAndroidPackageNameWhenNotProvided() {
        val json = ClientDataJson.build("webauthn.get", byteArrayOf(1), "https://example.com").decodeToString()
        assertTrue(!json.contains("androidPackageName"))
    }

    @Test
    fun includesAndroidPackageNameWhenProvided() {
        val json = ClientDataJson.build(
            "webauthn.create",
            byteArrayOf(1),
            "https://example.com",
            androidPackageName = "com.example.app",
        ).decodeToString()
        assertTrue(json.contains("\"androidPackageName\":\"com.example.app\""))
    }

    @Test
    fun hashIsSha256OfTheJsonBytes() = runTest {
        val clientDataJson = ClientDataJson.build("webauthn.get", byteArrayOf(9, 9), "https://a.example")
        val hash = ClientDataJson.hash(clientDataJson)
        assertEquals(32, hash.size)
    }
}
