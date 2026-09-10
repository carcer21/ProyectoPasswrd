package dev.passwrd.core.crypto

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

actual suspend fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray {
    val mac = Mac.getInstance("HmacSHA256")
    // RFC 5869 permite salt/ikm vacío; SecretKeySpec exige clave no vacía en JCA.
    mac.init(SecretKeySpec(if (key.isEmpty()) ByteArray(32) else key, "HmacSHA256"))
    return mac.doFinal(data)
}
