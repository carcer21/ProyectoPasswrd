# ADR 0008 — `browserExtension` sin Compose Web, sólo DOM vanilla

## Estado
Aceptado (Fase 9b).

## Contexto
Con `:core` ya compilando para `wasmJs` (ADR 0006, 0007), faltaba el módulo `browserExtension`
en sí: manifest MV3, persistencia y UI del popup.

## Decisión
- **Persistencia**: `ChromeStorageVaultStore` implementa la interfaz `VaultStore` de `:core` sobre
  `chrome.storage.local`. Todo el vault (header + items) se serializa como un único JSON
  (kotlinx.serialization) con los `ByteArray` en Base64, bajo una sola clave. El popup se recrea
  en cada apertura — `load()` hidrata un caché en memoria una vez al arrancar; de ahí en adelante
  se escribe a través (`persist()`) en cada cambio. Sin `chrome.storage.onChanged`: no hace falta
  para v1, un solo popup a la vez.
- **UI**: DOM vanilla vía `js(...)` (mismo patrón que `WebCrypto.kt`), no Compose Web ni
  kotlinx-browser. `popup.html` trae las tres pantallas (onboarding/unlock/list) ya en el HTML,
  ocultas con una clase `hidden`; `Main.kt` sólo hace show/hide, lee inputs y renderiza la lista
  con `innerHTML`. Menos superficie nueva que auditar que sumar un segundo framework de UI.
- **Sin PIN/biometría** (igual que `desktopApp`, ver ADR 0004): el navegador no tiene Keystore.
  v1 sólo desbloquea con contraseña maestra, sesión viva sólo mientras el popup está abierto (sin
  cache de Vault Key entre aperturas — más simple y más seguro que empezar a inventar un
  session storage propio).
- **CSP obligatorio**: MV3 exige `'wasm-unsafe-eval'` en `content_security_policy.extension_pages`
  para poder instanciar WebAssembly — sin esto, la extensión carga pero el wasm falla en
  silencio al primer `WebAssembly.instantiate`.

## Consecuencias
- `:browserExtension:wasmJsBrowserDistribution` genera una carpeta cargable directamente como
  extensión sin empaquetar en Chrome (`chrome://extensions` → Modo desarrollador → Cargar
  descomprimida → `browserExtension/build/dist/wasmJs/productionExecutable/`).
- El bundle de producción pesa ~360 KiB de wasm + ~275 KiB de JS (mayormente `hash-wasm`) — sin
  optimizar todavía, aceptable para v1 de una extensión de escritorio.
- **No probado en un navegador real todavía** (ver ADR 0007): compila y empaqueta, pero nadie
  abrió el popup en Chrome. Antes de confiar en el flujo completo (crear vault → añadir login →
  cerrar y reabrir → sigue ahí), hay que probarlo a mano.
- Fuera de alcance de v1: autofill en páginas (content scripts), passkeys, importar/exportar,
  edición/borrado de items desde el popup — sólo alta y listado. Se agrega cuando haga falta,
  reusando el mismo patrón de interop ya establecido.
