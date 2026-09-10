package dev.passwrd.core.transfer

import dev.passwrd.core.model.ItemPayload
import dev.passwrd.core.model.ItemType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Import de un export JSON *sin cifrar* de Bitwarden (Ajustes → Exportar bóveda → .json).
 * `type`: 1=login, 2=nota segura, 3=tarjeta, 4=identidad — igual en toda la historia del formato.
 */
object BitwardenImporter {
    private val json = Json { ignoreUnknownKeys = true }

    fun import(text: String): List<ImportedItem> {
        val root = json.parseToJsonElement(text).jsonObject
        val items = root["items"]?.jsonArray ?: return emptyList()
        return items.mapNotNull { toImportedItem(it.jsonObject) }
    }

    private fun toImportedItem(obj: JsonObject): ImportedItem? {
        val name = obj["name"]?.jsonPrimitive?.contentOrNull ?: ""
        val notes = obj["notes"]?.jsonPrimitive?.contentOrNull ?: ""

        return when (obj["type"]?.jsonPrimitive?.contentOrNull?.toIntOrNull()) {
            1 -> {
                val login = obj["login"]?.jsonObject ?: return null
                val uris = login["uris"]?.jsonArray
                    ?.mapNotNull { it.jsonObject["uri"]?.jsonPrimitive?.contentOrNull }
                    ?: emptyList()
                ImportedItem(
                    ItemType.LOGIN,
                    ItemPayload.Login(
                        name = name,
                        username = login["username"]?.jsonPrimitive?.contentOrNull ?: "",
                        password = login["password"]?.jsonPrimitive?.contentOrNull ?: "",
                        uris = uris,
                        notes = notes,
                        totpSecret = login["totp"]?.jsonPrimitive?.contentOrNull,
                    ),
                )
            }
            2 -> ImportedItem(ItemType.SECURE_NOTE, ItemPayload.SecureNote(name = name, content = notes))
            3 -> {
                val card = obj["card"]?.jsonObject ?: return null
                val expMonth = card["expMonth"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0
                val expYear = card["expYear"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0
                ImportedItem(
                    ItemType.CARD,
                    ItemPayload.Card(
                        name = name,
                        cardholderName = card["cardholderName"]?.jsonPrimitive?.contentOrNull ?: "",
                        number = card["number"]?.jsonPrimitive?.contentOrNull ?: "",
                        expiryMonth = expMonth,
                        expiryYear = expYear,
                        securityCode = card["code"]?.jsonPrimitive?.contentOrNull ?: "",
                    ),
                )
            }
            4 -> {
                val identity = obj["identity"]?.jsonObject ?: return null
                val fullName = listOfNotNull(
                    identity["firstName"]?.jsonPrimitive?.contentOrNull,
                    identity["lastName"]?.jsonPrimitive?.contentOrNull,
                ).joinToString(" ")
                val address = listOfNotNull(
                    identity["address1"]?.jsonPrimitive?.contentOrNull,
                    identity["city"]?.jsonPrimitive?.contentOrNull,
                    identity["postalCode"]?.jsonPrimitive?.contentOrNull,
                    identity["country"]?.jsonPrimitive?.contentOrNull,
                ).joinToString(", ")
                ImportedItem(
                    ItemType.IDENTITY,
                    ItemPayload.Identity(
                        name = name,
                        fullName = fullName,
                        email = identity["email"]?.jsonPrimitive?.contentOrNull ?: "",
                        phone = identity["phone"]?.jsonPrimitive?.contentOrNull ?: "",
                        address = address,
                    ),
                )
            }
            else -> null
        }
    }
}
