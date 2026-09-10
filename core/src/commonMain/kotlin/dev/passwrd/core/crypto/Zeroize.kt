package dev.passwrd.core.crypto

/**
 * Borra el contenido de una clave en memoria. Todo material criptográfico vive en
 * [ByteArray] (nunca [String], que es inmutable y sobrevive al GC) para poder llamar
 * esto al bloquear el vault — ver docs/CRYPTO_SPEC.md "Higiene de memoria".
 */
fun ByteArray.zeroize() {
    fill(0)
}
