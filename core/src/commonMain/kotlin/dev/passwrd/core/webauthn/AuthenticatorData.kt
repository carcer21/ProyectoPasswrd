package dev.passwrd.core.webauthn

import dev.passwrd.core.crypto.sha256

/** authenticatorData (WebAuthn §6.1): rpIdHash(32) + flags(1) + signCount(4) [+ datos de credencial]. */
object AuthenticatorData {
    private const val FLAG_USER_PRESENT = 0x01
    private const val FLAG_USER_VERIFIED = 0x04
    private const val FLAG_ATTESTED_CREDENTIAL_DATA = 0x40

    /** Para una aserción ("get"): sin datos de credencial adjuntos. */
    suspend fun forAssertion(rpId: String, signCount: Long): ByteArray = build(rpId, signCount, attestedCredentialData = null)

    /** Para un registro ("create"): incluye aaguid + credentialId + clave pública COSE. */
    suspend fun forRegistration(rpId: String, signCount: Long, credentialId: ByteArray, cosePublicKey: ByteArray): ByteArray {
        val aaguid = ByteArray(16) // sin modelo de autenticador propio declarado — todo ceros, válido por spec.
        val credentialIdLength = byteArrayOf((credentialId.size shr 8).toByte(), credentialId.size.toByte())
        val attestedCredentialData = aaguid + credentialIdLength + credentialId + cosePublicKey
        return build(rpId, signCount, attestedCredentialData)
    }

    private suspend fun build(rpId: String, signCount: Long, attestedCredentialData: ByteArray?): ByteArray {
        val rpIdHash = sha256(rpId.encodeToByteArray())
        var flags = FLAG_USER_PRESENT or FLAG_USER_VERIFIED
        if (attestedCredentialData != null) flags = flags or FLAG_ATTESTED_CREDENTIAL_DATA
        val signCountBytes = byteArrayOf(
            (signCount shr 24).toByte(),
            (signCount shr 16).toByte(),
            (signCount shr 8).toByte(),
            signCount.toByte(),
        )
        return rpIdHash + byteArrayOf(flags.toByte()) + signCountBytes + (attestedCredentialData ?: ByteArray(0))
    }
}
