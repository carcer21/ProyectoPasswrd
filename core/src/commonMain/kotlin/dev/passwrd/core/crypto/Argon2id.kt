package dev.passwrd.core.crypto

/**
 * Argon2id (RFC 9106). `secret` y `associatedData` sólo existen para poder verificar el
 * vector de test oficial del RFC — el uso en producción (ver docs/CRYPTO_SPEC.md) sólo
 * pasa password y salt.
 */
expect object Argon2id {
    suspend fun derive(
        password: ByteArray,
        salt: ByteArray,
        params: Argon2Params,
        secret: ByteArray = ByteArray(0),
        associatedData: ByteArray = ByteArray(0),
    ): ByteArray
}
