# Modelo de amenazas — Passwrd v1

## Qué protege

- **Confidencialidad del vault en reposo.** Sin la contraseña maestra (o el PIN/biometría del
  dispositivo que la desbloquea), el contenido de cada item es indistinguible de ruido aleatorio:
  AES-256-GCM con clave por item, envuelta en cascada hasta la contraseña maestra vía Argon2id.
  Ver `CRYPTO_SPEC.md`.
- **Ataques de fuerza bruta offline sobre el fichero robado.** No hay hash verificador barato — cada
  intento paga el Argon2id completo (m=64MiB, t calibrado).
- **Exfiltración por red.** Sin permiso `INTERNET` en el manifest: la app no puede llamar a casa
  aunque quisiera. Auditable en segundos con `aapt dump permissions`.
- **Backup/transferencia de sistema.** `allowBackup=false` + `dataExtractionRules` vacías: el vault
  cifrado no sale por Auto Backup de Google ni por la transferencia de dispositivo a dispositivo.
- **Captura de pantalla / recientes.** `FLAG_SECURE` bloquea capturas y miniatura en el selector de
  apps recientes.
- **Robo del dispositivo desbloqueado con PIN/biometría comprometidos.** Contador de intentos con
  borrado del atajo tras 5 fallos; la contraseña maestra sigue siendo necesaria tras eso.
- **Entrega de credenciales a la app o dominio equivocado (autofill), con matices.** Para
  navegadores conocidos, el `webDomain` que entrega `AssistStructure` (viene del propio motor
  de renderizado, no lo declara la app) se compara con el host guardado — coincidencia fuerte.
  Para apps nativas, v1 sólo tiene una heurística sobre el nombre de paquete (ver
  `RequesterIdentity.kt`) — **no** hay una base de datos de hashes de certificado esperados
  por dominio (eso es infraestructura a la escala de Bitwarden/Proton, fuera de alcance de
  v1). El principio que sí se mantiene siempre: si no hay coincidencia, no se ofrece nada —
  fallar cerrado.

## Qué NO protege (concesiones deliberadas)

- **Metadatos en claro.** `id`, `type` y timestamps de cada item son visibles sin descifrar —
  necesario para filtrar/ordenar sin pagar el coste de descifrar todo el vault en cada listado.
  Un atacante con el fichero sabe *cuántos* items hay y *de qué tipo*, no su contenido.
- **Dispositivo comprometido en tiempo de ejecución.** Si el atacante tiene control del proceso
  mientras el vault está desbloqueado (root, malware con accesibilidad, debugger adjunto), puede
  leer memoria. Mitigamos con `fill(0)` al bloquear y auto-lock agresivo, pero no es un modelo de
  amenaza que este proyecto pretenda resolver por completo.
- **La contraseña maestra pasa por `String` en algún punto de Compose.** `TextField` entrega
  `String`, inmutable, puede quedar en el heap hasta el siguiente GC. Se minimiza convirtiendo a
  `ByteArray` cuanto antes, pero no hay solución limpia en Compose hoy.
- **Ingeniería social / keylogging del propio dispositivo.** Fuera de alcance de un gestor local.
- **Passkeys: la clave privada vive en el vault cifrado, no en Keystore** (a diferencia del resto de
  claves), porque necesita poder exportarse/sincronizarse. Esto es coherente con cómo lo hacen
  Bitwarden/Proton Pass, pero es una decisión consciente de exponer esa clave al mismo modelo de
  amenaza que el resto del vault, no al hardware-backed.

## Superficie de ataque más sensible: autofill

Ver `CRYPTO_SPEC.md` fase 5/6 del plan. El fallo más grave posible en todo el proyecto es entregar
una credencial a la app o dominio equivocado. Cualquier cambio en `autofill/` o `credentials/` que
toque la identificación del solicitante requiere revisión explícita antes de mergear.

## Fuera de alcance en v1

- Sincronización entre dispositivos (el esquema está preparado, pero no hay servidor ni protocolo
  todavía).
- Recuperación de cuenta si se pierde la contraseña maestra: no existe recuperación por diseño (zero
  knowledge real). Debe comunicarse muy claramente en el onboarding.
