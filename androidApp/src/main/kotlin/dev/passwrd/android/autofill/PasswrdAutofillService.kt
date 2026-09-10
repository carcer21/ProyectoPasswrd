package dev.passwrd.android.autofill

import android.app.PendingIntent
import android.app.assist.AssistStructure
import android.content.Intent
import android.os.CancellationSignal
import android.service.autofill.AutofillService
import android.service.autofill.Dataset
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.FillResponse
import android.service.autofill.SaveCallback
import android.service.autofill.SaveRequest
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import dev.passwrd.android.PasswrdApplication
import dev.passwrd.android.di.AppContainer
import dev.passwrd.android.ui.MainActivity
import dev.passwrd.core.model.ItemPayload
import dev.passwrd.core.model.ItemType
import dev.passwrd.core.model.VaultItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Sin `android:process` en el manifest: corre en el mismo proceso que la Activity, así que
 * comparte la misma [dev.passwrd.core.vault.VaultSession] — si el usuario ya tiene el vault
 * desbloqueado, autofill no vuelve a pedir nada.
 */
class PasswrdAutofillService : AutofillService() {

    override fun onFillRequest(request: FillRequest, cancellationSignal: CancellationSignal, callback: FillCallback) {
        val structure = request.fillContexts.lastOrNull()?.structure
        if (structure == null) {
            callback.onSuccess(null)
            return
        }

        val fields = FieldClassifier.classify(structure)
        if (fields.usernameId == null && fields.passwordId == null) {
            callback.onSuccess(null)
            return
        }

        val callingPackage = structure.activityComponent?.packageName ?: packageName
        val container = (application as PasswrdApplication).container

        if (!container.vaultSession.isUnlocked) {
            callback.onSuccess(buildAuthResponse(fields))
            return
        }

        val items = runBlocking { container.vaultRepository.observeActive().first() }
        val matches = items.filter { item ->
            val payload = item.payload
            payload is ItemPayload.Login && matchesRequester(payload, fields.webDomain, callingPackage)
        }

        if (matches.isEmpty()) {
            // Ningún item guardado corresponde a este dominio/app: no se ofrece nada — ver
            // docs/THREAT_MODEL.md, fallar cerrado es la única opción aceptable aquí.
            callback.onSuccess(null)
            return
        }

        val responseBuilder = FillResponse.Builder()
        matches.forEach { item ->
            val payload = item.payload as ItemPayload.Login
            val dataset = Dataset.Builder()
            fields.usernameId?.let {
                dataset.setValue(it, AutofillValue.forText(payload.username), presentation(payload.name, payload.username))
            }
            fields.passwordId?.let {
                dataset.setValue(it, AutofillValue.forText(payload.password), presentation(payload.name, payload.username))
            }
            responseBuilder.addDataset(dataset.build())
        }
        callback.onSuccess(responseBuilder.build())
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        val structure = request.fillContexts.lastOrNull()?.structure
        if (structure == null) {
            callback.onFailure("sin estructura")
            return
        }

        val fields = FieldClassifier.classify(structure)
        val password = findValue(structure, fields.passwordId)
        if (password.isNullOrEmpty()) {
            callback.onFailure("sin contraseña que guardar")
            return
        }
        val username = findValue(structure, fields.usernameId).orEmpty()

        val container = (application as PasswrdApplication).container
        if (!container.vaultSession.isUnlocked) {
            callback.onFailure("vault bloqueado")
            return
        }

        runBlocking { saveNewLogin(container, fields.webDomain, username, password) }
        callback.onSuccess()
    }

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun saveNewLogin(container: AppContainer, domain: String?, username: String, password: String) {
        val now = System.currentTimeMillis()
        val item = VaultItem(
            id = Uuid.random().toString(),
            type = ItemType.LOGIN,
            payload = ItemPayload.Login(
                name = domain ?: "Nuevo login",
                username = username,
                password = password,
                uris = domain?.let { listOf("https://$it") } ?: emptyList(),
            ),
            createdAt = now,
            updatedAt = now,
            revision = 1,
        )
        container.vaultRepository.upsert(item)
    }

    private fun findValue(structure: AssistStructure, id: AutofillId?): String? {
        if (id == null) return null
        var result: String? = null
        for (i in 0 until structure.windowNodeCount) {
            FieldClassifier.walk(structure.getWindowNodeAt(i).rootViewNode) { node ->
                if (node.autofillId == id) {
                    val value = node.autofillValue
                    result = if (value != null && value.isText) value.textValue.toString() else null
                }
            }
        }
        return result
    }

    private fun matchesRequester(login: ItemPayload.Login, webDomain: String?, callingPackage: String): Boolean {
        if (webDomain != null && RequesterIdentity.isKnownBrowser(callingPackage)) {
            return login.uris.any { dev.passwrd.core.matching.UriMatcher.matchesHost(it, webDomain) }
        }
        val brand = RequesterIdentity.derivedBrandFromPackage(callingPackage) ?: return false
        return login.uris.any { it.contains(brand, ignoreCase = true) }
    }

    private fun presentation(title: String, subtitle: String): RemoteViews =
        RemoteViews(packageName, android.R.layout.simple_list_item_2).apply {
            setTextViewText(android.R.id.text1, title)
            setTextViewText(android.R.id.text2, subtitle)
        }

    private fun buildAuthResponse(fields: DetectedFields): FillResponse {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val ids = listOfNotNull(fields.usernameId, fields.passwordId).toTypedArray()
        return FillResponse.Builder()
            .setAuthentication(ids, pendingIntent.intentSender, presentation("Passwrd", "Desbloquear para autocompletar"))
            .build()
    }
}
