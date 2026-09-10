package dev.passwrd.core.webauthn

import kotlin.test.Test
import kotlin.test.assertEquals

/** Vectores oficiales RFC 8949 Apéndice A, verificados contra el texto crudo del RFC. */
class CborTest {

    private fun hex(bytes: ByteArray): String = bytes.joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }

    @Test
    fun unsignedIntegers() {
        assertEquals("00", hex(Cbor.encode(Cbor.int(0))))
        assertEquals("01", hex(Cbor.encode(Cbor.int(1))))
        assertEquals("0a", hex(Cbor.encode(Cbor.int(10))))
        assertEquals("17", hex(Cbor.encode(Cbor.int(23))))
        assertEquals("1818", hex(Cbor.encode(Cbor.int(24))))
        assertEquals("1819", hex(Cbor.encode(Cbor.int(25))))
        assertEquals("1864", hex(Cbor.encode(Cbor.int(100))))
        assertEquals("1903e8", hex(Cbor.encode(Cbor.int(1000))))
        assertEquals("1a000f4240", hex(Cbor.encode(Cbor.int(1000000))))
    }

    @Test
    fun negativeIntegers() {
        assertEquals("20", hex(Cbor.encode(Cbor.int(-1))))
        assertEquals("29", hex(Cbor.encode(Cbor.int(-10))))
        assertEquals("3863", hex(Cbor.encode(Cbor.int(-100))))
        assertEquals("3903e7", hex(Cbor.encode(Cbor.int(-1000))))
    }

    @Test
    fun byteStrings() {
        assertEquals("40", hex(Cbor.encode(CborValue.Bytes(ByteArray(0)))))
        assertEquals(
            "4401020304",
            hex(Cbor.encode(CborValue.Bytes(byteArrayOf(1, 2, 3, 4)))),
        )
    }

    @Test
    fun textStrings() {
        assertEquals("60", hex(Cbor.encode(CborValue.Text(""))))
        assertEquals("6161", hex(Cbor.encode(CborValue.Text("a"))))
        assertEquals("6449455446", hex(Cbor.encode(CborValue.Text("IETF"))))
    }

    @Test
    fun arrays() {
        assertEquals("80", hex(Cbor.encode(CborValue.Arr(emptyList()))))
        assertEquals(
            "83010203",
            hex(Cbor.encode(CborValue.Arr(listOf(Cbor.int(1), Cbor.int(2), Cbor.int(3))))),
        )
        assertEquals(
            "8301820203820405",
            hex(
                Cbor.encode(
                    CborValue.Arr(
                        listOf(
                            Cbor.int(1),
                            CborValue.Arr(listOf(Cbor.int(2), Cbor.int(3))),
                            CborValue.Arr(listOf(Cbor.int(4), Cbor.int(5))),
                        ),
                    ),
                ),
            ),
        )
    }

    @Test
    fun maps() {
        assertEquals("a0", hex(Cbor.encode(CborValue.Map(emptyList()))))
        assertEquals(
            "a201020304",
            hex(Cbor.encode(CborValue.Map(listOf(Cbor.int(1) to Cbor.int(2), Cbor.int(3) to Cbor.int(4))))),
        )
        assertEquals(
            "a26161016162820203",
            hex(
                Cbor.encode(
                    CborValue.Map(
                        listOf(
                            CborValue.Text("a") to Cbor.int(1),
                            CborValue.Text("b") to CborValue.Arr(listOf(Cbor.int(2), Cbor.int(3))),
                        ),
                    ),
                ),
            ),
        )
    }
}
