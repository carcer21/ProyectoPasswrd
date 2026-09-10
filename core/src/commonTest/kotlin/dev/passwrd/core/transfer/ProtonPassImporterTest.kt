package dev.passwrd.core.transfer

import dev.passwrd.core.model.ItemPayload
import dev.passwrd.core.model.ItemType
import kotlin.test.Test
import kotlin.test.assertEquals

class ProtonPassImporterTest {
    private val export = """
        {
          "vaults": {
            "vault-1": {
              "name": "Personal",
              "items": [
                {
                  "itemId": "a",
                  "data": {
                    "type": "login",
                    "metadata": {"name": "GitHub", "note": "cuenta personal"},
                    "content": {
                      "itemUsername": "carcertor21",
                      "password": "hunter2",
                      "urls": ["https://github.com"],
                      "totpUri": "otpauth://totp/GitHub?secret=JBSWY3DPEHPK3PXP"
                    }
                  }
                },
                {
                  "itemId": "b",
                  "data": {
                    "type": "note",
                    "metadata": {"name": "Nota", "note": "contenido secreto"},
                    "content": {}
                  }
                },
                {
                  "itemId": "c",
                  "data": {
                    "type": "creditCard",
                    "metadata": {"name": "Visa"},
                    "content": {
                      "cardholderName": "Coti",
                      "number": "4111111111111111",
                      "expirationDate": "2030-12",
                      "verificationNumber": "123"
                    }
                  }
                }
              ]
            }
          }
        }
    """.trimIndent()

    @Test
    fun importsLoginNoteAndCard() {
        val items = ProtonPassImporter.import(export)

        assertEquals(3, items.size)

        val login = items[0]
        assertEquals(ItemType.LOGIN, login.type)
        assertEquals(
            ItemPayload.Login(
                name = "GitHub",
                username = "carcertor21",
                password = "hunter2",
                uris = listOf("https://github.com"),
                notes = "cuenta personal",
                totpSecret = "otpauth://totp/GitHub?secret=JBSWY3DPEHPK3PXP",
            ),
            login.payload,
        )

        assertEquals(ItemType.SECURE_NOTE, items[1].type)
        assertEquals(ItemPayload.SecureNote("Nota", "contenido secreto"), items[1].payload)

        val card = items[2].payload as ItemPayload.Card
        assertEquals("4111111111111111", card.number)
        assertEquals(12, card.expiryMonth)
        assertEquals(2030, card.expiryYear)
    }
}
