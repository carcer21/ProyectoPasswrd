package dev.passwrd.core.transfer

import dev.passwrd.core.crypto.Argon2Params
import dev.passwrd.core.crypto.AuthenticationFailedException
import dev.passwrd.core.model.ItemPayload
import dev.passwrd.core.model.ItemType
import dev.passwrd.core.model.VaultItem
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

private val fastKdf = Argon2Params(memoryKib = 8 * 1024, iterations = 1, parallelism = 1)

class VaultExportTest {
    private val items = listOf(
        VaultItem(
            id = "item-1",
            type = ItemType.LOGIN,
            payload = ItemPayload.Login(
                name = "GitHub",
                username = "carcertor21",
                password = "hunter2",
                uris = listOf("https://github.com"),
            ),
            createdAt = 1000,
            updatedAt = 2000,
            revision = 3,
        ),
        VaultItem(
            id = "item-2",
            type = ItemType.SECURE_NOTE,
            payload = ItemPayload.SecureNote(name = "nota", content = "contenido"),
            createdAt = 1000,
            updatedAt = 1000,
            revision = 1,
        ),
        VaultItem(
            id = "item-3",
            type = ItemType.SECURE_NOTE,
            payload = ItemPayload.SecureNote(name = "borrada", content = "no debe exportarse"),
            createdAt = 1000,
            updatedAt = 1000,
            revision = 1,
            deletedAt = 5000,
        ),
    )

    @Test
    fun exportThenImportRoundTripsWithoutLoss() = runTest {
        val exported = VaultExporter.export(items, "export-pass".encodeToByteArray(), fastKdf)
        val imported = VaultImporter.import(exported, "export-pass".encodeToByteArray())

        assertEquals(2, imported.size)
        val login = imported.first { it.id == "item-1" }
        assertEquals(ItemType.LOGIN, login.type)
        assertEquals(items[0].payload, login.payload)
        assertEquals(items[0].createdAt, login.createdAt)
        assertEquals(items[0].updatedAt, login.updatedAt)
        assertTrue(imported.none { it.id == "item-3" })
    }

    @Test
    fun importWithWrongPasswordFails() = runTest {
        val exported = VaultExporter.export(items, "export-pass".encodeToByteArray(), fastKdf)

        assertFailsWith<AuthenticationFailedException> {
            VaultImporter.import(exported, "wrong-pass".encodeToByteArray())
        }
    }
}
