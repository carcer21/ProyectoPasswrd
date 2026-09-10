package dev.passwrd.core.storage

import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers

/**
 * Los constructores de [RoomDatabase.Builder] son específicos de plataforma (Android
 * necesita un `Context`, JVM sólo una ruta) y viven en `androidMain`/`jvmMain`. Este paso
 * final sí es común.
 */
fun RoomDatabase.Builder<PasswrdDatabase>.buildVaultDatabase(): PasswrdDatabase =
    setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
