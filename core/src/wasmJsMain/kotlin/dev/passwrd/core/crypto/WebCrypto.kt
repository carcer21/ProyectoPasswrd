@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package dev.passwrd.core.crypto

import kotlinx.coroutines.await
import kotlin.js.Promise

/**
 * Interop mínima Kotlin/Wasm <-> WebCrypto (`SubtleCrypto`). Kotlin/Wasm sólo permite tipos
 * `JsAny`/primitivos/`String`/función en la frontera JS — no hay `Uint8Array` tipado como en
 * Kotlin/JS clásico, así que los bytes viajan como `JsAny` opaco construido a mano. Cada
 * llamada a `js(...)` tiene que ser una expresión propia (no un argumento inline) — ver
 * restricción del compilador.
 */
private fun newUint8Array(length: Int): JsAny = js("new Uint8Array(length)")
private fun uint8ArraySet(array: JsAny, index: Int, value: Int): Unit = js("array[index] = value")
private fun uint8ArrayGet(array: JsAny, index: Int): Int = js("array[index]")
private fun uint8ArrayLength(array: JsAny): Int = js("array.length")

internal fun ByteArray.toJsBytes(): JsAny {
    val out = newUint8Array(size)
    for (i in indices) uint8ArraySet(out, i, this[i].toInt() and 0xFF)
    return out
}

internal fun JsAny.toByteArray(): ByteArray {
    val len = uint8ArrayLength(this)
    return ByteArray(len) { uint8ArrayGet(this, it).toByte() }
}

/**
 * `crypto.subtle.{digest,sign,encrypt,decrypt,exportKey}` resuelven a `ArrayBuffer`, no a
 * `Uint8Array` — `ArrayBuffer` no tiene `.length` ni acceso indexado, así que [toByteArray]
 * sobre él da longitud 0 en vez de fallar ruidosamente. Hay que envolverlo primero.
 */
private fun wrapArrayBuffer(buffer: JsAny): JsAny = js("new Uint8Array(buffer)")

internal fun JsAny.bufferToByteArray(): ByteArray = wrapArrayBuffer(this).toByteArray()

private fun jsGetRandomValues(array: JsAny): Unit = js("crypto.getRandomValues(array)")
private fun jsDigest(algorithm: String, data: JsAny): Promise<JsAny> = js("crypto.subtle.digest(algorithm, data)")
private fun jsImportKey(format: String, keyData: JsAny, algorithm: JsAny, extractable: Boolean, usages: JsAny): Promise<JsAny> =
    js("crypto.subtle.importKey(format, keyData, algorithm, extractable, usages)")
private fun jsSign(algorithm: JsAny, key: JsAny, data: JsAny): Promise<JsAny> = js("crypto.subtle.sign(algorithm, key, data)")
private fun jsVerify(algorithm: JsAny, key: JsAny, signature: JsAny, data: JsAny): Promise<JsAny> =
    js("crypto.subtle.verify(algorithm, key, signature, data)")
private fun jsEncrypt(algorithm: JsAny, key: JsAny, data: JsAny): Promise<JsAny> = js("crypto.subtle.encrypt(algorithm, key, data)")
private fun jsDecrypt(algorithm: JsAny, key: JsAny, data: JsAny): Promise<JsAny> = js("crypto.subtle.decrypt(algorithm, key, data)")
private fun jsGenerateKey(algorithm: JsAny, extractable: Boolean, usages: JsAny): Promise<JsAny> =
    js("crypto.subtle.generateKey(algorithm, extractable, usages)")
private fun jsExportKey(format: String, key: JsAny): Promise<JsAny> = js("crypto.subtle.exportKey(format, key)")

private fun jsArrayOf1(a: String): JsAny = js("[a]")
private fun jsArrayOf2(a: String, b: String): JsAny = js("[a, b]")

private fun jsObjHmacImportParams(): JsAny = js("({ name: 'HMAC', hash: 'SHA-256' })")
private fun jsObjAesGcmImportParams(): JsAny = js("({ name: 'AES-GCM' })")
private fun jsObjAesGcmParams(nonce: JsAny): JsAny = js("({ name: 'AES-GCM', iv: nonce })")
private fun jsObjAesGcmParamsWithAad(nonce: JsAny, aad: JsAny): JsAny = js("({ name: 'AES-GCM', iv: nonce, additionalData: aad })")
private fun jsObjEcdsaKeyParams(): JsAny = js("({ name: 'ECDSA', namedCurve: 'P-256' })")
private fun jsObjEcdsaSignParams(): JsAny = js("({ name: 'ECDSA', hash: 'SHA-256' })")
private fun jsKeyPairPrivate(pair: JsAny): JsAny = js("pair.privateKey")
private fun jsKeyPairPublic(pair: JsAny): JsAny = js("pair.publicKey")
private fun jsVerifyBoolean(algorithm: JsAny, key: JsAny, signature: JsAny, data: JsAny): Promise<JsBoolean> =
    js("crypto.subtle.verify(algorithm, key, signature, data)")

/** Síncrona a propósito: `crypto.getRandomValues` no es async en WebCrypto. */
internal fun webCryptoRandomBytes(size: Int): ByteArray {
    val array = newUint8Array(size)
    jsGetRandomValues(array)
    return array.toByteArray()
}

internal suspend fun webCryptoSha256(data: ByteArray): ByteArray =
    jsDigest("SHA-256", data.toJsBytes()).await<JsAny>().bufferToByteArray()

internal suspend fun webCryptoHmacSha256(key: ByteArray, data: ByteArray): ByteArray {
    val usages = jsArrayOf2("sign", "verify")
    val hmacKey = jsImportKey("raw", key.toJsBytes(), jsObjHmacImportParams(), false, usages).await<JsAny>()
    return jsSign(jsObjHmacImportParams(), hmacKey, data.toJsBytes()).await<JsAny>().bufferToByteArray()
}

internal suspend fun webCryptoAesGcmSeal(key: ByteArray, nonce: ByteArray, plaintext: ByteArray, aad: ByteArray): ByteArray {
    val usages = jsArrayOf2("encrypt", "decrypt")
    val aesKey = jsImportKey("raw", key.toJsBytes(), jsObjAesGcmImportParams(), false, usages).await<JsAny>()
    val algorithm = if (aad.isEmpty()) jsObjAesGcmParams(nonce.toJsBytes()) else jsObjAesGcmParamsWithAad(nonce.toJsBytes(), aad.toJsBytes())
    return jsEncrypt(algorithm, aesKey, plaintext.toJsBytes()).await<JsAny>().bufferToByteArray()
}

internal suspend fun webCryptoAesGcmOpen(key: ByteArray, nonce: ByteArray, ciphertextAndTag: ByteArray, aad: ByteArray): ByteArray {
    val usages = jsArrayOf2("encrypt", "decrypt")
    val aesKey = jsImportKey("raw", key.toJsBytes(), jsObjAesGcmImportParams(), false, usages).await<JsAny>()
    val algorithm = if (aad.isEmpty()) jsObjAesGcmParams(nonce.toJsBytes()) else jsObjAesGcmParamsWithAad(nonce.toJsBytes(), aad.toJsBytes())
    return try {
        jsDecrypt(algorithm, aesKey, ciphertextAndTag.toJsBytes()).await<JsAny>().bufferToByteArray()
    } catch (e: Throwable) {
        throw AuthenticationFailedException()
    }
}

internal class WebCryptoEcKeyPair(val privateKeyPkcs8: ByteArray, val publicKeyRaw: ByteArray)

internal suspend fun webCryptoGenerateEcKeyPair(): WebCryptoEcKeyPair {
    val usages = jsArrayOf2("sign", "verify")
    val pair = jsGenerateKey(jsObjEcdsaKeyParams(), true, usages).await<JsAny>()
    val pkcs8 = jsExportKey("pkcs8", jsKeyPairPrivate(pair)).await<JsAny>().bufferToByteArray()
    val raw = jsExportKey("raw", jsKeyPairPublic(pair)).await<JsAny>().bufferToByteArray()
    return WebCryptoEcKeyPair(pkcs8, raw)
}

/** @return firma raw ECDSA de WebCrypto (r||s, 64 bytes, IEEE P1363) — no DER. */
internal suspend fun webCryptoEcSignRaw(privateKeyPkcs8: ByteArray, data: ByteArray): ByteArray {
    val key = jsImportKey("pkcs8", privateKeyPkcs8.toJsBytes(), jsObjEcdsaKeyParams(), false, jsArrayOf1("sign")).await<JsAny>()
    return jsSign(jsObjEcdsaSignParams(), key, data.toJsBytes()).await<JsAny>().bufferToByteArray()
}

/** @param signatureRaw firma raw ECDSA (r||s, 64 bytes) — no DER. */
internal suspend fun webCryptoEcVerifyRaw(publicKeyRaw: ByteArray, data: ByteArray, signatureRaw: ByteArray): Boolean {
    val key = jsImportKey("raw", publicKeyRaw.toJsBytes(), jsObjEcdsaKeyParams(), false, jsArrayOf1("verify")).await<JsAny>()
    return jsVerifyBoolean(jsObjEcdsaSignParams(), key, signatureRaw.toJsBytes(), data.toJsBytes()).await<Boolean>()
}
