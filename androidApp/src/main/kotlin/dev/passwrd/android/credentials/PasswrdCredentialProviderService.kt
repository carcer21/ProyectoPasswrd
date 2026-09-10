package dev.passwrd.android.credentials

import android.app.PendingIntent
import android.content.Intent
import android.os.CancellationSignal
import android.os.OutcomeReceiver
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.provider.BeginCreateCredentialRequest
import androidx.credentials.provider.BeginCreateCredentialResponse
import androidx.credentials.provider.BeginGetCredentialRequest
import androidx.credentials.provider.BeginGetCredentialResponse
import androidx.credentials.provider.BeginGetPasswordOption
import androidx.credentials.provider.BeginGetPublicKeyCredentialOption
import androidx.credentials.provider.CreateEntry
import androidx.credentials.provider.CredentialEntry
import androidx.credentials.provider.CredentialProviderService
import androidx.credentials.provider.PasswordCredentialEntry
import androidx.credentials.provider.ProviderClearCredentialStateRequest
import androidx.credentials.provider.PublicKeyCredentialEntry
import dev.passwrd.android.PasswrdApplication
import dev.passwrd.core.model.ItemPayload
import dev.passwrd.core.webauthn.PublicKeyRequestParser
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

internal const val ACTION_GET_CREDENTIAL = "dev.passwrd.android.credentials.GET"
internal const val ACTION_CREATE_CREDENTIAL = "dev.passwrd.android.credentials.CREATE"
internal const val EXTRA_ITEM_ID = "dev.passwrd.android.credentials.EXTRA_ITEM_ID"

/**
 * Ver docs/CRYPTO_SPEC.md "Passkeys" y docs/THREAT_MODEL.md. Sin `android:process`: comparte
 * proceso y `VaultSession` con la Activity, igual que [dev.passwrd.android.autofill.PasswrdAutofillService].
 */
class PasswrdCredentialProviderService : CredentialProviderService() {

    override fun onBeginGetCredentialRequest(
        request: BeginGetCredentialRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<BeginGetCredentialResponse, GetCredentialException>,
    ) {
        val container = (application as PasswrdApplication).container

        if (!container.vaultSession.isUnlocked) {
            // Fallar cerrado: sin vault desbloqueado no se ofrece ninguna entrada — ver
            // docs/THREAT_MODEL.md, mismo principio que en autofill.
            callback.onResult(BeginGetCredentialResponse(emptyList()))
            return
        }

        val items = runBlocking { container.vaultRepository.observeActive().first() }
        val entries = mutableListOf<CredentialEntry>()

        for (option in request.beginGetCredentialOptions) {
            when (option) {
                is BeginGetPublicKeyCredentialOption -> {
                    val requestOptions = PublicKeyRequestParser.parseRequestOptions(option.requestJson)
                    items.forEach { item ->
                        val payload = item.payload
                        if (payload is ItemPayload.Passkey && payload.rpId == requestOptions.rpId) {
                            entries.add(
                                PublicKeyCredentialEntry(
                                    context = applicationContext,
                                    username = payload.userName,
                                    pendingIntent = authPendingIntent(ACTION_GET_CREDENTIAL, item.id),
                                    beginGetPublicKeyCredentialOption = option,
                                    displayName = payload.userDisplayName,
                                ),
                            )
                        }
                    }
                }
                is BeginGetPasswordOption -> {
                    items.forEach { item ->
                        val payload = item.payload
                        if (payload is ItemPayload.Login) {
                            entries.add(
                                PasswordCredentialEntry(
                                    context = applicationContext,
                                    username = payload.username,
                                    pendingIntent = authPendingIntent(ACTION_GET_CREDENTIAL, item.id),
                                    beginGetPasswordOption = option,
                                    displayName = payload.name,
                                ),
                            )
                        }
                    }
                }
                else -> Unit
            }
        }

        callback.onResult(BeginGetCredentialResponse(entries))
    }

    override fun onBeginCreateCredentialRequest(
        request: BeginCreateCredentialRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<BeginCreateCredentialResponse, CreateCredentialException>,
    ) {
        val entry = CreateEntry("Passwrd", authPendingIntent(ACTION_CREATE_CREDENTIAL, itemId = null))
        callback.onResult(BeginCreateCredentialResponse(listOf(entry)))
    }

    override fun onClearCredentialStateRequest(
        request: ProviderClearCredentialStateRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<Void?, ClearCredentialException>,
    ) {
        callback.onResult(null)
    }

    private fun authPendingIntent(action: String, itemId: String?): PendingIntent {
        val intent = Intent(action, null, this, CredentialAuthActivity::class.java)
        itemId?.let { intent.putExtra(EXTRA_ITEM_ID, it) }
        return PendingIntent.getActivity(
            this,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
    }
}
