package dev.passwrd.android.autofill

import android.app.assist.AssistStructure
import android.text.InputType
import android.view.View
import android.view.autofill.AutofillId

data class DetectedFields(
    val usernameId: AutofillId?,
    val passwordId: AutofillId?,
    val webDomain: String?,
)

/**
 * Clasifica los campos de un formulario en cascada: `autofillHints` (lo más fiable) →
 * `inputType` → heurísticas sobre hint/idEntry — ver docs/CRYPTO_SPEC.md sección 4.
 */
object FieldClassifier {

    fun classify(structure: AssistStructure): DetectedFields {
        var usernameId: AutofillId? = null
        var passwordId: AutofillId? = null
        var webDomain: String? = null

        for (i in 0 until structure.windowNodeCount) {
            walk(structure.getWindowNodeAt(i).rootViewNode) { node ->
                node.webDomain?.let { if (webDomain == null) webDomain = it }

                val id = node.autofillId ?: return@walk
                val hints = node.autofillHints

                when {
                    hints?.any { it == View.AUTOFILL_HINT_PASSWORD } == true -> passwordId = id
                    hints?.any { it == View.AUTOFILL_HINT_USERNAME || it == View.AUTOFILL_HINT_EMAIL_ADDRESS } == true ->
                        usernameId = id
                    passwordId == null && isLikelyPasswordField(node) -> passwordId = id
                    usernameId == null && isLikelyUsernameField(node) -> usernameId = id
                }
            }
        }
        return DetectedFields(usernameId, passwordId, webDomain)
    }

    private fun isLikelyPasswordField(node: AssistStructure.ViewNode): Boolean {
        val variation = node.inputType and InputType.TYPE_MASK_VARIATION
        val isPasswordInputType = variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD ||
            variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD
        return isPasswordInputType || matchesKeyword(node, "password", "contrasena", "contraseña", "pwd")
    }

    private fun isLikelyUsernameField(node: AssistStructure.ViewNode): Boolean {
        val isTextClass = (node.inputType and InputType.TYPE_MASK_CLASS) == InputType.TYPE_CLASS_TEXT
        return isTextClass && matchesKeyword(node, "user", "email", "login", "usuario", "correo")
    }

    private fun matchesKeyword(node: AssistStructure.ViewNode, vararg keywords: String): Boolean {
        val haystacks = listOfNotNull(node.hint, node.idEntry, node.text?.toString())
        return keywords.any { keyword -> haystacks.any { it.contains(keyword, ignoreCase = true) } }
    }

    internal fun walk(node: AssistStructure.ViewNode, action: (AssistStructure.ViewNode) -> Unit) {
        action(node)
        for (i in 0 until node.childCount) {
            walk(node.getChildAt(i), action)
        }
    }
}
