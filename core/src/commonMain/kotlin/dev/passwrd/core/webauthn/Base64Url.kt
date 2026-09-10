package dev.passwrd.core.webauthn

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/** Base64url sin padding — el que usa WebAuthn en `challenge`, `credentialId`, etc. */
@OptIn(ExperimentalEncodingApi::class)
object Base64Url {
    private val codec = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT)

    fun encode(bytes: ByteArray): String = codec.encode(bytes)
    fun decode(text: String): ByteArray = codec.decode(text)
}
