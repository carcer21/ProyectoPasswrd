package dev.passwrd.desktop.di

import dev.passwrd.core.storage.PasswrdDatabase
import dev.passwrd.core.storage.RoomVaultStore
import dev.passwrd.core.storage.buildVaultDatabase
import dev.passwrd.core.storage.vaultDatabaseBuilder
import dev.passwrd.core.vault.VaultRepository
import dev.passwrd.core.vault.VaultSession
import java.io.File

/** DI manual — mismo criterio que AppContainer de androidApp: una app de un solo módulo no necesita Koin/Hilt. */
class DesktopContainer {
    private val vaultDir = File(System.getProperty("user.home"), ".passwrd").apply { mkdirs() }

    val database: PasswrdDatabase = vaultDatabaseBuilder(File(vaultDir, "vault.db").absolutePath).buildVaultDatabase()
    private val vaultStore = RoomVaultStore(database)
    val vaultSession: VaultSession = VaultSession(vaultStore)
    val vaultRepository: VaultRepository = VaultRepository(vaultStore, vaultSession)
}
