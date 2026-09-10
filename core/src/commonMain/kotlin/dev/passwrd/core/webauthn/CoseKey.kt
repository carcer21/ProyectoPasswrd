package dev.passwrd.core.webauthn

/** COSE_Key (RFC 9053) para una clave pública EC2 P-256 — lo que WebAuthn mete en authenticatorData. */
object CoseKey {
    fun encodeP256PublicKey(x: ByteArray, y: ByteArray): ByteArray {
        require(x.size == 32 && y.size == 32)
        val map = CborValue.Map(
            listOf(
                Cbor.int(1) to Cbor.int(2), // kty: EC2
                Cbor.int(3) to Cbor.int(-7), // alg: ES256 (ECDSA w/ SHA-256)
                Cbor.int(-1) to Cbor.int(1), // crv: P-256
                Cbor.int(-2) to CborValue.Bytes(x),
                Cbor.int(-3) to CborValue.Bytes(y),
            ),
        )
        return Cbor.encode(map)
    }
}
