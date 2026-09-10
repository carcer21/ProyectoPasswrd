package dev.passwrd.android.transfer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dev.passwrd.core.crypto.zeroize
import dev.passwrd.core.model.VaultItem
import dev.passwrd.core.transfer.BitwardenImporter
import dev.passwrd.core.transfer.CsvImporter
import dev.passwrd.core.transfer.ImportedItem
import dev.passwrd.core.transfer.ProtonPassImporter
import dev.passwrd.core.transfer.VaultExporter
import dev.passwrd.core.transfer.VaultImporter
import dev.passwrd.core.vault.VaultRepository
import kotlinx.coroutines.flow.first
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

enum class ImportFormat { BITWARDEN, PROTON_PASS, CSV }

/** Fase 7 del plan: export/import cifrado propio + importadores de terceros. */
class TransferViewModel(private val repository: VaultRepository) : ViewModel() {

    suspend fun exportEncrypted(exportPassword: ByteArray): String {
        val items = repository.observeActive().first()
        val json = VaultExporter.export(items, exportPassword)
        exportPassword.zeroize()
        return json
    }

    /** @return número de items importados. Lanza si la contraseña de export es incorrecta. */
    suspend fun importEncrypted(exportedJson: String, exportPassword: ByteArray): Int {
        val items = VaultImporter.import(exportedJson, exportPassword)
        exportPassword.zeroize()
        items.forEach { repository.upsert(it) }
        return items.size
    }

    @OptIn(ExperimentalUuidApi::class)
    suspend fun importExternal(format: ImportFormat, text: String): Int {
        val imported: List<ImportedItem> = when (format) {
            ImportFormat.BITWARDEN -> BitwardenImporter.import(text)
            ImportFormat.PROTON_PASS -> ProtonPassImporter.import(text)
            ImportFormat.CSV -> CsvImporter.import(text)
        }
        val now = System.currentTimeMillis()
        imported.forEach {
            repository.upsert(
                VaultItem(
                    id = Uuid.random().toString(),
                    type = it.type,
                    payload = it.payload,
                    createdAt = now,
                    updatedAt = now,
                    revision = 1,
                ),
            )
        }
        return imported.size
    }

    companion object {
        fun factory(repository: VaultRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = TransferViewModel(repository) as T
        }
    }
}
