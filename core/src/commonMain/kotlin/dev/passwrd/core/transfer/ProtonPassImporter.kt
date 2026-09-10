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
 * Import de `data.json` de un export sin cifrar de Proton Pass (Ajustes → Exportar).
 * Estructura: `{ "vaults": { "<id>": { "items": [ { "data": { "type", "metadata", "content" } } ] } } }`.
 */
object ProtonPassImporter {
    private val json = Json { ignoreUnknownKeys = true }

    fun import(text: String): List<ImportedItem> {
        val root = json.parseToJsonElement(text).jsonObject
        val vaults = root["vaults"]?.jsonObject ?: return emptyList()
        return vaults.values.flatMap { vault ->
            vault.jsonObject["items"]?.jsonArray.orEmpty().mapNotNull { item ->
                item.jsonObject["data"]?.jsonObject?.let(::toImportedItem)
            }
        }
    }

    private fun toImportedItem(data: JsonObject): ImportedItem? {
        val metadata = data["metadata"]?.jsonObject
        val content = data["content"]?.jsonObject ?: JsonObject(emptyMap())
        val name = metadata?.get("name")?.jsonPrimitive?.contentOrNull ?: ""
        val note = metadata?.get("note")?.jsonPrimitive?.contentOrNull ?: ""

        return when (data["type"]?.jsonPrimitive?.contentOrNull) {
            "login" -> ImportedItem(
                ItemType.LOGIN,
                ItemPayload.Login(
                    name = name,
                    username = (content["itemUsername"] ?: content["itemEmail"])?.jsonPrimitive?.contentOrNull ?: "",
                    password = content["password"]?.jsonPrimitive?.contentOrNull ?: "",
                    uris = content["urls"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList(),
                    notes = note,
                    totpSecret = content["totpUri"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() },
                ),
            )
            "note" -> ImportedItem(ItemType.SECURE_NOTE, ItemPayload.SecureNote(name = name, content = note))
            "creditCard" -> {
                val (expMonth, expYear) = parseExpiry(content["expirationDate"]?.jsonPrimitive?.contentOrNull)
                ImportedItem(
                    ItemType.CARD,
                    ItemPayload.Card(
                        name = name,
                        cardholderName = content["cardholderName"]?.jsonPrimitive?.contentOrNull ?: "",
                        number = content["number"]?.jsonPrimitive?.contentOrNull ?: "",
                        expiryMonth = expMonth,
                        expiryYear = expYear,
                        securityCode = content["verificationNumber"]?.jsonPrimitive?.contentOrNull ?: "",
                    ),
                )
            }
            "identity" -> ImportedItem(
                ItemType.IDENTITY,
                ItemPayload.Identity(
                    name = name,
                    fullName = content["fullName"]?.jsonPrimitive?.contentOrNull ?: "",
                    email = content["email"]?.jsonPrimitive?.contentOrNull ?: "",
                    phone = content["phoneNumber"]?.jsonPrimitive?.contentOrNull ?: "",
                    address = content["streetAddress"]?.jsonPrimitive?.contentOrNull ?: "",
                ),
            )
            else -> null
        }
    }

    /** `expirationDate` de Proton Pass viene como "YYYY-MM". */
    private fun parseExpiry(value: String?): Pair<Int, Int> {
        val parts = value?.split("-") ?: return 0 to 0
        val year = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val month = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return month to year
    }
}
