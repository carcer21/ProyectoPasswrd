# ADR 0007 — Toda la cripto de `:core` es `suspend`

## Estado
Aceptado (Fase 9b).

## Contexto
`Aes256Gcm`, `Argon2id`, `EcP256`, `hmacSha256`, `sha256` eran funciones síncronas (JCA/
BouncyCastle en Android/JVM). El navegador sólo da cripto nativa async (`SubtleCrypto` vía
`Promise`); Argon2id encima no es nativo del navegador en absoluto.

## Decisión
Toda la API de cripto en `commonMain` es `suspend` (incluye `EncryptedBlob`, `Hkdf`,
`VaultExporter`/`VaultImporter`, `AuthenticatorData`/`ClientDataJson` en `webauthn/`). Los `actual`
de Android/JVM (JCA + BouncyCastle) tienen cuerpo síncrono envuelto en `suspend fun` — sin cambio
de comportamiento ahí, sólo de firma. El `actual` de `wasmJs` usa `WebCrypto SubtleCrypto` para
AES-GCM/HMAC/SHA-256/EC P-256, y `hash-wasm` (WASM puro) para Argon2id, que `SubtleCrypto` no
soporta.

`secureRandomBytes` es la única excepción: sigue síncrona en las tres plataformas porque
`crypto.getRandomValues` (a diferencia del resto de `SubtleCrypto`) no es async.

## Consecuencias
- Cascada real: `PinUnlockManager`, `CredentialAuthActivity` y las pantallas de PIN en
  `androidApp` pasaron a `suspend`/`scope.launch{}` — ver commits de esta fase. `desktopApp` no se
  vio afectado (nunca llamaba cripto directamente, sólo a través de `VaultSession`, que ya era
  `suspend`).
- Interop Kotlin/Wasm: los tipos `Uint8Array`/`ArrayBuffer` de `org.khronos.webgl` (Kotlin/JS
  clásico) NO existen en `wasmJs` — los bytes viajan como `JsAny` opaco, construidos elemento a
  elemento vía `js("...")`. Cada llamada `js(...)` tiene que ser una expresión propia (no un
  argumento inline) — restricción del compilador, no cosmética.
- WebCrypto firma ECDSA en raw IEEE P1363 (r‖s, 64 bytes); JCA y WebAuthn piden DER (`SEQUENCE`
  de dos `INTEGER`). El actual de `wasmJs` hace la conversión a mano en ambas direcciones — sin
  eso, las passkeys generadas en el navegador no validarían contra un RP real.
- El vector de test oficial RFC 9106 (usa `secret`/`associatedData`) sólo corre en `jvmTest`:
  `hash-wasm` no expone esos dos parámetros. La producción (ver `CRYPTO_SPEC.md`) tampoco los usa.
- **Pendiente de verificar en un navegador real**: esta ADR documenta una implementación que
  compila (`:core:compileKotlinWasmJs` en verde) pero `:core:wasmJsBrowserTest` no pudo correr en
  esta sesión por falta de Chrome en el entorno. Antes de confiar en el resultado byte a byte
  (vectores NIST/RFC vía WebCrypto/hash-wasm), hay que correr los tests en un navegador real.
