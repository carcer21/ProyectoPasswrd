package dev.passwrd.core.webauthn

import kotlin.test.Test
import kotlin.test.assertTrue

class AttestationObjectTest {

    @Test
    fun containsAuthDataBytesVerbatim() {
        val authData = ByteArray(37) { it.toByte() }
        val encoded = AttestationObject.build(authData)

        assertTrue(containsSubsequence(encoded, authData), "authData debe aparecer tal cual dentro del objeto de atestación")
    }

    private fun containsSubsequence(haystack: ByteArray, needle: ByteArray): Boolean {
        if (needle.isEmpty() || needle.size > haystack.size) return false
        outer@ for (start in 0..haystack.size - needle.size) {
            for (i in needle.indices) {
                if (haystack[start + i] != needle[i]) continue@outer
            }
            return true
        }
        return false
    }
}
