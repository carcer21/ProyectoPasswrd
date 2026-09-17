# Passwrd — Estado del proyecto

Gestor de contraseñas local, sin servidor, de conocimiento cero. Android primero, núcleo
reutilizable para desktop y extensión de navegador. Ver `CRYPTO_SPEC.md` y `THREAT_MODEL.md` para
detalle técnico completo; este documento es el resumen ejecutivo.

## Decisiones clave

| Decisión | Elección |
|---|---|
| Núcleo compartido | Kotlin Multiplatform (`:core`, cero imports de `android.*`) |
| Desbloqueo | Contraseña maestra = única raíz criptográfica. PIN y biometría son atajos envueltos en Android Keystore, no raíces |
| Cifrado | AES-256-GCM en cascada: contraseña → Argon2id → Master Key → HKDF → KEK → Vault Key → Item Key (una por item) → payload |
| Verificación de contraseña | Sin hash verificador — se intenta desenvolver la Vault Key; si el tag GCM no valida, es incorrecta |
| Autofill | Los dos: `AutofillService` (API 26+) y `CredentialProviderService` (API 34+, con passkeys) |
| Passkeys | Par P-256 por RP, clave privada dentro del vault cifrado (no en Keystore, necesita poder exportarse) |
| Sync | Sin permiso `INTERNET`. Export/import cifrado. Esquema de datos ya preparado para sync futuro (`updatedAt`/`revision`/`deletedAt`) |
| Almacenamiento | Room, sin SQLCipher — el cifrado ya es por campo, SQLCipher sería redundante y ataría a Android |

Razonamiento completo de cada una en `docs/adr/` (8 ADRs: jerarquía de claves, GCM vs CBC+HMAC,
sin hash verificador, PIN/bio no-raíz, sin INTERNET, Room fuera de commonMain, cripto
suspend/WebCrypto, browserExtension sin Compose Web).

## Estructura

```
ProyectoPasswrd/
├─ core/            # KMP puro: crypto, model, vault, storage, generator, matching, transfer, webauthn
├─ androidApp/       # UI Compose, autofill, credentials, platform (Keystore/PIN/bio), di
├─ desktopApp/       # Compose Multiplatform desktop (JVM) — v1 funcional, ver estado abajo
├─ browserExtension/ # MV3 + Kotlin/Wasm — v1 empaqueta, sin probar en navegador (ver Fase 9b)
└─ docs/             # CRYPTO_SPEC.md, THREAT_MODEL.md, adr/, PROJECT_STATUS.md (este fichero)
```

## Estado por fase

| Fase | Contenido | Estado |
|---|---|---|
| 0 | Esqueleto Gradle KMP, specs escritas antes del código | ✅ |
| 1 | `crypto/`: Argon2id, AES-GCM, HKDF, KeyRing, zeroize | ✅ tests RFC 9106 / NIST GCM en verde |
| 2 | `storage/` + `vault/`: Room, repositorio, máquina de estados lock/unlock | ✅ round-trip + detección de manipulación |
| 3 | UI Compose: onboarding, desbloqueo, lista, detalle, editor, generador, ajustes | ✅ |
| 4 | `platform/`: Keystore, PIN, biometría, auto-lock, FLAG_SECURE | ✅ probado en dispositivo real |
| 5 | `AutofillService` completo | ✅ probado en Chrome y app nativa |
| 6 | `CredentialProviderService` + passkeys | ✅ probado contra webauthn.io |
| 7 | Export/import cifrado + importadores Bitwarden/Proton/CSV | ✅ round-trip sin pérdida |
| 8 | Endurecimiento: R8, sin INTERNET, sin backup, sin logs, ADRs | ✅ auditado en APK release |
| 9a | `desktopApp` (Compose Multiplatform JVM) | ✅ v1 funcional, probado en local |
| 9b | `browserExtension` (Kotlin/Wasm + MV3) | ✅ probado en navegador real: crear vault, listar, ver/copiar, editar y borrar login |

## Verificación realizada

- CI en GitHub Actions (`.github/workflows/ci.yml`): `:core:jvmTest` en cada push/PR a `master`
  (Android SDK instalado manualmente en el runner; `androidApp` no tiene tests propios que
  `testDebugUnitTest` no cubra ya vía `commonTest`, así que CI se queda en jvmTest).
- `./gradlew :core:allTests` — verde (vectores Argon2id, AES-GCM, round-trips de vault).
- `./gradlew :androidApp:assembleRelease` — verde, R8/minify activo.
- `aapt dump permissions` sobre el APK release: sin `INTERNET`.
- `aapt dump xmltree` sobre el manifest final: `allowBackup=0x0`, no `debuggable`.
- Grep de `Log.`/`println` en todo `androidApp/` y `core/`: cero — sin fugas por logcat.
- Checklist manual completo en dispositivo real (Honor CRT-NX1): vault nuevo + medidor de fuerza,
  contraseña incorrecta, PIN con borrado a 5 fallos, biometría con invalidación al re-enrolar,
  inspección de la DB con `sqlite3` (campos cifrados opacos), autofill en Chrome y app nativa
  (caso negativo de dominio no coincidente probado), passkey contra webauthn.io, export/import
  ida y vuelta.
- Sesión 2026-09-17 en emulador (`passwrd_test`, API 34): crear vault, crear 3 logins, abrir el
  detalle de cada uno para editar. Encontró un bug real — ver abajo.

### Bug encontrado y arreglado: editar item cargaba el formulario vacío

`MainActivity.kt` navega entre pantallas a mano (comentario explícito: "sin Navigation-Compose,
la app tiene 5 pantallas, no hace falta"). El `viewModel(factory = ...)` de `ItemEditViewModel` no
llevaba `key`, así que Compose cacheaba **la misma instancia** de ViewModel en cada visita a
`Screen.Edit(itemId)`, sin importar qué `itemId` llegara. La primera vez que se abría la pantalla
(al crear un item nuevo, `itemId = null`) quedaba esa instancia fijada para siempre — abrir
cualquier item existente después reutilizaba esa instancia vieja con `itemId = null` en vez de
cargar el item real, y el formulario aparecía vacío. Reproducido con dos items distintos en el
emulador, confirmado por código, arreglado con `viewModel(key = current.itemId ?: "new", ...)` y
reverificado en el mismo emulador con los mismos items. No se detectó antes porque el flujo de
prueba manual siempre creaba un item y lo dejaba así, sin volver a abrirlo para editar.

## desktopApp (Fase 9a)

Módulo JVM puro (`org.jetbrains.kotlin.jvm` + Compose Multiplatform desktop), no KMP — reutiliza
`:core` (crypto, vault, storage, transfer) tal cual, sin ninguna implementación nueva ahí: el
`jvmMain` de `:core` ya existía desde la Fase 1-2. La UI reutiliza literalmente los composables de
`androidApp` que no tenían imports de `android.*` (`ItemListScreen`, `ItemEditScreen`,
`CreateVaultScreen`) y reescribe versiones simplificadas de las pantallas que sí dependían de
Android:

- **Sin PIN ni biometría.** Ambos son atajos envueltos en Android Keystore (ver ADR 0004) —
  desktop no tiene ese hardware. v1 sólo desbloquea con contraseña maestra.
- **Sin `androidx.lifecycle.ViewModel`.** No hay Activity cuyos cambios de configuración
  sobrevivir; los ViewModel de desktop son clases planas que reciben un `CoroutineScope` de
  `rememberCoroutineScope()`.
- **Clipboard** vía `java.awt.Toolkit`, no `ClipboardManager` de Android.
- **Export/import** vía `JFileChooser` (Swing), no Storage Access Framework.
- **Vault en disco** en `~/.passwrd/vault.db` (Room JVM + `BundledSQLiteDriver`, mismo motor que
  Android).

Probado en local: crear vault, desbloqueo, añadir/editar/listar items.

## Fase 9b — cerrada

Dos bloqueos de compilación se resolvieron primero, en este orden:

1. **Toda la cripto de `:core` pasó a `suspend`** (ADR 0007) — necesario porque el navegador sólo
   da cripto async (`WebCrypto SubtleCrypto`). Cascada real hacia `androidApp` (PIN, passkeys);
   `desktopApp` no se vio afectado.
2. **`storage/` salió de `commonMain`** (ADR 0006) — Room 2.7.0 no publica variante `wasmJs` (Room
   3.0, que lo prometía, todavía no existe). `VaultSession`/`VaultRepository` ahora dependen de la
   interfaz `VaultStore`, no de Room directamente. `RoomVaultStore` (Android/JVM) vive en
   `jvmCommonMain`.

El módulo `browserExtension` existe completo (ver ADR 0008): manifest MV3, `ChromeStorageVaultStore`
(`chrome.storage.local` detrás de `VaultStore`), popup con DOM vanilla (onboarding/unlock/lista +
alta de login + ver/copiar contraseña). `./gradlew :browserExtension:wasmJsBrowserDistribution`
genera `browserExtension/build/dist/wasmJs/productionExecutable/` — carpeta cargable tal cual en
`chrome://extensions` (Modo desarrollador → Cargar descomprimida).

**Probado en Chrome real end-to-end**, después de cazar cuatro bugs reales de interop
Kotlin/Wasm ↔ JS (ninguno documentado claramente en la documentación oficial a la fecha):

1. **`@JsModule` en una función/val suelta importa el *default export*, no un named export.**
   `hash-wasm` no tiene `export default` → `s.default is not a function`, y tras envolverlo en un
   `external val` con `@JsModule` seguía atado a `.default` (ahora vía getter) → `undefined` →
   `NullPointerException` al acceder. Arreglo real: `@JsModule("hash-wasm") external object
   HashWasm { fun argon2id(...) }` — un `external object` importa el módulo completo y sus
   miembros pasan a ser los named exports. Ver `core/src/wasmJsMain/.../Argon2id.wasmJs.kt`.
2. **`crypto.subtle.{sign,digest,encrypt,decrypt,exportKey}` resuelven a `ArrayBuffer`, no a
   `Uint8Array`.** Nuestra conversión a `ByteArray` asumía `Uint8Array` (`.length`, `array[i]`);
   sobre un `ArrayBuffer` esos son `undefined`, así que devolvía silenciosamente arrays de
   longitud 0 en vez de fallar alto — apareció como `IndexOutOfBoundsException` río abajo, en
   HKDF. Arreglo: `bufferToByteArray()` envuelve el resultado en `new Uint8Array(buffer)` antes de
   leer. Ver `WebCrypto.kt`.
3. **El devtool `eval` por defecto del build de desarrollo viola el CSP de MV3**
   (`script-src 'self' 'wasm-unsafe-eval'`, sin `unsafe-eval`). Sólo importa para depurar con
   nombres reales — producción no usa `eval`. Arreglo: `browserExtension/webpack.config.d/
   mv3-csp.js` fuerza `devtool = 'source-map'` en ambos builds.
4. Los tres bugs de arriba se depuraron sin nombres de función legibles porque el build de
   producción minifica con Terser (`$funcNNNN` en el `.wasm`, sin mapear). Para el próximo bug así,
   usar `wasmJsBrowserDevelopmentExecutableDistribution` (nombres reales, ahora sin `eval` gracias
   al punto 3) y/o instrumentar con stage tags como en `VaultSession.createVault` (quedaron ahí,
   son baratos y ya salvaron tiempo dos veces).

## Pendiente / v2+

- Correr `:core:wasmJsBrowserTest` para validar los vectores NIST/RFC contra WebCrypto/hash-wasm
  de verdad — hoy falla en este entorno por falta de Chrome headless instalado para Karma, no por
  el código; es la única red de seguridad que falta dado que los bugs de esta sesión (ArrayBuffer
  vs Uint8Array, `@JsModule` default export) son justo el tipo de cosa que un vector conocido
  habría detectado en CI en vez de a mano en el popup.
- `browserExtension` v2: content scripts para autofill en páginas web, export/import, passkeys —
  crear/listar/ver/copiar/editar/borrar ya implementado y probado en navegador real, sigue
  faltando lo demás.
- Sync entre dispositivos: esquema de datos ya preparado, sin protocolo ni servidor.
- Autofill en apps nativas: sólo heurística de nombre de paquete en v1, sin base de datos de
  hashes de certificado esperados por dominio (ver `THREAT_MODEL.md`).
- `desktopApp` v2: PIN/biometría vía credential store del SO (Keychain/DPAPI/Secret Service),
  auto-lock por inactividad, empaquetado nativo (`packageDistributionForCurrentOS`).
