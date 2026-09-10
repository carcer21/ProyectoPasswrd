package dev.passwrd.core.webauthn

/** fmt "none": sin declarar fabricante de hardware — ver docs/CRYPTO_SPEC.md "Passkeys". */
object AttestationObject {
    fun build(authData: ByteArray): ByteArray {
        val map = CborValue.Map(
            listOf(
                CborValue.Text("fmt") to CborValue.Text("none"),
                CborValue.Text("attStmt") to CborValue.Map(emptyList()),
                CborValue.Text("authData") to CborValue.Bytes(authData),
            ),
        )
        return Cbor.encode(map)
    }
}
