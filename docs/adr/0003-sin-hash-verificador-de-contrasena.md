# ADR 0003 — Sin hash verificador de contraseña maestra

## Estado
Aceptado (Fase 1-2).

## Contexto
Un gestor típico con backend guarda un hash de verificación separado de la clave de cifrado, para
poder decir "contraseña incorrecta" sin haber intentado descifrar nada.

## Decisión
No se guarda ningún verificador. La única prueba de que la contraseña es correcta es que el tag
GCM de la Vault Key valida al desenvolverla.

## Consecuencias
- Un atacante con el fichero robado no tiene oráculo barato: cada intento —legítimo o no— paga el
  Argon2id completo. Un hash verificador separado sería ese oráculo barato.
- Efecto secundario correcto: el mensaje de "contraseña incorrecta" no puede filtrar por timing si
  el vault existe o no, porque el trabajo es el mismo en ambos casos.
