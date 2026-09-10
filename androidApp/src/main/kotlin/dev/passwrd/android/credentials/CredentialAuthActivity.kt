package dev.passwrd.android.credentials

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.credentials.CreatePasswordRequest
import androidx.credentials.CreatePasswordResponse
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.GetCredentialResponse
import androidx.credentials.GetPasswordOption
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PasswordCredential
import androidx.credentials.PublicKeyCredential
import androidx.credentials.provider.PendingIntentHandler
import androidx.fragment.app.FragmentActivity
import dev.passwrd.android.PasswrdApplication
import dev.passwrd.android.ui.VaultUiState
import dev.passwrd.android.ui.VaultViewModel
import dev.passwrd.android.ui.unlock.UnlockScreen
import dev.passwrd.core.crypto.sha256
import dev.passwrd.core.model.ItemPayload
import dev.passwrd.core.model.ItemType
import dev.passwrd.core.model.VaultItem
import dev.passwrd.core.webauthn.AttestationObject
import dev.passwrd.core.webauthn.AuthenticatorData
import dev.passwrd.core.webauthn.Base64Url
import dev.passwrd.core.webauthn.ClientDataJson
import dev.passwrd.core.webauthn.CoseKey
import dev.passwrd.core.webauthn.PublicKeyRequestParser
import dev.passwrd.core.crypto.EcP256
import dev.passwrd.core.crypto.secureRandomBytes
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Resuelve tanto peticiones de lectura (`get`, firma de aserción o password) como de
 * creación (`create`, nueva passkey o password) — ver docs/CRYPTO_SPEC.md "Passkeys". Si el
 * vault está bloqueado, muestra [UnlockScreen] antes de continuar.
 */
class CredentialAuthActivity : FragmentActivity() {
    private val container get() = (application as PasswrdApplication).container
    private val vaultViewModel: VaultViewModel by viewModels { VaultViewModel.factory(container.vaultSession) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface {
                    val vaultState by vaultViewModel.state.collectAsState()
                    val unlockError by vaultViewModel.unlockError.collectAsState()

                    when (vaultState) {
                        VaultUiState.Loading -> Unit
                        VaultUiState.NeedsOnboarding -> {
                            // No hay vault creado todavía: no hay nada que ofrecer.
                            finish()
                        }
                        VaultUiState.Locked -> UnlockScreen(
                            activity = this@CredentialAuthActivity,
                            pinUnlockManager = container.pinUnlockManager,
                            biometricUnlockManager = container.biometricUnlockManager,
                            hasError = unlockError,
                            onUnlock = vaultViewModel::unlock,
                            onErrorShown = vaultViewModel::clearUnlockError,
                            onVaultKeyRecovered = vaultViewModel::unlockWithRecoveredVaultKey,
                        )
                        VaultUiState.Unlocked -> {
                            Text("Completando...")
                            LaunchedEffect(Unit) { handleRequest() }
                        }
                    }
                }
            }
        }
    }

    private suspend fun handleRequest() {
        when (intent.action) {
            ACTION_GET_CREDENTIAL -> handleGet()
            ACTION_CREATE_CREDENTIAL -> handleCreate()
            else -> finish()
        }
    }

    private suspend fun handleGet() {
        val request = PendingIntentHandler.retrieveProviderGetCredentialRequest(intent)
        val option = request?.credentialOptions?.firstOrNull()
        val itemId = intent.getStringExtra(EXTRA_ITEM_ID)
        if (option == null || itemId == null) {
            finish()
            return
        }
        val item = container.vaultRepository.findById(itemId)
        if (item == null) {
            finish()
            return
        }

        when (option) {
            is GetPublicKeyCredentialOption -> {
                val payload = item.payload as? ItemPayload.Passkey ?: return finish()
                val requestOptions = PublicKeyRequestParser.parseRequestOptions(option.requestJson)
                val callingOrigin = resolveOrigin(request.callingAppInfo)
                val packageName = request.callingAppInfo.packageName
                val origin = callingOrigin ?: androidOriginFor(packageName)
                val androidPackageName = if (callingOrigin == null) packageName else null

                val challenge = Base64Url.decode(requestOptions.challengeBase64)
                val clientDataJson = ClientDataJson.build("webauthn.get", challenge, origin, androidPackageName)
                val clientDataHash = ClientDataJson.hash(clientDataJson)
                val newSignCount = payload.signCount + 1
                val authenticatorData = AuthenticatorData.forAssertion(payload.rpId, newSignCount)
                val signature = EcP256.sign(Base64Url.decode(payload.privateKeyPkcs8Base64), authenticatorData + clientDataHash)

                val responseJson = buildJsonObject {
                    put("id", payload.credentialIdBase64)
                    put("rawId", payload.credentialIdBase64)
                    put("type", "public-key")
                    putJsonObject("response") {
                        put("clientDataJSON", Base64Url.encode(clientDataJson))
                        put("authenticatorData", Base64Url.encode(authenticatorData))
                        put("signature", Base64Url.encode(signature))
                        put("userHandle", payload.userHandleBase64)
                    }
                }.toString()

                container.vaultRepository.upsert(
                    item.copy(payload = payload.copy(signCount = newSignCount), updatedAt = System.currentTimeMillis(), revision = item.revision + 1),
                )

                setGetResult(PublicKeyCredential(responseJson))
            }
            is GetPasswordOption -> {
                val payload = item.payload as? ItemPayload.Login ?: return finish()
                setGetResult(PasswordCredential(payload.username, payload.password))
            }
            else -> finish()
        }
    }

    private fun setGetResult(credential: androidx.credentials.Credential) {
        val result = android.content.Intent()
        PendingIntentHandler.setGetCredentialResponse(result, GetCredentialResponse(credential))
        setResult(Activity.RESULT_OK, result)
        finish()
    }

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun handleCreate() {
        val request = PendingIntentHandler.retrieveProviderCreateCredentialRequest(intent)
        if (request == null) {
            finish()
            return
        }
        val now = System.currentTimeMillis()

        when (val callingRequest = request.callingRequest) {
            is CreatePublicKeyCredentialRequest -> {
                val creationOptions = PublicKeyRequestParser.parseCreationOptions(callingRequest.requestJson)
                val keyPair = EcP256.generateKeyPair()
                val credentialId = secureRandomBytes(32)
                val cosePublicKey = CoseKey.encodeP256PublicKey(keyPair.publicKeyX, keyPair.publicKeyY)

                val callingOrigin = resolveOrigin(request.callingAppInfo)
                val packageName = request.callingAppInfo.packageName
                val origin = callingOrigin ?: androidOriginFor(packageName)
                val androidPackageName = if (callingOrigin == null) packageName else null

                val challenge = Base64Url.decode(creationOptions.challengeBase64)
                val clientDataJson = ClientDataJson.build("webauthn.create", challenge, origin, androidPackageName)
                val authenticatorData = AuthenticatorData.forRegistration(creationOptions.rpId, 0, credentialId, cosePublicKey)
                val attestationObject = AttestationObject.build(authenticatorData)

                val responseJson = buildJsonObject {
                    put("id", Base64Url.encode(credentialId))
                    put("rawId", Base64Url.encode(credentialId))
                    put("type", "public-key")
                    putJsonObject("response") {
                        put("clientDataJSON", Base64Url.encode(clientDataJson))
                        put("attestationObject", Base64Url.encode(attestationObject))
                    }
                }.toString()

                val item = VaultItem(
                    id = Uuid.random().toString(),
                    type = ItemType.PASSKEY,
                    payload = ItemPayload.Passkey(
                        name = creationOptions.rpId,
                        rpId = creationOptions.rpId,
                        userHandleBase64 = creationOptions.userIdBase64,
                        userName = creationOptions.userName,
                        userDisplayName = creationOptions.userDisplayName,
                        credentialIdBase64 = Base64Url.encode(credentialId),
                        privateKeyPkcs8Base64 = Base64Url.encode(keyPair.privateKeyPkcs8),
                        publicKeyXBase64 = Base64Url.encode(keyPair.publicKeyX),
                        publicKeyYBase64 = Base64Url.encode(keyPair.publicKeyY),
                    ),
                    createdAt = now,
                    updatedAt = now,
                    revision = 1,
                )
                container.vaultRepository.upsert(item)

                val result = android.content.Intent()
                PendingIntentHandler.setCreateCredentialResponse(result, CreatePublicKeyCredentialResponse(responseJson))
                setResult(Activity.RESULT_OK, result)
                finish()
            }
            is CreatePasswordRequest -> {
                val item = VaultItem(
                    id = Uuid.random().toString(),
                    type = ItemType.LOGIN,
                    payload = ItemPayload.Login(name = callingRequest.id, username = callingRequest.id, password = callingRequest.password),
                    createdAt = now,
                    updatedAt = now,
                    revision = 1,
                )
                container.vaultRepository.upsert(item)

                val result = android.content.Intent()
                PendingIntentHandler.setCreateCredentialResponse(result, CreatePasswordResponse())
                setResult(Activity.RESULT_OK, result)
                finish()
            }
            else -> finish()
        }
    }

    /**
     * Origen sintético para llamadas de apps nativas (sin navegador de por medio) — ver
     * docs/THREAT_MODEL.md: es el propio hash de nuestro certificado de firma, no una
     * verificación de identidad de terceros.
     */
    private suspend fun androidOriginFor(packageName: String): String {
        val hash = try {
            val info = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            val cert = info.signingInfo?.apkContentsSigners?.firstOrNull()
            if (cert != null) sha256(cert.toByteArray()) else ByteArray(32)
        } catch (e: Exception) {
            ByteArray(32)
        }
        return "android:apk-key-hash:" + Base64Url.encode(hash)
    }

    /**
     * `getOrigin` sólo es no nulo cuando quien llama es un navegador privilegiado
     * retransmitiendo la petición de una web — y sólo si además coincide con
     * [PrivilegedApps.allowlistJson]. Si no coincide, la API lanza `IllegalStateException`
     * en vez de devolver null; lo tratamos igual que "no privilegiado" — ver
     * docs/CRYPTO_SPEC.md "Passkeys".
     */
    private fun resolveOrigin(callingAppInfo: androidx.credentials.provider.CallingAppInfo): String? =
        try {
            callingAppInfo.getOrigin(PrivilegedApps.allowlistJson)
        } catch (e: IllegalStateException) {
            null
        }
}
