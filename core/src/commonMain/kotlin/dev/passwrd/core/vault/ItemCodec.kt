package dev.passwrd.core.vault

import dev.passwrd.core.model.ItemPayload
import kotlinx.serialization.json.Json

internal object ItemCodec {
    private val json = Json { encodeDefaults = true }

    fun encode(payload: ItemPayload): ByteArray =
        json.encodeToString(ItemPayload.serializer(), payload).encodeToByteArray()

    fun decode(bytes: ByteArray): ItemPayload =
        json.decodeFromString(ItemPayload.serializer(), bytes.decodeToString())
}
