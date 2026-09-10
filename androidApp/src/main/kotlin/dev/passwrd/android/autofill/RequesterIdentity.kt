package dev.passwrd.android.autofill

private val KNOWN_BROWSER_PACKAGES = setOf(
    "com.android.chrome", "com.chrome.beta", "com.chrome.dev", "com.chrome.canary",
    "org.mozilla.firefox", "org.mozilla.firefox_beta",
    "com.brave.browser", "com.microsoft.emmx", "com.opera.browser", "com.opera.mini.native",
    "com.duckduckgo.mobile.android", "com.sec.android.app.sbrowser", "com.vivaldi.browser",
)

/**
 * Identidad del solicitante de autofill — ver docs/THREAT_MODEL.md "Superficie de ataque
 * más sensible". `structure.activityComponent.packageName` lo resuelve el propio Android
 * (no lo declara la app solicitante), así que no es falsificable por una app maliciosa en
 * tiempo de ejecución normal.
 *
 * Limitación honesta de v1: no hay una base de datos de hashes de certificado esperados por
 * dominio (eso es infraestructura de la escala de Bitwarden/Proton). Para navegadores
 * conocidos usamos el `webDomain` — fuerte, viene del propio motor de renderizado. Para apps
 * nativas sólo hay una heurística sobre el nombre de paquete — débil a propósito, documentada
 * como tal, y sólo se usa para *filtrar sugerencias*, nunca para decidir qué mostrar sin que
 * el dominio guardado también encaje.
 */
object RequesterIdentity {
    fun isKnownBrowser(packageName: String): Boolean = packageName in KNOWN_BROWSER_PACKAGES

    /** "com.github.android" -> "github". Heurística, no una verificación de identidad real. */
    fun derivedBrandFromPackage(packageName: String): String? {
        val parts = packageName.split(".")
        if (parts.size < 2) return null
        return parts[parts.size - 2].takeIf { it.length >= 3 }
    }
}
