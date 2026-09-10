package dev.passwrd.core.matching

/**
 * Compara el host de una URI guardada en un item con el dominio que pide autofill.
 * Ver docs/THREAT_MODEL.md "Superficie de ataque más sensible: autofill" — un fallo aquí
 * es el peor fallo posible del proyecto (entregar una credencial al dominio equivocado).
 */
object UriMatcher {
    /** Coincidencia exacta o de subdominio: "github.com" coincide con "gist.github.com". */
    fun matchesHost(storedUri: String, requestHost: String): Boolean {
        val storedHost = extractHost(storedUri)?.lowercase() ?: return false
        val target = requestHost.lowercase()
        return storedHost == target || target.endsWith(".$storedHost")
    }

    private fun extractHost(uri: String): String? {
        val withoutScheme = uri.substringAfter("://", uri)
        return withoutScheme.substringBefore("/").substringBefore(":").takeIf { it.isNotBlank() }
    }
}
