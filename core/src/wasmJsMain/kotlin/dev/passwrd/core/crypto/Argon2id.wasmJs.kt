package dev.passwrd.core.crypto

import kotlinx.coroutines.await
import kotlin.js.Promise

/**
 * hash-wasm no expone `secret`/`associatedData` (a diferencia de BouncyCastle) — esos
 * parámetros sólo existen en la API para el vector de test RFC 9106, que por eso corre
 * únicamente en `jvmTest`, no en `commonTest`. Ver docs/adr/0007.
 */
@JsModule("hash-wasm")
private external object HashWasm {
    fun argon2id(options: JsAny): Promise<JsAny>
}

private fun jsArgon2Options(
    password: JsAny,
    salt: JsAny,
    parallelism: Int,
    iterations: Int,
    memorySize: Int,
    hashLength: Int,
): JsAny = js(
    "({ password: password, salt: salt, parallelism: parallelism, iterations: iterations, memorySize: memorySize, hashLength: hashLength, outputType: 'binary' })",
)

actual object Argon2id {
    actual suspend fun derive(
        password: ByteArray,
        salt: ByteArray,
        params: Argon2Params,
        secret: ByteArray,
        associatedData: ByteArray,
    ): ByteArray {
        require(secret.isEmpty() && associatedData.isEmpty()) {
            "hash-wasm no soporta secret/associatedData — sólo para el vector de test RFC 9106"
        }
        val options = jsArgon2Options(
            password.toJsBytes(),
            salt.toJsBytes(),
            params.parallelism,
            params.iterations,
            params.memoryKib,
            params.outputLength,
        )
        return HashWasm.argon2id(options).await<JsAny>().toByteArray()
    }
}
