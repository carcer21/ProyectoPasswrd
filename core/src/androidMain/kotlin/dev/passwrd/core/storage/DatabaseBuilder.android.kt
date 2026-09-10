package dev.passwrd.core.storage

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

fun vaultDatabaseBuilder(context: Context, fileName: String = "passwrd.db"): RoomDatabase.Builder<PasswrdDatabase> {
    val appContext = context.applicationContext
    val dbFile = appContext.getDatabasePath(fileName)
    return Room.databaseBuilder<PasswrdDatabase>(context = appContext, name = dbFile.absolutePath)
}
