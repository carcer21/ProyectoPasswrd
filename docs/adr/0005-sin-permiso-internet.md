# ADR 0005 — Sin permiso INTERNET en el manifest

## Estado
Aceptado (Fase 0), verificado en release (Fase 8).

## Contexto
El proyecto es local-first, sin servidor, de conocimiento cero. Cualquier permiso de red es
superficie de exfiltración, intencional o por una dependencia comprometida.

## Decisión
El manifest no declara `android.permission.INTERNET`. Sync queda para v2+ (esquema ya preparado en
`VaultItemEntity` con `updatedAt`/`revision`/`deletedAt`, pero sin protocolo ni servidor).

## Consecuencias
- Auditable en segundos por cualquiera con `aapt dump permissions` — verificado en
  `androidApp-release-unsigned.apk`: no aparece `INTERNET`.
- Bloquea de raíz cualquier librería o código futuro que intente llamar a casa, aunque el resto de
  la app esté comprometido — Android deniega la conexión a nivel de kernel, no de lógica de la app.
- Si algún día se añade sync, este ADR debe revisarse explícitamente — no es un permiso que se
  pueda colar sin darse cuenta.
