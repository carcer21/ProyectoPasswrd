package dev.passwrd.android.di

import android.content.Context
import dev.passwrd.android.platform.BiometricUnlockManager
import dev.passwrd.android.platform.PinUnlockManager
import dev.passwrd.android.platform.QuickUnlockStore
import dev.passwrd.core.storage.PasswrdDatabase
import dev.passwrd.core.storage.RoomVaultStore
import dev.passwrd.core.storage.buildVaultDatabase
import dev.passwrd.core.storage.vaultDatabaseBuilder
import dev.passwrd.core.vault.VaultRepository
import dev.passwrd.core.vault.VaultSession

/**
 * DI manual: una app de un solo módulo no necesita Hilt/Koin (ver ponytail — "solución de
 * una línea antes que dependencia nueva"). Vive en la Application para sobrevivir a
 * cambios de configuración de la Activity.
 */
class AppContainer(context: Context) {
    val database: PasswrdDatabase = vaultDatabaseBuilder(context).buildVaultDatabase()
    private val vaultStore = RoomVaultStore(database)
    val vaultSession: VaultSession = VaultSession(vaultStore)
    val vaultRepository: VaultRepository = VaultRepository(vaultStore, vaultSession)

    private val quickUnlockStore = QuickUnlockStore(context)
    val pinUnlockManager = PinUnlockManager(quickUnlockStore)
    val biometricUnlockManager = BiometricUnlockManager(quickUnlockStore)
}
