# Handoff — retomar aquí

Última sesión: 2026-09-17.

## Qué se hizo esta sesión

1. **CI en GitHub Actions** (`.github/workflows/ci.yml`): `:core:jvmTest` + `:core:testDebugUnitTest`
   en cada push/PR a `master`. Badge en el README.
2. **README con badges** (CI + licencia) y captura de pantalla del `desktopApp` (pantalla de
   desbloqueo, `docs/screenshots/desktopapp-unlock.png`).
3. **Verificado en navegador real por el usuario**: `browserExtension` cargada como extensión
   descomprimida — crear vault, listar, ver/copiar, **editar y borrar login** funcionan. Cierra el
   pendiente que quedaba abierto desde la sesión anterior (commit `623eeaf`).

Commits de la sesión: `515e763` (ci + badges).

## Cómo retomar

```bash
cd "/c/Users/Coti/Documents/ProyectoPasswrd"
export JAVA_HOME="$LOCALAPPDATA/Programs/Android Studio/jbr"
git log --oneline -5                       # ubicarse
./gradlew :core:jvmTest :core:testDebugUnitTest   # confirmar que todo sigue verde
```

## Pendiente / próximos pasos (por orden de impacto sugerido)

- [ ] `browserExtension` v2: content scripts para autofill en páginas web, export/import,
      passkeys — v1 sigue sin nada de esto.
- [ ] Sync entre dispositivos: esquema de datos ya preparado (`updatedAt`/`revision`/`deletedAt`),
      sin protocolo ni servidor.
- [ ] Autofill nativo Android: hoy sólo heurística de nombre de paquete; falta base de hashes de
      certificado esperados por dominio (ver `THREAT_MODEL.md`).
- [ ] `desktopApp` v2: PIN/biometría vía credential store del SO (Keychain/DPAPI/Secret Service),
      auto-lock por inactividad, empaquetado nativo.
- [ ] `:core:wasmJsBrowserTest` falla en esta máquina por falta de Chrome headless instalado
      (`CHROME_BIN` sin resolver) — no es bug de código, instalar Chrome resolvería la única red
      de seguridad que falta para los vectores NIST/RFC contra WebCrypto/hash-wasm reales.
- [ ] Capturas adicionales para el README (Android: lista de items, autofill en acción) — hoy sólo
      hay una del `desktopApp`.

Detalle técnico completo (spec cripto, modelo de amenazas, ADRs, estado fase por fase) en
`docs/CRYPTO_SPEC.md`, `docs/THREAT_MODEL.md`, `docs/adr/`, `docs/PROJECT_STATUS.md`.
