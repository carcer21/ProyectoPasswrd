package dev.passwrd.core.model

import kotlinx.serialization.Serializable

/**
 * Todo lo que no es metadato en claro (ver [ItemType]) vive aquí dentro, serializado a
 * JSON y cifrado como una unidad — ver docs/CRYPTO_SPEC.md "Modelo de datos".
 */
@Serializable
sealed interface ItemPayload {

    @Serializable
    data class Login(
        val name: String,
        val username: String,
        val password: String,
        val uris: List<String> = emptyList(),
        val notes: String = "",
        val totpSecret: String? = null,
    ) : ItemPayload

    @Serializable
    data class SecureNote(
        val name: String,
        val content: String,
    ) : ItemPayload

    @Serializable
    data class Card(
        val name: String,
        val cardholderName: String,
        val number: String,
        val expiryMonth: Int,
        val expiryYear: Int,
        val securityCode: String,
    ) : ItemPayload

    @Serializable
    data class Identity(
        val name: String,
        val fullName: String,
        val email: String,
        val phone: String,
        val address: String,
    ) : ItemPayload

    /**
     * Par de claves P-256 por RP — ver docs/CRYPTO_SPEC.md "Passkeys". Todos los campos
     * binarios van en Base64 porque el payload entero se serializa a JSON antes de cifrarse
     * (ver [dev.passwrd.core.vault.ItemCodec]). La clave privada vive aquí dentro del vault
     * cifrado, NO en el Android Keystore: a diferencia del resto de claves, ésta tiene que
     * poder exportarse/sincronizarse — es una concesión de seguridad deliberada, documentada
     * en docs/THREAT_MODEL.md.
     */
    @Serializable
    data class Passkey(
        val name: String,
        val rpId: String,
        val userHandleBase64: String,
        val userName: String,
        val userDisplayName: String,
        val credentialIdBase64: String,
        val privateKeyPkcs8Base64: String,
        val publicKeyXBase64: String,
        val publicKeyYBase64: String,
        val signCount: Long = 0,
    ) : ItemPayload
}
