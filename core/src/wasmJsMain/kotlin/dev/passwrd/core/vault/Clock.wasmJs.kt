@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package dev.passwrd.core.vault

private fun jsDateNow(): Double = js("Date.now()")

actual fun currentTimeMillis(): Long = jsDateNow().toLong()
