package dev.passwrd.core.transfer

import dev.passwrd.core.model.ItemPayload
import dev.passwrd.core.model.ItemType

/**
 * CSV genérico de logins — el mínimo común entre Chrome, Bitwarden y la mayoría de
 * gestores: cabecera con alguna variante de name/url/username/password/notes, en
 * cualquier orden. Todo lo demás se ignora.
 */
object CsvImporter {
    private val nameHeaders = setOf("name", "title")
    private val urlHeaders = setOf("url", "uri", "login_uri", "web site")
    private val userHeaders = setOf("username", "login", "login_username")
    private val passwordHeaders = setOf("password", "login_password")
    private val noteHeaders = setOf("notes", "note", "extra")

    fun import(text: String): List<ImportedItem> {
        val rows = parseCsv(text)
        if (rows.isEmpty()) return emptyList()
        val header = rows.first().map { it.trim().lowercase() }

        fun columnIndex(candidates: Set<String>) = header.indexOfFirst { it in candidates }
        val nameCol = columnIndex(nameHeaders)
        val urlCol = columnIndex(urlHeaders)
        val userCol = columnIndex(userHeaders)
        val passwordCol = columnIndex(passwordHeaders)
        val noteCol = columnIndex(noteHeaders)

        fun String?.orEmptyCell() = this ?: ""

        return rows.drop(1).filter { it.isNotEmpty() }.map { row ->
            fun cell(index: Int) = row.getOrNull(index).orEmptyCell()
            ImportedItem(
                ItemType.LOGIN,
                ItemPayload.Login(
                    name = cell(nameCol).ifBlank { cell(urlCol) },
                    username = cell(userCol),
                    password = cell(passwordCol),
                    uris = cell(urlCol).takeIf { it.isNotBlank() }?.let(::listOf) ?: emptyList(),
                    notes = cell(noteCol),
                ),
            )
        }
    }

    /** Parser RFC 4180 mínimo: comillas dobles, comas y comillas escapadas (""), sin librerías externas. */
    private fun parseCsv(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var i = 0

        fun endField() {
            row.add(field.toString())
            field.clear()
        }

        fun endRow() {
            endField()
            rows.add(row)
            row = mutableListOf()
        }

        while (i < text.length) {
            val c = text[i]
            when {
                inQuotes && c == '"' && i + 1 < text.length && text[i + 1] == '"' -> {
                    field.append('"')
                    i++
                }
                c == '"' -> inQuotes = !inQuotes
                !inQuotes && c == ',' -> endField()
                !inQuotes && (c == '\n' || c == '\r') -> {
                    if (c == '\r' && i + 1 < text.length && text[i + 1] == '\n') i++
                    endRow()
                }
                else -> field.append(c)
            }
            i++
        }
        if (field.isNotEmpty() || row.isNotEmpty()) endRow()
        return rows
    }
}
