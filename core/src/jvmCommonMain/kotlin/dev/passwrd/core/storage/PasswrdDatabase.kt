package dev.passwrd.core.storage

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

@Database(
    entities = [VaultItemEntity::class, VaultHeaderEntity::class],
    version = 1,
    exportSchema = true,
)
@ConstructedBy(PasswrdDatabaseConstructor::class)
abstract class PasswrdDatabase : RoomDatabase() {
    abstract fun vaultItemDao(): VaultItemDao
    abstract fun vaultHeaderDao(): VaultHeaderDao
}

// El compilador KSP de Room genera el `actual` de esto en cada target — no se escribe a mano.
@Suppress("KotlinNoActualForExpect")
expect object PasswrdDatabaseConstructor : RoomDatabaseConstructor<PasswrdDatabase> {
    override fun initialize(): PasswrdDatabase
}
