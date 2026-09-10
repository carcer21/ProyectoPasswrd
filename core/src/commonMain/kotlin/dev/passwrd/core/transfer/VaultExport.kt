package dev.passwrd.core.transfer

import dev.passwrd.core.crypto.Argon2Params
import dev.passwrd.core.crypto.Argon2id
import dev.passwrd.core.crypto.EncryptedBlob
import dev.passwrd.core.crypto.secureRandomBytes
import dev.passwrd.core.crypto.zeroize
import dev.passwrd.core.model.ItemPayload
import dev.passwrd.core.model.ItemType
import dev.passwrd.core.model.VaultItem
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

private const val EXPORT_SALT_SIZE = 16
private val exportJson = Json { encodeDefaults = true; ignoreUnknownKeys = true }

@Serializable
private data class ExportedItem(
    val id: String,
    val type: Int,
    val payload: ItemPayload,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
private data class ExportEnvelope(
    val version: Int = 1,
    val kdfMemoryKib: Int,
    val kdfIterations: Int,
    val kdfParallelism: Int,
    val saltBase64: String,
    val blobBase64: String,
)

/**
 * Export cifrado con contraseña propia (no la maestra: así el export sigue siendo válido
 * si luego se cambia la del vault). Mismo esquema que el vault — Argon2id + AES-256-GCM,
 * ver docs/CRYPTO_SPEC.md — pero autocontenido en un único JSON portable entre plataformas.
 */
@OptIn(ExperimentalEncodingApi::class)
object VaultExporter {
    suspend fun export(
        items: List<VaultItem>,
        exportPassword: ByteArray,
        kdfParams: Argon2Params = Argon2Params.default(),
    ): String {
        val salt = secureRandomBytes(EXPORT_SALT_SIZE)
        val key = Argon2id.derive(exportPassword, salt, kdfParams)

        val exported = items.filter { it.deletedAt == null }.map {
            ExportedItem(it.id, it.type.code, it.payload, it.createdAt, it.updatedAt)
        }
        val plaintext = exportJson
            .encodeToString(ListSerializer(ExportedItem.serializer()), exported)
            .encodeToByteArray()
        val blob = EncryptedBlob.encrypt(key, plaintext)
        key.zeroize()

        val envelope = ExportEnvelope(
            kdfMemoryKib = kdfParams.memoryKib,
            kdfIterations = kdfParams.iterations,
            kdfParallelism = kdfParams.parallelism,
            saltBase64 = Base64.encode(salt),
            blobBase64 = Base64.encode(blob),
        )
        return exportJson.encodeToString(ExportEnvelope.serializer(), envelope)
    }
}

/**
 * @throws dev.passwrd.core.crypto.AuthenticationFailedException si la contraseña de export
 * es incorrecta o el fichero fue manipulado — mismo oráculo que el desbloqueo del vault.
 */
@OptIn(ExperimentalEncodingApi::class)
object VaultImporter {
    suspend fun import(exportedJson: String, exportPassword: ByteArray): List<VaultItem> {
        val envelope = exportJson.decodeFromString(ExportEnvelope.serializer(), exportedJson)
        val salt = Base64.decode(envelope.saltBase64)
        val params = Argon2Params(envelope.kdfMemoryKib, envelope.kdfIterations, envelope.kdfParallelism)
        val key = Argon2id.derive(exportPassword, salt, params)

        val plaintext = try {
            EncryptedBlob.decrypt(key, Base64.decode(envelope.blobBase64))
        } finally {
            key.zeroize()
        }

        val exported = exportJson.decodeFromString(ListSerializer(ExportedItem.serializer()), plaintext.decodeToString())
        return exported.map {
            VaultItem(
                id = it.id,
                type = ItemType.fromCode(it.type),
                payload = it.payload,
                createdAt = it.createdAt,
                updatedAt = it.updatedAt,
                revision = 1,
            )
        }
    }
}
