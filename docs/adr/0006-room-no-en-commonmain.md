# ADR 0006 — Room fuera de `commonMain`, detrás de `VaultStore`

## Estado
Aceptado (Fase 9b).

## Contexto
Room 2.7.0 (la única versión publicada — 3.0, que preveía soporte KMP completo incluido wasm,
todavía no existe) no publica variante para el target `wasmJs`. `storage/` vivía en `commonMain`
con entidades y DAOs anotados de Room, lo que bloqueaba compilar `:core` para `wasmJs` sin importar
qué tan resuelta estuviera la cripto.

## Decisión
`commonMain` define `VaultStore` (interfaz) + `VaultHeaderData`/`VaultItemData` (data classes
planas, sin anotaciones de Room). `VaultSession`/`VaultRepository` dependen sólo de eso. Las
entidades/DAOs/`PasswrdDatabase` con anotaciones de Room, y `RoomVaultStore` (la implementación),
se movieron a `jvmCommonMain` — compartido por Android y JVM (desktop), igual que ya se hacía con
`crypto/`. `browserExtension` implementará su propio `VaultStore` con `chrome.storage.local`.

## Consecuencias
- `:core` compila para `wasmJs` sin arrastrar Room.
- Los constructores de `RoomDatabase.Builder` (`vaultDatabaseBuilder`) siguen por plataforma
  (`androidMain` necesita `Context`, `jvmMain` sólo una ruta) — no se pudieron subir a
  `jvmCommonMain` porque el Room de Android no tiene el overload sin `Context`.
- Si algún día sale Room 3.0 con soporte wasm real, esta capa de interfaz sigue siendo útil (no
  hay que deshacerla) — sólo haría falta un `WasmRoomVaultStore` en vez de uno basado en
  `chrome.storage`.
