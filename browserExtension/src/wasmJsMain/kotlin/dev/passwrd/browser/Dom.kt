@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package dev.passwrd.browser

/**
 * Interop DOM mínima — sin Compose Web ni kotlinx-browser, mismo criterio que `WebCrypto.kt`
 * en `:core`: menos superficie nueva que auditar. Los elementos viajan como `JsAny` opaco.
 */
private fun jsGetElementById(id: String): JsAny? = js("document.getElementById(id)")
private fun jsSetHidden(el: JsAny, hidden: Boolean): Unit = js("el.classList.toggle('hidden', hidden)")
private fun jsGetValue(el: JsAny): String = js("el.value")
private fun jsSetValue(el: JsAny, value: String): Unit = js("el.value = value")
private fun jsSetTextContent(el: JsAny, text: String): Unit = js("el.textContent = text")
private fun jsSetInnerHtml(el: JsAny, html: String): Unit = js("el.innerHTML = html")
private fun jsAddClickListener(el: JsAny, handler: () -> Unit): Unit = js("el.addEventListener('click', function() { handler(); })")
private fun jsCopyToClipboard(text: String): Unit = js("navigator.clipboard.writeText(text)")
private fun jsConfirm(message: String): Boolean = js("window.confirm(message)")

internal fun byId(id: String): JsAny = requireNotNull(jsGetElementById(id)) { "no existe #$id en popup.html" }

internal fun show(id: String) = jsSetHidden(byId(id), false)
internal fun hide(id: String) = jsSetHidden(byId(id), true)

internal fun value(id: String): String = jsGetValue(byId(id))
internal fun setValue(id: String, text: String) = jsSetValue(byId(id), text)

internal fun setText(id: String, text: String) = jsSetTextContent(byId(id), text)
internal fun setHtml(id: String, html: String) = jsSetInnerHtml(byId(id), html)

internal fun onClick(id: String, handler: () -> Unit) = jsAddClickListener(byId(id), handler)

internal fun copyToClipboard(text: String) = jsCopyToClipboard(text)

internal fun confirmAction(message: String): Boolean = jsConfirm(message)

/** Escapa lo mínimo para no romper el HTML al listar nombres/usuarios de items. */
internal fun escapeHtml(text: String): String = text
    .replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
    .replace("\"", "&quot;")
