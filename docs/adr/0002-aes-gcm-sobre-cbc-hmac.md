# ADR 0002 — AES-256-GCM en vez de AES-CBC + HMAC

## Estado
Aceptado (Fase 1).

## Contexto
Bitwarden histórico usa AES-CBC-256 + HMAC-SHA256 (encrypt-then-MAC). Alternativa: un AEAD de una
sola pieza.

## Decisión
AES-256-GCM en toda la jerarquía de claves y en cada item. Nonce de 96 bits, CSPRNG, uno por
operación; cada item tiene su propia clave, lo que acota el riesgo de reutilización de nonce.

## Consecuencias
- Sin riesgo de implementar mal el orden encrypt-then-MAC (bug clásico de sistemas caseros).
- Aceleración por hardware en ARMv8, y disponible nativamente en JCA (Android/JVM) y WebCrypto
  `SubtleCrypto` (navegador) — cero dependencias externas para el cifrado en sí.
- Riesgo residual de GCM (colisión de nonce) mitigado por clave única por item + nonce aleatorio,
  no por nonce determinista/contador (más simple de implementar sin estado persistente).
