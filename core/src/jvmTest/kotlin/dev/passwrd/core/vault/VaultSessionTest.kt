package dev.passwrd.core.vault

import dev.passwrd.core.crypto.Argon2Params
import dev.passwrd.core.storage.PasswrdDatabase
import dev.passwrd.core.storage.RoomVaultStore
import dev.passwrd.core.storage.buildVaultDatabase
import dev.passwrd.core.storage.inMemoryVaultDatabaseBuilder
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Parámetros mínimos para que los tests no paguen el coste real de Argon2id. */
private val fastKdf = Argon2Params(memoryKib = 8 * 1024, iterations = 1, parallelism = 1)

class VaultSessionTest {
    private lateinit var db: PasswrdDatabase

    @BeforeTest
    fun setUp() {
        db = inMemoryVaultDatabaseBuilder().buildVaultDatabase()
    }

    @AfterTest
    fun tearDown() {
        db.close()
    }

    @Test
    fun createThenLockThenUnlockWithCorrectPassword() = runBlocking {
        val session = VaultSession(RoomVaultStore(db))
        val password = "correct horse battery staple".encodeToByteArray()

        session.createVault(password, fastKdf)
        assertTrue(session.isUnlocked)

        session.lock()
        assertFalse(session.isUnlocked)

        val ok = session.unlock(password.copyOf())
        assertTrue(ok)
        assertTrue(session.isUnlocked)
    }

    @Test
    fun unlockWithWrongPasswordFails() = runBlocking {
        val session = VaultSession(RoomVaultStore(db))
        session.createVault("real-password".encodeToByteArray(), fastKdf)
        session.lock()

        val ok = session.unlock("wrong-password".encodeToByteArray())

        assertFalse(ok)
        assertFalse(session.isUnlocked)
    }

    @Test
    fun changeMasterPasswordThenUnlockWithNewPasswordOnly() = runBlocking {
        val session = VaultSession(RoomVaultStore(db))
        session.createVault("old-password".encodeToByteArray(), fastKdf)

        session.changeMasterPassword("new-password".encodeToByteArray(), fastKdf)
        session.lock()

        assertFalse(session.unlock("old-password".encodeToByteArray()))
        assertTrue(session.unlock("new-password".encodeToByteArray()))
    }
}
