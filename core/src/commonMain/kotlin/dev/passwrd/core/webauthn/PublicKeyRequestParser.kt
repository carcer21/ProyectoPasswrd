package dev.passwrd.core.webauthn

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class CreationOptions(
    val rpId: String,
    val userIdBase64: String,
    val userName: String,
    val userDisplayName: String,
    val challengeBase64: String,
)

data class RequestOptions(
    val rpId: String,
    val challengeBase64: String,
    /** IDs (base64url) de `allowCredentials`; vacío = cualquier credencial del RP sirve. */
    val allowedCredentialIds: List<String>,
)

/**
 * Parsea el `requestJson` que entrega Credential Manager — sigue el formato estándar de
 * `PublicKeyCredentialCreationOptionsJSON` / `...RequestOptionsJSON` del propio WebAuthn.
 * Sólo se leen los campos que usamos en v1 (sin `authenticatorSelection`, `extensions`,
 * `excludeCredentials`, preferencia de atestación, etc. — ver docs/CRYPTO_SPEC.md "Passkeys").
 */
object PublicKeyRequestParser {
    fun parseCreationOptions(requestJson: String): CreationOptions {
        val root = Json.parseToJsonElement(requestJson).jsonObject
        val rp = root.getValue("rp").jsonObject
        val user = root.getValue("user").jsonObject
        return CreationOptions(
            rpId = rp["id"]?.jsonPrimitive?.content ?: rp.getValue("name").jsonPrimitive.content,
            userIdBase64 = user.getValue("id").jsonPrimitive.content,
            userName = user.getValue("name").jsonPrimitive.content,
            userDisplayName = user["displayName"]?.jsonPrimitive?.content ?: user.getValue("name").jsonPrimitive.content,
            challengeBase64 = root.getValue("challenge").jsonPrimitive.content,
        )
    }

    fun parseRequestOptions(requestJson: String): RequestOptions {
        val root = Json.parseToJsonElement(requestJson).jsonObject
        val allowCredentials = root["allowCredentials"]?.jsonArray?.map { it.jsonObject.getValue("id").jsonPrimitive.content }
            ?: emptyList()
        return RequestOptions(
            rpId = root["rpId"]?.jsonPrimitive?.content ?: "",
            challengeBase64 = root.getValue("challenge").jsonPrimitive.content,
            allowedCredentialIds = allowCredentials,
        )
    }
}
