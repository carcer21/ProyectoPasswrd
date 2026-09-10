package dev.passwrd.core.crypto

/** Coordenadas de 32 bytes big-endian (tamaño de campo de P-256) — formato que pide COSE_Key. */
data class EcKeyPair(
    val privateKeyPkcs8: ByteArray,
    val publicKeyX: ByteArray,
    val publicKeyY: ByteArray,
)

/** EC P-256 (secp256r1) para passkeys — ver docs/CRYPTO_SPEC.md "Passkeys". */
expect object EcP256 {
    suspend fun generateKeyPair(): EcKeyPair

    /** @return firma ECDSA en formato DER (ASN.1 SEQUENCE de dos INTEGER) — el que pide WebAuthn. */
    suspend fun sign(privateKeyPkcs8: ByteArray, data: ByteArray): ByteArray

    suspend fun verify(publicKeyX: ByteArray, publicKeyY: ByteArray, data: ByteArray, signature: ByteArray): Boolean
}
