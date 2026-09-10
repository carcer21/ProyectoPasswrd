package dev.passwrd.core.crypto

/**
 * Parámetros Argon2id. Se guardan en el header del vault (ver docs/CRYPTO_SPEC.md) para
 * poder subirlos sin romper vaults existentes.
 *
 * `parallelism = 1` a propósito: BouncyCastle recorre los lanes de forma secuencial, así
 * que p > 1 sólo penaliza al defensor sin penalizar a un atacante con paralelismo real.
 */
data class Argon2Params(
    val memoryKib: Int,
    val iterations: Int,
    val parallelism: Int,
    val outputLength: Int = 32,
) {
    companion object {
        /** Punto de partida v1. En onboarding, calibrar `iterations` con objetivo ~1s real. */
        fun default() = Argon2Params(memoryKib = 64 * 1024, iterations = 3, parallelism = 1)
    }
}
