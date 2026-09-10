# Especificación criptográfica — Passwrd v1

Documento vivo. Cualquier cambio aquí es un cambio de formato de vault y debe ir versionado
(ver "Formato de blob cifrado" abajo, byte 0). Fuente original: plan de arquitectura,
`C:\Users\Coti\.claude\plans\soy-un-programador-el-shimmying-turtle.md`.

## Jerarquía de claves

```
Contraseña maestra  (nunca se guarda, nunca sale del dispositivo)
      │  Argon2id(salt = 16B aleatorios del header, m, t, p calibrados; salida 32B)
      ▼
Master Key (MK, 32B)                                    ── sólo en memoria, se pone a cero al bloquear
      │  HKDF-SHA256(ikm = MK, salt = header.salt, info = "passwrd.v1.kek", L = 32)
      ▼
KEK (32B)
      │  AES-256-GCM   (envuelve)
      ▼
Vault Key (VK, 32B, CSPRNG)                             ── guardada envuelta en el header del vault
      │  AES-256-GCM   (envuelve)
      ▼
Item Key (IK, 32B, CSPRNG, una por elemento)            ── guardada envuelta en la fila del item
      │  AES-256-GCM
      ▼
Payload del item (JSON serializado) → ciphertext
```

**Por qué AES-256-GCM y no AES-CBC+HMAC:** AEAD de una pieza (sin riesgo de orden encrypt-then-MAC),
aceleración por hardware en ARMv8, y disponible nativamente en JCA (Android/JVM) y WebCrypto
`SubtleCrypto` (navegador) — cero dependencias externas para el cifrado en sí. El nonce es aleatorio
de 96 bits por operación y cada item tiene su propia clave, lo que acota el riesgo de reutilización.

## Formato de blob cifrado

Un único `ByteArray`, idéntico en todas las plataformas:

```
byte 0        : versión de formato (0x01)
bytes 1..12   : nonce (96 bits, CSPRNG)
bytes 13..N-17: ciphertext
bytes N-16..N : tag GCM (128 bits)
```

## Parámetros Argon2id

Arranque: `m = 64 MiB, t = 3, p = 1`, guardados **en el header del vault** (permite subirlos sin
romper vaults existentes). `p = 1` a propósito — BouncyCastle recorre los lanes de forma secuencial,
así que `p > 1` sólo penaliza al defensor. En onboarding, calibrar `t` con objetivo ~1s en el
dispositivo real y persistir el valor resultante.

## Verificación de la contraseña

**No se almacena ningún hash verificador.** Se intenta desenvolver la Vault Key; si el tag GCM no
valida, la contraseña es incorrecta. Un atacante con el fichero robado paga el Argon2id completo por
cada intento — no hay oráculo barato.

## Rotación

| Operación | Coste |
|---|---|
| Cambiar contraseña maestra | Re-derivar KEK y re-envolver **sólo** la VK — O(1) |
| Rotar Vault Key | Re-envolver las item keys — O(n), n pequeño |
| Rotar un item | Nueva IK, re-cifrar ese payload — O(1) |

## Desbloqueo por PIN

El PIN **no es una raíz criptográfica** (10⁶ combinaciones para 6 dígitos, offline caería en
segundos). La resistencia la aporta el hardware:

```
PIN → Argon2id(salt_pin) → PDK → AES-GCM envuelve VK → pinBlob
pinBlob → cifrado OTRA VEZ con clave AES del Android Keystore
          alias "passwrd.pin.kek", StrongBox si el dispositivo lo soporta,
          no exportable, muere al desinstalar o borrar el dispositivo
```

Contador de intentos dentro del blob cifrado; a los 5 fallos se borra el pinBlob y se exige
contraseña maestra. Opcional: `setMaxUsageCount()` (API 31+) para un límite aplicado por hardware
que sobrevive a restauraciones del fichero.

## Desbloqueo biométrico

```
VK envuelta por clave Keystore "passwrd.bio.kek":
    setUserAuthenticationRequired(true)
    setUserAuthenticationParameters(0, AUTH_BIOMETRIC_STRONG)   // auth en CADA uso
    setInvalidatedByBiometricEnrollment(true)                    // huella nueva → clave muerta
```

**Requisito no opcional:** la clave se usa siempre vía `BiometricPrompt.CryptoObject(cipher)`. Un
flujo que sólo reacciona a `onAuthenticationSucceeded()` sin objeto criptográfico se salta con Frida
en dos líneas.

Manejar `KeyPermanentlyInvalidatedException` y `UnrecoverableKeyException` → borrar el blob y caer a
contraseña maestra sin perder el vault.

## Higiene de memoria y superficie

- Claves en `ByteArray`, `fill(0)` al bloquear. Nunca en `String`.
- `FLAG_SECURE` en la ventana.
- `android:allowBackup="false"` + `dataExtractionRules` vacías.
- **Sin permiso `INTERNET`.**
- Auto-lock: temporizador configurable, al pasar a segundo plano, al apagar pantalla.

## Modelo de datos preparado para sync

```kotlin
@Entity
data class VaultItemEntity(
    @PrimaryKey val id: String,      // UUIDv7
    val type: Int,                   // en claro: filtrar sin descifrar
    val wrappedItemKey: ByteArray,   // IK envuelta con VK
    val encPayload: ByteArray,       // nombre, usuario, password, notas, URIs, TOTP…
    val createdAt: Long,
    val updatedAt: Long,
    val revision: Long,
    val deletedAt: Long?,            // tombstone
)
```

Metadatos en claro: `id`, `type`, timestamps. Ver `THREAT_MODEL.md` para la fuga que esto implica.
