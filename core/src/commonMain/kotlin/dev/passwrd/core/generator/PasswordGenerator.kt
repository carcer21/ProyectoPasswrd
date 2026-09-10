package dev.passwrd.core.generator

import dev.passwrd.core.crypto.secureRandomBytes
import kotlin.math.ln

private const val LOWER = "abcdefghijklmnopqrstuvwxyz"
private const val UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
private const val DIGITS = "0123456789"
private const val SYMBOLS = "!@#\$%^&*()-_=+[]{};:,.<>?"
// Sin 0/O/1/l/I: legibles a simple vista cuando el usuario tiene que teclear el resultado.
private const val LOWER_NO_AMBIGUOUS = "abcdefghjkmnpqrstuvwxyz"
private const val UPPER_NO_AMBIGUOUS = "ABCDEFGHJKMNPQRSTUVWXYZ"
private const val DIGITS_NO_AMBIGUOUS = "23456789"

data class PasswordPolicy(
    val length: Int = 20,
    val useLower: Boolean = true,
    val useUpper: Boolean = true,
    val useDigits: Boolean = true,
    val useSymbols: Boolean = true,
    val avoidAmbiguous: Boolean = true,
)

/** Password aleatoria por CSPRNG (nunca `kotlin.random.Random`) con selección por rechazo, sin sesgo modulo. */
object PasswordGenerator {

    fun generate(policy: PasswordPolicy): String {
        val charset = buildCharset(policy)
        require(charset.isNotEmpty()) { "la política no habilita ningún conjunto de caracteres" }
        require(policy.length > 0)

        return buildString {
            repeat(policy.length) {
                append(charset[randomIndex(charset.length)])
            }
        }
    }

    /** Passphrase estilo Diceware simplificada: N palabras de una lista corta embebida, unidas por separador. */
    fun generatePassphrase(wordCount: Int = 5, separator: String = "-"): String {
        require(wordCount > 0)
        return (1..wordCount).joinToString(separator) { WORDLIST[randomIndex(WORDLIST.size)] }
    }

    /** Estimación conservadora en bits: log2(tamaño del alfabeto efectivo) * longitud. */
    fun estimateEntropyBits(policy: PasswordPolicy): Double {
        val charset = buildCharset(policy)
        if (charset.isEmpty()) return 0.0
        return policy.length * (ln(charset.length.toDouble()) / ln(2.0))
    }

    private fun buildCharset(policy: PasswordPolicy): String = buildString {
        if (policy.useLower) append(if (policy.avoidAmbiguous) LOWER_NO_AMBIGUOUS else LOWER)
        if (policy.useUpper) append(if (policy.avoidAmbiguous) UPPER_NO_AMBIGUOUS else UPPER)
        if (policy.useDigits) append(if (policy.avoidAmbiguous) DIGITS_NO_AMBIGUOUS else DIGITS)
        if (policy.useSymbols) append(SYMBOLS)
    }

    /** Índice uniforme en [0, bound) por rechazo, sin el sesgo de `random() % bound`. */
    private fun randomIndex(bound: Int): Int {
        require(bound in 1..256)
        val limit = 256 - (256 % bound)
        while (true) {
            val b = secureRandomBytes(1)[0].toInt() and 0xFF
            if (b < limit) return b % bound
        }
    }
}
