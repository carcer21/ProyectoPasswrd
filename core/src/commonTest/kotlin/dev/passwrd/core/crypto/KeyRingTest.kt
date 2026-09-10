package dev.passwrd.core.crypto

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KeyRingTest {

    @Test
    fun locksAndZeroizesKey() {
        val vaultKey = secureRandomBytes(32).also { it[0] = 0x42 }
        val ring = KeyRing.unlocked(vaultKey)

        assertTrue(ring.isUnlocked)
        ring.lock()

        assertFalse(ring.isUnlocked)
        assertTrue(vaultKey.all { it == 0.toByte() }, "la clave debe quedar a cero tras lock()")
    }

    @Test
    fun requireVaultKeyThrowsWhenLocked() {
        val ring = KeyRing.locked()
        assertFailsWith<IllegalStateException> { ring.requireVaultKey() }
    }
}
