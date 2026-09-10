package dev.passwrd.android.credentials

/**
 * Allowlist para [androidx.credentials.provider.CallingAppInfo.getOrigin] — ver
 * docs/CRYPTO_SPEC.md "Passkeys" y docs/THREAT_MODEL.md. Formato exacto exigido por la
 * propia API (androidx.credentials.provider.CallingAppInfo, comentario de `getOrigin`).
 *
 * Limitación honesta de v1: el hash de Chrome de aquí abajo se extrajo con `apksigner` del
 * Chrome instalado en el emulador de pruebas — es el certificado de esa imagen de sistema
 * AOSP, no necesariamente el mismo que el build de Play Store en un dispositivo real. Un
 * despliegue real necesitaría el/los hash(es) oficiales de Chrome (y del resto de
 * navegadores que se quieran soportar como llamantes privilegiados), verificados contra una
 * fuente de confianza — no hay una base de datos centralizada que este proyecto pueda usar
 * sin mantenerla activamente.
 */
internal object PrivilegedApps {
    val allowlistJson: String = """
        {"apps": [
          {
            "type": "android",
            "info": {
              "package_name": "com.android.chrome",
              "signatures": [
                {"build": "release", "cert_fingerprint_sha256": "F0:FD:6C:5B:41:0F:25:CB:25:C3:B5:33:46:C8:97:2F:AE:30:F8:EE:74:11:DF:91:04:80:AD:6B:2D:60:DB:83"},
                {"build": "userdebug", "cert_fingerprint_sha256": "F0:FD:6C:5B:41:0F:25:CB:25:C3:B5:33:46:C8:97:2F:AE:30:F8:EE:74:11:DF:91:04:80:AD:6B:2D:60:DB:83"}
              ]
            }
          }
        ]}
    """.trimIndent()
}
