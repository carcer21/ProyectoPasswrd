# Handoff — retomar aquí

Última sesión: 2026-09-10.

## Qué se hizo esta sesión

1. **Repo git creado e inicializado** (no existía antes, pese a que se pensaba que sí).
   Remoto: https://github.com/carcer21/ProyectoPasswrd — **público**.
2. **README.md + LICENSE (MIT)** añadidos en la raíz — el repo ya se ve presentable para
   portfolio.
3. **`browserExtension`: editar y borrar items de login** (v1 sólo tenía alta/listado/ver).
   - Editar repuebla el formulario de alta y reutiliza `VaultRepository.upsert` (mismo `id`,
     `revision + 1`). Sólo para logins, porque es el único payload que el form construye.
   - Borrar pide confirmación (`window.confirm`) y llama `VaultRepository.delete`. Funciona con
     cualquier tipo de item porque sólo necesita el `id`.
   - Archivos tocados: `browserExtension/src/wasmJsMain/kotlin/dev/passwrd/browser/{Main,Dom}.kt`,
     `browserExtension/src/wasmJsMain/resources/popup.html`.
   - Compila y empaqueta en verde (`:browserExtension:wasmJsBrowserDistribution`).
   - **No probado en Chrome real todavía** — falta cargar
     `browserExtension/build/dist/wasmJs/productionExecutable/` en `chrome://extensions`
     (Modo desarrollador → Cargar descomprimida) y verificar editar/borrar a mano.

Commits de la sesión (orden): `d1163b6` (init) → `2727e39` (README+LICENSE) → `623eeaf`
(editar/borrar) → `0f1422c` (docs).

## Cómo retomar

```bash
cd "/c/Users/Coti/Documents/ProyectoPasswrd"
export JAVA_HOME="$LOCALAPPDATA/Programs/Android Studio/jbr"
git log --oneline -5                       # ubicarse
./gradlew :core:jvmTest :core:testDebugUnitTest   # confirmar que todo sigue verde
```

Primer paso pendiente real: **probar en Chrome** el editar/borrar de `browserExtension` antes de
seguir — si algo falla, hay que leer la consola del popup (clic derecho en el icono →
"Inspeccionar la ventana emergente", así no se cierra al perder foco).

## Pendiente / próximos pasos (por orden de impacto sugerido)

- [ ] Verificar en Chrome real editar/borrar de `browserExtension` (recién implementado, sin
      probar).
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

Detalle técnico completo (spec cripto, modelo de amenazas, ADRs, estado fase por fase) en
`docs/CRYPTO_SPEC.md`, `docs/THREAT_MODEL.md`, `docs/adr/`, `docs/PROJECT_STATUS.md`.
