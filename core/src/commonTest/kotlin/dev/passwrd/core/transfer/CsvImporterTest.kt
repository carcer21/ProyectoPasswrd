package dev.passwrd.core.transfer

import dev.passwrd.core.model.ItemPayload
import dev.passwrd.core.model.ItemType
import kotlin.test.Test
import kotlin.test.assertEquals

class CsvImporterTest {
    @Test
    fun importsRowsWithQuotedFieldsAndCommas() {
        val csv = "name,url,username,password,notes\n" +
            "GitHub,https://github.com,carcertor21,hunter2,\"cuenta, personal\"\n" +
            "\"Sitio \"\"raro\"\"\",https://example.com,user,\"p,a\"\"s\","

        val items = CsvImporter.import(csv)

        assertEquals(2, items.size)
        assertEquals(ItemType.LOGIN, items[0].type)
        assertEquals(
            ItemPayload.Login(
                name = "GitHub",
                username = "carcertor21",
                password = "hunter2",
                uris = listOf("https://github.com"),
                notes = "cuenta, personal",
            ),
            items[0].payload,
        )

        val second = items[1].payload as ItemPayload.Login
        assertEquals("Sitio \"raro\"", second.name)
        assertEquals("p,a\"s", second.password)
    }

    @Test
    fun handlesReorderedAndUnknownColumns() {
        val csv = """
            password,foo,login_username,web site,name
            hunter2,ignoreme,carcertor21,https://github.com,GitHub
        """.trimIndent()

        val items = CsvImporter.import(csv)

        assertEquals(1, items.size)
        val login = items[0].payload as ItemPayload.Login
        assertEquals("GitHub", login.name)
        assertEquals("carcertor21", login.username)
        assertEquals("hunter2", login.password)
        assertEquals(listOf("https://github.com"), login.uris)
    }
}
