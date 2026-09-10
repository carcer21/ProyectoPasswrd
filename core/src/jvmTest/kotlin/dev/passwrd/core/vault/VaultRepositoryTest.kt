package dev.passwrd.core.vault

import dev.passwrd.core.crypto.AuthenticationFailedException
import dev.passwrd.core.crypto.Argon2Params
import dev.passwrd.core.model.ItemPayload
import dev.passwrd.core.model.ItemType
import dev.passwrd.core.model.VaultItem
import dev.passwrd.core.storage.PasswrdDatabase
import dev.passwrd.core.storage.RoomVaultStore
import dev.passwrd.core.storage.buildVaultDatabase
import dev.passwrd.core.storage.inMemoryVaultDatabaseBuilder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

private val fastKdf = Argon2Params(memoryKib = 8 * 1024, iterations = 1, parallelism = 1)

class VaultRepositoryTest {
    private lateinit var db: PasswrdDatabase
    private lateinit var session: VaultSession
    private lateinit var repository: VaultRepository

    @BeforeTest
    fun setUp() = runBlocking {
        db = inMemoryVaultDatabaseBuilder().buildVaultDatabase()
        val store = RoomVaultStore(db)
        session = VaultSession(store)
        repository = VaultRepository(store, session)
        session.createVault("master-password".encodeToByteArray(), fastKdf)
    }

    @AfterTest
    fun tearDown() {
        db.close()
    }

    @Test
    fun upsertThenReadBackMatchesOriginal() = runBlocking {
        val login = ItemPayload.Login(
            name = "GitHub",
            username = "carcertor21",
            password = "hunter2",
            uris = listOf("https://github.com"),
        )
        val item = VaultItem(
            id = "item-1",
            type = ItemType.LOGIN,
            payload = login,
            createdAt = 1000,
            updatedAt = 1000,
            revision = 1,
        )

        repository.upsert(item)
        val loaded = repository.findById("item-1")

        assertEquals(item, loaded)
    }

    @Test
    fun deletedItemIsExcludedFromActiveList() = runBlocking {
        val item = VaultItem(
            id = "item-1",
            type = ItemType.SECURE_NOTE,
            payload = ItemPayload.SecureNote("nota", "contenido"),
            createdAt = 1000,
            updatedAt = 1000,
            revision = 1,
        )
        repository.upsert(item)
        repository.delete("item-1")

        val active = repository.observeActive().first()
        assertEquals(emptyList(), active)
    }

    /** Fase 2 del plan: "detección de manipulación". */
    @Test
    fun tamperedPayloadFailsToDecrypt() = runBlocking {
        val item = VaultItem(
            id = "item-1",
            type = ItemType.LOGIN,
            payload = ItemPayload.Login(name = "x", username = "y", password = "z"),
            createdAt = 1000,
            updatedAt = 1000,
            revision = 1,
        )
        repository.upsert(item)

        val stored = db.vaultItemDao().findById("item-1")!!
        val tamperedPayload = stored.encPayload.copyOf().also { it[it.size - 1] = it[it.size - 1].inc() }
        db.vaultItemDao().upsert(stored.copy(encPayload = tamperedPayload))

        assertFailsWith<AuthenticationFailedException> {
            repository.findById("item-1")
        }
        Unit
    }

    @Test
    fun swappingCiphertextBetweenItemsFailsAuthentication() = runBlocking {
        val a = VaultItem("item-a", ItemType.SECURE_NOTE, ItemPayload.SecureNote("a", "contenido a"), 1000, 1000, 1)
        val b = VaultItem("item-b", ItemType.SECURE_NOTE, ItemPayload.SecureNote("b", "contenido b"), 1000, 1000, 1)
        repository.upsert(a)
        repository.upsert(b)

        val storedA = db.vaultItemDao().findById("item-a")!!
        val storedB = db.vaultItemDao().findById("item-b")!!
        // El AAD liga cada blob a su propio id: mover el ciphertext de b a la fila de a
        // debe fallar la autenticación, no descifrar contenido ajeno.
        db.vaultItemDao().upsert(storedA.copy(encPayload = storedB.encPayload, wrappedItemKey = storedB.wrappedItemKey))

        assertFailsWith<AuthenticationFailedException> {
            repository.findById("item-a")
        }
        Unit
    }

    @Test
    fun missingItemReturnsNull() = runBlocking {
        assertNull(repository.findById("no-existe"))
    }
}
