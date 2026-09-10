package dev.passwrd.core.transfer

import dev.passwrd.core.model.ItemPayload
import dev.passwrd.core.model.ItemType

/**
 * Resultado de un importador externo, antes de tener id/timestamps de vault. El caller
 * (UI) lo convierte a [dev.passwrd.core.model.VaultItem] igual que hace al crear un item
 * a mano — ver `ItemEditViewModel.save`.
 */
data class ImportedItem(val type: ItemType, val payload: ItemPayload)
