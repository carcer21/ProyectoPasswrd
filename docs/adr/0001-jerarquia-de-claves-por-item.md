# ADR 0001 — Jerarquía de claves por item, no por bóveda monolítica

## Estado
Aceptado (Fase 0-1).

## Contexto
Bitwarden cifra por item con una Cipher Key propia; Proton Pass usa vault key + item key. Ambos
comparten el patrón: la contraseña maestra nunca cifra datos directamente, sólo envuelve una clave
aleatoria intermedia.

## Decisión
Cadena de cuatro capas: contraseña maestra → Argon2id → Master Key → HKDF → KEK → envuelve Vault
Key → envuelve Item Key (una por item) → cifra el payload. Ver `docs/CRYPTO_SPEC.md`.

## Consecuencias
- Cambiar la contraseña maestra es O(1): sólo se re-deriva el KEK y se re-envuelve la VK.
- Rotar un item es O(1): nueva IK, re-cifrar sólo ese payload.
- Coste: una capa más de indirección que un esquema de clave única — aceptable, el vault es
  personal y pequeño.
