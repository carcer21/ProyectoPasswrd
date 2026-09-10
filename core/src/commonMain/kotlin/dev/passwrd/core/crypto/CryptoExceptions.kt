package dev.passwrd.core.crypto

/**
 * Falla la apertura de un [Aes256Gcm] blob: contraseña incorrecta o dato manipulado.
 * Deliberadamente no se distingue entre ambos casos (ver docs/CRYPTO_SPEC.md
 * "Verificación de la contraseña") para no dar pistas a un atacante.
 */
class AuthenticationFailedException : Exception("AEAD authentication failed")
