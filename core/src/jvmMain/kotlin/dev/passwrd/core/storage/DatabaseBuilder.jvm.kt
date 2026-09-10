package dev.passwrd.core.storage

import androidx.room.Room
import androidx.room.RoomDatabase

/** Para el futuro desktopApp (Fase 9) y para tests en `:core:jvmTest`. */
fun vaultDatabaseBuilder(path: String): RoomDatabase.Builder<PasswrdDatabase> =
    Room.databaseBuilder<PasswrdDatabase>(name = path)

fun inMemoryVaultDatabaseBuilder(): RoomDatabase.Builder<PasswrdDatabase> =
    Room.inMemoryDatabaseBuilder<PasswrdDatabase>()
