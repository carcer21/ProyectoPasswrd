package dev.passwrd.core.webauthn

/**
 * Encoder CBOR (RFC 8949) mínimo: sólo lo que hace falta para COSE_Key y el objeto de
 * atestación WebAuthn — enteros (con y sin signo), byte strings, text strings y mapas de
 * longitud definida. Sin librería: ~80 líneas, no vale la pena una dependencia para esto.
 */
sealed interface CborValue {
    data class UInt(val value: Long) : CborValue
    data class NegInt(val value: Long) : CborValue // representa el entero negativo -1-value... ver encode()
    data class Bytes(val value: ByteArray) : CborValue
    data class Text(val value: String) : CborValue
    data class Arr(val items: List<CborValue>) : CborValue
    data class Map(val entries: List<Pair<CborValue, CborValue>>) : CborValue
}

object Cbor {
    /** Entero con signo: positivo -> UInt, negativo -> NegInt (codificado como -1-n). */
    fun int(value: Long): CborValue =
        if (value >= 0) CborValue.UInt(value) else CborValue.NegInt(-1 - value)

    fun encode(value: CborValue): ByteArray = buildList<Byte> { encodeInto(value, this) }.toByteArray()

    private fun encodeInto(value: CborValue, out: MutableList<Byte>) {
        when (value) {
            is CborValue.UInt -> encodeHead(0, value.value, out)
            is CborValue.NegInt -> encodeHead(1, value.value, out)
            is CborValue.Bytes -> {
                encodeHead(2, value.value.size.toLong(), out)
                value.value.forEach { out.add(it) }
            }
            is CborValue.Text -> {
                val bytes = value.value.encodeToByteArray()
                encodeHead(3, bytes.size.toLong(), out)
                bytes.forEach { out.add(it) }
            }
            is CborValue.Arr -> {
                encodeHead(4, value.items.size.toLong(), out)
                value.items.forEach { encodeInto(it, out) }
            }
            is CborValue.Map -> {
                encodeHead(5, value.entries.size.toLong(), out)
                value.entries.forEach { (k, v) -> encodeInto(k, out); encodeInto(v, out) }
            }
        }
    }

    /** Cabecera de major type (0-7 en los 3 bits altos) + argumento, RFC 8949 §3. */
    private fun encodeHead(majorType: Int, arg: Long, out: MutableList<Byte>) {
        val m = majorType shl 5
        when {
            arg < 24 -> out.add((m or arg.toInt()).toByte())
            arg <= 0xFF -> {
                out.add((m or 24).toByte())
                out.add(arg.toByte())
            }
            arg <= 0xFFFF -> {
                out.add((m or 25).toByte())
                out.add((arg shr 8).toByte())
                out.add(arg.toByte())
            }
            arg <= 0xFFFFFFFFL -> {
                out.add((m or 26).toByte())
                out.add((arg shr 24).toByte())
                out.add((arg shr 16).toByte())
                out.add((arg shr 8).toByte())
                out.add(arg.toByte())
            }
            else -> {
                out.add((m or 27).toByte())
                for (shift in 56 downTo 0 step 8) out.add((arg shr shift).toByte())
            }
        }
    }
}
