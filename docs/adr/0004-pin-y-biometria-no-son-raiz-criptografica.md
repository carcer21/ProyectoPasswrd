# ADR 0004 — PIN y biometría son atajos envueltos en Keystore, no raíces criptográficas

## Estado
Aceptado (Fase 4).

## Contexto
Un PIN de 6 dígitos son 10⁶ combinaciones — cae en segundos offline aunque pase por Argon2id. La
biometría no es un secreto derivable en absoluto.

## Decisión
La única raíz criptográfica real es la contraseña maestra. PIN y biometría envuelven la Vault Key
con Argon2id(PIN) → AES-GCM, y ese blob se cifra otra vez con una clave del Android Keystore
(`setIsStrongBoxBacked` si el dispositivo lo soporta, no exportable). La resistencia la aporta el
hardware (TEE/StrongBox), no el secreto del usuario.

## Consecuencias
- Con el fichero robado pero sin el TEE del dispositivo, el pinBlob es ruido — no hay ataque
  offline posible sobre el PIN aislado del hardware.
- Contador de intentos dentro del blob cifrado: 5 fallos borran el pinBlob y fuerzan volver a la
  contraseña maestra, sin perder el vault.
- Biometría exige `BiometricPrompt.CryptoObject(cipher)`, nunca sólo reaccionar a
  `onAuthenticationSucceeded` — un flujo que no ata la autenticación al objeto criptográfico se
  salta con un hook de Frida en dos líneas.
