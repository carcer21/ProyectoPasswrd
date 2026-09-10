package dev.passwrd.core.webauthn

import dev.passwrd.core.crypto.sha256
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** clientDataJSON (WebAuthn §5.8.1). */
object ClientDataJson {
    fun build(type: String, challenge: ByteArray, origin: String, androidPackageName: String? = null): ByteArray {
        val json: JsonObject = buildJsonObject {
            put("type", type)
            put("challenge", Base64Url.encode(challenge))
            put("origin", origin)
            androidPackageName?.let { put("androidPackageName", it) }
        }
        return Json.encodeToString(JsonObject.serializer(), json).encodeToByteArray()
    }

    suspend fun hash(clientDataJson: ByteArray): ByteArray = sha256(clientDataJson)
}
