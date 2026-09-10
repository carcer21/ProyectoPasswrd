package dev.passwrd.core.webauthn

import kotlin.test.Test
import kotlin.test.assertEquals

class CoseKeyTest {

    @Test
    fun encodesAsFiveEntryMap() {
        val x = ByteArray(32) { 0xAA.toByte() }
        val y = ByteArray(32) { 0xBB.toByte() }

        val encoded = CoseKey.encodeP256PublicKey(x, y)

        // Cabecera de mapa CBOR con 5 entradas: major type 5, longitud 5 -> 0xa5.
        assertEquals(0xa5.toByte(), encoded[0])
        // La codificación es determinista: mismo input -> mismo output.
        assertEquals(encoded.toList(), CoseKey.encodeP256PublicKey(x, y).toList())
    }
}
