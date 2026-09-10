package dev.passwrd.core.transfer

import dev.passwrd.core.model.ItemPayload
import dev.passwrd.core.model.ItemType
import kotlin.test.Test
import kotlin.test.assertEquals

class BitwardenImporterTest {
    private val export = """
        {
          "encrypted": false,
          "items": [
            {
              "type": 1,
              "name": "GitHub",
              "notes": "cuenta personal",
              "login": {
                "username": "carcertor21",
                "password": "hunter2",
                "totp": "JBSWY3DPEHPK3PXP",
                "uris": [{"match": null, "uri": "https://github.com"}]
              }
            },
            {
              "type": 2,
              "name": "Nota",
              "notes": "contenido secreto"
            },
            {
              "type": 3,
              "name": "Visa",
              "notes": "",
              "card": {
                "cardholderName": "Coti",
                "number": "4111111111111111",
                "expMonth": "12",
                "expYear": "2030",
                "code": "123"
              }
            }
          ]
        }
    """.trimIndent()

    @Test
    fun importsLoginNoteAndCard() {
        val items = BitwardenImporter.import(export)

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
                totpSecret = "JBSWY3DPEHPK3PXP",
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
