@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package dev.passwrd.browser

import kotlinx.coroutines.await
import kotlin.js.Promise

/** `chrome.storage.local` — en MV3 devuelve `Promise` (sin callback) cuando se llama sin argumento final. */
private fun jsStorageGet(key: String): Promise<JsAny> = js("chrome.storage.local.get(key)")
private fun jsStorageSet(key: String, value: String): Promise<JsAny> = js("chrome.storage.local.set({ [key]: value })")
private fun jsExtractString(result: JsAny, key: String): JsString? = js("result[key] ?? null")

internal suspend fun chromeStorageGet(key: String): String? {
    val result = jsStorageGet(key).await<JsAny>()
    return jsExtractString(result, key)?.toString()
}

internal suspend fun chromeStorageSet(key: String, value: String) {
    jsStorageSet(key, value).await<JsAny>()
}
